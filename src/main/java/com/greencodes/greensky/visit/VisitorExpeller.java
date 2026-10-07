package com.greencodes.greensky.visit;

import com.greencodes.greensky.core.GreenScheduler;
import com.greencodes.greensky.island.Island;
import com.greencodes.greensky.island.IslandListener;
import com.greencodes.greensky.island.IslandVisibility;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.protection.PlayerProtectionListener;
import com.greencodes.greensky.world.SkyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Quando uma ilha fica privada, quem está nela sem ser membro é levado ao spawn do
 * servidor. Sem isso, "privada" só valeria para novas visitas.
 */
public final class VisitorExpeller implements IslandListener {

    private final Server server;
    private final SkyWorld skyWorld;
    private final IslandProtectionService protection;
    private final GreenScheduler scheduler;

    public VisitorExpeller(
            Server server, SkyWorld skyWorld, IslandProtectionService protection, GreenScheduler scheduler) {
        this.server = server;
        this.skyWorld = skyWorld;
        this.protection = protection;
        this.scheduler = scheduler;
    }

    @Override
    public void onVisibilityChanged(Island island, IslandVisibility visibility) {
        if (visibility != IslandVisibility.PRIVATE) {
            return;
        }
        scheduler.runSync(() -> {
            Location spawn = server.getWorlds().get(0).getSpawnLocation();
            for (Player player : skyWorld.bukkit().getPlayers()) {
                Location at = player.getLocation();
                if (island.region().contains(at.getBlockX(), at.getBlockZ())
                        && !protection.isMemberOf(player.getUniqueId(), island.id())
                        && !player.hasPermission(PlayerProtectionListener.BYPASS)) {
                    player.teleportAsync(spawn);
                    player.sendMessage(Component.text("Esta ilha agora é privada.", NamedTextColor.YELLOW));
                }
            }
        });
    }
}
