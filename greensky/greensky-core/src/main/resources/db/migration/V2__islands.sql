-- Fase 4: ilhas, membros e permissões. Só metadados; os blocos ficam no mundo do Paper.

-- Cada ilha recebe uma posição (slot) única e nunca reutilizada. O slot 0 é a origem do mundo
-- (spawn), então a sequência começa em 1. Buracos na numeração são aceitáveis.
CREATE SEQUENCE island_slot_seq START 1;

CREATE TABLE islands (
    id         UUID         PRIMARY KEY,
    owner_uuid UUID         NOT NULL REFERENCES players (uuid),
    slot       BIGINT       NOT NULL UNIQUE,
    -- Centro gravado na criação: mudar island.spacing no config não move ilhas existentes.
    center_x   INTEGER      NOT NULL,
    center_z   INTEGER      NOT NULL,
    size       INTEGER      NOT NULL CHECK (size > 0),
    -- PENDING = linha criada mas blocos ainda não confirmados (recuperável após crash).
    state      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (state IN ('PENDING', 'READY')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Um jogador é dono de no máximo uma ilha; garante a regra mesmo sob criações simultâneas.
CREATE UNIQUE INDEX islands_owner_uidx ON islands (owner_uuid);

CREATE TABLE island_members (
    island_id   UUID         NOT NULL REFERENCES islands (id) ON DELETE CASCADE,
    player_uuid UUID         NOT NULL REFERENCES players (uuid),
    role        VARCHAR(16)  NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (island_id, player_uuid)
);

CREATE INDEX island_members_player_idx ON island_members (player_uuid);

CREATE TABLE island_permissions (
    island_id   UUID         NOT NULL,
    player_uuid UUID         NOT NULL,
    permission  VARCHAR(32)  NOT NULL,
    PRIMARY KEY (island_id, player_uuid, permission),
    FOREIGN KEY (island_id, player_uuid) REFERENCES island_members (island_id, player_uuid) ON DELETE CASCADE
);
