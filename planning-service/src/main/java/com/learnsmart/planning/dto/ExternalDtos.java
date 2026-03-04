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
    public static class DomainDto {
        private UUID id;
        private String name;
        private String code;
        private String description;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDiagnosticTestRequest {
        @Builder.Default
        private String domainId = "00000000-0000-0000-0000-000000000000";
        private String domainName; // Human-readable domain name forwarded to the AI
        @Builder.Default
        private String level = "BEGINNER";
        @Builder.Default
        private int nQuestions = 5;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDiagnosticTestResponse {
        private List<Map<String, Object>> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplanRequest {
        private String userId;
        private String reason; // e.g. "Failed_Assessment" — forwarded to the AI prompt
        private Map<String, Object> currentPlan;
        private List<Map<String, Object>> recentEvents;
        private List<Map<String, Object>> updatedSkillState;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplanResponse {
        private Map<String, Object> plan;
        private String changeSummary;
    }
}
