package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A persistent record of something that happened to a user.
 *
 * The WebSocket push only reaches people who have the site open. This row is what a
 * landlord sees in the bell when they come back tomorrow, and what the email job reads
 * to tell them about it if they never come back at all.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, read_flag"),
        @Index(name = "idx_notifications_email_queue", columnList = "read_flag, emailed_at, created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    /** Path inside the frontend to open when the notification is clicked, e.g. /bookings. */
    @Column(length = 255)
    private String link;

    /**
     * Groups notifications that should collapse into one, e.g. "message:room:sender".
     * Ten chat messages from the same person become one unread row, not ten.
     */
    @Column(name = "collapse_key", length = 100)
    private String collapseKey;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    /** Set once the email job has dealt with this row - sent, or deliberately skipped. */
    @Column(name = "emailed_at")
    private LocalDateTime emailedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Notification() {
    }

    public Notification(User recipient, NotificationType type, String title, String body, String link) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getCollapseKey() { return collapseKey; }
    public void setCollapseKey(String collapseKey) { this.collapseKey = collapseKey; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getEmailedAt() { return emailedAt; }
    public void setEmailedAt(LocalDateTime emailedAt) { this.emailedAt = emailedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
