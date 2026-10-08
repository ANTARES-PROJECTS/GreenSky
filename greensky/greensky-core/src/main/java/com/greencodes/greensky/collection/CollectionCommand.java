package com.greencodes.greensky.collection;

import com.greencodes.greensky.core.GreenScheduler;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * {@code /collections [id]}: registro de descobertas. {@code /collections admin grant <nick>
 * <entrada> [qtd]}: concede (testes e suporte). Só traduz comandos; regra fica no serviço.
 */
public final class CollectionCommand implements CommandExecutor, TabCompleter {

    private static final int BAR_WIDTH = 10;

    private final Server server;
    private final CollectionService service;
    private final CollectionTracker tracker;
    private final Predicate<Player> ready;
    private final GreenScheduler scheduler;

    public CollectionCommand(
            Server server,
            CollectionService service,
            CollectionTracker tracker,
            Predicate<Player> ready,
            GreenScheduler scheduler) {
        this.server = server;
        this.service = service;
        this.tracker = tracker;
        this.ready = ready;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            admin(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return true;
        }
        if (!ready.test(player) || !service.isLoaded(player.getUniqueId())) {
            player.sendMessage(Component.text("Faça login primeiro.", NamedTextColor.RED));
            return true;
        }
        Map<String, Long> mine = service.snapshot(player.getUniqueId());
        if (args.length == 0) {
            overview(player, mine);
        } else {
            details(player, args[0].toLowerCase(Locale.ROOT), mine);
        }
        return true;
    }

    private void overview(Player player, Map<String, Long> mine) {
        player.sendMessage(Component.text("=== Coleções ===", NamedTextColor.GOLD));
        for (CollectionDefinition collection : service.catalog().collections()) {
            int done = collection.discoveredCount(mine.keySet());
            int total = collection.entries().size();
            player.sendMessage(Component.text(collection.name() + " (" + collection.id() + ") ", NamedTextColor.YELLOW)
                    .append(Component.text(ProgressBar.render(done, total, BAR_WIDTH) + "  " + done + "/" + total,
                            done == total ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        }
        player.sendMessage(Component.text("Use /collections <id> para ver os detalhes.", NamedTextColor.DARK_GRAY));
    }

    private void details(Player player, String id, Map<String, Long> mine) {
        CollectionDefinition collection = service.catalog().collection(id).orElse(null);
        if (collection == null) {
            player.sendMessage(Component.text("Coleção '" + id + "' não existe.", NamedTextColor.RED));
            return;
        }
        int done = collection.discoveredCount(mine.keySet());
        player.sendMessage(Component.text("=== " + collection.name() + " " + ProgressBar.render(done,
                collection.entries().size(), BAR_WIDTH) + " ===", NamedTextColor.GOLD));
        for (CollectionEntry entry : collection.entries()) {
            Long amount = mine.get(entry.key());
            if (amount != null) {
                player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                        .append(Component.text(entry.name(), entry.rarity().color()))
                        .append(Component.text(" [" + entry.rarity().label() + "] x" + amount, NamedTextColor.GRAY)));
            } else if (entry.secret()) {
                player.sendMessage(Component.text("✘ ???", NamedTextColor.DARK_GRAY));
            } else {
                player.sendMessage(Component.text("✘ " + entry.name() + " [" + entry.rarity().label() + "]",
                        NamedTextColor.DARK_GRAY));
            }
        }
    }

    /** {@code /collections admin grant <nick> <entrada> [qtd]}: só para jogador online e pronto. */
    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("greensky.admin")) {
            sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("grant")) {
            sender.sendMessage(Component.text("Uso: /collections admin grant <nick> <entrada> [quantidade]",
                    NamedTextColor.YELLOW));
            return;
        }
        Player target = server.getPlayerExact(args[2]);
        if (target == null || !ready.test(target)) {
            sender.sendMessage(Component.text("Jogador '" + args[2] + "' não está online/logado.", NamedTextColor.RED));
            return;
        }
        long amount;
        try {
            amount = args.length >= 5 ? Long.parseLong(args[4]) : 1;
        } catch (NumberFormatException e) {
            amount = -1;
        }
        if (amount <= 0) {
            sender.sendMessage(Component.text("Quantidade inválida.", NamedTextColor.RED));
            return;
        }
        String key = args[3].toLowerCase(Locale.ROOT);
        long finalAmount = amount;
        // O registro dispara evento do Bukkit: sempre na thread do servidor.
        scheduler.runSync(() -> {
            CollectionService.Outcome outcome = tracker.record(target, key, finalAmount);
            sender.sendMessage(Component.text("Coleção: " + key + " +" + finalAmount + " para " + target.getName()
                    + " -> " + outcome, outcome == CollectionService.Outcome.DISCOVERED
                            || outcome == CollectionService.Outcome.PROGRESSED ? NamedTextColor.GREEN : NamedTextColor.RED));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return service.catalog().collections().stream()
                    .map(CollectionDefinition::id)
                    .filter(id -> id.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
