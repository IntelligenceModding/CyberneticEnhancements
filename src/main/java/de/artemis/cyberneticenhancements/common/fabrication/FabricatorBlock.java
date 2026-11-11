package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class FabricatorBlock extends Block implements EntityBlock {
    public FabricatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricatorBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.FABRICATOR.get() ? (l, p, s, e) -> FabricatorBlockEntity.tick(l, p, s, (FabricatorBlockEntity) e) : null;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server)
            server.openMenu(new SimpleMenuProvider((id, inv, p) -> new FabricatorMenu(id, inv, pos), getName()), b -> b.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(FabricationContent.part("precision_assembly_module")) || stack.is(FabricationContent.part("high_spec_fabrication_module"))) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof FabricatorBlockEntity station) {
                boolean upgraded = station.upgrade(stack);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(upgraded ? "Fabricator upgraded" : "Apply upgrades in order; finish fabrication first."), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof FabricatorBlockEntity station) {
            Containers.dropContents(level, pos, station);
            if (station.capability >= 2)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(FabricationContent.part("precision_assembly_module")));
            if (station.capability >= 3)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(FabricationContent.part("high_spec_fabrication_module")));
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
