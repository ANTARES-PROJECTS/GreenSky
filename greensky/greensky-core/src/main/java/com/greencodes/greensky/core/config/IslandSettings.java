package com.greencodes.greensky.core.config;

/**
 * Parâmetros de tamanho, espaçamento e ilha inicial.
 *
 * <p>Invariante: {@code spacing > maxSize + spacingMargin}, para que duas ilhas
 * vizinhas nunca se sobreponham, mesmo no tamanho máximo.
 *
 * @param baseY altura da superfície (grama) da ilha inicial
 * @param starterRadius raio do disco da ilha inicial; o diâmetro cabe no tamanho inicial
 */
public record IslandSettings(
        int initialSize, int maxSize, int spacing, int spacingMargin, int baseY, int starterRadius) {

    public IslandSettings {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("islands.initial-size deve ser > 0 (atual: " + initialSize + ")");
        }
        if (maxSize < initialSize) {
            throw new IllegalArgumentException(
                    "islands.max-size (" + maxSize + ") deve ser >= islands.initial-size (" + initialSize + ")");
        }
        if (spacingMargin < 0) {
            throw new IllegalArgumentException("islands.spacing-margin deve ser >= 0 (atual: " + spacingMargin + ")");
        }
        long required = (long) maxSize + spacingMargin;
        if ((long) spacing <= required) {
            throw new IllegalArgumentException(
                    "islands.spacing (" + spacing + ") deve ser > islands.max-size (" + maxSize
                            + ") + islands.spacing-margin (" + spacingMargin + ") = " + required);
        }
        // Limites verticais do overworld: -64..319. Sobra espaço para 3 camadas abaixo e árvore acima.
        if (baseY < -50 || baseY > 290) {
            throw new IllegalArgumentException("islands.base-y deve estar entre -50 e 290 (atual: " + baseY + ")");
        }
        if (starterRadius < 1 || starterRadius > 16) {
            throw new IllegalArgumentException(
                    "islands.starter-radius deve estar entre 1 e 16 (atual: " + starterRadius + ")");
        }
        if (starterRadius * 2 + 1 > initialSize) {
            throw new IllegalArgumentException("islands.starter-radius (" + starterRadius
                    + ") não cabe em islands.initial-size (" + initialSize + ")");
        }
    }
}
