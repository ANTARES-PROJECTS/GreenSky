package com.greencodes.greensky.island;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissões de um membro dentro da ilha. Esta fase só as guarda; a fase 5 (Protection) é
 * quem as aplica. O dono tem todas, implicitamente.
 */
public enum IslandPermission {
    BUILD,
    BREAK,
    INTERACT,
    CONTAINERS,
    INVITE,
    KICK,
    MANAGE_PERMISSIONS;

    /** Permissões de um membro recém-adicionado: construir e usar, sem administrar. */
    public static Set<IslandPermission> defaultsForMember() {
        return EnumSet.of(BUILD, BREAK, INTERACT, CONTAINERS);
    }
}
