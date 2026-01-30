CREATE TABLE domains (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,   -- 'math', 'english'
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE skills (
    id          UUID PRIMARY KEY,
    domain_id   UUID NOT NULL REFERENCES domains(id),
    code        VARCHAR(100) NOT NULL,        -- 'fractions_addition'
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    level       VARCHAR(50),                  -- 'A1', 'B2', etc. o tu propia escala
    tags        TEXT[],                       -- ['conceptual','basic']
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (domain_id, code)
);

CREATE TABLE skill_prerequisites (
    skill_id        UUID NOT NULL REFERENCES skills(id),
    prerequisite_id UUID NOT NULL REFERENCES skills(id),
    PRIMARY KEY (skill_id, prerequisite_id)
);

CREATE TABLE content_items (
    id                UUID PRIMARY KEY,
    domain_id         UUID NOT NULL REFERENCES domains(id),
    type              VARCHAR(30) NOT NULL,   -- 'lesson','quiz','practice','video'
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    estimated_minutes INT,
    difficulty        NUMERIC(3,2),          -- 0.00 - 1.00
    metadata          JSONB,                 -- url, formato, fuente, etc.
    is_active         BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE content_item_skills (
    content_item_id UUID NOT NULL REFERENCES content_items(id),
    skill_id        UUID NOT NULL REFERENCES skills(id),
    weight          NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    PRIMARY KEY (content_item_id, skill_id)
);
