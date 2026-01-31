package com.learnsmart.content.dto;

import lombok.Data;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ContentDtos {

    @Data
    public static class DomainInput {
        private String code;
        private String name;
        private String description;
    }

    @Data
    public static class SkillInput {
        private UUID domainId;
        private String code;
        private String name;
        private String description;
        private String level;
        private List<String> tags;
    }

    @Data
    public static class ContentItemInput {
        private UUID domainId;
        private String type;
        private String title;
        private String description;
        private Integer estimatedMinutes;
        private BigDecimal difficulty;
        private Map<String, Object> metadata;
        private boolean isActive;
    }

    @Data
    public static class GenerateContentInput {
        private UUID domainId;
        private String topic;
        private int nLessons;
    }

    @Data
    public static class ContentItemResponse {
        private UUID id;
        private DomainInput domain;
        private String type;
        private String title;
        private String description;
        private Integer estimatedMinutes;
        private BigDecimal difficulty;
        private Map<String, Object> metadata;
        private boolean isActive;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    public static class ContentItemSkillInput {
        private UUID skillId;
        private Double weight;
    }
}
