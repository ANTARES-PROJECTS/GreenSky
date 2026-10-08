package com.greencodes.greensky.generation;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/**
 * Gerador de mundo vazio (void). Não sobrescreve nenhuma etapa de geração e todos os
 * {@code shouldGenerate*} do {@link ChunkGenerator} ficam no padrão {@code false}: não há
 * ruído, superfície, cavernas, decorações, mobs nem estruturas vanilla. As ilhas
 * (fase 4) serão colocadas por cima, a partir de templates.
 */
public final class VoidGenerator extends ChunkGenerator {

    private final int spawnY;

    public VoidGenerator(int spawnY) {
        this.spawnY = spawnY;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new SingleBiomeProvider(Biome.PLAINS);
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, spawnY, 0.5);
    }

    /** Sem chão não há onde nascer; o spawn é sempre o fixo acima. */
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return false;
    }
}
