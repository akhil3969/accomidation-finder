package com.accommodationfinder.repository;

import com.accommodationfinder.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    boolean existsByRoomIdAndAuthorId(Long roomId, Long authorId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.room.id = :roomId")
    double averageRatingForRoom(@Param("roomId") Long roomId);

    long countByRoomId(Long roomId);

    /** Batch aggregate so a page of rooms costs one extra query instead of N. */
    @Query("""
           select r.room.id, avg(r.rating), count(r)
           from Review r
           where r.room.id in :roomIds
           group by r.room.id
           """)
    List<Object[]> aggregateRatings(@Param("roomIds") Collection<Long> roomIds);
}
