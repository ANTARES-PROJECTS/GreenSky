package com.greencodes.greensky.fishing;

/**
 * Resultado de um sorteio: o peixe, o tamanho e a qualidade (1 a 3 estrelas, pelo tamanho
 * relativo à faixa do peixe: os maiores são os melhores).
 */
public record FishCatch(FishDefinition fish, double sizeCm, int stars) {

    public String starsText() {
        return "★".repeat(stars) + "☆".repeat(3 - stars);
    }
}
