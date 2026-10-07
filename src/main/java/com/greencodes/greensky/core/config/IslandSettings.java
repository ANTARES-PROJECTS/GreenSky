package com.greencodes.greensky.core.config;

/**
 * Parâmetros de tamanho e espaçamento das ilhas.
 *
 * <p>Invariante: {@code spacing > maxSize + spacingMargin}, para que duas ilhas
 * vizinhas nunca se sobreponham, mesmo no tamanho máximo.
 */
public record IslandSettings(int initialSize, int maxSize, int spacing, int spacingMargin) {

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
    }
}
