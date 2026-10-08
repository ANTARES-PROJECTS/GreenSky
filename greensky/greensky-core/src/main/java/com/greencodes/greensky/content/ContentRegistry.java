package com.greencodes.greensky.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Itens do GreenSky, lidos de {@code content/items.yml} e validados no boot. Imutável. */
public final class ContentRegistry {

    private final Map<String, ContentItem> items;

    private ContentRegistry(Map<String, ContentItem> items) {
        this.items = Map.copyOf(items);
    }

    /** Lê a seção {@code items} validando o material com o registro do servidor ({@link Material#isItem}). */
    public static ContentRegistry load(ConfigurationSection root) {
        return load(root, Material::isItem);
    }

    /**
     * Lê a seção {@code items}. Qualquer erro impede o boot, com a mensagem dizendo onde.
     *
     * @param isItem se o material pode existir como item (o real precisa do servidor; testes passam outro)
     * @throws IllegalArgumentException se algum item for inválido
     */
    public static ContentRegistry load(ConfigurationSection root, Predicate<Material> isItem) {
        Map<String, ContentItem> items = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("items");
        if (section == null) {
            throw new IllegalArgumentException("items.yml: seção 'items' ausente");
        }
        for (String id : section.getKeys(false)) {
            String where = "items.yml > " + id;
            ContentKeys.require(id, "items.yml");
            ConfigurationSection item = section.getConfigurationSection(id);
            if (item == null) {
                throw new IllegalArgumentException(where + ": esperado um bloco com material, name e rarity");
            }
            String materialName = item.getString("material");
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material == null || !isItem.test(material)) {
                throw new IllegalArgumentException(where + ": material inválido '" + materialName + "'");
            }
            String name = item.getString("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(where + ": 'name' é obrigatório");
            }
            Rarity rarity;
            try {
                rarity = Rarity.parse(item.getString("rarity"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(where + ": " + e.getMessage());
            }
            List<String> lore = new ArrayList<>(item.getStringList("lore"));
            items.put(id, new ContentItem(id, material, name, rarity, lore));
        }
        return new ContentRegistry(items);
    }

    public Optional<ContentItem> item(String id) {
        return Optional.ofNullable(items.get(id));
    }

    /** Acrescenta definições de atividades sem permitir duas identidades para o mesmo id. */
    public ContentRegistry withItems(Collection<ContentItem> additions) {
        Map<String, ContentItem> merged = new LinkedHashMap<>(items);
        for (ContentItem item : additions) {
            if (merged.putIfAbsent(item.id(), item) != null) {
                throw new IllegalArgumentException("Conteúdo: id duplicado entre catálogos: " + item.id());
            }
        }
        return new ContentRegistry(merged);
    }

    public Collection<ContentItem> items() {
        return items.values();
    }
}
