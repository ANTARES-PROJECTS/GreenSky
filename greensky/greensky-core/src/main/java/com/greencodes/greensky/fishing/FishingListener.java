package com.greencodes.greensky.fishing;

import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.collection.CollectionService;
import com.greencodes.greensky.content.ContentItem;
import com.greencodes.greensky.content.ItemFactory;
import com.greencodes.greensky.core.config.FishingSettings;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Pesca do GreenSky. Na fisgada de um <b>peixe</b> vanilla, troca o item por um peixe do
 * GreenSky e registra na coleção — só se todas as regras passarem; senão a pesca fica vanilla.
 * Lixo e tesouro vanilla nunca são trocados.
 *
 * <p>Regras anti-exploit: só no SkyWorld; só na ilha de que o jogador é dono ou membro
 * (visitante não alimenta a própria coleção na ilha alheia); só em água aberta (barra o
 * "balde de 1 bloco"); sem pesca parada ({@link AfkFishingGuard}). A coleção conta aqui, na
 * fisgada, nunca ao pegar o item: com inventário cheio o peixe cai no chão sem contar de novo.
 */
public final class FishingListener implements Listener {

    /** Peixes vanilla que são substituídos (lixo e tesouro ficam como estão). */
    private static final Set<Material> VANILLA_FISH =
            Set.of(Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH);

    private final FishTable table;
    private final FishingSettings settings;
    private final AfkFishingGuard afkGuard;
    private final IslandProtectionService protection;
    private final SkyWorld skyWorld;
    private final ItemFactory items;
    private final CollectionTracker collections;
    private final Predicate<Player> ready;
    private final RandomGenerator random;
    private final NamespacedKey sizeKey;
    private final NamespacedKey starsKey;
    /** Jogadores já avisados de pesca parada (avisa uma vez por sequência). */
    private final Set<UUID> warnedIdle = ConcurrentHashMap.newKeySet();

    public FishingListener(
            Plugin plugin,
            FishTable table,
            FishingSettings settings,
            IslandProtectionService protection,
            SkyWorld skyWorld,
            ItemFactory items,
            CollectionTracker collections,
            Predicate<Player> ready,
            RandomGenerator random) {
        this.table = table;
        this.settings = settings;
        this.afkGuard = new AfkFishingGuard(settings.afkMaxCatchesSameSpot());
        this.protection = protection;
        this.skyWorld = skyWorld;
        this.items = items;
        this.collections = collections;
        this.ready = ready;
        this.random = random;
        this.sizeKey = new NamespacedKey(plugin, "fish_size_cm");
        this.starsKey = new NamespacedKey(plugin, "fish_stars");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        Location at = hook.getLocation();
        if (!skyWorld.contains(at.getWorld()) || !ready.test(player) || !collections.isLoaded(player)) {
            return;
        }

        // Pesca parada conta qualquer fisgada (inclusive lixo/tesouro), para a sequência não "zerar" com sorte.
        if (isIdle(player)) {
            return;
        }
        if (!VANILLA_FISH.contains(caught.getItemStack().getType())) {
            return; // lixo e tesouro vanilla: mantém
        }
        if (!protection.isMemberAt(player.getUniqueId(), at.getBlockX(), at.getBlockZ())) {
            return; // ilha alheia ou fora de ilha: vanilla
        }
        if (settings.requireOpenWater() && !hook.isInOpenWater()) {
            return; // poça/balde: vanilla
        }
        Optional<FishCatch> roll = table.roll(FishingEnvironment.of(at.getWorld()), random);
        if (roll.isEmpty()) {
            return;
        }
        FishCatch fishCatch = roll.get();
        ItemStack reward = createItem(fishCatch);
        CollectionService.Outcome outcome = collections.record(player, fishCatch.fish().id(), 1);
        if (outcome != CollectionService.Outcome.DISCOVERED && outcome != CollectionService.Outcome.PROGRESSED) {
            return;
        }
        caught.setItemStack(reward);
        // Só símbolos do plano básico Unicode: emojis como 🎣 aparecem como quadrados na fonte do Minecraft.
        player.sendMessage(Component.text("» Você pescou ", NamedTextColor.AQUA)
                .append(Component.text(fishCatch.fish().name(), fishCatch.fish().rarity().color()))
                .append(Component.text(" — " + formatSize(fishCatch.sizeCm()) + " cm " + fishCatch.starsText()
                        + " [" + fishCatch.fish().rarity().label() + "]", NamedTextColor.GRAY)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        afkGuard.forget(event.getPlayer().getUniqueId());
        warnedIdle.remove(event.getPlayer().getUniqueId());
    }

    private boolean isIdle(Player player) {
        Location eye = player.getLocation();
        AfkFishingGuard.Spot spot = new AfkFishingGuard.Spot(eye.getBlockX(), eye.getBlockY(), eye.getBlockZ(),
                Math.round(eye.getYaw()), Math.round(eye.getPitch()));
        boolean idle = afkGuard.catchIsIdle(player.getUniqueId(), spot);
        if (!idle) {
            warnedIdle.remove(player.getUniqueId());
        } else if (warnedIdle.add(player.getUniqueId())) {
            player.sendMessage(Component.text("Pesca parada detectada: mude de lugar ou de mira para voltar a pescar"
                    + " peixes do GreenSky.", NamedTextColor.YELLOW));
        }
        return idle;
    }

    private ItemStack createItem(FishCatch fishCatch) {
        FishDefinition fish = fishCatch.fish();
        ItemStack stack = items.create(new ContentItem(fish.id(), fish.material(), fish.name(), fish.rarity(), List.of()), 1);
        stack.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Tamanho: " + formatSize(fishCatch.sizeCm()) + " cm", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Qualidade: " + fishCatch.starsText(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> existing = meta.lore();
            if (existing != null) {
                lore.addAll(existing);
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(sizeKey, PersistentDataType.DOUBLE, fishCatch.sizeCm());
            meta.getPersistentDataContainer().set(starsKey, PersistentDataType.INTEGER, fishCatch.stars());
        });
        return stack;
    }

    private static String formatSize(double size) {
        return String.format(Locale.ROOT, "%.1f", size);
    }
}
