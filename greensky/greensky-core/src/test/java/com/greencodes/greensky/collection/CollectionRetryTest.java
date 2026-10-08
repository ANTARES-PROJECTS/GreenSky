package com.greencodes.greensky.collection;

import static org.junit.jupiter.api.Assertions.*;

import com.greencodes.greensky.content.ContentYaml;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CollectionRetryTest {
    private final UUID player = UUID.randomUUID();
    private final List<Map<String, Long>> batches = new ArrayList<>();
    private final List<CompletableFuture<Void>> writes = new ArrayList<>();

    private CollectionService service() {
        CollectionCatalog catalog = CollectionCatalog.load(ContentYaml.parse("""
                collections:
                  fishing:
                    name: Pesca
                    entries:
                      fish.cod: { name: Bacalhau, rarity: COMMON }
                """));
        return new CollectionService(id -> CompletableFuture.completedFuture(Map.of()), (id, batch) -> {
            assertEquals(player, id);
            batches.add(Map.copyOf(batch));
            CompletableFuture<Void> write = new CompletableFuture<>();
            writes.add(write);
            return write;
        }, catalog);
    }

    @Test
    void failedQuitBatchIsRetriedEvenAfterReconnect() {
        CollectionService service = service();
        service.load(player).join();
        service.record(player, "fish.cod", 5);
        CompletableFuture<Void> quit = service.unload(player);
        assertFalse(service.isLoaded(player));
        writes.getFirst().completeExceptionally(new IllegalStateException("banco indisponível"));
        quit.join();
        service.load(player).join();
        CompletableFuture<Void> retry = service.flushAll();
        assertEquals(List.of(Map.of("fish.cod", 5L), Map.of("fish.cod", 5L)), batches);
        writes.get(1).complete(null);
        retry.join();
        service.flushAll().join();
        assertEquals(2, writes.size(), "não reenviar lote já confirmado");
    }

    @Test
    void quitRetainsBatchAlreadyBeingWrittenAndNewPendingAmounts() {
        CollectionService service = service();
        service.load(player).join();
        service.record(player, "fish.cod", 3);
        CompletableFuture<Void> flush = service.flush(player);
        service.record(player, "fish.cod", 2);
        service.unload(player);
        assertEquals(1, writes.size(), "uma gravação por sessão por vez");
        writes.getFirst().completeExceptionally(new IllegalStateException("falha"));
        flush.join();
        CompletableFuture<Void> retry = service.flushAll();
        assertEquals(Map.of("fish.cod", 5L), batches.get(1));
        writes.get(1).complete(null);
        retry.join();
        service.flushAll().join();
        assertEquals(2, writes.size());
    }

    @Test
    void successfulInFlightBatchDoesNotDiscardRemainingQuitProgress() {
        CollectionService service = service();
        service.load(player).join();
        service.record(player, "fish.cod", 3);
        service.flush(player);
        service.record(player, "fish.cod", 2);
        service.unload(player);
        CompletableFuture<Void> finalFlush = service.flushAll();
        writes.getFirst().complete(null);
        assertFalse(finalFlush.isDone(), "desligamento espera também o lote restante");
        CompletableFuture<Void> retry = service.flushAll();
        assertEquals(Map.of("fish.cod", 2L), batches.get(1));
        writes.get(1).complete(null);
        retry.join();
        finalFlush.join();
        service.flushAll().join();
        assertEquals(2, writes.size());
    }
}
