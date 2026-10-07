package com.greencodes.greensky.core;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/**
 * Único ponto de acesso a schedulers do plugin. Usa os schedulers do Paper
 * (global e async) em vez de {@code BukkitScheduler}, mantendo o caminho aberto
 * para uma futura adaptação ao Folia. Não implica compatibilidade com Folia.
 */
public final class GreenScheduler {

    private final Plugin plugin;
    private final Server server;

    public GreenScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    /** Executa na thread do servidor (região global). */
    public void runSync(Runnable task) {
        server.getGlobalRegionScheduler().execute(plugin, task);
    }

    public ScheduledTask runSyncLater(Runnable task, long delayTicks) {
        return server.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
    }

    public ScheduledTask runSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return server.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delayTicks, periodTicks);
    }

    /** Executa fora da thread do servidor. Não tocar na API de mundo/entidades aqui dentro. */
    public ScheduledTask runAsync(Runnable task) {
        return server.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    public ScheduledTask runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        return server.getAsyncScheduler().runDelayed(plugin, t -> task.run(), delay, unit);
    }

    /** Cancela todas as tarefas do plugin; chamado no disable. */
    public void cancelAll() {
        server.getGlobalRegionScheduler().cancelTasks(plugin);
        server.getAsyncScheduler().cancelTasks(plugin);
    }
}
