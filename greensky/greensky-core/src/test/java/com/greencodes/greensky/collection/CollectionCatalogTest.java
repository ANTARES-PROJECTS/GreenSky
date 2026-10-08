package com.greencodes.greensky.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.content.ContentYaml;
import com.greencodes.greensky.content.Rarity;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CollectionCatalogTest {

    private static YamlConfiguration yaml(String text) {
        return ContentYaml.parse(text);
    }

    @Test
    void shippedCollectionsFileIsValid() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("content/collections.yml")) {
            CollectionCatalog catalog = CollectionCatalog.load(
                    ContentYaml.load(new InputStreamReader(in, StandardCharsets.UTF_8), "jar"));
            assertEquals(Set.of("fishing", "farming", "exploration"),
                    Set.copyOf(catalog.collections().stream().map(CollectionDefinition::id).toList()));
            assertEquals("fishing", catalog.ownerOf("fish.cod").orElseThrow().id());
            assertTrue(catalog.entry("fish.abyssal_whisper").orElseThrow().secret());
        }
    }

    @Test
    void parsesEntriesInOrderWithRarity() throws Exception {
        CollectionCatalog catalog = CollectionCatalog.load(yaml("""
                collections:
                  fishing:
                    name: "Pesca"
                    entries:
                      fish.cod: { name: "Bacalhau", rarity: common }
                      fish.koi: { name: "Koi", rarity: MYTHIC }
                """));
        CollectionDefinition fishing = catalog.collection("fishing").orElseThrow();
        assertEquals("Pesca", fishing.name());
        assertEquals("fish.cod", fishing.entries().get(0).key());
        assertEquals(Rarity.MYTHIC, fishing.entries().get(1).rarity());
        assertEquals(1, fishing.discoveredCount(Set.of("fish.koi", "outra.coisa")));
        assertFalse(catalog.entry("fish.salmon").isPresent());
    }

    @Test
    void duplicatedKeyAcrossCollectionsIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CollectionCatalog.load(yaml("""
                collections:
                  a:
                    name: "A"
                    entries:
                      shared.key: { name: "X", rarity: COMMON }
                  b:
                    name: "B"
                    entries:
                      shared.key: { name: "Y", rarity: COMMON }
                """)));
        assertTrue(e.getMessage().contains("shared.key"), e.getMessage());
    }

    @Test
    void invalidContentIsRejectedWithLocation() {
        IllegalArgumentException rarity = assertThrows(IllegalArgumentException.class, () -> CollectionCatalog.load(yaml("""
                collections:
                  fishing:
                    name: "Pesca"
                    entries:
                      fish.cod: { name: "Bacalhau", rarity: SUPER_RARO }
                """)));
        assertTrue(rarity.getMessage().contains("fish.cod"), rarity.getMessage());

        assertThrows(IllegalArgumentException.class, () -> CollectionCatalog.load(yaml("""
                collections:
                  fishing:
                    name: "Pesca"
                    entries:
                      Fish.COD: { name: "Bacalhau", rarity: COMMON }
                """)));
        assertThrows(IllegalArgumentException.class, () -> CollectionCatalog.load(yaml("""
                collections:
                  fishing:
                    name: "Pesca"
                    entries: {}
                """)));
        assertThrows(IllegalArgumentException.class, () -> CollectionCatalog.load(yaml("outra: 1")));
    }
}
