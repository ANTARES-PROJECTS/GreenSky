package com.greencodes.greensky.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.collection.CollectionCatalog;
import com.greencodes.greensky.content.ContentYaml;
import io.papermc.paper.world.MoonPhase;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Usa os arquivos reais do jar (fish.yml + collections.yml). */
class FishTableTest {

    private static FishTable table;

    private static final FishingEnvironment CLEAR_DAY = new FishingEnvironment(true, false, false, MoonPhase.FIRST_QUARTER);
    private static final FishingEnvironment RAIN_DAY = new FishingEnvironment(true, true, false, MoonPhase.FIRST_QUARTER);
    private static final FishingEnvironment STORM_DAY = new FishingEnvironment(true, true, true, MoonPhase.FIRST_QUARTER);
    private static final FishingEnvironment FULL_MOON_NIGHT = new FishingEnvironment(false, false, false, MoonPhase.FULL_MOON);
    private static final FishingEnvironment STORM_NEW_MOON_NIGHT = new FishingEnvironment(false, true, true, MoonPhase.NEW_MOON);

    @BeforeAll
    static void load() throws Exception {
        CollectionCatalog catalog;
        try (var in = FishTableTest.class.getClassLoader().getResourceAsStream("content/collections.yml")) {
            catalog = CollectionCatalog.load(ContentYaml.load(new InputStreamReader(in, StandardCharsets.UTF_8), "c"));
        }
        try (var in = FishTableTest.class.getClassLoader().getResourceAsStream("content/fish.yml")) {
            table = FishLoader.load(ContentYaml.load(new InputStreamReader(in, StandardCharsets.UTF_8), "f"),
                    catalog, material -> true);
        }
    }

    private static Set<String> eligible(FishingEnvironment env) {
        return table.eligible(env).stream().map(FishDefinition::id).collect(Collectors.toSet());
    }

    @Test
    void clearDayOnlyHasCommonFish() {
        assertEquals(Set.of("fish.cod", "fish.salmon", "fish.pufferfish"), eligible(CLEAR_DAY));
    }

    @Test
    void weatherAndMoonUnlockSpecialFish() {
        assertTrue(eligible(RAIN_DAY).contains("fish.tropical"), "chuva -> raro");
        assertFalse(eligible(RAIN_DAY).contains("fish.storm_eel"), "chuva sem raio não é tempestade");
        assertTrue(eligible(STORM_DAY).containsAll(Set.of("fish.tropical", "fish.storm_eel")), "tempestade conta como chuva");
        assertTrue(eligible(FULL_MOON_NIGHT).contains("fish.moon_koi"));
        assertFalse(eligible(CLEAR_DAY).contains("fish.moon_koi"), "koi só de noite com lua cheia");
        assertFalse(eligible(new FishingEnvironment(true, false, false, MoonPhase.FULL_MOON)).contains("fish.moon_koi"),
                "lua cheia de dia não vale");
    }

    @Test
    void secretFishNeedsAllConditions() {
        assertTrue(eligible(STORM_NEW_MOON_NIGHT).contains("fish.abyssal_whisper"));
        assertFalse(eligible(new FishingEnvironment(false, true, true, MoonPhase.FULL_MOON)).contains("fish.abyssal_whisper"));
        assertFalse(eligible(new FishingEnvironment(false, false, false, MoonPhase.NEW_MOON)).contains("fish.abyssal_whisper"));
    }

    @Test
    void rollRespectsWeightsSizesAndStars() {
        Random random = new Random(42);
        Map<String, Integer> counts = new HashMap<>();
        int[] stars = new int[4];
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            FishCatch c = table.roll(CLEAR_DAY, random).orElseThrow();
            counts.merge(c.fish().id(), 1, Integer::sum);
            assertTrue(c.sizeCm() >= c.fish().minSizeCm() && c.sizeCm() <= c.fish().maxSizeCm(), "tamanho fora da faixa");
            stars[c.stars()]++;
        }
        // Pesos 50/35/12 (total 97): proporções esperadas com folga.
        assertEquals(50.0 / 97, counts.get("fish.cod") / (double) n, 0.01);
        assertEquals(35.0 / 97, counts.get("fish.salmon") / (double) n, 0.01);
        assertEquals(12.0 / 97, counts.get("fish.pufferfish") / (double) n, 0.01);
        // Tamanho puxado para baixo: 3 estrelas são raras (posição^1.5 >= 0.9 => ~6.8%).
        assertTrue(stars[3] < stars[2] && stars[2] < stars[1], "3★ < 2★ < 1★");
        assertEquals(0.068, stars[3] / (double) n, 0.01);
    }

    @Test
    void emptyTableOrNoMatchGivesNothing() {
        FishTable onlyStorm = new FishTable(List.of(new FishDefinition("fish.x", "X",
                com.greencodes.greensky.content.Rarity.RARE, org.bukkit.Material.COD, 1, 1, 2,
                new FishConditions(FishConditions.Time.ANY, FishConditions.Weather.THUNDER, FishConditions.Moon.ANY))));
        assertTrue(onlyStorm.roll(CLEAR_DAY, new Random(1)).isEmpty());
        assertTrue(onlyStorm.roll(STORM_DAY, new Random(1)).isPresent());
    }

    @Test
    void starsThresholds() {
        assertEquals(1, FishTable.starsFor(0.0));
        assertEquals(1, FishTable.starsFor(0.59));
        assertEquals(2, FishTable.starsFor(0.6));
        assertEquals(3, FishTable.starsFor(0.9));
        assertEquals(3, FishTable.starsFor(1.0));
    }
}
