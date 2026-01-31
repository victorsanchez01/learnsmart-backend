package com.learnsmart.planning.dto;

import lombok.Data;
import java.util.UUID;
import java.util.List;

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
    }
}
