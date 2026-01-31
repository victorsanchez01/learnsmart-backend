package com.learnsmart.assessment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.OffsetDateTime;

public class MasteryDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMasteryEnriched {
        private UUID skillId;
        private String skillName;
        private String domainName;
        private Double mastery;
        private Integer attempts;
        private OffsetDateTime lastUpdate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillInfo {
        private UUID id;
        private String name;
        private DomainInfo domain;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainInfo {
        private UUID id;
        private String name;
    }
}
