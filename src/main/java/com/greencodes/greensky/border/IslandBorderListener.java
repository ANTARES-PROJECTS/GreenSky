package com.greencodes.greensky.border;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Atualiza a borda visual quando o jogador muda de lugar de forma "descontínua" (entrar,
 * teleportar, trocar de mundo, renascer). Andando não é preciso: ilhas ficam a centenas de
 * blocos e o próprio cliente colide com a borda. Fino: só agenda {@link IslandBorderService#refresh}
 * para o tick seguinte, quando a posição nova já vale.
 */
public final class IslandBorderListener implements Listener {

    private final IslandBorderService borders;

    public IslandBorderListener(IslandBorderService borders) {
        this.borders = borders;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        borders.refreshLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        borders.refreshLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        borders.refreshLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        borders.refreshLater(event.getPlayer());
    }
}
