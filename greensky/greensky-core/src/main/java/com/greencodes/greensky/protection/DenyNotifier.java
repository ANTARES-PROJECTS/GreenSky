package com.greencodes.greensky.protection;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Avisa o jogador (action bar) que a ação foi negada, no máximo uma vez por segundo. */
public final class DenyNotifier {

    private static final long INTERVAL_NANOS = 1_000_000_000L;
    private static final Component MESSAGE = Component.text("Você não tem permissão aqui.", NamedTextColor.RED);

    private final ConcurrentHashMap<UUID, Long> last = new ConcurrentHashMap<>();

    public void denied(Player player) {
        long now = System.nanoTime();
        Long previous = last.get(player.getUniqueId());
        if (previous == null || now - previous >= INTERVAL_NANOS) {
            last.put(player.getUniqueId(), now);
            player.sendActionBar(MESSAGE);
        }
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}
