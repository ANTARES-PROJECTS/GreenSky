package com.greencodes.greensky.content;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Cria itens do GreenSky e os reconhece. A identidade fica nos dados persistentes do item
 * ({@code greensky:item = <id>}): renomear na bigorna ou copiar nome/lore num item comum não
 * transforma ninguém em item do GreenSky.
 */
public final class ItemFactory {

    private final NamespacedKey idKey;

    public ItemFactory(Plugin plugin) {
        this.idKey = new NamespacedKey(plugin, "item");
    }

    public ItemStack create(ContentItem item, int amount) {
        ItemStack stack = ItemStack.of(item.material(), Math.max(1, amount));
        stack.editMeta(meta -> {
            // itemName é o nome base (sem itálico); uma renomeação na bigorna não muda a identidade.
            meta.itemName(Component.text(item.name(), item.rarity().color()));
            List<Component> lore = new java.util.ArrayList<>();
            for (String line : item.lore()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text(item.rarity().label().toUpperCase(java.util.Locale.ROOT), item.rarity().color())
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, item.id());
        });
        return stack;
    }

    /** Id do item do GreenSky, ou vazio se for um item comum (mesmo com nome/lore iguais). */
    public Optional<String> idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.getPersistentDataContainer().get(idKey, PersistentDataType.STRING));
    }
}
