package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload;
import de.artemis.cyberneticenhancements.common.quest.QuestNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Arrays;
import java.util.List;

public final class ChromiumDiscovery {
    private static final String MARKERS = "cyberneticenhancements.chromium_discoveries";
    private static final String DELETED = "cyberneticenhancements.chromium_deleted_markers";

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) discoverLookedAtOre(player);
    }

    /** Server-side block ray trace requires a visible ore face within the player's actual reach. */
    public static boolean discoverLookedAtOre(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Level.OVERWORLD)) return false;
        var hit = player.pick(player.blockInteractionRange(), 1, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) return false;
        var pos = blockHit.getBlockPos();
        var state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChromiumOreBlock) || !state.getValue(ChromiumOreBlock.LARGE_DEPOSIT)) return false;
        var center = ChromiumLargeVeins.owner(player.serverLevel().getSeed(), pos);
        // Earlier marked deposits were confined to their anchor chunk. Preserve discovery in existing saves.
        if (center == null) center = ChromiumGeology.deposit(player.serverLevel().getSeed(), pos.getX() >> 4, pos.getZ() >> 4);
        if (center == null) return false;
        if (!mark(player, center, false)) return false;
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.chromium.discovered"), true);
        PlayerJournal.recordAt(player, center, "world", "chromium_discovered");
        de.artemis.cyberneticenhancements.common.streetcred.StreetCred.gain(player, de.artemis.cyberneticenhancements.common.streetcred.StreetCredRewards.CHROMIUM_DEPOSIT);
        return true;
    }

    /** Explicit quest rewards can restore a previously deleted marker without loading its chunk. */
    public static boolean rewardSurvey(ServerPlayer player, BlockPos center) {
        if (center == null || !center.equals(ChromiumGeology.deposit(player.server.overworld().getSeed(), center.getX() >> 4, center.getZ() >> 4))) return false;
        if (!mark(player, center, true)) return false;
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.chromium.survey_reward"));
        PlayerJournal.record(player, "quests", "chromium_survey_reward", center.getX(), center.getY(), center.getZ());
        return true;
    }

    private static boolean mark(ServerPlayer player, BlockPos center, boolean reward) {
        long[] known = player.getPersistentData().getLongArray(MARKERS);
        for (long marker : known) if (marker == center.asLong()) return false;
        long[] deleted = player.getPersistentData().getLongArray(DELETED);
        if (!reward) {
            for (long marker : deleted) if (marker == center.asLong()) return false;
        } else player.getPersistentData().putLongArray(DELETED, Arrays.stream(deleted).filter(value -> value != center.asLong()).toArray());
        long[] updated = Arrays.copyOf(known, known.length + 1);
        updated[known.length] = center.asLong();
        player.getPersistentData().putLongArray(MARKERS, updated);
        QuestNavigation.sync(player);
        return true;
    }

    public static List<QuestNavigationPayload.Target> navigationTargets(ServerPlayer player) {
        return Arrays.stream(player.getPersistentData().getLongArray(MARKERS)).mapToObj(value ->
                new QuestNavigationPayload.Target("chromium:" + value,
                        Component.translatable("quest.cyberneticenhancements.target.chromium_deposit").getString(),
                        Level.OVERWORLD.location().toString(), BlockPos.of(value), false)).toList();
    }

    public static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(MARKERS))
            clone.getPersistentData().putLongArray(MARKERS, original.getPersistentData().getLongArray(MARKERS).clone());
        if (original.getPersistentData().contains(DELETED))
            clone.getPersistentData().putLongArray(DELETED, original.getPersistentData().getLongArray(DELETED).clone());
    }

    public static boolean deleteMarker(ServerPlayer player, String id) {
        long[] known = player.getPersistentData().getLongArray(MARKERS);
        long[] kept = Arrays.stream(known).filter(value -> !id.equals("chromium:" + value)).toArray();
        if (kept.length == known.length) return false;
        long removed = Arrays.stream(known).filter(value -> id.equals("chromium:" + value)).findFirst().orElseThrow();
        long[] deleted = player.getPersistentData().getLongArray(DELETED);
        long[] updated = Arrays.copyOf(deleted, deleted.length + 1); updated[deleted.length] = removed;
        player.getPersistentData().putLongArray(DELETED, updated);
        player.getPersistentData().putLongArray(MARKERS, kept);
        QuestNavigation.sync(player);
        return true;
    }

    private ChromiumDiscovery() { }
}
