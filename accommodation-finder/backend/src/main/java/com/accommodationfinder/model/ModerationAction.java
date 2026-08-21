package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/** An immutable record of every moderator decision, so admin actions are accountable. */
@Entity
@Table(name = "moderation_actions", indexes = {
        @Index(name = "idx_moderation_created", columnList = "created_at")
})
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private User moderator;

    /** e.g. HIDE_LISTING, VERIFY_LANDLORD, BAN_USER. */
    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ModerationAction() {
    }

    public ModerationAction(User moderator, String action, String targetType, Long targetId, String reason) {
        this.moderator = moderator;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getModerator() { return moderator; }
    public void setModerator(User moderator) { this.moderator = moderator; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationAction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
