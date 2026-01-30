package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "assessment_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "domain_id", nullable = false)
    private UUID domainId;

    @Column(nullable = false, length = 20)
    private String origin = "static";

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stem;

    @Column(precision = 3, scale = 2)
    private BigDecimal difficulty;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSONB in Postgres, TEXT in H2

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "assessmentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentItemSkill> skills;

    @OneToMany(mappedBy = "assessmentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentItemOption> options;

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
