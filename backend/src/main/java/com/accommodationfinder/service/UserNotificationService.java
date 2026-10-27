package com.accommodationfinder.service;

import com.accommodationfinder.dto.NotificationDtos.NotificationResponse;
import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.Notification;
import com.accommodationfinder.model.NotificationType;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The persistent half of notifications: every event is written as a row (the bell, the
 * email job) and then pushed over the socket for anyone who happens to be online.
 *
 * {@link NotificationService} stays the low-level broker gateway; this class decides
 * what a person should be told and remembers that it told them.
 */
@Service
public class UserNotificationService {

    private static final int PREVIEW_LENGTH = 140;

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public UserNotificationService(NotificationRepository notificationRepository,
                                   NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public NotificationResponse notify(User recipient, NotificationType type, String title, String body, String link) {
        return push(recipient, notificationRepository.save(
                new Notification(recipient, type, title, preview(body), link)));
    }

    /**
     * Like {@link #notify}, but if an unread notification with the same key already exists
     * it is refreshed instead of duplicated - a burst of chat messages is one bell entry.
     * Refreshing also restarts the email clock, so the digest waits for the burst to end.
     */
    @Transactional
    public NotificationResponse notifyCollapsing(User recipient, NotificationType type, String title,
                                                 String body, String link, String collapseKey) {
        Notification notification = notificationRepository
                .findFirstByRecipientIdAndCollapseKeyAndReadFalseOrderByCreatedAtDesc(recipient.getId(), collapseKey)
                .orElseGet(() -> new Notification(recipient, type, title, null, link));
        notification.setTitle(title);
        notification.setBody(preview(body));
        notification.setLink(link);
        notification.setCollapseKey(collapseKey);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setEmailedAt(null);
        return push(recipient, notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, Pageable pageable) {
        return PageResponse.of(notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, User actor) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(actor.getId()))
                // Someone else's notification is reported as missing, not forbidden,
                // so ids cannot be probed to learn that they exist.
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(User actor) {
        return notificationRepository.markAllRead(actor.getId());
    }

    private NotificationResponse push(User recipient, Notification saved) {
        NotificationResponse response = NotificationResponse.from(saved);
        notificationService.notification(recipient.getEmail(), response);
        return response;
    }

    private static String preview(String text) {
        if (text == null) {
            return null;
        }
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH - 1) + "…";
    }
}
