package com.greencodes.greensky.farming;

import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.content.ItemFactory;
import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/** Uma aplicação por planta; o efeito acompanha o save do chunk. */
public final class FertilizerListener implements Listener {
    private final CropCatalog crops;
    private final AncientPlants ancient;
    private final CropMarkers fertilized;
    private final ItemFactory items;
    private final IslandProtectionService protection;
    private final SkyWorld world;
    private final Predicate<Player> ready;
    private final CollectionTracker collections;

    public FertilizerListener(CropCatalog crops, AncientPlants ancient, CropMarkers fertilized,
            ItemFactory items, IslandProtectionService protection, SkyWorld world,
            Predicate<Player> ready, CollectionTracker collections) {
        this.crops = crops; this.ancient = ancient; this.fertilized = fertilized;
        this.items = items; this.protection = protection; this.world = world;
        this.ready = ready; this.collections = collections;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onApply(PlayerInteractEvent event) {
        // Minecraft pode negar só a ação vanilla de trigo na mão; respeitar negação explícita do bloco.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null
                || event.useInteractedBlock() == Event.Result.DENY
                || !items.idOf(event.getItem()).filter("crop.golden_wheat"::equals).isPresent()) return;
        var block = event.getClickedBlock();
        if (block == null || !world.contains(block.getWorld()) || crops.crop(block.getType()).isEmpty()) return;
        var player = event.getPlayer();
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (player.getGameMode() != GameMode.SURVIVAL || !ready.test(player) || !collections.isLoaded(player)
                || !protection.can(player.getUniqueId(), IslandPermission.BUILD, block.getX(), block.getZ())
                || !protection.can(player.getUniqueId(), IslandPermission.INTERACT, block.getX(), block.getZ())
                || ancient.contains(block) || fertilized.contains(block)
                || !(block.getBlockData() instanceof Ageable age) || age.getAge() >= age.getMaximumAge()) return;
        var held = player.getInventory().getItem(event.getHand());
        if (!items.idOf(held).filter("crop.golden_wheat"::equals).isPresent() || held.getAmount() < 1) return;
        age.setAge(Math.min(age.getMaximumAge(), age.getAge() + 2));
        block.setBlockData(age);
        fertilized.set(block, true);
        var remaining = held.clone();
        remaining.setAmount(held.getAmount() - 1);
        player.getInventory().setItem(event.getHand(), remaining);
        player.sendMessage(Component.text("» Planta fertilizada: a colheita madura terá ★★★."));
    }
}
