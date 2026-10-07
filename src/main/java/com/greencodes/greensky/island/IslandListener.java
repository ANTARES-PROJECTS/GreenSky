package com.greencodes.greensky.island;

import java.util.UUID;

/**
 * Notificações de mudança nas ilhas, disparadas pelo {@link IslandService} depois que a
 * alteração foi gravada no banco. Os métodos são chamados em threads do banco, não na
 * thread do servidor: implementações devem ser thread-safe e rápidas.
 */
public interface IslandListener {

    /** Ilha criada (linha já gravada; os blocos ainda podem estar sendo gerados). */
    default void onIslandCreated(Island island, IslandMember owner) {}

    default void onMemberAdded(IslandMember member) {}

    default void onMemberRemoved(UUID islandId, UUID playerId) {}

    default void onMemberPermissionsChanged(IslandMember member) {}
}
