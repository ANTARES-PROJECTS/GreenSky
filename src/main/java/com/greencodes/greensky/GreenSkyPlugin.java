package com.greencodes.greensky;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.ConfigException;
import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.core.config.GreenSkyConfig;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.database.DatabaseException;
import com.greencodes.greensky.island.IslandCommand;
import com.greencodes.greensky.island.IslandRepository;
import com.greencodes.greensky.island.IslandService;
import com.greencodes.greensky.island.StarterIslandBuilder;
import com.greencodes.greensky.player.PlayerRepository;
import com.greencodes.greensky.protection.DenyNotifier;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.protection.PlayerProtectionListener;
import com.greencodes.greensky.protection.ProtectionSessionListener;
import com.greencodes.greensky.protection.WorldProtectionListener;
import com.greencodes.greensky.world.WorldManager;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GreenSkyPlugin extends JavaPlugin {

    private GreenSkyConfig config;
    private GreenScheduler scheduler;
    private Database database;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            this.config = GreenSkyConfig.load(getConfig());
        } catch (ConfigException e) {
            abort("config.yml inválido: " + e.getMessage());
            return;
        }

        String password = System.getenv(DatabaseSettings.PASSWORD_ENV);
        if (password == null || password.isBlank()) {
            abort("Variável de ambiente " + DatabaseSettings.PASSWORD_ENV + " não definida.");
            return;
        }
        try {
            // Boot: conexão e migrations são bloqueantes aqui; depois disso todo SQL é assíncrono.
            this.database = Database.open(config.database(), password, getClassLoader());
        } catch (DatabaseException e) {
            getLogger().severe(e.getMessage() + ": " + rootMessage(e));
            abort("Banco de dados indisponível.");
            return;
        }
        this.scheduler = new GreenScheduler(this);

        try {
            this.worldManager = new WorldManager(getServer(), getLogger(), config.world());
            this.worldManager.load();
            this.worldManager.verifyVoid();
        } catch (IllegalStateException e) {
            abort("Mundo SkyBlock indisponível: " + e.getMessage());
            return;
        }

        IslandService islandService = new IslandService(
                database,
                new IslandRepository(),
                new PlayerRepository(),
                new StarterIslandBuilder(worldManager.requireLoaded(), scheduler, config.islands()),
                config.islands());
        IslandCommand islandCommand = new IslandCommand(
                getServer(), getLogger(), islandService, worldManager.requireLoaded(), config.islands(), scheduler);

        // Proteção: índice em memória alimentado pelo banco; até carregar, tudo é negado.
        DenyNotifier notifier = new DenyNotifier();
        IslandProtectionService protection = new IslandProtectionService();
        islandService.addListener(protection);
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new PlayerProtectionListener(protection, worldManager.requireLoaded(), notifier), this);
        plugins.registerEvents(new WorldProtectionListener(protection, worldManager.requireLoaded()), this);
        plugins.registerEvents(new ProtectionSessionListener(protection, islandService, notifier, getLogger()), this);
        islandService.loadAll().whenComplete((all, error) -> {
            if (error != null) {
                getLogger().log(Level.SEVERE, "Falha ao carregar as ilhas; a proteção negará tudo.", error);
            } else {
                protection.load(all);
                getLogger().info("Proteção ativa: " + all.size() + " ilha(s) indexada(s).");
            }
        });
        // Quem já estava online (ex.: /reload) precisa ter as participações carregadas.
        for (Player online : getServer().getOnlinePlayers()) {
            islandService.membershipsOf(online.getUniqueId())
                    .thenAccept(members -> protection.playerJoined(online.getUniqueId(), members));
        }

        PluginCommand command = getCommand("island");
        command.setExecutor(islandCommand);
        command.setTabCompleter(islandCommand);

        getLogger().info("GreenSky " + getPluginMeta().getVersion() + " habilitado (ilhas: tamanho inicial "
                + config.islands().initialSize() + ", máx " + config.islands().maxSize()
                + ", espaçamento " + config.islands().spacing() + "; banco "
                + config.database().jdbcUrl() + ").");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        // O mundo é salvo e descarregado pelo próprio servidor ao desligar.
        if (database != null) {
            database.close();
        }
        getLogger().info("GreenSky desabilitado.");
    }

    private void abort(String reason) {
        getLogger().severe(reason);
        getLogger().severe("GreenSky não pode iniciar. Corrija e reinicie.");
        getServer().getPluginManager().disablePlugin(this);
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
