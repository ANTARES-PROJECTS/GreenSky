package com.greencodes.greensky.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * O jogador obteve pela primeira vez uma entrada de coleção (peixe, colheita, região...).
 * Disparado uma única vez por jogador e entrada, na thread do servidor.
 */
public final class CollectionDiscoverEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String collectionId;
    private final String entryKey;

    public CollectionDiscoverEvent(Player player, String collectionId, String entryKey) {
        super(player);
        this.collectionId = collectionId;
        this.entryKey = entryKey;
    }

    /** Id da coleção (ex.: {@code fishing}). */
    public String getCollectionId() {
        return collectionId;
    }

    /** Chave da entrada descoberta (ex.: {@code fish.cod}). */
    public String getEntryKey() {
        return entryKey;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
