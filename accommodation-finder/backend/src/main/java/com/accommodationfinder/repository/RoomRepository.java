package com.accommodationfinder.repository;

import com.accommodationfinder.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
