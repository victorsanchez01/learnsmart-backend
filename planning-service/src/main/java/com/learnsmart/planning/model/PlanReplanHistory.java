package com.learnsmart.planning.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "plan_replans_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanReplanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LearningPlan plan;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "request_payload", columnDefinition = "TEXT") // JSON mapped as TEXT for compatibility
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT") // JSON mapped as TEXT for compatibility
    private String responsePayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
