package com.greencodes.greensky.visit;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.island.IslandService;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * A ilha é a casa: quem morre renasce nela (exceto com cama/âncora definida), e quem entra
 * sem ilha recebe a dica de como começar.
 */
public final class HomeListener implements Listener {

    private static final Component WELCOME = Component.text(
            "Bem-vindo ao GreenSky! Use /is create para ganhar a sua ilha.", NamedTextColor.GREEN);

    private final IslandService islands;
    private final IslandProtectionService protection;
    private final SkyWorld skyWorld;
    private final IslandSettings settings;
    private final GreenScheduler scheduler;

    public HomeListener(
            IslandService islands,
            IslandProtectionService protection,
            SkyWorld skyWorld,
            IslandSettings settings,
            GreenScheduler scheduler) {
        this.islands = islands;
        this.protection = protection;
        this.skyWorld = skyWorld;
        this.settings = settings;
        this.scheduler = scheduler;
    }

    /** Síncrono e sem banco: usa as participações já carregadas no join. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH
                || event.isBedSpawn()
                || event.isAnchorSpawn()) {
            return;
        }
        protection.ownedIsland(event.getPlayer().getUniqueId())
                .ifPresent(island -> event.setRespawnLocation(island.home(skyWorld.bukkit(), settings.baseY())));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        islands.findByOwner(player.getUniqueId()).thenAccept(found -> {
            if (found.isEmpty()) {
                scheduler.runSync(() -> {
                    if (player.isOnline()) {
                        player.sendMessage(WELCOME);
                    }
                });
            }
        });
    }
}
