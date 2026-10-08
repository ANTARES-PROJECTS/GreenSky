package com.greencodes.greensky.collection;

import com.greencodes.greensky.api.event.CollectionDiscoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 * Porta de entrada das atividades (pesca, agricultura, exploração...) para as coleções.
 * Chamar na thread do servidor: na primeira vez de uma entrada dispara
 * {@link CollectionDiscoverEvent}.
 */
public final class CollectionTracker {

    private final CollectionService service;
    private final PluginManager plugins;

    public CollectionTracker(CollectionService service, PluginManager plugins) {
        this.service = service;
        this.plugins = plugins;
    }

    public boolean isLoaded(Player player) {
        return service.isLoaded(player.getUniqueId());
    }

    public CollectionService.Outcome record(Player player, String entryKey, long amount) {
        CollectionService.Outcome outcome = service.record(player.getUniqueId(), entryKey, amount);
        if (outcome == CollectionService.Outcome.DISCOVERED) {
            String collectionId = service.catalog().ownerOf(entryKey).map(CollectionDefinition::id).orElse("");
            plugins.callEvent(new CollectionDiscoverEvent(player, collectionId, entryKey));
        }
        return outcome;
    }
}
