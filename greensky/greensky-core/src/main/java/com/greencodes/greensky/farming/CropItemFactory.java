package com.greencodes.greensky.farming;

import com.greencodes.greensky.content.ContentItem;
import com.greencodes.greensky.content.ItemFactory;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Produtos agrícolas usam a identidade comum de itens e qualidade persistente separada. */
public final class CropItemFactory {
    private final ItemFactory items;
    private final NamespacedKey qualityKey;
    public CropItemFactory(Plugin plugin, ItemFactory items) {
        this.items = items;
        qualityKey = new NamespacedKey(plugin, "crop_quality");
    }
    public ItemStack create(ContentItem crop, int amount, int quality) {
        String stars = CropQuality.stars(quality);
        ItemStack stack = items.create(crop, amount);
        stack.editMeta(meta -> {
            var lore = new ArrayList<Component>();
            lore.add(Component.text("Qualidade: " + stars, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            if (meta.lore() != null) lore.addAll(meta.lore());
            meta.lore(lore);
            meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.INTEGER, quality);
        });
        return stack;
    }
}
