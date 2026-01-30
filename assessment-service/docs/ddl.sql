-- Extensión para uuid si no la tienes
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--------------------------------------------------
-- 1) Banco de ítems
--------------------------------------------------

CREATE TABLE assessment_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain_id       UUID NOT NULL,                         -- referencia lógica a content-service
    origin          VARCHAR(20) NOT NULL DEFAULT 'static', -- 'static' | 'ai_generated'
    type            VARCHAR(30) NOT NULL,                  -- 'multiple_choice','open','numeric'
    stem            TEXT NOT NULL,                         -- enunciado
    difficulty      NUMERIC(3,2),                          -- 0.00 - 1.00
    metadata        JSONB,                                 -- tags, error_tags, etc.
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_assessment_items_domain ON assessment_items(domain_id);
CREATE INDEX idx_assessment_items_origin ON assessment_items(origin);
CREATE INDEX idx_assessment_items_type   ON assessment_items(type);
CREATE INDEX idx_assessment_items_active ON assessment_items(is_active);

--------------------------------------------------
-- 2) Relación ítem ↔ skills
--------------------------------------------------

CREATE TABLE assessment_item_skills (
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id) ON DELETE CASCADE,
    skill_id            UUID NOT NULL,                   -- skill_id de content-service
    weight              NUMERIC(3,2) NOT NULL DEFAULT 1.0,
    PRIMARY KEY (assessment_item_id, skill_id)
);

CREATE INDEX idx_item_skills_skill ON assessment_item_skills(skill_id);

--------------------------------------------------
-- 3) Opciones de ítems de elección múltiple
--------------------------------------------------

CREATE TABLE assessment_item_options (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id) ON DELETE CASCADE,
    label               VARCHAR(10),                     -- 'A','B','C'...
    statement           TEXT NOT NULL,                   -- texto de la opción
    is_correct          BOOLEAN NOT NULL DEFAULT FALSE,
    error_tag           VARCHAR(100),                    -- para feedback personalizado
    feedback_template   TEXT                             -- mensaje específico para esta opción
);

CREATE INDEX idx_options_item ON assessment_item_options(assessment_item_id);

--------------------------------------------------
-- 4) Sesiones de evaluación
--------------------------------------------------

CREATE TABLE user_assessment_sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,                       -- del IdP (Keycloak)
    type            VARCHAR(20) NOT NULL,                -- 'diagnostic','practice','exam'
    status          VARCHAR(20) NOT NULL DEFAULT 'in_progress', -- 'in_progress','completed','cancelled'
    plan_id         UUID,                                -- referencia lógica a planning-service
    module_id       UUID,                                -- referencia lógica a planning-service
    config          JSONB,                               -- config sesión (nº items, skills foco, etc.)
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    CONSTRAINT chk_session_status CHECK (status IN ('in_progress','completed','cancelled'))
);

CREATE INDEX idx_sessions_user          ON user_assessment_sessions(user_id);
CREATE INDEX idx_sessions_status        ON user_assessment_sessions(status);
CREATE INDEX idx_sessions_plan_module   ON user_assessment_sessions(plan_id, module_id);

--------------------------------------------------
-- 5) Respuestas a ítems
--------------------------------------------------

CREATE TABLE user_item_responses (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id          UUID NOT NULL REFERENCES user_assessment_sessions(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL,
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id),
    selected_option_id  UUID,                            -- NULL si respuesta abierta
    response_payload    JSONB,                           -- texto libre, número, etc.
    is_correct          BOOLEAN,
    response_time_ms    INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_responses_session ON user_item_responses(session_id);
CREATE INDEX idx_responses_user    ON user_item_responses(user_id);
CREATE INDEX idx_responses_item    ON user_item_responses(assessment_item_id);

--------------------------------------------------
-- 6) Estado de dominio por skill (knowledge state)
--------------------------------------------------

CREATE TABLE user_skill_mastery (
    user_id         UUID NOT NULL,
    skill_id        UUID NOT NULL,       -- skill_id de content-service
    mastery         NUMERIC(4,3) NOT NULL,   -- 0.000 - 1.000
    attempts        INT NOT NULL DEFAULT 0,
    last_update     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, skill_id)
);

CREATE INDEX idx_mastery_skill ON user_skill_mastery(skill_id);

