package com.greencodes.greensky.farming;

import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.content.ItemFactory;
import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/** Plantio ancestral e proteção das culturas especiais ou fertilizadas contra perdas por automação. */
public final class AncientPlantListener implements Listener {
    public static final String SEED = "crop.ancient_seed";
    private final AncientPlants plants;
    private final CropMarkers fertilized;
    private final ItemFactory items;
    private final IslandProtectionService protection;
    private final SkyWorld world;
    private final Predicate<Player> ready;
    private final CollectionTracker collections;

    private boolean protectedBlock(org.bukkit.block.Block block) {
        return plants.protectedBlock(block) || fertilized.protectedBlock(block);
    }

    private void clear(org.bukkit.block.Block block) {
        plants.set(block, false);
        fertilized.set(block, false);
    }
    public AncientPlantListener(AncientPlants plants, CropMarkers fertilized, ItemFactory items, IslandProtectionService protection,
            SkyWorld world, Predicate<Player> ready, CollectionTracker collections) {
        this.plants = plants; this.fertilized = fertilized; this.items = items; this.protection = protection;
        this.world = world; this.ready = ready; this.collections = collections;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlant(BlockPlaceEvent event) {
        if (!items.idOf(event.getItemInHand()).filter(SEED::equals).isPresent()) return;
        var block = event.getBlockPlaced();
        if (!event.canBuild() || event.getPlayer().getGameMode() != org.bukkit.GameMode.SURVIVAL
                || block.getType() != Material.WHEAT || !world.contains(block.getWorld())
                || !ready.test(event.getPlayer()) || !collections.isLoaded(event.getPlayer())
                || !protection.can(event.getPlayer().getUniqueId(), IslandPermission.BUILD, block.getX(), block.getZ())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent event) {
        if (!event.canBuild() || !world.contains(event.getBlockPlaced().getWorld())) return;
        fertilized.set(event.getBlockPlaced(), false);
        plants.set(event.getBlockPlaced(), event.getBlockPlaced().getType() == Material.WHEAT
                && items.idOf(event.getItemInHand()).filter(SEED::equals).isPresent());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakSoil(BlockBreakEvent event) {
        var block = event.getBlock();
        if (!world.contains(block.getWorld())) return;
        if (block.getType() == Material.FARMLAND && protectedBlock(block)) event.setCancelled(true);
        if ((plants.contains(block) || fertilized.contains(block))
                && (!ready.test(event.getPlayer()) || !collections.isLoaded(event.getPlayer())
                || !protection.can(event.getPlayer().getUniqueId(), IslandPermission.BREAK, block.getX(), block.getZ()))) {
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCancelledDrops(BlockDropItemEvent event) {
        // Cancelar os drops não restaura o bloco quebrado; não deixar o solo preso a um marcador vazio.
        if (event.isCancelled() && world.contains(event.getBlock().getWorld())) clear(event.getBlock());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemovedWithoutDrops(BlockBreakEvent event) {
        if (world.contains(event.getBlock().getWorld())
                && (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE || !event.isDropItems())) {
            clear(event.getBlock());
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        if (world.contains(event.getToBlock().getWorld()) && protectedBlock(event.getToBlock())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (world.contains(event.getBlock().getWorld()) && protectedBlock(event.getBlock())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (world.contains(event.getBlock().getWorld()) && protectedBlock(event.getBlock())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (world.contains(event.getBlock().getWorld())
                && (protectedBlock(event.getBlock().getRelative(event.getDirection()))
                || event.getBlocks().stream().anyMatch(block -> protectedBlock(block)
                        || protectedBlock(block.getRelative(event.getDirection()))))) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (world.contains(event.getBlock().getWorld()) && event.getBlocks().stream().anyMatch(block ->
                protectedBlock(block) || protectedBlock(block.getRelative(event.getDirection()))
                        || protectedBlock(block.getRelative(event.getDirection().getOppositeFace())))) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (world.contains(event.getLocation().getWorld())) event.blockList().removeIf(this::protectedBlock);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (world.contains(event.getBlock().getWorld())) event.blockList().removeIf(this::protectedBlock);
    }
}
