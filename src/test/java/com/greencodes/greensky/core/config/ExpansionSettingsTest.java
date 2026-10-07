package com.greencodes.greensky.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExpansionSettingsTest {

    private static final IslandSettings ISLANDS = new IslandSettings(100, 1000, 1200, 100, 100, 4);
    private final ExpansionSettings levels =
            new ExpansionSettings(List.of(100, 150, 200, 300, 500)).validateAgainst(ISLANDS);

    @Test
    void nextLevelFollowsTheList() {
        assertEquals(150, levels.nextAfter(100).getAsInt());
        assertEquals(200, levels.nextAfter(150).getAsInt());
        assertEquals(500, levels.nextAfter(300).getAsInt());
        assertTrue(levels.nextAfter(500).isEmpty());
        // Tamanho fora da lista (ex.: config mudou): vai para o próximo nível acima dele.
        assertEquals(200, levels.nextAfter(170).getAsInt());
    }

    @Test
    void levelNumbers() {
        assertEquals(1, levels.levelOf(100));
        assertEquals(2, levels.levelOf(150));
        assertEquals(5, levels.levelOf(500));
        assertEquals(2, levels.levelOf(170));
    }

    @Test
    void rejectsInvalidLists() {
        assertThrows(IllegalArgumentException.class, () -> new ExpansionSettings(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ExpansionSettings(List.of(100, 100)));
        assertThrows(IllegalArgumentException.class, () -> new ExpansionSettings(List.of(100, 200, 150)));
        // Não começa no tamanho inicial.
        assertThrows(IllegalArgumentException.class,
                () -> new ExpansionSettings(List.of(120, 200)).validateAgainst(ISLANDS));
        // Passa do tamanho máximo (o que quebraria a garantia de não sobreposição).
        assertThrows(IllegalArgumentException.class,
                () -> new ExpansionSettings(List.of(100, 1001)).validateAgainst(ISLANDS));
    }
}
