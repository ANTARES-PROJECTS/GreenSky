-- Fase 2: base do schema. Metadados apenas; blocos e chunks ficam no Minecraft/Paper.
-- Demais tabelas (islands, quests, ...) chegam junto com as fases que as usam.
CREATE TABLE players (
    uuid       UUID         PRIMARY KEY,
    name       VARCHAR(16)  NOT NULL,
    first_seen TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX players_name_idx ON players (lower(name));
