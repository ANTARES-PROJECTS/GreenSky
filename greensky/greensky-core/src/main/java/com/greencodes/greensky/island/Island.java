package com.greencodes.greensky.island;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

/** Uma ilha. Imutável: mudanças de estado geram uma nova instância. */
public record Island(UUID id, UUID owner, long slot, IslandRegion region, IslandState state) {

    public Island withState(IslandState newState) {
        return new Island(id, owner, slot, region, newState);
    }

    public boolean isReady() {
        return state == IslandState.READY;
    }

    /** Ponto de chegada: centro da ilha, sobre a grama. */
    public Location home(World world, int baseY) {
        return new Location(world, region.centerX() + 0.5, baseY + 1, region.centerZ() + 0.5);
    }
}
