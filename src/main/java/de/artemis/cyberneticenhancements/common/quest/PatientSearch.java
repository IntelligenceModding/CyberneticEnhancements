package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The patient hunt behind a world encounter's quest: the quest only knows rough coordinates, the real patient lies
 * somewhere within them and only turns up once the player is close, and is found by walking right up to them.
 */
public final class PatientSearch {
    /** How far the marked area reaches; the patient is always inside it, never at its centre. */
    public static final int AREA_RADIUS = 56;
    private static final int MIN_OFFSET = 14, MAX_OFFSET = 38, SPAWN_RANGE = 110;

    private PatientSearch() {}

    /** Where the patient lies, worked out from the contract alone so it needs no storage of its own. */
    public static BlockPos column(QuestContract contract) {
        var center = contract.targetPos();
        var random = new java.util.Random(contract.id().hashCode() * 341873128712L);
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = MIN_OFFSET + random.nextInt(MAX_OFFSET - MIN_OFFSET + 1);
        return center.offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
    }

    static boolean update(ServerPlayer player, QuestContract contract) {
        var level = player.serverLevel();
        if (contract.status() != QuestStatus.ACTIVE || contract.destinationReached() || contract.targetPos() == null
                || !level.dimension().location().toString().equals(contract.dimensionId())) return false;
        var column = column(contract);
        double dx = player.getX() - column.getX(), dz = player.getZ() - column.getZ();
        if (dx * dx + dz * dz > (double) SPAWN_RANGE * SPAWN_RANGE || !level.hasChunkAt(column)) return false;
        if (level.getEntity(contract.patientId()) == null) {
            var patient = spawn(level, contract, column);
            if (patient != null) {
                contract.setPatientPos(patient.blockPosition()); // What the beacon is judged against at the hand-in.
                return true;
            }
        }
        return false; // It is the beacon, not finding the patient, that readies the quest.
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, net.minecraft.core.Direction.UP) || level.getBlockState(pos).isSolidRender(level, pos);
    }

    private static boolean empty(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** The side of the spot where the world already has a wall to lean on (a cliff, a boulder, a tree trunk), if any. */
    private static net.minecraft.core.Direction wallSide(ServerLevel level, BlockPos pos) {
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (solid(level, pos.relative(side)) && solid(level, pos.relative(side).above())) return side;
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
                && empty(level, pos) && empty(level, pos.above())
                && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()
                && !level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ICE); // Nobody sits down on a frozen lake.
    }

    /**
     * The nearest dry ground to the planned spot that already has a natural wall beside it; only if the whole
     * neighbourhood is flat, open ground does the patient simply sit down on the nearest dry spot instead.
     */
    private static BlockPos standingSpot(ServerLevel level, BlockPos column) {
        BlockPos open = null;
        for (int ring = 0; ring <= 24; ring++) {
            int steps = ring == 0 ? 1 : Math.min(32, ring * 4);
            for (int step = 0; step < steps; step++) {
                double angle = step * Math.PI * 2 / steps;
                var at = column.offset((int) Math.round(Math.cos(angle) * ring), 0, (int) Math.round(Math.sin(angle) * ring));
                if (!level.hasChunkAt(at) || !level.getWorldBorder().isWithinBounds(at)) continue;
                var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
                if (!standable(level, pos)) continue;
                if (wallSide(level, pos) != null) return pos;
                if (open == null) open = pos;
            }
        }
        return open;
    }

    public static WorldEncounterNpcEntity spawn(ServerLevel level, QuestContract contract, BlockPos column) {
        var pos = standingSpot(level, column);
        if (pos == null) return null;
        var patient = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(level);
        if (patient == null) return null;
        patient.setUUID(contract.patientId());
        var wall = wallSide(level, pos);
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        float yaw = level.random.nextFloat() * 360;
        if (wall != null) { // Back to the wall, facing away from it.
            x += wall.getStepX() * 0.2;
            z += wall.getStepZ() * 0.2;
            yaw = wall.getOpposite().toYRot();
        }
        patient.moveTo(x, pos.getY(), z, yaw, 0);
        patient.setYHeadRot(yaw);
        patient.setYBodyRot(yaw);
        patient.configure(WorldEncounterNpcEntity.Role.PATIENT, pos);
        patient.setGlowingTag(true); // Slumped in the open, they would be easy to walk right past.
        patient.setPersistenceRequired();
        patient.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        return level.addFreshEntity(patient) ? patient : null;
    }

    /** Right-clicking the patient with the quest's items; the patient needs no screen, the item does the work. */
    public static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof WorldEncounterNpcEntity patient)) return;
        if (patient.role() != WorldEncounterNpcEntity.Role.PATIENT) return;
        if (PlayerQuestManager.usePatientItem(player, patient, player.getMainHandItem())) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    static void cleanup(ServerPlayer player, QuestContract contract) {
        for (var level : player.server.getAllLevels()) {
            if (level.getEntity(contract.patientId()) instanceof WorldEncounterNpcEntity patient) patient.discard();
        }
    }
}
