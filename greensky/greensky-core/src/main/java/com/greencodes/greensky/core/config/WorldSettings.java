package com.greencodes.greensky.core.config;

import java.util.Set;
import java.util.regex.Pattern;

/** Parâmetros do mundo SkyBlock. */
public record WorldSettings(String name, int spawnY) {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    /** Mundos que o próprio servidor cria; usá-los colidiria com o mundo padrão. */
    private static final Set<String> RESERVED = Set.of("world", "world_nether", "world_the_end");

    public WorldSettings {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "world.name deve ter 1-32 caracteres [a-z0-9_-] (atual: " + name + ")");
        }
        if (RESERVED.contains(name)) {
            throw new IllegalArgumentException("world.name '" + name + "' é reservado pelo servidor");
        }
        // Limites verticais do overworld vanilla: -64..319.
        if (spawnY < -63 || spawnY > 318) {
            throw new IllegalArgumentException("world.spawn-y deve estar entre -63 e 318 (atual: " + spawnY + ")");
        }
    }
}
