package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.BookingRequest;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.BookingRepository;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real (H2) database, because the bug being guarded against only
 * exists when several transactions genuinely overlap - a mock cannot reproduce it.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTests {

    private static final int CONCURRENT_TENANTS = 8;

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void simultaneousRequestsForTheSameDatesProduceExactlyOneBooking() throws Exception {
        Room room = newRoom();
        List<User> tenants = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_TENANTS; i++) {
            tenants.add(newUser(Role.TENANT));
        }
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(60);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_TENANTS);
        CountDownLatch startingGun = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (User tenant : tenants) {
                futures.add(pool.submit(() -> {
                    startingGun.await();
                    try {
                        bookingService.request(
                                new BookingRequest(room.getId(), checkIn, checkOut, 1, null), tenant);
                        accepted.incrementAndGet();
                    } catch (ConflictException | ConcurrencyFailureException expected) {
                        refused.incrementAndGet();
                    }
                    return null;
                }));
            }
            startingGun.countDown();               // release every thread at once
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);  // rethrows anything unexpected
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(accepted.get()).as("requests accepted").isEqualTo(1);
        assertThat(refused.get()).as("requests refused").isEqualTo(CONCURRENT_TENANTS - 1);
        assertThat(bookingRepository.findByRoomIdAndStatusInOrderByCheckInAsc(
                room.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .hasSize(1);
    }

    @Test
    void aStaleCopyOfABookingCannotOverwriteANewerChange() {
        Room room = newRoom();
        User tenant = newUser(Role.TENANT);
        LocalDate checkIn = LocalDate.now().plusDays(100);
        Long bookingId = bookingService.request(
                new BookingRequest(room.getId(), checkIn, checkIn.plusDays(30), 1, null), tenant).id();

        // Two people load the same booking...
        Booking landlordsCopy = transactionTemplate.execute(s -> bookingRepository.findById(bookingId).orElseThrow());
        Booking tenantsCopy = transactionTemplate.execute(s -> bookingRepository.findById(bookingId).orElseThrow());

        // ...the tenant saves first...
        tenantsCopy.setStatus(BookingStatus.CANCELLED);
        transactionTemplate.executeWithoutResult(s -> bookingRepository.save(tenantsCopy));

        // ...so the landlord's save, based on the old version, must be rejected.
        landlordsCopy.setStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(s -> bookingRepository.save(landlordsCopy)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    private User newUser(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.fr";
        return userRepository.save(new User("Test " + role, email, "hash", role));
    }

    private Room newRoom() {
        Room room = new Room();
        room.setTitle("Contested studio");
        room.setCity("Paris");
        room.setPrice(BigDecimal.valueOf(800));
        room.setMaxGuests(2);
        room.setAvailable(true);
        room.setLandlord(newUser(Role.LANDLORD));
        return roomRepository.save(room);
    }
}
