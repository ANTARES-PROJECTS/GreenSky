package com.greencodes.greensky.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.content.ContentYaml;
import com.greencodes.greensky.content.Rarity;
import org.junit.jupiter.api.Test;

class FishLoaderTest {

    private static final CollectionCatalog CATALOG = CollectionCatalog.load(ContentYaml.parse("""
            collections:
              fishing:
                name: "Pesca"
                entries:
                  fish.cod: { name: "Bacalhau", rarity: COMMON }
                  fish.koi: { name: "Koi", rarity: MYTHIC }
              farming:
                name: "Agricultura"
                entries:
                  crop.wheat: { name: "Trigo", rarity: COMMON }
            """));

    private static FishTable load(String yaml) {
        return FishLoader.load(ContentYaml.parse(yaml), CATALOG, material -> true);
    }

    @Test
    void nameAndRarityComeFromTheCollection() {
        FishTable table = load("""
                fish:
                  fish.koi:
                    material: TROPICAL_FISH
                    weight: 3
                    size: [30, 70.5]
                    conditions: { time: night, moon: FULL }
                """);
        FishDefinition koi = table.fish().get(0);
        assertEquals("Koi", koi.name());
        assertEquals(Rarity.MYTHIC, koi.rarity());
        assertEquals(70.5, koi.maxSizeCm());
        assertEquals(FishConditions.Time.NIGHT, koi.conditions().time());
        assertEquals(FishConditions.Weather.ANY, koi.conditions().weather(), "condição omitida = ANY");
    }

    @Test
    void rejectsInvalidFish() {
        // Não existe na coleção Pesca (mesmo existindo em outra coleção).
        assertTrue(assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  crop.wheat: { material: WHEAT, weight: 1, size: [1, 2] }
                """)).getMessage().contains("crop.wheat"));
        assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  fish.cod: { material: NAO_EXISTE, weight: 1, size: [1, 2] }
                """));
        assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  fish.cod: { material: COD, weight: 0, size: [1, 2] }
                """));
        assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  fish.cod: { material: COD, weight: 1, size: [5, 2] }
                """));
        assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  fish.cod: { material: COD, weight: 1, size: [5] }
                """));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> load("""
                fish:
                  fish.cod: { material: COD, weight: 1, size: [1, 2], conditions: { weather: NEVOA } }
                """)).getMessage().contains("weather"));
        assertThrows(IllegalArgumentException.class, () -> load("fish: {}"));
    }

    @Test
    void requiresTheFishingCollection() {
        CollectionCatalog noFishing = CollectionCatalog.load(ContentYaml.parse("""
                collections:
                  farming:
                    name: "A"
                    entries:
                      crop.wheat: { name: "Trigo", rarity: COMMON }
                """));
        assertThrows(IllegalArgumentException.class, () -> FishLoader.load(ContentYaml.parse("""
                fish:
                  fish.cod: { material: COD, weight: 1, size: [1, 2] }
                """), noFishing, material -> true));
    }
}
