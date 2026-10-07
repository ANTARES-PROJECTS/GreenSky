package com.greencodes.greensky.island;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.world.SkyWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;

/**
 * Ilha inicial gerada por código: um disco de grama sobre terra afunilando para baixo, uma
 * pedra de bedrock no centro e uma árvore. Só toca os chunks do próprio disco.
 */
public final class StarterIslandBuilder implements IslandBuilder {

    /** Chunks de folga em volta do disco, para a copa da árvore não cair em chunk descarregado. */
    private static final int TREE_MARGIN = 3;

    private final SkyWorld skyWorld;
    private final GreenScheduler scheduler;
    private final IslandSettings settings;

    public StarterIslandBuilder(SkyWorld skyWorld, GreenScheduler scheduler, IslandSettings settings) {
        this.skyWorld = skyWorld;
        this.scheduler = scheduler;
        this.settings = settings;
    }

    @Override
    public CompletableFuture<Void> build(IslandRegion region) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        // A API de mundo só pode ser usada na thread do servidor.
        scheduler.runSync(() -> {
            try {
                World world = skyWorld.bukkit();
                int reach = settings.starterRadius() + TREE_MARGIN;
                List<CompletableFuture<?>> chunks = new ArrayList<>();
                for (int cx = (region.centerX() - reach) >> 4; cx <= (region.centerX() + reach) >> 4; cx++) {
                    for (int cz = (region.centerZ() - reach) >> 4; cz <= (region.centerZ() + reach) >> 4; cz++) {
                        chunks.add(world.getChunkAtAsync(cx, cz));
                    }
                }
                CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
                    if (error != null) {
                        result.completeExceptionally(error);
                        return;
                    }
                    scheduler.runSync(() -> {
                        try {
                            place(world, region);
                            result.complete(null);
                        } catch (Throwable t) {
                            result.completeExceptionally(t);
                        }
                    });
                });
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result;
    }

    private void place(World world, IslandRegion region) {
        int r = settings.starterRadius();
        int y = settings.baseY();
        int cx = region.centerX();
        int cz = region.centerZ();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= r * r + r) {
                    set(world, cx + dx, y, cz + dz, Material.GRASS_BLOCK);
                    set(world, cx + dx, y - 1, cz + dz, Material.DIRT);
                }
                if (d2 <= (r - 1) * (r - 1) + (r - 1)) {
                    set(world, cx + dx, y - 2, cz + dz, Material.DIRT);
                }
                if (d2 <= Math.max(0, (r - 2) * (r - 2))) {
                    set(world, cx + dx, y - 3, cz + dz, Material.DIRT);
                }
            }
        }
        set(world, cx, y - 3, cz, Material.BEDROCK);

        // Semente fixa pela posição: refazer a ilha (recuperação após crash) gera a mesma árvore.
        // A geração pode falhar; a ilha continua válida sem ela.
        Random random = new Random(31L * cx + cz);
        world.generateTree(new Location(world, cx + 2, y + 1, cz + 2), random, TreeType.TREE);
    }

    private static void set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
