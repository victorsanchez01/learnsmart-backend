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
    private final com.learnsmart.assessment.client.PlanningClient planningClient;
    private final com.learnsmart.assessment.client.AiClient aiClient;
    private final com.learnsmart.assessment.client.ContentClient contentClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
            // Adaptivity Loop: Notify Planning Service
            if (session.getPlanId() != null) {
                try {
                    planningClient.replan(session.getPlanId(), "Assessment Completed",
                            "{\"score\": \"calculated_score_placeholder\"}");
                } catch (Exception e) {
                    System.err.println("Failed to notify Planning Service: " + e.getMessage());
                    // Non-blocking for MVP
                }
            }
        }
        return sessionRepository.save(session);
    }

    @Override
    public AssessmentItem getNextItem(UUID sessionId) {
        AssessmentSession session = getSession(sessionId);

        if (session.getDomainId() == null) {
            throw new IllegalStateException(
                    "Session " + sessionId + " has no domainId — cannot select next assessment item");
        }

        // Build mastery state from existing mastery records for this user
        List<java.util.Map<String, Object>> skillState = masteryRepository
                .findByIdUserId(session.getUserId())
                .stream()
                .map(m -> {
                    java.util.Map<String, Object> entry = new java.util.HashMap<>();
                    entry.put("skillId", m.getId().getSkillId().toString());
                    entry.put("mastery", m.getMastery());
                    entry.put("attempts", m.getAttempts());
                    return entry;
                })
                .collect(java.util.stream.Collectors.toList());

        List<java.util.Map<String, Object>> recentHistory = new ArrayList<>();

        // Resolve domainId → human-readable name for the AI prompt (fail-fast: no
        // fallback)
        String domainName = contentClient.getDomain(session.getDomainId()).getName();

        com.learnsmart.assessment.dto.AiDtos.NextItemRequest request = com.learnsmart.assessment.dto.AiDtos.NextItemRequest
                .builder()
                .userId(session.getUserId().toString())
                .domain(domainName)
                .skillState(skillState)
                .recentHistory(recentHistory)
                .excludeItemIds(session.getPresentedItemIds())
                .build();

        // Call AI Service — no fallback; the AI must provide the item
        com.learnsmart.assessment.dto.AiDtos.NextItemResponse response = aiClient.getNextItem(request);

        if (response == null || response.getItem() == null) {
            throw new RuntimeException("AI service returned no item for session " + sessionId);
        }

        java.util.Map<String, Object> aiItem = response.getItem();

        // Try to reuse an existing item if the AI returned a known UUID
        String returnedId = (String) aiItem.get("id");
        if (returnedId != null) {
            try {
                UUID existingId = UUID.fromString(returnedId);
                java.util.Optional<AssessmentItem> existing = itemRepository.findById(existingId);
                if (existing.isPresent()) {
                    trackPresentedItem(session, existingId);
                    return existing.get();
                }
            } catch (IllegalArgumentException ignored) {
                // ID is not a valid UUID — fall through to create a new item
            }
        }

        // Persist AI-generated item so it can be referenced in responses
        AssessmentItem generated = new AssessmentItem();
        generated.setDomainId(session.getDomainId());
        generated.setOrigin("ai");
        generated.setType(aiItem.getOrDefault("type", "multiple_choice").toString());
        generated.setStem(aiItem.getOrDefault("stem", "").toString());

        Object difficultyRaw = aiItem.get("difficulty");
        if (difficultyRaw != null) {
            try {
                generated.setDifficulty(new java.math.BigDecimal(difficultyRaw.toString()));
            } catch (NumberFormatException ignored) {
                generated.setDifficulty(new java.math.BigDecimal("0.5"));
            }
        } else {
            generated.setDifficulty(new java.math.BigDecimal("0.5"));
        }
        generated.setIsActive(true);

        // Persist options from AI response
        Object optionsRaw = aiItem.get("options");
        if (optionsRaw instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> optionsList = (java.util.List<java.util.Map<String, Object>>) optionsRaw;
            java.util.List<AssessmentItemOption> options = new ArrayList<>();
            for (java.util.Map<String, Object> opt : optionsList) {
                AssessmentItemOption option = new AssessmentItemOption();
                option.setAssessmentItem(generated);
                option.setStatement(opt.getOrDefault("statement", "").toString());
                option.setIsCorrect(Boolean.TRUE.equals(opt.get("isCorrect")));
                Object feedback = opt.get("feedbackTemplate");
                if (feedback != null) {
                    option.setFeedbackTemplate(feedback.toString());
                }
                options.add(option);
            }
            generated.setOptions(options);
        }

        AssessmentItem saved = itemRepository.save(generated);
        trackPresentedItem(session, saved.getId());
        return saved;
    }

    private void trackPresentedItem(AssessmentSession session, UUID itemId) {
        if (!session.getPresentedItemIds().contains(itemId)) {
            session.getPresentedItemIds().add(itemId);
            sessionRepository.save(session);
        }
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

        // AI Feedback (US-084)
        if (!isCorrect) {
            try {
                // Prepare request
                java.util.Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("id", item.getId().toString());
                itemMap.put("stem",
                        item.getDomainId() != null ? "Question for domain " + item.getDomainId() : "Question");
                // Avoid full serialization to prevent loops
                // java.util.Map<String, Object> itemMap = objectMapper.convertValue(item,
                // java.util.Map.class);
                java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
                responseMap.put("selectedOptionId", request.getSelectedOptionId());
                responseMap.put("openAnswer", request.getResponsePayload());

                com.learnsmart.assessment.dto.AiDtos.FeedbackRequest feedbackReq = com.learnsmart.assessment.dto.AiDtos.FeedbackRequest
                        .builder()
                        .userId(session.getUserId().toString())
                        .item(itemMap)
                        .userResponse(responseMap)
                        .build();

                com.learnsmart.assessment.dto.AiDtos.FeedbackResponse aiFeedback = aiClient.getFeedback(feedbackReq);
                if (aiFeedback != null && aiFeedback.getFeedbackMessage() != null) {
                    feedback = aiFeedback.getFeedbackMessage();
                }
            } catch (Exception e) {
                System.err.println("AI Feedback failed: " + e.getMessage());
            }
        }

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
