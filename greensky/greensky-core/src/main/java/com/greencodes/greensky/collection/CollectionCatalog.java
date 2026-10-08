package com.greencodes.greensky.collection;

import com.greencodes.greensky.content.ContentKeys;
import com.greencodes.greensky.content.Rarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Coleções e entradas, lidas de {@code content/collections.yml} e validadas no boot. Imutável. */
public final class CollectionCatalog {

    private final List<CollectionDefinition> collections;
    /** chave da entrada -> coleção dona (cada chave pertence a uma única coleção). */
    private final Map<String, CollectionDefinition> owner;
    private final Map<String, CollectionEntry> entries;

    private CollectionCatalog(List<CollectionDefinition> collections) {
        this.collections = List.copyOf(collections);
        Map<String, CollectionDefinition> owner = new HashMap<>();
        Map<String, CollectionEntry> entries = new HashMap<>();
        for (CollectionDefinition collection : collections) {
            for (CollectionEntry entry : collection.entries()) {
                owner.put(entry.key(), collection);
                entries.put(entry.key(), entry);
            }
        }
        this.owner = Map.copyOf(owner);
        this.entries = Map.copyOf(entries);
    }

    /**
     * Lê a seção {@code collections}. Ids de coleção e chaves de entrada são validados; uma chave
     * repetida (mesmo em coleções diferentes) é erro, pois o progresso é gravado pela chave.
     *
     * @throws IllegalArgumentException se algo for inválido
     */
    public static CollectionCatalog load(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("collections");
        if (section == null) {
            throw new IllegalArgumentException("collections.yml: seção 'collections' ausente");
        }
        List<CollectionDefinition> result = new ArrayList<>();
        Map<String, String> seen = new HashMap<>();
        for (String id : section.getKeys(false)) {
            ContentKeys.require(id, "collections.yml");
            ConfigurationSection collection = section.getConfigurationSection(id);
            String where = "collections.yml > " + id;
            if (collection == null || collection.getString("name") == null) {
                throw new IllegalArgumentException(where + ": esperado um bloco com 'name' e 'entries'");
            }
            ConfigurationSection entriesSection = collection.getConfigurationSection("entries");
            if (entriesSection == null || entriesSection.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException(where + ": 'entries' vazio");
            }
            List<CollectionEntry> entries = new ArrayList<>();
            for (String key : entriesSection.getKeys(false)) {
                String entryWhere = where + " > " + key;
                ContentKeys.require(key, where);
                if (seen.containsKey(key)) {
                    throw new IllegalArgumentException(entryWhere + ": chave repetida (já usada em '" + seen.get(key) + "')");
                }
                seen.put(key, id);
                ConfigurationSection entry = entriesSection.getConfigurationSection(key);
                if (entry == null || entry.getString("name") == null) {
                    throw new IllegalArgumentException(entryWhere + ": esperado { name, rarity }");
                }
                Rarity rarity;
                try {
                    rarity = Rarity.parse(entry.getString("rarity"));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(entryWhere + ": " + e.getMessage());
                }
                entries.add(new CollectionEntry(key, entry.getString("name"), rarity));
            }
            result.add(new CollectionDefinition(id, collection.getString("name"), entries));
        }
        return new CollectionCatalog(result);
    }

    public List<CollectionDefinition> collections() {
        return collections;
    }

    public Optional<CollectionDefinition> collection(String id) {
        return collections.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public Optional<CollectionEntry> entry(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    /** Coleção à qual a entrada pertence. */
    public Optional<CollectionDefinition> ownerOf(String entryKey) {
        return Optional.ofNullable(owner.get(entryKey));
    }

    /** Todas as entradas, por chave, na ordem das coleções. */
    public Map<String, CollectionEntry> allEntries() {
        Map<String, CollectionEntry> ordered = new LinkedHashMap<>();
        for (CollectionDefinition collection : collections) {
            for (CollectionEntry entry : collection.entries()) {
                ordered.put(entry.key(), entry);
            }
        }
        return ordered;
    }
}
