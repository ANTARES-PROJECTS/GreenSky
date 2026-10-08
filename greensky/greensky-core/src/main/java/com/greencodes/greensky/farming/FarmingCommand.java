package com.greencodes.greensky.farming;

import com.greencodes.greensky.collection.CollectionTracker;
import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.protection.IslandProtectionService;
import com.greencodes.greensky.world.SkyWorld;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Guia e inspeção local; consultar nunca concede itens ou progresso. */
public final class FarmingCommand implements CommandExecutor {
    private final CropCatalog crops;
    private final AncientPlants ancient;
    private final CropMarkers fertilized;
    private final SkyWorld world;
    private final IslandProtectionService protection;
    private final CollectionTracker collections;
    private final Predicate<Player> ready;

    public FarmingCommand(CropCatalog crops, AncientPlants ancient, CropMarkers fertilized, SkyWorld world,
            IslandProtectionService protection, CollectionTracker collections, Predicate<Player> ready) {
        this.crops = crops; this.ancient = ancient; this.fertilized = fertilized;
        this.world = world; this.protection = protection; this.collections = collections; this.ready = ready;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Use este comando dentro do jogo.", NamedTextColor.YELLOW));
            return true;
        }
        if (!ready.test(player) || !collections.isLoaded(player)) {
            player.sendMessage(Component.text("Faça login e aguarde seus dados carregarem.", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text("=== Agricultura ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Colha trigo, cenoura ou batata maduros: +1 planta em /collections farming."));
        player.sendMessage(Component.text("Produtos colhidos recebem qualidade de uma a três estrelas."));
        player.sendMessage(Component.text("Trigo Dourado: clique direito em planta imatura; consome 1, avança 2 estágios e garante ★★★."));
        player.sendMessage(Component.text("Semente Ancestral: plante no solo arado; madura rende 1 Trigo Dourado ★★★; imatura devolve a semente."));
        player.sendMessage(Component.text("Uma fertilização por planta; quebrar imatura perde o tratamento."));
        if (!world.contains(player.getWorld())) return true;
        var block = player.getTargetBlockExact(5);
        if (block == null || crops.crop(block.getType()).isEmpty()
                || !protection.can(player.getUniqueId(), IslandPermission.INTERACT, block.getX(), block.getZ())
                || !(block.getBlockData() instanceof Ageable age)) {
            player.sendMessage(Component.text("Olhe para uma cultura na sua ilha, a até 5 blocos, para consultar o estado.", NamedTextColor.GRAY));
            return true;
        }
        var crop = crops.crop(block.getType()).orElseThrow();
        player.sendMessage(Component.text("Planta: " + crop.product().name() + " — estágio " + age.getAge()
                + "/" + age.getMaximumAge(), NamedTextColor.GREEN));
        if (ancient.contains(block)) {
            player.sendMessage(Component.text("Ancestral: a colheita madura entrega 1 Trigo Dourado ★★★.", NamedTextColor.GOLD));
        } else if (fertilized.contains(block)) {
            player.sendMessage(Component.text("Fertilizada: qualidade ★★★ garantida na colheita madura.", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("Sem fertilizante: qualidade sorteada na colheita madura.", NamedTextColor.GRAY));
        }
        return true;
    }
}
