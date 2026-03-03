package com.learnsmart.planning.service;

import com.learnsmart.planning.dto.PrerequisiteDtos;
import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.model.PlanModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for PrerequisiteValidationService — no Spring context needed.
 */
class PrerequisiteValidationServiceTest {

    private PrerequisiteValidationService service;

    @BeforeEach
    void setUp() {
        service = new PrerequisiteValidationService();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PlanModule moduleWithSkill(UUID skillId, int position) {
        PlanModule m = new PlanModule();
        m.setId(UUID.randomUUID());
        m.setPosition(position);
        m.setTargetSkills(new ArrayList<>(List.of(skillId.toString())));
        return m;
    }

    private LearningPlan planWithModules(List<PlanModule> modules) {
        LearningPlan plan = new LearningPlan();
        plan.setId(UUID.randomUUID());
        plan.setModules(new ArrayList<>(modules));
        return plan;
    }

    // -------------------------------------------------------------------------
    // validatePlan — no modules
    // -------------------------------------------------------------------------

    @Test
    void testValidatePlan_NullModules_ReturnsEmpty() {
        LearningPlan plan = new LearningPlan();
        plan.setModules(null);

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, Map.of());

        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidatePlan_EmptyModules_ReturnsEmpty() {
        LearningPlan plan = planWithModules(List.of());

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, Map.of());

        assertTrue(violations.isEmpty());
    }

    // -------------------------------------------------------------------------
    // validatePlan — no violations
    // -------------------------------------------------------------------------

    @Test
    void testValidatePlan_PrerequisiteBeforeSkill_NoViolation() {
        UUID skillA = UUID.randomUUID();
        UUID skillB = UUID.randomUUID();

        // Module 0 = A (prerequisite), Module 1 = B (depends on A)
        LearningPlan plan = planWithModules(List.of(
                moduleWithSkill(skillA, 1),
                moduleWithSkill(skillB, 2)));

        // B requires A
        Map<UUID, List<UUID>> graph = Map.of(skillB, List.of(skillA));

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, graph);

        assertTrue(violations.isEmpty(), "No violation when prerequisite comes before skill");
    }

    @Test
    void testValidatePlan_EmptySkillGraph_NoViolations() {
        UUID skillA = UUID.randomUUID();
        LearningPlan plan = planWithModules(List.of(moduleWithSkill(skillA, 1)));

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, Map.of());

        assertTrue(violations.isEmpty());
    }

    // -------------------------------------------------------------------------
    // validatePlan — violations
    // -------------------------------------------------------------------------

    @Test
    void testValidatePlan_PrerequisiteAfterSkill_ReturnsViolation() {
        UUID skillA = UUID.randomUUID(); // prerequisite
        UUID skillB = UUID.randomUUID(); // depends on A

        // Module 0 = B (dependent), Module 1 = A (prerequisite) — WRONG ORDER
        LearningPlan plan = planWithModules(List.of(
                moduleWithSkill(skillB, 1),
                moduleWithSkill(skillA, 2)));

        Map<UUID, List<UUID>> graph = Map.of(skillB, List.of(skillA));

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, graph);

        assertEquals(1, violations.size());
        assertEquals(skillB.toString(), violations.get(0).getSkillId());
        assertEquals(skillA.toString(), violations.get(0).getPrerequisiteSkillId());
    }

    @Test
    void testValidatePlan_MissingPrerequisiteInPlan_NoViolation() {
        UUID skillA = UUID.randomUUID(); // prerequisite (NOT in plan)
        UUID skillB = UUID.randomUUID(); // depends on A

        LearningPlan plan = planWithModules(List.of(moduleWithSkill(skillB, 1)));

        // A is a prerequisite but not in the plan — not a violation per design
        Map<UUID, List<UUID>> graph = Map.of(skillB, List.of(skillA));

        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(plan, graph);

        assertTrue(violations.isEmpty(),
                "Missing prerequisite (not in plan) must not be treated as a violation");
    }

    // -------------------------------------------------------------------------
    // validatePlan — module with non-UUID skill reference is ignored
    // -------------------------------------------------------------------------

    @Test
    void testValidatePlan_NonUuidSkillRef_IsIgnored() {
        PlanModule m = new PlanModule();
        m.setId(UUID.randomUUID());
        m.setPosition(1);
        m.setTargetSkills(List.of("not-a-uuid"));
        LearningPlan plan = planWithModules(List.of(m));

        assertDoesNotThrow(() -> service.validatePlan(plan, Map.of()));
    }

    @Test
    void testValidatePlan_NullTargetSkills_IsIgnored() {
        PlanModule m = new PlanModule();
        m.setId(UUID.randomUUID());
        m.setPosition(1);
        m.setTargetSkills(null);
        LearningPlan plan = planWithModules(List.of(m));

        assertDoesNotThrow(() -> service.validatePlan(plan, Map.of()));
    }

    // -------------------------------------------------------------------------
    // reorderForPrerequisites
    // -------------------------------------------------------------------------

    @Test
    void testReorderForPrerequisites_EmptyPlan_ReturnsSamePlan() {
        LearningPlan plan = planWithModules(List.of());
        LearningPlan result = service.reorderForPrerequisites(plan, Map.of());
        assertSame(plan, result);
    }

    @Test
    void testReorderForPrerequisites_NullModules_ReturnsSamePlan() {
        LearningPlan plan = new LearningPlan();
        plan.setModules(null);
        LearningPlan result = service.reorderForPrerequisites(plan, Map.of());
        assertSame(plan, result);
    }

    @Test
    void testReorderForPrerequisites_ReordersCorrectly() {
        UUID skillA = UUID.randomUUID(); // prerequisite
        UUID skillB = UUID.randomUUID(); // depends on A

        PlanModule moduleB = moduleWithSkill(skillB, 1); // wrong: B before A
        PlanModule moduleA = moduleWithSkill(skillA, 2);

        LearningPlan plan = planWithModules(List.of(moduleB, moduleA));

        // B requires A
        Map<UUID, List<UUID>> graph = Map.of(skillB, List.of(skillA));

        LearningPlan result = service.reorderForPrerequisites(plan, graph);

        assertNotNull(result.getModules());
        assertEquals(2, result.getModules().size());
        // After reorder, A (prerequisite) must come first
        assertEquals(1, result.getModules().get(0).getPosition());
        assertEquals(2, result.getModules().get(1).getPosition());

        // Validate no violations after reorder
        List<PrerequisiteDtos.PrerequisiteViolation> violations = service.validatePlan(result, graph);
        assertTrue(violations.isEmpty(), "No violations expected after reorder");
    }

    @Test
    void testReorderForPrerequisites_CyclicDependency_ThrowsIllegalState() {
        UUID skillA = UUID.randomUUID();
        UUID skillB = UUID.randomUUID();

        LearningPlan plan = planWithModules(List.of(
                moduleWithSkill(skillA, 1),
                moduleWithSkill(skillB, 2)));

        // Cycle: A → requires B, B → requires A
        Map<UUID, List<UUID>> cyclicGraph = Map.of(
                skillA, List.of(skillB),
                skillB, List.of(skillA));

        assertThrows(IllegalStateException.class,
                () -> service.reorderForPrerequisites(plan, cyclicGraph),
                "Cyclic dependency must throw IllegalStateException");
    }
}
