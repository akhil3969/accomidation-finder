package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/** A user flagging a listing or another user for a moderator to look at. */
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_status", columnList = "status"),
        @Index(name = "idx_reports_room", columnList = "room_id")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    /** Either a room or a user is set, depending on what is being reported. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "moderator_note", columnDefinition = "TEXT")
    private String moderatorNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    private User handledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getReportedUser() { return reportedUser; }
    public void setReportedUser(User reportedUser) { this.reportedUser = reportedUser; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getModeratorNote() { return moderatorNote; }
    public void setModeratorNote(String moderatorNote) { this.moderatorNote = moderatorNote; }

    public User getHandledBy() { return handledBy; }
    public void setHandledBy(User handledBy) { this.handledBy = handledBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
