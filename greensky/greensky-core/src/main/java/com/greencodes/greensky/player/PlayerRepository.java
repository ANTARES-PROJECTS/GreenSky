package com.greencodes.greensky.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Persistência da tabela {@code players}. Métodos recebem a conexão para compor transações. */
public final class PlayerRepository {

    private static final String UNIQUE_VIOLATION = "23505";

    public Optional<PlayerRecord> findByUuid(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT uuid, name FROM players WHERE uuid = ?")) {
            query.setObject(1, uuid);
            try (ResultSet result = query.executeQuery()) {
                return result.next() ? Optional.of(new PlayerRecord(result.getObject(1, UUID.class), result.getString(2)))
                        : Optional.empty();
            }
        }
    }

    /**
     * Cria o jogador ou atualiza nome e último acesso.
     *
     * @throws NameTakenException se o nick (sem diferenciar maiúsculas) já for de outra conta
     */
    public void upsert(Connection connection, UUID uuid, String name) throws SQLException {
        // Há duas unicidades (uuid e lower(name)). "ON CONFLICT DO NOTHING" sem alvo cobre as duas, inclusive
        // quando duas transações da MESMA conta inserem ao mesmo tempo (a segunda espera e é ignorada).
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO players (uuid, name) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            insert.setObject(1, uuid);
            insert.setString(2, name);
            insert.executeUpdate();
        }
        // Então atualiza pela identidade (UUID). Se não existe linha com este UUID, o insert foi ignorado
        // porque o nick já é de OUTRA conta.
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE players SET name = ?, last_seen = now() WHERE uuid = ?")) {
            update.setString(1, name);
            update.setObject(2, uuid);
            if (update.executeUpdate() == 0) {
                throw new NameTakenException(name);
            }
        } catch (SQLException e) {
            // A própria conta trocou para uma grafia que pertence a outra conta.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new NameTakenException(name);
            }
            throw e;
        }
    }

    /** Busca pelo nick sem diferenciar maiúsculas (usa o índice único em lower(name)). */
    public Optional<PlayerRecord> findByNameIgnoreCase(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps =
                connection.prepareStatement("SELECT uuid, name FROM players WHERE lower(name) = lower(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new PlayerRecord(rs.getObject(1, UUID.class), rs.getString(2)))
                        : Optional.empty();
            }
        }
    }
}
