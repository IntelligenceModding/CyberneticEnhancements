package de.artemis.cyberneticenhancements.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DropoffPointBlock extends IronQuestCrateBlock {
    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(DropoffPointBlock::new);

    public DropoffPointBlock(Properties properties) { super(properties); }
    @Override
    public MapCodec<BarrelBlock> codec() { return CODEC; }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DropoffPointBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof DropoffPointBlockEntity point) point.bind(player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DropoffPointBlockEntity point && !point.bind(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.dropoff.other_owner"), true);
            return InteractionResult.CONSUME;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof DropoffPointBlockEntity point && point.owner() != null) {
            QuestRewardDeliveries.get(serverLevel.getServer()).unbind(point.owner(), serverLevel.dimension(), pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
