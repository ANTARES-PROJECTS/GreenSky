package com.greencodes.greensky.collection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistência de {@code collection_progress}. Recebe a conexão para compor transações. */
public final class CollectionRepository {

    /** chave da entrada -> quantidade obtida. */
    public Map<String, Long> load(Connection c, UUID player) throws SQLException {
        Map<String, Long> result = new HashMap<>();
        try (PreparedStatement ps =
                c.prepareStatement("SELECT entry_key, amount FROM collection_progress WHERE player_uuid = ?")) {
            ps.setObject(1, player);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return result;
    }

    /** Soma as quantidades (cria a linha na primeira vez). Um único lote por chamada. */
    public void addAmounts(Connection c, UUID player, Map<String, Long> deltas) throws SQLException {
        if (deltas.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO collection_progress (player_uuid, entry_key, amount) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, entry_key) DO UPDATE "
                + "SET amount = collection_progress.amount + EXCLUDED.amount, updated_at = now()";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Map.Entry<String, Long> delta : deltas.entrySet()) {
                ps.setObject(1, player);
                ps.setString(2, delta.getKey());
                ps.setLong(3, delta.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
