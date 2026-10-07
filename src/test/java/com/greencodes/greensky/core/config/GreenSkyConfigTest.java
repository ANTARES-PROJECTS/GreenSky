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
