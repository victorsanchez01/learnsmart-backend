package com.learnsmart.profile.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGoal {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "domain_id")
    private UUID domainId;

    @Column(name = "target_level")
    private String targetLevel;

    @Column(name = "due_date")
    private LocalDate dueDate;

    private String intensity; // minimum, standard, aggressive

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // US-093: Link to specific skill
    @Column(name = "skill_id")
    private UUID skillId;

    // US-096: Completion tracking fields
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Integer completionPercentage = 0;

    @Column(length = 20)
    @Builder.Default
    private String status = "active"; // active, in_progress, completed

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // US-096: Helper methods for completion tracking
    public boolean isCompleted() {
        return "completed".equals(status) || (completionPercentage != null && completionPercentage >= 100);
    }

    public void markCompleted() {
        this.completedAt = OffsetDateTime.now();
        this.completionPercentage = 100;
        this.status = "completed";
    }

    public void updateProgress(int percentage) {
        this.completionPercentage = percentage;

        // Auto-update status based on progress
        if (percentage >= 100) {
            markCompleted();
        } else if (percentage > 0) {
            this.status = "in_progress";
        } else {
            this.status = "active";
        }
    }
}
