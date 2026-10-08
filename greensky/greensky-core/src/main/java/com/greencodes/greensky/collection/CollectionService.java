package com.greencodes.greensky.collection;

import com.greencodes.greensky.database.Database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Progresso das coleções. Sem Bukkit (testável): quem chama é que dispara eventos.
 *
 * <p>Desempenho: o progresso de quem está online fica em memória. Registrar não toca o banco;
 * as somas pendentes vão em lote no {@link #flush} (a cada poucos segundos, ao sair e ao
 * desligar). Lotes com erro permanecem em memória inclusive após a saída; um crash pode
 * perder tudo que ainda não foi confirmado pelo banco.
 */
public final class CollectionService {

    private static final Logger LOGGER = Logger.getLogger(CollectionService.class.getName());

    public enum Outcome {
        /** Primeira vez desta entrada para o jogador. */
        DISCOVERED,
        /** Já tinha; só somou. */
        PROGRESSED,
        /** A chave não existe em collections.yml. */
        UNKNOWN_ENTRY,
        /** O progresso do jogador ainda não foi carregado (não está pronto/online). */
        NOT_LOADED
    }

    private final Function<UUID, CompletableFuture<Map<String, Long>>> loader;
    private final BiFunction<UUID, Map<String, Long>, CompletableFuture<Void>> writer;
    private final CollectionCatalog catalog;
    private final ConcurrentHashMap<UUID, PlayerProgress> loaded = new ConcurrentHashMap<>();
    // Sessões encerradas continuam aqui até todos os lotes serem confirmados pelo banco.
    private final Set<PlayerProgress> retiring = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Void>> loading = new HashMap<>();

    public CollectionService(Database database, CollectionRepository repository, CollectionCatalog catalog) {
        this(player -> database.query(c -> repository.load(c, player)),
                (player, batch) -> database.transaction(c -> {
                    repository.addAmounts(c, player, batch);
                    return null;
                }), catalog);
    }

    CollectionService(Function<UUID, CompletableFuture<Map<String, Long>>> loader,
            BiFunction<UUID, Map<String, Long>, CompletableFuture<Void>> writer, CollectionCatalog catalog) {
        this.loader = loader;
        this.writer = writer;
        this.catalog = catalog;
    }

    public CollectionCatalog catalog() {
        return catalog;
    }

