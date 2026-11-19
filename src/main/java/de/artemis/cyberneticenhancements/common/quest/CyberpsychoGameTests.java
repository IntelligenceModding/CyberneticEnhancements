package de.artemis.cyberneticenhancements.common.quest;

import com.mojang.authlib.GameProfile;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusType;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoMissile;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyberpsychoGameTests {
    private CyberpsychoGameTests() {}

    @GameTest(template = "sandevistan_test", batch = "cyberpsycho_debug_command", timeoutTicks = 80)
    public static void debugCommandSpawnsRepeatableQuestFreeEncountersAndEnforcesPermission(GameTestHelper h) throws Exception {
        var player = player(h, "psycho-debug");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(2, 4, 2));
        var ground = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var spawned = new java.util.ArrayList<CyberpsychoEntity>();
        var dispatcher = player.server.getCommands().getDispatcher();
        var source = player.createCommandSourceStack();
        String command = "cyberneticenhancements dev cyberpsycho";
        try {
            for (int x = -17; x <= 17; x++) for (int z = -17; z <= 17; z++) for (int y = -1; y < 3; y++) {
                var pos = center.offset(x, y, z);
                ground.put(pos, level.getBlockState(pos));
                level.setBlock(pos, (y == -1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
            }
            player.setPos(Vec3.atBottomCenterOf(center));
            try {
                dispatcher.execute(command, source.withPermission(0));
                h.fail("Non-operator started a debug encounter");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            int quests = PlayerQuestManager.activeContracts(player).size();
            var area = new net.minecraft.world.phys.AABB(center).inflate(24);
            for (String arguments : List.of(" 1 mantis", " 3 launcher", "")) {
                var before = level.getEntitiesOfClass(CyberpsychoEntity.class, area).stream().map(CyberpsychoEntity::getUUID).toList();
                h.assertTrue(dispatcher.execute(command + arguments, source.withPermission(2)) == 1, "Debug encounter did not spawn: " + arguments);
                var additions = level.getEntitiesOfClass(CyberpsychoEntity.class, area).stream().filter(entity -> !before.contains(entity.getUUID())).toList();
                h.assertTrue(additions.size() == 1, "Command did not spawn exactly one subject");
                var psycho = additions.getFirst();
                spawned.add(psycho);
                h.assertTrue(psycho.contract() == null && psycho.questOwner() == null && psycho.getTarget() == player,
                        "Debug subject created a quest or did not target its survival player");
                h.assertTrue(psycho.distanceToSqr(player) >= 36 && psycho.distanceToSqr(player) <= 18 * 18 && level.noCollision(psycho), "Subject spawned too far away or inside terrain");
            }
            h.assertTrue(spawned.get(0).getMaxHealth() == 110 && spawned.get(0).loadout() == CyberpsychoLoadout.MANTIS
                    && spawned.get(1).getMaxHealth() == 170 && spawned.get(1).loadout() == CyberpsychoLoadout.LAUNCHER
                    && spawned.get(2).getMaxHealth() == 140, "Tier or loadout arguments were ignored");
            h.assertTrue(PlayerQuestManager.activeContracts(player).size() == quests, "Debug command changed the quest journal");
            h.assertTrue(dispatcher.execute(command + " 2 invalid", source.withPermission(2)) == 0, "Invalid loadout was accepted");
            // A command in mid-air must fail cleanly rather than burying or floating an encounter.
            player.setPos(player.position().add(0, 100, 0));
            h.assertTrue(dispatcher.execute(command, player.createCommandSourceStack().withPermission(2)) == 0, "Encounter spawned without supporting ground");
        } finally {
            spawned.forEach(CyberpsychoEntity::discard);
            ground.forEach((pos, state) -> level.setBlock(pos, state, 2));
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "cyberpsycho_contract", timeoutTicks = 80)
    public static void bossBarTracksNearbyViewersHealthAndRemovalWithoutAnAiTarget(GameTestHelper h) {
        var viewer = player(h, "psycho-bar");
        var psycho = ModEntityTypes.CYBERPSYCHO.get().create(h.getLevel());
        psycho.setPos(viewer.position());
        psycho.setNoAi(true);
        h.getLevel().addFreshEntity(psycho);
        try {
            psycho.setHealth(psycho.getMaxHealth() * 0.35F);
            psycho.tick();
            h.assertTrue(psycho.getTarget() == null && psycho.bossEvent().getPlayers().contains(viewer), "Nearby idle subject did not show its boss bar");
            h.assertTrue(Math.abs(psycho.bossEvent().getProgress() - 0.35F) < 0.001F, "Boss bar did not reflect current health");
            h.assertTrue(psycho.bossEvent().getId().equals(psycho.getUUID()), "Client cannot identify the cyberpsycho boss event");
            psycho.setHealth(psycho.getMaxHealth() * 0.75F);
            psycho.setCustomName(net.minecraft.network.chat.Component.literal("Updated subject"));
            psycho.tick();
            h.assertTrue(Math.abs(psycho.bossEvent().getProgress() - 0.75F) < 0.001F
                    && psycho.bossEvent().getName().getString().equals("Updated subject"), "Health or name updates left a stale bar");
            viewer.setPos(psycho.position().add(60, 0, 0));
            psycho.tick();
            h.assertTrue(!psycho.bossEvent().getPlayers().contains(viewer), "Distant viewer kept the boss bar");
            viewer.setPos(psycho.position());
            viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            psycho.tick();
            h.assertTrue(!psycho.bossEvent().getPlayers().contains(viewer), "Spectator received a combat bar");
            viewer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            psycho.tick();
            h.assertTrue(psycho.bossEvent().getPlayers().contains(viewer), "Returning viewer did not regain the bar");
            psycho.discard();
            h.assertTrue(psycho.bossEvent().getPlayers().isEmpty(), "Removed cyberpsycho left a stuck boss bar");
            var officer = ModEntityTypes.MAX_TAC.get().create(h.getLevel());
            officer.setPos(viewer.position());
            officer.setNoAi(true);
            h.getLevel().addFreshEntity(officer);
            officer.tick();
            h.assertTrue(officer.bossEvent().getPlayers().isEmpty(), "MaxTec inherited the cyberpsycho boss bar");
            officer.discard();
        } finally {
            if (!psycho.isRemoved()) psycho.discard();
            viewer.server.getPlayerList().remove(viewer);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "cyberpsycho_contract", timeoutTicks = 80)
    public static void sightingSpawnsOnePersistentSubjectAndRequiresTheirRecoveredShardForPayment(GameTestHelper h) {
        var owner = player(h, "psycho-owner");
        var observer = player(h, "psycho-other");
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(3, 14, 3));
        var ground = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var npc = ModEntityTypes.NETRUNNER.get().create(level);
        npc.setPos(Vec3.atBottomCenterOf(center.offset(-65, 0, 0)));
        npc.setNoAi(true);
        level.addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(owner);
        var quest = quest(npc.getUUID(), npc.getName().getString(), 2, CyberpsychoLoadout.SHOCK.ordinal(), center, level.dimension().location().toString());
        seed(owner, quest);
        try {
            for (int x = -29; x <= 29; x++) for (int z = -29; z <= 29; z++) {
                var pos = center.offset(x, -1, z);
                ground.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            owner.setPos(Vec3.atBottomCenterOf(center.offset(-60, 0, 0)));
            h.assertTrue(CyberpsychoEncounter.trySpawn(owner, quest), "Sighting did not create a cyberpsycho on valid ground");
            seed(owner, quest);
            var psycho = (CyberpsychoEntity) level.getEntity(quest.psychoId());
            h.assertTrue(psycho != null && psycho.getMaxHealth() == 140 && psycho.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) >= 16,
                    "Encounter did not have its intended health and armor");
            h.assertTrue(psycho.getName().getString().equals(quest.psychoName()) && psycho.getName().getStyle().getColor().getValue() == 0xFF5555,
                    "Idle cyberpsycho did not have its red identity");
            psycho.setTarget(owner);
            h.assertTrue(psycho.getName().getStyle().getColor().getValue() == 0xFF5555, "Aggressive name did not turn red");
            psycho.setTarget(null);
            h.assertTrue(psycho.getName().getStyle().getColor().getValue() == 0xFF5555, "Idle cyberpsycho stopped being red");
            psycho.setHealth(53);
            var saved = new CompoundTag();
            psycho.saveWithoutId(saved);
            var restored = ModEntityTypes.CYBERPSYCHO.get().create(level);
            restored.load(saved);
            h.assertTrue(restored.getName().getStyle().getColor().getValue() == 0xFF5555, "Reload changed the red nameplate");
            h.assertTrue(restored.getHealth() == 53 && restored.loadout() == CyberpsychoLoadout.SHOCK
                    && restored.questOwner().equals(owner.getUUID()) && restored.contract().id().equals(quest.id()), "Reload reset the opponent or its binding");
            restored.discard();
            h.assertTrue(!CyberpsychoEncounter.trySpawn(owner, QuestContract.fromTag(quest.toTag())), "Reload created a second opponent");
            h.assertTrue(QuestNavigation.capture(owner).targets().getFirst().pos().equals(center), "Sighting revealed the exact opponent position");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(owner, npc) != PlayerQuestManager.TurnInResult.OK, "Sighting paid before the kill");
            PlayerEurodollarManager.setBalance(owner, 0);
            psycho.invulnerableTime = 0;
            psycho.hurt(level.damageSources().playerAttack(owner), 10000);
            var death = CyberpsychoEncounter.get(level).death(quest.id());
            h.assertTrue(death != null, "Death did not persist a dossier record");
            var dataReloaded = CyberpsychoEncounter.load(CyberpsychoEncounter.get(level).save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            h.assertTrue(death.equals(dataReloaded.death(quest.id())), "Offline death record did not survive saving");
            var shard = (ItemEntity) level.getEntity(death.shard());
            h.assertTrue(shard != null && shard.getTarget().equals(owner.getUUID()) && shard.getItem().get(DataComponents.LORE).lines().size() >= 3,
                    "Defeated subject had no protected information shard");
            ticks(owner, 1);
            h.assertTrue(!PlayerQuestManager.activeContracts(owner).getFirst().isReady(), "Killing alone finished the sighting");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(owner, npc) != PlayerQuestManager.TurnInResult.OK, "Sighting paid without evidence");
            shard.setNoPickUpDelay();
            shard.playerTouch(observer);
            h.assertTrue(observer.getInventory().countItem(Items.AMETHYST_SHARD) == 0 && !shard.isRemoved(), "Another player stole the dossier");
            shard.playerTouch(owner);
            ticks(owner, 1);
            h.assertTrue(PlayerQuestManager.activeContracts(owner).getFirst().isReady(), "Recovering the correct dossier did not unlock turn-in");
            h.assertTrue(PlayerQuestManager.turnInRemotely(owner, quest.id()) == PlayerQuestManager.TurnInResult.IN_PERSON_REQUIRED,
                    "Dossier could be returned without meeting the quest giver");
            var held = ItemStack.EMPTY;
            for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
                if (owner.getInventory().getItem(slot).is(Items.AMETHYST_SHARD)) { held = owner.getInventory().removeItemNoUpdate(slot); break; }
            }
            h.assertTrue(!held.isEmpty(), "Dossier did not enter inventory");
            owner.containerMenu.setCarried(held);
            ticks(owner, 100);
            h.assertTrue(owner.getInventory().countItem(Items.AMETHYST_SHARD) == 0 && owner.containerMenu.getCarried().getCount() == 1,
                    "Moving the dossier on the cursor duplicated it");
            owner.containerMenu.setCarried(ItemStack.EMPTY);
            var lost = owner.drop(held, false);
            ticks(owner, 220);
            h.assertTrue(owner.getInventory().countItem(Items.AMETHYST_SHARD) == 0
                    && PlayerQuestManager.activeContracts(owner).getFirst().itemLossPending(), "Lost dossier did not await a loss-call decision");
            lost.setNoPickUpDelay();
            lost.playerTouch(owner);
            h.assertTrue(lost.isRemoved() && owner.getInventory().countItem(Items.AMETHYST_SHARD) == 1, "Original dossier could not be retrieved");
            ticks(owner, 1);
            int trust = npc.trustPoints(owner);
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(owner, npc) == PlayerQuestManager.TurnInResult.OK,
                    "Returning the dossier did not finish the contract");
            h.assertTrue(PlayerEurodollarManager.balance(owner) == quest.rewardMoney() && npc.trustPoints(owner) == trust + quest.rewardTrust()
                    && owner.getInventory().countItem(Items.AMETHYST_SHARD) == 0, "Sighting payout or evidence consumption was wrong");
            h.assertTrue(CyberpsychoEncounter.get(level).retired(quest.id()) && CyberpsychoEncounter.get(level).death(quest.id()) == null,
                    "Finished sighting left a live encounter record");
            h.assertTrue(de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.archiveContacts(observer).isEmpty(), "Cyberpsycho became a normal contact");
        } finally {
            PlayerQuestManager.clearAllQuestData(owner);
            CyberpsychoEncounter.cleanup(owner, quest);
            npc.discard();
            ground.forEach((pos, state) -> level.setBlock(pos, state, 2));
            owner.server.getPlayerList().remove(owner);
            observer.server.getPlayerList().remove(observer);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "cyberpsycho_combat", timeoutTicks = 80)
    public static void cyberwareMeleeAppliesShockAndChargedMissilesActuallyDamagePlayersWithoutBreakingTerrain(GameTestHelper h) {
        var player = player(h, "psycho-combat");
        var level = h.getLevel();
        var previousDifficulty = level.getDifficulty();
        var home = h.absolutePos(new BlockPos(3, 2, 3));
        var psychos = new java.util.ArrayList<CyberpsychoEntity>();
        var arena = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        try {
            player.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for (int x = -16; x <= 7; x++) for (int z = 0; z <= 7; z++) for (int y = 1; y <= 6; y++) {
                var pos = h.absolutePos(new BlockPos(x, y, z));
                arena.put(pos, level.getBlockState(pos));
                level.setBlock(pos, (y == 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
            }
            player.setPos(Vec3.atBottomCenterOf(home));
            for (int i = 0; i < 65; i++) player.tick();
            player.setPos(Vec3.atBottomCenterOf(home));
            player.setHealth(20);
            player.invulnerableTime = 0;
            var melee = ModEntityTypes.CYBERPSYCHO.get().create(level);
            melee.configure(quest(UUID.randomUUID(), "Contact", 3, CyberpsychoLoadout.SHOCK.ordinal(), home, level.dimension().location().toString()), player.getUUID(), home);
            melee.setPos(player.position().add(2, 0, 0));
            level.addFreshEntity(melee);
            psychos.add(melee);
            melee.setTarget(player);
            for (int i = 0; i < 8; i++) melee.tick();
            h.assertTrue(player.getHealth() < 20 && CombatStatusManager.collectActiveStatuses(player, level.getGameTime()).stream()
                    .anyMatch(status -> status.type() == CombatStatusType.SHOCK), "Cyberware melee was cosmetic or did not apply shock");
            melee.discard();
            CombatStatusManager.clearStatuses(player);
            player.setPos(Vec3.atBottomCenterOf(home));
            player.setDeltaMovement(Vec3.ZERO);
            player.setHealth(20);
            player.invulnerableTime = 0;
            var launcher = ModEntityTypes.CYBERPSYCHO.get().create(level);
            launcher.configure(quest(UUID.randomUUID(), "Contact", 2, CyberpsychoLoadout.LAUNCHER.ordinal(), home, level.dimension().location().toString()), player.getUUID(), home);
            launcher.setPos(player.position().add(-12, 0, 0));
            level.addFreshEntity(launcher);
            psychos.add(launcher);
            launcher.setTarget(player);
            boolean windup = false, missileSeen = false;
            var ground = level.getBlockState(home.below());
            for (int i = 0; i < 125; i++) {
                launcher.tick();
                windup |= launcher.charging();
                for (var missile : level.getEntitiesOfClass(CyberpsychoMissile.class, launcher.getBoundingBox().inflate(40))) {
                    missileSeen = true;
                    missile.tick();
                }
            }
            h.assertTrue(windup && missileSeen && player.getHealth() < 20 && player.isAlive(), "Launcher did not wind up, fire and deal survivable real damage");
            h.assertTrue(level.getBlockState(home.below()).equals(ground), "Missile explosion damaged terrain");
        } finally {
            psychos.forEach(net.minecraft.world.entity.Entity::discard);
            level.getEntitiesOfClass(CyberpsychoMissile.class, new net.minecraft.world.phys.AABB(home).inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
            CombatStatusManager.clearStatuses(player);
            player.server.setDifficulty(previousDifficulty, true);
            arena.forEach((pos, state) -> level.setBlock(pos, state, 2));
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "cyberpsycho_repeat", timeoutTicks = 80)
    public static void repeatedRetiredEncounterIsRekeyedAndSpawnsWithoutClearingTheFirstMissionsHistory(GameTestHelper h) {
        var player = player(h, "repeat-psycho");
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(2, 10, 2));
        var ground = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var first = quest(UUID.randomUUID(), "Dev-Test", 1, 0, center, level.dimension().location().toString());
        try {
            h.assertTrue(CyberpsychoEncounter.repairRepeatedContract(player, first) == first, "A fresh mission was unnecessarily replaced");
            CyberpsychoEncounter.cleanup(player, first);
            for (int x = -29; x <= 29; x++) for (int z = -29; z <= 29; z++) {
                var pos = center.offset(x, -1, z); ground.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            var repeated = QuestContract.fromTag(first.toTag());
            repeated.markPsychoSpawned();
            seed(player, repeated);
            player.setPos(Vec3.atBottomCenterOf(center.offset(-60, 0, 0)));
            ticks(player, 40);
            var active = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(!active.id().equals(first.id()) && !active.psychoId().equals(first.psychoId()), "Repeat reused the retired encounter identity");
            h.assertTrue(active.psychoSpawned() && level.getEntity(active.psychoId()) instanceof CyberpsychoEntity,
                    "Already accepted broken repeat did not recover and spawn its cyberpsycho");
            h.assertTrue(active.issuerId().equals(first.issuerId()) && active.targetPos().equals(center), "Recovery changed the issuer or mission location");
            h.assertTrue(CyberpsychoEncounter.get(level).retired(first.id()) && !CyberpsychoEncounter.get(level).retired(active.id()), "Recovery cleared old mission history");
        } finally {
            for (var active : PlayerQuestManager.activeContracts(player)) CyberpsychoEncounter.cleanup(player, active);
            ground.forEach((pos, state) -> level.setBlock(pos, state, 2));
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static QuestContract quest(UUID issuer, String name, int tier, int loadout, BlockPos pos, String dimension) {
        return new QuestContract(UUID.randomUUID().toString(), issuer, name, "netrunner", QuestType.CYBERPSYCHO,
                tier, QuestStatus.ACTIVE, loadout, 1, 0, false, dimension, pos, 48, 1800, 5, 0, 0, 0, 0);
    }

    private static void seed(ServerPlayer player, QuestContract quest) {
        var root = new CompoundTag();
        var boards = new CompoundTag();
        var board = new CompoundTag();
        board.put("Active", quest.toTag());
        board.putLong("RefreshDay", player.level().getDayTime() / 24000);
        board.put("Offers", new net.minecraft.nbt.ListTag());
        boards.put(quest.issuerId().toString(), board);
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static void ticks(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            player.tickCount++;
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
