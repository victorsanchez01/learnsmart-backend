-- Extensión para uuid si no la tienes
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS learning_events_v2 CASCADE;

CREATE TABLE IF NOT EXISTS learning_events_v2 (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_learning_events_user_occurred ON learning_events_v2 (user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_learning_events_type_occurred ON learning_events_v2 (event_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_learning_events_entity ON learning_events_v2 (entity_type, entity_id);
