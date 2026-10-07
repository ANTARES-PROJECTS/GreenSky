package com.greencodes.greensky.protection;

import com.greencodes.greensky.island.Island;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Índice espacial das ilhas em memória: dado (x, z), acha a ilha que contém o ponto sem
 * consultar o banco. Divide o mundo em baldes de 512x512 e guarda a ilha em cada balde que
 * sua região toca; a busca vê só as poucas ilhas do balde. Não depende do {@code spacing}
 * do config, apenas da região gravada de cada ilha. Seguro para uso concorrente.
 */
public final class IslandIndex {

    private static final int BUCKET_SHIFT = 9;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Island>> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Island> byId = new ConcurrentHashMap<>();

    /** Insere a ilha ou substitui a de mesmo id (ex.: após expandir a região). */
    public void put(Island island) {
        remove(island.id());
        byId.put(island.id(), island);
        forEachBucket(island, key -> buckets.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(island));
    }

    public void remove(UUID islandId) {
        Island old = byId.remove(islandId);
        if (old != null) {
            forEachBucket(old, key -> {
                CopyOnWriteArrayList<Island> list = buckets.get(key);
                if (list != null) {
                    list.removeIf(i -> i.id().equals(islandId));
                    if (list.isEmpty()) {
                        buckets.remove(key, list);
                    }
                }
            });
        }
    }

    /** Ilha cuja região contém o bloco (x, z), se houver. */
    public Optional<Island> at(int x, int z) {
        CopyOnWriteArrayList<Island> list = buckets.get(key(x >> BUCKET_SHIFT, z >> BUCKET_SHIFT));
        if (list == null) {
            return Optional.empty();
        }
        for (Island island : list) {
            if (island.region().contains(x, z)) {
                return Optional.of(island);
            }
        }
        return Optional.empty();
    }

    public Optional<Island> get(UUID islandId) {
        return Optional.ofNullable(byId.get(islandId));
    }

    public int size() {
        return byId.size();
    }

    private static void forEachBucket(Island island, java.util.function.LongConsumer action) {
        var r = island.region();
        for (int bx = r.minX() >> BUCKET_SHIFT; bx <= (r.maxX() - 1) >> BUCKET_SHIFT; bx++) {
            for (int bz = r.minZ() >> BUCKET_SHIFT; bz <= (r.maxZ() - 1) >> BUCKET_SHIFT; bz++) {
                action.accept(key(bx, bz));
            }
        }
    }

    private static long key(int bx, int bz) {
        return ((long) bx << 32) ^ (bz & 0xffffffffL);
    }
}
