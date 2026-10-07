package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatabaseSettingsTest {

    @Test
    void acceptsDefaultsAndBuildsUrl() {
        DatabaseSettings s = new DatabaseSettings("127.0.0.1", 5432, "greensky", "greensky", 10);
        assertEquals("jdbc:postgresql://127.0.0.1:5432/greensky", s.jdbcUrl());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("", 5432, "db", "u", 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(null, 5432, "db", "u", 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 0, "db", "u", 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 70000, "db", "u", 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 5432, " ", "u", 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 5432, "db", null, 10));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 5432, "db", "u", 0));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings("h", 5432, "db", "u", 51));
    }

    @Test
    void acceptsPoolBounds() {
        assertDoesNotThrow(() -> new DatabaseSettings("h", 1, "db", "u", 1));
        assertDoesNotThrow(() -> new DatabaseSettings("h", 65535, "db", "u", 50));
    }
}
