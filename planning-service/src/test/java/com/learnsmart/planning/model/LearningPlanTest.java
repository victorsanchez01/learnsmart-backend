package com.learnsmart.planning.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for LearningPlan JPA lifecycle callbacks.
 */
class LearningPlanTest {

    @Test
    void testPrePersist_SetsCreatedAtAndUpdatedAt_WhenCreatedAtIsNull() {
        LearningPlan plan = new LearningPlan();
        assertNull(plan.getCreatedAt());

        plan.prePersist();

        assertNotNull(plan.getCreatedAt());
        assertNotNull(plan.getUpdatedAt());
        // createdAt and updatedAt should be the same instant (set in the same now()
        // call)
        assertFalse(plan.getCreatedAt().isAfter(plan.getUpdatedAt()),
                "createdAt must not be after updatedAt when set in the same prePersist call");
    }

    @Test
    void testPrePersist_DoesNotOverrideCreatedAt_WhenAlreadySet() {
        LearningPlan plan = new LearningPlan();
        OffsetDateTime original = OffsetDateTime.now().minusDays(5);
        plan.setCreatedAt(original);

        plan.prePersist();

        assertEquals(original, plan.getCreatedAt(),
                "prePersist must not overwrite an existing createdAt");
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    void testPreUpdate_RefreshesUpdatedAt() {
        LearningPlan plan = new LearningPlan();
        OffsetDateTime old = OffsetDateTime.now().minusHours(5);
        plan.setUpdatedAt(old);

        plan.preUpdate();

        assertTrue(plan.getUpdatedAt().isAfter(old),
                "preUpdate must refresh updatedAt to a later instant");
    }
}
