package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.BookingRequest;
import com.accommodationfinder.dto.MessageDtos.SendMessageRequest;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.*;
import com.accommodationfinder.security.JwtService;
import com.accommodationfinder.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Right of access (export) and right to erasure. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GdprTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingService bookingService;
    @Autowired private MessageService messageService;
    @Autowired private UserNotificationService userNotificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @MockitoBean private MailGateway mailGateway;

    private TestFixtures fixtures;
    private User landlord;
    private User tenant;
    private Room room;
    private Long bookingId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(userRepository, roomRepository, passwordEncoder, jwtService);
        landlord = fixtures.user(Role.LANDLORD, true);
        tenant = fixtures.user(Role.TENANT, true);
        room = fixtures.room(landlord);

        LocalDate checkIn = LocalDate.now().plusDays(20);
        bookingId = bookingService.request(new BookingRequest(room.getId(), checkIn, checkIn.plusDays(60), 1, null), tenant).id();
        messageService.send(new SendMessageRequest(landlord.getId(), room.getId(), "My passport number is 12AB34567"), tenant);
        favoriteRepository.save(new Favorite(tenant, room));
    }

    @Test
    void theExportContainsEverythingAboutThePersonButNotThePasswordHash() throws Exception {
        String hash = userRepository.findById(tenant.getId()).orElseThrow().getPassword();

        mockMvc.perform(get("/api/users/me/export").header("Authorization", fixtures.bearer(tenant)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.profile.email").value(tenant.getEmail()))
                .andExpect(jsonPath("$.profile.phone").value("+33 6 11 22 33 44"))
                .andExpect(jsonPath("$.bookingsAsTenant.length()").value(1))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.favouriteRoomIds[0]").value(room.getId()))
                .andExpect(content().string(not(containsString(hash))));
    }

    @Test
    void deletionNeedsThePassword() throws Exception {
        mockMvc.perform(delete("/api/users/me").header("Authorization", fixtures.bearer(tenant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong-password\"}"))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findById(tenant.getId()).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    void deletionErasesThePersonButKeepsTheOtherPartysRecordsIntact() throws Exception {
        String originalEmail = tenant.getEmail();
        String token = fixtures.bearer(tenant);

        mockMvc.perform(delete("/api/users/me").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestFixtures.PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        User erased = userRepository.findById(tenant.getId()).orElseThrow();
        assertThat(erased.getName()).isEqualTo(AccountService.DELETED_NAME);
        assertThat(erased.getEmail()).endsWith("@deleted.invalid");
        assertThat(erased.getPhone()).isNull();
        assertThat(erased.isEnabled()).isFalse();
        assertThat(erased.getDeletedAt()).isNotNull();

        // The landlord keeps the booking record and the thread, minus the tenant's words.
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(messageRepository.findAllForUser(landlord.getId()))
                .singleElement()
                .satisfies(m -> assertThat(m.getContent()).isEqualTo(AccountService.REDACTED_MESSAGE));
        assertThat(favoriteRepository.findRoomIdsByUserId(tenant.getId())).isEmpty();
        assertThat(userNotificationService.list(landlord.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .content()).anyMatch(n -> n.title().equals("A booking request was withdrawn"));

        // The session that did it is over, and the old credentials open nothing.
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + originalEmail + "\",\"password\":\"" + TestFixtures.PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        verify(mailGateway).send(eq(originalEmail), eq("Your account has been deleted"), anyString());
    }

    @Test
    void anAdminCanEraseAUserWithHistory() throws Exception {
        User admin = fixtures.user(Role.ADMIN, true);
        // This used to be a hard DELETE that failed on the booking and message foreign keys.
        mockMvc.perform(delete("/api/admin/users/" + tenant.getId()).header("Authorization", fixtures.bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(tenant.getId()).orElseThrow().getDeletedAt()).isNotNull();
    }
}
