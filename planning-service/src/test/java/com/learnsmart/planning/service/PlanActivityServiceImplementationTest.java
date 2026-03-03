package com.learnsmart.planning.service;

import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.model.PlanActivity;
import com.learnsmart.planning.model.PlanModule;
import com.learnsmart.planning.repository.PlanActivityRepository;
import com.learnsmart.planning.repository.PlanModuleRepository;
import com.learnsmart.planning.repository.LearningPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanActivityServiceImplementationTest {

    @Mock
    private PlanActivityRepository activityRepository;

    @Mock
    private PlanModuleRepository moduleRepository;

    @Mock
    private LearningPlanRepository planRepository;

    @InjectMocks
    private PlanActivityServiceImplementation activityService;

    @Test
    void testGetActivitiesByModule() {
        UUID moduleId = UUID.randomUUID();
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(Collections.emptyList());

        List<PlanActivity> result = activityService.getActivitiesByModule(moduleId);
        assertTrue(result.isEmpty());
        verify(activityRepository).findByModuleIdOrderByPositionAsc(moduleId);
    }

    @Test
    void testUpdateActivityStatus_Success() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(PlanActivity.class))).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        PlanActivity result = activityService.updateActivityStatus(planId, activityId, "completed", null);
        assertEquals("completed", result.getStatus());
        verify(activityRepository).save(activity);
    }

    @Test
    void testUpdateActivityStatus_WithOverrideMinutes() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(PlanActivity.class))).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        PlanActivity result = activityService.updateActivityStatus(planId, activityId, "completed", 45);
        assertEquals("completed", result.getStatus());
        assertEquals(45, result.getOverrideEstimatedMinutes());
    }

    @Test
    void testUpdateActivityStatus_WrongPlan() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID wrongPlanId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(wrongPlanId);

        PlanModule module = new PlanModule();
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        assertThrows(RuntimeException.class,
                () -> activityService.updateActivityStatus(planId, activityId, "completed", null));
    }

    @Test
    void testFindById_Found() {
        UUID id = UUID.randomUUID();
        PlanActivity activity = new PlanActivity();
        activity.setId(id);

        when(activityRepository.findById(id)).thenReturn(Optional.of(activity));

        PlanActivity result = activityService.findById(id);
        assertEquals(id, result.getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(activityRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> activityService.findById(id));
    }
    // -------------------------------------------------------------------------
    // validateStatusTransition — called indirectly via updateActivityStatus
    // -------------------------------------------------------------------------

    @Test
    void testUpdateActivityStatus_CompletedToInProgress_ThrowsIllegalArgument() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);

        PlanModule module = new PlanModule();
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("completed"); // Current status = completed

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        // Trying to move from completed → in_progress should throw
        assertThrows(IllegalArgumentException.class,
                () -> activityService.updateActivityStatus(planId, activityId, "in_progress", null));
    }

    @Test
    void testUpdateActivityStatus_CompletedToCompleted_IsAllowed() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(List.of());

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);
        module.setStatus("completed"); // already completed

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("completed");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        // completed → completed must NOT throw
        PlanActivity result = activityService.updateActivityStatus(planId, activityId, "completed", null);
        assertEquals("completed", result.getStatus());
    }

    // -------------------------------------------------------------------------
    // updateModuleCompletionStatus — module already completed (branch not
    // exercised)
    // -------------------------------------------------------------------------

    @Test
    void testUpdateActivityStatus_AllCompleted_ModuleAlreadyCompleted_SkipsSave() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);
        module.setStatus("completed"); // already completed → no moduleRepository.save expected

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        activityService.updateActivityStatus(planId, activityId, "completed", null);

        // Module was already completed → moduleRepository.save must NOT be called again
        verify(moduleRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updatePlanCompletionStatus — null/empty modules and not-all-completed
    // -------------------------------------------------------------------------

    @Test
    void testUpdateActivityStatus_AllCompleted_PlanModulesNull_SkipsPlanSave() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(null); // null → updatePlanCompletionStatus must return early

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        activityService.updateActivityStatus(planId, activityId, "completed", null);

        verify(planRepository, never()).save(any());
    }

    @Test
    void testUpdateActivityStatus_NotAllModulesCompleted_DoesNotCompletePlan() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        // Another module is still pending
        PlanModule pendingModule = new PlanModule();
        pendingModule.setId(UUID.randomUUID());
        pendingModule.setStatus("pending");

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());
        plan.setStatus("active");

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);
        plan.setModules(List.of(module, pendingModule)); // one module still pending

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        activityService.updateActivityStatus(planId, activityId, "completed", null);

        // Plan must remain active since not all modules are completed
        assertEquals("active", plan.getStatus());
        verify(planRepository, never()).save(any());
    }

    @Test
    void testUpdateActivityStatus_AllModulesCompleted_CompletesPlan() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());
        plan.setStatus("active");

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);
        // After activity is completed, module will be marked completed too
        // so the plan's only module list contains this module (will be completed)
        plan.setModules(List.of(module));

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));
        when(moduleRepository.save(any(PlanModule.class))).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        activityService.updateActivityStatus(planId, activityId, "completed", null);

        // After all activities and modules completed, plan must be completed
        assertEquals("completed", plan.getStatus());
        verify(planRepository).save(plan);
    }

    // -------------------------------------------------------------------------
    // emitActivityCompletedEvent — completedAt non-null branch
    // -------------------------------------------------------------------------

    @Test
    void testUpdateActivityStatus_WithCompletedAt_UsesExistingTimestamp() {
        UUID planId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(List.of());

        PlanModule module = new PlanModule();
        module.setId(moduleId);
        module.setPlan(plan);

        PlanActivity activity = new PlanActivity();
        activity.setId(activityId);
        activity.setModule(module);
        activity.setStatus("pending");
        activity.setCompletedAt(java.time.OffsetDateTime.now().minusHours(1)); // non-null → no fallback
        activity.setActualMinutesSpent(30);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(activity));

        // Must complete without exception even with non-null completedAt
        PlanActivity result = activityService.updateActivityStatus(planId, activityId, "completed", null);
        assertEquals("completed", result.getStatus());
    }
}
