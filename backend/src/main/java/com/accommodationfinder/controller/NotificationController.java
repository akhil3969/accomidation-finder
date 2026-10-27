package com.accommodationfinder.controller;

import com.accommodationfinder.dto.NotificationDtos.NotificationResponse;
import com.accommodationfinder.dto.NotificationDtos.UnreadCount;
import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "The bell: booking updates and new messages, stored until read")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    public NotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @GetMapping
    @Operation(summary = "Your notifications, newest first")
    public PageResponse<NotificationResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userNotificationService.list(SecurityUtils.requireCurrentUser().getId(), pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "How many notifications are unread - the number on the bell")
    public UnreadCount unreadCount() {
        return new UnreadCount(userNotificationService.unreadCount(SecurityUtils.requireCurrentUser().getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return userNotificationService.markRead(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<Void> markAllRead() {
        userNotificationService.markAllRead(SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
