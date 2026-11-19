package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.network.BannerWaypointsPayload;
import de.artemis.cyberneticenhancements.common.network.BannerWaypointsPayload.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;

public final class BannerNavigation {
    private static final String ROOT = "cyberneticenhancements.banner_waypoints";
    private static final int LIMIT = 128;
    private static final Map<ServerPlayer, BannerWaypointsPayload> SENT = new WeakHashMap<>();
    private static final Map<ServerPlayer, MapClick> MAP_CLICKS = new WeakHashMap<>();

    private BannerNavigation() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || !(player.level().getBlockEntity(event.getPos()) instanceof BannerBlockEntity banner)) return;
        if (event.getItemStack().is(Items.FILLED_MAP)) {
            MapId id = event.getItemStack().get(DataComponents.MAP_ID);
            if (id != null) MAP_CLICKS.put(player, new MapClick(id, player.level().dimension().location().toString(), event.getPos().immutable()));
        } else if (event.getHand() == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty() && banner.hasCustomName()) {
            String result = toggle(player, event.getPos());
            if (result.equals("invalid")) return;
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.banner." + result, banner.getName()), true);
            player.playNotifySound(SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.35F, result.equals("removed") ? 0.8F : 1.15F);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static String toggle(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof BannerBlockEntity banner)) return "invalid";
        Waypoint point = waypoint(player.level().dimension().location().toString(), banner);
        if (point == null) return "invalid";
        State state = read(player);
        String result;
        if (state.points.remove(point.id()) != null) result = "removed";
        else {
            if (state.points.size() >= LIMIT) return "full";
            state.points.put(point.id(), point);
            result = "marked";
        }
        save(player, state);
        sync(player);
        return result;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MapClick click = MAP_CLICKS.remove(player);
        if (click == null && player.tickCount % 20 != 0) return;
        State state = read(player);
        boolean changed = false;
        if (click != null) {
            var data = MapItem.getSavedData(click.map, player.level());
            if (data != null && data.dimension.location().toString().equals(click.dimension)) {
                String key = click.dimension + "/" + click.pos.asLong();
                var selected = data.getBanners().stream().filter(banner -> banner.pos().equals(click.pos)).findFirst();
                if (selected.isPresent() && selected.get().name().isPresent()) {
                    var banner = selected.get();
                    String name = cleanName(banner.name().get().getString());
                    if (!name.isEmpty() && (state.points.containsKey(key) || state.points.size() < LIMIT)) {
                        var point = new Waypoint(click.dimension, click.pos, name, 0xFF000000 | banner.color().getTextColor());
                        changed |= !point.equals(state.points.put(key, point));
                    }
                } else changed |= state.points.remove(key) != null;
            }
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            MapId id = stack.is(Items.FILLED_MAP) ? stack.get(DataComponents.MAP_ID) : null;
            if (id == null || state.maps.contains(id.id()) || state.maps.size() >= LIMIT) continue;
            var data = MapItem.getSavedData(stack, player.level());
            if (data == null) continue;
            state.maps.add(id.id());
            changed = true;
            for (var banner : data.getBanners()) {
                if (banner.name().isEmpty() || state.points.size() >= LIMIT) continue;
                String name = cleanName(banner.name().get().getString());
                if (name.isEmpty()) continue;
                var point = new Waypoint(data.dimension.location().toString(), banner.pos(), name, 0xFF000000 | banner.color().getTextColor());
                state.points.putIfAbsent(point.id(), point);
            }
        }
        var iterator = state.points.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Waypoint point = entry.getValue();
            var dimension = ResourceLocation.tryParse(point.dimension());
            var level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null || !level.hasChunkAt(point.pos())) continue;
            Waypoint current = level.getBlockEntity(point.pos()) instanceof BannerBlockEntity banner ? waypoint(point.dimension(), banner) : null;
            if (current == null) {
                iterator.remove();
                changed = true;
            } else if (!current.equals(point)) {
                entry.setValue(current);
                changed = true;
            }
        }
        if (changed) save(player, state);
        sync(player);
    }

    private static Waypoint waypoint(String dimension, BannerBlockEntity banner) {
        if (banner.getCustomName() == null) return null;
        String name = cleanName(banner.getCustomName().getString());
        return name.isEmpty() ? null : new Waypoint(dimension, banner.getBlockPos(), name, 0xFF000000 | banner.getBaseColor().getTextColor());
    }

    private static String cleanName(String name) {
        String cleaned = name.replaceAll("\\p{Cntrl}", "").strip();
        return cleaned.substring(0, Math.min(128, cleaned.length()));
    }

    public static BannerWaypointsPayload capture(ServerPlayer player) {
        return new BannerWaypointsPayload(java.util.List.copyOf(read(player).points.values()));
    }

    public static boolean deleteMarker(ServerPlayer player, String id) {
        var state = read(player);
        if (state.points.remove(id) == null) return false;
        save(player, state);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        var snapshot = capture(player);
        if (!snapshot.equals(SENT.put(player, snapshot))) PacketDistributor.sendToPlayer(player, snapshot);
    }

    public static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND))
            clone.getPersistentData().put(ROOT, original.getPersistentData().getCompound(ROOT).copy());
    }

    public static void reset(ServerPlayer player) {
        SENT.remove(player);
        MAP_CLICKS.remove(player);
    }

    private static State read(Player player) {
        var state = new State();
        var root = player.getPersistentData().getCompound(ROOT);
        for (var tag : root.getList("Points", Tag.TAG_COMPOUND)) {
            if (state.points.size() >= LIMIT) break;
            var entry = (CompoundTag) tag;
            String dimension = entry.getString("Dimension"), name = cleanName(entry.getString("Name"));
            if (ResourceLocation.tryParse(dimension) == null || name.isEmpty()) continue;
            var point = new Waypoint(dimension, BlockPos.of(entry.getLong("Pos")), name, 0xFF000000 | entry.getInt("Color"));
            state.points.put(point.id(), point);
        }
        for (int id : root.getIntArray("ImportedMaps")) if (state.maps.size() < LIMIT) state.maps.add(id);
        return state;
    }

    private static void save(Player player, State state) {
        var root = new CompoundTag();
        var points = new ListTag();
        for (var point : state.points.values()) {
            var entry = new CompoundTag();
            entry.putString("Dimension", point.dimension());
            entry.putLong("Pos", point.pos().asLong());
            entry.putString("Name", point.name());
            entry.putInt("Color", point.color());
            points.add(entry);
        }
        root.put("Points", points);
        root.putIntArray("ImportedMaps", state.maps.stream().mapToInt(Integer::intValue).toArray());
        player.getPersistentData().put(ROOT, root);
    }

    private static final class State {
        private final LinkedHashMap<String, Waypoint> points = new LinkedHashMap<>();
        private final LinkedHashSet<Integer> maps = new LinkedHashSet<>();
    }

    private record MapClick(MapId map, String dimension, BlockPos pos) {
    }
}
