package com.accommodationfinder.dto;

import com.accommodationfinder.model.Notification;
import com.accommodationfinder.model.NotificationType;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String title,
            String body,
            String link,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                    n.getLink(), n.isRead(), n.getCreatedAt());
        }
    }

    public record UnreadCount(long unread) {
    }
}
