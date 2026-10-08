package com.greencodes.greensky.content;

import java.util.Locale;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Raridades do README (seção 103), da mais comum à mais rara. "Não colocar tudo como
 * Legendary": cada nível tem cor própria e a ordem importa (ex.: ordenar coleções).
 */
public enum Rarity {
    COMMON("Comum", NamedTextColor.WHITE),
    UNCOMMON("Incomum", NamedTextColor.GREEN),
    RARE("Raro", NamedTextColor.BLUE),
    EPIC("Épico", NamedTextColor.DARK_PURPLE),
    LEGENDARY("Lendário", NamedTextColor.GOLD),
    MYTHIC("Mítico", NamedTextColor.LIGHT_PURPLE),
    /** Escondida: na coleção aparece como "???" até ser descoberta. */
    SECRET("Secreto", NamedTextColor.DARK_RED);

    private final String label;
    private final TextColor color;

    Rarity(String label, TextColor color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public TextColor color() {
        return color;
    }

    /**
     * @throws IllegalArgumentException com a lista de valores aceitos
     */
    public static Rarity parse(String value) {
        if (value != null) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // cai na mensagem abaixo
            }
        }
        throw new IllegalArgumentException("raridade inválida '" + value + "' (use COMMON, UNCOMMON, RARE, EPIC,"
                + " LEGENDARY, MYTHIC ou SECRET)");
    }
}
