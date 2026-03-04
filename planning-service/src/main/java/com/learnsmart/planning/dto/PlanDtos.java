package com.learnsmart.planning.dto;

import lombok.Data;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PlanDtos {

    @Data
    public static class UpdateModuleRequest {
        private String status;
    }

    @Data
    public static class UpdateActivityRequest {
        private String status;
        private Integer overrideEstimatedMinutes;
    }

    @Data
    public static class ModuleResponse {
        private UUID id;
        private UUID planId;
        private Integer position;
        private String title;
        private String description;
        private String status;
        private List<String> targetSkills;
    }

    @Data
    public static class ActivityResponse {
        private UUID id;
        private UUID moduleId;
        private Integer position;
        private String activityType;
        private String status;
        private String contentRef;
        private Integer estimatedMinutes;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private Integer actualMinutesSpent;
    }

    @Data
    public static class CreateActivityRequest {
        private Integer position;
        private String activityType;
        private String contentRef;
        private Integer estimatedMinutes;
    }

    @Data
    public static class ReplanTriggerResponse {
        private UUID id;
        private UUID planId;
        private String triggerType;
        private String triggerReason;
        private String severity;
        private OffsetDateTime detectedAt;
        private String status;
        private String metadata;
    }

    /**
     * Flat, serialization-safe view of LearningPlan returned from the replan
     * endpoint.
     * Avoids the circular Jackson serialization caused by LearningPlan.replans →
     * plan → replans → …
     */
    @Data
    public static class PlanSummaryResponse {
        private UUID id;
        private String userId;
        private String goalId;
        private String status;
        private String generatedBy;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal hoursPerWeek;
        private String rawPlanAi;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private List<ModuleSummary> modules;

        @Data
        public static class ModuleSummary {
            private UUID id;
            private Integer position;
            private String title;
            private String status;
            private BigDecimal estimatedHours;
            private List<String> targetSkills;
        }
    }
}
