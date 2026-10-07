package com.greencodes.greensky.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/** Persistência da tabela {@code players}. Métodos recebem a conexão para compor transações. */
public final class PlayerRepository {

    /** Cria o jogador ou atualiza nome e último acesso. */
    public void upsert(Connection connection, UUID uuid, String name) throws SQLException {
        String sql = "INSERT INTO players (uuid, name) VALUES (?, ?) "
                + "ON CONFLICT (uuid) DO UPDATE SET name = EXCLUDED.name, last_seen = now()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }
}
