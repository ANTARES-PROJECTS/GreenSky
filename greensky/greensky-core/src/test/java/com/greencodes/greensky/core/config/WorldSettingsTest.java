package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorldSettingsTest {

    @Test
    void acceptsDefaults() {
        assertDoesNotThrow(() -> new WorldSettings("greensky_world", 100));
    }

    @Test
    void rejectsReservedNames() {
        for (String name : new String[] {"world", "world_nether", "world_the_end"}) {
            assertThrows(IllegalArgumentException.class, () -> new WorldSettings(name, 100));
        }
    }

    @Test
    void rejectsUnsafeNames() {
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings(null, 100));
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("", 100));
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("../etc", 100));
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("Sky World", 100));
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("a".repeat(33), 100));
    }

    @Test
    void rejectsSpawnOutsideWorldHeight() {
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("sky", -64));
        assertThrows(IllegalArgumentException.class, () -> new WorldSettings("sky", 319));
        assertDoesNotThrow(() -> new WorldSettings("sky", -63));
        assertDoesNotThrow(() -> new WorldSettings("sky", 318));
    }
}
