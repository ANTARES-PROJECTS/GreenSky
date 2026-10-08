package com.greencodes.greensky.island;

import java.util.concurrent.CompletableFuture;

/**
 * Coloca no mundo a estrutura inicial de uma ilha. Deve ser idempotente (pode rodar de novo
 * sobre a mesma ilha após um crash). Hoje é gerada por código; no futuro pode vir de templates.
 */
public interface IslandBuilder {

    /** Completa quando os blocos estiverem no mundo. Pode ser chamado de qualquer thread. */
    CompletableFuture<Void> build(IslandRegion region);
}
