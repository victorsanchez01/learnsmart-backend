package com.learnsmart.planning;

import com.learnsmart.planning.model.*;
import com.learnsmart.planning.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PlanningServiceIntegrationTests {

    @Autowired
    private LearningPlanService planService;

    @Autowired
    private PlanModuleService moduleService;

    @Test
    @Transactional
    void fullPlanningFlow() {
        // 1. Create Plan (User ID simulated)
        String userId = UUID.randomUUID().toString();
        LearningPlan plan = new LearningPlan();
        plan.setUserId(userId);
        plan.setHoursPerWeek(new java.math.BigDecimal("10.0"));

        LearningPlan created = planService.createPlan(plan);
        assertNotNull(created.getId());
        assertEquals("active", created.getStatus());
        assertEquals(userId, created.getUserId());

        // Modules should be mock generated
        assertNotNull(created.getModules());
        assertFalse(created.getModules().isEmpty());
        PlanModule module1 = created.getModules().get(0);
        assertEquals(1, module1.getPosition());
        assertEquals("pending", module1.getStatus());

        // 2. Find Plan
        LearningPlan found = planService.findById(created.getId());
        assertEquals(created.getId(), found.getId());

        // 3. Update Status
        PlanModule updatedModule = moduleService.updateModuleStatus(created.getId(), module1.getId(), "in_progress");
        assertEquals("in_progress", updatedModule.getStatus());

        // 4. Replan
        LearningPlan replanned = planService.replan(created.getId(), "Too hard", "{\"easier\": true}");
        assertNotNull(replanned.getRawPlanAi());
        assertTrue(replanned.getRawPlanAi().contains("mock\": \"replanned"));
    }
}
