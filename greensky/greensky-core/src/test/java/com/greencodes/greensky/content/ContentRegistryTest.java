package com.greencodes.greensky.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ContentRegistryTest {

    @Test
    void activityItemsAreAddedWithoutChangingOriginalRegistryAndCollisionsAreRejected() {
        var registry = load(yaml("items: {}"));
        var seed = new ContentItem("crop.ancient_seed", Material.WHEAT_SEEDS, "Semente", Rarity.LEGENDARY,
                java.util.List.of());
        var merged = registry.withItems(java.util.List.of(seed));
        assertTrue(registry.item(seed.id()).isEmpty());
        assertEquals(seed, merged.item(seed.id()).orElseThrow());
        var error = assertThrows(IllegalArgumentException.class,
                () -> merged.withItems(java.util.List.of(seed)));
        assertTrue(error.getMessage().contains(seed.id()));
        assertThrows(IllegalArgumentException.class, () -> registry.withItems(java.util.List.of(seed, seed)));
    }

    /** Sem servidor nos testes: qualquer material conhecido conta como item (o boot real usa Material::isItem). */
    private static ContentRegistry load(YamlConfiguration yaml) {
        return ContentRegistry.load(yaml, material -> true);
    }

    private static YamlConfiguration yaml(String text) {
        return ContentYaml.parse(text);
    }

    @Test
    void shippedItemsFileIsValid() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("content/items.yml")) {
            ContentRegistry registry = load(
                    ContentYaml.load(new InputStreamReader(in, StandardCharsets.UTF_8), "jar"));
            ContentItem relic = registry.item("relic.heart_of_ancient_forest").orElseThrow();
            assertEquals(Material.HEART_OF_THE_SEA, relic.material());
            assertEquals(Rarity.MYTHIC, relic.rarity());
            assertEquals(2, relic.lore().size());
        }
    }

    @Test
    void invalidItemsAreRejectedWithLocation() {
        IllegalArgumentException material = assertThrows(IllegalArgumentException.class, () -> load(yaml("""
                items:
                  relic.x:
                    material: NAO_EXISTE
                    name: "X"
                    rarity: RARE
                """)));
        assertTrue(material.getMessage().contains("relic.x") && material.getMessage().contains("material"),
                material.getMessage());
        assertThrows(IllegalArgumentException.class, () -> load(yaml("""
                items:
                  relic.x:
                    material: COD
                    rarity: RARE
                """)));
        assertThrows(IllegalArgumentException.class, () -> load(yaml("""
                items:
                  relic.x:
                    material: COD
                    name: "X"
                    rarity: LENDARIO
                """)));
        assertThrows(IllegalArgumentException.class, () -> load(yaml("""
                items:
                  "Relic X":
                    material: COD
                    name: "X"
                    rarity: RARE
                """)));
    }

    @Test
    void yamlSyntaxErrorIsAnErrorNotAnEmptyFile() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ContentYaml.parse("items:\n  relic.x: { material: COD, name: \"sem fechar\n"));
        assertTrue(e.getMessage().contains("YAML inválido"), e.getMessage());
    }

    @Test
    void dottedKeysStayWhole() {
        // No YAML padrão do Bukkit, "relic.x" viraria relic -> x.
        ContentRegistry registry = load(yaml("""
                items:
                  relic.deep.x:
                    material: COD
                    name: "X"
                    rarity: RARE
                """));
        assertTrue(registry.item("relic.deep.x").isPresent());
    }

    @Test
    void rarityParsingAndOrder() {
        assertEquals(Rarity.LEGENDARY, Rarity.parse(" legendary "));
        assertThrows(IllegalArgumentException.class, () -> Rarity.parse("ultra"));
        assertThrows(IllegalArgumentException.class, () -> Rarity.parse(null));
        assertTrue(Rarity.COMMON.compareTo(Rarity.MYTHIC) < 0);
        assertTrue(Rarity.MYTHIC.compareTo(Rarity.SECRET) < 0);
    }
}
