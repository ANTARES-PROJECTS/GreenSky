package com.greencodes.greensky.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * O jogador está pronto para o GreenSky: autenticou no plugin de login (ou o servidor não
 * exige login em modo de desenvolvimento) e a conta dele foi registrada. Toda a lógica de
 * entrada (acesso às ilhas, dicas, borda, comandos) parte deste evento, nunca do join.
 * Disparado na thread do servidor.
 */
public final class GreenSkyPlayerReadyEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GreenSkyPlayerReadyEvent(Player player) {
        super(player);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
