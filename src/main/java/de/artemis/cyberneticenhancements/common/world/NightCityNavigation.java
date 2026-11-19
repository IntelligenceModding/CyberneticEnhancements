package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.CityNavigationPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NightCityNavigation {
    static final String TAG = "cyberneticenhancements.city_navigation";
    private static final ResourceKey<Structure> CITY = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "night_city"));
    private static final Map<UUID, Long> PENDING = new LinkedHashMap<>();
    private static final Map<ServerPlayer, CityNavigationPayload> SENT = new WeakHashMap<>();

    private NightCityNavigation() {}

    public static void onLogin(ServerPlayer player) {
        reset(player);
        var state = state(player);
        if (!state.getBoolean("Searched")) {
            if (!state.contains("Origin")) {
                BlockPos origin = player.level().dimension().equals(Level.OVERWORLD) ? player.blockPosition() : player.server.overworld().getSharedSpawnPos();
                state.putLong("Origin", origin.asLong());
                save(player, state);
            }
            PENDING.put(player.getUUID(), player.server.overworld().getGameTime() + 40);
        }
        sync(player);
    }

    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (var player : server.getPlayerList().getPlayers()) checkArrival(player);
        long now = server.overworld().getGameTime();
        var iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { iterator.remove(); continue; }
            if (entry.getValue() > now) continue;
            iterator.remove();
            locate(player);
            break;
        }
    }

    static void locate(ServerPlayer player) {
        var state = state(player);
        if (state.getBoolean("Searched")) return;
        var level = player.server.overworld();
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(CITY).orElse(null);
        state.putBoolean("Searched", true);
        save(player, state);
        if (holder == null || !player.server.getWorldData().worldGenOptions().generateStructures()
                || level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).isEmpty()) return;
        BlockPos origin = BlockPos.of(state.getLong("Origin"));
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), origin, 100, false);
        if (found == null || !level.getWorldBorder().isWithinBounds(found.getFirst())) return;
        var chunkPos = new ChunkPos(found.getFirst());
        var chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
        var start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), holder.value(), chunk);
        if (start == null || !start.isValid()) return;
        mark(player, level.dimension().location().toString(), start.getBoundingBox());
        checkArrival(player);
        if (!capture(player).visited())
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.city.marked").withStyle(net.minecraft.ChatFormatting.AQUA));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.1F);
    }

    static void mark(ServerPlayer player, String dimension, BoundingBox bounds) {
        var state = state(player);
        if (state.contains("Pos")) return;
        state.putBoolean("Searched", true);
        state.putString("Dimension", dimension);
        state.putLong("Pos", bounds.getCenter().asLong());
        state.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        state.putBoolean("Enabled", true);
        save(player, state);
        sync(player);
    }

    public static void checkArrival(ServerPlayer player) {
        var state = state(player);
        if (!state.contains("Pos") || state.getBoolean("Visited") || !player.level().dimension().location().toString().equals(state.getString("Dimension"))) return;
        int[] bounds = state.getIntArray("Bounds");
        if (bounds.length != 6 || !new BoundingBox(bounds[0], bounds[1] - 4, bounds[2], bounds[3], bounds[4] + 4, bounds[5]).isInside(player.blockPosition())) return;
        state.putBoolean("Visited", true);
        de.artemis.cyberneticenhancements.common.journal.JournalEvents.enterCity(player, state.getString("Dimension") + ":" + bounds[0] + ":" + bounds[2], BlockPos.of(state.getLong("Pos")));
        state.putBoolean("Enabled", false);
        save(player, state);
        sync(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.city.arrived").withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        var state = state(player);
        if (!state.contains("Pos")) return;
        state.putBoolean("Enabled", enabled);
        save(player, state);
        sync(player);
    }

    public static CityNavigationPayload capture(ServerPlayer player) {
        var state = state(player);
        return !state.contains("Pos") ? CityNavigationPayload.EMPTY : new CityNavigationPayload(true, state.getString("Dimension"),
                BlockPos.of(state.getLong("Pos")), state.getBoolean("Visited"), state.getBoolean("Enabled"));
    }

    public static void sync(ServerPlayer player) {
        var payload = capture(player);
        if (!payload.equals(SENT.put(player, payload))) PacketDistributor.sendToPlayer(player, payload);
    }

    public static void reset(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        SENT.remove(player);
    }

    public static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(TAG)) clone.getPersistentData().put(TAG, original.getPersistentData().getCompound(TAG).copy());
    }

    public static void onStop(ServerStoppedEvent event) { PENDING.clear(); SENT.clear(); }
    private static CompoundTag state(Player player) { return player.getPersistentData().getCompound(TAG); }
    private static void save(Player player, CompoundTag state) { player.getPersistentData().put(TAG, state); }
}
