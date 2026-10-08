package com.greencodes.greensky.world;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * O mundo SkyBlock: um único mundo Bukkit que hospeda todas as ilhas (cada uma ocupa
 * uma região física própria). Acesso ao {@link World} só na thread do servidor.
 */
public final class SkyWorld {

    private final World world;
    private final int spawnY;

    SkyWorld(World world, int spawnY) {
        this.world = world;
        this.spawnY = spawnY;
    }

    public World bukkit() {
        return world;
    }

    public String name() {
        return world.getName();
    }

    /** Spawn fixo do mundo (centro de x=0, z=0). */
    public Location spawn() {
        return new Location(world, 0.5, spawnY, 0.5);
    }

    public boolean contains(World other) {
        return world.equals(other);
    }
}
