package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/** Ticks off one checklist step for one user. */
@Entity
@Table(name = "checklist_progress", uniqueConstraints = @UniqueConstraint(
        name = "uk_progress_user_item", columnNames = {"user_id", "item_id"}))
public class ChecklistProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ChecklistItem item;

    @Column(nullable = false)
    private boolean done = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public ChecklistProgress() {
    }

    public ChecklistProgress(User user, ChecklistItem item) {
        this.user = user;
        this.item = item;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ChecklistItem getItem() { return item; }
    public void setItem(ChecklistItem item) { this.item = item; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecklistProgress other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
