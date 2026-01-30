package com.learnsmart.assessment.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextItemResponse {
    private boolean done;
    private AssessmentItem item;
    private AssessmentSession session;
}
