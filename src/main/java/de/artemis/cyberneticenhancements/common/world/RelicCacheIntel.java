package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload;
import de.artemis.cyberneticenhancements.common.quest.QuestNavigation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public final class RelicCacheIntel {
    private static final String CACHE_COMPASS_TAG = "cyberneticenhancements.cache_compass";
    private static final String MARKERS_TAG = "cyberneticenhancements.cache_navigation";
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
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.cache_intel.none").withStyle(ChatFormatting.GRAY));
            return false;
        }
        BlockPos pos = target.get();
        var tier = RelicCacheStructurePlacementSavedData.get(player.serverLevel()).tier(pos);
        Component name = Component.translatable("block.cyberneticenhancements." + tier.name().toLowerCase(java.util.Locale.ROOT) + "_relic_cache");
        int distance = (int) Math.sqrt(pos.distSqr(player.blockPosition()));
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.cache_intel.found",
                name, pos.getX(), pos.getY(), pos.getZ(), distance, player.level().dimension().location().toString())
                .withStyle(ChatFormatting.AQUA));
        boolean added = mark(player, GlobalPos.of(player.level().dimension(), pos));
        QuestNavigation.sync(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.cache_intel." + (added ? "marked" : "already_marked"))
                .withStyle(ChatFormatting.GRAY));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.3F);
        return true;
    }

    private static boolean mark(ServerPlayer player, GlobalPos target) {
        ListTag markers = player.getPersistentData().getList(MARKERS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < markers.size(); index++) {
            if (target.equals(readTarget(markers.getCompound(index)))) return false;
        }
        CompoundTag marker = new CompoundTag();
        marker.putString("Dimension", target.dimension().location().toString());
        marker.putLong("Pos", target.pos().asLong());
        markers.add(marker);
        player.getPersistentData().put(MARKERS_TAG, markers);
        return true;
    }

    public static void onCacheInteracted(ServerPlayer player, BlockPos pos) {
        migrateLegacyCompasses(player);
        GlobalPos target = GlobalPos.of(player.level().dimension(), pos);
        ListTag markers = player.getPersistentData().getList(MARKERS_TAG, Tag.TAG_COMPOUND);
        boolean removed = markers.removeIf(tag -> target.equals(readTarget((CompoundTag) tag)));
        if (removed) {
            player.getPersistentData().put(MARKERS_TAG, markers);
            QuestNavigation.sync(player);
        }
    }

    public static List<QuestNavigationPayload.Target> navigationTargets(ServerPlayer player) {
        migrateLegacyCompasses(player);
        ListTag markers = player.getPersistentData().getList(MARKERS_TAG, Tag.TAG_COMPOUND);
        List<QuestNavigationPayload.Target> targets = new ArrayList<>();
        boolean changed = false;
        for (int index = markers.size() - 1; index >= 0; index--) {
            GlobalPos target = readTarget(markers.getCompound(index));
            ServerLevel level = target == null ? null : player.server.getLevel(target.dimension());
            var tier = level == null ? null : RelicCacheStructurePlacementSavedData.get(level).tier(target.pos());
            if (tier == null || RelicCacheBlock.hasPlayerOpened(player, level, target.pos())
                    || (level.hasChunkAt(target.pos()) && !(level.getBlockState(target.pos()).getBlock() instanceof RelicCacheBlock))) {
                markers.remove(index);
                changed = true;
                continue;
            }
            String title = Component.translatable("block.cyberneticenhancements." + tier.name().toLowerCase(java.util.Locale.ROOT) + "_relic_cache").getString();
            targets.add(new QuestNavigationPayload.Target("cache:" + target.dimension().location() + ":" + target.pos().asLong(),
                    title, target.dimension().location().toString(), target.pos(), false));
        }
        if (changed) player.getPersistentData().put(MARKERS_TAG, markers);
        return targets;
    }

    public static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(MARKERS_TAG))
            clone.getPersistentData().put(MARKERS_TAG, original.getPersistentData().getList(MARKERS_TAG, Tag.TAG_COMPOUND).copy());
    }

    public static boolean deleteMarker(ServerPlayer player, String id) {
        var markers = player.getPersistentData().getList(MARKERS_TAG, Tag.TAG_COMPOUND);
        boolean removed = markers.removeIf(tag -> {
            var target = readTarget((CompoundTag) tag);
            return target != null && id.equals("cache:" + target.dimension().location() + ":" + target.pos().asLong());
        });
        if (removed) {
            player.getPersistentData().put(MARKERS_TAG, markers);
            QuestNavigation.sync(player);
        }
        return removed;
    }

    private static GlobalPos readTarget(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        return dimension == null || !tag.contains("Pos", Tag.TAG_LONG) ? null
                : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(tag.getLong("Pos")));
    }

    private static void migrateLegacyCompasses(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (migrateLegacyCompass(player, stack)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (migrateLegacyCompass(player, player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (player.containerMenu != player.inventoryMenu && migrateLegacyCompass(player, player.inventoryMenu.getCarried())) {
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static boolean migrateLegacyCompass(ServerPlayer player, ItemStack stack) {
        if (!stack.is(Items.COMPASS)) return false;
        boolean issued = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(CACHE_COMPASS_TAG)
                || Component.translatable("item.cyberneticenhancements.cache_compass").equals(stack.get(DataComponents.CUSTOM_NAME));
        if (!issued) return false;
        var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null) tracker.target().ifPresent(target -> mark(player, target));
        return true;
    }
}
