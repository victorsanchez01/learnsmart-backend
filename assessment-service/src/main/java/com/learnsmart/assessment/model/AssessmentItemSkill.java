package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "assessment_item_skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItemSkill {

    @EmbeddedId
    private AssessmentItemSkillId id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assessmentItemId")
    @JoinColumn(name = "assessment_item_id")
    private AssessmentItem assessmentItem;

    @Column(name = "skill_id", insertable = false, updatable = false)
    private UUID skillId;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentItemSkillId implements Serializable {
        @Column(name = "assessment_item_id")
        private UUID assessmentItemId;

        @Column(name = "skill_id")
        private UUID skillId;
    }
}
