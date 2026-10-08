package com.greencodes.greensky.collection;

import com.greencodes.greensky.api.event.CollectionDiscoverEvent;
import com.greencodes.greensky.api.event.GreenSkyPlayerReadyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Carrega o progresso quando o jogador fica pronto (depois do login), grava ao sair, e mostra
 * "Descoberto: X" na primeira vez de cada entrada.
 */
public final class CollectionListener implements Listener {

    private static final Sound DISCOVER_SOUND =
            Sound.sound(Key.key("minecraft", "entity.player.levelup"), Sound.Source.PLAYER, 0.8f, 1.4f);

    private final CollectionService service;
    private final Logger logger;

    public CollectionListener(CollectionService service, Logger logger) {
        this.service = service;
        this.logger = logger;
    }

    @EventHandler
    public void onReady(GreenSkyPlayerReadyEvent event) {
        Player player = event.getPlayer();
        service.load(player.getUniqueId()).exceptionally(error -> {
            logger.log(Level.SEVERE, "Falha ao carregar coleções de " + player.getName(), error);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        service.unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDiscover(CollectionDiscoverEvent event) {
        CollectionCatalog catalog = service.catalog();
        catalog.entry(event.getEntryKey()).ifPresent(entry -> {
            CollectionDefinition collection = catalog.ownerOf(entry.key()).orElse(null);
            int done = collection == null ? 0
                    : collection.discoveredCount(service.snapshot(event.getPlayer().getUniqueId()).keySet());
            int total = collection == null ? 0 : collection.entries().size();
            Component name = Component.text(entry.name(), entry.rarity().color());
            Player player = event.getPlayer();
            player.sendMessage(Component.text("✦ Descoberto: ", NamedTextColor.GREEN)
                    .append(name)
                    .append(Component.text(" [" + entry.rarity().label() + "]", entry.rarity().color()))
                    .append(Component.text(collection == null ? ""
                            : "  (" + collection.name() + " " + done + "/" + total + ")", NamedTextColor.GRAY)));
            player.showTitle(Title.title(Component.text("Descoberto!", NamedTextColor.GREEN), name));
            player.playSound(DISCOVER_SOUND);
        });
    }
}
