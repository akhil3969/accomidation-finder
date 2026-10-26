package com.accommodationfinder.repository;

import com.accommodationfinder.model.Room;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    /** Eagerly loads the landlord so detail responses do not lazy-load outside the session. */
    @Query("select r from Room r join fetch r.landlord where r.id = :id")
    Optional<Room> findByIdWithLandlord(@Param("id") Long id);

    /**
     * Same as {@link #findByIdWithLandlord} but takes a row lock ({@code SELECT ... FOR UPDATE})
     * held until the surrounding transaction commits. Booking requests for the same room
     * therefore run their overlap check one at a time instead of both passing it at once.
     * The timeout turns a stuck lock into a clean 409 rather than a hung request.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select r from Room r join fetch r.landlord where r.id = :id")
    Optional<Room> findByIdWithLandlordForUpdate(@Param("id") Long id);

    /**
     * Admin listing rows carry the landlord's name. Fetching it in the same
     * query is what keeps the mapping working once the session has closed -
     * `open-in-view` is off, so touching the lazy proxy afterwards threw
     * LazyInitializationException and the endpoint returned a 500.
     */
    @Query(value = "select r from Room r join fetch r.landlord",
           countQuery = "select count(r) from Room r")
    Page<Room> findAllWithLandlord(Pageable pageable);

    Page<Room> findByLandlordId(Long landlordId, Pageable pageable);

    long countByLandlordId(Long landlordId);

    long countByLandlordIdAndAvailableTrue(Long landlordId);

    long countByAvailableTrue();

    long countByCreatedAtAfter(LocalDateTime since);

    long countByVerifiedTrue();

    long countByHiddenTrue();

    /** Listings the scam scorer flagged, worst first - the moderator's work queue. */
    List<Room> findTop20ByRiskScoreGreaterThanOrderByRiskScoreDesc(int threshold);

    @Query("select distinct r.city from Room r where r.hidden = false order by r.city asc")
    List<String> findDistinctCities();

    /** Listings per city, for the admin breakdown chart. */
    @Query("select r.city, count(r) from Room r where r.hidden = false "
            + "group by r.city order by count(r) desc")
    List<Object[]> countByCity();

    @Query("select function('date', r.createdAt), count(r) from Room r "
            + "where r.createdAt >= :since "
            + "group by function('date', r.createdAt) "
            + "order by function('date', r.createdAt)")
    List<Object[]> countPerDaySince(@Param("since") LocalDateTime since);

    /** Average rent in a city, used to spot listings priced suspiciously far below market. */
    @Query("select avg(r.price) from Room r where r.city = :city and r.hidden = false")
    Double averagePriceForCity(@Param("city") String city);
}
