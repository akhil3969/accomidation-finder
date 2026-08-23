package com.accommodationfinder.repository;

import com.accommodationfinder.model.Booking;
import com.accommodationfinder.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByTenantIdOrderByRequestedAtDesc(Long tenantId, Pageable pageable);

    Page<Booking> findByRoomLandlordIdOrderByRequestedAtDesc(Long landlordId, Pageable pageable);

    List<Booking> findByRoomIdAndStatusInOrderByCheckInAsc(Long roomId, List<BookingStatus> statuses);

    long countByRoomLandlordIdAndStatus(Long landlordId, BookingStatus status);

    long countByTenantIdAndStatus(Long tenantId, BookingStatus status);

    /**
     * Detects a date clash with any booking that still holds the calendar
     * (PENDING or CONFIRMED). Two ranges overlap when each starts before the other ends.
     */
    @Query("""
           select count(b) > 0 from Booking b
           where b.room.id = :roomId
             and b.status in (com.accommodationfinder.model.BookingStatus.PENDING,
                              com.accommodationfinder.model.BookingStatus.CONFIRMED)
             and b.checkIn < :checkOut
             and b.checkOut > :checkIn
           """)
    boolean existsOverlappingBooking(@Param("roomId") Long roomId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut);

    /** Same as above but ignores one booking - used when a landlord re-confirms an existing request. */
    @Query("""
           select count(b) > 0 from Booking b
           where b.room.id = :roomId
             and b.id <> :excludedId
             and b.status = com.accommodationfinder.model.BookingStatus.CONFIRMED
             and b.checkIn < :checkOut
             and b.checkOut > :checkIn
           """)
    boolean existsOverlappingConfirmedBookingExcluding(@Param("roomId") Long roomId,
                                                       @Param("excludedId") Long excludedId,
                                                       @Param("checkIn") LocalDate checkIn,
                                                       @Param("checkOut") LocalDate checkOut);

    boolean existsByRoomIdAndTenantIdAndStatus(Long roomId, Long tenantId, BookingStatus status);

    long countByStatus(BookingStatus status);

    long countByRequestedAtAfter(LocalDateTime since);

    @Query("select function('date', b.requestedAt), count(b) from Booking b "
            + "where b.requestedAt >= :since "
            + "group by function('date', b.requestedAt) "
            + "order by function('date', b.requestedAt)")
    List<Object[]> countPerDaySince(@Param("since") LocalDateTime since);

    @Query("select b.status, count(b) from Booking b group by b.status")
    List<Object[]> countGroupedByStatus();
}
