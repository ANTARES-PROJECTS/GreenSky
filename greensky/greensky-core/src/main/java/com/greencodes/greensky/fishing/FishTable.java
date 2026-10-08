package com.greencodes.greensky.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Sorteio de peixes. Sem Bukkit e sem banco: só memória (roda a cada fisgada sem custo).
 * Filtra os peixes possíveis nas condições atuais e sorteia por peso.
 */
public final class FishTable {

    private final List<FishDefinition> fish;

    public FishTable(List<FishDefinition> fish) {
        this.fish = List.copyOf(fish);
    }

    public List<FishDefinition> fish() {
        return fish;
    }

    /** Peixes que podem aparecer nessas condições. */
    public List<FishDefinition> eligible(FishingEnvironment env) {
        List<FishDefinition> result = new ArrayList<>();
        for (FishDefinition definition : fish) {
            if (definition.conditions().matches(env)) {
                result.add(definition);
            }
        }
        return result;
    }

    /** Sorteia um peixe possível (vazio se nenhum peixe combina com as condições). */
    public Optional<FishCatch> roll(FishingEnvironment env, RandomGenerator random) {
        List<FishDefinition> pool = eligible(env);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        int total = 0;
        for (FishDefinition definition : pool) {
            total += definition.weight();
        }
        int pick = random.nextInt(total);
        FishDefinition chosen = pool.get(pool.size() - 1);
        for (FishDefinition definition : pool) {
            pick -= definition.weight();
            if (pick < 0) {
                chosen = definition;
                break;
            }
        }
        // Tamanho puxado para baixo (peixes grandes são mais raros): posição^1.5 na faixa.
        double position = Math.pow(random.nextDouble(), 1.5);
        double size = chosen.minSizeCm() + (chosen.maxSizeCm() - chosen.minSizeCm()) * position;
        double rounded = Math.round(size * 10.0) / 10.0;
        return Optional.of(new FishCatch(chosen, rounded, starsFor(position)));
    }

    /** 3 estrelas no topo da faixa (>= 90%), 2 a partir de 60%, senão 1. */
    static int starsFor(double position) {
        if (position >= 0.9) {
            return 3;
        }
        return position >= 0.6 ? 2 : 1;
    }
}
