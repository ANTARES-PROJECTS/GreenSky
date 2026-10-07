package com.greencodes.greensky.protection;

import com.greencodes.greensky.island.Island;
import com.greencodes.greensky.island.IslandListener;
import com.greencodes.greensky.island.IslandMember;
import com.greencodes.greensky.island.IslandPermission;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decide quem pode fazer o quê em cada ponto do mundo SkyBlock. Esta é a proteção real
 * (o WorldBorder por jogador é só visual). Não depende do Bukkit: recebe coordenadas e
 * UUIDs, o que a torna testável; os listeners apenas traduzem eventos em chamadas aqui.
 *
 * <p>Regras, em ordem:
 * <ol>
 *   <li>Antes de {@link #load} terminar, tudo é negado (padrão seguro).
 *   <li>Fora da região de qualquer ilha (vazio, spawn) é negado.
 *   <li>Dentro de uma ilha, só quem é membro dela e tem a permissão pode.
 * </ol>
 * O bypass de administradores é decidido nos listeners, não aqui.
 */
public final class IslandProtectionService implements IslandListener {

    private final IslandIndex index = new IslandIndex();
    /** jogador online -> (id da ilha -> participação). Preenchido no join; ausente = nenhuma. */
    private final ConcurrentHashMap<UUID, Map<UUID, IslandMember>> memberships = new ConcurrentHashMap<>();

    private volatile boolean ready;

    /** Carrega o índice com as ilhas existentes e libera a proteção para decidir. */
    public void load(Collection<Island> islands) {
        islands.forEach(index::put);
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public void playerJoined(UUID player, Collection<IslandMember> members) {
        Map<UUID, IslandMember> map = new ConcurrentHashMap<>();
        for (IslandMember member : members) {
            map.put(member.islandId(), member);
        }
        memberships.put(player, map);
    }

    public void playerQuit(UUID player) {
        memberships.remove(player);
    }

    public Optional<Island> islandAt(int x, int z) {
        return index.at(x, z);
    }

    /** O jogador pode agir com essa permissão no bloco (x, z)? */
    public boolean can(UUID player, IslandPermission permission, int x, int z) {
        return memberAt(player, x, z).map(m -> m.can(permission)).orElse(false);
    }

    /** O jogador é dono ou membro da ilha que contém (x, z)? */
    public boolean isMemberAt(UUID player, int x, int z) {
        return memberAt(player, x, z).isPresent();
    }

    /**
     * Os dois pontos estão na mesma ilha? Falso se qualquer um estiver fora de todas as ilhas.
     * Usado para impedir que efeitos (líquidos, pistões, explosões) cruzem fronteiras.
     */
    public boolean sameIsland(int x1, int z1, int x2, int z2) {
        if (!ready) {
            return false;
        }
        Optional<Island> a = index.at(x1, z1);
        if (a.isEmpty()) {
            return false;
        }
        Optional<Island> b = index.at(x2, z2);
        return b.isPresent() && a.get().id().equals(b.get().id());
    }

    private Optional<IslandMember> memberAt(UUID player, int x, int z) {
        if (!ready) {
            return Optional.empty();
        }
        Map<UUID, IslandMember> mine = memberships.get(player);
        if (mine == null) {
            return Optional.empty();
        }
        return index.at(x, z).map(island -> mine.get(island.id()));
    }

    // --- Mudanças vindas do IslandService (threads do banco) ---

    @Override
    public void onIslandCreated(Island island, IslandMember owner) {
        index.put(island);
        Map<UUID, IslandMember> mine = memberships.get(owner.playerId());
        if (mine != null) {
            mine.put(owner.islandId(), owner);
        }
    }

    @Override
    public void onMemberAdded(IslandMember member) {
        Map<UUID, IslandMember> mine = memberships.get(member.playerId());
        if (mine != null) {
            mine.put(member.islandId(), member);
        }
    }

    @Override
    public void onMemberRemoved(UUID islandId, UUID playerId) {
        Map<UUID, IslandMember> mine = memberships.get(playerId);
        if (mine != null) {
            mine.remove(islandId);
        }
    }

    @Override
    public void onMemberPermissionsChanged(IslandMember member) {
        onMemberAdded(member);
    }
}
