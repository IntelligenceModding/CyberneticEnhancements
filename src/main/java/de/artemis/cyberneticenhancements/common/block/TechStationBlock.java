package de.artemis.cyberneticenhancements.common.block;

import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.menu.TechStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TechStationBlock extends Block implements EntityBlock {
    public TechStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return !level.isClientSide && type == de.artemis.cyberneticenhancements.common.registry.ModBlockEntities.TECH_STATION.get()
                ? (l, p, s, e) -> TechStationBlockEntity.tick(l, p, s, (TechStationBlockEntity) e) : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (level.getBlockEntity(pos) instanceof TechStationBlockEntity station) station.updateRedstone();
        super.neighborChanged(state, level, pos, block, fromPos, moving);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof TechStationBlockEntity station) {
            station.dropLegacyRecovery();
            net.minecraft.world.Containers.dropContents(level, pos, station);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new TechStationMenu(containerId, inventory, pos),
                    this.getName()
            );
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
