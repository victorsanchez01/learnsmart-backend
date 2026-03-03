package com.learnsmart.planning.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for PlanModule JPA lifecycle callbacks.
 * No Spring context needed — callbacks are plain public methods.
 */
class PlanModuleTest {

    // -------------------------------------------------------------------------
    // prePersist — createdAt is null (first time)
    // -------------------------------------------------------------------------

    @Test
    void testPrePersist_SetsCreatedAtAndUpdatedAt_WhenCreatedAtIsNull() {
        PlanModule module = new PlanModule();
        assertNull(module.getCreatedAt());
        assertNull(module.getUpdatedAt());

        module.prePersist();

        assertNotNull(module.getCreatedAt(), "prePersist must set createdAt");
        assertNotNull(module.getUpdatedAt(), "prePersist must set updatedAt");
        // Both should be the same instant (set in the same now() call)
        assertFalse(module.getCreatedAt().isAfter(module.getUpdatedAt()),
                "createdAt must not be after updatedAt");
    }

    // -------------------------------------------------------------------------
    // prePersist — createdAt already set (re-persist guard)
    // -------------------------------------------------------------------------

    @Test
    void testPrePersist_DoesNotOverrideCreatedAt_WhenAlreadySet() {
        PlanModule module = new PlanModule();
        OffsetDateTime original = OffsetDateTime.now().minusDays(3);
        module.setCreatedAt(original);

        module.prePersist();

        assertEquals(original, module.getCreatedAt(),
                "prePersist must not overwrite an existing createdAt");
        assertNotNull(module.getUpdatedAt(), "prePersist must still update updatedAt");
    }

    // -------------------------------------------------------------------------
    // preUpdate — always refreshes updatedAt
    // -------------------------------------------------------------------------

    @Test
    void testPreUpdate_RefreshesUpdatedAt() {
        PlanModule module = new PlanModule();
        OffsetDateTime old = OffsetDateTime.now().minusHours(2);
        module.setUpdatedAt(old);

        module.preUpdate();

        assertNotNull(module.getUpdatedAt());
        assertTrue(module.getUpdatedAt().isAfter(old),
                "preUpdate must refresh updatedAt to a later instant");
    }
}
