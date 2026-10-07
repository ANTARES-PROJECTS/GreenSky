package com.greencodes.greensky.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greencodes.greensky.island.Island;
import com.greencodes.greensky.island.IslandMember;
import com.greencodes.greensky.island.IslandPermission;
import com.greencodes.greensky.island.IslandRegion;
import com.greencodes.greensky.island.IslandRole;
import com.greencodes.greensky.island.IslandState;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IslandProtectionServiceTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();

    private Island aliceIsland;
    private Island carolIsland;
    private IslandProtectionService service;

    @BeforeEach
    void setUp() {
        aliceIsland = new Island(UUID.randomUUID(), alice, 1, new IslandRegion(1200, 0, 100), IslandState.READY);
        carolIsland = new Island(UUID.randomUUID(), carol, 2, new IslandRegion(1200, 1200, 100), IslandState.READY);
        service = new IslandProtectionService();
        service.load(List.of(aliceIsland, carolIsland));
        service.playerJoined(alice, List.of(owner(aliceIsland, alice)));
        service.playerJoined(carol, List.of(owner(carolIsland, carol)));
        service.playerJoined(bob, List.of()); // visitante, sem ilhas
    }

    private static IslandMember owner(Island island, UUID player) {
        return new IslandMember(island.id(), player, IslandRole.OWNER, EnumSet.noneOf(IslandPermission.class));
    }

    @Test
    void ownerCanDoEverythingOnOwnIsland() {
        for (IslandPermission p : IslandPermission.values()) {
            assertTrue(service.can(alice, p, 1200, 0), p.name());
        }
    }

    @Test
    void visitorIsDeniedEverythingOnForeignIsland() {
        for (IslandPermission p : IslandPermission.values()) {
            assertFalse(service.can(bob, p, 1200, 0), p.name());
        }
        assertFalse(service.isMemberAt(bob, 1200, 0));
    }

    @Test
    void ownerCannotTouchAnotherPlayersIsland() {
        assertFalse(service.can(alice, IslandPermission.BUILD, 1200, 1200));
        assertFalse(service.can(alice, IslandPermission.BREAK, 1200, 1200));
        assertFalse(service.isMemberAt(alice, 1200, 1200));
    }

    @Test
    void outsideAnyIslandNobodyCanActNotEvenOwners() {
        assertFalse(service.can(alice, IslandPermission.BUILD, 0, 0)); // spawn
        assertFalse(service.can(alice, IslandPermission.BUILD, 600, 0)); // vazio
        assertFalse(service.can(alice, IslandPermission.BUILD, 1250, 0)); // 1 bloco além da região
        assertTrue(service.can(alice, IslandPermission.BUILD, 1249, 49)); // último bloco dentro
        assertFalse(service.isMemberAt(alice, 600, 0));
    }

    @Test
    void memberHasOnlyGrantedPermissions() {
        service.onMemberAdded(new IslandMember(
                aliceIsland.id(), bob, IslandRole.MEMBER, EnumSet.of(IslandPermission.BUILD, IslandPermission.BREAK)));

        assertTrue(service.can(bob, IslandPermission.BUILD, 1200, 0));
        assertTrue(service.can(bob, IslandPermission.BREAK, 1200, 0));
        assertFalse(service.can(bob, IslandPermission.CONTAINERS, 1200, 0));
        assertFalse(service.can(bob, IslandPermission.MANAGE_PERMISSIONS, 1200, 0));
        assertTrue(service.isMemberAt(bob, 1200, 0));
        // E nada na ilha vizinha.
        assertFalse(service.can(bob, IslandPermission.BUILD, 1200, 1200));
    }

    @Test
    void permissionChangeAndRemovalTakeEffectImmediately() {
        service.onMemberAdded(new IslandMember(
                aliceIsland.id(), bob, IslandRole.MEMBER, IslandPermission.defaultsForMember()));
        assertTrue(service.can(bob, IslandPermission.CONTAINERS, 1200, 0));

        service.onMemberPermissionsChanged(new IslandMember(
                aliceIsland.id(), bob, IslandRole.MEMBER, EnumSet.of(IslandPermission.INTERACT)));
        assertFalse(service.can(bob, IslandPermission.CONTAINERS, 1200, 0));
        assertTrue(service.can(bob, IslandPermission.INTERACT, 1200, 0));

        service.onMemberRemoved(aliceIsland.id(), bob);
        assertFalse(service.can(bob, IslandPermission.INTERACT, 1200, 0));
        assertFalse(service.isMemberAt(bob, 1200, 0));
    }

    @Test
    void deniesEverythingUntilLoaded() {
        IslandProtectionService fresh = new IslandProtectionService();
        fresh.playerJoined(alice, List.of(owner(aliceIsland, alice)));
        assertFalse(fresh.isReady());
        assertFalse(fresh.can(alice, IslandPermission.BUILD, 1200, 0));
        assertFalse(fresh.sameIsland(1200, 0, 1201, 0));

        fresh.load(List.of(aliceIsland));
        assertTrue(fresh.can(alice, IslandPermission.BUILD, 1200, 0));
    }

    @Test
    void playerWhoseMembershipsAreNotLoadedYetIsDenied() {
        UUID dave = UUID.randomUUID(); // nunca chamou playerJoined
        assertFalse(service.can(dave, IslandPermission.BUILD, 1200, 0));
        service.playerQuit(alice);
        assertFalse(service.can(alice, IslandPermission.BUILD, 1200, 0));
    }

    @Test
    void newIslandIsProtectedAsSoonAsItIsCreated() {
        Island fresh = new Island(UUID.randomUUID(), bob, 3, new IslandRegion(-1200, 0, 100), IslandState.PENDING);
        assertFalse(service.can(bob, IslandPermission.BUILD, -1200, 0));
        service.onIslandCreated(fresh, owner(fresh, bob));
        assertTrue(service.can(bob, IslandPermission.BUILD, -1200, 0));
        assertFalse(service.can(alice, IslandPermission.BUILD, -1200, 0));
    }

    @Test
    void sameIslandOnlyWhenBothPointsShareAnIsland() {
        assertTrue(service.sameIsland(1200, 0, 1210, 20));
        assertFalse(service.sameIsland(1200, 0, 1200, 1200)); // ilhas diferentes
        assertFalse(service.sameIsland(1200, 0, 1250, 0)); // um ponto fora
        assertFalse(service.sameIsland(0, 0, 1, 1)); // ambos fora de qualquer ilha
    }
}
