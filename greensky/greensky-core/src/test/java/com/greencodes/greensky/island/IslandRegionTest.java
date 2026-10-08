package com.greencodes.greensky.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IslandRegionTest {

    @Test
    void spiralStartsAtOriginAndFirstRing() {
        assertEquals("0,0", pos(0));
        assertEquals("1,0", pos(1));
        assertEquals("1,1", pos(2));
        assertEquals("0,1", pos(3));
        assertEquals("-1,1", pos(4));
        assertEquals("-1,0", pos(5));
        assertEquals("-1,-1", pos(6));
        assertEquals("0,-1", pos(7));
        assertEquals("1,-1", pos(8));
        assertEquals("2,-1", pos(9));
    }

    @Test
    void everyPositionIsUnique() {
        Set<String> seen = new HashSet<>();
        for (long n = 0; n < 40_000; n++) {
            assertTrue(seen.add(pos(n)), "posição repetida no slot " + n);
        }
    }

    @Test
    void ringsFillTheSquaresExactly() {
        // Os primeiros (2K+1)^2 slots devem cobrir exatamente o quadrado [-K,K] x [-K,K].
        for (int k = 1; k <= 30; k++) {
            long total = (2L * k + 1) * (2L * k + 1);
            Set<String> seen = new HashSet<>();
            for (long n = 0; n < total; n++) {
                long[] p = IslandRegion.gridPosition(n);
                assertTrue(Math.abs(p[0]) <= k && Math.abs(p[1]) <= k, "slot " + n + " fora do anel " + k);
                seen.add(pos(n));
            }
            assertEquals(total, seen.size(), "anel " + k + " incompleto");
        }
    }

    @Test
    void slotZeroIsReservedForSpawn() {
        assertThrows(IllegalArgumentException.class, () -> IslandRegion.forSlot(0, 1200, 100));
        assertThrows(IllegalArgumentException.class, () -> IslandRegion.forSlot(-1, 1200, 100));
    }

    @Test
    void boundsAndContains() {
        IslandRegion r = new IslandRegion(0, 0, 100);
        assertEquals(-50, r.minX());
        assertEquals(50, r.maxX());
        assertTrue(r.contains(-50, -50));
        assertTrue(r.contains(49, 49));
        assertFalse(r.contains(50, 0));
        assertFalse(r.contains(0, -51));
        assertEquals(-4, r.minChunkX());
        assertEquals(3, r.maxChunkX());
    }

    @Test
    void neverOverlapsAtMaxSizeWithConfiguredSpacing() {
        // Padrão do config: spacing 1200, max-size 1000. Nenhum par de ilhas pode se sobrepor.
        List<IslandRegion> regions = new ArrayList<>();
        regions.add(new IslandRegion(0, 0, 1000)); // área do spawn (slot 0)
        for (long slot = 1; slot <= 300; slot++) {
            regions.add(IslandRegion.forSlot(slot, 1200, 1000));
        }
        for (int i = 0; i < regions.size(); i++) {
            for (int j = i + 1; j < regions.size(); j++) {
                assertFalse(regions.get(i).overlaps(regions.get(j)), "ilhas " + i + " e " + j + " se sobrepõem");
            }
        }
    }

    @Test
    void overlapsDetectedWhenSpacingTooSmall() {
        IslandRegion a = IslandRegion.forSlot(1, 512, 1000);
        IslandRegion b = new IslandRegion(0, 0, 1000);
        assertTrue(a.overlaps(b));
    }

    @Test
    void rejectsSlotsBeyondWorldRange() {
        assertThrows(IllegalArgumentException.class, () -> IslandRegion.forSlot(Long.MAX_VALUE / 2, 1200, 100));
    }

    private static String pos(long n) {
        long[] p = IslandRegion.gridPosition(n);
        return p[0] + "," + p[1];
    }
}
