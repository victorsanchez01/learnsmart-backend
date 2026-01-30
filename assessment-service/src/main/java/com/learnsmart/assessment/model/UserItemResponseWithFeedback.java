package com.learnsmart.assessment.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserItemResponseWithFeedback extends UserItemResponse {
    private String feedback;
    private List<UserSkillMastery> masteryUpdates;
}
