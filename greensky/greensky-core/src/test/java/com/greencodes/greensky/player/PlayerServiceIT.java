package com.greencodes.greensky.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.database.Database;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Identidade dos jogadores contra PostgreSQL real (inclui o índice único da V4). */
@EnabledIfEnvironmentVariable(named = DatabaseSettings.PASSWORD_ENV, matches = ".+")
class PlayerServiceIT {

    private static Database db;
    private static PlayerService service;
    private final List<UUID> created = new ArrayList<>();

    @BeforeAll
    static void open() {
        DatabaseSettings settings = new DatabaseSettings("127.0.0.1", 5432, "greensky", "greensky", 4);
        db = Database.open(settings, System.getenv(DatabaseSettings.PASSWORD_ENV), PlayerServiceIT.class.getClassLoader());
        service = new PlayerService(db, new PlayerRepository());
    }

    @AfterAll
    static void close() {
        db.close();
    }

    @AfterEach
    void cleanup() throws Exception {
        db.transaction(c -> {
            for (UUID id : created) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM players WHERE uuid = ?")) {
                    ps.setObject(1, id);
                    ps.executeUpdate();
                }
            }
            return null;
        }).get(10, TimeUnit.SECONDS);
        created.clear();
    }

    private UUID uuid() {
        UUID id = UUID.randomUUID();
        created.add(id);
        return id;
    }

    /** Nick único por execução (máx. 16 caracteres no Minecraft). */
    private static String nick() {
        String clock = Long.toString(System.nanoTime(), 36); // os dígitos finais mudam a cada chamada
        return "It" + clock.substring(Math.max(0, clock.length() - 10));
    }

    @Test
    void sameNickWithDifferentCaseCannotBelongToTwoAccounts() throws Exception {
        String nick = nick();
        UUID first = uuid();
        UUID second = uuid();
        service.claim(first, nick).get(10, TimeUnit.SECONDS);

        ExecutionException e = assertThrows(ExecutionException.class,
                () -> service.claim(second, nick.toUpperCase()).get(10, TimeUnit.SECONDS));
        assertInstanceOf(NameTakenException.class, e.getCause());
        e = assertThrows(ExecutionException.class,
                () -> service.claim(second, nick.toLowerCase()).get(10, TimeUnit.SECONDS));
        assertInstanceOf(NameTakenException.class, e.getCause());
    }

    @Test
    void preLoginCheckReportsTheRegisteredSpelling() throws Exception {
        String nick = nick();
        UUID owner = uuid();
        service.claim(owner, nick).get(10, TimeUnit.SECONDS);

        assertEquals(Optional.of(nick), service.conflictingName(uuid(), nick.toUpperCase()).get(10, TimeUnit.SECONDS));
        // A própria conta (mesmo UUID) entra normalmente.
        assertTrue(service.conflictingName(owner, nick).get(10, TimeUnit.SECONDS).isEmpty());
        // Nick que ninguém usa: livre.
        assertTrue(service.conflictingName(uuid(), nick()).get(10, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void findByNameIgnoresCaseAndReturnsTheSingleAccount() throws Exception {
        String nick = nick();
        UUID owner = uuid();
        service.claim(owner, nick).get(10, TimeUnit.SECONDS);

        for (String spelling : new String[] {nick, nick.toUpperCase(), nick.toLowerCase()}) {
            PlayerRecord found = service.findByName(spelling).get(10, TimeUnit.SECONDS).orElseThrow();
            assertEquals(owner, found.uuid());
            assertEquals(nick, found.name());
        }
        assertTrue(service.findByName(nick()).get(10, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void simultaneousClaimsOfTheSameAccountAllSucceed() throws Exception {
        // Regressão: com o índice único do nome, inserções simultâneas da MESMA conta eram
        // confundidas com "nick de outra conta".
        String nick = nick();
        UUID owner = uuid();
        List<java.util.concurrent.CompletableFuture<Void>> claims = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            claims.add(service.claim(owner, nick));
        }
        for (java.util.concurrent.CompletableFuture<Void> claim : claims) {
            claim.get(10, TimeUnit.SECONDS); // nenhuma pode falhar
        }
        assertEquals(owner, service.findByName(nick).get(10, TimeUnit.SECONDS).orElseThrow().uuid());
    }

    @Test
    void simultaneousClaimsOfTheSameNickByDifferentAccountsLetOnlyOneWin() throws Exception {
        String nick = nick();
        List<UUID> accounts = new ArrayList<>();
        List<java.util.concurrent.CompletableFuture<Void>> claims = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            UUID id = uuid();
            accounts.add(id);
            // Grafias diferentes do mesmo nick, ao mesmo tempo.
            claims.add(service.claim(id, i % 2 == 0 ? nick.toUpperCase() : nick.toLowerCase()));
        }
        int ok = 0;
        for (java.util.concurrent.CompletableFuture<Void> claim : claims) {
            try {
                claim.get(10, TimeUnit.SECONDS);
                ok++;
            } catch (ExecutionException e) {
                assertInstanceOf(NameTakenException.class, e.getCause());
            }
        }
        assertEquals(1, ok, "só uma conta pode ficar com o nick");
        assertTrue(accounts.contains(service.findByName(nick).get(10, TimeUnit.SECONDS).orElseThrow().uuid()));
    }

    @Test
    void reclaimingTheSameAccountIsIdempotent() throws Exception {
        String nick = nick();
        UUID owner = uuid();
        service.claim(owner, nick).get(10, TimeUnit.SECONDS);
        service.claim(owner, nick).get(10, TimeUnit.SECONDS); // segundo login: só atualiza last_seen
        assertEquals(owner, service.findByName(nick).get(10, TimeUnit.SECONDS).orElseThrow().uuid());
    }
}
