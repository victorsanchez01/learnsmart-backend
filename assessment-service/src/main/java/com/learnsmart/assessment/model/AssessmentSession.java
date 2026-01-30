package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_assessment_sessions_v2")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String status = "in_progress";

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(columnDefinition = "TEXT")
    private String config;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (startedAt == null)
            startedAt = OffsetDateTime.now();
    }
}
