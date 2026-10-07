package com.greencodes.greensky.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Garante que o gerador não delega nada ao vanilla. Se um desses flags virar true,
 * o servidor passaria a gerar terreno/estruturas/mobs no mundo SkyBlock.
 */
class VoidGeneratorTest {

    private final VoidGenerator generator = new VoidGenerator(100);

    @Test
    void doesNotDelegateAnyStageToVanilla() {
        assertFalse(generator.shouldGenerateNoise());
        assertFalse(generator.shouldGenerateSurface());
        assertFalse(generator.shouldGenerateCaves());
        assertFalse(generator.shouldGenerateDecorations());
        assertFalse(generator.shouldGenerateMobs());
        assertFalse(generator.shouldGenerateStructures());
    }

    @Test
    void neverAllowsNaturalPlayerSpawn() {
        assertFalse(generator.canSpawn(null, 0, 0));
    }
}
