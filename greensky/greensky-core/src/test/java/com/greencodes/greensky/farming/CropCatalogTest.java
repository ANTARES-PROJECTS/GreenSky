package com.greencodes.greensky.farming;

import static org.junit.jupiter.api.Assertions.*;
import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.content.ContentYaml;
import java.util.Random;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CropCatalogTest {
    private CollectionCatalog collections() throws Exception {
        return CollectionCatalog.load(ContentYaml.parse(new String(getClass().getResourceAsStream(
                "/content/collections.yml").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void realCatalogSupportsThreeCropsAndRareFindsMatchWeights() throws Exception {
        var catalog = CropCatalog.load(ContentYaml.parse(new String(getClass().getResourceAsStream(
                "/content/crops.yml").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)), collections(), m -> true);
        assertEquals("crop.carrot", catalog.crop(Material.CARROTS).orElseThrow().entry());
        assertEquals("crop.potato", catalog.crop(Material.POTATOES).orElseThrow().entry());
        assertTrue(catalog.crop(Material.STONE).isEmpty());
        Random random = new Random(42);
        int golden = 0, ancient = 0;
        var wheat = catalog.crop(Material.WHEAT).orElseThrow();
        for (int i = 0; i < 100000; i++) {
            var bonus = wheat.roll(random);
            if (bonus.isPresent()) {
                if (bonus.get().id().equals("crop.golden_wheat")) golden++;
                if (bonus.get().id().equals("crop.ancient_seed")) ancient++;
            }
        }
        assertTrue(golden > 1800 && golden < 2200);
        assertTrue(ancient > 140 && ancient < 260);
        assertTrue(catalog.crop(Material.CARROTS).orElseThrow().roll(random).isEmpty());
    }

    @Test
    void rejectsUnknownCropEntryAndInvalidBonus() throws Exception {
        var definitions = collections();
        for (String yaml : new String[] {
                "crops: { STONE: { entry: crop.wheat } }",
                "crops: { WHEAT: { entry: nope } }",
                "crops: { WHEAT: { entry: crop.wheat, bonuses: { crop.golden_wheat: { material: WHEAT, chance: -1 } } } }",
                "crops: { WHEAT: { entry: crop.wheat, bonuses: { crop.golden_wheat: { material: WHEAT, chance: 0.8 }, crop.ancient_seed: { material: WHEAT_SEEDS, chance: 0.8 } } } }",
                "crops: { WHEAT: { entry: crop.wheat, bonuses: { fish.cod: { material: COD, chance: 0.1 } } } }"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> CropCatalog.load(ContentYaml.parse(yaml), definitions, m -> true));
        }
    }
}
