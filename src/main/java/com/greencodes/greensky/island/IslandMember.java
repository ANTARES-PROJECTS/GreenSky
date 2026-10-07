package com.greencodes.greensky.island;

import java.util.Set;
import java.util.UUID;

public record IslandMember(UUID islandId, UUID playerId, IslandRole role, Set<IslandPermission> permissions) {

    public IslandMember {
        permissions = Set.copyOf(permissions);
    }

    /** O dono pode tudo; membros, apenas o que foi concedido. */
    public boolean can(IslandPermission permission) {
        return role == IslandRole.OWNER || permissions.contains(permission);
    }
}
