package com.greencodes.greensky.island;

import com.greencodes.greensky.core.config.ExpansionSettings;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.player.PlayerRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Regras de negócio das ilhas. Tudo é assíncrono; nenhuma chamada bloqueia a thread do
 * servidor. Falhas de regra chegam como {@link IslandException} dentro do future.
 */
public final class IslandService {

    private static final Logger LOGGER = Logger.getLogger(IslandService.class.getName());

    private final Database database;
    private final IslandRepository islands;
    private final PlayerRepository players;
    private final IslandBuilder builder;
    private final IslandSettings settings;
    private final ExpansionSettings expansion;
    private final IslandVisibility defaultVisibility;

    /** Cache id da ilha -> visibilidade. Atualizado ao trocar; carregado sob demanda. */
    private final ConcurrentMap<UUID, IslandVisibility> visibilityById = new ConcurrentHashMap<>();

    /** Cache dono -> ilha. Só guarda ilhas existentes; atualizado na criação e no estado. */
    private final ConcurrentMap<UUID, Island> byOwner = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<Island>> generating = new ConcurrentHashMap<>();

    private final List<IslandListener> listeners = new CopyOnWriteArrayList<>();

    public IslandService(
            Database database,
            IslandRepository islands,
            PlayerRepository players,
            IslandBuilder builder,
            IslandSettings settings,
            ExpansionSettings expansion,
            IslandVisibility defaultVisibility) {
        this.database = database;
        this.islands = islands;
        this.players = players;
        this.builder = builder;
        this.settings = settings;
        this.expansion = expansion;
        this.defaultVisibility = defaultVisibility;
    }

    public CompletableFuture<IslandVisibility> visibility(Island island) {
        IslandVisibility cached = visibilityById.get(island.id());
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return database.query(c -> islands.visibility(c, island.id())).thenApply(found -> {
            IslandVisibility v = found.orElse(defaultVisibility);
            visibilityById.put(island.id(), v);
            return v;
        });
    }

    /**
     * Troca a visibilidade. Apenas o dono pode.
     *
     * @throws IslandException NOT_OWNER
     */
    public CompletableFuture<Void> setVisibility(Island island, UUID actor, IslandVisibility visibility) {
        if (!island.owner().equals(actor)) {
            return denied(IslandException.Reason.NOT_OWNER);
        }
        return database.query(c -> {
            islands.setVisibility(c, island.id(), visibility);
            return null;
        }).thenAccept(ignored -> {
            visibilityById.put(island.id(), visibility);
            notifyListeners(l -> l.onVisibilityChanged(island, visibility));
        });
    }

    /**
     * Decide se {@code visitor} pode visitar a ilha de {@code targetOwner} e devolve a ilha
     * pronta para o teleporte. Membros sempre podem; os demais, só se a ilha for pública.
     *
     * @throws IslandException TARGET_HAS_NO_ISLAND ou ISLAND_PRIVATE
     */
    public CompletableFuture<Island> authorizeVisit(UUID visitor, UUID targetOwner) {
        return findByOwner(targetOwner).thenCompose(found -> {
            if (found.isEmpty()) {
                return denied(IslandException.Reason.TARGET_HAS_NO_ISLAND);
            }
            Island island = found.get();
            return visibility(island).thenCompose(visibility -> {
                if (visibility == IslandVisibility.PUBLIC) {
                    return ensureReady(island);
                }
                return database.query(c -> islands.findMember(c, island.id(), visitor)).thenCompose(member ->
                        member.isPresent()
                                ? ensureReady(island)
                                : denied(IslandException.Reason.ISLAND_PRIVATE));
            });
        });
    }

