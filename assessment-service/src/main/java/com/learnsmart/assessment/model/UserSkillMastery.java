package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_skill_mastery")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillMastery {

    @EmbeddedId
    private UserSkillMasteryId id;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal mastery;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_update", nullable = false)
    private OffsetDateTime lastUpdate;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSkillMasteryId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "skill_id")
        private UUID skillId;
    }

    @PrePersist
    public void prePersist() {
        if (lastUpdate == null)
            lastUpdate = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdate = OffsetDateTime.now();
    }
}
