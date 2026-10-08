package com.greencodes.greensky.player;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Entrada e saída. O pré-login (assíncrono, fora da thread do servidor) recusa um nick que
 * já pertence a outra conta com outras maiúsculas/minúsculas, antes de o jogador entrar.
 */
public final class PlayerSessionListener implements Listener {

    private static final long LOOKUP_TIMEOUT_SECONDS = 5;

    private final PlayerService players;
    private final PlayerSessionService sessions;
    private final Logger logger;

    public PlayerSessionListener(PlayerService players, PlayerSessionService sessions, Logger logger) {
        this.players = players;
        this.sessions = sessions;
        this.logger = logger;
    }

    /** Roda numa thread de login (assíncrona): esperar o banco aqui não trava o servidor. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        Optional<String> taken;
        try {
            taken = players.conflictingName(event.getUniqueId(), event.getName())
                    .get(LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deny(event, "Servidor ocupado. Tente de novo.");
            return;
        } catch (Exception e) {
            // Sem conseguir checar, não deixa entrar (falha fechada).
            logger.log(Level.SEVERE, "Falha ao verificar o nick " + event.getName(), e);
            deny(event, "Não foi possível verificar sua conta agora. Tente de novo.");
            return;
        }
        taken.ifPresent(registered -> deny(event, "O nick '" + registered + "' já está registrado. Entre usando"
                + " exatamente '" + registered + "' (maiúsculas e minúsculas contam)."));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sessions.ended(event.getPlayer().getUniqueId());
    }

    private static void deny(AsyncPlayerPreLoginEvent event, String message) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(message, NamedTextColor.RED));
    }
}
