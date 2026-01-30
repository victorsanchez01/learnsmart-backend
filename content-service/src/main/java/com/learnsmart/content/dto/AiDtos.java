package com.learnsmart.content.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

public class AiDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateLessonsRequest {
        private String domain;
        private List<String> skillIds;
        private int nLessons;
        private String level;
        private Double difficulty;
        private String locale;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateLessonsResponse {
        private List<ContentLessonDraft> lessons;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentLessonDraft {
        private String tempId;
        private String title;
        private String description;
        private String body;
        private Integer estimatedMinutes;
        private Double difficulty;
        private String type; // lesson, practice
        private Map<String, Object> metadata;
    }
}
