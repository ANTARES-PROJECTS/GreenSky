package com.greencodes.greensky.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.core.config.DatabaseSettings;
import com.greencodes.greensky.core.config.ExpansionSettings;
import com.greencodes.greensky.core.config.IslandSettings;
import com.greencodes.greensky.database.Database;
import com.greencodes.greensky.player.PlayerRepository;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Serviço de ilhas contra PostgreSQL real, com um builder falso (sem mundo Bukkit). */
@EnabledIfEnvironmentVariable(named = DatabaseSettings.PASSWORD_ENV, matches = ".+")
class IslandServiceIT {

    @Test
    void concurrentRecoverySharesOneBuildAndStalePendingObjectDoesNotRebuild() throws Exception {
        UUID owner = player();
        Island ready = service(new FakeBuilder(0)).create(owner, "rec" + owner.toString().substring(0, 8))
                .get(10, TimeUnit.SECONDS);
        IslandRepository repository = new IslandRepository();
        db.query(c -> { repository.updateState(c, ready.id(), IslandState.PENDING); return null; })
                .get(10, TimeUnit.SECONDS);
        Island pending = ready.withState(IslandState.PENDING);
        AtomicInteger builds = new AtomicInteger();
        CompletableFuture<Void> build = new CompletableFuture<>();
        CompletableFuture<Void> started = new CompletableFuture<>();
        IslandService recovering = new IslandService(db, repository, new PlayerRepository(), region -> {
            builds.incrementAndGet();
            started.complete(null);
            return build;
        }, SETTINGS, EXPANSION, IslandVisibility.PUBLIC);
        CompletableFuture<Island> first = recovering.ensureReady(pending);
        started.get(10, TimeUnit.SECONDS);
        List<CompletableFuture<Island>> callers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            callers.add(CompletableFuture.supplyAsync(() -> recovering.ensureReady(pending)).thenCompose(f -> f));
        }
        build.complete(null);
        assertTrue(first.get(10, TimeUnit.SECONDS).isReady());
        CompletableFuture.allOf(callers.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        assertTrue(recovering.ensureReady(pending).get(10, TimeUnit.SECONDS).isReady());
        assertEquals(1, builds.get());
    }

    private static final IslandSettings SETTINGS = new IslandSettings(100, 1000, 1200, 100, 100, 4);

    private static Database db;
    private final List<UUID> created = new ArrayList<>();

    @BeforeAll
    static void open() {
        DatabaseSettings settings = new DatabaseSettings("127.0.0.1", 5432, "greensky", "greensky", 8);
        db = Database.open(settings, System.getenv(DatabaseSettings.PASSWORD_ENV),
                IslandServiceIT.class.getClassLoader());
    }

    @AfterAll
    static void close() {
        db.close();
    }

    @AfterEach
    void cleanup() throws Exception {
        db.transaction(c -> {
            for (UUID id : created) {
                exec(c, "DELETE FROM islands WHERE owner_uuid = ?", id);
                exec(c, "DELETE FROM island_members WHERE player_uuid = ?", id);
                exec(c, "DELETE FROM players WHERE uuid = ?", id);
            }
            return null;
        }).get(10, TimeUnit.SECONDS);
        created.clear();
    }

    private static void exec(java.sql.Connection c, String sql, UUID id) throws java.sql.SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    private UUID player() {
        UUID id = UUID.randomUUID();
        created.add(id);
        return id;
    }

    private static final ExpansionSettings EXPANSION =
            new ExpansionSettings(List.of(100, 150, 200, 300, 500)).validateAgainst(SETTINGS);

    private static IslandService service(FakeBuilder builder) {
        return new IslandService(
                db, new IslandRepository(), new PlayerRepository(), builder, SETTINGS, EXPANSION, IslandVisibility.PUBLIC);
    }

    @Test
    void visitRulesFollowVisibilityAndMembership() throws Exception {
        UUID owner = player();
        UUID friend = player();
        UUID stranger = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "host").get(10, TimeUnit.SECONDS);
        service.addMember(island, owner, friend, "friend5").get(10, TimeUnit.SECONDS);

