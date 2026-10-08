package com.greencodes.greensky.protection;

import com.greencodes.greensky.api.event.GreenSkyPlayerReadyEvent;
import com.greencodes.greensky.island.IslandService;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Carrega as participações do jogador (ilhas das quais é dono ou membro) quando ele fica
 * pronto (depois de autenticar) e as descarta ao sair. Até lá ele não tem acesso a nada.
 */
public final class ProtectionSessionListener implements Listener {

    private final IslandProtectionService protection;
    private final IslandService islands;
    private final DenyNotifier notifier;
    private final Logger logger;

    public ProtectionSessionListener(
            IslandProtectionService protection, IslandService islands, DenyNotifier notifier, Logger logger) {
        this.protection = protection;
        this.islands = islands;
        this.notifier = notifier;
        this.logger = logger;
    }

    @EventHandler
    public void onReady(GreenSkyPlayerReadyEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        UUID session = protection.beginSession(id);
        String name = event.getPlayer().getName();
        islands.membershipsOf(id).whenComplete((members, error) -> {
            if (error != null) {
                logger.log(Level.SEVERE, "Falha ao carregar ilhas de " + name, error);
            } else {
                protection.completeSession(id, session, members);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        protection.playerQuit(id);
        notifier.forget(id);
    }
}
