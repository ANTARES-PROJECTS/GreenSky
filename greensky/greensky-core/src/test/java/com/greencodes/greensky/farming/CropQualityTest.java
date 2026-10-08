package com.greencodes.greensky.farming;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CropQualityTest {
    @Test
    void weightsMatchDistributionAndProduceOnlyValidStars() {
        Random random = new Random(21);
        int[] count = new int[4];
        for (int i = 0; i < 100000; i++) count[CropQuality.DEFAULT.roll(random)]++;
        assertTrue(count[1] > 79000 && count[1] < 81000);
        assertTrue(count[2] > 17000 && count[2] < 19000);
        assertTrue(count[3] > 1700 && count[3] < 2300);
        assertEquals("★☆☆", CropQuality.stars(1));
        assertEquals("★★★", CropQuality.stars(3));
    }

    @Test
    void disabledTiersNeverRollAndSingleEnabledTierAlwaysWins() {
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            assertEquals(2, new CropQuality(0, 1, 0).roll(random));
            assertEquals(3, new CropQuality(0, 0, 10).roll(random));
        }
    }

    @Test
    void rejectsBadWeightsAndOverflow() {
        for (List<?> values : List.of(List.of(0, 0, 0), List.of(-1, 2, 3), List.of(1, 2),
                List.of(1, 2, 3.5), List.of(Integer.MAX_VALUE, 1, 1))) {
            assertThrows(IllegalArgumentException.class, () -> CropQuality.from(values));
        }
        assertThrows(IllegalArgumentException.class, () -> CropQuality.stars(0));
    }
}
