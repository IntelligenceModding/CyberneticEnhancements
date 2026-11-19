package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HolographicDeploymentGameTests {
    @GameTest(template = "sandevistan_test", batch = "hologram_commands", timeoutTicks = 75)
    public static void previewCommandsSpawnAndDismissOnlyTheirOwnTestNpcs(GameTestHelper h) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var level = h.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hologram-preview");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        var origin = h.absolutePos(new BlockPos(3, 1, 2));
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
        player.setYRot(0);
        for (int x = -3; x <= 3; x++) for (int z = 3; z <= 5; z++) {
            level.setBlockAndUpdate(origin.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(origin.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var source = level.getServer().createCommandSourceStack().withLevel(level).withEntity(player).withPermission(2);
        var dispatcher = level.getServer().getCommands().getDispatcher();
        String prefix = "cyberneticenhancements dev hologram ";
        h.assertTrue(dispatcher.execute(prefix + "spawn medtec 2", source) == 2, "Medtec preview command did not spawn two responders");
        h.assertTrue(dispatcher.execute(prefix + "spawn maxtec", source) == 1, "MaxTec preview command failed");
        var real = ModEntityTypes.MAX_TAC.get().create(level);
        real.setNoAi(true);
        real.setPos(player.position());
        level.addFreshEntity(real);
        h.assertTrue(dispatcher.execute(prefix + "despawn medtec", source) == 2, "Medtec-only dismissal selected the wrong previews");
        h.assertTrue(!HolographicDeployment.departing(real), "Preview command dismissed a real squad member");
        h.runAfterDelay(30, () -> {
            try {
                h.assertTrue(dispatcher.execute(prefix + "despawn all", source) == 1, "Remaining MaxTac preview was not dismissed");
                h.assertTrue(!real.isRemoved() && !HolographicDeployment.departing(real), "Dismiss-all affected a real squad member");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                h.fail(exception.getMessage());
            }
        });
        h.runAfterDelay(55, () -> {
            try {
                for (var entity : level.getAllEntities())
                    h.assertTrue(!entity.getTags().contains("cyberneticenhancements.hologram_preview")
                                    || !entity.getPersistentData().getUUID("HologramPreviewOwner").equals(player.getUUID()),
                            "Preview command left an NPC after its dissolve animation");
                h.succeed();
            } finally {
                real.discard();
            }
        });
    }

    @GameTest(template = "sandevistan_test", batch = "holographic_deployment", timeoutTicks = 65)
    public static void bothSquadsMaterializeOnceAndFinishDismissalDespiteRepeatedRequests(GameTestHelper h) {
        java.util.List<Mob> squad = java.util.List.of(ModEntityTypes.TRAUMA_TEAM.get().create(h.getLevel()),
                ModEntityTypes.MAX_TAC.get().create(h.getLevel()));
        for (int i = 0; i < squad.size(); i++) {
            Mob mob = squad.get(i);
            mob.setNoAi(true);
            mob.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2 + i * 2, 1, 2))));
            h.getLevel().addFreshEntity(mob);
        }
        h.runAfterDelay(3, () -> {
            for (Mob mob : squad)
                h.assertTrue(HolographicDeployment.visibility(mob, 0) > 0 && HolographicDeployment.visibility(mob, 0) < 1,
                        "Squad member did not enter its arrival animation");
        });
        h.runAfterDelay(25, () -> {
            for (Mob mob : squad) {
                h.assertTrue(HolographicDeployment.visibility(mob, 0) == 1, "Arrival did not complete");
                var restored = (Mob) mob.getType().create(h.getLevel());
                restored.load(mob.saveWithoutId(new CompoundTag()));
                HolographicDeployment.tick(restored);
                h.assertTrue(HolographicDeployment.visibility(restored, 0) == 1, "Reload replayed arrival");
                restored.discard();
                HolographicDeployment.dismiss(mob);
            }
        });
        h.runAfterDelay(30, () -> {
            for (Mob mob : squad) {
                HolographicDeployment.dismiss(mob);
                h.assertTrue(!mob.isRemoved() && HolographicDeployment.departing(mob), "Departure removed the model immediately");
                h.assertTrue(mob.isNoAi() && mob.getTarget() == null, "Departing squad member remained combat active");
                h.assertTrue(!de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(mob, false), "Living departure started a corpse animation");
                h.assertTrue(HolographicDeployment.visibility(mob, 0) < 1, "Departure did not fade the model");
            }
        });
        h.runAfterDelay(48, () -> {
            for (Mob mob : squad) {
                h.assertTrue(mob.isRemoved(), "Repeated dismissal restarted the animation timer");
                h.assertTrue(mob.getHealth() > 0 && !de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(mob, false),
                        "Healthy removed responder was classified as a fallen body");
                h.assertTrue(de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(mob, true), "Confirmed server death was ignored before a health update");
                mob.setHealth(0);
                h.assertTrue(de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(mob, false), "A genuine dead body was discarded with living departures");
            }
            h.succeed();
        });
    }
}
