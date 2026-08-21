package com.accommodationfinder.service;

import com.accommodationfinder.dto.MessageDtos.*;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.MessageMapper;
import com.accommodationfinder.model.Message;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.MessageRepository;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          RoomRepository roomRepository,
                          MessageMapper messageMapper,
                          NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.messageMapper = messageMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public MessageResponse send(SendMessageRequest request, User sender) {
        if (request.recipientId().equals(sender.getId())) {
            throw new BadRequestException("You cannot message yourself");
        }
        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.recipientId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.roomId()));

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setRoom(room);
        message.setContent(request.content().trim());

        MessageResponse response = messageMapper.toResponse(messageRepository.save(message));

        // Push to the recipient, and echo to the sender so their other tabs stay in sync.
        notificationService.chatMessage(recipient.getEmail(), response);
        notificationService.chatMessage(sender.getEmail(), response);
        return response;
    }

    @Transactional
    public List<MessageResponse> conversation(Long roomId, Long otherUserId, User currentUser) {
        List<MessageResponse> thread = messageRepository
                .findConversation(roomId, currentUser.getId(), otherUserId)
                .stream()
                .map(messageMapper::toResponse)
                .toList();

        messageRepository.markConversationRead(currentUser.getId(), roomId, otherUserId);
        return thread;
    }

    /** Groups every message into one row per (room, counterpart) pair, newest first. */
    @Transactional(readOnly = true)
    public List<ConversationSummary> inbox(User currentUser) {
        List<Message> messages = messageRepository.findAllForUser(currentUser.getId());
        Map<String, ConversationSummary> threads = new LinkedHashMap<>();
        Map<String, Long> unread = new LinkedHashMap<>();

        // First pass: count unread per thread.
        for (Message message : messages) {
            if (!message.isRead() && message.getRecipient().getId().equals(currentUser.getId())) {
                unread.merge(key(message, currentUser), 1L, Long::sum);
            }
        }

        // Second pass: messages arrive newest first, so the first hit per key is the latest.
        for (Message message : messages) {
            String key = key(message, currentUser);
            if (threads.containsKey(key)) {
                continue;
            }
            User other = message.getSender().getId().equals(currentUser.getId())
                    ? message.getRecipient()
                    : message.getSender();
            threads.put(key, new ConversationSummary(
                    message.getRoom() == null ? null : message.getRoom().getId(),
                    message.getRoom() == null ? "Deleted listing" : message.getRoom().getTitle(),
                    UserDtos.UserSummary.publicProfile(other),
                    message.getContent(),
                    message.getSentAt(),
                    unread.getOrDefault(key, 0L)));
        }

        List<ConversationSummary> result = new ArrayList<>(threads.values());
        result.sort(Comparator.comparing(ConversationSummary::lastMessageAt).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countByRecipientIdAndReadFalse(userId);
    }

    private String key(Message message, User currentUser) {
        Long otherId = message.getSender().getId().equals(currentUser.getId())
                ? message.getRecipient().getId()
                : message.getSender().getId();
        Long roomId = message.getRoom() == null ? 0L : message.getRoom().getId();
        return roomId + ":" + otherId;
    }
}
