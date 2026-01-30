package com.learnsmart.assessment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponseRequest {
    private UUID assessmentItemId;
    private UUID selectedOptionId;
    private String responsePayload; // JSON string payload
    private Integer responseTimeMs;
}
