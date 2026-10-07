package com.greencodes.greensky.protection;

import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.world.SkyWorld;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

/**
 * Ações de jogadores no mundo SkyBlock. Fino: cada evento vira uma pergunta ao
 * {@link IslandProtectionService}. Quem tem {@value #BYPASS} ignora a proteção.
 */
public final class PlayerProtectionListener implements Listener {

    public static final String BYPASS = "greensky.admin.bypass";

    /** Inventários de armazenamento (guardam itens do dono); mesas de trabalho etc. ficam em INTERACT. */
    private static final Set<InventoryType> STORAGE = Set.of(
            InventoryType.CHEST,
            InventoryType.BARREL,
            InventoryType.SHULKER_BOX,
            InventoryType.HOPPER,
            InventoryType.DISPENSER,
            InventoryType.DROPPER,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.BREWING,
            InventoryType.LECTERN,
            InventoryType.CRAFTER);

    private final IslandProtectionService protection;
    private final SkyWorld skyWorld;
    private final DenyNotifier notifier;

    public PlayerProtectionListener(IslandProtectionService protection, SkyWorld skyWorld, DenyNotifier notifier) {
        this.protection = protection;
        this.skyWorld = skyWorld;
        this.notifier = notifier;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        deny(event, event.getPlayer(), IslandPermission.BREAK, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        deny(event, event.getPlayer(), IslandPermission.BUILD, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        deny(event, event.getPlayer(), IslandPermission.BUILD, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        deny(event, event.getPlayer(), IslandPermission.BREAK, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (event.getClickedBlock() == null || action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR) {
            return;
        }
        deny(event, event.getPlayer(), IslandPermission.INTERACT, event.getClickedBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent event) {
        Location location = event.getInventory().getLocation();
        if (location == null || !STORAGE.contains(event.getInventory().getType())) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            deny(event, player, IslandPermission.CONTAINERS, location);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        deny(event, event.getPlayer(), IslandPermission.INTERACT, event.getRightClicked().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        deny(event, event.getPlayer(), IslandPermission.BUILD, event.getRightClicked().getLocation());
    }

    /** Bater em entidades "do cenário" (animais, aldeões, quadros, armor stands, veículos) conta como quebrar. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        Player attacker = playerBehind(event.getDamager());
        Entity victim = event.getEntity();
        if (attacker == null || !isScenery(victim)) {
            return;
        }
        deny(event, attacker, IslandPermission.BREAK, victim.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            deny(event, event.getPlayer(), IslandPermission.BUILD, event.getEntity().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player remover = playerBehind(event.getRemover());
        if (remover != null) {
            deny(event, remover, IslandPermission.BREAK, event.getEntity().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        Player attacker = playerBehind(event.getAttacker());
        if (attacker != null) {
            deny(event, attacker, IslandPermission.BREAK, event.getVehicle().getLocation());
        }
    }

    /** Acender fogo por jogador (isqueiro) é construir; o fogo que se espalha é do {@link WorldProtectionListener}. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnitePlayer(BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            deny(event, event.getPlayer(), IslandPermission.BUILD, event.getBlock().getLocation());
        }
    }

    /**
     * Fuga: pérola do fim e fruta do coro só levam a ilhas das quais o jogador é membro.
     * Teleportes por comando ou plugin (ex.: /island home) não passam por aqui.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                // CHORUS_FRUIT está deprecated; a fruta do coro (e itens com o mesmo efeito) vem como CONSUMABLE_EFFECT.
                && cause != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            return;
        }
        Location to = event.getTo();
        Player player = event.getPlayer();
        if (to == null || !skyWorld.contains(to.getWorld()) || player.hasPermission(BYPASS)) {
            return;
        }
        if (!protection.isMemberAt(player.getUniqueId(), to.getBlockX(), to.getBlockZ())) {
            event.setCancelled(true);
            notifier.denied(player);
        }
    }

    private void deny(Event event, Player player, IslandPermission permission, Location location) {
        if (!(event instanceof org.bukkit.event.Cancellable cancellable)
                || !skyWorld.contains(location.getWorld())
                || player.hasPermission(BYPASS)) {
            return;
        }
        if (!protection.can(player.getUniqueId(), permission, location.getBlockX(), location.getBlockZ())) {
            cancellable.setCancelled(true);
            notifier.denied(player);
        }
    }

    private static boolean isScenery(Entity entity) {
        return entity instanceof Hanging
                || entity instanceof ArmorStand
                || entity instanceof Animals
                || entity instanceof AbstractVillager
                || entity instanceof Vehicle;
    }

    /** O jogador responsável por um dano: ele mesmo, ou quem atirou o projétil. */
    private static Player playerBehind(Entity source) {
        if (source instanceof Player player) {
            return player;
        }
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
