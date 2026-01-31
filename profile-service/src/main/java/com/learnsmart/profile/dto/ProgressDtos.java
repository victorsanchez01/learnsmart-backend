package com.learnsmart.profile.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;

public class ProgressDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProgressResponse {
        private ProfileInfo profile;
        private List<GoalProgress> goals;
        private PlanProgress currentPlan;
        private List<SkillMasteryShort> skillsInProgress;
        private ActivitySummary activity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileInfo {
        private String userId;
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProgress {
        private UUID goalId;
        private String title;
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanProgress {
        private UUID planId;
        private String goalId;
        private String status;
        private Integer completedModules;
        private Integer totalModules;
        private Double overallPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMasteryShort {
        private UUID skillId;
        private String skillName;
        private Double mastery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivitySummary {
        private Double totalHours;
        private Integer currentStreak;
    }
}
