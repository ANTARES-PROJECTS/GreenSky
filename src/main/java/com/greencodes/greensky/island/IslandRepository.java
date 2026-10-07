package com.greencodes.greensky.island;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistência de ilhas, membros e permissões. Só SQL, sem regra de negócio. Os métodos
 * recebem a {@link Connection} para que o serviço componha várias operações numa transação.
 */
public final class IslandRepository {

    /** SQLSTATE de violação de unicidade no PostgreSQL. */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final String SELECT_ISLAND =
            "SELECT id, owner_uuid, slot, center_x, center_z, size, state FROM islands ";

    /** Reserva o próximo slot. Único mesmo sob concorrência (sequência do banco). */
    public long nextSlot(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT nextval('island_slot_seq')");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /**
     * @throws IslandException ALREADY_HAS_ISLAND se o dono já tem uma ilha
     */
    public void insert(Connection c, Island island) throws SQLException {
        String sql = "INSERT INTO islands (id, owner_uuid, slot, center_x, center_z, size, state) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, island.id());
            ps.setObject(2, island.owner());
            ps.setLong(3, island.slot());
            ps.setInt(4, island.region().centerX());
            ps.setInt(5, island.region().centerZ());
            ps.setInt(6, island.region().size());
            ps.setString(7, island.state().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new IslandException(IslandException.Reason.ALREADY_HAS_ISLAND);
            }
            throw e;
        }
    }

    public Optional<Island> findByOwner(Connection c, UUID owner) throws SQLException {
        return findOne(c, SELECT_ISLAND + "WHERE owner_uuid = ?", owner);
    }

    public Optional<Island> findById(Connection c, UUID id) throws SQLException {
        return findOne(c, SELECT_ISLAND + "WHERE id = ?", id);
    }

    public void updateState(Connection c, UUID id, IslandState state) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE islands SET state = ? WHERE id = ?")) {
            ps.setString(1, state.name());
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * @throws IslandException ALREADY_MEMBER se o jogador já é membro
     */
    public void addMember(Connection c, UUID islandId, UUID player, IslandRole role, Set<IslandPermission> permissions)
            throws SQLException {
        try (PreparedStatement ps =
                c.prepareStatement("INSERT INTO island_members (island_id, player_uuid, role) VALUES (?, ?, ?)")) {
            ps.setObject(1, islandId);
            ps.setObject(2, player);
            ps.setString(3, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new IslandException(IslandException.Reason.ALREADY_MEMBER);
            }
            throw e;
        }
        insertPermissions(c, islandId, player, permissions);
    }

    /** @return true se havia o membro e ele foi removido (as permissões saem em cascata) */
    public boolean removeMember(Connection c, UUID islandId, UUID player) throws SQLException {
        try (PreparedStatement ps =
                c.prepareStatement("DELETE FROM island_members WHERE island_id = ? AND player_uuid = ?")) {
            ps.setObject(1, islandId);
            ps.setObject(2, player);
            return ps.executeUpdate() > 0;
        }
    }

    public void setPermissions(Connection c, UUID islandId, UUID player, Set<IslandPermission> permissions)
            throws SQLException {
        try (PreparedStatement ps =
                c.prepareStatement("DELETE FROM island_permissions WHERE island_id = ? AND player_uuid = ?")) {
            ps.setObject(1, islandId);
            ps.setObject(2, player);
            ps.executeUpdate();
        }
        insertPermissions(c, islandId, player, permissions);
    }

    public Optional<IslandMember> findMember(Connection c, UUID islandId, UUID player) throws SQLException {
        return members(c, islandId).stream().filter(m -> m.playerId().equals(player)).findFirst();
    }

    public List<IslandMember> members(Connection c, UUID islandId) throws SQLException {
        Map<UUID, Set<IslandPermission>> permissions = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT player_uuid, permission FROM island_permissions WHERE island_id = ?")) {
            ps.setObject(1, islandId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions
                            .computeIfAbsent(rs.getObject(1, UUID.class), k -> EnumSet.noneOf(IslandPermission.class))
                            .add(IslandPermission.valueOf(rs.getString(2)));
                }
            }
        }
        List<IslandMember> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT player_uuid, role FROM island_members WHERE island_id = ? ORDER BY joined_at, player_uuid")) {
            ps.setObject(1, islandId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID player = rs.getObject(1, UUID.class);
                    result.add(new IslandMember(
                            islandId,
                            player,
                            IslandRole.valueOf(rs.getString(2)),
                            permissions.getOrDefault(player, EnumSet.noneOf(IslandPermission.class))));
                }
            }
        }
        return result;
    }

    private void insertPermissions(Connection c, UUID islandId, UUID player, Set<IslandPermission> permissions)
            throws SQLException {
        if (permissions.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO island_permissions (island_id, player_uuid, permission) VALUES (?, ?, ?)")) {
            for (IslandPermission permission : permissions) {
                ps.setObject(1, islandId);
                ps.setObject(2, player);
                ps.setString(3, permission.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Optional<Island> findOne(Connection c, String sql, UUID key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Island(
                        rs.getObject("id", UUID.class),
                        rs.getObject("owner_uuid", UUID.class),
                        rs.getLong("slot"),
                        new IslandRegion(rs.getInt("center_x"), rs.getInt("center_z"), rs.getInt("size")),
                        IslandState.valueOf(rs.getString("state"))));
            }
        }
    }
}
