package com.greencodes.greensky.fishing;

import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.collection.CollectionDefinition;
import com.greencodes.greensky.collection.CollectionEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Lê {@code content/fish.yml}. Cada peixe precisa existir como entrada da coleção
 * {@value #COLLECTION} (nome e raridade vêm de lá). Qualquer erro impede o boot.
 */
public final class FishLoader {

    public static final String COLLECTION = "fishing";

    private FishLoader() {}

    /**
     * @param isItem se o material pode ser item (o real precisa do servidor; testes passam outro)
     * @throws IllegalArgumentException com o arquivo e a chave do erro
     */
    public static FishTable load(ConfigurationSection root, CollectionCatalog catalog, Predicate<Material> isItem) {
        ConfigurationSection section = root.getConfigurationSection("fish");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("fish.yml: seção 'fish' ausente ou vazia");
        }
        CollectionDefinition fishing = catalog.collection(COLLECTION).orElseThrow(() ->
                new IllegalArgumentException("collections.yml: a coleção '" + COLLECTION + "' é obrigatória para a pesca"));
        List<FishDefinition> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            String where = "fish.yml > " + id;
            CollectionEntry entry = fishing.entries().stream().filter(e -> e.key().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(where + ": não existe na coleção '" + COLLECTION
                            + "' de collections.yml"));
            ConfigurationSection fish = section.getConfigurationSection(id);
            if (fish == null) {
                throw new IllegalArgumentException(where + ": esperado um bloco com material, weight e size");
            }
            String materialName = fish.getString("material");
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material == null || !isItem.test(material)) {
                throw new IllegalArgumentException(where + ": material inválido '" + materialName + "'");
            }
            if (!fish.isInt("weight")) {
                throw new IllegalArgumentException(where + ": 'weight' deve ser um número inteiro");
            }
            List<?> size = fish.getList("size");
            if (size == null || size.size() != 2 || !(size.get(0) instanceof Number) || !(size.get(1) instanceof Number)) {
                throw new IllegalArgumentException(where + ": 'size' deve ser [mínimo, máximo] em cm");
            }
            ConfigurationSection cond = fish.getConfigurationSection("conditions");
            FishConditions conditions = cond == null ? FishConditions.ANY : new FishConditions(
                    FishConditions.parse(FishConditions.Time.class, cond.getString("time"), where + " > time"),
                    FishConditions.parse(FishConditions.Weather.class, cond.getString("weather"), where + " > weather"),
                    FishConditions.parse(FishConditions.Moon.class, cond.getString("moon"), where + " > moon"));
            try {
                result.add(new FishDefinition(id, entry.name(), entry.rarity(), material, fish.getInt("weight"),
                        ((Number) size.get(0)).doubleValue(), ((Number) size.get(1)).doubleValue(), conditions));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("fish.yml > " + e.getMessage());
            }
        }
        return new FishTable(result);
    }
}
