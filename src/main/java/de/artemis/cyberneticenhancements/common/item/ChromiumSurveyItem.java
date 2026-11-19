package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.world.ChromiumGeology;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A reusable geological lead; using it never places ore or loads distant chunks. */
public final class ChromiumSurveyItem extends Item {
    public ChromiumSurveyItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            var world = serverPlayer.server.overworld();
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            var pos = ChromiumGeology.nearest(world.getSeed(), player.blockPosition(), 64, 1024);
            // Store one prospect per report, so repeated use is a useful reminder rather than an ore locator exploit.
            if (data.contains("ChromiumProspect")) pos = net.minecraft.core.BlockPos.of(data.getLong("ChromiumProspect"));
            if (pos != null) {
                data.putLong("ChromiumProspect", pos.asLong()); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                var message = Component.translatable("message.cyberneticenhancements.chromium.prospect", pos.getX(), pos.getY(), pos.getZ());
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, message);
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(serverPlayer, "exploration", "chromium_prospect", message);
            } else de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.chromium.none"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
