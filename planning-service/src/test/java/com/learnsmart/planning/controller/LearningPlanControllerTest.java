package com.learnsmart.planning.controller;

import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.dto.PlanDtos;
import com.learnsmart.planning.service.LearningPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningPlanControllerTest {

    @Mock
    private LearningPlanService planService;

    @InjectMocks
    private LearningPlanController controller;

    @Test
    void testCreatePlan() {
        LearningPlan plan = new LearningPlan();
        when(planService.createPlan(any(LearningPlan.class))).thenAnswer(i -> {
            LearningPlan p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ResponseEntity<LearningPlan> response = controller.createPlan(plan);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void testGetPlan() {
        UUID id = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(id);
        when(planService.findById(id)).thenReturn(plan);

        ResponseEntity<LearningPlan> response = controller.getPlan(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void testGetPlans_All() {
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planService.findAll(null, 0, 20)).thenReturn(page);

        ResponseEntity<Page<LearningPlan>> response = controller.getPlans(null, null, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetPlans_ByUser() {
        String userId = UUID.randomUUID().toString();
        Page<LearningPlan> page = new PageImpl<>(Collections.emptyList());
        when(planService.findByUser(userId, null, 0, 20)).thenReturn(page);

        ResponseEntity<Page<LearningPlan>> response = controller.getPlans(userId, null, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testUpdatePlan() {
        UUID id = UUID.randomUUID();
        LearningPlan updates = new LearningPlan();
        updates.setStatus("completed");

        LearningPlan updated = new LearningPlan();
        updated.setId(id);
        updated.setStatus("completed");

        when(planService.updatePlan(eq(id), any(LearningPlan.class))).thenReturn(updated);

        ResponseEntity<LearningPlan> response = controller.updatePlan(id, updates);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("completed", response.getBody().getStatus());
    }

    @Test
    void testReplan() {
        UUID id = UUID.randomUUID();
        LearningPlan plan = new LearningPlan();
        plan.setId(id);

        when(planService.replan(id, "User requested", "{}")).thenReturn(plan);

        ResponseEntity<PlanDtos.PlanSummaryResponse> response = controller.replan(id, "User requested", "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
