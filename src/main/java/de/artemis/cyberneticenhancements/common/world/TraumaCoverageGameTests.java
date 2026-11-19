package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TraumaCoverageGameTests {
    @GameTest(template = "sandevistan_test", batch = "medtec_coverage_command", timeoutTicks = 30)
    public static void coverageCommandRequiresPermissionAndGrantsAFreePersistentPolicy(GameTestHelper h) throws Exception {
        var player = player(h);
        try {
            var source = player.createCommandSourceStack();
            var dispatcher = player.server.getCommands().getDispatcher();
            String command = "cyberneticenhancements dev medtec coverage";
            int money = de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player);
            try {
                dispatcher.execute(command, source.withPermission(0));
                h.fail("Coverage command allowed a player without operator permission");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            h.assertTrue(!player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Rejected command granted coverage");
            h.assertTrue(dispatcher.execute(command, source.withPermission(2)) == 1
                    && player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Debug command did not activate a real policy");
            h.assertTrue(dispatcher.execute(command, source.withPermission(2)) == 1
                    && de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == money, "Coverage grant charged money or failed on repeat");
            var clone = new ServerPlayer(player.server, h.getLevel(), player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            TraumaCoverage.copy(player, clone);
            h.assertTrue(clone.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Granted policy did not survive respawning");
        } finally { player.server.getPlayerList().remove(player); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "trauma_purchase", timeoutTicks = 30)
    public static void medTechSellsOneServerOwnedPolicyWithoutChargingForDuplicates(GameTestHelper h) {
        var player = player(h);
        var npc = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.MED_TECH.get().create(h.getLevel());
        npc.setPos(player.position().add(2, 0, 0));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        try {
            de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.setBalance(player, 50000);
            npc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            var menu = (de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu) player.containerMenu;
            int price = de.artemis.cyberneticenhancements.common.dialogue.NpcService.TRAUMA_COVERAGE.basePrice();
            menu.handleChoice(player, 2);
            h.assertTrue(player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Med-tech did not activate coverage");
            h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == 50000 - price, "Coverage did not charge the configured price");
            menu.handleChoice(player, 2);
            h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == 50000 - price, "Duplicate policy purchase charged again");
            h.assertTrue(NpcAppearanceRules.reserved("cyberneticenhancements:textures/entity/npc/generic/trauma_team.png"), "Trauma skin is available to other NPC professions");
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "trauma_rescue", timeoutTicks = 550)
    public static void coveredLethalDamageWaitsForCallThenRevivesWithoutDeathOrLoot(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos origin = player.blockPosition();
        for (int x = -25; x <= 25; x++)
            for (int z = -25; z <= 25; z++) {
                h.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 3; y++)
                    h.getLevel().setBlockAndUpdate(origin.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        player.getPersistentData().putBoolean(TraumaCoverage.COVERAGE, true);
        int deaths = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        player.setHealth(0);
        player.die(player.damageSources().generic());
        h.assertTrue(TraumaCoverage.downed(player) && player.isAlive(), "Lethal damage was not converted into a recoverable downed state");
        var bodies = bodyUpdates(player);
        h.assertTrue(bodies.size() == 1 && bodies.getFirst().phase() == de.artemis.cyberneticenhancements.common.network.DeathBodyPayload.DOWNED,
                "Recoverable damage sent a confirmed-death body instead of holding the downed body");
        h.assertTrue(bodies.getFirst().equipment().size() == 6, "Body synchronization did not limit appearance to the six visible equipment slots");
        h.assertTrue(player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Coverage was consumed before calling");
        h.runAfterDelay(5, () -> {
            h.assertTrue(h.getLevel().getEntitiesOfClass(TraumaTeamEntity.class, player.getBoundingBox().inflate(35)).isEmpty(), "Responders dispatched before calling");
            TraumaCoverage.callMedtec(player);
            TraumaCoverage.callMedtec(player);
            h.assertTrue(!player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Calling did not consume coverage");
        });
        h.runAfterDelay(8, () -> {
            var team = h.getLevel().getEntitiesOfClass(TraumaTeamEntity.class, player.getBoundingBox().inflate(35));
            h.assertTrue(!team.isEmpty(), "No reachable responders were deployed");
            team.forEach(medic -> medic.setPos(player.getX() + 1.2, player.getY(), player.getZ()));
        });
        h.runAfterDelay(100, () -> {
            try {
                h.assertTrue(!TraumaCoverage.downed(player) && player.getHealth() > 1, "Responders never revived the patient");
                var recovered = bodyUpdates(player);
                h.assertTrue(recovered.size() == 1 && recovered.getFirst().phase() == de.artemis.cyberneticenhancements.common.network.DeathBodyPayload.RECOVERED,
                        "Revival did not release the held body exactly once");
                h.assertTrue(player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) == deaths, "Successful rescue incremented the death statistic");
                h.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 7, "Successful rescue lost or duplicated inventory");
                h.assertTrue(player.getActiveEffects().isEmpty(), "Revival granted vanilla potion effects");
                h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.TemporaryCyberwareEffectManager.hasStoredEffects(player), "Revival did not grant cyberware status effects");
                var team = h.getLevel().getEntitiesOfClass(TraumaTeamEntity.class, player.getBoundingBox().inflate(35));
                h.assertTrue(!team.isEmpty(), "Responders disappeared before aftercare");
                team.getFirst().mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                h.assertTrue(player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu, "Responder did not allow conversation after revival");
            } finally {
                player.closeContainer();
            }
        });
        h.runAfterDelay(510, () -> {
            try {
                h.assertTrue(h.getLevel().getEntitiesOfClass(TraumaTeamEntity.class, player.getBoundingBox().inflate(80)).isEmpty(), "Responders did not leave after 20 seconds");
                h.succeed();
            } finally {
                player.server.getPlayerList().remove(player);
            }
        });
    }

    @GameTest(template = "sandevistan_test", batch = "trauma_abandon", timeoutTicks = 30)
    public static void abandoningARescueCommitsExactlyOneDeath(GameTestHelper h) {
        ServerPlayer player = player(h);
        try {
            player.getPersistentData().putBoolean(TraumaCoverage.COVERAGE, true);
            int before = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
            player.setHealth(0);
            player.die(player.damageSources().generic());
            bodyUpdates(player);
            TraumaCoverage.abandon(player);
            TraumaCoverage.abandon(player);
            var bodies = bodyUpdates(player);
            h.assertTrue(bodies.stream().filter(p -> p.phase() == de.artemis.cyberneticenhancements.common.network.DeathBodyPayload.DEAD).count() == 1,
                    "Abandon did not start exactly one corpse cleanup timer");
            h.assertTrue(player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Respawning without calling consumed coverage");
            h.assertTrue(!player.isAlive() && !TraumaCoverage.downed(player), "Abandon left an immortal downed player");
            h.assertTrue(player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) == before + 1, "Abandon counted more or fewer than one death");
        } finally {
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "trauma_invalid_call", timeoutTicks = 30)
    public static void callingWithoutCoverageOrWhileHealthyDoesNothing(GameTestHelper h) {
        ServerPlayer player = player(h);
        try {
            TraumaCoverage.callMedtec(player);
            h.assertTrue(!TraumaCoverage.downed(player), "Uninsured call started a rescue");
            player.getPersistentData().putBoolean(TraumaCoverage.COVERAGE, true);
            TraumaCoverage.callMedtec(player);
            h.assertTrue(player.getPersistentData().getBoolean(TraumaCoverage.COVERAGE), "Healthy call consumed coverage");
            h.assertTrue(!TraumaCoverage.downed(player), "Healthy call started a rescue");
        } finally {
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "trauma_guard", timeoutTicks = 460)
    public static void respondersFightPatientAggressorsAndLeaveAfterTwentySeconds(GameTestHelper h) {
        var patient = player(h);
        var medic = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.TRAUMA_TEAM.get().create(h.getLevel());
        var aggressor = net.minecraft.world.entity.EntityType.HUSK.create(h.getLevel());
        var neutral = net.minecraft.world.entity.EntityType.COW.create(h.getLevel());
        medic.setPos(patient.position().add(1, 0, 0));
        medic.addTag("cyberneticenhancements.trauma_dispatch");
        h.getLevel().addFreshEntity(medic);
        h.assertTrue(!medic.protects(patient.getUUID()), "Unassigned responder had a protection marker");
        medic.beginAftercare(patient);
        h.assertTrue(medic.protects(patient.getUUID()), "Guarding responder had no protection marker");
        h.assertTrue(!medic.protects(UUID.randomUUID()), "Responder was marked for another player");
        var saved = medic.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.TRAUMA_TEAM.get().create(h.getLevel());
        restored.load(saved);
        h.assertTrue(restored.protects(patient.getUUID()), "Protection marker ownership did not survive saving");
        restored.discard();
        aggressor.setPos(patient.position().add(1.5, 0, 0));
        aggressor.setNoAi(true);
        aggressor.setTarget(patient);
        h.getLevel().addFreshEntity(aggressor);
        neutral.setPos(patient.position().add(0.5, 0, 0));
        neutral.setNoAi(true);
        h.getLevel().addFreshEntity(neutral);
        h.runAfterDelay(10, () -> h.assertTrue(medic.getTarget() == aggressor, "Medic ignored a mob targeting the patient"));
        h.runAfterDelay(35, () -> {
            h.assertTrue(aggressor.getLastHurtByMob() == medic, "Medic selected an aggressor but never attacked it");
            aggressor.setTarget(medic);
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(medic.getTarget() == aggressor, "Medic stopped defending when the aggressor retaliated");
            aggressor.setTarget(null);
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(medic.getTarget() == null, "Medic continued attacking a mob that stopped being aggressive");
            h.assertTrue(neutral.getHealth() == neutral.getMaxHealth(), "Medic attacked a neutral animal");
        });
        h.runAfterDelay(390, () -> h.assertTrue(medic.isAlive() && !medic.isRemoved(), "Medic left before the protection period ended"));
        h.runAfterDelay(430, () -> {
            try {
                h.assertTrue(medic.isRemoved(), "Medic remained after the protection period");
                h.assertTrue(!medic.protects(patient.getUUID()), "Expired responder retained a protection marker");
                h.succeed();
            } finally {
                aggressor.discard();
                neutral.discard();
                medic.discard();
                patient.server.getPlayerList().remove(patient);
            }
        });
    }

    @GameTest(template = "sandevistan_test", batch = "death_direction", timeoutTicks = 30)
    public static void npcFallsAwayFromItsLastHitEvenWhenFinalDamageHasNoDirection(GameTestHelper h) {
        var attacker = player(h);
        var npc = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.MED_TECH.get().create(h.getLevel());
        npc.setPos(attacker.position()); npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        try {
            attacker.setPos(npc.position().add(3, 0, 0));
            npc.hurt(attacker.damageSources().playerAttack(attacker), 1);
            attacker.setPos(npc.position().add(-3, 0, 0));
            npc.invulnerableTime = 0;
            npc.hurt(attacker.damageSources().playerAttack(attacker), 1);
            npc.setHealth(0); npc.die(npc.damageSources().generic());
            // A newly tracking client must receive the authoritative direction too.
            bodyUpdates(attacker);
            DeathBodySync.tracking(new net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking(attacker, npc));
            var bodies = bodyUpdates(attacker);
            h.assertTrue(bodies.size() == 1 && bodies.getFirst().player().equals(npc.getUUID()), "NPC death appearance was not synchronized to a new observer");
            h.assertTrue(Math.abs(bodies.getFirst().fallYaw() - 90) < 0.01, "NPC did not fall east after its last hit from the west");
        } finally {
            npc.discard(); attacker.server.getPlayerList().remove(attacker);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "death_direction", timeoutTicks = 100)
    public static void downedPlayerKeepsHitDirectionWhenAbandoningRevival(GameTestHelper h) {
        var patient = player(h);
        patient.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // Native players cannot take ordinary damage during their first 60 ticks.
        h.runAfterDelay(65, () -> {
            var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());
            attacker.setPos(patient.position().add(3, 0, 0)); attacker.setNoAi(true);
            h.getLevel().addFreshEntity(attacker);
            try {
                patient.getPersistentData().putBoolean(TraumaCoverage.COVERAGE, true);
                h.assertTrue(patient.hurt(patient.damageSources().mobAttack(attacker), 1000), "Test patient rejected damage");
                var downed = bodyUpdates(patient);
                h.assertTrue(TraumaCoverage.downed(patient) && downed.size() == 1, "Lethal hit did not synchronize one held body");
                h.assertTrue(Math.abs(downed.getFirst().fallYaw() + 90) < 0.01, "Hit from the east did not tip the body west");
                attacker.setPos(patient.position().add(-3, 0, 0));
                TraumaCoverage.abandon(patient);
                var dead = bodyUpdates(patient);
                h.assertTrue(dead.size() == 1 && dead.getFirst().phase() == de.artemis.cyberneticenhancements.common.network.DeathBodyPayload.DEAD,
                        "Abandoning revived the body before committing death");
                h.assertTrue(dead.getFirst().fallYaw() == downed.getFirst().fallYaw(), "Abandoning changed the already fallen body's direction");
            } finally {
                attacker.discard(); patient.server.getPlayerList().remove(patient);
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "death_direction", timeoutTicks = 100)
    public static void projectileFallUsesIncomingTravelInsteadOfShootersCurrentPosition(GameTestHelper h) {
        var victim = player(h);
        victim.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runAfterDelay(65, () -> {
            var shooter = net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());
            shooter.setPos(victim.position().add(3, 0, 0));
            var arrow = net.minecraft.world.entity.EntityType.ARROW.create(h.getLevel());
            arrow.setPos(victim.position().add(-0.1, 0, 0)); arrow.setDeltaMovement(1, 0, 0);
            try {
                h.assertTrue(victim.hurt(victim.damageSources().arrow(arrow, shooter), 1000), "Test victim rejected projectile damage");
                var bodies = bodyUpdates(victim);
                h.assertTrue(bodies.size() == 1 && Math.abs(bodies.getFirst().fallYaw() - 90) < 0.01,
                        "An eastbound projectile did not tip the body east");
            } finally {
                victim.server.getPlayerList().remove(victim);
            }
            h.succeed();
        });
    }

    private static java.util.List<de.artemis.cyberneticenhancements.common.network.DeathBodyPayload> bodyUpdates(ServerPlayer player) {
        var channel = (io.netty.channel.embedded.EmbeddedChannel) player.connection.getConnection().channel();
        channel.runPendingTasks();
        // GameTests run inside the world tick, while native player packet flushing is suspended.
        channel.flushOutbound();
        var bodies = new java.util.ArrayList<de.artemis.cyberneticenhancements.common.network.DeathBodyPayload>();
        Object next;
        while ((next = channel.readOutbound()) != null)
            if (next instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet
                    && packet.payload() instanceof de.artemis.cyberneticenhancements.common.network.DeathBodyPayload payload) bodies.add(payload);
        return bodies;
    }

    private static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "trauma-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }
}
