package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightCityServicesGameTests {
    @GameTest(template = "sandevistan_test", batch = "city_guard_reachable", timeoutTicks = 180)
    public static void cityGuardSkipsAnUnreachableZombieAndAutomaticallyKillsAReachableCreeper(GameTestHelper h) {
        var city = new City(h);
        var guard = (MaxTecGuardEntity) city.role(NightCityResidents.Role.MAXTEC);
        guard.setNoAi(false); guard.setPos(Vec3.atBottomCenterOf(city.center));
        var cage = city.center.offset(3, 0, 0);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y <= 3; y++)
            if (x != 0 || z != 0 || y == 3) h.getLevel().setBlock(cage.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        var trapped = EntityType.HUSK.create(h.getLevel()); trapped.setNoAi(true); trapped.setPos(Vec3.atBottomCenterOf(cage)); h.getLevel().addFreshEntity(trapped);
        var creeper = EntityType.CREEPER.create(h.getLevel()); creeper.setNoAi(true); creeper.setHealth(1);
        creeper.setPos(Vec3.atBottomCenterOf(city.center.offset(-5, 0, 0))); h.getLevel().addFreshEntity(creeper);
        h.runAfterDelay(130, () -> {
            try {
                h.assertTrue(!creeper.isAlive(), "Unreachable nearer mob prevented the guard from attacking a reachable creeper");
                h.assertTrue(trapped.getHealth() == trapped.getMaxHealth(), "Guard attacked through the cage");
            } finally { trapped.discard(); creeper.discard(); city.close(); }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "city_guard_contact", timeoutTicks = 220)
    public static void cityGuardHasNormalDialogueContactsAndCallsButRetaliatesAfterOnePunch(GameTestHelper h) {
        var city = new City(h);
        var guard = (MaxTecGuardEntity) city.role(NightCityResidents.Role.MAXTEC);
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        guard.setPos(Vec3.atBottomCenterOf(city.center)); player.setPos(guard.position().add(1.5, 0, 0));
        guard.raiseTrust(player, 300);
        var callId = UUID.randomUUID();
        h.assertTrue(guard.nameColorId(player).equals("aqua"), "Resident officer started with the squad's red name");
        guard.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu, "Resident officer could not be talked to");
        h.assertTrue(AbstractCityNpcEntity.archiveContacts(player).stream().anyMatch(contact -> contact.npcId().equals(guard.getUUID())), "Meeting an officer did not add a contact");
        player.closeContainer(); guard.setNoAi(false);
        de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player,
                new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(callId, guard.getUUID(), -1));
        h.runAfterDelay(100, () -> {
            h.assertTrue(guard.isOnCall(), "Resident officer did not answer the phone");
            de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player,
                    new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(callId, guard.getUUID(), 0));
            // Patrols can wander to the city edge during the call; keep both fighters inside the fixture.
            guard.getNavigation().stop();
            guard.setPos(Vec3.atBottomCenterOf(city.center));
            player.setPos(guard.position().add(1.2, 0, 0));
            guard.hurt(guard.damageSources().playerAttack(player), 1);
            h.assertTrue(guard.getTarget() == player && guard.isDefendingAgainst(player) && guard.getHealth() > guard.getMaxHealth() * 0.25F,
                    "A single punch did not trigger immediate retaliation at healthy HP");
            h.assertTrue(guard.nameColorId(player).equals("red"), "Attacking an officer did not show their hostility to the attacker");
        });
        h.runAfterDelay(160, () -> {
            try { h.assertTrue(player.getHealth() < player.getMaxHealth(), "Officer targeted the attacker without actually fighting"); }
            finally {
                de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player,
                        new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(callId, guard.getUUID(), 0));
                player.closeContainer(); player.server.getPlayerList().remove(player); city.close();
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "city_guard_migration", timeoutTicks = 40)
    public static void legacyOfficersBecomeContactsWithoutChangingTheirUuidOrDuplicatingResidents(GameTestHelper h) {
        try (var city = new City(h)) {
            var current = city.role(NightCityResidents.Role.MAXTEC);
            var id = current.getUUID();
            var saved = new CompoundTag(); current.saveWithoutId(saved); current.discard();
            var legacy = ModEntityTypes.MAX_TAC.get().create(h.getLevel()); legacy.load(saved); legacy.setHealth(30);
            h.assertTrue(h.getLevel().addFreshEntity(legacy), "Could not install legacy officer fixture");
            h.assertTrue(NightCityResidents.migrateGuard(legacy), "Legacy officer was not upgraded");
            h.assertTrue(h.getLevel().getEntity(id) instanceof MaxTecGuardEntity upgraded && upgraded.getHealth() == 30 && upgraded.nameColorId().equals("aqua"),
                    "Officer migration lost its UUID, health or normal name color");
            city.data.maintain(h.getLevel());
            h.assertTrue(city.residents().size() == NightCityResidents.Role.values().length, "Migration duplicated city residents");
        }
        h.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "guard-contact"), false);
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    @GameTest(template = "sandevistan_test", batch = "city_population", timeoutTicks = 30)
    public static void everyCityRoleIsPresentAndSavedAssignmentsDoNotDuplicateUnloadedResidents(GameTestHelper h) {
        try (var city = new City(h)) {
            for (var role : NightCityResidents.Role.values())
                h.assertTrue(city.npcs.stream().filter(npc -> npc.getType() == role.type()).count() == 1, "Missing or duplicate role: " + role);
            city.data.maintain(h.getLevel());
            h.assertTrue(city.residents().size() == NightCityResidents.Role.values().length, "Repeated population check duplicated residents");
            var saved = city.data.save(new CompoundTag(), h.getLevel().registryAccess());
            var unloaded = city.role(NightCityResidents.Role.TECHIE);
            unloaded.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var restored = NightCityResidents.load(saved, h.getLevel().registryAccess());
            restored.maintain(h.getLevel());
            h.assertTrue(city.residents().stream().noneMatch(npc -> npc.getType() == ModEntityTypes.TECHIE.get()), "Reload replaced an unloaded living resident");
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "city_replacement", timeoutTicks = 660)
    public static void confirmedDeathsAreReplacedWithoutTurningUnloadsIntoDeaths(GameTestHelper h) {
        var city = new City(h);
        var fixer = city.role(NightCityResidents.Role.FIXER);
        fixer.setHealth(0); fixer.die(fixer.damageSources().genericKill());
        h.runAfterDelay(625, () -> {
            try {
                city.data.maintain(h.getLevel());
                var replacements = city.residents().stream().filter(npc -> npc.getType() == ModEntityTypes.FIXER.get() && npc.isAlive()).toList();
                h.assertTrue(replacements.size() == 1 && !replacements.getFirst().getUUID().equals(fixer.getUUID()), "Confirmed resident death did not produce exactly one replacement");
            } finally { city.close(); }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "city_guard", timeoutTicks = 160)
    public static void cityMaxtecAttacksHostilesButLeavesCiviliansAnimalsAndOutsideMobsAlone(GameTestHelper h) {
        var city = new City(h);
        var guard = (MaxTecGuardEntity) city.role(NightCityResidents.Role.MAXTEC);
        guard.setNoAi(false); guard.setPos(Vec3.atBottomCenterOf(city.center));
        var hostile = EntityType.HUSK.create(h.getLevel()); hostile.setNoAi(true); hostile.setHealth(1);
        hostile.setPos(guard.position().add(1.5, 0, 0)); h.getLevel().addFreshEntity(hostile);
        var outside = EntityType.HUSK.create(h.getLevel()); outside.setPos(guard.position().add(30, 0, 0));
        var cow = EntityType.COW.create(h.getLevel()); cow.setPos(guard.position().add(2, 0, 0)); cow.setNoAi(true); h.getLevel().addFreshEntity(cow);
        h.assertTrue(guard.canAttack(hostile) && !guard.canAttack(outside) && !guard.canAttack(cow)
                && !guard.canAttack(city.role(NightCityResidents.Role.MERC)), "City guard's target policy includes friendly or outside entities");
        h.runAfterDelay(120, () -> {
            try {
                h.assertTrue(!hostile.isAlive(), "City guard selected no hostile or never attacked it");
                h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "City guard damaged a neutral animal");
                h.assertTrue(city.npcs.stream().filter(npc -> npc != guard).allMatch(npc -> npc.getHealth() == npc.getMaxHealth()), "Guard damaged a city resident");
            } finally { hostile.discard(); cow.discard(); city.close(); }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "city_medical", timeoutTicks = 400)
    public static void medtecHealsAndRevivesTheSameNpcWithoutMarkingTheirIdentityDeceased(GameTestHelper h) {
        var city = new City(h);
        var medic = (MedTechEntity) city.role(NightCityResidents.Role.MEDTEC);
        medic.setNoAi(false); medic.setPos(Vec3.atBottomCenterOf(city.center));
        var patient = (AbstractCityNpcEntity) city.role(NightCityResidents.Role.TECHIE);
        patient.setPos(medic.position().add(1.5, 0, 0));
        var id = patient.getUUID(); patient.setHealth(patient.getMaxHealth() - 8);
        h.runAfterDelay(80, () -> {
            h.assertTrue(patient.getHealth() > patient.getMaxHealth() - 8, "MedTec did not heal an injured NPC");
            patient.setHealth(0); patient.die(patient.damageSources().generic());
            h.assertTrue(patient.isAlive() && CityNpcMedicine.downed(patient), "NPC was killed instead of becoming revivable");
            h.assertTrue(!FixerIdentitySavedData.get(h.getLevel()).isDeceased(id), "Downed resident's identity was marked deceased");
        });
        h.runAfterDelay(210, () -> {
            h.assertTrue(patient.getUUID().equals(id) && patient.isAlive() && !CityNpcMedicine.downed(patient) && patient.getHealth() > 1,
                    "MedTec did not revive the original NPC");
            h.assertTrue(patient.isNoAi(), "Revival overwrote the NPC's previous AI setting");
            h.assertTrue(!FixerIdentitySavedData.get(h.getLevel()).isDeceased(id), "Revival changed the contact's identity state");
            patient.setHealth(patient.getMaxHealth());
            var guard = city.role(NightCityResidents.Role.MAXTEC);
            guard.setPos(medic.position().add(-1.5, 0, 0));
            guard.setHealth(0); guard.die(guard.damageSources().generic());
            h.assertTrue(guard.isAlive() && CityNpcMedicine.downed(guard), "City MaxTec could not enter the recoverable downed state");
        });
        h.runAfterDelay(350, () -> {
            try {
                var guard = city.role(NightCityResidents.Role.MAXTEC);
                h.assertTrue(guard.isAlive() && !CityNpcMedicine.downed(guard) && guard.getHealth() > 1, "MedTec did not revive the city MaxTec guard");
            } finally { city.close(); }
            h.succeed();
        });
    }

    private static final class City implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos center;
        final BoundingBox bounds;
        final NightCityResidents data;
        final List<Mob> npcs;
        final List<ChunkPos> forced = new ArrayList<>();
        City(GameTestHelper h) {
            this.h = h; center = h.absolutePos(new BlockPos(2, 5, 2));
            bounds = new BoundingBox(center.getX() - 12, center.getY(), center.getZ() - 12, center.getX() + 12, center.getY() + 4, center.getZ() + 12);
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = new ChunkPos(x, z);
                if (!h.getLevel().getForcedChunks().contains(chunk.toLong())) { h.getLevel().setChunkForced(x, z, true); forced.add(chunk); }
            }
            for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) {
                h.getLevel().setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 4; y++) h.getLevel().setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            data = NightCityResidents.get(h.getLevel()); data.register(bounds); data.maintain(h.getLevel());
            npcs = residents(); npcs.forEach(npc -> npc.setNoAi(true));
        }
        List<Mob> residents() { return h.getLevel().getEntitiesOfClass(Mob.class, NightCityResidents.area(bounds), NightCityResidents::assigned); }
        Mob role(NightCityResidents.Role role) { return npcs.stream().filter(npc -> npc.getType() == role.type()).findFirst().orElseThrow(); }
        public void close() {
            data.forget(bounds); residents().forEach(Entity::discard); npcs.forEach(Entity::discard);
            for (var chunk : forced) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
        }
    }
    private NightCityServicesGameTests() { }
}
