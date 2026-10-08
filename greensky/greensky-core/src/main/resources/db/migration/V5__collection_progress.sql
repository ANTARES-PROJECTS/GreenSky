-- Fase 8.1: progresso de coleções por jogador. As coleções em si (nomes, entradas) ficam em
-- content/collections.yml; aqui só o que cada jogador já obteve e quantas vezes.
CREATE TABLE collection_progress (
    player_uuid UUID         NOT NULL REFERENCES players (uuid),
    entry_key   VARCHAR(64)  NOT NULL,
    amount      BIGINT       NOT NULL CHECK (amount > 0),
    first_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (player_uuid, entry_key)
);
