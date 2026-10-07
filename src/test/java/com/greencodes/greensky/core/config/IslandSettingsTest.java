package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IslandSettingsTest {

    private static IslandSettings of(int initial, int max, int spacing, int margin) {
        return new IslandSettings(initial, max, spacing, margin, 100, 4);
    }

    @Test
    void acceptsDefaults() {
        assertDoesNotThrow(() -> of(100, 1000, 1200, 100));
    }

    @Test
    void rejectsSpacingEqualToMaxPlusMargin() {
        assertThrows(IllegalArgumentException.class, () -> of(100, 1000, 1100, 100));
    }

    @Test
    void rejectsOverlappingSpacing() {
        // Exemplo original do README: spacing 512 com max-size 1000.
        assertThrows(IllegalArgumentException.class, () -> of(100, 1000, 512, 0));
    }

    @Test
    void rejectsMaxSmallerThanInitial() {
        assertThrows(IllegalArgumentException.class, () -> of(200, 100, 1200, 100));
    }

    @Test
    void rejectsNegativeMargin() {
        assertThrows(IllegalArgumentException.class, () -> of(100, 1000, 1200, -1));
    }

    @Test
    void doesNotOverflow() {
        assertThrows(IllegalArgumentException.class, () -> of(1, Integer.MAX_VALUE, Integer.MAX_VALUE, 1));
    }

    @Test
    void validatesStarterIsland() {
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 1200, 100, 400, 4));
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 1200, 100, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(100, 1000, 1200, 100, 100, 17));
        // diametro 2*16+1 = 33 nao cabe em initial-size 20
        assertThrows(IllegalArgumentException.class, () -> new IslandSettings(20, 1000, 1200, 100, 100, 16));
        assertDoesNotThrow(() -> new IslandSettings(33, 1000, 1200, 100, 100, 16));
    }
}
