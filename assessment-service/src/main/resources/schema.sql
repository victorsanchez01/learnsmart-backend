-- Extensión para uuid si no la tienes
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--------------------------------------------------
-- 0) Limpieza (Solo desarrollo)
--------------------------------------------------
DROP TABLE IF EXISTS user_skill_mastery CASCADE;
DROP TABLE IF EXISTS user_item_responses CASCADE;
DROP TABLE IF EXISTS user_assessment_sessions CASCADE;
DROP TABLE IF EXISTS assessment_item_options CASCADE;
DROP TABLE IF EXISTS assessment_item_skills CASCADE;
DROP TABLE IF EXISTS assessment_items CASCADE;

--------------------------------------------------
-- 1) Banco de ítems
--------------------------------------------------

CREATE TABLE IF NOT EXISTS assessment_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain_id       UUID NOT NULL,
    origin          VARCHAR(20) NOT NULL DEFAULT 'static',
    type            VARCHAR(30) NOT NULL,
    stem            TEXT NOT NULL,
    difficulty      NUMERIC(3,2),
    metadata        TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assessment_items_domain ON assessment_items(domain_id);
CREATE INDEX IF NOT EXISTS idx_assessment_items_origin ON assessment_items(origin);
CREATE INDEX IF NOT EXISTS idx_assessment_items_type   ON assessment_items(type);
CREATE INDEX IF NOT EXISTS idx_assessment_items_active ON assessment_items(is_active);

--------------------------------------------------
-- 2) Relación ítem ↔ skills
--------------------------------------------------

CREATE TABLE IF NOT EXISTS assessment_item_skills (
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id) ON DELETE CASCADE,
    skill_id            UUID NOT NULL,
    weight              NUMERIC(3,2) NOT NULL DEFAULT 1.0,
    PRIMARY KEY (assessment_item_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_item_skills_skill ON assessment_item_skills(skill_id);

--------------------------------------------------
-- 3) Opciones de ítems de elección múltiple
--------------------------------------------------

CREATE TABLE IF NOT EXISTS assessment_item_options (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id) ON DELETE CASCADE,
    label               VARCHAR(10),
    statement           TEXT NOT NULL,
    is_correct          BOOLEAN NOT NULL DEFAULT FALSE,
    error_tag           VARCHAR(100),
    feedback_template   TEXT
);

CREATE INDEX IF NOT EXISTS idx_options_item ON assessment_item_options(assessment_item_id);

--------------------------------------------------
-- 4) Sesiones de evaluación
--------------------------------------------------

CREATE TABLE IF NOT EXISTS user_assessment_sessions_v2 (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'in_progress',
    plan_id         UUID,
    module_id       UUID,
    config          TEXT,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    CONSTRAINT chk_session_status CHECK (status IN ('in_progress','completed','cancelled'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_user          ON user_assessment_sessions_v2(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status        ON user_assessment_sessions_v2(status);
CREATE INDEX IF NOT EXISTS idx_sessions_plan_module   ON user_assessment_sessions_v2(plan_id, module_id);

--------------------------------------------------
-- 5) Respuestas a ítems
--------------------------------------------------

CREATE TABLE IF NOT EXISTS user_item_responses (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id          UUID NOT NULL REFERENCES user_assessment_sessions_v2(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL,
    assessment_item_id  UUID NOT NULL REFERENCES assessment_items(id),
    selected_option_id  UUID,
    response_payload    TEXT,
    is_correct          BOOLEAN,
    response_time_ms    INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_responses_session ON user_item_responses(session_id);
CREATE INDEX IF NOT EXISTS idx_responses_user    ON user_item_responses(user_id);
CREATE INDEX IF NOT EXISTS idx_responses_item    ON user_item_responses(assessment_item_id);

--------------------------------------------------
-- 6) Estado de dominio por skill (knowledge state)
--------------------------------------------------

CREATE TABLE IF NOT EXISTS user_skill_mastery (
    user_id         UUID NOT NULL,
    skill_id        UUID NOT NULL,
    mastery         NUMERIC(4,3) NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    last_update     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_mastery_skill ON user_skill_mastery(skill_id);
