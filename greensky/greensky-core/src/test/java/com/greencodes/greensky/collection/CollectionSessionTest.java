package com.greencodes.greensky.collection;

import static org.junit.jupiter.api.Assertions.*;

import com.greencodes.greensky.content.ContentYaml;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CollectionSessionTest {
    private final UUID player = UUID.randomUUID();
    private final List<CompletableFuture<Map<String, Long>>> reads = new ArrayList<>();
    private final List<CompletableFuture<Void>> writes = new ArrayList<>();
    private final CollectionService service = new CollectionService(id -> {
        CompletableFuture<Map<String, Long>> read = new CompletableFuture<>();
        reads.add(read);
        return read;
    }, (id, batch) -> {
        CompletableFuture<Void> write = new CompletableFuture<>();
        writes.add(write);
        return write;
    }, CollectionCatalog.load(ContentYaml.parse("""
            collections:
              fishing:
                name: Pesca
                entries:
                  fish.cod: { name: Bacalhau, rarity: COMMON }
            """)));

    @Test
    void oldReadCannotReplaceNewSessionOrRestoreQuitSession() {
        CompletableFuture<Void> old = service.load(player);
        service.unload(player).join();
        CompletableFuture<Void> current = service.load(player);
        reads.get(1).complete(Map.of("fish.cod", 5L));
        current.join();
        service.record(player, "fish.cod", 2);
        reads.getFirst().complete(Map.of());
        old.join();
        assertEquals(Map.of("fish.cod", 7L), service.snapshot(player));
        service.unload(player);
        writes.getFirst().complete(null);
        service.load(player);
        service.unload(player).join();
        reads.get(2).complete(Map.of("fish.cod", 7L));
        assertFalse(service.isLoaded(player));
    }

    @Test
    void duplicateLoadSharesReadAndDoesNotErasePendingRecords() {
        CompletableFuture<Void> first = service.load(player);
        assertSame(first, service.load(player));
        assertEquals(1, reads.size());
        reads.getFirst().complete(Map.of());
        first.join();
        service.record(player, "fish.cod", 3);
        service.load(player).join();
        assertEquals(1, reads.size());
        assertEquals(Map.of("fish.cod", 3L), service.snapshot(player));
    }

    @Test
    void reconnectPreservesTotalsWhilePreviousWriteIsStillPending() {
        service.load(player);
        reads.getFirst().complete(Map.of("fish.cod", 10L));
        service.record(player, "fish.cod", 5);
        service.unload(player);
        service.load(player).join();
        assertEquals(1, reads.size(), "não ler banco antes da confirmação das somas locais");
        assertEquals(Map.of("fish.cod", 15L), service.snapshot(player));
        assertEquals(CollectionService.Outcome.PROGRESSED, service.record(player, "fish.cod", 1));
        writes.getFirst().complete(null);
        assertEquals(Map.of("fish.cod", 16L), service.snapshot(player));
    }

    @Test
    void failedLoadAllowsRetryWithoutPublishingEmptyProgress() {
        CompletableFuture<Void> first = service.load(player);
        reads.getFirst().completeExceptionally(new IllegalStateException("offline"));
        assertThrows(java.util.concurrent.CompletionException.class, first::join);
        assertFalse(service.isLoaded(player));
        assertEquals(CollectionService.Outcome.NOT_LOADED, service.record(player, "fish.cod", 1));
        CompletableFuture<Void> retry = service.load(player);
        reads.get(1).complete(Map.of("fish.cod", 8L));
        retry.join();
        assertEquals(Map.of("fish.cod", 8L), service.snapshot(player));
    }
}
