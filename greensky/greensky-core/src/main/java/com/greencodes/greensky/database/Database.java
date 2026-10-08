package com.greencodes.greensky.database;

import com.greencodes.greensky.core.config.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Pool de conexões + executor assíncrono. Todo acesso SQL passa por aqui e roda
 * fora da thread do servidor; os repositories recebem uma instância desta classe.
 */
public final class Database implements AutoCloseable {

    private static final String MIGRATIONS = "classpath:db/migration";

    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    private Database(HikariDataSource dataSource, ExecutorService executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    /**
     * Conecta, aplica as migrations pendentes e devolve o banco pronto. Bloqueante:
     * chamar apenas no boot do plugin, nunca durante o jogo.
     *
     * @param migrationLoader class loader que enxerga {@code db/migration} (o do plugin)
     */
    public static Database open(DatabaseSettings settings, String password, ClassLoader migrationLoader) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("GreenSky-DB");
        hikari.setJdbcUrl(settings.jdbcUrl());
        hikari.setUsername(settings.user());
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(settings.poolSize());
        hikari.setConnectionTimeout(10_000);
        // O DriverManager não enxerga drivers de class loaders de plugin; indicar a classe explicitamente.
        hikari.setDriverClassName(org.postgresql.Driver.class.getName());

        HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(hikari);
        } catch (RuntimeException e) {
            throw new DatabaseException("Não foi possível conectar em " + settings.jdbcUrl(), e);
        }

        try {
            new FluentConfiguration(migrationLoader)
                    .dataSource(dataSource)
                    .locations(MIGRATIONS)
                    // Falha alto se as migrations não forem achadas (ex.: erro de empacotamento).
                    .failOnMissingLocations(true)
                    .load()
                    .migrate();
        } catch (RuntimeException e) {
            dataSource.close();
            throw new DatabaseException("Falha ao aplicar migrations", e);
        }

        ExecutorService executor = Executors.newFixedThreadPool(settings.poolSize(), new DbThreadFactory());
        return new Database(dataSource, executor);
    }

    /** Executa {@code work} numa conexão em auto-commit, fora da thread do chamador. */
    public <T> CompletableFuture<T> query(SqlFunction<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return work.apply(connection);
            } catch (SQLException e) {
                throw new CompletionException(new DatabaseException("Falha na consulta SQL", e));
            }
        }, executor);
    }

    /**
     * Executa {@code work} numa transação: commit se terminar, rollback se lançar
     * qualquer exceção. Tudo ou nada.
     */
    public <T> CompletableFuture<T> transaction(SqlFunction<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.apply(connection);
                    connection.commit();
                    return result;
                } catch (Throwable t) {
                    rollbackQuietly(connection, t);
                    throw t;
                }
            } catch (SQLException e) {
                throw new CompletionException(new DatabaseException("Falha na transação SQL", e));
            }
        }, executor);
    }

    /** Verifica que o banco responde. */
    public CompletableFuture<Void> ping() {
        return query(connection -> {
            try (Statement st = connection.createStatement()) {
                st.execute("SELECT 1");
            }
            return null;
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        dataSource.close();
    }

    private static void rollbackQuietly(Connection connection, Throwable cause) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }

    private static final class DbThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "greensky-db-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
