package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.*;
import com.learnsmart.assessment.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssessmentSessionServiceImpl.
 *
 * Mastery delta constants match the implementation:
 * MASTERY_CORRECT_DELTA = +0.1
 * MASTERY_INCORRECT_DELTA= -0.05
 * MASTERY_INITIAL = 0.3 (default when no record exists)
 */
@ExtendWith(MockitoExtension.class)
class AssessmentSessionServiceImplTest {

    private static final BigDecimal MASTERY_INITIAL = new BigDecimal("0.3");
    private static final BigDecimal MASTERY_CORRECT_DELTA = new BigDecimal("0.1");
    private static final BigDecimal MASTERY_INCORRECT_DELTA = new BigDecimal("-0.05");

    @Mock
    private AssessmentSessionRepository sessionRepository;
    @Mock
    private AssessmentItemRepository itemRepository;
    @Mock
    private UserItemResponseRepository responseRepository;
    @Mock
    private UserSkillMasteryRepository masteryRepository;
    @Mock
    private com.learnsmart.assessment.client.PlanningClient planningClient;
    @Mock
    private com.learnsmart.assessment.client.AiClient aiClient;
    @Mock
    private com.learnsmart.assessment.client.ContentClient contentClient;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AssessmentSessionServiceImpl sessionService;

    // -------------------------------------------------------------------------
    // createSession
    // -------------------------------------------------------------------------

    @Test
    void testCreateSession_SetsStartedAtAndStatus() {
        // The service sets startedAt & status BEFORE calling save; ArgumentCaptor lets
        // us assert what was actually persisted.
        ArgumentCaptor<AssessmentSession> captor = ArgumentCaptor.forClass(AssessmentSession.class);
        AssessmentSession session = new AssessmentSession();
        when(sessionRepository.save(captor.capture())).thenReturn(session);

        AssessmentSession result = sessionService.createSession(session);

        assertNotNull(result);
        AssessmentSession saved = captor.getValue();
        assertNotNull(saved.getStartedAt(), "startedAt must be set before save");
        assertEquals("in_progress", saved.getStatus(), "status must be 'in_progress' before save");
        verify(sessionRepository).save(any(AssessmentSession.class));
    }

    // -------------------------------------------------------------------------
    // getSession
    // -------------------------------------------------------------------------

    @Test
    void testGetSession_Found() {
        UUID id = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        AssessmentSession result = sessionService.getSession(id);
        assertEquals(id, result.getId());
    }

