package com.accommodationfinder.service;

import com.accommodationfinder.model.Notification;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emails people about notifications they have not seen.
 *
 * The rule is "unread after {@code app.notifications.email-delay}". Someone with the site
 * open reads the bell within seconds and is never emailed; a landlord who closed the tab
 * yesterday gets one digest per run, not one email per booking. This is what makes
 * bookings work for people who are not watching the screen.
 */
@Component
public class NotificationEmailJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailJob.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final Duration delay;

    public NotificationEmailJob(NotificationRepository notificationRepository,
                                EmailService emailService,
                                @Value("${app.notifications.email-delay:10m}") Duration delay) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.delay = delay;
    }

    @Scheduled(fixedDelayString = "${app.notifications.email-check-interval:60000}",
               initialDelayString = "${app.notifications.email-check-interval:60000}")
    @Transactional
    public int run() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> due = notificationRepository.findAwaitingEmail(now.minus(delay));
        if (due.isEmpty()) {
            return 0;
        }

        Map<User, List<Notification>> byRecipient = new LinkedHashMap<>();
        for (Notification n : due) {
            byRecipient.computeIfAbsent(n.getRecipient(), u -> new ArrayList<>()).add(n);
        }

        int sent = 0;
        for (Map.Entry<User, List<Notification>> entry : byRecipient.entrySet()) {
            User recipient = entry.getKey();
            // Only verified addresses: emailing an address nobody has confirmed is how a
            // platform ends up spamming strangers who were signed up by someone else.
            if (recipient.isEnabled() && recipient.isEmailVerified() && recipient.isEmailNotifications()) {
                emailService.sendNotificationDigest(recipient, entry.getValue());
                sent++;
            }
            // Mark skipped rows too, or they would be reconsidered on every run forever.
            entry.getValue().forEach(n -> n.setEmailedAt(now));
        }
        log.info("Notification digest: {} email(s) for {} notification(s)", sent, due.size());
        return sent;
    }
}
