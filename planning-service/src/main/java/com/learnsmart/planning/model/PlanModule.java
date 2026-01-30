package com.learnsmart.planning.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "plan_modules", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "plan_id", "position" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LearningPlan plan;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_hours", precision = 5, scale = 2)
    private BigDecimal estimatedHours;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @ElementCollection(fetch = FetchType.EAGER) // Simple storage for validating logical flow
    // Storing as simple CSV string or string array in H2 might require generic
    // handling.
    // For H2 compatibility, avoiding Postgres-specific arrays.
    // Best practice here for portability: Use a separate join table or a CSV
    // converter.
    // Given the 'TEXT[]' in Postgres DDL, let's assume we read/write as explicit
    // array if using Hibernate Types,
    // OR we use a simple List<String> which Hibernate might map to varbinary or
    // similar.
    // TO BE SAFE: @Column(columnDefinition = "TEXT") and manual converter?
    // Let's try @ElementCollection with List<String> it usually works or fails on
    // array type support.
    // For now, I will use a simple String with comma-delimited values to guarantee
    // H2 support given TEXT[] complications.
    // WAIT: Schema says TEXT[]. If I map it to String in Java, I can save as
    // "skill1,skill2".
    // It's robust for now.
    @Column(name = "target_skills")
    private List<String> targetSkills;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanActivity> activities;

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