    @Test
    void testGetSession_NotFound() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sessionService.getSession(id));
    }

    // -------------------------------------------------------------------------
    // updateStatus
    // -------------------------------------------------------------------------

    @Test
    void testUpdateStatus_Completed_SetsCompletedAt() {
        UUID id = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));

        AssessmentSession result = sessionService.updateStatus(id, "completed");

        assertEquals("completed", result.getStatus());
        assertNotNull(result.getCompletedAt(), "completedAt must be set when status is 'completed'");
    }

    @Test
    void testUpdateStatus_InProgress_DoesNotSetCompletedAt() {
        UUID id = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        session.setId(id);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));

        AssessmentSession result = sessionService.updateStatus(id, "in_progress");

        assertEquals("in_progress", result.getStatus());
        assertNull(result.getCompletedAt(), "completedAt must remain null for non-completed status");
    }

    // -------------------------------------------------------------------------
    // getNextItem
    // -------------------------------------------------------------------------

    @Test
    void testGetNextItem_AiFails_ThrowsException() {
        UUID sessionId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        session.setDomainId(domainId);
        session.setPresentedItemIds(new java.util.ArrayList<>());

        com.learnsmart.assessment.dto.MasteryDtos.DomainInfo domainInfo = new com.learnsmart.assessment.dto.MasteryDtos.DomainInfo(
                domainId, "Programming Fundamentals");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(masteryRepository.findByIdUserId(any())).thenReturn(Collections.emptyList());
        when(contentClient.getDomain(domainId)).thenReturn(domainInfo);
        when(aiClient.getNextItem(any())).thenThrow(new RuntimeException("AI unavailable"));

        // No fallback: AI failure must propagate
        assertThrows(RuntimeException.class, () -> sessionService.getNextItem(sessionId));
    }

    @Test
    void testGetNextItem_NoDomainId_Throws() {
        UUID sessionId = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        // domainId intentionally NOT set
        session.setPresentedItemIds(new java.util.ArrayList<>());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class, () -> sessionService.getNextItem(sessionId),
                "Session without domainId must throw IllegalStateException");
    }

    // -------------------------------------------------------------------------
    // submitResponse
    // -------------------------------------------------------------------------

    @Test
    void testSubmitResponse_Correct_UpdatesMastery() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(userId);

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(optionId);
        option.setIsCorrect(true);
        item.setOptions(List.of(option));

        AssessmentItemSkill itemSkill = new AssessmentItemSkill();
        AssessmentItemSkill.AssessmentItemSkillId itemSkillId = new AssessmentItemSkill.AssessmentItemSkillId();
        itemSkillId.setSkillId(skillId);
        itemSkill.setId(itemSkillId);
        item.setSkills(List.of(itemSkill));

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(optionId);
        request.setResponseTimeMs(1000);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });
        when(masteryRepository.findById(any(UserSkillMastery.UserSkillMasteryId.class))).thenReturn(Optional.empty());
        when(masteryRepository.save(any(UserSkillMastery.class))).thenAnswer(i -> i.getArgument(0));

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        assertTrue(result.getIsCorrect());
        assertEquals("Correct!", result.getFeedback());
        assertEquals(1, result.getMasteryUpdates().size());
        UserSkillMastery update = result.getMasteryUpdates().get(0);
        assertEquals(1, update.getAttempts());
        // Expected: MASTERY_INITIAL (0.3) + MASTERY_CORRECT_DELTA (0.1) = 0.4
        BigDecimal expected = MASTERY_INITIAL.add(MASTERY_CORRECT_DELTA);
        assertEquals(0, expected.compareTo(update.getMastery()),
                "Mastery should be " + expected + " after correct answer");
    }

    @Test
    void testSubmitResponse_Incorrect_UsesFeedbackTemplate() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(userId);

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(optionId);
        option.setIsCorrect(false);
        option.setFeedbackTemplate("Wrong answer");
        item.setOptions(List.of(option));
        item.setSkills(Collections.emptyList());

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(optionId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        assertFalse(result.getIsCorrect());
        // Initial feedback comes from feedbackTemplate; AI feedback may override it if
        // available, but AI client is not mocked here, so it falls back to template.
        assertEquals("Wrong answer", result.getFeedback());
    }

    @Test
    void testSubmitResponse_OptionNotInItem_Throws() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID wrongOptId = UUID.randomUUID(); // ID not present in item options

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(UUID.randomUUID()); // Different from wrongOptId
        option.setIsCorrect(true);
        item.setOptions(List.of(option));

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(wrongOptId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(RuntimeException.class,
                () -> sessionService.submitResponse(sessionId, request),
                "Should throw when selectedOptionId is not found in item options");
    }

    @Test
    void testSubmitResponse_OpenText_RecordedAsPending() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        item.setOptions(Collections.emptyList());
        item.setSkills(Collections.emptyList());

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(null); // Open text response
        request.setResponsePayload("My open answer");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        assertTrue(result.getIsCorrect(), "Open-text MVP marks as correct pending review");
        assertEquals("Response recorded. Pending review.", result.getFeedback());
    }

    @Test
    void testSubmitResponse_MasteryMaxCap() {
        // If mastery would exceed 1.0, it must be capped at 1.0
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(userId);

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(optionId);
        option.setIsCorrect(true);
        item.setOptions(List.of(option));

        AssessmentItemSkill itemSkill = new AssessmentItemSkill();
        AssessmentItemSkill.AssessmentItemSkillId skId = new AssessmentItemSkill.AssessmentItemSkillId();
        skId.setSkillId(skillId);
        itemSkill.setId(skId);
        item.setSkills(List.of(itemSkill));

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(optionId);

        // Simulate mastery already at 0.95 (adding 0.1 would overflow)
        UserSkillMastery.UserSkillMasteryId masteryId = new UserSkillMastery.UserSkillMasteryId(userId, skillId);
        UserSkillMastery existingMastery = new UserSkillMastery(masteryId, new BigDecimal("0.95"), 5,
                OffsetDateTime.now());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });
        when(masteryRepository.findById(any(UserSkillMastery.UserSkillMasteryId.class)))
                .thenReturn(Optional.of(existingMastery));
        when(masteryRepository.save(any(UserSkillMastery.class))).thenAnswer(i -> i.getArgument(0));

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        UserSkillMastery update = result.getMasteryUpdates().get(0);
        // Must be capped at 1.0
        assertEquals(0, BigDecimal.ONE.compareTo(update.getMastery()),
                "Mastery must be capped at 1.0");
    }

    // -------------------------------------------------------------------------
    // getSessionResponses / getUserSkillMastery
    // -------------------------------------------------------------------------

    @Test
    void testGetSessionResponses() {
        UUID sessionId = UUID.randomUUID();
        when(responseRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(Collections.emptyList());

        List<UserItemResponse> result = sessionService.getSessionResponses(sessionId);
        assertTrue(result.isEmpty());
        verify(responseRepository).findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Test
    void testGetUserSkillMastery() {
        UUID userId = UUID.randomUUID();
        when(masteryRepository.findByIdUserId(userId)).thenReturn(Collections.emptyList());

        List<UserSkillMastery> result = sessionService.getUserSkillMastery(userId);
        assertTrue(result.isEmpty());
        verify(masteryRepository).findByIdUserId(userId);
    }

    // -------------------------------------------------------------------------
    // updateStatus — completed + planId present → calls planningClient.replan
    // -------------------------------------------------------------------------

    @Test
    void testUpdateStatus_Completed_WithPlanId_CallsReplan() {
        UUID id = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(id);
        session.setPlanId(planId);

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));
        when(planningClient.replan(any(), any(), any())).thenReturn(null);

        AssessmentSession result = sessionService.updateStatus(id, "completed");

        assertEquals("completed", result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(planningClient).replan(eq(planId), any(), any());
    }

    // -------------------------------------------------------------------------
    // updateStatus — planningClient fails → non-blocking (no exception propagated)
    // -------------------------------------------------------------------------

    @Test
    void testUpdateStatus_Completed_ReplanFails_NonBlocking() {
        UUID id = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(id);
        session.setPlanId(planId);

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));
        when(planningClient.replan(any(), any(), any())).thenThrow(new RuntimeException("Planning unavailable"));

        // Must NOT propagate the exception
        assertDoesNotThrow(() -> sessionService.updateStatus(id, "completed"));
    }

    // -------------------------------------------------------------------------
    // getNextItem — AI returns valid item with known UUID → item fetched from repo
    // -------------------------------------------------------------------------

    @Test
    void testGetNextItem_AiReturnsKnownItemId_ReturnsItem() {
        UUID sessionId = UUID.randomUUID();
        UUID knownItemId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        session.setDomainId(domainId); // required since fail-fast guard
        session.setPresentedItemIds(new java.util.ArrayList<>());

        AssessmentItem expectedItem = new AssessmentItem();
        expectedItem.setId(knownItemId);

        // AI response with a valid item UUID
        java.util.Map<String, Object> itemMap = new java.util.HashMap<>();
        itemMap.put("id", knownItemId.toString());
        com.learnsmart.assessment.dto.AiDtos.NextItemResponse aiResponse = new com.learnsmart.assessment.dto.AiDtos.NextItemResponse(
                itemMap, null);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(masteryRepository.findByIdUserId(any())).thenReturn(Collections.emptyList());
        when(contentClient.getDomain(domainId)).thenReturn(
                new com.learnsmart.assessment.dto.MasteryDtos.DomainInfo(domainId, "Programming Fundamentals"));
        when(aiClient.getNextItem(any())).thenReturn(aiResponse);
        when(itemRepository.findById(knownItemId)).thenReturn(Optional.of(expectedItem));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));

        AssessmentItem result = sessionService.getNextItem(sessionId);

        assertEquals(knownItemId, result.getId());
        // The item was recorded as presented
        assertTrue(session.getPresentedItemIds().contains(knownItemId));
    }

    // -------------------------------------------------------------------------
    // submitResponse — incorrect answer + AI feedback overrides the template
    // -------------------------------------------------------------------------

    @Test
    void testSubmitResponse_Incorrect_AiOverridesFeedback() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(userId);

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(optionId);
        option.setIsCorrect(false);
        option.setFeedbackTemplate("Generic wrong");
        item.setOptions(List.of(option));
        item.setSkills(Collections.emptyList());

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(optionId);

        com.learnsmart.assessment.dto.AiDtos.FeedbackResponse aiFeedback = new com.learnsmart.assessment.dto.AiDtos.FeedbackResponse(
                null, "AI: Think again about the concept.", null);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(java.time.OffsetDateTime.now());
            return r;
        });
        when(aiClient.getFeedback(any())).thenReturn(aiFeedback);

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        assertFalse(result.getIsCorrect());
        assertEquals("AI: Think again about the concept.", result.getFeedback());
    }

    // -------------------------------------------------------------------------
    // submitResponse — mastery floor cap (below 0 → stays at 0)
    // -------------------------------------------------------------------------

    @Test
    void testSubmitResponse_MasteryFloorCap() {
        UUID sessionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setUserId(userId);

        AssessmentItem item = new AssessmentItem();
        item.setId(itemId);
        AssessmentItemOption option = new AssessmentItemOption();
        option.setId(optionId);
        option.setIsCorrect(false);
        option.setFeedbackTemplate("Incorrect");
        item.setOptions(List.of(option));

        AssessmentItemSkill itemSkill = new AssessmentItemSkill();
        AssessmentItemSkill.AssessmentItemSkillId skId = new AssessmentItemSkill.AssessmentItemSkillId();
        skId.setSkillId(skillId);
        itemSkill.setId(skId);
        item.setSkills(List.of(itemSkill));

        SubmitResponseRequest request = new SubmitResponseRequest();
        request.setAssessmentItemId(itemId);
        request.setSelectedOptionId(optionId);

        // Mastery already at 0.01 → subtracting 0.05 would go below 0
        UserSkillMastery.UserSkillMasteryId masteryId = new UserSkillMastery.UserSkillMasteryId(userId, skillId);
        UserSkillMastery existingMastery = new UserSkillMastery(masteryId, new BigDecimal("0.01"), 3,
                java.time.OffsetDateTime.now());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(responseRepository.save(any(UserItemResponse.class))).thenAnswer(i -> {
            UserItemResponse r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(java.time.OffsetDateTime.now());
            return r;
        });
        when(masteryRepository.findById(any(UserSkillMastery.UserSkillMasteryId.class)))
                .thenReturn(Optional.of(existingMastery));
        when(masteryRepository.save(any(UserSkillMastery.class))).thenAnswer(i -> i.getArgument(0));

        UserItemResponseWithFeedback result = sessionService.submitResponse(sessionId, request);

        UserSkillMastery update = result.getMasteryUpdates().get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(update.getMastery()),
                "Mastery must be floored at 0.0");
    }
}
