package com.accommodationfinder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class MessageDtos {

    private MessageDtos() {
    }

    /** Sent over REST (POST /api/messages) or STOMP (/app/chat.send). */
    public record SendMessageRequest(
            @NotNull(message = "Recipient is required")
            Long recipientId,

            @NotNull(message = "Room is required")
            Long roomId,

            @NotBlank(message = "Message cannot be empty")
            @Size(max = 2000, message = "Message cannot exceed 2000 characters")
            String content
    ) {
    }

    public record MessageResponse(
            Long id,
            Long roomId,
            String roomTitle,
            UserDtos.UserSummary sender,
            UserDtos.UserSummary recipient,
            String content,
            boolean read,
            LocalDateTime sentAt
    ) {
    }

    /** One row in the inbox list. */
    public record ConversationSummary(
            Long roomId,
            String roomTitle,
            UserDtos.UserSummary otherUser,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount
    ) {
    }
}
