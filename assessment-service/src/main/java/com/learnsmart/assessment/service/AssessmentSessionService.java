package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.AssessmentSession;
import com.learnsmart.assessment.model.NextItemResponse;
import com.learnsmart.assessment.model.UserItemResponse;
import com.learnsmart.assessment.model.UserItemResponseWithFeedback;
import com.learnsmart.assessment.model.SubmitResponseRequest;
import com.learnsmart.assessment.model.AssessmentItem;
import java.util.UUID;
import java.util.List;

public interface AssessmentSessionService {
    AssessmentSession createSession(AssessmentSession session);

    AssessmentSession getSession(UUID sessionId);

    AssessmentSession updateStatus(UUID sessionId, String status);

    // Core Adaptive Logic
    AssessmentItem getNextItem(UUID sessionId); // Logic to pick next item

    // Response Handling
    UserItemResponseWithFeedback submitResponse(UUID sessionId, SubmitResponseRequest request);

    List<UserItemResponse> getSessionResponses(UUID sessionId);
}
