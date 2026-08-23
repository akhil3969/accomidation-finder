package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.BookingRequest;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.mapper.BookingMapper;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.BookingRepository;
import com.accommodationfinder.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private NotificationService notificationService;
    @Spy  private BookingMapper bookingMapper = new BookingMapper();

    @InjectMocks private BookingService bookingService;

    private User landlord;
    private User tenant;
    private Room room;

    @BeforeEach
    void setUp() {
        landlord = new User("Jean", "jean@landlord.fr", "hash", Role.LANDLORD);
        landlord.setId(1L);

        tenant = new User("Akhil", "akhil@epita.fr", "hash", Role.TENANT);
        tenant.setId(2L);

        room = new Room();
        room.setId(10L);
        room.setTitle("Bright studio");
        room.setCity("Paris");
        room.setPrice(BigDecimal.valueOf(900));
        room.setAvailable(true);
        room.setMaxGuests(2);
        room.setLandlord(landlord);
    }

    @Test
    void createsAPendingBookingAndNotifiesTheLandlord() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(30);

        when(roomRepository.findByIdWithLandlord(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(anyLong(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> {
            Booking saved = call.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        var response = bookingService.request(
                new BookingRequest(10L, checkIn, checkOut, 1, "Hello"), tenant);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        // 30 days of a EUR 900/month rent, pro-rated over a 30-day month.
        assertThat(response.totalPrice()).isEqualByComparingTo("900.00");

        ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
        verify(notificationService).bookingEvent(recipient.capture(), any());
        assertThat(recipient.getValue()).isEqualTo("jean@landlord.fr");
    }

    @Test
    void refusesOverlappingDates() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        when(roomRepository.findByIdWithLandlord(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(anyLong(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.request(
                new BookingRequest(10L, checkIn, checkIn.plusDays(20), 1, null), tenant))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already requested");
    }

    @Test
    void refusesCheckOutBeforeCheckIn() {
        LocalDate checkIn = LocalDate.now().plusDays(20);
        when(roomRepository.findByIdWithLandlord(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.request(
                new BookingRequest(10L, checkIn, checkIn.minusDays(1), 1, null), tenant))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Check-out must be after check-in");
    }

    @Test
    void refusesToLetALandlordBookTheirOwnListing() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        when(roomRepository.findByIdWithLandlord(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.request(
                new BookingRequest(10L, checkIn, checkIn.plusDays(40), 1, null), landlord))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own listing");
    }

    @Test
    void refusesMoreGuestsThanTheListingAccepts() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        when(roomRepository.findByIdWithLandlord(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> bookingService.request(
                new BookingRequest(10L, checkIn, checkIn.plusDays(40), 5, null), tenant))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most 2 guest");
    }
}
