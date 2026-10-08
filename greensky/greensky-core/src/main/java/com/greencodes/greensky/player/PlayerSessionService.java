package com.greencodes.greensky.player;

import com.greencodes.greensky.api.event.GreenSkyPlayerReadyEvent;
import com.greencodes.greensky.core.GreenScheduler;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 * Início da sessão do jogador no GreenSky. Só começa depois da autenticação: registra a
 * conta (reivindica o nick) e então dispara {@link GreenSkyPlayerReadyEvent}. Antes disso o
 * jogador não tem acesso a nada (a proteção nega tudo e os comandos /is recusam).
 */
public final class PlayerSessionService {

    private final PlayerService players;
    private final GreenScheduler scheduler;
    private final PluginManager pluginManager;
    private final Logger logger;
    private final Set<UUID> ready = ConcurrentHashMap.newKeySet();

    public PlayerSessionService(
            PlayerService players, GreenScheduler scheduler, PluginManager pluginManager, Logger logger) {
        this.players = players;
        this.scheduler = scheduler;
        this.pluginManager = pluginManager;
        this.logger = logger;
    }

    /** Chamado pela {@code AuthBridge}, na thread do servidor, quando o jogador autentica. */
    public void authenticated(Player player) {
        UUID id = player.getUniqueId();
        if (ready.contains(id)) {
            return; // login repetido na mesma sessão
        }
        players.claim(id, player.getName()).whenComplete((ignored, error) -> scheduler.runSync(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                if (cause instanceof NameTakenException) {
                    player.kick(Component.text(
                            "Este nick já pertence a outra conta (com outras maiúsculas/minúsculas).",
                            NamedTextColor.RED));
                } else {
                    logger.log(Level.SEVERE, "Falha ao registrar " + player.getName(), cause);
                    player.kick(Component.text("Erro ao carregar sua conta. Tente de novo.", NamedTextColor.RED));
                }
                return;
            }
            ready.add(id);
            pluginManager.callEvent(new GreenSkyPlayerReadyEvent(player));
        }));
    }

    /** O jogador já autenticou e a sessão dele no GreenSky começou? */
    public boolean isReady(Player player) {
        return ready.contains(player.getUniqueId());
    }

    public void ended(UUID player) {
        ready.remove(player);
    }
}
