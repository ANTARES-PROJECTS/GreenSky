package com.greencodes.greensky.player;

import com.greencodes.greensky.database.Database;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Identidade dos jogadores. A identidade é o UUID; o nick é único sem diferenciar
 * maiúsculas (índice no banco), porque com online-mode=false o UUID é derivado do nick exato
 * e "Gustavo"/"gustavo" seriam contas diferentes.
 */
public final class PlayerService {

    private final Database database;
    private final PlayerRepository players;

    public PlayerService(Database database, PlayerRepository players) {
        this.database = database;
        this.players = players;
    }

    /**
     * O nick pode entrar com este UUID? Vazio = pode; preenchido = o nick já pertence a outra
     * conta (devolve a grafia registrada).
     */
    public CompletableFuture<Optional<String>> conflictingName(UUID uuid, String name) {
        return database.query(c -> players.findByNameIgnoreCase(c, name)
                .filter(found -> !found.uuid().equals(uuid))
                .map(PlayerRecord::name));
    }

    /**
     * Registra (ou atualiza) o jogador com o nick atual. Chamado só depois da autenticação,
     * para que ninguém "reserve" um nick sem ter feito login.
     *
     * @throws NameTakenException (no future) se outra conta já tem esse nick
     */
    public CompletableFuture<Void> claim(UUID uuid, String name) {
        return database.transaction(c -> {
            players.upsert(c, uuid, name);
            return null;
        }).thenAccept(ignored -> {});
    }

    /** Jogador pelo nick, sem diferenciar maiúsculas. */
    public CompletableFuture<Optional<PlayerRecord>> findByName(String name) {
        return database.query(c -> players.findByNameIgnoreCase(c, name));
    }

    public CompletableFuture<Optional<PlayerRecord>> findByUuid(UUID uuid) {
        return database.query(c -> players.findByUuid(c, uuid));
    }
}
