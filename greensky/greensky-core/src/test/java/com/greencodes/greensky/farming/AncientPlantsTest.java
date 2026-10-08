package com.greencodes.greensky.farming;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class AncientPlantsTest {
    @Test
    void positionsIncludeHeightAndHandleNegativeCoordinates() {
        assertEquals(AncientPlants.position(-1, 100, -1), AncientPlants.position(15, 100, 15));
        assertNotEquals(AncientPlants.position(1, -64, 1), AncientPlants.position(1, 0, 1));
        assertNotEquals(AncientPlants.position(1, 100, 2), AncientPlants.position(2, 100, 1));
    }

    @Test
    void marksSurviveNewServiceAndRemovingOnePlantPreservesOtherPlants() {
        Map<NamespacedKey, Object> persisted = new HashMap<>();
        PersistentDataContainer data = proxy(PersistentDataContainer.class, (o, m, a) -> switch (m.getName()) {
            case "get" -> persisted.get(a[0]);
            case "set" -> { persisted.put((NamespacedKey) a[0], a[2]); yield null; }
            case "remove" -> { persisted.remove(a[0]); yield null; }
            default -> throw new AssertionError(m);
        });
        Chunk chunk = proxy(Chunk.class, (o, m, a) -> {
            if (m.getName().equals("getPersistentDataContainer")) return data;
            throw new AssertionError(m);
        });
        Plugin plugin = proxy(Plugin.class, (o, m, a) -> {
            if (m.getName().equals("namespace")) return "greensky";
            if (m.getName().equals("getName")) return "GreenSky";
            throw new AssertionError(m);
        });
        Block first = block(chunk, 1, 100, 2), second = block(chunk, 1, 101, 2);
        AncientPlants initial = new AncientPlants(plugin);
        initial.set(first, true);
        initial.set(first, true); // idempotente
        initial.set(second, true);
        AncientPlants reloaded = new AncientPlants(plugin);
        CropMarkers fertilizer = new CropMarkers(plugin, "fertilized_plants");
        assertFalse(fertilizer.contains(first), "identidade ancestral não equivale a fertilizante");
        fertilizer.set(first, true);
        new CropMarkers(plugin, "fertilized_plants").set(first, false);
        assertTrue(reloaded.contains(first));
        assertTrue(reloaded.contains(second));
        reloaded.set(first, false);
        assertFalse(reloaded.contains(first));
        assertTrue(reloaded.contains(second));
        reloaded.set(second, false);
        assertTrue(persisted.isEmpty(), "não deixar array vazio no chunk");
    }

    private static Block block(Chunk chunk, int x, int y, int z) {
        return proxy(Block.class, (o, m, a) -> switch (m.getName()) {
            case "getChunk" -> chunk;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            default -> throw new AssertionError(m);
        });
    }
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
