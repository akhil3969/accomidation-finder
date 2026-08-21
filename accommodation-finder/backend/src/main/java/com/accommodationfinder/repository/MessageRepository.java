package com.accommodationfinder.repository;

import com.accommodationfinder.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Full thread between two users about one room, oldest first. */
    @Query("""
           select m from Message m
           where m.room.id = :roomId
             and ((m.sender.id = :userA and m.recipient.id = :userB)
               or (m.sender.id = :userB and m.recipient.id = :userA))
           order by m.sentAt asc
           """)
    List<Message> findConversation(@Param("roomId") Long roomId,
                                   @Param("userA") Long userA,
                                   @Param("userB") Long userB);

    /** Every message the user is part of, newest first - grouped into threads in the service. */
    @Query("""
           select m from Message m
           where m.sender.id = :userId or m.recipient.id = :userId
           order by m.sentAt desc
           """)
    List<Message> findAllForUser(@Param("userId") Long userId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("""
           update Message m set m.read = true
           where m.recipient.id = :userId and m.room.id = :roomId and m.sender.id = :otherUserId
           """)
    int markConversationRead(@Param("userId") Long userId,
                             @Param("roomId") Long roomId,
                             @Param("otherUserId") Long otherUserId);
}
