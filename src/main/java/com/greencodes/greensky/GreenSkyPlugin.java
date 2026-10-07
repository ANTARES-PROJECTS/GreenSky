package com.greencodes.greensky;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.ConfigException;
import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.core.config.GreenSkyConfig;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.database.DatabaseException;
import com.greencodes.greensky.world.WorldManager;
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
