package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

final class EliminationEncounter {
    private EliminationEncounter() {
    }

    static boolean trySpawn(ServerPlayer player, QuestContract contract) {
        ServerLevel level = player.serverLevel();
        BlockPos center = contract.targetPos();
        if (contract.status() != QuestStatus.ACTIVE || center == null
                || !level.dimension().location().toString().equals(contract.dimensionId())
                || level.getDifficulty() == Difficulty.PEACEFUL
                || !player.blockPosition().closerThan(center, 64)) {
            return false;
        }
        int limit = Math.min(12, contract.targetCount());
        int remaining = Math.min(limit - contract.huntSpawnedCount(), contract.targetCount() - contract.progress());
        boolean dirty = false;
        int radius = Math.max(2, Math.min(24, contract.areaRadius() - 2));
        for (int attempt = 0; remaining > 0 && attempt < 32; attempt++) {
            BlockPos column = center.offset(level.random.nextInt(radius * 2 + 1) - radius, 0,
                    level.random.nextInt(radius * 2 + 1) - radius);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (!pos.closerThan(center, contract.areaRadius()) || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 144
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(pos).isEmpty()) {
                continue;
            }
            if (!(contract.eliminationTarget().create(level) instanceof Mob mob)) {
                break;
            }
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            if (!level.noCollision(mob)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            if (level.addFreshEntity(mob)) {
                contract.recordHuntSpawn();
                remaining--;
                dirty = true;
            }
        }
        return dirty;
    }
}
