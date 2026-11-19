package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The patient quest's beacon: right-click anywhere to log where you stand. */
public final class SignalBeaconItem extends Item {
    public SignalBeaconItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !PlayerQuestManager.useSignalBeacon(serverPlayer, stack))
            return InteractionResultHolder.pass(stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
