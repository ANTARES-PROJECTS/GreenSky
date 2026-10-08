package com.greencodes.greensky.core.config;

/**
 * Regras da pesca do GreenSky.
 *
 * @param requireOpenWater peixe do GreenSky só em água aberta (regra do próprio Minecraft); barra o "balde de 1 bloco"
 * @param afkMaxCatchesSameSpot fisgadas seguidas na mesma posição e mira que ainda contam; acima disso é pesca parada
 */
public record FishingSettings(boolean requireOpenWater, int afkMaxCatchesSameSpot) {

    public FishingSettings {
        if (afkMaxCatchesSameSpot < 2 || afkMaxCatchesSameSpot > 1000) {
            throw new IllegalArgumentException(
                    "fishing.afk-max-catches-same-spot deve estar entre 2 e 1000 (atual: " + afkMaxCatchesSameSpot + ")");
        }
    }
}
