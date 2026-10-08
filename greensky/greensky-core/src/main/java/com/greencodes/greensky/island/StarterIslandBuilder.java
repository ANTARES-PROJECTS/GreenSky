package com.greencodes.greensky.island;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.world.SkyWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Aplica um blueprint determinístico com estilo, terreno, lago e kit inicial.
 * Carrega chunks de forma assíncrona e coloca blocos somente na thread do servidor.
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

        StarterIslandPlan plan = StarterIslandPlan.create(cx, cz, r);
        for (var block : plan.blocks()) {
            if (!region.contains(cx + block.x(), cz + block.z())) continue;
            var placed = world.getBlockAt(cx + block.x(), y + block.y(), cz + block.z());
            placed.setType(block.material(), false);
            if (placed.getBlockData() instanceof org.bukkit.block.data.type.Leaves leaves) {
                leaves.setPersistent(true);
                placed.setBlockData(leaves, false);
            }
        }
        if (r >= 3 && world.getBlockAt(cx - 2, y + 1, cz + 2).getState() instanceof org.bukkit.block.Chest chest) {
            chest.getBlockInventory().clear();
            chest.getBlockInventory().addItem(
                    org.bukkit.inventory.ItemStack.of(Material.WHEAT_SEEDS, 8),
                    org.bukkit.inventory.ItemStack.of(Material.CARROT, 2),
                    org.bukkit.inventory.ItemStack.of(Material.POTATO, 2),
                    org.bukkit.inventory.ItemStack.of(Material.WOODEN_HOE),
                    org.bukkit.inventory.ItemStack.of(Material.FISHING_ROD),
                    org.bukkit.inventory.ItemStack.of(Material.BREAD, 12),
                    org.bukkit.inventory.ItemStack.of(Material.BONE_MEAL, 12),
                    org.bukkit.inventory.ItemStack.of(Material.WATER_BUCKET),
                    org.bukkit.inventory.ItemStack.of(Material.LAVA_BUCKET),
                    org.bukkit.inventory.ItemStack.of(Material.OAK_PLANKS, 16),
                    org.bukkit.inventory.ItemStack.of(Material.COBBLESTONE, 24),
                    org.bukkit.inventory.ItemStack.of(plan.theme().sapling(), 2));
        }
    }
}
