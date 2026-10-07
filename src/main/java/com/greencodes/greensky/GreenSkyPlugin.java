package com.greencodes.greensky;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.core.config.ConfigException;
import com.greencodes.greensky.core.config.GreenSkyConfig;
import org.bukkit.plugin.java.JavaPlugin;

public final class GreenSkyPlugin extends JavaPlugin {

    private GreenSkyConfig config;
    private GreenScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            this.config = GreenSkyConfig.load(getConfig());
        } catch (ConfigException e) {
            getLogger().severe("config.yml inválido: " + e.getMessage());
            getLogger().severe("GreenSky não pode iniciar. Corrija o config.yml e reinicie.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.scheduler = new GreenScheduler(this);

        getLogger().info("GreenSky " + getPluginMeta().getVersion() + " habilitado (ilhas: tamanho inicial "
                + config.islands().initialSize() + ", máx " + config.islands().maxSize()
                + ", espaçamento " + config.islands().spacing() + ").");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        getLogger().info("GreenSky desabilitado.");
    }
}
