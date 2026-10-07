package com.greencodes.greensky.island;

public enum IslandState {
    /** Linha criada no banco, mas os blocos ainda não foram confirmados (ex.: crash no meio). */
    PENDING,
    READY
}
