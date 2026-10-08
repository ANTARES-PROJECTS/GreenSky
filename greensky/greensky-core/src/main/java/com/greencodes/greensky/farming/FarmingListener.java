package com.greencodes.greensky.farming;

import com.greencodes.greensky.collection.CollectionService;
import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.content.ItemFactory;
import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

/** Conta a planta madura quebrada pelo jogador; pegar drops ou colher por máquinas não conta. */
public final class FarmingListener implements Listener {
    private final CropCatalog crops;
    private final IslandProtectionService protection;
    private final SkyWorld world;
    private final CollectionTracker collections;
    private final ItemFactory items;
    private final CropItemFactory products;
    private final AncientPlants ancient;
    private final CropMarkers fertilized;
    private final Predicate<Player> ready;
    private final RandomGenerator random;

    public FarmingListener(CropCatalog crops, IslandProtectionService protection, SkyWorld world,
            CollectionTracker collections, ItemFactory items, CropItemFactory products, AncientPlants ancient, CropMarkers fertilized,
            Predicate<Player> ready, RandomGenerator random) {
        this.crops = crops;
        this.protection = protection;
        this.world = world;
        this.collections = collections;
        this.items = items;
        this.products = products;
        this.ancient = ancient;
        this.fertilized = fertilized;
        this.ready = ready;
        this.random = random;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onQuality(BlockDropItemEvent event) {
        if (event.getBlockState().getType() == org.bukkit.Material.WHEAT
                && ancient.contains(event.getBlock()) && permitted(event)) {
            var age = (Ageable) event.getBlockState().getBlockData();
            boolean mature = age.getAge() == age.getMaximumAge();
            var wheat = crops.crop(org.bukkit.Material.WHEAT).orElseThrow();
            String id = mature ? "crop.golden_wheat" : AncientPlantListener.SEED;
            var definition = wheat.bonuses().stream().map(CropCatalog.Bonus::item)
                    .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
            var reward = mature ? products.create(definition, 1, 3) : items.create(definition, 1);
            var first = event.getItems().getFirst();
            first.setItemStack(reward);
            event.getItems().removeIf(drop -> drop != first);
            return;
        }
        if (!eligible(event)) return;
        crops.crop(event.getBlockState().getType()).ifPresent(crop -> {
            int quality = fertilized.contains(event.getBlock()) ? 3 : crops.quality().roll(random); // mesmo sorteio para todos os drops desta planta
            for (var drop : event.getItems()) {
                var stack = drop.getItemStack();
                if (stack.getType() == crop.product().material() && items.idOf(stack).isEmpty()) {
                    drop.setItemStack(products.create(crop.product(), stack.getAmount(), quality));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(BlockDropItemEvent event) {
        if (world.contains(event.getBlock().getWorld())) fertilized.set(event.getBlock(), false);
        var state = event.getBlockState();
        Player player = event.getPlayer();
        if (state.getType() == org.bukkit.Material.WHEAT && ancient.contains(event.getBlock()) && permitted(event)) {
            ancient.set(event.getBlock(), false);
            var age = (Ageable) state.getBlockData();
            if (age.getAge() == age.getMaximumAge()) {
                collections.record(player, "crop.wheat", 1);
                collections.record(player, "crop.golden_wheat", 1);
            }
            return;
        }
        if (!eligible(event)) return;
        crops.crop(state.getType()).ifPresent(crop -> {
            CollectionService.Outcome outcome = collections.record(player, crop.entry(), 1);
            if (!accepted(outcome)) return;
            crop.roll(random).ifPresent(bonus -> {
                var reward = items.create(bonus, 1);
                if (!accepted(collections.record(player, bonus.id(), 1))) return;
                state.getWorld().dropItemNaturally(state.getLocation().add(0.5, 0.5, 0.5), reward);
                player.sendMessage(Component.text("» Achado da colheita: " + bonus.name(), bonus.rarity().color()));
            });
        });
    }

    private boolean eligible(BlockDropItemEvent event) {
        return permitted(event) && ((Ageable) event.getBlockState().getBlockData()).getAge()
                == ((Ageable) event.getBlockState().getBlockData()).getMaximumAge();
    }

    private boolean permitted(BlockDropItemEvent event) {
        var state = event.getBlockState();
        Player player = event.getPlayer();
        return world.contains(state.getWorld()) && ready.test(player) && collections.isLoaded(player)
                && !event.getItems().isEmpty() && state.getBlockData() instanceof Ageable
                && protection.can(player.getUniqueId(), IslandPermission.BREAK, state.getX(), state.getZ());
    }

    private static boolean accepted(CollectionService.Outcome outcome) {
        return outcome == CollectionService.Outcome.DISCOVERED || outcome == CollectionService.Outcome.PROGRESSED;
    }
}
