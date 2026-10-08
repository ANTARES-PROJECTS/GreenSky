package com.greencodes.greensky.world;

import static org.junit.jupiter.api.Assertions.*;

import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.collection.CollectionRepository;
import com.greencodes.greensky.collection.CollectionService;
import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.content.ContentYaml;
import com.greencodes.greensky.core.config.FishingSettings;
import com.greencodes.greensky.fishing.FishTable;
import com.greencodes.greensky.fishing.FishingListener;
import com.greencodes.greensky.protection.IslandProtectionService;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class FishingReadinessTest {
    @Test
    void authenticatedPlayerWithoutLoadedCollectionsKeepsVanillaCatchUntouched() {
        UUID id = UUID.randomUUID();
        World world = proxy(World.class, (object, method, args) -> switch (method.getName()) {
            case "equals" -> object == args[0];
            case "hashCode" -> System.identityHashCode(object);
            default -> throw new AssertionError("mundo não deve ser consultado: " + method);
        });
        Player player = proxy(Player.class, (object, method, args) -> {
            if (method.getName().equals("getUniqueId")) return id;
            throw new AssertionError("não deve processar fisgada: " + method);
        });
        Item caught = proxy(Item.class, (object, method, args) -> {
            throw new AssertionError("item vanilla não deve ser lido nem substituído: " + method);
        });
        FishHook hook = proxy(FishHook.class, (object, method, args) -> {
            if (method.getName().equals("getLocation")) return new Location(world, 1200, 100, 0);
            throw new AssertionError("não deve avaliar água: " + method);
        });
        Plugin plugin = proxy(Plugin.class, (object, method, args) -> {
            if (method.getName().equals("getName")) return "GreenSky";
            if (method.getName().equals("namespace")) return "greensky";
            throw new AssertionError(method);
        });
        CollectionCatalog catalog = CollectionCatalog.load(ContentYaml.parse("""
                collections:
                  fishing:
                    name: Pesca
                    entries:
                      fish.cod: { name: Bacalhau, rarity: COMMON }
                """));
        CollectionService collections = new CollectionService(null, new CollectionRepository(), catalog);
        FishingListener listener = new FishingListener(plugin, new FishTable(List.of()),
                new FishingSettings(true, 10), new IslandProtectionService(), new SkyWorld(world, 100),
                null, new CollectionTracker(collections, null), p -> true, new Random());
        PlayerFishEvent event = new PlayerFishEvent(player, caught, hook, EquipmentSlot.HAND,
                PlayerFishEvent.State.CAUGHT_FISH);
        listener.onFish(event);
        assertFalse(event.isCancelled());
        assertTrue(collections.snapshot(id).isEmpty());
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
