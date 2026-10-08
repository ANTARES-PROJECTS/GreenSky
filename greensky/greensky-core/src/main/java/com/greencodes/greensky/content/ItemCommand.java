package com.greencodes.greensky.content;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /greensky item give <nick> <id> [qtd]} e {@code /greensky item check}: administração e
 * diagnóstico dos itens do GreenSky (permissão {@code greensky.admin}).
 */
public final class ItemCommand implements CommandExecutor, TabCompleter {

    private final Server server;
    private final ContentRegistry registry;
    private final ItemFactory factory;

    public ItemCommand(Server server, ContentRegistry registry, ItemFactory factory) {
        this.server = server;
        this.registry = registry;
        this.factory = factory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("greensky.admin")) {
            sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("check")) {
            check(sender);
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
            give(sender, args);
        } else {
            sender.sendMessage(Component.text("Uso: /" + label + " item give <nick> <id> [qtd] | /" + label
                    + " item check", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        Player target = server.getPlayerExact(args[2]);
        Optional<ContentItem> item = registry.item(args[3].toLowerCase(Locale.ROOT));
        if (target == null) {
            sender.sendMessage(Component.text("Jogador '" + args[2] + "' não está online.", NamedTextColor.RED));
            return;
        }
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("Item '" + args[3] + "' não existe no conteúdo carregado.", NamedTextColor.RED));
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
            } catch (NumberFormatException ignored) {
                // fica 1
            }
        }
        // Inventário cheio: o excedente cai aos pés do jogador (não some).
        target.getInventory().addItem(factory.create(item.get(), amount)).values()
                .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        sender.sendMessage(Component.text("Entregue " + item.get().id() + " x" + amount + " para " + target.getName(),
                NamedTextColor.GREEN));
    }

    /** Diz se o item na mão é do GreenSky (pela marca escondida, não pelo nome). */
    private void check(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Use segurando o item.", NamedTextColor.RED));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        Optional<String> id = factory.idOf(hand);
        player.sendMessage(id.map(value -> Component.text("Item do GreenSky: " + value, NamedTextColor.GREEN))
                .orElse(Component.text("Não é item do GreenSky.", NamedTextColor.YELLOW)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("item");
        }
        if (args.length == 2) {
            return List.of("give", "check");
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return registry.items().stream().map(ContentItem::id).filter(id -> id.startsWith(args[3])).toList();
        }
        return args.length == 3 ? null : List.of();
    }
}
