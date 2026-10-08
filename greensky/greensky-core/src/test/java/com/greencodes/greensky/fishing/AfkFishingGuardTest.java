package com.greencodes.greensky.fishing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AfkFishingGuardTest {

    private static final AfkFishingGuard.Spot SPOT = new AfkFishingGuard.Spot(10, 101, 5, 90, 30);

    @Test
    void sameSpotAboveTheLimitIsIdle() {
        AfkFishingGuard guard = new AfkFishingGuard(3);
        UUID p = UUID.randomUUID();
        assertFalse(guard.catchIsIdle(p, SPOT)); // 1
        assertFalse(guard.catchIsIdle(p, SPOT)); // 2
        assertFalse(guard.catchIsIdle(p, SPOT)); // 3 (limite)
        assertTrue(guard.catchIsIdle(p, SPOT)); // 4: parada
        assertTrue(guard.catchIsIdle(p, SPOT)); // continua parada
    }

    @Test
    void movingOrTurningResetsTheCount() {
        AfkFishingGuard guard = new AfkFishingGuard(2);
        UUID p = UUID.randomUUID();
        guard.catchIsIdle(p, SPOT);
        guard.catchIsIdle(p, SPOT);
        assertTrue(guard.catchIsIdle(p, SPOT));
        // Mudou a mira em 1 grau: volta a contar do zero.
        assertFalse(guard.catchIsIdle(p, new AfkFishingGuard.Spot(10, 101, 5, 91, 30)));
        assertFalse(guard.catchIsIdle(p, new AfkFishingGuard.Spot(10, 101, 5, 91, 30)));
        // Mudou de bloco.
        assertFalse(guard.catchIsIdle(p, new AfkFishingGuard.Spot(11, 101, 5, 91, 30)));
    }

    @Test
    void playersAreIndependentAndForgetClears() {
        AfkFishingGuard guard = new AfkFishingGuard(1);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        guard.catchIsIdle(a, SPOT);
        assertTrue(guard.catchIsIdle(a, SPOT));
        assertFalse(guard.catchIsIdle(b, SPOT), "outro jogador não herda a contagem");
        guard.forget(a);
        assertFalse(guard.catchIsIdle(a, SPOT), "ao sair, a contagem some");
    }
}
