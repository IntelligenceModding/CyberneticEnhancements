package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class QuestRecovery {
    static final int GRACE_TICKS = 30 * 20;
    private static final Map<ServerPlayer, Map<String, Blocked>> BLOCKED = new WeakHashMap<>();
    private record Blocked(String reason, int since) {}
    private QuestRecovery() {}

    static ServerLevel level(ServerPlayer player, String dimension) {
        var id = ResourceLocation.tryParse(dimension);
        return id == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    static String immediateIssue(ServerPlayer player, QuestContract contract) {
        if (FixerIdentitySavedData.get(player.serverLevel()).isDeceased(contract.issuerId())) return "issuer_dead";
        if (contract.isReady()) return null;
        if (contract.type() == QuestType.RECOVERY)
            return QuestDimension.of(contract.dimensionId()) != QuestDimension.STANDARD && level(player, contract.dimensionId()) == null
                    ? "dimension_missing" : null;
        if (contract.targetPos() == null) return contract.type() == QuestType.ELIMINATION ? null : "site_missing";
        var level = level(player, contract.dimensionId());
        if (level == null) return "dimension_missing";
        if (!level.isInWorldBounds(contract.targetPos()) || !level.getWorldBorder().isWithinBounds(contract.targetPos())) return "outside_world";
        return null;
    }

    static String checkSite(ServerPlayer player, QuestContract contract) {
        if (player.tickCount % 20 != 0) return null;
        var checks = BLOCKED.computeIfAbsent(player, ignored -> new HashMap<>());
        var level = level(player, contract.dimensionId());
        var pos = contract.targetPos();
        if (contract.isReady() || contract.type() == QuestType.EXTRACTION && contract.returnItemCollected()
                || pos == null || level == null || player.level() != level || !player.blockPosition().closerThan(pos, 80)
                || !level.hasChunkAt(pos)) {
            checks.remove(contract.id());
            return null;
        }
        String reason = switch (contract.type()) {
            case DELIVERY, EXTRACTION -> level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate
                    && level.getBlockState(pos).is(ModBlocks.IRON_QUEST_CRATE.get()) && crate.questId().equals(contract.id())
                    && level.getBlockState(pos.above()).canBeReplaced() ? null : "site_blocked";
            case BREACH -> cacheIssue(player, contract, level);
            case INVESTIGATION -> !contract.chromiumSurvey() && contract.investigation() != null && contract.investigation().located() ? cacheIssue(player, contract, level) : null;
            case CYBERPSYCHO -> {
                if (contract.destinationReached() || CyberpsychoEncounter.get(level).death(contract.id()) != null) yield null;
                if (level.getEntity(contract.psychoId()) != null) yield null;
                yield level.hasChunksAt(pos.offset(-80, 0, -80), pos.offset(80, 0, 80)) ? "target_missing" : null;
            }
            case ELIMINATION -> {
                int range = Math.max(32, contract.areaRadius());
                if (!level.hasChunksAt(pos.offset(-range, 0, -range), pos.offset(range, 0, range))) yield null;
                yield level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(pos).inflate(range),
                        mob -> mob.isAlive() && mob.getType() == contract.eliminationTarget()).isEmpty() ? "target_missing" : null;
            }
            case RECOVERY, LOCATE, SALVAGE -> null; // A missing patient is simply laid out again when the player comes near.
        };
        if (reason == null) {
            checks.remove(contract.id());
            return null;
        }
        var previous = checks.get(contract.id());
        if (previous == null || !previous.reason.equals(reason)) {
            checks.put(contract.id(), new Blocked(reason, player.tickCount));
            return null;
        }
        return player.tickCount - previous.since >= GRACE_TICKS ? reason : null;
    }

    private static String cacheIssue(ServerPlayer player, QuestContract contract, ServerLevel level) {
        return level.getBlockEntity(contract.targetPos()) instanceof RelicCacheBlockEntity cache
                && cache.isBoundToQuest(player.getUUID(), contract.id()) ? null : "site_blocked";
    }

    static void retain(ServerPlayer player, Set<String> ids) {
        var checks = BLOCKED.get(player);
        if (checks == null) return;
        checks.keySet().retainAll(ids);
        if (checks.isEmpty()) BLOCKED.remove(player);
    }

    public static boolean cleanupRetiredSite(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate
                && level.getBlockState(pos).is(ModBlocks.IRON_QUEST_CRATE.get()) && !crate.questId().isEmpty() && QuestItemVersions.get(level).retired(crate.questId())) {
            cleanupContainer(level, pos, crate.questId());
            return true;
        }
        if (blockEntity instanceof RelicCacheBlockEntity cache && cache.isQuestCache() && !cache.isAnimating()
                && QuestItemVersions.get(level).retired(cache.questId())) {
            for (var player : level.players())
                if (player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu menu && menu.isForBlock(pos)) player.closeContainer();
            level.removeBlock(pos, false);
            return true;
        }
        return false;
    }

    static void cleanupContainer(ServerLevel level, net.minecraft.core.BlockPos pos, String id) {
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.IRON_QUEST_CRATE.get())
                || !(level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate)
                || !crate.questId().equals(id)) return;
        for (int slot = 0; slot < crate.getContainerSize(); slot++)
            if (QuestItemVersions.key(crate.getItem(slot)).startsWith(id + "/")) crate.setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
        for (var player : level.players())
            if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu && menu.getContainer() == crate) player.closeContainer();
        level.removeBlock(pos, false);
    }
}
