CREATE TABLE IF NOT EXISTS domains (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS skills (
    id          UUID PRIMARY KEY,
    domain_id   UUID NOT NULL REFERENCES domains(id),
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    level       VARCHAR(50),
    tags        TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (domain_id, code)
);

CREATE TABLE IF NOT EXISTS skill_prerequisites (
    skill_id        UUID NOT NULL REFERENCES skills(id),
    prerequisite_id UUID NOT NULL REFERENCES skills(id),
    PRIMARY KEY (skill_id, prerequisite_id)
);

CREATE TABLE IF NOT EXISTS content_items (
    id                UUID PRIMARY KEY,
    domain_id         UUID NOT NULL REFERENCES domains(id),
    type              VARCHAR(30) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    estimated_minutes INT,
    difficulty        NUMERIC(3,2),
    metadata          JSONB,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS content_item_skills (
    content_item_id UUID NOT NULL REFERENCES content_items(id),
    skill_id        UUID NOT NULL REFERENCES skills(id),
    weight          NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    PRIMARY KEY (content_item_id, skill_id)
);
