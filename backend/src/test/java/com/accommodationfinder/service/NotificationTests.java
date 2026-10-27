package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.BookingDecisionRequest;
import com.accommodationfinder.dto.BookingDtos.BookingRequest;
import com.accommodationfinder.dto.MessageDtos.SendMessageRequest;
import com.accommodationfinder.dto.NotificationDtos.NotificationResponse;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.NotificationRepository;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.security.JwtService;
import com.accommodationfinder.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Persistent notifications and the "you missed this" email digest. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationTests {

    @Autowired private BookingService bookingService;
    @Autowired private MessageService messageService;
    @Autowired private UserNotificationService userNotificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EmailService emailService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MailGateway mailGateway;

    private TestFixtures fixtures;
    private User landlord;
    private User tenant;
    private Room room;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(userRepository, roomRepository, passwordEncoder, jwtService);
        landlord = fixtures.user(Role.LANDLORD, true);
        tenant = fixtures.user(Role.TENANT, true);
        room = fixtures.room(landlord);
    }

    @Test
    void aBookingRequestWaitsInTheLandlordsBellUntilRead() {
        requestBooking();

        assertThat(userNotificationService.unreadCount(landlord.getId())).isEqualTo(1);
        NotificationResponse n = userNotificationService.list(landlord.getId(), PageRequest.of(0, 10)).content().get(0);
        assertThat(n.type()).isEqualTo(NotificationType.BOOKING_REQUESTED);
        assertThat(n.title()).contains(room.getTitle());
        assertThat(n.link()).isEqualTo("/dashboard");
    }

    @Test
    void theLandlordsDecisionReachesTheTenant() {
        Long bookingId = requestBooking();
        bookingService.decide(bookingId, new BookingDecisionRequest(BookingStatus.CONFIRMED, "See you in September"),
                userRepository.findById(landlord.getId()).orElseThrow());

        NotificationResponse n = userNotificationService.list(tenant.getId(), PageRequest.of(0, 10)).content().get(0);
        assertThat(n.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(n.body()).contains("See you in September");
    }

    @Test
    void aBurstOfChatMessagesIsOneBellEntryNotTen() {
        for (int i = 0; i < 5; i++) {
            messageService.send(new SendMessageRequest(landlord.getId(), room.getId(), "Message " + i), tenant);
        }
        assertThat(userNotificationService.unreadCount(landlord.getId())).isEqualTo(1);
        assertThat(userNotificationService.list(landlord.getId(), PageRequest.of(0, 10)).content().get(0).body())
                .isEqualTo("Message 4");
    }

    @Test
    void someoneElsesNotificationCannotBeReadOrProbed() throws Exception {
        requestBooking();
        Long landlordsNotification = userNotificationService.list(landlord.getId(), PageRequest.of(0, 1))
                .content().get(0).id();

        mockMvc.perform(patch("/api/notifications/" + landlordsNotification + "/read")
                        .header("Authorization", fixtures.bearer(tenant)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", fixtures.bearer(landlord)))
                .andExpect(jsonPath("$.unread").value(1));
    }

    @Test
    void theDigestEmailsMissedNotificationsOnceAndSkipsWhatWasRead() {
        User unverified = fixtures.user(Role.LANDLORD, false);
        Room unverifiedRoom = fixtures.room(unverified);
        requestBooking();                                                  // landlord: unread
        bookingService.request(new BookingRequest(unverifiedRoom.getId(),
                LocalDate.now().plusDays(40), LocalDate.now().plusDays(70), 1, null), tenant);
        messageService.send(new SendMessageRequest(tenant.getId(), room.getId(), "Seen it"), landlord);
        userNotificationService.markAllRead(tenant);                       // tenant: read in time

        NotificationEmailJob job = new NotificationEmailJob(notificationRepository, emailService, Duration.ZERO);
        transactionTemplate.executeWithoutResult(s -> job.run());

        verify(mailGateway).send(eq(landlord.getEmail()), contains("New booking request"), anyString());
        verify(mailGateway, never()).send(eq(tenant.getEmail()), anyString(), anyString());
        verify(mailGateway, never()).send(eq(unverified.getEmail()), anyString(), anyString());

        clearInvocations(mailGateway);
        transactionTemplate.executeWithoutResult(s -> job.run());
        verify(mailGateway, never()).send(eq(landlord.getEmail()), anyString(), anyString());
    }

    @Test
    void peopleWhoOptedOutAreNotEmailed() {
        transactionTemplate.executeWithoutResult(s -> {
            User l = userRepository.findById(landlord.getId()).orElseThrow();
            l.setEmailNotifications(false);
        });
        requestBooking();

        NotificationEmailJob job = new NotificationEmailJob(notificationRepository, emailService, Duration.ZERO);
        transactionTemplate.executeWithoutResult(s -> job.run());

        verify(mailGateway, never()).send(eq(landlord.getEmail()), anyString(), anyString());
        assertThat(userNotificationService.unreadCount(landlord.getId())).as("still in the bell").isEqualTo(1);
    }

    private Long requestBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(10);   // each test has its own room
        return bookingService.request(new BookingRequest(room.getId(), checkIn, checkIn.plusDays(30), 1,
                "Hello, I start my MSc in September"), tenant).id();
    }
}
