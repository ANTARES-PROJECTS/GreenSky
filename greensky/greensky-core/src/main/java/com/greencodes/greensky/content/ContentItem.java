package com.greencodes.greensky.content;

import java.util.List;
import org.bukkit.Material;

/**
 * Item do GreenSky definido em {@code content/items.yml}. A identidade é o {@code id}, gravado
 * escondido no próprio item (nunca o nome ou a lore, que qualquer bigorna muda).
 */
public record ContentItem(String id, Material material, String name, Rarity rarity, List<String> lore) {

    public ContentItem {
        lore = List.copyOf(lore);
    }
}
