package com.greencodes.greensky.farming;

import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.content.ContentItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Conteúdo agrícola validado no boot; sorteio em memória, um achado raro no máximo por planta. */
public final class CropCatalog {
    public record Bonus(ContentItem item, double chance) {}
    public record Crop(ContentItem product, List<Bonus> bonuses) {
        public Crop { bonuses = List.copyOf(bonuses); }
        public String entry() { return product.id(); }
        public Optional<ContentItem> roll(RandomGenerator random) {
            double pick = random.nextDouble();
            for (Bonus bonus : bonuses) {
                pick -= bonus.chance();
                if (pick < 0) return Optional.of(bonus.item());
            }
            return Optional.empty();
        }
    }

    private final Map<Material, Crop> crops;
    private final CropQuality quality;
    private CropCatalog(Map<Material, Crop> crops, CropQuality quality) {
        this.crops = Map.copyOf(crops);
        this.quality = quality;
    }
    public CropQuality quality() { return quality; }
    public Optional<Crop> crop(Material block) { return Optional.ofNullable(crops.get(block)); }
    public List<ContentItem> bonusItems() {
        return crops.values().stream().flatMap(crop -> crop.bonuses().stream())
                .map(Bonus::item).distinct().toList();
    }

    public static CropCatalog load(ConfigurationSection root, CollectionCatalog collections,
            Predicate<Material> isItem) {
        ConfigurationSection section = root.getConfigurationSection("crops");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("crops.yml: seção crops ausente ou vazia");
        }
        var farming = collections.collection("farming").orElseThrow(
                () -> new IllegalArgumentException("crops.yml: coleção farming obrigatória"));
        CropQuality quality = root.contains("quality-weights")
                ? CropQuality.from(root.getList("quality-weights")) : CropQuality.DEFAULT;
        Map<Material, Crop> result = new EnumMap<>(Material.class);
        Set<Material> supported = Set.of(Material.WHEAT, Material.CARROTS, Material.POTATOES);
        for (String name : section.getKeys(false)) {
            String where = "crops.yml > " + name;
            Material block = Material.matchMaterial(name);
            ConfigurationSection config = section.getConfigurationSection(name);
            if (!supported.contains(block == null ? Material.AIR : block) || config == null) {
                throw new IllegalArgumentException(where + ": cultura inválida (WHEAT, CARROTS ou POTATOES)");
            }
            String entry = config.getString("entry");
            var base = farming.entries().stream().filter(e -> e.key().equals(entry)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(where + ": entry não existe na coleção farming"));
            Material product = switch (block) {
                case WHEAT -> Material.WHEAT;
                case CARROTS -> Material.CARROT;
                case POTATOES -> Material.POTATO;
                default -> throw new IllegalArgumentException(where + ": cultura inválida");
            };
            List<Bonus> bonuses = new ArrayList<>();
            ConfigurationSection bonusSection = config.getConfigurationSection("bonuses");
            if (config.contains("bonuses") && bonusSection == null) {
                throw new IllegalArgumentException(where + ": bonuses deve ser uma seção");
            }
            double total = 0;
            if (bonusSection != null) for (String id : bonusSection.getKeys(false)) {
                var definition = farming.entries().stream().filter(e -> e.key().equals(id)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(where + ": bônus " + id + " fora de farming"));
                ConfigurationSection bonus = bonusSection.getConfigurationSection(id);
                if (bonus == null || !(bonus.get("chance") instanceof Number)) {
                    throw new IllegalArgumentException(where + " > " + id + ": chance numérica obrigatória");
                }
                double chance = bonus.getDouble("chance");
                Material material = Material.matchMaterial(bonus.getString("material", ""));
                if (!Double.isFinite(chance) || chance <= 0 || chance > 1 || material == null || !isItem.test(material)) {
                    throw new IllegalArgumentException(where + " > " + id + ": chance/material inválidos");
                }
                total += chance;
                bonuses.add(new Bonus(new ContentItem(id, material, definition.name(), definition.rarity(),
                        switch (id) {
                            case "crop.golden_wheat" -> List.of("Fertilizante: use em uma cultura imatura.",
                                    "Avança 2 estágios e garante colheita ★★★.", "Uma aplicação por planta.");
                            case "crop.ancient_seed" -> List.of("Plante em solo arado na sua ilha.",
                                    "Madura: 1 Trigo Dourado ★★★.", "Imatura: devolve esta semente.");
                            default -> List.of("Achado raro de uma colheita.");
                        }), chance));
            }
            if (total > 1) throw new IllegalArgumentException(where + ": soma das chances deve ser <= 1");
            if (result.put(block, new Crop(new ContentItem(entry, product, base.name(), base.rarity(), List.of()), bonuses)) != null) {
                throw new IllegalArgumentException(where + ": cultura duplicada");
            }
        }
        Crop wheat = result.get(Material.WHEAT);
        if (wheat == null || !wheat.entry().equals("crop.wheat")
                || wheat.bonuses().stream().noneMatch(b -> b.item().id().equals("crop.golden_wheat") && b.item().material() == Material.WHEAT)
                || wheat.bonuses().stream().noneMatch(b -> b.item().id().equals("crop.ancient_seed") && b.item().material() == Material.WHEAT_SEEDS)) {
            throw new IllegalArgumentException("crops.yml: plantio ancestral exige WHEAT/crop.wheat e bônus crop.golden_wheat/WHEAT e crop.ancient_seed/WHEAT_SEEDS");
        }
        return new CropCatalog(result, quality);
    }
}
