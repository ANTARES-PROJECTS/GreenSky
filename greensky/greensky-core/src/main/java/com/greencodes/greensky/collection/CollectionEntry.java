package com.greencodes.greensky.collection;

import com.greencodes.greensky.content.Rarity;

/**
 * Uma coisa a descobrir dentro de uma coleção (um peixe, uma colheita, uma região).
 *
 * @param key identificador global e único (ex.: {@code fish.cod}); é o que vai para o banco
 */
public record CollectionEntry(String key, String name, Rarity rarity) {

    /** Entradas secretas aparecem como "???" até serem descobertas. */
    public boolean secret() {
        return rarity == Rarity.SECRET;
    }
}
