package com.greencodes.greensky.farming;

import java.util.List;
import java.util.random.RandomGenerator;

/** Pesos de uma, duas e três estrelas; zero desativa uma faixa. */
public record CropQuality(int normal, int good, int excellent) {
    public static final CropQuality DEFAULT = new CropQuality(80, 18, 2);
    public CropQuality {
        long total = (long) normal + good + excellent;
        if (normal < 0 || good < 0 || excellent < 0 || total <= 0 || total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("crops.yml: quality-weights exige três pesos >= 0 e soma entre 1 e 2147483647");
        }
    }
    public static CropQuality from(List<?> weights) {
        if (weights == null || weights.size() != 3 || weights.stream().anyMatch(v -> !(v instanceof Integer))) {
            throw new IllegalArgumentException("crops.yml: quality-weights deve ser [peso1, peso2, peso3], inteiros");
        }
        return new CropQuality((Integer) weights.get(0), (Integer) weights.get(1), (Integer) weights.get(2));
    }
    public int roll(RandomGenerator random) {
        int pick = random.nextInt(normal + good + excellent);
        return pick < normal ? 1 : pick < normal + good ? 2 : 3;
    }
    public static String stars(int quality) {
        if (quality < 1 || quality > 3) throw new IllegalArgumentException("qualidade deve ser 1..3");
        return "★".repeat(quality) + "☆".repeat(3 - quality);
    }
}
