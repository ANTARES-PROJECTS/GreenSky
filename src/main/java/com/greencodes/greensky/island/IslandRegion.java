package com.greencodes.greensky.island;

/**
 * Área física quadrada de uma ilha no mundo: centro e tamanho. Os limites são
 * {@code [minX, maxX)} x {@code [minZ, maxZ)} (máximo exclusivo).
 */
public record IslandRegion(int centerX, int centerZ, int size) {

    public IslandRegion {
        if (size <= 0) {
            throw new IllegalArgumentException("size deve ser > 0 (atual: " + size + ")");
        }
    }

    /**
     * Região da ilha de um slot. Os slots seguem uma espiral quadrada em volta da origem, com
     * uma ilha a cada {@code spacing} blocos. O slot 0 é a origem (spawn) e não é usado por ilhas.
     */
    public static IslandRegion forSlot(long slot, int spacing, int size) {
        if (slot < 1) {
            throw new IllegalArgumentException("slot de ilha deve ser >= 1 (atual: " + slot + ")");
        }
        long[] grid = gridPosition(slot);
        long x = grid[0] * spacing;
        long z = grid[1] * spacing;
        if (Math.abs(x) > Integer.MAX_VALUE / 2 || Math.abs(z) > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("slot " + slot + " fica fora do alcance do mundo");
        }
        return new IslandRegion((int) x, (int) z, size);
    }

    /**
     * Posição na grade {x, z} do n-ésimo ponto da espiral quadrada: 0 é (0,0), depois o anel 1
     * (8 células), o anel 2 (16 células) e assim por diante. Cada célula aparece uma única vez.
     */
    static long[] gridPosition(long n) {
        if (n == 0) {
            return new long[] {0, 0};
        }
        // Anel k contém as células n em [(2k-1)^2, (2k+1)^2).
        long k = (long) ((Math.sqrt((double) n) - 1) / 2) + 1;
        while ((2 * k - 1) * (2 * k - 1) > n) {
            k--;
        }
        while ((2 * k + 1) * (2 * k + 1) <= n) {
            k++;
        }
        long i = n - (2 * k - 1) * (2 * k - 1); // 0 .. 8k-1
        long side = 2 * k;
        if (i < side) {
            return new long[] {k, -k + 1 + i};
        }
        if (i < 2 * side) {
            return new long[] {k - 1 - (i - side), k};
        }
        if (i < 3 * side) {
            return new long[] {-k, k - 1 - (i - 2 * side)};
        }
        return new long[] {-k + 1 + (i - 3 * side), -k};
    }

    public int minX() {
        return centerX - size / 2;
    }

    public int maxX() {
        return minX() + size;
    }

    public int minZ() {
        return centerZ - size / 2;
    }

    public int maxZ() {
        return minZ() + size;
    }

    public boolean contains(int x, int z) {
        return x >= minX() && x < maxX() && z >= minZ() && z < maxZ();
    }

    /** Verdadeiro se as duas regiões compartilham algum bloco. */
    public boolean overlaps(IslandRegion other) {
        return minX() < other.maxX() && other.minX() < maxX() && minZ() < other.maxZ() && other.minZ() < maxZ();
    }

    public int minChunkX() {
        return minX() >> 4;
    }

    public int maxChunkX() {
        return (maxX() - 1) >> 4;
    }

    public int minChunkZ() {
        return minZ() >> 4;
    }

    public int maxChunkZ() {
        return (maxZ() - 1) >> 4;
    }

    public IslandRegion withSize(int newSize) {
        return new IslandRegion(centerX, centerZ, newSize);
    }
}
