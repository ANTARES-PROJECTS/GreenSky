package com.greencodes.greensky.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.core.config.DatabaseSettings;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Testes contra um PostgreSQL real (docker compose up -d). Só rodam quando
 * GREENSKY_DB_PASSWORD está definida; host/porta/nome/usuário usam os padrões do compose.
 */
@EnabledIfEnvironmentVariable(named = DatabaseSettings.PASSWORD_ENV, matches = ".+")
class DatabaseIT {

    private static Database db;

    @BeforeAll
    static void open() {
        DatabaseSettings settings = new DatabaseSettings("127.0.0.1", 5432, "greensky", "greensky", 4);
        db = Database.open(settings, System.getenv(DatabaseSettings.PASSWORD_ENV),
                DatabaseIT.class.getClassLoader());
    }

    @AfterAll
    static void close() {
        db.close();
    }

    @Test
    void pingWorks() throws Exception {
        db.ping().get(10, TimeUnit.SECONDS);
    }

    @Test
    void migrationCreatedPlayersTable() throws Exception {
        boolean exists = db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT to_regclass('public.players') IS NOT NULL");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }).get(10, TimeUnit.SECONDS);
        assertTrue(exists);
    }

    @Test
    void queryRunsOffTheCallerThread() throws Exception {
        String caller = Thread.currentThread().getName();
        String worker = db.query(c -> Thread.currentThread().getName()).get(10, TimeUnit.SECONDS);
        assertNotEquals(caller, worker);
        assertTrue(worker.startsWith("greensky-db-"));
    }

    @Test
    void transactionCommits() throws Exception {
        UUID id = UUID.randomUUID();
        db.transaction(c -> insertPlayer(c, id)).get(10, TimeUnit.SECONDS);
        assertTrue(playerExists(id));
        deletePlayer(id);
    }

    @Test
    void transactionRollsBackOnFailure() {
        UUID id = UUID.randomUUID();
        CompletableFuture<Object> failing = db.transaction(c -> {
            insertPlayer(c, id);
            throw new IllegalStateException("falha simulada (ex.: crash antes de entregar o item)");
        });
        ExecutionException e = assertThrows(ExecutionException.class, () -> failing.get(10, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof IllegalStateException);
        assertFalse(playerExists(id), "o insert deveria ter sofrido rollback");
    }

    @Test
    void sqlErrorsSurfaceAsDatabaseException() {
        CompletableFuture<Object> bad = db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM tabela_que_nao_existe")) {
                return ps.executeQuery();
            }
        });
        ExecutionException e = assertThrows(ExecutionException.class, () -> bad.get(10, TimeUnit.SECONDS));
        assertEquals(DatabaseException.class, e.getCause().getClass());
    }

    private static Object insertPlayer(java.sql.Connection c, UUID id) throws java.sql.SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO players (uuid, name) VALUES (?, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, "it-test");
            ps.executeUpdate();
        }
        return null;
    }

    private static boolean playerExists(UUID id) {
        return db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM players WHERE uuid = ?")) {
                ps.setObject(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }).join();
    }

    private static void deletePlayer(UUID id) {
        db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM players WHERE uuid = ?")) {
                ps.setObject(1, id);
                return ps.executeUpdate();
            }
        }).join();
    }
}