    /** Carrega o progresso salvo (chamado quando o jogador fica pronto, depois do login). */
    public synchronized CompletableFuture<Void> load(UUID player) {
        if (loaded.containsKey(player)) {
            return CompletableFuture.completedFuture(null);
        }
        if (loading.containsKey(player)) {
            return loading.get(player);
        }
        // Reconexão usa os totais locais enquanto há lotes não confirmados/em andamento.
        for (PlayerProgress progress : retiring) {
            if (progress.player.equals(player) && retiring.remove(progress)) {
                loaded.put(player, progress);
                return CompletableFuture.completedFuture(null);
            }
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        loading.put(player, result);
        CompletableFuture<Map<String, Long>> read;
        try {
            read = loader.apply(player);
        } catch (RuntimeException error) {
            read = CompletableFuture.failedFuture(error);
        }
        read.whenComplete((totals, error) -> {
            synchronized (this) {
                if (loading.get(player) == result) {
                    loading.remove(player);
                    if (error == null) {
                        loaded.put(player, new PlayerProgress(player, totals));
                    }
                }
            }
            if (error == null) {
                result.complete(null);
            } else {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    public boolean isLoaded(UUID player) {
        return loaded.containsKey(player);
    }

    /** Soma {@code amount} à entrada. Não acessa o banco. */
    public synchronized Outcome record(UUID player, String entryKey, long amount) {
        if (amount <= 0 || catalog.entry(entryKey).isEmpty()) {
            return Outcome.UNKNOWN_ENTRY;
        }
        PlayerProgress progress = loaded.get(player);
        if (progress == null) {
            return Outcome.NOT_LOADED;
        }
        return progress.add(entryKey, amount) ? Outcome.DISCOVERED : Outcome.PROGRESSED;
    }

    /** Cópia do progresso em memória (vazia se não carregado). */
    public Map<String, Long> snapshot(UUID player) {
        PlayerProgress progress = loaded.get(player);
        return progress == null ? Map.of() : progress.totals();
    }

    /**
     * Grava em lote o que está pendente do jogador. Se o banco falhar, as somas voltam para a
     * fila e vão no próximo lote (nada se perde por erro de gravação).
     */
    public CompletableFuture<Void> flush(UUID player) {
        PlayerProgress progress = loaded.get(player);
        if (progress == null) {
            return CompletableFuture.completedFuture(null);
        }
        return flushProgress(progress);
    }

    private CompletableFuture<Void> flushProgress(PlayerProgress progress) {
        synchronized (progress) {
            if (progress.inFlight != null) {
                return progress.inFlight;
            }
            Map<String, Long> batch = progress.drainPending();
            if (batch.isEmpty()) {
                retiring.remove(progress);
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<Void> result = new CompletableFuture<>();
            progress.inFlight = result;
            CompletableFuture<Void> write;
            try {
                write = writer.apply(progress.player, batch);
            } catch (RuntimeException error) {
                write = CompletableFuture.failedFuture(error);
            }
            write.whenComplete((ignored, error) -> {
                boolean finishRemaining;
                synchronized (progress) {
                    if (error != null) {
                        progress.requeue(batch);
                        LOGGER.log(Level.WARNING, "Falha ao gravar coleções de " + progress.player
                                + "; vai no próximo lote", error);
                    }
                    progress.inFlight = null;
                    if (progress.pending.isEmpty()) {
                        retiring.remove(progress);
                    }
                    finishRemaining = error == null && retiring.contains(progress);
                }
                if (finishRemaining) {
                    // A saída/desligamento também espera o pendente surgido durante o lote anterior.
                    flushProgress(progress).whenComplete((done, failure) -> result.complete(null));
                } else {
                    result.complete(null);
                }
            });
            return result;
        }
    }

    public CompletableFuture<Void> flushAll() {
        List<CompletableFuture<Void>> all = new ArrayList<>();
        for (UUID player : loaded.keySet()) {
            all.add(flush(player));
        }
        for (PlayerProgress progress : retiring) {
            all.add(flushProgress(progress));
        }
        return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new));
    }

    /** Encerra o acesso em memória; mantém os lotes pendentes para novas tentativas. */
    public synchronized CompletableFuture<Void> unload(UUID player) {
        loading.remove(player); // resposta de uma sessão encerrada não pode criar progresso online
        PlayerProgress session = loaded.remove(player);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        retiring.add(session);
        return flushProgress(session);
    }

    /** Progresso de um jogador online. Métodos sincronizados: registros podem vir de threads diferentes. */
    private static final class PlayerProgress {
        private final UUID player;
        private final Map<String, Long> totals;
        private Map<String, Long> pending = new HashMap<>();
        private CompletableFuture<Void> inFlight;

        PlayerProgress(UUID player, Map<String, Long> totals) {
            this.player = player;
            this.totals = new HashMap<>(totals);
        }

        /** @return true se é a primeira vez da entrada */
        synchronized boolean add(String key, long amount) {
            boolean first = !totals.containsKey(key);
            totals.merge(key, amount, Long::sum);
            pending.merge(key, amount, Long::sum);
            return first;
        }

        synchronized Map<String, Long> drainPending() {
            Map<String, Long> batch = pending;
            pending = new HashMap<>();
            return batch;
        }

        synchronized void requeue(Map<String, Long> batch) {
            batch.forEach((key, amount) -> pending.merge(key, amount, Long::sum));
        }

        synchronized Map<String, Long> totals() {
            return Map.copyOf(totals);
        }
    }
}
