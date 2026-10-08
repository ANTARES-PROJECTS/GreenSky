package com.greencodes.greensky.content;

import java.util.regex.Pattern;

/** Regras dos identificadores de conteúdo (itens, entradas de coleção). */
public final class ContentKeys {

    /** Minúsculas, números e _ em partes separadas por ponto. Ex.: {@code fish.cod}, {@code relic.heart_of_forest}. */
    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)*");
    public static final int MAX_LENGTH = 64; // coluna entry_key do banco

    private ContentKeys() {}

    /**
     * @throws IllegalArgumentException se o identificador for inválido
     */
    public static String require(String key, String where) {
        if (key == null || key.length() > MAX_LENGTH || !VALID.matcher(key).matches()) {
            throw new IllegalArgumentException(where + ": id inválido '" + key + "' (use minúsculas, números, _ e"
                    + " pontos, até " + MAX_LENGTH + " caracteres)");
        }
        return key;
    }
}