        // Pública por padrão: qualquer um visita.
        assertEquals(IslandVisibility.PUBLIC, service.visibility(island).get(10, TimeUnit.SECONDS));
        assertEquals(island.id(), service.authorizeVisit(stranger, owner).get(10, TimeUnit.SECONDS).id());

        // Privada: só membros (e o dono).
        service.setVisibility(island, owner, IslandVisibility.PRIVATE).get(10, TimeUnit.SECONDS);
        assertReason(IslandException.Reason.ISLAND_PRIVATE, service.authorizeVisit(stranger, owner));
        assertEquals(island.id(), service.authorizeVisit(friend, owner).get(10, TimeUnit.SECONDS).id());
        assertEquals(island.id(), service.authorizeVisit(owner, owner).get(10, TimeUnit.SECONDS).id());

        // Persistiu: outra instância (sem cache) lê do banco.
        IslandService fresh = service(new FakeBuilder(0));
        Island loaded = fresh.findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(IslandVisibility.PRIVATE, fresh.visibility(loaded).get(10, TimeUnit.SECONDS));

        // Só o dono troca.
        assertReason(IslandException.Reason.NOT_OWNER, service.setVisibility(island, friend, IslandVisibility.PUBLIC));
        // Quem não tem ilha não pode ser visitado.
        assertReason(IslandException.Reason.TARGET_HAS_NO_ISLAND, service.authorizeVisit(owner, stranger));
    }

    @Test
    void visibilityChangeNotifiesListeners() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        List<IslandVisibility> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(new IslandListener() {
            @Override
            public void onVisibilityChanged(Island island, IslandVisibility visibility) {
                seen.add(visibility);
            }
        });
        Island island = service.create(owner, "notify2").get(10, TimeUnit.SECONDS);
        service.setVisibility(island, owner, IslandVisibility.PRIVATE).get(10, TimeUnit.SECONDS);
        service.setVisibility(island, owner, IslandVisibility.PUBLIC).get(10, TimeUnit.SECONDS);
        assertEquals(List.of(IslandVisibility.PRIVATE, IslandVisibility.PUBLIC), seen);
    }

    @Test
    void newIslandsUseTheConfiguredDefaultVisibility() throws Exception {
        UUID owner = player();
        IslandService privateByDefault = new IslandService(db, new IslandRepository(), new PlayerRepository(),
                new FakeBuilder(0), SETTINGS, EXPANSION, IslandVisibility.PRIVATE);
        Island island = privateByDefault.create(owner, "shy").get(10, TimeUnit.SECONDS);
        // Instância nova com padrão PUBLIC: o valor gravado na criação (PRIVATE) é o que vale.
        assertEquals(IslandVisibility.PRIVATE, service(new FakeBuilder(0)).visibility(island).get(10, TimeUnit.SECONDS));
    }

    @Test
    void expandsThroughAllLevelsKeepingTheCenterAndStopsAtMax() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "grower").get(10, TimeUnit.SECONDS);
        List<String> expanded = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(new IslandListener() {
            @Override
            public void onIslandExpanded(Island i) {
                expanded.add(i.region().size() + "");
            }
        });

        for (int size : new int[] {150, 200, 300, 500}) {
            island = service.expand(island).get(10, TimeUnit.SECONDS);
            assertEquals(size, island.region().size());
        }
        assertEquals(List.of("150", "200", "300", "500"), expanded);
        assertEquals(5, service.levelOf(island));
        assertReason(IslandException.Reason.MAX_SIZE_REACHED, service.expand(island));

        // Persistiu; o centro nunca mudou (a ilha não se move).
        Island loaded = service(new FakeBuilder(0)).findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(500, loaded.region().size());
        assertEquals(island.region().centerX(), loaded.region().centerX());
        assertEquals(island.region().centerZ(), loaded.region().centerZ());
    }

    @Test
    void simultaneousExpansionsOfTheSameIslandApplyOnlyOnce() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "racer2").get(10, TimeUnit.SECONDS);

        List<CompletableFuture<Island>> attempts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            attempts.add(service.expand(island)); // todos partem do mesmo tamanho (100)
        }
        int ok = 0;
        int conflicts = 0;
        for (CompletableFuture<Island> attempt : attempts) {
            try {
                assertEquals(150, attempt.get(10, TimeUnit.SECONDS).region().size());
                ok++;
            } catch (ExecutionException e) {
                assertEquals(IslandException.Reason.EXPANSION_CONFLICT, ((IslandException) root(e)).reason());
                conflicts++;
            }
        }
        assertEquals(1, ok);
        assertEquals(7, conflicts);
        Island loaded = service(new FakeBuilder(0)).findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(150, loaded.region().size(), "só um nível deve ter sido aplicado");
    }

    @Test
    void staleIslandObjectCannotSkipALevel() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        Island old = service.create(owner, "stale").get(10, TimeUnit.SECONDS);
        service.expand(old).get(10, TimeUnit.SECONDS); // 100 -> 150
        // Usar de novo o objeto antigo (tamanho 100) não pode aplicar 150 por cima de 150.
        assertReason(IslandException.Reason.EXPANSION_CONFLICT, service.expand(old));
        Island fresh = service.findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(150, fresh.region().size());
        assertEquals(200, service.expand(fresh).get(10, TimeUnit.SECONDS).region().size());
    }

    @Test
    void createsReadyIslandAndPersistsIt() throws Exception {
        UUID owner = player();
        FakeBuilder builder = new FakeBuilder(0);
        IslandService service = service(builder);

        Island island = service.create(owner, "owner1").get(10, TimeUnit.SECONDS);

        assertTrue(island.isReady());
        assertEquals(1, builder.builds.get());
        assertEquals(100, island.region().size());

        // Outra instância do serviço (cache vazio) enxerga a ilha gravada no banco.
        Island loaded = service(new FakeBuilder(0)).findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(island.id(), loaded.id());
        assertEquals(island.region(), loaded.region());
        assertTrue(loaded.isReady());

        List<IslandMember> members = service.members(island.id()).get(10, TimeUnit.SECONDS);
        assertEquals(1, members.size());
        assertEquals(IslandRole.OWNER, members.get(0).role());
        assertTrue(members.get(0).can(IslandPermission.MANAGE_PERMISSIONS));
    }

    @Test
    void secondIslandForSameOwnerIsRejected() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        service.create(owner, "owner2").get(10, TimeUnit.SECONDS);

        assertReason(IslandException.Reason.ALREADY_HAS_ISLAND, service.create(owner, "owner2"));
    }

    @Test
    void simultaneousCreationsForSameOwnerYieldExactlyOneIsland() throws Exception {
        UUID owner = player();
        FakeBuilder builder = new FakeBuilder(0);
        IslandService service = service(builder);

        List<CompletableFuture<Island>> attempts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            attempts.add(service.create(owner, "racer"));
        }
        int ok = 0;
        int rejected = 0;
        for (CompletableFuture<Island> attempt : attempts) {
            try {
                attempt.get(20, TimeUnit.SECONDS);
                ok++;
            } catch (ExecutionException e) {
                assertEquals(IslandException.Reason.ALREADY_HAS_ISLAND, ((IslandException) root(e)).reason());
                rejected++;
            }
        }
        assertEquals(1, ok);
        assertEquals(7, rejected);
        assertEquals(1, builder.builds.get(), "blocos só podem ser gerados para a ilha que existe");
    }

    @Test
    void simultaneousCreationsForDifferentPlayersGetDistinctNonOverlappingSlots() throws Exception {
        IslandService service = service(new FakeBuilder(0));
        List<CompletableFuture<Island>> all = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            all.add(service.create(player(), "p" + i));
        }
        List<Island> islands = new ArrayList<>();
        for (CompletableFuture<Island> f : all) {
            islands.add(f.get(30, TimeUnit.SECONDS));
        }
        Set<Long> slots = new HashSet<>();
        for (Island island : islands) {
            assertTrue(slots.add(island.slot()), "slot repetido: " + island.slot());
        }
        // Mesmo no tamanho máximo, nenhuma região se sobrepõe a outra.
        for (int i = 0; i < islands.size(); i++) {
            for (int j = i + 1; j < islands.size(); j++) {
                IslandRegion a = islands.get(i).region().withSize(SETTINGS.maxSize());
                IslandRegion b = islands.get(j).region().withSize(SETTINGS.maxSize());
                assertFalse(a.overlaps(b));
            }
        }
    }

    @Test
    void crashedCreationStaysPendingAndIsRecovered() throws Exception {
        UUID owner = player();
        FakeBuilder builder = new FakeBuilder(1); // falha na primeira geração
        IslandService service = service(builder);

        CompletableFuture<Island> failing = service.create(owner, "crash");
        assertThrows(ExecutionException.class, () -> failing.get(10, TimeUnit.SECONDS));

        // A linha existe, mas ainda não está pronta.
        IslandService fresh = service(builder);
        Island pending = fresh.findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(IslandState.PENDING, pending.state());

        // Recuperação: refaz os blocos e marca READY, inclusive no banco.
        Island recovered = fresh.ensureReady(pending).get(10, TimeUnit.SECONDS);
        assertTrue(recovered.isReady());
        Island persisted = service(builder).findByOwner(owner).get(10, TimeUnit.SECONDS).orElseThrow();
        assertEquals(IslandState.READY, persisted.state());
        assertEquals(pending.region(), persisted.region());
    }

    @Test
    void ownerManagesMembersAndPermissions() throws Exception {
        UUID owner = player();
        UUID friend = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "boss").get(10, TimeUnit.SECONDS);

        IslandMember added = service.addMember(island, owner, friend, "friend").get(10, TimeUnit.SECONDS);
        assertEquals(IslandPermission.defaultsForMember(), added.permissions());
        assertTrue(added.can(IslandPermission.BUILD));
        assertFalse(added.can(IslandPermission.KICK));

        assertReason(IslandException.Reason.ALREADY_MEMBER, service.addMember(island, owner, friend, "friend"));

        service.setPermissions(island, owner, friend, EnumSet.of(IslandPermission.INTERACT))
                .get(10, TimeUnit.SECONDS);
        IslandMember updated = service.members(island.id()).get(10, TimeUnit.SECONDS).stream()
                .filter(m -> m.playerId().equals(friend))
                .findFirst()
                .orElseThrow();
        assertEquals(EnumSet.of(IslandPermission.INTERACT), updated.permissions());
        assertFalse(updated.can(IslandPermission.BUILD));

        service.removeMember(island, owner, friend).get(10, TimeUnit.SECONDS);
        assertEquals(1, service.members(island.id()).get(10, TimeUnit.SECONDS).size());
        assertReason(IslandException.Reason.NOT_A_MEMBER, service.removeMember(island, owner, friend));
    }

    @Test
    void authorizationRules() throws Exception {
        UUID owner = player();
        UUID friend = player();
        UUID stranger = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "boss2").get(10, TimeUnit.SECONDS);
        service.addMember(island, owner, friend, "friend2").get(10, TimeUnit.SECONDS);

        assertReason(IslandException.Reason.NOT_OWNER, service.addMember(island, friend, stranger, "stranger"));
        assertReason(IslandException.Reason.NOT_OWNER, service.removeMember(island, stranger, friend));
        assertReason(IslandException.Reason.NOT_OWNER,
                service.setPermissions(island, friend, friend, EnumSet.allOf(IslandPermission.class)));
        assertReason(IslandException.Reason.CANNOT_REMOVE_OWNER, service.removeMember(island, owner, owner));
        assertReason(IslandException.Reason.NOT_A_MEMBER,
                service.setPermissions(island, owner, owner, EnumSet.of(IslandPermission.BUILD)));

        // Um membro pode sair sozinho.
        service.removeMember(island, friend, friend).get(10, TimeUnit.SECONDS);
    }

    @Test
    void loadAllAndMembershipsReflectTheDatabase() throws Exception {
        UUID owner = player();
        UUID friend = player();
        IslandService service = service(new FakeBuilder(0));
        Island island = service.create(owner, "loader").get(10, TimeUnit.SECONDS);
        service.addMember(island, owner, friend, "friend3").get(10, TimeUnit.SECONDS);
        service.setPermissions(island, owner, friend, EnumSet.of(IslandPermission.BUILD, IslandPermission.KICK))
                .get(10, TimeUnit.SECONDS);

        assertTrue(service.loadAll().get(10, TimeUnit.SECONDS).stream().anyMatch(i -> i.id().equals(island.id())));

        List<IslandMember> ownerMemberships = service.membershipsOf(owner).get(10, TimeUnit.SECONDS);
        assertEquals(1, ownerMemberships.size());
        assertEquals(IslandRole.OWNER, ownerMemberships.get(0).role());

        List<IslandMember> friendMemberships = service.membershipsOf(friend).get(10, TimeUnit.SECONDS);
        assertEquals(1, friendMemberships.size());
        assertEquals(island.id(), friendMemberships.get(0).islandId());
        assertEquals(EnumSet.of(IslandPermission.BUILD, IslandPermission.KICK), friendMemberships.get(0).permissions());

        assertTrue(service.membershipsOf(player()).get(10, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void listenersAreNotifiedOnlyAfterSuccessfulChanges() throws Exception {
        UUID owner = player();
        UUID friend = player();
        List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        IslandService service = service(new FakeBuilder(0));
        service.addListener(new IslandListener() {
            @Override
            public void onIslandCreated(Island island, IslandMember member) {
                events.add("created");
            }

            @Override
            public void onMemberAdded(IslandMember member) {
                events.add("added");
            }

            @Override
            public void onMemberRemoved(UUID islandId, UUID playerId) {
                events.add("removed");
            }

            @Override
            public void onMemberPermissionsChanged(IslandMember member) {
                events.add("perms");
            }
        });

        Island island = service.create(owner, "notify").get(10, TimeUnit.SECONDS);
        service.addMember(island, owner, friend, "friend4").get(10, TimeUnit.SECONDS);
        service.setPermissions(island, owner, friend, EnumSet.of(IslandPermission.BUILD)).get(10, TimeUnit.SECONDS);
        service.removeMember(island, owner, friend).get(10, TimeUnit.SECONDS);
        assertEquals(List.of("created", "added", "perms", "removed"), events);

        // Operações recusadas não notificam nada.
        events.clear();
        assertReason(IslandException.Reason.NOT_OWNER, service.addMember(island, friend, friend, "x"));
        assertReason(IslandException.Reason.NOT_A_MEMBER, service.removeMember(island, owner, friend));
        assertReason(IslandException.Reason.ALREADY_HAS_ISLAND, service.create(owner, "notify"));
        assertTrue(events.isEmpty(), "eventos indevidos: " + events);
    }

    @Test
    void aFailingListenerDoesNotBreakTheOperation() throws Exception {
        UUID owner = player();
        IslandService service = service(new FakeBuilder(0));
        service.addListener(new IslandListener() {
            @Override
            public void onIslandCreated(Island island, IslandMember member) {
                throw new IllegalStateException("listener com defeito");
            }
        });
        Island island = service.create(owner, "fragile").get(10, TimeUnit.SECONDS);
        assertTrue(island.isReady());
    }

    private static void assertReason(IslandException.Reason expected, CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        Throwable root = root(e);
        assertTrue(root instanceof IslandException, "esperava IslandException, veio: " + root);
        assertEquals(expected, ((IslandException) root).reason());
    }

    private static Throwable root(Throwable t) {
        while ((t instanceof ExecutionException || t instanceof CompletionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** Builder falso: conta chamadas e pode falhar as N primeiras. */
    private static final class FakeBuilder implements IslandBuilder {
        final AtomicInteger builds = new AtomicInteger();
        private final AtomicInteger failuresLeft;

        FakeBuilder(int failures) {
            this.failuresLeft = new AtomicInteger(failures);
        }

        @Override
        public CompletableFuture<Void> build(IslandRegion region) {
            if (failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                return CompletableFuture.failedFuture(new IllegalStateException("crash simulado na geração"));
            }
            builds.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }
}
