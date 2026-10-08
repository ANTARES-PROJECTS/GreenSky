package com.greencodes.greensky.integration.auth;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Sem plugin de login: entrar já conta como autenticado. Seguro só com online-mode=true
 * (a Mojang autentica) ou em desenvolvimento ({@code auth.dev-mode: true}).
 */
public final class NoAuthBridge implements AuthBridge {

    @Override
    public String name() {
        return "none";
    }

    @Override
    public void start(Plugin plugin, Consumer<Player> onAuthenticated) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onJoin(PlayerJoinEvent event) {
                onAuthenticated.accept(event.getPlayer());
            }
        }, plugin);
    }

    @Override
    public boolean isAuthenticated(Player player) {
        return true;
    }
}
