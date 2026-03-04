package com.learnsmart.planning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "plan_replans_history")
@Getter
@Setter
@ToString(exclude = "plan")
@EqualsAndHashCode(exclude = "plan")
@NoArgsConstructor
@AllArgsConstructor
public class PlanReplanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private LearningPlan plan;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "request_payload", columnDefinition = "TEXT") // JSON mapped as TEXT for compatibility
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT") // JSON mapped as TEXT for compatibility
    private String responsePayload;

    @Column(name = "trigger_id")
    private UUID triggerId; // US-107: Link to ReplanTrigger if auto-triggered

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
