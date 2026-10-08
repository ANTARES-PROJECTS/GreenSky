package com.greencodes.greensky.border;

import com.greencodes.greensky.api.event.GreenSkyPlayerReadyEvent;
import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Atualiza a borda visual quando o jogador muda de lugar de forma "descontínua" (ficar pronto,
 * teleportar, trocar de mundo, renascer). Andando não é preciso: ilhas ficam a centenas de
 * blocos e o próprio cliente colide com a borda. Fino: só agenda {@link IslandBorderService#refresh}
 * para o tick seguinte, quando a posição nova já vale. Ignora quem ainda não autenticou.
 */
public final class IslandBorderListener implements Listener {

    private final IslandBorderService borders;
    private final Predicate<Player> ready;

    public IslandBorderListener(IslandBorderService borders, Predicate<Player> ready) {
        this.borders = borders;
        this.ready = ready;
    }

    /** A borda acompanha o jogador a partir do login (antes disso ele vê a borda normal). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onReady(GreenSkyPlayerReadyEvent event) {
        borders.refreshLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        refreshIfReady(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        refreshIfReady(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        refreshIfReady(event.getPlayer());
    }

    private void refreshIfReady(Player player) {
        if (ready.test(player)) {
            borders.refreshLater(player);
        }
    }
}
