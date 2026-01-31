package com.learnsmart.assessment.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

public class AssessmentDtos {

    @Data
    public static class AssessmentItemInput {
        private UUID domainId;
        private String type;
        private String stem;
        private Double difficulty;
        private String metadata;
        private List<OptionInput> options;
        private List<SkillInput> skills;
    }

    @Data
    public static class OptionInput {
        private String text;
        private boolean isCorrect;
        private String feedback;
    }

    @Data
    public static class SkillInput {
        private UUID skillId;
        private Double weight;
    }
}
