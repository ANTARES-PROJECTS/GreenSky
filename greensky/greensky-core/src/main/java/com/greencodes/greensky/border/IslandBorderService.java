package com.greencodes.greensky.border;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.island.Island;
import com.greencodes.greensky.island.IslandListener;
import com.greencodes.greensky.island.IslandRegion;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

/**
 * Borda visual por jogador: quem está dentro de uma ilha vê a borda do mundo exatamente
 * na região liberada dela. É só visual (o cliente desenha e colide); a proteção real é do
 * {@link IslandProtectionService}. Fora de ilhas ou fora do mundo SkyBlock, o jogador volta
 * a ver a borda normal do mundo.
 */
public final class IslandBorderService implements IslandListener {

    private final Server server;
    private final SkyWorld skyWorld;
    private final IslandProtectionService protection;
    private final GreenScheduler scheduler;

    public IslandBorderService(
            Server server, SkyWorld skyWorld, IslandProtectionService protection, GreenScheduler scheduler) {
        this.server = server;
        this.skyWorld = skyWorld;
        this.protection = protection;
        this.scheduler = scheduler;
    }

    /** Ajusta a borda do jogador à posição atual. Chamar na thread do servidor. */
    public void refresh(Player player) {
        Location at = player.getLocation();
        Optional<Island> island = skyWorld.contains(at.getWorld())
                ? protection.islandAt(at.getBlockX(), at.getBlockZ())
                : Optional.empty();
        if (island.isEmpty()) {
            if (player.getWorldBorder() != null) {
                player.setWorldBorder(null);
            }
            return;
        }
        player.setWorldBorder(borderFor(island.get().region()));
    }

    /** A região é [min, min+size); o centro geométrico fica em min + size/2. */
    WorldBorder borderFor(IslandRegion region) {
        WorldBorder border = server.createWorldBorder();
        border.setCenter(region.minX() + region.size() / 2.0, region.minZ() + region.size() / 2.0);
        border.setSize(region.size());
        border.setWarningDistance(2);
        return border;
    }

    /** Ilha cresceu: quem está nela vê a borda nova na hora (vem de thread do banco). */
    @Override
    public void onIslandExpanded(Island island) {
        scheduler.runSync(() -> {
            for (Player player : skyWorld.bukkit().getPlayers()) {
                Location at = player.getLocation();
                if (island.region().contains(at.getBlockX(), at.getBlockZ())) {
                    refresh(player);
                }
            }
        });
    }

    /** Usado por quem não está na thread do servidor. */
    public void refreshLater(Player player) {
        scheduler.runSync(() -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }
}
