package com.greencodes.greensky.core.config;

import org.bukkit.configuration.ConfigurationSection;

/** Configuração tipada e já validada. Imutável; um reload cria uma nova instância. */
public record GreenSkyConfig(
        IslandSettings islands, DatabaseSettings database, WorldSettings world, int maxEntitiesPerIsland) {

    public GreenSkyConfig {
        if (maxEntitiesPerIsland <= 0) {
            throw new IllegalArgumentException(
                    "performance.max-entities-per-island deve ser > 0 (atual: " + maxEntitiesPerIsland + ")");
        }
    }

    /**
     * @throws ConfigException se uma seção estiver ausente ou algum valor violar as regras
     */
    public static GreenSkyConfig load(ConfigurationSection root) throws ConfigException {
        ConfigurationSection islands = require(root, "islands");
        ConfigurationSection database = require(root, "database");
        ConfigurationSection world = require(root, "world");
        ConfigurationSection performance = require(root, "performance");
        try {
            return new GreenSkyConfig(
                    new IslandSettings(
                            islands.getInt("initial-size"),
                            islands.getInt("max-size"),
                            islands.getInt("spacing"),
                            islands.getInt("spacing-margin"),
                            islands.getInt("base-y"),
                            islands.getInt("starter-radius")),
                    new DatabaseSettings(
                            database.getString("host"),
                            database.getInt("port"),
                            database.getString("name"),
                            database.getString("user"),
                            database.getInt("pool-size")),
                    new WorldSettings(world.getString("name"), world.getInt("spawn-y")),
                    performance.getInt("max-entities-per-island"));
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e.getMessage());
        }
    }

    private static ConfigurationSection require(ConfigurationSection root, String path) throws ConfigException {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new ConfigException("Seção obrigatória ausente no config.yml: '" + path + "'");
        }
        return section;
    }
}
