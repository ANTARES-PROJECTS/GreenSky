package com.greencodes.greensky.integration.auth;

import com.greencodes.greensky.core.GreenScheduler;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.login.api.nLoginAPI;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Ponte com o nLogin (API conferida com javap em libs/nLogin-2.0.24.jar):
 * {@code com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent} ("disparado quando o
 * jogador termina a autenticação") e {@code nLoginAPI#isAuthenticated(String)}.
 * Só pode ser instanciada com o nLogin carregado (as classes dele são compileOnly).
 */
public final class NLoginAuthBridge implements AuthBridge {

    private final GreenScheduler scheduler;
    private final Logger logger;

    public NLoginAuthBridge(GreenScheduler scheduler, Logger logger) {
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "nLogin";
    }

    @Override
    public void start(Plugin plugin, Consumer<Player> onAuthenticated) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onAuthenticate(AuthenticateEvent event) {
                Player player = event.getPlayer();
                // A documentação do nLogin avisa que os eventos de autenticação podem ser assíncronos.
                if (Bukkit.isPrimaryThread()) {
                    onAuthenticated.accept(player);
                } else {
                    scheduler.runSync(() -> {
                        if (player.isOnline()) {
                            onAuthenticated.accept(player);
                        }
                    });
                }
            }
        }, plugin);
    }

    @Override
    public boolean isAuthenticated(Player player) {
        try {
            nLoginAPI api = nLoginAPI.getApi();
            return api.isAvailable() && api.isAuthenticated(player.getName());
        } catch (RuntimeException e) {
            // nLoginNotLoadedException / nLoginNotReadyException: na dúvida, não autenticado.
            logger.log(Level.FINE, "nLogin ainda não está pronto", e);
            return false;
        }
    }
}
