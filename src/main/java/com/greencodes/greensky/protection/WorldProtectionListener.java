package com.greencodes.greensky.protection;

import com.greencodes.greensky.world.SkyWorld;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Efeitos que não vêm de um jogador (explosões, pistões, líquidos, fogo, hoppers, crescimento)
 * não podem cruzar a fronteira da ilha: tudo deve começar e terminar dentro da mesma região.
 * Fino: cada evento vira uma pergunta {@code sameIsland} ao {@link IslandProtectionService}.
 */
public final class WorldProtectionListener implements Listener {

    private final IslandProtectionService protection;
    private final SkyWorld skyWorld;

    public WorldProtectionListener(IslandProtectionService protection, SkyWorld skyWorld) {
        this.protection = protection;
        this.skyWorld = skyWorld;
    }

    /** Explosão só afeta blocos da ilha onde ocorreu; fora de qualquer ilha não destrói nada. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location origin = event.getLocation();
        if (skyWorld.contains(origin.getWorld())) {
            event.blockList().removeIf(block -> !same(origin.getBlockX(), origin.getBlockZ(), block));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Block origin = event.getBlock();
        if (isSky(origin)) {
            event.blockList().removeIf(block -> !same(origin, block));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (pistonCrosses(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (pistonCrosses(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    /** Líquido (e ovo de dragão) não escorre para fora da região da ilha. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        if (isSky(event.getBlock()) && !same(event.getBlock(), event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    /** Hopper e afins só movem itens entre inventários da mesma ilha. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMove(InventoryMoveItemEvent event) {
        Location from = event.getSource().getLocation();
        Location to = event.getDestination().getLocation();
        if (from == null || to == null || !(skyWorld.contains(from.getWorld()) || skyWorld.contains(to.getWorld()))) {
            return;
        }
        if (!protection.sameIsland(from.getBlockX(), from.getBlockZ(), to.getBlockX(), to.getBlockZ())) {
            event.setCancelled(true);
        }
    }

    /** Fogo de causa natural (lava, raio, propagação) só vale dentro da ilha; o de jogador é do outro listener. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgniteNatural(BlockIgniteEvent event) {
        if (event.getPlayer() != null || !isSky(event.getBlock())) {
            return;
        }
        Block source = event.getIgnitingBlock() != null ? event.getIgnitingBlock() : event.getBlock();
        if (!same(source, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (isSky(event.getBlock()) && !same(event.getSource(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Árvores e cogumelos que crescem não podem colocar blocos fora da região da ilha. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Location origin = event.getLocation();
        if (!skyWorld.contains(origin.getWorld())) {
            return;
        }
        if (protection.islandAt(origin.getBlockX(), origin.getBlockZ()).isEmpty()) {
            event.setCancelled(true);
            return;
        }
        event.getBlocks()
                .removeIf(state -> !protection.sameIsland(
                        origin.getBlockX(), origin.getBlockZ(), state.getX(), state.getZ()));
    }

    /** Dispenser não despeja (balde, TNT...) para fora da própria ilha. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block dispenser = event.getBlock();
        if (isSky(dispenser) && dispenser.getBlockData() instanceof Directional directional) {
            if (!same(dispenser, dispenser.getRelative(directional.getFacing()))) {
                event.setCancelled(true);
            }
        }
    }

    /** Entidades não-jogador (blocos caindo, endermen, wither) só alteram blocos dentro de ilhas. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player || !isSky(event.getBlock())) {
            return;
        }
        if (protection.islandAt(event.getBlock().getX(), event.getBlock().getZ()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    /** Pistão fora de ilha, ou que empurre/puxe qualquer bloco (ou a cabeça) para outra região, é cancelado. */
    private boolean pistonCrosses(Block piston, List<Block> moved, BlockFace direction) {
        if (!isSky(piston)) {
            return false;
        }
        if (!same(piston, piston.getRelative(direction))) {
            return true;
        }
        for (Block block : moved) {
            if (!same(piston, block)
                    || !same(piston, block.getRelative(direction))
                    || !same(piston, block.getRelative(direction.getOppositeFace()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSky(Block block) {
        return skyWorld.contains(block.getWorld());
    }

    private boolean same(Block a, Block b) {
        return protection.sameIsland(a.getX(), a.getZ(), b.getX(), b.getZ());
    }

    private boolean same(int x, int z, Block block) {
        return protection.sameIsland(x, z, block.getX(), block.getZ());
    }
}
