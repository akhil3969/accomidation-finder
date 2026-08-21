package com.accommodationfinder.controller;

import com.accommodationfinder.dto.MessageDtos.*;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Messages", description = "Direct messaging between tenants and landlords")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Send a message (REST fallback for clients without a WebSocket)")
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.send(request, SecurityUtils.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/inbox")
    @Operation(summary = "One row per conversation, newest first")
    public List<ConversationSummary> inbox() {
        return messageService.inbox(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/conversation")
    @Operation(summary = "Full thread with one person about one listing - also marks it read")
    public List<MessageResponse> conversation(@RequestParam Long roomId, @RequestParam Long userId) {
        return messageService.conversation(roomId, userId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Badge counter for the navbar")
    public Map<String, Long> unreadCount() {
        long count = messageService.unreadCount(SecurityUtils.requireCurrentUser().getId());
        return Map.of("unread", count);
    }
}
