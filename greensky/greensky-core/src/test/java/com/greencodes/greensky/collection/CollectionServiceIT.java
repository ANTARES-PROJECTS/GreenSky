package com.greencodes.greensky.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.player.PlayerRepository;
import com.greencodes.greensky.player.PlayerService;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Coleções contra PostgreSQL real (inclui a V5). */
@EnabledIfEnvironmentVariable(named = DatabaseSettings.PASSWORD_ENV, matches = ".+")
class CollectionServiceIT {

    private static Database db;
    private static PlayerService players;
    private static CollectionCatalog catalog;
    private final List<UUID> created = new ArrayList<>();

    @BeforeAll
    static void open() throws Exception {
        DatabaseSettings settings = new DatabaseSettings("127.0.0.1", 5432, "greensky", "greensky", 6);
        db = Database.open(settings, System.getenv(DatabaseSettings.PASSWORD_ENV),
                CollectionServiceIT.class.getClassLoader());
        players = new PlayerService(db, new PlayerRepository());
        YamlConfiguration yaml = com.greencodes.greensky.content.ContentYaml.parse("""
                collections:
                  fishing:
                    name: "Pesca"
                    entries:
                      fish.cod: { name: "Bacalhau", rarity: COMMON }
                      fish.koi: { name: "Koi", rarity: MYTHIC }
                """);
        catalog = CollectionCatalog.load(yaml);
    }

    @AfterAll
    static void close() {
        db.close();
    }

    @AfterEach
    void cleanup() throws Exception {
        db.transaction(c -> {
            for (UUID id : created) {
                for (String sql : new String[] {
                    "DELETE FROM collection_progress WHERE player_uuid = ?", "DELETE FROM players WHERE uuid = ?"}) {
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setObject(1, id);
                        ps.executeUpdate();
                    }
                }
            }
            return null;
        }).get(10, TimeUnit.SECONDS);
        created.clear();
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    private UUID player() throws Exception {
        UUID id = UUID.randomUUID();
        created.add(id);
        String clock = Long.toString(System.nanoTime(), 36);
        players.claim(id, "Co" + clock.substring(clock.length() - 8) + SEQ.incrementAndGet()).get(10, TimeUnit.SECONDS);
        return id;
    }

    private static CollectionService service() {
        return new CollectionService(db, new CollectionRepository(), catalog);
    }

    @Test
    void firstTimeIsDiscoveryThenProgress() throws Exception {
        UUID p = player();
        CollectionService service = service();
        service.load(p).get(10, TimeUnit.SECONDS);

        assertEquals(CollectionService.Outcome.DISCOVERED, service.record(p, "fish.cod", 1));
        assertEquals(CollectionService.Outcome.PROGRESSED, service.record(p, "fish.cod", 2));
        assertEquals(CollectionService.Outcome.DISCOVERED, service.record(p, "fish.koi", 1));
        assertEquals(Map.of("fish.cod", 3L, "fish.koi", 1L), service.snapshot(p));
    }

    @Test
    void rejectsUnknownEntriesBadAmountsAndUnloadedPlayers() throws Exception {
        UUID p = player();
        CollectionService service = service();
        assertEquals(CollectionService.Outcome.NOT_LOADED, service.record(p, "fish.cod", 1));
        service.load(p).get(10, TimeUnit.SECONDS);
        assertEquals(CollectionService.Outcome.UNKNOWN_ENTRY, service.record(p, "fish.nao_existe", 1));
        assertEquals(CollectionService.Outcome.UNKNOWN_ENTRY, service.record(p, "fish.cod", 0));
        assertEquals(CollectionService.Outcome.UNKNOWN_ENTRY, service.record(p, "fish.cod", -5));
        assertTrue(service.snapshot(p).isEmpty());
    }

    @Test
    void progressSurvivesFlushAndReloadAndIsNotRediscovered() throws Exception {
        UUID p = player();
        CollectionService first = service();
        first.load(p).get(10, TimeUnit.SECONDS);
        first.record(p, "fish.cod", 5);
        first.unload(p).get(10, TimeUnit.SECONDS); // sair grava o lote

        CollectionService second = service(); // "restart": memória vazia
        second.load(p).get(10, TimeUnit.SECONDS);
        assertEquals(Map.of("fish.cod", 5L), second.snapshot(p));
        // Já descoberta antes: não descobre de novo.
        assertEquals(CollectionService.Outcome.PROGRESSED, second.record(p, "fish.cod", 1));
        second.flushAll().get(10, TimeUnit.SECONDS);
        second.flushAll().get(10, TimeUnit.SECONDS); // lote vazio: não pode somar de novo

        CollectionService third = service();
        third.load(p).get(10, TimeUnit.SECONDS);
        assertEquals(Map.of("fish.cod", 6L), third.snapshot(p));
    }

    @Test
    void concurrentRecordsAreNeitherLostNorDoubleCounted() throws Exception {
        UUID p = player();
        CollectionService service = service();
        service.load(p).get(10, TimeUnit.SECONDS);
        AtomicInteger discoveries = new AtomicInteger();
        List<CompletableFuture<Void>> work = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            work.add(CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 250; i++) {
                    if (service.record(p, "fish.cod", 1) == CollectionService.Outcome.DISCOVERED) {
                        discoveries.incrementAndGet();
                    }
                    if (i % 50 == 0) {
                        service.flush(p).join(); // lotes no meio dos registros
                    }
                }
            }));
        }
        CompletableFuture.allOf(work.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);
        service.flushAll().get(10, TimeUnit.SECONDS);

        assertEquals(1, discoveries.get(), "a descoberta acontece uma única vez");
        assertEquals(2000L, service.snapshot(p).get("fish.cod"));
        CollectionService reloaded = service();
        reloaded.load(p).get(10, TimeUnit.SECONDS);
        assertEquals(2000L, reloaded.snapshot(p).get("fish.cod"), "o banco tem exatamente a soma de tudo");
    }
}
