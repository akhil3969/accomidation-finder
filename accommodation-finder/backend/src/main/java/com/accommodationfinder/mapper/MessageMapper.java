package com.accommodationfinder.mapper;

import com.accommodationfinder.dto.MessageDtos.MessageResponse;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }
        return new MessageResponse(
                message.getId(),
                message.getRoom() == null ? null : message.getRoom().getId(),
                message.getRoom() == null ? null : message.getRoom().getTitle(),
                UserDtos.UserSummary.publicProfile(message.getSender()),
                UserDtos.UserSummary.publicProfile(message.getRecipient()),
                message.getContent(),
                message.isRead(),
                message.getSentAt());
    }
}
