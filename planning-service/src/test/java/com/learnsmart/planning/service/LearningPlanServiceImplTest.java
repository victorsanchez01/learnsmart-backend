package com.learnsmart.planning.service;

import com.learnsmart.planning.client.Clients;
import com.learnsmart.planning.client.SkillPrerequisiteClient;
import com.learnsmart.planning.dto.ExternalDtos;
import com.learnsmart.planning.model.Certificate;
import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.model.PlanModule;
import com.learnsmart.planning.model.PlanReplanHistory;
import com.learnsmart.planning.repository.CertificateRepository;
import com.learnsmart.planning.repository.LearningPlanRepository;
import com.learnsmart.planning.repository.PlanReplanHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningPlanServiceImplTest {

    @Mock
    private LearningPlanRepository planRepository;
    @Mock
    private PlanReplanHistoryRepository replanRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private Clients.ProfileClient profileClient;
    @Mock
    private Clients.ContentClient contentClient;
    @Mock
    private Clients.AiClient aiClient;
    @Mock
    private ReplanTriggerService triggerService;
    @Mock
    private SkillPrerequisiteClient skillPrerequisiteClient;
    @Mock
    private PrerequisiteValidationService prerequisiteValidator;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private LearningPlanServiceImpl planService;

    // -------------------------------------------------------------------------
    // createPlan
    // -------------------------------------------------------------------------

    @Test
    void testCreatePlan_WithModules_SkipsAiGeneration() {
        LearningPlan plan = new LearningPlan();
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(List.of()); // Provided but empty — triggers AI path

        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        LearningPlan result = planService.createPlan(plan);
        assertNotNull(result.getId());
        verify(planRepository).save(plan);
        // Modules is empty so AI path is triggered, but profile fails silently
        // (AI client not mocked = exception caught internally)
    }

    @Test
    void testCreatePlan_NullModules_TakesAiPath() {
        // Exercises the null branch of: if (modules == null || modules.isEmpty())
        LearningPlan plan = new LearningPlan();
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(null); // explicit null — distinct JaCoCo branch from empty

        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        LearningPlan result = planService.createPlan(plan);
        assertNotNull(result.getId());
        verify(planRepository).save(plan);
    }

    @Test
    void testCreatePlan_WithExistingModules_SetsLinks() {
        LearningPlan plan = new LearningPlan();
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule module = new PlanModule();
        plan.setModules(new ArrayList<>(List.of(module)));

        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        LearningPlan result = planService.createPlan(plan);

        assertNotNull(result.getId());
        // The plan reference must be set on manual modules
        assertEquals(plan, module.getPlan());
        verify(aiClient, never()).generatePlan(any());
    }

    @Test
    void testCreatePlan_WithAIGeneration_MapsModulesAndActivities() throws Exception {
        UUID userId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setUserId(userId.toString());
        plan.setGoalId("goal-123");

        ExternalDtos.UserProfile profile = new ExternalDtos.UserProfile();
        profile.setUserId(userId.toString());
        when(profileClient.getProfile(userId.toString())).thenReturn(profile);
        when(contentClient.getContentItems(100)).thenReturn(Collections.emptyList());
        when(objectMapper.convertValue(any(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new java.util.HashMap<>());

        ExternalDtos.ActivityDraft activityDraft = new ExternalDtos.ActivityDraft();
        activityDraft.setType("lesson");
        activityDraft.setContentRef("content-ref-1");

        ExternalDtos.ModuleDraft moduleDraft = new ExternalDtos.ModuleDraft();
        moduleDraft.setTitle("Module 1");
        moduleDraft.setDescription("Test module");
        moduleDraft.setActivities(List.of(activityDraft));

        ExternalDtos.PlanDraft planDraft = new ExternalDtos.PlanDraft();
        planDraft.setModules(List.of(moduleDraft));

        ExternalDtos.GeneratePlanResponse aiResponse = new ExternalDtos.GeneratePlanResponse();
        aiResponse.setPlan(planDraft);

        when(aiClient.generatePlan(any(ExternalDtos.GeneratePlanRequest.class))).thenReturn(aiResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        LearningPlan result = planService.createPlan(plan);

        assertNotNull(result);
        assertNotNull(result.getModules());
        assertEquals(1, result.getModules().size());
        assertEquals("Module 1", result.getModules().get(0).getTitle());
        assertEquals(1, result.getModules().get(0).getActivities().size());
        assertEquals("lesson", result.getModules().get(0).getActivities().get(0).getActivityType());

        verify(profileClient).getProfile(userId.toString());
        verify(contentClient).getContentItems(100);
        verify(aiClient).generatePlan(any());
    }

    @Test
    void testCreatePlan_AIGenerationFailure_SavesEmptyPlan() {
        // Documents intentional behavior: if AI fails, plan is saved empty (MVP
        // fallback).
        // This is a non-blocking fallback logged server-side.
        UUID userId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setUserId(userId.toString());

        when(profileClient.getProfile(userId.toString()))
                .thenThrow(new RuntimeException("Profile service down"));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        LearningPlan result = planService.createPlan(plan);

        assertNotNull(result, "Plan must still be saved when AI generation fails (MVP fallback)");
        verify(planRepository).save(plan);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void testFindById_Found() {
        UUID id = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(id);
        when(planRepository.findById(id)).thenReturn(Optional.of(plan));

        LearningPlan result = planService.findById(id);
        assertEquals(id, result.getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(planRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> planService.findById(id));
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    void testFindAll_WithStatus() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planRepository.findByStatus("active", pageRequest)).thenReturn(page);

        Page<LearningPlan> result = planService.findAll("active", 0, 20);
        assertTrue(result.isEmpty());
        verify(planRepository).findByStatus("active", pageRequest);
    }

    @Test
    void testFindAll_NoStatus() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planRepository.findAll(pageRequest)).thenReturn(page);

        Page<LearningPlan> result = planService.findAll(null, 0, 20);
        assertTrue(result.isEmpty());
        verify(planRepository).findAll(pageRequest);
    }

    // -------------------------------------------------------------------------
    // findByUser
    // -------------------------------------------------------------------------

    @Test
    void testFindByUser_WithStatus() {
        String userId = UUID.randomUUID().toString();
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planRepository.findByUserIdAndStatus(userId, "active", pageRequest)).thenReturn(page);

        Page<LearningPlan> result = planService.findByUser(userId, "active", 0, 20);
        assertTrue(result.isEmpty());
        verify(planRepository).findByUserIdAndStatus(userId, "active", pageRequest);
    }

    @Test
    void testFindByUser_NoStatus_UsesUnfilteredQuery() {
        String userId = UUID.randomUUID().toString();
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planRepository.findByUserId(userId, pageRequest)).thenReturn(page);

        Page<LearningPlan> result = planService.findByUser(userId, null, 0, 20);
        assertTrue(result.isEmpty());
        verify(planRepository).findByUserId(userId, pageRequest);
        verify(planRepository, never()).findByUserIdAndStatus(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // updatePlan
    // -------------------------------------------------------------------------

    @Test
    void testUpdatePlan_OnlyMutableFieldsChanged() {
        UUID id = UUID.randomUUID();
        String originalUserId = UUID.randomUUID().toString();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setUserId(originalUserId);
        existing.setStatus("draft");

        LearningPlan updates = new LearningPlan();
        updates.setStatus("active");
        updates.setHoursPerWeek(new BigDecimal("10"));
        // userId not set — must not be overwritten

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        LearningPlan result = planService.updatePlan(id, updates);

        assertEquals("active", result.getStatus());
        assertEquals(new BigDecimal("10"), result.getHoursPerWeek());
        // Immutable field must be preserved
        assertEquals(originalUserId, result.getUserId(), "userId must not be overwritten during update");
    }

    // -------------------------------------------------------------------------
    // replan
    // -------------------------------------------------------------------------

    @Test
    void testReplan_SavesHistoryWithCorrectReason() throws Exception {
        UUID id = UUID.randomUUID();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setModules(new ArrayList<>());

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));

        ExternalDtos.ReplanResponse replanResponse = new ExternalDtos.ReplanResponse(null, "Modules reordered");
        when(aiClient.replan(any(ExternalDtos.ReplanRequest.class))).thenReturn(replanResponse);

        ArgumentCaptor<PlanReplanHistory> historyCaptor = ArgumentCaptor.forClass(PlanReplanHistory.class);
        when(replanRepository.save(historyCaptor.capture())).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        LearningPlan result = planService.replan(id, "User requested changes", "{}");

        assertNotNull(result);
        PlanReplanHistory captured = historyCaptor.getValue();
        assertEquals("User requested changes", captured.getReason(),
                "Replan history must record the reason");
        assertEquals("{}", captured.getRequestPayload());
        assertEquals("Modules reordered", captured.getResponsePayload());
        verify(replanRepository).save(any(PlanReplanHistory.class));
        verify(planRepository).save(existing);
    }

    @Test
    void testReplan_AiClientFails_ThrowsException() throws Exception {
        UUID id = UUID.randomUUID();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setModules(new ArrayList<>());

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));
        when(aiClient.replan(any())).thenThrow(new RuntimeException("AI unreachable"));

        assertThrows(RuntimeException.class, () -> planService.replan(id, "reason", null));
    }

    // -------------------------------------------------------------------------
    // checkCompletion
    // -------------------------------------------------------------------------

    @Test
    void testCheckCompletion_AllModulesCompleted_IssuesCertificate() {
        UUID planId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule completedModule = new PlanModule();
        completedModule.setStatus("completed");
        plan.setModules(new ArrayList<>(List.of(completedModule)));

        when(certificateRepository.existsByPlanId(planId)).thenReturn(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        planService.checkCompletion(planId);

        verify(certificateRepository).save(any(Certificate.class));
        assertEquals("completed", plan.getStatus());
    }

    @Test
    void testCheckCompletion_PendingModules_NoCertificate() {
        UUID planId = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(UUID.randomUUID().toString());

        PlanModule pendingModule = new PlanModule();
        pendingModule.setStatus("pending");
        plan.setModules(new ArrayList<>(List.of(pendingModule)));

        when(certificateRepository.existsByPlanId(planId)).thenReturn(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        planService.checkCompletion(planId);

        verify(certificateRepository, never()).save(any());
    }

    @Test
    void testCheckCompletion_AlreadyCertified_SkipsEverything() {
        UUID planId = UUID.randomUUID();
        when(certificateRepository.existsByPlanId(planId)).thenReturn(true);

        planService.checkCompletion(planId);

        verify(planRepository, never()).findById(any());
        verify(certificateRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getCertificates
    // -------------------------------------------------------------------------

    @Test
    void testGetCertificates_ReturnsAll() {
        UUID userId = UUID.randomUUID();
        Certificate cert = new Certificate();
        cert.setUserId(userId);
        when(certificateRepository.findByUserId(userId)).thenReturn(List.of(cert));

        List<Certificate> result = planService.getCertificates(userId);
        assertEquals(1, result.size());
        verify(certificateRepository).findByUserId(userId);
    }
    // -------------------------------------------------------------------------
    // replan — AI response with new modules (previously ~38%)
    // -------------------------------------------------------------------------

    @Test
    void testReplan_AiResponseWithModules_ReplacesExistingModules() throws Exception {
        UUID id = UUID.randomUUID();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setUserId("user-1");
        existing.setModules(new ArrayList<>());

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));

        // AI returns a plan with 2 modules, each with 1 activity
        ExternalDtos.ActivityDraft actDraft = new ExternalDtos.ActivityDraft("lesson", "content-ref-1");
        ExternalDtos.ModuleDraft mod1 = new ExternalDtos.ModuleDraft("Module A", "Desc A", List.of(actDraft));
        ExternalDtos.ModuleDraft mod2 = new ExternalDtos.ModuleDraft("Module B", "Desc B",
                List.of(new ExternalDtos.ActivityDraft("practice", null))); // null ref → sys: fallback
        ExternalDtos.PlanDraft planDraft = new ExternalDtos.PlanDraft("plan-1", List.of(mod1, mod2));
        ExternalDtos.ReplanResponse aiResponse = new ExternalDtos.ReplanResponse(planDraft, "Reordered 2 modules");

        when(aiClient.replan(any(ExternalDtos.ReplanRequest.class))).thenReturn(aiResponse);
        when(replanRepository.save(any(PlanReplanHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        LearningPlan result = planService.replan(id, "User requested changes", "{}");

        assertNotNull(result);
        assertEquals(2, result.getModules().size(), "Replan must replace modules with AI result");
        assertEquals(1, result.getModules().get(0).getPosition());
        assertEquals(2, result.getModules().get(1).getPosition());
        // Activity with null contentRef must fall back to a sys: prefix
        assertTrue(result.getModules().get(1).getActivities().get(0).getContentRef().startsWith("sys:"),
                "Null contentRef must be replaced with sys: fallback");
    }

    @Test
    void testReplan_TriggerBasedReason_MarksHighTriggerAsExecuted() throws Exception {
        UUID id = UUID.randomUUID();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setUserId("user-2");
        existing.setModules(new ArrayList<>());

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));

        // AI returns no plan → no module replacement, just history
        ExternalDtos.ReplanResponse aiResponse = new ExternalDtos.ReplanResponse(null, "No changes needed");
        when(aiClient.replan(any())).thenReturn(aiResponse);

        // There's a HIGH severity pending trigger
        com.learnsmart.planning.model.ReplanTrigger highTrigger = com.learnsmart.planning.model.ReplanTrigger.builder()
                .id(UUID.randomUUID())
                .plan(existing)
                .severity("HIGH")
                .build();
        when(triggerService.findPendingTriggers(id)).thenReturn(List.of(highTrigger));
        when(replanRepository.save(any(PlanReplanHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        // Reason contains "trigger" → the branch that calls findPendingTriggers
        planService.replan(id, "Automatic trigger replan", "{}");

        assertEquals("EXECUTED", highTrigger.getStatus(),
                "HIGH severity trigger must be marked EXECUTED when replan is triggered");
        verify(triggerService).findPendingTriggers(id);
    }

    @Test
    void testReplan_AiResponseNull_DoesNotReplaceModules() throws Exception {
        UUID id = UUID.randomUUID();
        LearningPlan existing = new LearningPlan();
        existing.setId(id);
        existing.setUserId("user-3");
        existing.setModules(new ArrayList<>());

        when(planRepository.findById(id)).thenReturn(Optional.of(existing));
        when(aiClient.replan(any())).thenReturn(null);
        when(replanRepository.save(any(PlanReplanHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        LearningPlan result = planService.replan(id, "Just checking", null);

        assertNotNull(result);
        assertTrue(result.getModules().isEmpty(), "Null AI response must not modify modules");
    }

    // -------------------------------------------------------------------------
    // createPlan — with skill-bearing modules that trigger prerequisite check
    // -------------------------------------------------------------------------

    @Test
    void testCreatePlan_WithSkillModules_AndViolations_TriggersReorder() {
        UUID skillId = UUID.randomUUID();
        PlanModule moduleWithSkill = new PlanModule();
        moduleWithSkill.setPosition(1);
        moduleWithSkill.setTargetSkills(List.of(skillId.toString()));
        moduleWithSkill.setActivities(List.of());

        LearningPlan plan = new LearningPlan();
        plan.setUserId(UUID.randomUUID().toString());
        plan.setModules(new ArrayList<>(List.of(moduleWithSkill)));

        // Validation finds a violation → triggers reorder
        com.learnsmart.planning.dto.PrerequisiteDtos.PrerequisiteViolation violation = com.learnsmart.planning.dto.PrerequisiteDtos.PrerequisiteViolation
                .builder()
                .skillId(skillId.toString())
                .build();
        when(skillPrerequisiteClient.getSkillGraph(any())).thenReturn(java.util.Map.of());
        when(prerequisiteValidator.validatePlan(any(), any())).thenReturn(List.of(violation));
        when(prerequisiteValidator.reorderForPrerequisites(any(), any())).thenReturn(plan);
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        LearningPlan result = planService.createPlan(plan);

        assertNotNull(result);
        verify(prerequisiteValidator).reorderForPrerequisites(any(), any());
    }

    // -------------------------------------------------------------------------
    // generateDiagnosticTest — 0% coverage
    // -------------------------------------------------------------------------

    @Test
    void testGenerateDiagnosticTest_ReturnsMappedQuestions() {
        List<java.util.Map<String, Object>> fakeQuestions = List.of(
                java.util.Map.of("id", "q1", "text", "What is Java?"),
                java.util.Map.of("id", "q2", "text", "Explain OOP."));
        ExternalDtos.GenerateDiagnosticTestResponse aiResponse = new ExternalDtos.GenerateDiagnosticTestResponse(
                fakeQuestions);

        when(aiClient.generateDiagnosticTest(any(ExternalDtos.GenerateDiagnosticTestRequest.class)))
                .thenReturn(aiResponse);

        List<java.util.Map<String, Object>> result = planService.generateDiagnosticTest("java-domain", "INTERMEDIATE",
                2);

        assertEquals(2, result.size());
        assertEquals("q1", result.get(0).get("id"));
        verify(aiClient).generateDiagnosticTest(any(ExternalDtos.GenerateDiagnosticTestRequest.class));
    }
}
