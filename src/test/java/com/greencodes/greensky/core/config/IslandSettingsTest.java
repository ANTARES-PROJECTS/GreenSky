package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IslandSettingsTest {

    @Test
    void acceptsDefaults() {
        assertDoesNotThrow(() -> new IslandSettings(100, 1000, 1200, 100));
    }

    @Test
    void rejectsSpacingEqualToMaxPlusMargin() {
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 1100, 100));
    }

    @Test
    void rejectsOverlappingSpacing() {
        // Exemplo original do README: spacing 512 com max-size 1000.
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 512, 0));
    }

    @Test
    void rejectsMaxSmallerThanInitial() {
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(200, 100, 1200, 100));
    }

    @Test
    void rejectsNegativeMargin() {
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 1200, -1));
    }

    @Test
    void doesNotOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> new IslandSettings(1, Integer.MAX_VALUE, Integer.MAX_VALUE, 1));
    }
}
