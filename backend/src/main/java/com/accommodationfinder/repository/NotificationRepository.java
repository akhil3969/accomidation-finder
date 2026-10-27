package com.accommodationfinder.repository;

import com.accommodationfinder.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<Notification> findFirstByRecipientIdAndCollapseKeyAndReadFalseOrderByCreatedAtDesc(
            Long recipientId, String collapseKey);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);

    /** Unread rows nobody has emailed about yet, old enough that the person clearly missed them. */
    @Query("""
           select n from Notification n join fetch n.recipient
           where n.read = false and n.emailedAt is null and n.createdAt < :before
           order by n.recipient.id, n.createdAt
           """)
    List<Notification> findAwaitingEmail(@Param("before") LocalDateTime before);

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :recipientId")
    int deleteByRecipientId(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("delete from Notification n where n.read = true and n.createdAt < :before")
    int deleteReadOlderThan(@Param("before") LocalDateTime before);
}
