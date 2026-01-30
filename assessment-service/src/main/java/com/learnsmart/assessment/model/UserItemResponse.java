package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_item_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserItemResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assessment_item_id", nullable = false)
    private UUID assessmentItemId;

    @Column(name = "selected_option_id")
    private UUID selectedOptionId;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
    }
}
