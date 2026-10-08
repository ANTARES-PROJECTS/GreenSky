package com.greencodes.greensky.fishing;

import com.greencodes.greensky.content.Rarity;
import org.bukkit.Material;

/**
 * Um peixe do GreenSky. O {@code id} é também a chave da entrada na coleção Pesca; nome e
 * raridade vêm de {@code collections.yml} (uma única fonte).
 *
 * @param weight peso relativo no sorteio entre os peixes possíveis naquele momento
 */
public record FishDefinition(
        String id,
        String name,
        Rarity rarity,
        Material material,
        int weight,
        double minSizeCm,
        double maxSizeCm,
        FishConditions conditions) {

    public FishDefinition {
        if (weight <= 0) {
            throw new IllegalArgumentException(id + ": weight deve ser > 0");
        }
        if (minSizeCm <= 0 || maxSizeCm < minSizeCm) {
            throw new IllegalArgumentException(id + ": tamanho inválido (min > 0 e max >= min)");
        }
    }
}
