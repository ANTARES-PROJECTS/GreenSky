-- Fase 7: configurações da ilha (por enquanto, só a visibilidade para visitas).
CREATE TABLE island_settings (
    island_id  UUID         PRIMARY KEY REFERENCES islands (id) ON DELETE CASCADE,
    visibility VARCHAR(16)  NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Ilhas que já existiam ficam públicas (o padrão do config.yml só vale para ilhas novas).
INSERT INTO island_settings (island_id, visibility)
SELECT id, 'PUBLIC' FROM islands;
