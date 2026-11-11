package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.Comparator;
import java.util.Optional;

public final class RelicCacheIntel {
    private static final String CACHE_COMPASS_TAG = "cyberneticenhancements.cache_compass";
    private RelicCacheIntel() {
    }

    public static Optional<BlockPos> nearest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var data = RelicCacheStructurePlacementSavedData.get(level);
        return data.cachePositions().stream().filter(pos -> {
            if (level.hasChunkAt(pos) && !(level.getBlockState(pos).getBlock() instanceof RelicCacheBlock)) {
                data.remove(pos);
                return false;
            }
            return !RelicCacheBlock.hasPlayerOpened(player, level, pos);
        }).min(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(player.blockPosition()))
                .thenComparingLong(BlockPos::asLong));
    }

    public static boolean provide(ServerPlayer player) {
        var target = nearest(player);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.cyberneticenhancements.cache_intel.none").withStyle(ChatFormatting.GRAY));
            return false;
        }
        BlockPos pos = target.get();
        var tier = RelicCacheStructurePlacementSavedData.get(player.serverLevel()).tier(pos);
        Component name = Component.translatable("block.cyberneticenhancements." + tier.name().toLowerCase(java.util.Locale.ROOT) + "_relic_cache");
        int distance = (int) Math.sqrt(pos.distSqr(player.blockPosition()));
        player.sendSystemMessage(Component.translatable("message.cyberneticenhancements.cache_intel.found",
                name, pos.getX(), pos.getY(), pos.getZ(), distance, player.level().dimension().location().toString())
                .withStyle(ChatFormatting.AQUA));
        boolean given = giveCompass(player, pos);
        player.sendSystemMessage(Component.translatable("message.cyberneticenhancements.cache_intel." + (given ? "given" : "already_tuned"))
                .withStyle(ChatFormatting.GRAY));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.3F);
        return true;
    }

    public static boolean giveCompass(ServerPlayer player, BlockPos pos) {
        GlobalPos target = GlobalPos.of(player.level().dimension(), pos);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.COMPASS)) continue;
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker != null && tracker.target().filter(target::equals).isPresent()) return false;
        }
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(target), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.cache_compass"));
        CompoundTag data = new CompoundTag();
        data.putBoolean(CACHE_COMPASS_TAG, true);
        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.getInventory().placeItemBackInInventory(compass);
        player.getInventory().setChanged();
        return true;
    }

    public static void removeCompassForCache(ServerPlayer player, BlockPos pos) {
        GlobalPos target = GlobalPos.of(player.level().dimension(), pos);
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.COMPASS)) continue;
            boolean issued = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(CACHE_COMPASS_TAG)
                    || Component.translatable("item.cyberneticenhancements.cache_compass").equals(stack.get(DataComponents.CUSTOM_NAME));
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (issued && tracker != null && tracker.target().filter(target::equals).isPresent()) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }
}
