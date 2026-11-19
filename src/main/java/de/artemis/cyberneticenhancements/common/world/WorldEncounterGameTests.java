package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity.Role;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.PatientSearch;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestStatus;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldEncounterGameTests {
    @GameTest(template = "sandevistan_test", batch = "world_encounters", timeoutTicks = 100)
    public static void encounterPeopleCanBeCreatedAndTakeTheirRole(GameTestHelper h) {
        for (var role : Role.values()) {
            var npc = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
            h.assertTrue(npc != null, "A world encounter NPC could not be created");
            npc.configure(role, BlockPos.ZERO);
            h.assertTrue(npc.role() == role, "The NPC lost its role " + role);
            h.assertTrue(npc.canInteract(null) == (role == Role.LEADER), "Only the leader may be talked to");
        }
        h.assertTrue(WorldEncounters.slots(BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE).size() == 3, "A world encounter needs three places to stand");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounters", timeoutTicks = 100)
    public static void theMissingPatientIsPlacedOnOpenGroundWithinTheMarkedArea(GameTestHelper h) {
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(2, 1, 2));
        var contract = new de.artemis.cyberneticenhancements.common.quest.QuestContract("patient-test", java.util.UUID.randomUUID(), "MedTec", "world_med_tech",
                QuestType.LOCATE, 1, de.artemis.cyberneticenhancements.common.quest.QuestStatus.ACTIVE, 0, 1, 0, false,
                level.dimension().location().toString(), center, de.artemis.cyberneticenhancements.common.quest.PatientSearch.AREA_RADIUS, 100, 1, 0, 0, 0, 0);
        var column = de.artemis.cyberneticenhancements.common.quest.PatientSearch.column(contract);
        level.getChunk(column);
        var patient = de.artemis.cyberneticenhancements.common.quest.PatientSearch.spawn(level, contract, column);
        h.assertTrue(patient != null && patient.isAlive(), "The patient could not be placed");
        h.assertTrue(patient.role() == Role.PATIENT && level.getEntity(contract.patientId()) == patient, "The patient must be findable by the contract's id");
        double dx = patient.getX() - center.getX(), dz = patient.getZ() - center.getZ();
        h.assertTrue(Math.sqrt(dx * dx + dz * dz) < de.artemis.cyberneticenhancements.common.quest.PatientSearch.AREA_RADIUS, "The patient lies outside the marked area");
        patient.discard();
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounter_board", timeoutTicks = 1600)
    public static void leaderOffersOnlyTheLocateQuestAndNoShop(GameTestHelper h) {
        var player = player(h);
        var leader = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
        leader.configure(Role.LEADER, BlockPos.ZERO);
        leader.setPos(player.position());
        h.getLevel().addFreshEntity(leader);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(
                        !PlayerQuestManager.snapshotForIssuer(player, leader).offers().isEmpty(), "The leader's request is still being prepared"))
                .thenExecute(() -> {
                    var offers = PlayerQuestManager.snapshotForIssuer(player, leader).offers();
                    h.assertTrue(offers.size() == 1 && offers.getFirst().type() == QuestType.LOCATE, "The leader should offer exactly the locate quest, got " + offers);
                    h.assertTrue(!leader.npcType().supportsShop() && !leader.npcType().supportsMedicalServices(), "The leader must not sell or treat");
                    leader.discard();
                    player.server.getPlayerList().remove(player);
                })
                .thenSucceed();
    }

    private static WorldEncounterNpcEntity seedPatientQuest(GameTestHelper h, net.minecraft.server.level.ServerPlayer player) {
        var leader = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
        leader.configure(Role.LEADER, BlockPos.ZERO);
        leader.setPos(player.position());
        h.getLevel().addFreshEntity(leader);
        // The offer is seeded rather than generated: generation waits on terrain preparation, which is not what these tests are about.
        var offer = new QuestContract(java.util.UUID.randomUUID().toString(), leader.getUUID(), leader.getName().getString(), leader.npcTypeId(),
                QuestType.LOCATE, 1, QuestStatus.OFFERED, 0, 1, 0, false, h.getLevel().dimension().location().toString(),
                h.absolutePos(new BlockPos(2, 1, 2)).offset(0, 0, 40000 + new java.util.Random().nextInt(20000)), PatientSearch.AREA_RADIUS, 500, 1, QuestContract.REWARD_NONE, 0, h.getLevel().getDayTime() / 24000L, -1);
        var root = player.getPersistentData().getCompound("cyberneticenhancements.quest_state");
        var boards = root.getCompound("IssuerBoards");
        var board = new net.minecraft.nbt.CompoundTag();
        var list = new net.minecraft.nbt.ListTag();
        list.add(offer.toTag());
        board.put("Offers", list);
        board.putLong("RefreshDay", h.getLevel().getDayTime() / 24000L);
        boards.put(leader.getStringUUID(), board);
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
        return leader;
    }

    private static WorldEncounterNpcEntity spawnPatient(GameTestHelper h, net.minecraft.server.level.ServerPlayer player, QuestContract contract) {
        var patient = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
        patient.setUUID(contract.patientId());
        patient.configure(Role.PATIENT, BlockPos.ZERO);
        patient.setPos(player.position());
        h.getLevel().addFreshEntity(patient);
        contract.setPatientPos(patient.blockPosition());
        PlayerQuestManager.storeActiveContract(player, contract);
        return patient;
    }

    private static net.minecraft.world.item.ItemStack held(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().items.stream().filter(s -> s.is(item)).findFirst().orElseThrow();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounter_patient_quest", timeoutTicks = 1600)
    public static void thePatientQuestRunsFromInjectorToBeaconToPayoutAndItsContactEndsWithIt(GameTestHelper h) {
        var player = player(h);
        var leader = seedPatientQuest(h, player);
        var injector = de.artemis.cyberneticenhancements.common.registry.ModItems.REVIVE_INJECTOR.get();
        var signal = de.artemis.cyberneticenhancements.common.registry.ModItems.SIGNAL_BEACON.get();
        h.startSequence().thenExecute(() -> {
                    h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, leader, 0) == PlayerQuestManager.Result.OK, "The patient quest could not be accepted");
                    var inv = player.getInventory();
                    h.assertTrue(inv.countItem(injector) == 1 && inv.countItem(signal) == 1, "Both quest items must be handed out at the start");
                    h.assertTrue(de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.archiveContacts(player).stream().anyMatch(c -> c.npcId().equals(leader.getUUID())),
                            "The questgiver did not become a temporary contact");
                    var contract = PlayerQuestManager.activeContracts(player).getFirst();
                    h.assertTrue(contract.type() == QuestType.LOCATE && contract.urgentLimitMinutes() == 20, "The patient quest must run on a twenty minute clock");
                    var patient = spawnPatient(h, player, contract);
                    h.assertTrue(patient.isSitting(), "The patient must be sitting");
                    h.assertTrue(PlayerQuestManager.usePatientItem(player, patient, held(player, injector)), "The injector did nothing on the patient");
                    h.assertTrue(inv.countItem(injector) == 0, "The injector must be used up");
                    h.assertTrue(PlayerQuestManager.useSignalBeacon(player, held(player, signal)), "The beacon did nothing");
                    h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().isReady(), "Logging the position must ready the quest");
                    var before = de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player);
                    h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, leader) == PlayerQuestManager.TurnInResult.OK, "The beacon could not be handed in");
                    h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) > before, "The quest paid nothing");
                    h.assertTrue(de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.archiveContacts(player).stream().noneMatch(c -> c.npcId().equals(leader.getUUID())),
                            "The temporary contact must end with the quest");
                    patient.discard();
                    leader.discard();
                    player.server.getPlayerList().remove(player);
                })
                .thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounter_patient_quest", timeoutTicks = 1600)
    public static void aBeaconLoggedFarFromThePatientPaysNothingAndAngersTheQuestgiver(GameTestHelper h) {
        var player = player(h);
        var leader = seedPatientQuest(h, player);
        var injector = de.artemis.cyberneticenhancements.common.registry.ModItems.REVIVE_INJECTOR.get();
        var signal = de.artemis.cyberneticenhancements.common.registry.ModItems.SIGNAL_BEACON.get();
        h.startSequence().thenExecute(() -> {
                    h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, leader, 0) == PlayerQuestManager.Result.OK, "The patient quest could not be accepted");
                    var contract = PlayerQuestManager.activeContracts(player).getFirst();
                    var patient = spawnPatient(h, player, contract);
                    h.assertTrue(PlayerQuestManager.usePatientItem(player, patient, held(player, injector)), "The injector did nothing on the patient");
                    // The patient is somewhere else entirely from where the beacon is used.
                    contract = PlayerQuestManager.activeContracts(player).getFirst();
                    contract.setPatientPos(player.blockPosition().offset(30, 0, 0));
                    PlayerQuestManager.storeActiveContract(player, contract);
                    h.assertTrue(PlayerQuestManager.useSignalBeacon(player, held(player, signal)), "The beacon did nothing");
                    var before = de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player);
                    h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, leader) == PlayerQuestManager.TurnInResult.REJECTED, "A false report must be rejected");
                    h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == before, "A false report must not be paid");
                    h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty(), "The rejected quest must be over");
                    patient.discard();
                    leader.discard();
                    player.server.getPlayerList().remove(player);
                })
                .thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounters", timeoutTicks = 400)
    public static void aMedTecGuardWalksBackToItsPostAndFacesTheWayItDidOnceTheFightIsOver(GameTestHelper h) {
        var level = h.getLevel();
        var post = h.absolutePos(new BlockPos(2, 1, 2));
        var npc = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(level);
        npc.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 180, 0);
        npc.configure(Role.BYSTANDER, post);
        level.addFreshEntity(npc);
        var home = npc.position();
        // As if it had chased someone off and the fight had ended.
        npc.teleportTo(home.x + 5, home.y, home.z + 3);
        npc.setYRot(40);
        npc.setYHeadRot(40);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(npc.position().distanceToSqr(home) < 0.5, "The guard did not walk back to where it was posted");
            float off = Math.abs(net.minecraft.util.Mth.wrapDegrees(npc.getYHeadRot() - 180));
            h.assertTrue(off < 10, "The guard did not turn back to its original heading, it faces " + npc.getYHeadRot());
        }).thenExecute(npc::discard).thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounters", timeoutTicks = 200)
    public static void aSeatedGuardSitsUntilTheSiteIsRobbedThenStandsAndFights(GameTestHelper h) {
        var player = player(h);
        var seat = h.absolutePos(new BlockPos(2, 1, 2));
        var npc = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
        npc.moveTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, 270, 0);
        npc.configure(Role.SEATED, seat);
        h.getLevel().addFreshEntity(npc);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(seat.getX() + 6.5, seat.getY(), seat.getZ() + 0.5);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(npc.isSitting(), "An undisturbed seated guard must be sitting"))
                .thenExecute(() -> npc.provoke(player))
                .thenWaitUntil(() -> {
                    h.assertTrue(!npc.isSitting(), "A guard that has been provoked must get up");
                    h.assertTrue(npc.getTarget() == player, "A provoked guard must go for the player");
                })
                .thenExecute(() -> { npc.discard(); player.server.getPlayerList().remove(player); })
                .thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "world_encounter_board", timeoutTicks = 1600)
    public static void strikingTheEncounterOrBreakingItsBuildWithdrawsTheQuest(GameTestHelper h) {
        var player = player(h);
        var leader = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(h.getLevel());
        leader.configure(Role.LEADER, h.absolutePos(new BlockPos(2, 1, 2)));
        leader.setPos(player.position());
        h.getLevel().addFreshEntity(leader);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(
                        !PlayerQuestManager.snapshotForIssuer(player, leader).offers().isEmpty(), "The leader's request is still being prepared"))
                .thenExecute(() -> {
                    h.assertTrue(!WorldEncounterOffenses.refuses(player, leader), "A newcomer must not be refused");
                    WorldEncounterOffenses.record(player, h.absolutePos(new BlockPos(1, 1, 1)));
                    h.assertTrue(WorldEncounterOffenses.refuses(player, leader), "Wronging the site must be held against the player");
                    h.assertTrue(PlayerQuestManager.snapshotForIssuer(player, leader).offers().isEmpty(), "A refused player must be offered nothing");
                    h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, leader, 0) == PlayerQuestManager.Result.INVALID, "A refused player must not be able to accept");
                    var far = new WorldEncounterNpcEntity(ModEntityTypes.WORLD_ENCOUNTER_NPC.get(), h.getLevel());
                    far.configure(Role.LEADER, h.absolutePos(new BlockPos(2, 1, 2)).offset(500, 0, 0));
                    h.assertTrue(!WorldEncounterOffenses.refuses(player, far), "Another site's leader must not hold it against them");
                    leader.discard();
                    player.server.getPlayerList().remove(player);
                })
                .thenSucceed();
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "encounter-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }
}