    /**
     * Sobe a ilha para o próximo nível: mesmo centro, região maior. Não regenera nada (o
     * mundo é void; a área nova já existe vazia). Uma única UPDATE condicional garante que,
     * de duas expansões simultâneas, só uma vale. Nada a recuperar após crash: ou a linha
     * mudou, ou não.
     *
     * @throws IslandException (no future) MAX_SIZE_REACHED ou EXPANSION_CONFLICT
     */
    public CompletableFuture<Island> expand(Island island) {
        OptionalInt next = expansion.nextAfter(island.region().size());
        if (next.isEmpty()) {
            return denied(IslandException.Reason.MAX_SIZE_REACHED);
        }
        int newSize = next.getAsInt();
        return database.query(c -> islands.updateSize(c, island.id(), island.region().size(), newSize))
                .thenApply(updated -> {
                    if (!updated) {
                        // Alguém expandiu antes: o cache pode estar velho; a próxima leitura vem do banco.
                        byOwner.remove(island.owner());
                        throw new IslandException(IslandException.Reason.EXPANSION_CONFLICT);
                    }
                    Island expanded = new Island(
                            island.id(), island.owner(), island.slot(), island.region().withSize(newSize), island.state());
                    byOwner.put(expanded.owner(), expanded);
                    notifyListeners(l -> l.onIslandExpanded(expanded));
                    return expanded;
                });
    }

    /** Nível de expansão atual (1 = inicial) de uma ilha. */
    public int levelOf(Island island) {
        return expansion.levelOf(island.region().size());
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
                    islands.setVisibility(c, island.id(), defaultVisibility);
                    return island;
                })
                .thenCompose(island -> {
                    // Avisa já com a linha gravada, antes de gerar os blocos: a proteção cobre a área desde o início.
                    IslandMember ownerMember =
                            new IslandMember(island.id(), owner, IslandRole.OWNER, EnumSet.noneOf(IslandPermission.class));
                    notifyListeners(l -> l.onIslandCreated(island, ownerMember));
                    return generate(island);
                });
    }

    /** Todas as ilhas, para montar o índice espacial da proteção. */
    public CompletableFuture<List<Island>> loadAll() {
        return database.query(islands::findAll);
    }

    /** Participações do jogador (dono ou membro) em qualquer ilha. */
    public CompletableFuture<List<IslandMember>> membershipsOf(UUID player) {
        return database.query(c -> islands.membershipsOf(c, player));
    }

    public void addListener(IslandListener listener) {
        listeners.add(listener);
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
        }).thenApply(member -> {
            notifyListeners(l -> l.onMemberAdded(member));
            return member;
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
        }).thenAccept(ignored -> notifyListeners(l -> l.onMemberRemoved(island.id(), target)));
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
        }).thenAccept(ignored -> {
            IslandMember updated = new IslandMember(island.id(), target, IslandRole.MEMBER, permissions);
            notifyListeners(l -> l.onMemberPermissionsChanged(updated));
        });
    }

    /** Descarta o cache de um dono (ex.: após alteração feita fora deste serviço). */
    public void invalidate(UUID owner) {
        byOwner.remove(owner);
    }

    private CompletableFuture<Island> generate(Island island) {
        CompletableFuture<Island> result = new CompletableFuture<>();
        CompletableFuture<Island> existing = generating.putIfAbsent(island.id(), result);
        if (existing != null) {
            return existing;
        }
        // Um objeto PENDING antigo não pode regenerar uma ilha já concluída.
        database.query(c -> islands.findById(c, island.id()).orElseThrow(
                () -> new IslandException(IslandException.Reason.NO_ISLAND)))
                .thenCompose(current -> current.isReady()
                        ? CompletableFuture.completedFuture(current)
                        : builder.build(current.region()).thenCompose(ignored -> database.query(c -> {
                            islands.updateState(c, current.id(), IslandState.READY);
                            return current.withState(IslandState.READY);
                        })))
                .whenComplete((ready, error) -> {
                    if (error == null) {
                        byOwner.put(ready.owner(), ready);
                    }
                    generating.remove(island.id(), result);
                    if (error == null) {
                        result.complete(ready);
                    } else {
                        result.completeExceptionally(error);
                    }
                });
        return result;
    }

    private void notifyListeners(Consumer<IslandListener> action) {
        for (IslandListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (RuntimeException e) {
                // Um listener com defeito não pode derrubar a operação já gravada.
                LOGGER.log(Level.SEVERE, "IslandListener falhou", e);
            }
        }
    }

    private static <T> CompletableFuture<T> denied(IslandException.Reason reason) {
        return CompletableFuture.failedFuture(new IslandException(reason));
    }
}
