package de.artemis.cyberneticenhancements.common.block;

import de.artemis.cyberneticenhancements.common.menu.RipperStationMenu;
import de.artemis.cyberneticenhancements.common.blockentity.RipperStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class RipperStationBlock extends Block implements EntityBlock {
    public RipperStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RipperStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new RipperStationMenu(containerId, inventory, pos),
                    this.getName()
            );
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
