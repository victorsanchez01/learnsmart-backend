CREATE TABLE learning_events (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,                    -- viene de Keycloak (sub)
    event_type      VARCHAR(50) NOT NULL,             -- p.ej. 'activity_completed'
    entity_type     VARCHAR(50),                      -- p.ej. 'plan_activity'
    entity_id       UUID,                             -- id de la entidad (si aplica)
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(), -- momento real del evento

    payload         JSONB,                            -- datos adicionales del evento

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ                       -- NULL = activo; no NULL = borrado lógico
);

-- Consultas por usuario + fecha
CREATE INDEX idx_learning_events_user_occurred
    ON learning_events (user_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- Consultas por tipo de evento
CREATE INDEX idx_learning_events_type_occurred
    ON learning_events (event_type, occurred_at DESC)
    WHERE deleted_at IS NULL;

-- Consultas por entidad (ej: actividad, assessment, etc.)
CREATE INDEX idx_learning_events_entity
    ON learning_events (entity_type, entity_id)
    WHERE deleted_at IS NULL;

