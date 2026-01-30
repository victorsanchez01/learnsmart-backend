package com.learnsmart.content.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "content_item_skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentItemSkill {

    @EmbeddedId
    private ContentItemSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contentItemId")
    @JoinColumn(name = "content_item_id")
    private ContentItem contentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItemSkillId implements Serializable {
        private UUID contentItemId;
        private UUID skillId;
    }
}
