package de.artemis.cyberneticenhancements.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.cyberneticenhancements.common.blockentity.FastTravelBlockEntity;
import de.artemis.cyberneticenhancements.common.fasttravel.FastTravel;
import de.artemis.cyberneticenhancements.common.fasttravel.FastTravelPoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fast travel terminal: an enchanting table with a hologram where the book was. Using it opens the map of the terminals found so far. */
public final class FastTravelBlock extends BaseEntityBlock {
    public static final MapCodec<FastTravelBlock> CODEC = simpleCodec(FastTravelBlock::new);
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 12, 16);

    public FastTravelBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FastTravelBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return;
        var points = FastTravelPoints.get(server.getServer());
        // A renamed item names the terminal.
        var point = points.add(server.dimension().location().toString(), pos, stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : null);
        if (placer instanceof ServerPlayer player && points.discover(player.getUUID(), point)) points.sync(player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) FastTravel.use(serverPlayer, server, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel server) {
            var points = FastTravelPoints.get(server.getServer());
            points.remove(server.dimension().location().toString(), pos);
            points.syncAll(server.getServer());
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
