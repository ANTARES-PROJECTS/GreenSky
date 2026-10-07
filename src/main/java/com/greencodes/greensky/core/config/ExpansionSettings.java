package com.greencodes.greensky.core.config;

import java.util.List;
import java.util.OptionalInt;

/**
 * Tamanhos (lado, em blocos) por nível de expansão. O primeiro é o tamanho inicial e o
 * último não passa do tamanho máximo; a lista é estritamente crescente.
 */
public record ExpansionSettings(List<Integer> levels) {

    public ExpansionSettings {
        levels = List.copyOf(levels);
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("islands.expansion-levels não pode ser vazio");
        }
        for (int i = 1; i < levels.size(); i++) {
            if (levels.get(i) <= levels.get(i - 1)) {
                throw new IllegalArgumentException(
                        "islands.expansion-levels deve ser estritamente crescente (atual: " + levels + ")");
            }
        }
    }

    /** Confere os níveis contra o tamanho inicial e o máximo das ilhas. */
    public ExpansionSettings validateAgainst(IslandSettings islands) {
        if (levels.get(0) != islands.initialSize()) {
            throw new IllegalArgumentException("islands.expansion-levels deve começar em islands.initial-size ("
                    + islands.initialSize() + "), atual: " + levels.get(0));
        }
        if (levels.get(levels.size() - 1) > islands.maxSize()) {
            throw new IllegalArgumentException("islands.expansion-levels passa de islands.max-size ("
                    + islands.maxSize() + "): " + levels);
        }
        return this;
    }

    /** Próximo tamanho depois de {@code currentSize}, ou vazio se já está no último nível. */
    public OptionalInt nextAfter(int currentSize) {
        for (int size : levels) {
            if (size > currentSize) {
                return OptionalInt.of(size);
            }
        }
        return OptionalInt.empty();
    }

    /** Nível (1 = inicial) do tamanho atual; tamanhos fora da lista contam como o maior nível abaixo deles. */
    public int levelOf(int currentSize) {
        int level = 0;
        for (int size : levels) {
            if (size <= currentSize) {
                level++;
            }
        }
        return Math.max(1, level);
    }
}
