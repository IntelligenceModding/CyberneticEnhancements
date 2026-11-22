package de.artemis.cyberneticenhancements.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A container that lies on its side is its template turned a quarter around its long axis. Templates can only be turned around the vertical axis,
 * so the turned copy is made here, block by block, with the directions of the blocks that have one turned with it. Blocks that have no way to
 * face in the new direction (stairs running across the length, walls' side arms) are replaced by the nearest thing that does.
 * A roll of +1 turns east towards up, -1 turns east towards down, 0 leaves the container upright.
 */
final class ShippingContainerRoll {
    private static final Map<String, StructureTemplate> CACHE = new ConcurrentHashMap<>();

    private ShippingContainerRoll() {}

    static BlockPos rolled(BlockPos pos, int roll) {
        return roll > 0 ? new BlockPos(6 - pos.getY(), pos.getX(), pos.getZ()) : roll < 0 ? new BlockPos(pos.getY(), 6 - pos.getX(), pos.getZ()) : pos;
    }

    static BlockPos unrolled(BlockPos pos, int roll) { return rolled(pos, -roll); }

    static Direction rolled(Direction direction, int roll) {
        if (roll == 0) return direction;
        return switch (direction) {
            case EAST -> roll > 0 ? Direction.UP : Direction.DOWN;
            case UP -> roll > 0 ? Direction.WEST : Direction.EAST;
            case WEST -> roll > 0 ? Direction.DOWN : Direction.UP;
            case DOWN -> roll > 0 ? Direction.EAST : Direction.WEST;
            default -> direction;
        };
    }

    static StructureTemplate of(StructureTemplate original, ResourceLocation id, int roll) {
        if (roll == 0) return original;
        return CACHE.computeIfAbsent(id + "@" + roll, key -> {
            var lookup = BuiltInRegistries.BLOCK.asLookup();
            var source = original.save(new CompoundTag());
            var palette = source.getList("palette", Tag.TAG_COMPOUND);
            var states = new ListTag();
            for (Tag entry : palette) {
                BlockState state = NbtUtils.readBlockState(lookup, (CompoundTag) entry);
                BlockState turned = roll(state, roll);
                states.add(NbtUtils.writeBlockState(turned == null ? Blocks.AIR.defaultBlockState() : turned));
            }
            var blocks = new ListTag();
            for (Tag entry : source.getList("blocks", Tag.TAG_COMPOUND)) {
                var block = ((CompoundTag) entry).copy();
                var at = block.getList("pos", Tag.TAG_INT);
                var to = rolled(new BlockPos(at.getInt(0), at.getInt(1), at.getInt(2)), roll);
                var pos = new ListTag();
                pos.add(net.minecraft.nbt.IntTag.valueOf(to.getX()));
                pos.add(net.minecraft.nbt.IntTag.valueOf(to.getY()));
                pos.add(net.minecraft.nbt.IntTag.valueOf(to.getZ()));
                block.put("pos", pos);
                blocks.add(block);
            }
            source.put("palette", states);
            source.put("blocks", blocks);
            source.put("entities", new ListTag());
            var template = new StructureTemplate();
            template.load(lookup, source);
            return template;
        });
    }

    /** The block as it looks after the quarter turn, or null where nothing of it can be kept. */
    private static BlockState roll(BlockState state, int roll) {
        var block = state.getBlock();
        if (block instanceof StairBlock && state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.X) {
            // A stair is a square with one quarter missing; the missing quarter turns with the container.
            int mx = -state.getValue(BlockStateProperties.HORIZONTAL_FACING).getStepX();
            int my = state.getValue(BlockStateProperties.HALF) == Half.TOP ? -1 : 1;
            int nx = roll > 0 ? -my : my, ny = roll > 0 ? mx : -mx;
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, -nx > 0 ? Direction.EAST : Direction.WEST)
                    .setValue(BlockStateProperties.HALF, -ny > 0 ? Half.TOP : Half.BOTTOM)
                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
        }
        if (block instanceof TrapDoorBlock) {
            var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (facing.getAxis() == Direction.Axis.Z) return state;
            // The panel lies against the top or bottom when closed and against the side opposite its facing when open.
            Direction panel = state.getValue(BlockStateProperties.OPEN) ? facing.getOpposite()
                    : state.getValue(BlockStateProperties.HALF) == Half.TOP ? Direction.UP : Direction.DOWN;
            panel = rolled(panel, roll);
            if (panel.getAxis() == Direction.Axis.Y)
                return state.setValue(BlockStateProperties.OPEN, false).setValue(BlockStateProperties.HALF, panel == Direction.UP ? Half.TOP : Half.BOTTOM);
            return state.setValue(BlockStateProperties.OPEN, true).setValue(BlockStateProperties.HORIZONTAL_FACING, panel.getOpposite());
        }
        if (block instanceof WallBlock)
            return state.setValue(WallBlock.EAST_WALL, WallSide.NONE).setValue(WallBlock.WEST_WALL, WallSide.NONE).setValue(WallBlock.UP, true);
        if (block instanceof LeverBlock || block instanceof WallSignBlock)
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z ? state : null;
        if (state.hasProperty(BlockStateProperties.FACING)) return state.setValue(BlockStateProperties.FACING, rolled(state.getValue(BlockStateProperties.FACING), roll));
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            var axis = state.getValue(BlockStateProperties.AXIS);
            return state.setValue(BlockStateProperties.AXIS, axis == Direction.Axis.X ? Direction.Axis.Y : axis == Direction.Axis.Y ? Direction.Axis.X : axis);
        }
        return state;
    }
}
