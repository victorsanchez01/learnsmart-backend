package com.learnsmart.planning.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public class ExternalDtos {

    @Data
    public static class UserProfile {
        private UUID id;
        private String userId; // Keycloak ID
        private String email;
        private String displayName;
        private String locale;
        private Integer age;
        private List<String> interests;
        private String learningStyle;
    }

    @Data
    public static class ContentItemDto {
        private String id; // UUID as String for AI
        private String title;
        private String description;
        private String type;
        private Object domain;
    }

    // AI Service DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratePlanRequest {
        private String userId;
        private Map<String, Object> profile;
        private List<Map<String, Object>> goals;
        private List<Map<String, Object>> contentCatalog;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratePlanResponse {
        private PlanDraft plan;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanDraft {
        private String planId;
        private List<ModuleDraft> modules;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleDraft {
        private String title;
        private String description;
        private List<ActivityDraft> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDraft {
        private String type; // lesson, practice
        private String contentRef; // ID from catalog
    }
}
