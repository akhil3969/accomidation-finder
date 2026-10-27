package com.accommodationfinder.service;

import com.accommodationfinder.repository.NotificationRepository;
import com.accommodationfinder.repository.UserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Enforces the retention periods stated in the privacy policy (GDPR art. 5(1)(e):
 * keep personal data no longer than needed). A policy that says "90 days" while the
 * rows sit there forever is worse than no policy.
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final NotificationRepository notificationRepository;
    private final UserTokenRepository tokenRepository;
    private final Duration readNotifications;
    private final Duration expiredTokens;

    public RetentionJob(NotificationRepository notificationRepository,
                        UserTokenRepository tokenRepository,
                        @Value("${app.retention.read-notifications:90d}") Duration readNotifications,
                        @Value("${app.retention.expired-tokens:7d}") Duration expiredTokens) {
        this.notificationRepository = notificationRepository;
        this.tokenRepository = tokenRepository;
        this.readNotifications = readNotifications;
        this.expiredTokens = expiredTokens;
    }

    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}")   // nightly, 03:30
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int notifications = notificationRepository.deleteReadOlderThan(now.minus(readNotifications));
        int tokens = tokenRepository.deleteExpiredBefore(now.minus(expiredTokens));
        log.info("Retention clean-up: {} read notification(s), {} expired token(s) deleted", notifications, tokens);
    }
}
