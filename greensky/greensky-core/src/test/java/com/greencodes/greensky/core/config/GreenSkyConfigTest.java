package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Lê o config.yml real que vai no jar e testa chaves ausentes/inválidas. */
class GreenSkyConfigTest {

    private static YamlConfiguration defaults() throws Exception {
        try (var in = GreenSkyConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void shippedConfigIsValid() throws Exception {
        GreenSkyConfig config = GreenSkyConfig.load(defaults());
        assertEquals(100, config.islands().initialSize());
        assertEquals(100, config.islands().baseY());
        assertEquals("greensky_world", config.world().name());
        assertEquals(5432, config.database().port());
        assertEquals(java.util.List.of(100, 150, 200, 300, 500), config.expansion().levels());
    }

    @Test
    void fishingSettingsAreParsedAndValidated() throws Exception {
        YamlConfiguration yaml = defaults();
        GreenSkyConfig config = GreenSkyConfig.load(yaml);
        assertEquals(true, config.fishing().requireOpenWater());
        assertEquals(10, config.fishing().afkMaxCatchesSameSpot());
        yaml.set("fishing.afk-max-catches-same-spot", 1);
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("afk-max-catches-same-spot"), e.getMessage());
    }

    @Test
    void authDefaultsAreSafeAndValidated() throws Exception {
        YamlConfiguration yaml = defaults();
        GreenSkyConfig config = GreenSkyConfig.load(yaml);
        // O padrão que vai no jar é o seguro para produção.
        assertEquals(com.greencodes.greensky.integration.auth.AuthProvider.NLOGIN, config.auth().provider());
        assertEquals(false, config.auth().devMode());

        yaml.set("auth.provider", "None");
        assertEquals(com.greencodes.greensky.integration.auth.AuthProvider.NONE, GreenSkyConfig.load(yaml).auth().provider());
        yaml.set("auth.provider", "authme");
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("auth.provider"), e.getMessage());

        yaml.set("auth.provider", "nlogin");
        yaml.set("auth.dev-mode", "sim");
        e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("auth.dev-mode"), e.getMessage());
    }

    @Test
    void defaultVisibilityIsParsedAndValidated() throws Exception {
        YamlConfiguration yaml = defaults();
        assertEquals(com.greencodes.greensky.island.IslandVisibility.PUBLIC, GreenSkyConfig.load(yaml).defaultVisibility());
        yaml.set("islands.default-visibility", "Private");
        assertEquals(com.greencodes.greensky.island.IslandVisibility.PRIVATE, GreenSkyConfig.load(yaml).defaultVisibility());
        yaml.set("islands.default-visibility", "amigos");
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("default-visibility"), e.getMessage());
    }

    @Test
    void invalidExpansionLevelsAreRejected() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("islands.expansion-levels", java.util.List.of(100, 2000));
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("max-size"), e.getMessage());

        yaml.set("islands.expansion-levels", java.util.List.of("cem", "duzentos"));
        assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));

        yaml.set("islands.expansion-levels", null);
        e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("islands.expansion-levels"), e.getMessage());
    }

    @Test
    void missingKeyIsRejectedInsteadOfBecomingZero() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("islands.base-y", null); // config.yml antigo, sem a chave nova
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("islands.base-y"), e.getMessage());
    }

    @Test
    void wrongTypeIsRejected() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("database.port", "cinco mil");
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("database.port"), e.getMessage());
    }

    @Test
    void missingSectionIsRejected() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("world", null);
        assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
    }

    @Test
    void ruleViolationIsRejected() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("islands.spacing", 512);
        ConfigException e = assertThrows(ConfigException.class, () -> GreenSkyConfig.load(yaml));
        assertTrue(e.getMessage().contains("islands.spacing"), e.getMessage());
    }
}
