package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity.Role;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

/**
 * World encounters: scenes fixed in the world rather than rolled on the street. Each is a structure with people
 * beside it, one of whom has a quest. Their quests are ordinary contracts, handed in in person.
 */
public final class WorldEncounters {
    /** Where one person of the group stands: {@code inside} the build, or out in front of its door. */
    public record Slot(BlockPos pos, float yaw, boolean inside) {}

    // Where things are in the prefab itself (med_tec_av.nbt), before it is turned: its doorway is two blocks wide on the
    // south side, with the entrance stairs at x 9-10; the guards stand one on each side of those stairs.
    private static final BlockPos LEADER = new BlockPos(11, 1, 7), DOOR_LEFT = new BlockPos(8, 1, 10), DOOR_RIGHT = new BlockPos(11, 1, 10);

    private WorldEncounters() {}

    /**
     * The three places of the group, leader first: the leader inside by the door, the others out in front, one beside each side of the entrance stairs, facing the door.
     * Where they stand follows the build, however it was turned when it was placed.
     */
    public static List<Slot> slots(BlockPos origin, Rotation rotation) {
        var settings = new StructurePlaceSettings().setRotation(rotation);
        float inward = rotation.rotate(Direction.NORTH).toYRot(), outward = rotation.rotate(Direction.SOUTH).toYRot();
        return List.of(
                new Slot(StructureTemplate.calculateRelativePosition(settings, LEADER).offset(origin), outward, true),
                new Slot(StructureTemplate.calculateRelativePosition(settings, DOOR_LEFT).offset(origin), inward, false),
                new Slot(StructureTemplate.calculateRelativePosition(settings, DOOR_RIGHT).offset(origin), inward, false));
    }

    /**
     * Places the group whose spot lies in the chunk being generated, so each is added exactly once however
     * many chunks the structure spans.
     */
    public static void populate(WorldGenLevel level, BlockPos origin, Rotation rotation, BoundingBox chunk) {
        var slots = slots(origin, rotation);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (!chunk.isInside(slot.pos())) continue;
            var pos = slot.pos();
            if (!slot.inside()) {
                pos = groundAt(level, pos.getX(), pos.getZ());
                clearAbove(level, pos);
            }
            var npc = ModEntityTypes.WORLD_ENCOUNTER_NPC.get().create(level.getLevel());
            if (npc == null) continue;
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, slot.yaw(), 0);
            npc.setYHeadRot(slot.yaw());
            npc.configure(i == 0 ? Role.LEADER : Role.BYSTANDER, pos);
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            level.addFreshEntityWithPassengers(npc);
        }
    }

    /**
     * The first free spot above the ground as it really is right now: the world generation heightmap does not know about
     * the blocks the structure and its terrain adaptation have just placed, and is often a block too low.
     */
    private static BlockPos groundAt(WorldGenLevel level, int x, int z) {
        int guess = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        var cursor = new BlockPos.MutableBlockPos();
        for (int y = guess + 3; y >= guess - 2; y--) {
            cursor.set(x, y - 1, z);
            var below = level.getBlockState(cursor);
            if (below.getCollisionShape(level, cursor).isEmpty() || !below.getFluidState().isEmpty()
                    || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) continue;
            cursor.set(x, y, z);
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) continue;
            cursor.set(x, y + 1, z);
            if (level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return new BlockPos(x, y, z);
        }
        return new BlockPos(x, guess, z);
    }

    /** Nobody should be standing in a treetop or a bush that happens to grow there. */
    private static void clearAbove(WorldGenLevel level, BlockPos pos) {
        var cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy < 3; dy++) {
            cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
            var state = level.getBlockState(cursor);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.VINE)
                    || dx == 0 && dz == 0 && !state.isAir() && state.getCollisionShape(level, cursor).isEmpty())
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
