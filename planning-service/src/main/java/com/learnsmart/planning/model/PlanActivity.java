package com.learnsmart.planning.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "plan_activities", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "module_id", "position" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private PlanModule module;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "content_ref", nullable = false, columnDefinition = "TEXT")
    private String contentRef;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "override_estimated_minutes")
    private Integer overrideEstimatedMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null)
            createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
