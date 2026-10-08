package com.greencodes.greensky.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.island.Island;
import com.greencodes.greensky.island.IslandRegion;
import com.greencodes.greensky.island.IslandState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IslandIndexTest {

    @Test
    void concurrentReplacementsAndRemovalLeaveNoStaleRegions() throws Exception {
        IslandIndex index = new IslandIndex();
        Island original = island(1, 0, 0, 100);
        var workers = new java.util.ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (int t = 0; t < 8; t++) {
            final int worker = t;
            workers.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 250; i++) {
                    index.put(new Island(original.id(), original.owner(), 1,
                            new IslandRegion(worker * 1200, 0, 1000), IslandState.READY));
                }
            }));
        }
        java.util.concurrent.CompletableFuture.allOf(workers.toArray(java.util.concurrent.CompletableFuture[]::new))
                .get(10, java.util.concurrent.TimeUnit.SECONDS);
        index.put(original);
        for (int t = 1; t < 8; t++) {
            assertTrue(index.at(t * 1200, 0).isEmpty(), "sem cópia antiga em outro balde");
        }
        assertEquals(original, index.at(0, 0).orElseThrow());
        index.remove(original.id());
        assertEquals(0, index.size());
        for (int t = 0; t < 8; t++) {
            assertTrue(index.at(t * 1200, 0).isEmpty());
        }
    }

    private static Island island(long slot, int cx, int cz, int size) {
        return new Island(UUID.randomUUID(), UUID.randomUUID(), slot, new IslandRegion(cx, cz, size), IslandState.READY);
    }

    @Test
    void findsIslandInsideAndNotOutside() {
        IslandIndex index = new IslandIndex();
        Island a = island(1, 1200, 0, 100);
        index.put(a);

        assertEquals(a.id(), index.at(1200, 0).orElseThrow().id());
        assertEquals(a.id(), index.at(1150, -50).orElseThrow().id()); // canto mínimo (inclusivo)
        assertEquals(a.id(), index.at(1249, 49).orElseThrow().id()); // canto máximo (exclusivo - 1)
        assertTrue(index.at(1250, 0).isEmpty());
        assertTrue(index.at(1149, 0).isEmpty());
        assertTrue(index.at(0, 0).isEmpty()); // spawn
        assertTrue(index.at(600, 0).isEmpty()); // vazio entre ilhas
    }

    @Test
    void worksAcrossNegativeCoordinatesAndBucketBorders() {
        IslandIndex index = new IslandIndex();
        // Região cruza vários baldes de 512 e coordenadas negativas.
        Island a = island(1, -1200, -4800, 1000);
        index.put(a);
        for (int x : new int[] {-1700, -1699, -1201, -1200, -700, -701}) {
            for (int z : new int[] {-5300, -5299, -4800, -4301, -4300}) {
                boolean inside = a.region().contains(x, z);
                assertEquals(inside, index.at(x, z).isPresent(), "x=" + x + " z=" + z);
            }
        }
    }

    @Test
    void distinguishesNeighbours() {
        IslandIndex index = new IslandIndex();
        Island a = island(1, 1200, 0, 1000);
        Island b = island(2, 1200, 1200, 1000);
        index.put(a);
        index.put(b);
        assertEquals(a.id(), index.at(1200, 400).orElseThrow().id());
        assertEquals(b.id(), index.at(1200, 800).orElseThrow().id());
        assertTrue(index.at(1200, 600).isEmpty()); // faixa entre as duas (spacing 1200 > max-size 1000)
    }

    @Test
    void replacingAnIslandMovesItsRegion() {
        IslandIndex index = new IslandIndex();
        Island small = island(1, 1200, 0, 100);
        index.put(small);
        assertTrue(index.at(1280, 0).isEmpty());

        Island grown = new Island(small.id(), small.owner(), 1, small.region().withSize(200), IslandState.READY);
        index.put(grown); // expansão: mesmo id, região maior
        assertEquals(1, index.size());
        assertEquals(small.id(), index.at(1280, 0).orElseThrow().id());

        Island shrunk = new Island(small.id(), small.owner(), 1, small.region().withSize(50), IslandState.READY);
        index.put(shrunk);
        assertTrue(index.at(1280, 0).isEmpty(), "balde antigo não pode reter a região velha");
        assertEquals(small.id(), index.at(1200, 0).orElseThrow().id());
    }

    @Test
    void removeDropsIslandEverywhere() {
        IslandIndex index = new IslandIndex();
        Island a = island(1, 1200, 0, 1000);
        index.put(a);
        index.remove(a.id());
        assertEquals(0, index.size());
        assertTrue(index.at(1200, 0).isEmpty());
        assertTrue(index.at(900, 400).isEmpty());
    }

    @Test
    void handlesManyIslands() {
        IslandIndex index = new IslandIndex();
        for (long slot = 1; slot <= 2000; slot++) {
            index.put(new Island(
                    UUID.randomUUID(), UUID.randomUUID(), slot, IslandRegion.forSlot(slot, 1200, 1000), IslandState.READY));
        }
        for (long slot = 1; slot <= 2000; slot += 37) {
            IslandRegion r = IslandRegion.forSlot(slot, 1200, 1000);
            Island found = index.at(r.centerX(), r.centerZ()).orElseThrow();
            assertEquals(slot, found.slot());
        }
    }
}
