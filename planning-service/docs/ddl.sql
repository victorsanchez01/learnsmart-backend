CREATE TABLE learning_plans (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,          -- viene de Keycloak/auth-service
    goal_id         UUID,                  -- id lógico del profile-service
    status          VARCHAR(20) NOT NULL DEFAULT 'active',  -- 'active','completed','cancelled'
    start_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date        DATE,
    hours_per_week  NUMERIC(4,1),
    generated_by    VARCHAR(20) NOT NULL DEFAULT 'ai',      -- 'ai','manual'
    raw_plan_ai     JSONB,                                  -- respuesta completa del modelo
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plan_modules (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    position        INT NOT NULL,                   -- orden en el plan
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    estimated_hours NUMERIC(5,2),
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',  -- 'pending','in_progress','done','skipped'
    target_skills   TEXT[],                         -- códigos de skills del content-service
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (plan_id, position)
);

CREATE TABLE plan_activities (
    id                      UUID PRIMARY KEY,
    module_id               UUID NOT NULL REFERENCES plan_modules(id) ON DELETE CASCADE,
    position                INT NOT NULL,
    activity_type           VARCHAR(30) NOT NULL,     -- 'lesson','quiz','practice','exam','other'
    status                  VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending','in_progress','done','skipped'
    content_ref             TEXT NOT NULL,           -- referencia que entiende el ai-service
    estimated_minutes       INT,
    override_estimated_minutes INT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (module_id, position)
);

CREATE TABLE plan_replans_history (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    reason          TEXT,
    request_payload JSONB,     -- lo que se mandó a IA
    response_payload JSONB,    -- respuesta de IA
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

