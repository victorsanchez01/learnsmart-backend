package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.*;
import com.learnsmart.assessment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AssessmentSessionServiceImpl implements AssessmentSessionService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentItemRepository itemRepository;
    private final UserItemResponseRepository responseRepository;
    private final UserSkillMasteryRepository masteryRepository;

    @Override
    @Transactional
    public AssessmentSession createSession(AssessmentSession session) {
        session.setStartedAt(OffsetDateTime.now());
        session.setStatus("in_progress");
        return sessionRepository.save(session);
    }

    @Override
    public AssessmentSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    @Override
    @Transactional
    public AssessmentSession updateStatus(UUID sessionId, String status) {
        AssessmentSession session = getSession(sessionId);
        session.setStatus(status);
        if ("completed".equals(status)) {
            session.setCompletedAt(OffsetDateTime.now());
        }
        return sessionRepository.save(session);
    }

    @Override
    public AssessmentItem getNextItem(UUID sessionId) {
        // Mock Adaptive Logic: Pick random active item
        // In real world: check mastery, pick optimal difficulty items based on IRT
        return itemRepository.findRandomActiveItem()
                .orElseThrow(() -> new RuntimeException("No active assessment items found"));
    }

    @Override
    @Transactional
    public UserItemResponseWithFeedback submitResponse(UUID sessionId, SubmitResponseRequest request) {
        AssessmentSession session = getSession(sessionId);
        AssessmentItem item = itemRepository.findById(request.getAssessmentItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        boolean isCorrect = false;
        String feedback = "";

        // Grading Logic
        if (request.getSelectedOptionId() != null) {
            AssessmentItemOption selected = item.getOptions().stream()
                    .filter(o -> o.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Option not found"));
            isCorrect = selected.getIsCorrect();
            feedback = isCorrect ? "Correct!"
                    : (selected.getFeedbackTemplate() != null ? selected.getFeedbackTemplate() : "Incorrect");
        } else {
            // Open text assumes manual grading or AI -> Mock as correct for MVP
            isCorrect = true;
            feedback = "Response recorded. Pending review.";
        }

        // Save Response
        UserItemResponse response = new UserItemResponse();
        response.setSessionId(sessionId);
        response.setUserId(session.getUserId());
        response.setAssessmentItemId(item.getId());
        response.setSelectedOptionId(request.getSelectedOptionId());
        response.setResponsePayload(request.getResponsePayload());
        response.setIsCorrect(isCorrect);
        response.setResponseTimeMs(request.getResponseTimeMs());

        responseRepository.save(response);

        // Update Mastery (Mock)
        List<UserSkillMastery> masteryUpdates = new ArrayList<>();
        if (item.getSkills() != null) {
            for (AssessmentItemSkill itemSkill : item.getSkills()) {
                UserSkillMastery.UserSkillMasteryId masteryId = new UserSkillMastery.UserSkillMasteryId(
                        session.getUserId(), itemSkill.getId().getSkillId());
                UserSkillMastery mastery = masteryRepository.findById(masteryId)
                        .orElse(new UserSkillMastery(masteryId, new BigDecimal("0.3"), 0, OffsetDateTime.now())); // Default
                                                                                                                  // 0.3

                mastery.setAttempts(mastery.getAttempts() + 1);
                // Simple bump: +0.1 if correct, -0.05 if wrong
                BigDecimal change = isCorrect ? new BigDecimal("0.1") : new BigDecimal("-0.05");
                BigDecimal newMastery = mastery.getMastery().add(change);
                if (newMastery.compareTo(BigDecimal.ONE) > 0)
                    newMastery = BigDecimal.ONE;
                if (newMastery.compareTo(BigDecimal.ZERO) < 0)
                    newMastery = BigDecimal.ZERO;

                mastery.setMastery(newMastery);
                masteryRepository.save(mastery);
                masteryUpdates.add(mastery);
            }
        }

        UserItemResponseWithFeedback res = new UserItemResponseWithFeedback();
        // Manually copy props (or use mapper)
        res.setId(response.getId());
        res.setSessionId(response.getSessionId());
        res.setUserId(response.getUserId());
        res.setAssessmentItemId(response.getAssessmentItemId());
        res.setIsCorrect(response.getIsCorrect());
        res.setCreatedAt(response.getCreatedAt());
        res.setFeedback(feedback);
        res.setMasteryUpdates(masteryUpdates);

        return res;
    }

    @Override
    public List<UserItemResponse> getSessionResponses(UUID sessionId) {
        return responseRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    public List<UserSkillMastery> getUserSkillMastery(UUID userId) {
        return masteryRepository.findByIdUserId(userId);
    }
}
