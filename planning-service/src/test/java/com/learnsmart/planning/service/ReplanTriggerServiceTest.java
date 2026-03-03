package com.learnsmart.planning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.model.PlanActivity;
import com.learnsmart.planning.model.PlanModule;
import com.learnsmart.planning.model.ReplanTrigger;
import com.learnsmart.planning.repository.PlanActivityRepository;
import com.learnsmart.planning.repository.ReplanTriggerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplanTriggerServiceTest {

    @Mock
    private ReplanTriggerRepository triggerRepository;
    @Mock
    private PlanActivityRepository activityRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReplanTriggerService triggerService;

    // -------------------------------------------------------------------------
    // evaluateProgressDeviation
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateProgressDeviation_InactivePlan_ReturnsNull() {
        LearningPlan plan = new LearningPlan();
        plan.setStatus("completed");

        assertNull(triggerService.evaluateProgressDeviation(plan));
        verifyNoInteractions(triggerRepository);
    }

    @Test
    void testEvaluateProgressDeviation_NullStartDate_ReturnsNull() {
        LearningPlan plan = new LearningPlan();
        plan.setStatus("active");
        plan.setStartDate(null);
        plan.setEndDate(LocalDate.now().plusDays(30));

        assertNull(triggerService.evaluateProgressDeviation(plan));
    }

    @Test
    void testEvaluateProgressDeviation_NoActivities_ReturnsNull() {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now().minusDays(10));
        plan.setEndDate(LocalDate.now().plusDays(20));
        plan.setModules(List.of()); // no activities

        assertNull(triggerService.evaluateProgressDeviation(plan));
    }

    @Test
    void testEvaluateProgressDeviation_SmallDeviation_ReturnsNull() {
        LearningPlan plan = activePlanWithActivities(0, 10, 5, 5); // 50% expected, 50% actual

        assertNull(triggerService.evaluateProgressDeviation(plan));
    }

    @Test
    void testEvaluateProgressDeviation_HighDeviation_CreatesTrigger() throws Exception {
        UUID planId = UUID.randomUUID();
        // Plan started 20 days ago, ends in 10 days → ~66% expected but 0% actual →
        // deviation >50
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now().minusDays(20));
        plan.setEndDate(LocalDate.now().plusDays(10));

        PlanActivity activity = new PlanActivity();
        activity.setStatus("pending"); // 0% completed
        PlanModule module = new PlanModule();
        module.setActivities(List.of(activity));
        plan.setModules(List.of(module));

        // No recent trigger
        when(triggerRepository.findTopByPlanIdAndTriggerTypeOrderByDetectedAtDesc(eq(planId), anyString()))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"deviation\":66.7}");

        ReplanTrigger saved = ReplanTrigger.builder()
                .plan(plan).triggerType("PROGRESS_DEVIATION").severity("HIGH").build();
        when(triggerRepository.save(any(ReplanTrigger.class))).thenReturn(saved);

        ReplanTrigger result = triggerService.evaluateProgressDeviation(plan);

        assertNotNull(result);
        assertEquals("HIGH", result.getSeverity());
        verify(triggerRepository).save(any(ReplanTrigger.class));
    }

    @Test
    void testEvaluateProgressDeviation_RecentTriggerExists_ReturnsNull() throws Exception {
        UUID planId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now().minusDays(20));
        plan.setEndDate(LocalDate.now().plusDays(10));

        PlanActivity activity = new PlanActivity();
        activity.setStatus("pending");
        PlanModule module = new PlanModule();
        module.setActivities(List.of(activity));
        plan.setModules(List.of(module));

        // A trigger was created 1 day ago → still within the 7-day window
        ReplanTrigger recentTrigger = new ReplanTrigger();
        recentTrigger.setDetectedAt(OffsetDateTime.now().minusDays(1));
        when(triggerRepository.findTopByPlanIdAndTriggerTypeOrderByDetectedAtDesc(eq(planId), anyString()))
                .thenReturn(Optional.of(recentTrigger));

        assertNull(triggerService.evaluateProgressDeviation(plan));
        verify(triggerRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // evaluateInactivity
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateInactivity_InactivePlan_ReturnsNull() {
        LearningPlan plan = new LearningPlan();
        plan.setStatus("paused");

        assertNull(triggerService.evaluateInactivity(plan));
    }

    @Test
    void testEvaluateInactivity_ActiveRecently_ReturnsNull() {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());
        plan.setStatus("active");
        plan.setUpdatedAt(OffsetDateTime.now().minusDays(1)); // only 1 day inactive

        assertNull(triggerService.evaluateInactivity(plan));
    }

    @Test
    void testEvaluateInactivity_HighInactivity_CreatesTrigger() throws Exception {
        UUID planId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setStatus("active");
        plan.setUpdatedAt(OffsetDateTime.now().minusDays(15)); // 15 days inactive → HIGH

        when(triggerRepository.findTopByPlanIdAndTriggerTypeOrderByDetectedAtDesc(eq(planId), anyString()))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"days\":15}");

        ReplanTrigger saved = ReplanTrigger.builder()
                .plan(plan).triggerType("INACTIVITY").severity("HIGH").build();
        when(triggerRepository.save(any(ReplanTrigger.class))).thenReturn(saved);

        ReplanTrigger result = triggerService.evaluateInactivity(plan);

        assertNotNull(result);
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    void testEvaluateInactivity_FallbackToCreatedAt_WhenUpdatedAtNull() throws Exception {
        UUID planId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setStatus("active");
        plan.setUpdatedAt(null);
        plan.setCreatedAt(OffsetDateTime.now().minusDays(10)); // 10 days → MEDIUM

        when(triggerRepository.findTopByPlanIdAndTriggerTypeOrderByDetectedAtDesc(eq(planId), anyString()))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ReplanTrigger saved = ReplanTrigger.builder()
                .plan(plan).triggerType("INACTIVITY").severity("MEDIUM").build();
        when(triggerRepository.save(any(ReplanTrigger.class))).thenReturn(saved);

        ReplanTrigger result = triggerService.evaluateInactivity(plan);

        assertNotNull(result);
        assertEquals("MEDIUM", result.getSeverity());
    }

    // -------------------------------------------------------------------------
    // evaluateAllTriggers
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateAllTriggers_InactivePlan_ReturnsEmptyList() {
        LearningPlan plan = new LearningPlan();
        plan.setStatus("completed");

        List<ReplanTrigger> results = triggerService.evaluateAllTriggers(plan);

        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------------------------------
    // evaluateMasteryChanges — placeholder always returns null
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateMasteryChanges_AlwaysReturnsNull() {
        assertNull(triggerService.evaluateMasteryChanges(new LearningPlan()));
    }

    // -------------------------------------------------------------------------
    // createTriggerSuggestion
    // -------------------------------------------------------------------------

    @Test
    void testCreateTriggerSuggestion_SetsStatusAndSaves() {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());
        ReplanTrigger trigger = ReplanTrigger.builder().plan(plan).build();
        when(triggerRepository.save(trigger)).thenReturn(trigger);

        triggerService.createTriggerSuggestion(trigger);

        assertEquals("SUGGESTED", trigger.getStatus());
        assertNotNull(trigger.getEvaluatedAt());
        verify(triggerRepository).save(trigger);
    }

    // -------------------------------------------------------------------------
    // findPendingTriggers
    // -------------------------------------------------------------------------

    @Test
    void testFindPendingTriggers_DelegatesToRepository() {
        UUID planId = UUID.randomUUID();
        when(triggerRepository.findByPlanIdAndStatusOrderByDetectedAtDesc(planId, "PENDING"))
                .thenReturn(List.of());

        List<ReplanTrigger> result = triggerService.findPendingTriggers(planId);

        assertTrue(result.isEmpty());
        verify(triggerRepository).findByPlanIdAndStatusOrderByDetectedAtDesc(planId, "PENDING");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates an active plan where {@code completedActivities} out of
     * {@code totalActivities}
     * are completed. The plan started {@code daysAgo} and will end in
     * {@code daysLeft} days.
     */
    private LearningPlan activePlanWithActivities(
            int daysAgo, int daysLeft, int completedActivities, int totalActivities) {

        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now().minusDays(daysAgo));
        plan.setEndDate(LocalDate.now().plusDays(daysLeft));

        List<PlanActivity> activities = new ArrayList<>();
        for (int i = 0; i < totalActivities; i++) {
            PlanActivity a = new PlanActivity();
            a.setStatus(i < completedActivities ? "completed" : "pending");
            activities.add(a);
        }

        PlanModule module = new PlanModule();
        module.setActivities(activities);
        plan.setModules(List.of(module));
        return plan;
    }
}
