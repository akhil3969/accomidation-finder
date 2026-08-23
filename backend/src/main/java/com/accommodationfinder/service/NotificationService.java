package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.BookingResponse;
import com.accommodationfinder.dto.MessageDtos.MessageResponse;
import com.accommodationfinder.dto.RoomDtos.AvailabilityEvent;
import com.accommodationfinder.dto.RoomDtos.RoomResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Single place where the app pushes real-time events, so controllers and services
 * never talk to the broker directly.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String TOPIC_NEW_ROOM = "/topic/rooms/new";
    public static final String TOPIC_AVAILABILITY = "/topic/rooms/availability";
    public static final String TOPIC_ROOM_PREFIX = "/topic/rooms/";
    public static final String QUEUE_MESSAGES = "/queue/messages";
    public static final String QUEUE_BOOKINGS = "/queue/bookings";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void roomPublished(RoomResponse room) {
        send(TOPIC_NEW_ROOM, room);
    }

    public void roomUpdated(RoomResponse room) {
        send(TOPIC_ROOM_PREFIX + room.id(), room);
    }

    public void availabilityChanged(AvailabilityEvent event) {
        send(TOPIC_AVAILABILITY, event);
        send(TOPIC_ROOM_PREFIX + event.roomId() + "/availability", event);
    }

    /** Private delivery - the recipient receives it on /user/queue/messages. */
    public void chatMessage(String recipientEmail, MessageResponse message) {
        sendToUser(recipientEmail, QUEUE_MESSAGES, message);
    }

    public void bookingEvent(String recipientEmail, BookingResponse booking) {
        sendToUser(recipientEmail, QUEUE_BOOKINGS, booking);
    }

    private void send(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            // A broker hiccup must never fail the HTTP request that triggered it.
            log.warn("Could not broadcast to {}: {}", destination, ex.getMessage());
        }
    }

    private void sendToUser(String username, String destination, Object payload) {
        if (username == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
        } catch (Exception ex) {
            log.warn("Could not deliver to {}{}: {}", username, destination, ex.getMessage());
        }
    }
}
