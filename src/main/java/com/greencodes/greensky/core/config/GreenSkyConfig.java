package com.greencodes.greensky.core.config;

import com.greencodes.greensky.island.IslandVisibility;
import org.bukkit.configuration.ConfigurationSection;

/** Configuração tipada e já validada. Imutável; um reload cria uma nova instância. */
public record GreenSkyConfig(
        IslandSettings islands,
        ExpansionSettings expansion,
        IslandVisibility defaultVisibility,
        DatabaseSettings database,
        WorldSettings world,
        int maxEntitiesPerIsland) {

    public GreenSkyConfig {
        if (maxEntitiesPerIsland <= 0) {
            throw new IllegalArgumentException(
                    "performance.max-entities-per-island deve ser > 0 (atual: " + maxEntitiesPerIsland + ")");
        }
    }

    /**
     * Toda chave é obrigatória: um valor ausente nunca vira 0 em silêncio (ex.: um config.yml
     * antigo, de antes de uma chave nova existir, faz o plugin recusar o boot dizendo o que falta).
     *
     * @throws ConfigException se uma seção/chave estiver ausente ou algum valor violar as regras
     */
    public static GreenSkyConfig load(ConfigurationSection root) throws ConfigException {
        ConfigurationSection islands = require(root, "islands");
        ConfigurationSection database = require(root, "database");
        ConfigurationSection world = require(root, "world");
        ConfigurationSection performance = require(root, "performance");
        try {
            IslandSettings islandSettings = new IslandSettings(
                    requireInt(islands, "initial-size"),
                    requireInt(islands, "max-size"),
                    requireInt(islands, "spacing"),
                    requireInt(islands, "spacing-margin"),
                    requireInt(islands, "base-y"),
                    requireInt(islands, "starter-radius"));
            return new GreenSkyConfig(
                    islandSettings,
                    new ExpansionSettings(requireIntList(islands, "expansion-levels")).validateAgainst(islandSettings),
                    visibility(requireString(islands, "default-visibility")),
                    new DatabaseSettings(
                            requireString(database, "host"),
                            requireInt(database, "port"),
                            requireString(database, "name"),
                            requireString(database, "user"),
                            requireInt(database, "pool-size")),
                    new WorldSettings(requireString(world, "name"), requireInt(world, "spawn-y")),
                    requireInt(performance, "max-entities-per-island"));
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e.getMessage());
        }
    }

    private static IslandVisibility visibility(String value) {
        try {
            return IslandVisibility.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "islands.default-visibility deve ser public ou private (atual: " + value + ")");
        }
    }

    private static ConfigurationSection require(ConfigurationSection root, String path) throws ConfigException {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new ConfigException("Seção obrigatória ausente no config.yml: '" + path + "'");
        }
        return section;
    }

    private static int requireInt(ConfigurationSection section, String key) throws ConfigException {
        if (!section.isInt(key)) {
            throw new ConfigException(missing(section, key, "um número inteiro"));
        }
        return section.getInt(key);
    }

    private static java.util.List<Integer> requireIntList(ConfigurationSection section, String key)
            throws ConfigException {
        if (!section.isList(key) || section.getList(key).stream().anyMatch(v -> !(v instanceof Integer))) {
            throw new ConfigException(missing(section, key, "uma lista de números inteiros"));
        }
        return section.getIntegerList(key);
    }

    private static String requireString(ConfigurationSection section, String key) throws ConfigException {
        if (!section.isString(key)) {
            throw new ConfigException(missing(section, key, "um texto"));
        }
        return section.getString(key);
    }

    private static String missing(ConfigurationSection section, String key, String expected) {
        String path = section.getCurrentPath() == null || section.getCurrentPath().isEmpty()
                ? key
                : section.getCurrentPath() + "." + key;
        return "'" + path + "' ausente ou inválido no config.yml (esperado " + expected + ")."
                + " Se você atualizou o plugin, copie a chave do config.yml padrão.";
    }
}
