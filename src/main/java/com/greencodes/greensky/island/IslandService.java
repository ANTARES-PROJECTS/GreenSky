package com.greencodes.greensky.island;

import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.player.PlayerRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Regras de negócio das ilhas. Tudo é assíncrono; nenhuma chamada bloqueia a thread do
 * servidor. Falhas de regra chegam como {@link IslandException} dentro do future.
 */
public final class IslandService {

    private final Database database;
    private final IslandRepository islands;
    private final PlayerRepository players;
    private final IslandBuilder builder;
    private final IslandSettings settings;

    /** Cache dono -> ilha. Só guarda ilhas existentes; atualizado na criação e no estado. */
    private final ConcurrentMap<UUID, Island> byOwner = new ConcurrentHashMap<>();

    public IslandService(
            Database database,
            IslandRepository islands,
            PlayerRepository players,
            IslandBuilder builder,
            IslandSettings settings) {
        this.database = database;
        this.islands = islands;
        this.players = players;
        this.builder = builder;
        this.settings = settings;
    }

    /**
     * Cria a ilha do jogador: reserva o slot e grava a linha (PENDING) numa transação, gera os
     * blocos e só então marca READY. Se algo falhar no meio, a linha fica PENDING e
     * {@link #ensureReady} refaz a geração.
     *
     * @throws IslandException (no future) ALREADY_HAS_ISLAND se o jogador já tem ilha
     */
    public CompletableFuture<Island> create(UUID owner, String ownerName) {
        return database
                .transaction(c -> {
                    players.upsert(c, owner, ownerName);
                    long slot = islands.nextSlot(c);
                    IslandRegion region = IslandRegion.forSlot(slot, settings.spacing(), settings.initialSize());
                    Island island = new Island(UUID.randomUUID(), owner, slot, region, IslandState.PENDING);
                    islands.insert(c, island);
                    islands.addMember(c, island.id(), owner, IslandRole.OWNER, EnumSet.noneOf(IslandPermission.class));
                    return island;
                })
                .thenCompose(this::generate);
    }

    /** Ilha da qual o jogador é dono, se houver. */
    public CompletableFuture<Optional<Island>> findByOwner(UUID owner) {
        Island cached = byOwner.get(owner);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return database.query(c -> islands.findByOwner(c, owner)).thenApply(found -> {
            found.ifPresent(island -> byOwner.put(owner, island));
            return found;
        });
    }

    /** Garante que os blocos da ilha existem; refaz a geração de ilhas PENDING (recuperação). */
    public CompletableFuture<Island> ensureReady(Island island) {
        return island.isReady() ? CompletableFuture.completedFuture(island) : generate(island);
    }

    public CompletableFuture<List<IslandMember>> members(UUID islandId) {
        return database.query(c -> islands.members(c, islandId));
    }

    /**
     * Adiciona um membro com as permissões padrão. Apenas o dono pode.
     *
     * @throws IslandException NOT_OWNER ou ALREADY_MEMBER
     */
    public CompletableFuture<IslandMember> addMember(Island island, UUID actor, UUID target, String targetName) {
        if (!island.owner().equals(actor)) {
            return denied(IslandException.Reason.NOT_OWNER);
        }
        Set<IslandPermission> defaults = IslandPermission.defaultsForMember();
        return database.transaction(c -> {
            players.upsert(c, target, targetName);
            islands.addMember(c, island.id(), target, IslandRole.MEMBER, defaults);
            return new IslandMember(island.id(), target, IslandRole.MEMBER, defaults);
        });
    }

    /**
     * Remove um membro. O dono pode remover qualquer membro; um membro pode sair sozinho. O
     * dono nunca pode ser removido.
     *
     * @throws IslandException NOT_OWNER, CANNOT_REMOVE_OWNER ou NOT_A_MEMBER
     */
    public CompletableFuture<Void> removeMember(Island island, UUID actor, UUID target) {
        if (target.equals(island.owner())) {
            return denied(IslandException.Reason.CANNOT_REMOVE_OWNER);
        }
        if (!island.owner().equals(actor) && !actor.equals(target)) {
            return denied(IslandException.Reason.NOT_OWNER);
        }
        return database.transaction(c -> {
            if (!islands.removeMember(c, island.id(), target)) {
                throw new IslandException(IslandException.Reason.NOT_A_MEMBER);
            }
            return null;
        });
    }

    /**
     * Substitui as permissões de um membro. Apenas o dono pode; o dono em si tem tudo implícito.
     *
     * @throws IslandException NOT_OWNER ou NOT_A_MEMBER
     */
    public CompletableFuture<Void> setPermissions(
            Island island, UUID actor, UUID target, Set<IslandPermission> permissions) {
        if (!island.owner().equals(actor)) {
            return denied(IslandException.Reason.NOT_OWNER);
        }
        return database.transaction(c -> {
            Optional<IslandMember> member = islands.findMember(c, island.id(), target);
            if (member.isEmpty() || member.get().role() == IslandRole.OWNER) {
                throw new IslandException(IslandException.Reason.NOT_A_MEMBER);
            }
            islands.setPermissions(c, island.id(), target, permissions);
            return null;
        });
    }

    /** Descarta o cache de um dono (ex.: após alteração feita fora deste serviço). */
    public void invalidate(UUID owner) {
        byOwner.remove(owner);
    }

    private CompletableFuture<Island> generate(Island island) {
        return builder.build(island.region())
                .thenCompose(ignored -> database.query(c -> {
                    islands.updateState(c, island.id(), IslandState.READY);
                    return null;
                }))
                .thenApply(ignored -> {
                    Island ready = island.withState(IslandState.READY);
                    byOwner.put(ready.owner(), ready);
                    return ready;
                });
    }

    private static <T> CompletableFuture<T> denied(IslandException.Reason reason) {
        return CompletableFuture.failedFuture(new IslandException(reason));
    }
}
