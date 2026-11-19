package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.entity.HolographicNpc;
import de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class HologramDebugCommand {
    private static final String PREVIEW = "cyberneticenhancements.hologram_preview";
    private static final String OWNER = "HologramPreviewOwner";
    private HologramDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var spawn = Commands.literal("spawn");
        var despawn = Commands.literal("despawn").executes(context -> dismiss(context.getSource(), "all"));
        for (String type : new String[]{"medtec", "maxtec"}) {
            spawn.then(Commands.literal(type).executes(context -> spawn(context.getSource(), type, 1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
                            .executes(context -> spawn(context.getSource(), type, IntegerArgumentType.getInteger(context, "count")))));
            despawn.then(Commands.literal(type).executes(context -> dismiss(context.getSource(), type)));
        }
        despawn.then(Commands.literal("all").executes(context -> dismiss(context.getSource(), "all")));
        dispatcher.register(Commands.literal("cyberneticenhancements").then(Commands.literal("dev")
                .then(Commands.literal("hologram").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(spawn).then(despawn))));
    }

    private static int spawn(CommandSourceStack source, String type, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var level = source.getLevel();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            Mob mob = type.equals("medtec") ? ModEntityTypes.TRAUMA_TEAM.get().create(level) : ModEntityTypes.MAX_TAC.get().create(level);
            if (mob == null) continue;
            Vec3 desired = player.position().add(forward.scale(4)).add(right.scale((index - (count - 1) / 2.0) * 1.5));
            boolean placed = false;
            for (int dy = 2; dy >= -4; dy--) {
                var pos = net.minecraft.core.BlockPos.containing(desired).offset(0, dy, 0);
                if (!level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty()
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180, 0);
                if (level.noCollision(mob)) { placed = true; break; }
            }
            if (!placed) { mob.discard(); continue; }
            mob.setNoAi(true);
            mob.setInvulnerable(true);
            mob.setPersistenceRequired();
            mob.addTag(PREVIEW);
            mob.getPersistentData().putUUID(OWNER, player.getUUID());
            if (mob instanceof TraumaTeamEntity medic) medic.ensureIdentity();
            if (level.addFreshEntity(mob)) spawned++;
        }
        int result = spawned;
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.literal("Spawned " + result + " " + type
                + " hologram preview(s). Use /cyberneticenhancements dev hologram despawn to dissolve them."), false);
        if (spawned < count) de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.literal("Some previews could not spawn. Stand near clear, solid ground with room in front of you."));
        return spawned;
    }

    private static int dismiss(CommandSourceStack source, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var owner = source.getPlayerOrException().getUUID();
        int dismissed = 0;
        for (var level : source.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob) || !(mob instanceof HolographicNpc) || !mob.getTags().contains(PREVIEW)
                        || !mob.getPersistentData().hasUUID(OWNER) || !owner.equals(mob.getPersistentData().getUUID(OWNER))
                        || HolographicDeployment.departing(mob)) continue;
                if (!type.equals("all") && type.equals("medtec") != (mob instanceof TraumaTeamEntity)) continue;
                HolographicDeployment.dismiss(mob);
                dismissed++;
            }
        }
        int result = dismissed;
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.literal("Dissolving " + result + " hologram preview(s)."), false);
        return dismissed;
    }
}
