package de.artemis.cyberneticenhancements.common.fasttravel;

import de.artemis.cyberneticenhancements.common.config.FastTravelConfig;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.network.FastTravelEffectPayload;
import de.artemis.cyberneticenhancements.common.network.FastTravelOpenPayload;
import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload.Point;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Fast travel between terminals: pick a node on the map, pay, dissolve into a hologram, and arrive as one at the other end. */
public final class FastTravel {
    /** How long the dissolving takes, in ticks; the player is moved when it is over. */
    public static final int DELAY = 24;
    private static final double REACH = 8;
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.2F, 0.75F, 1F), 0.8F);
    private static final Map<UUID, Trip> TRIPS = new HashMap<>();

    private record Trip(String dimension, BlockPos target, String name, long arrives, int cost) {}

    private FastTravel() {}

    private static void message(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.fast_travel." + key, args), true);
    }

    private static void denied(ServerPlayer player, String key, Object... args) {
        message(player, key, args);
        player.playNotifySound(ModSounds.FAST_TRAVEL_DENIED.get(), SoundSource.BLOCKS, 0.8F, 1F);
    }

    /** The player used a terminal: it joins their network if it is new, and the travel map opens. */
    public static void use(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!FastTravelConfig.enabled()) { denied(player, "disabled"); return; }
        var points = FastTravelPoints.get(level.getServer());
        String dimension = level.dimension().location().toString();
        var point = points.find(dimension, pos);
        if (point == null) point = points.add(dimension, pos, null);
        if (points.discover(player.getUUID(), point)) {
            points.sync(player);
            message(player, "linked", point.name());
            level.playSound(null, pos, ModSounds.FAST_TRAVEL_ACTIVATE.get(), SoundSource.BLOCKS, 1F, 1F);
            // A terminal has to be activated in person once before it can be used.
            return;
        }
        PacketDistributor.sendToPlayer(player, new FastTravelOpenPayload(pos, FastTravelConfig.cost()));
    }

    public static void request(ServerPlayer player, String dimension, BlockPos target) {
        if (!FastTravelConfig.enabled()) { denied(player, "disabled"); return; }
        if (TRIPS.containsKey(player.getUUID()) || !player.isAlive()) return;
        var server = player.server;
        var points = FastTravelPoints.get(server);
        String here = player.level().dimension().location().toString();
        Point origin = null;
        for (var point : points.all())
            if (point.dimension().equals(here) && point.pos().getCenter().distanceToSqr(player.position()) <= REACH * REACH
                    && (origin == null || point.pos().distSqr(player.blockPosition()) < origin.pos().distSqr(player.blockPosition()))) origin = point;
        if (origin == null) { denied(player, "far"); return; }
        var destination = points.find(dimension, target);
        if (destination == null || !points.discovered(player.getUUID(), destination) || destination.equals(origin)) { denied(player, "unknown"); return; }
        var key = ResourceLocation.tryParse(dimension);
        if (key == null || server.getLevel(ResourceKey.create(Registries.DIMENSION, key)) == null) { denied(player, "unknown"); return; }
        int cost = FastTravelConfig.cost();
        if (!PlayerEurodollarManager.tryWithdraw(player, cost, "expense", "Fast travel to " + destination.name(), null, null)) {
            denied(player, "poor", cost);
            return;
        }
        var level = player.serverLevel();
        TRIPS.put(player.getUUID(), new Trip(dimension, destination.pos(), destination.name(), level.getGameTime() + DELAY, cost));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FastTravelEffectPayload(player.getId(), level.getGameTime(), true));
        play(level, player.position(), ModSounds.FAST_TRAVEL_DEPART.get(), 1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6F, 1.7F);
        message(player, "linking", destination.name());
    }

    public static void tick(ServerTickEvent.Post event) {
        if (TRIPS.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Trip>> it = TRIPS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            var trip = entry.getValue();
            var player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) { it.remove(); continue; }
            var level = player.serverLevel();
            long left = trip.arrives - level.getGameTime();
            if (left > 0) {
                if (left % 2 == 0) dissolve(level, player, 1 - left / (float) DELAY);
                continue;
            }
            it.remove();
            arrive(player, trip);
        }
    }

    /** Rings of light climbing the player while they fade. */
    private static void dissolve(ServerLevel level, ServerPlayer player, float progress) {
        double y = player.getY() + progress * player.getBbHeight();
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI / 5 + progress * 9;
            level.sendParticles(DUST, player.getX() + Math.cos(angle) * 0.6, y, player.getZ() + Math.sin(angle) * 0.6, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), y, player.getZ(), 2, 0.25, 0.1, 0.25, 0.02);
    }

    private static void arrive(ServerPlayer player, Trip trip) {
        var server = player.server;
        var key = ResourceLocation.tryParse(trip.dimension);
        var level = key == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
        var points = FastTravelPoints.get(server);
        if (level == null || points.find(trip.dimension, trip.target) == null) {
            // The terminal is gone, or its world: the trip is off and the money is back.
            PlayerEurodollarManager.add(player, trip.cost, "income", "Fast travel refund", null, null);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FastTravelEffectPayload(player.getId(), player.level().getGameTime(), false));
            denied(player, "unknown");
            return;
        }
        var from = player.position();
        play(player.serverLevel(), from, ModSounds.FAST_TRAVEL_ARRIVE.get(), 0.8F);
        var landing = landing(level, trip.target);
        player.teleportTo(level, landing.x, landing.y, landing.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FastTravelEffectPayload(player.getId(), level.getGameTime(), false));
        play(level, landing, ModSounds.FAST_TRAVEL_ARRIVE.get(), 1F);
        level.sendParticles(DUST, landing.x, landing.y + 1, landing.z, 40, 0.5, 0.9, 0.5, 0.02);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, landing.x, landing.y + 1, landing.z, 20, 0.4, 0.8, 0.4, 0.1);
        message(player, "arrived", trip.name);
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 1F);
    }

    /** Beside the terminal where there is room to stand, otherwise on top of it. */
    private static Vec3 landing(ServerLevel level, BlockPos terminal) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var feet = terminal.relative(direction);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) return Vec3.atBottomCenterOf(feet);
        }
        return new Vec3(terminal.getX() + 0.5, terminal.getY() + 0.75, terminal.getZ() + 0.5);
    }

    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { TRIPS.remove(event.getEntity().getUUID()); }

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FastTravelPoints.get(player.server).sync(player);
    }
}
