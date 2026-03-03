package com.learnsmart.planning.scheduler;

import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.model.ReplanTrigger;
import com.learnsmart.planning.repository.LearningPlanRepository;
import com.learnsmart.planning.service.ReplanTriggerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplanScheduledJobTest {

    @Mock
    private LearningPlanRepository planRepository;
    @Mock
    private ReplanTriggerService triggerService;

    @InjectMocks
    private ReplanScheduledJob scheduledJob;

    // -------------------------------------------------------------------------
    // evaluateActivePlansForReplanning — no active plans
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateActivePlans_NoPlans_DoesNothing() {
        Page<LearningPlan> emptyPage = Page.empty();
        when(planRepository.findByStatus(eq("active"), any(PageRequest.class))).thenReturn(emptyPage);

        scheduledJob.evaluateActivePlansForReplanning();

        verifyNoInteractions(triggerService);
    }

    // -------------------------------------------------------------------------
    // evaluateActivePlansForReplanning — plans with no HIGH triggers
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateActivePlans_NoHighTriggers_DoesNotCreateSuggestions() {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());

        ReplanTrigger lowTrigger = ReplanTrigger.builder().severity("LOW").build();

        Page<LearningPlan> page = new PageImpl<>(List.of(plan));
        when(planRepository.findByStatus(eq("active"), any(PageRequest.class))).thenReturn(page);
        when(triggerService.evaluateAllTriggers(plan)).thenReturn(List.of(lowTrigger));

        scheduledJob.evaluateActivePlansForReplanning();

        verify(triggerService).evaluateAllTriggers(plan);
        verify(triggerService, never()).createTriggerSuggestion(any());
    }

    // -------------------------------------------------------------------------
    // evaluateActivePlansForReplanning — HIGH trigger creates suggestion
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateActivePlans_HighTrigger_CreatesSuggestion() {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());

        ReplanTrigger highTrigger = ReplanTrigger.builder().severity("HIGH").build();

        Page<LearningPlan> page = new PageImpl<>(List.of(plan));
        when(planRepository.findByStatus(eq("active"), any(PageRequest.class))).thenReturn(page);
        when(triggerService.evaluateAllTriggers(plan)).thenReturn(List.of(highTrigger));

        scheduledJob.evaluateActivePlansForReplanning();

        verify(triggerService).createTriggerSuggestion(highTrigger);
    }

    // -------------------------------------------------------------------------
    // evaluateActivePlansForReplanning — single plan throws, continues
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateActivePlans_PlanThrows_ContinuesWithOtherPlans() {
        LearningPlan failingPlan = new LearningPlan();
        failingPlan.setId(UUID.randomUUID());

        LearningPlan goodPlan = new LearningPlan();
        goodPlan.setId(UUID.randomUUID());

        Page<LearningPlan> page = new PageImpl<>(List.of(failingPlan, goodPlan));
        when(planRepository.findByStatus(eq("active"), any(PageRequest.class))).thenReturn(page);
        when(triggerService.evaluateAllTriggers(failingPlan))
                .thenThrow(new RuntimeException("Service unavailable"));
        when(triggerService.evaluateAllTriggers(goodPlan)).thenReturn(List.of());

        // Must not throw even when one plan evaluation fails
        scheduledJob.evaluateActivePlansForReplanning();

        verify(triggerService).evaluateAllTriggers(failingPlan);
        verify(triggerService).evaluateAllTriggers(goodPlan);
    }

    // -------------------------------------------------------------------------
    // evaluateActivePlansForReplanning — pagination (2 pages)
    // -------------------------------------------------------------------------

    @Test
    void testEvaluateActivePlans_MultiplePagesOfPlans_ProcessesAll() {
        LearningPlan plan1 = new LearningPlan();
        plan1.setId(UUID.randomUUID());
        LearningPlan plan2 = new LearningPlan();
        plan2.setId(UUID.randomUUID());

        // total must be > BATCH_SIZE (100) for hasNext() to be true on page 0:
        // hasNext() = (pageNumber+1)*pageSize < total → (0+1)*100 < 101 = true
        Page<LearningPlan> firstPage = new PageImpl<>(
                List.of(plan1),
                PageRequest.of(0, 100),
                101);
        Page<LearningPlan> secondPage = new PageImpl<>(
                List.of(plan2),
                PageRequest.of(1, 100),
                101 // (1+1)*100 = 200 >= 101 → hasNext() = false
        );

        when(planRepository.findByStatus(eq("active"), any(PageRequest.class)))
                .thenReturn(firstPage, secondPage);
        when(triggerService.evaluateAllTriggers(any())).thenReturn(List.of());

        scheduledJob.evaluateActivePlansForReplanning();

        verify(triggerService, times(2)).evaluateAllTriggers(any());
    }

    // -------------------------------------------------------------------------
    // runManualEvaluation — delegates to job method
    // -------------------------------------------------------------------------

    @Test
    void testRunManualEvaluation_DelegatesToEvaluateMethod() {
        Page<LearningPlan> emptyPage = Page.empty();
        when(planRepository.findByStatus(eq("active"), any(PageRequest.class))).thenReturn(emptyPage);

        scheduledJob.runManualEvaluation();

        verify(planRepository).findByStatus(eq("active"), any(PageRequest.class));
    }
}
