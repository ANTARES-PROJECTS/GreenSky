package com.greencodes.greensky.fishing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detecta pesca parada (AFK/fazenda automática): muitas fisgadas seguidas exatamente da mesma
 * posição e com a mesma mira. Não ouve o movimento do jogador (evento caro); só compara a
 * cada fisgada. Mover-se ou mudar a mira zera a contagem.
 */
public final class AfkFishingGuard {

    /** Posição (bloco) e mira arredondada a 1 grau no momento da fisgada. */
    public record Spot(int x, int y, int z, int yaw, int pitch) {}

    private final int maxCatchesSameSpot;
    private final Map<UUID, State> last = new ConcurrentHashMap<>();

    private record State(Spot spot, int count) {}

    /**
     * @param maxCatchesSameSpot quantas fisgadas seguidas do mesmo jeito ainda contam como normais
     */
    public AfkFishingGuard(int maxCatchesSameSpot) {
        this.maxCatchesSameSpot = maxCatchesSameSpot;
    }

    /** Registra a fisgada e diz se ela já é "parada" (passou do limite no mesmo lugar e mira). */
    public boolean catchIsIdle(UUID player, Spot spot) {
        State updated = last.merge(player, new State(spot, 1),
                (old, fresh) -> old.spot().equals(spot) ? new State(spot, old.count() + 1) : fresh);
        return updated.count() > maxCatchesSameSpot;
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}
