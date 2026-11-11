package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareBalance;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.menu.RipperStationMenu;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.network.SandevistanSnapshotPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SandevistanManager {
    private static final String DATA_KEY = "cyberneticenhancements.sandevistan";
    private static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "sandevistan_immune"));
    private static final Map<MinecraftServer, ServerState> SERVERS = new ConcurrentHashMap<>();

    private SandevistanManager() {
    }

    private static ServerState state(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.server, ignored -> new ServerState());
    }

    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    private static CompoundTag data(Player player) { return player.getPersistentData().getCompound(DATA_KEY); }

    public static boolean canActivate(Player player, String id) {
        if (!(player instanceof ServerPlayer)) return false;
        String problem = null;
        ItemStack stack = new PlayerCyberwareInventory(player).getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isFallFlying() || player.isSleeping()) {
            problem = "unavailable_pose";
        } else if (player.containerMenu instanceof RipperStationMenu) {
            problem = "maintenance_open";
        } else if (!(stack.getItem() instanceof CyberwareItem item) || !id.equals(item.getDefinition().id())
                || CyberwareConditionHelper.getIntegrityRatio(stack, item.getDefinition()) <= 0) {
            problem = "repair_required";
        } else if (recoveryRemaining(player) > 0) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.ability.cooldown",
                    (recoveryRemaining(player) + 19) / 20), true);
            return false;
        }
        if (problem != null) player.displayClientMessage(Component.translatable("message.cyberneticenhancements.sandevistan." + problem), true);
        return problem == null;
    }

    public static long adjustedDuration(Player player, long duration) {
        ItemStack stack = new PlayerCyberwareInventory(player).getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
        double condition = stack.getItem() instanceof CyberwareItem item ? CyberwareConditionHelper.getIntegrityRatio(stack, item.getDefinition()) : 0;
        long upgraded = SandevistanUpgrades.duration(duration, stack);
        return Math.clamp(Math.round(upgraded * (0.55 + 0.45 * condition)), 20, 1200);
    }

    public static void start(Player player, String id, long durationTicks, long cooldownTicks) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        ItemStack stack = inventory.getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
        if (!(stack.getItem() instanceof CyberwareItem item)) return;
        CompoundTag stored = data(player);
        long tick = now(serverPlayer);
        long recoveryWindow = recoveryWindow();
        int uses = SandevistanSession.recentUseCount(stored.getInt("uses"), stored.getLong("last_use"), tick, recoveryWindow);
        SandevistanProfile profile = SandevistanUpgrades.profile(SandevistanProfile.configured(id),
                de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(stack));
        SandevistanSession session = new SandevistanSession(profile, tick, durationTicks,
                cooldownTicks, CyberwareConditionHelper.getIntegrityRatio(stack, item.getDefinition()), uses);
        stored.putInt("uses", uses);
        stored.putLong("last_use", tick);
        stored.putLong("recovery_until", tick + durationTicks + cooldownTicks);
        stored.putLong("cooldown_ticks", cooldownTicks);
        player.getPersistentData().put(DATA_KEY, stored);
        ServerState state = state(serverPlayer);
        state.sessions.put(player.getUUID(), new Active(session, player.level().dimension()));
        state.ends.remove(player.getUUID());
        state.dirty.add(player.level().dimension());
        CyberstrainManager.addInstability(player, session.activationLoad());
        CyberwareConditionHelper.damage(stack, item.getDefinition(), session.profile.wear() + Math.max(0, uses - 1));
        inventory.save();
        CyberwareAbilities.enforceAbilityCooldown(player, player.level().getGameTime() + durationTicks + cooldownTicks, cooldownTicks);
        if (uses >= 3 || session.condition < 0.25) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.sandevistan.warning", uses), true);
        }
    }

    public static SandevistanSession session(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return null;
        Active active = state(serverPlayer).sessions.get(player.getUUID());
        return active == null || active.dimension != player.level().dimension() ? null : active.session;
    }

    public static long recoveryRemaining(Player player) {
        return player instanceof ServerPlayer serverPlayer ? Math.max(0, data(player).getLong("recovery_until") - now(serverPlayer)) : 0;
    }

    public static int recoveryTotalSeconds(Player player) { return (int) Math.clamp((data(player).getLong("cooldown_ticks") + 19) / 20, 0, 3600); }

    public static void resetRecovery(Player player) { player.getPersistentData().remove(DATA_KEY); }

    public static int recentUses(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;
        CompoundTag stored = data(player);
        return (int) Math.max(0, stored.getInt("uses") - Math.max(0, now(serverPlayer) - stored.getLong("last_use")) / recoveryWindow());
    }

    private static long recoveryWindow() {
        return (long) (20 * SandevistanProfile.bounded(CyberwareBalance.doubleValue("sandevistan.recovery_window_seconds"), 30, 600, 120));
    }

    public static void stop(Player player, boolean overload) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ServerState state = state(serverPlayer);
        Active active = state.sessions.remove(player.getUUID());
        if (active == null) return;
        SandevistanSession session = active.session;
        long tick = now(serverPlayer);
        CompoundTag stored = data(player);
        long until = Math.max(stored.getLong("recovery_until"), tick + session.cooldownTicks * (overload ? 2 : 1));
        stored.putLong("recovery_until", until);
        stored.putLong("cooldown_ticks", session.cooldownTicks * (overload ? 2 : 1));
        player.getPersistentData().put(DATA_KEY, stored);
        CyberwareAbilities.enforceAbilityCooldown(player, player.level().getGameTime() + Math.max(0, until - tick), stored.getLong("cooldown_ticks"));
        state.ends.put(player.getUUID(), new End(active.dimension, player.position(), tick,
                new SandevistanSnapshotPayload.User(player.getId(), player.getUUID(), session.profile.id(), session.started,
                        0, overload ? 2 : 0, (float) session.effectiveTimeScale())));
        state.dirty.add(active.dimension);
        if (overload) {
            PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
            ItemStack stack = inventory.getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
            if (stack.getItem() instanceof CyberwareItem item && item.getDefinition().id().equals(session.profile.id())) {
                CyberwareConditionHelper.damage(stack, item.getDefinition(), session.profile.wear() * 4 + 5);
                inventory.save();
            }
            player.hurt(player.damageSources().magic(), 3);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.sandevistan.overload"), true);
        }
    }

    public static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ServerState state = SERVERS.computeIfAbsent(level.getServer(), ignored -> new ServerState());
        long tick = level.getServer().overworld().getGameTime();
        for (ServerPlayer player : level.players()) {
            Active active = state.sessions.get(player.getUUID());
            if (active == null) continue;
            SandevistanSession session = active.session;
            ItemStack stack = new PlayerCyberwareInventory(player).getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
            boolean valid = active.dimension == level.dimension() && player.isAlive() && !player.isSpectator()
                    && !player.isPassenger() && !player.isFallFlying() && !player.isSleeping()
                    && !(player.containerMenu instanceof RipperStationMenu)
                    && stack.getItem() instanceof CyberwareItem item && item.getDefinition().id().equals(session.profile.id())
                    && CyberwareConditionHelper.getIntegrityRatio(stack, item.getDefinition()) > 0;
            if (!valid || tick >= session.until()) {
                CyberwareAbilities.stopActiveOperatingSystem(player);
                continue;
            }
            if (tick % 20 == 0) {
                CyberstrainManager.addInstability(player, session.profile.runningStrain() * (1 + (session.recentUses - 1) * 0.3));
                if (tick - session.started >= 20 && session.shouldOverload(CyberstrainManager.getInstability(player))) {
                    stop(player, true);
                    CyberwareAbilities.stopActiveOperatingSystem(player);
                }
            }
        }
        Map<UUID, Influence> influences = new LinkedHashMap<>();
        List<SandevistanSnapshotPayload.User> users = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            SandevistanSession session = session(player);
            if (session == null) continue;
            Vec3 echoPosition = state.echoPositions.get(player.getUUID());
            Vec3 position = player.position();
            if (echoPosition == null || echoPosition.distanceToSqr(position) > 64) {
                state.echoPositions.put(player.getUUID(), position);
            } else if (echoPosition.distanceToSqr(position) >= 0.0025
                    && (session.effectiveTimeScale() < 0.12 || tick % 2 == 0)) {
                double distance = Math.sqrt(echoPosition.distanceToSqr(position));
                float stride = state.echoStrides.getOrDefault(player.getUUID(), 0F) + (float) distance * 4;
                state.echoStrides.put(player.getUUID(), stride);
                state.echoPositions.put(player.getUUID(), position);
                var echo = new de.artemis.cyberneticenhancements.common.network.SandevistanEchoPayload(
                        level.dimension().location(), player.getId(), player.getUUID(), level.getGameTime(), position,
                        player.yBodyRot, player.yHeadRot, player.getXRot(), player.getScale(), player.isCrouching(),
                        stride, (float) Math.min(1, distance * 4), player.getAttackAnim(0),
                        !player.getMainHandItem().isEmpty(), !player.getOffhandItem().isEmpty());
                for (ServerPlayer viewer : level.players()) {
                    if (viewer.distanceToSqr(player) <= 64 * 64) PacketDistributor.sendToPlayer(viewer, echo);
                }
            }
            users.add(new SandevistanSnapshotPayload.User(player.getId(), player.getUUID(), session.profile.id(), session.started,
                    (int) Math.max(0, session.until() - tick), 1, (float) session.effectiveTimeScale()));
            for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(session.profile.radius()),
                    entity -> eligible(entity, player) && entity.distanceToSqr(player) <= session.profile.radius() * session.profile.radius())) {
                float scale = (float) session.effectiveTimeScale();
                Influence previous = influences.get(entity.getUUID());
                if (previous == null && influences.size() < SandevistanSnapshotPayload.MAX_TARGETS || previous != null && scale < previous.scale) {
                    influences.put(entity.getUUID(), new Influence(entity, scale));
                }
            }
        }
        Map<UUID, Float> rates = new HashMap<>();
        state.echoPositions.keySet().removeIf(uuid -> !state.sessions.containsKey(uuid));
        state.echoStrides.keySet().removeIf(uuid -> !state.sessions.containsKey(uuid));
        influences.forEach((uuid, influence) -> rates.put(uuid, influence.scale));
        SandevistanTimeDilation.replace(level, rates, Long.MAX_VALUE);
        state.ends.values().removeIf(end -> tick - end.tick > 12);
        for (End end : state.ends.values()) if (end.dimension == level.dimension()) users.add(end.user);
        boolean dirty = state.dirty.remove(level.dimension());
        if (tick % 4 != 0 && !dirty) return;
        for (ServerPlayer viewer : level.players()) {
            List<SandevistanSnapshotPayload.User> nearby = users.stream().filter(user -> {
                Entity owner = level.getEntity(user.entityId());
                End end = state.ends.get(user.uuid());
                Vec3 position = owner != null ? owner.position() : end == null ? null : end.position;
                return position != null && position.distanceToSqr(viewer.position()) <= 128 * 128;
            }).limit(SandevistanSnapshotPayload.MAX_USERS).toList();
            if (nearby.isEmpty() && !state.viewers.remove(viewer.getUUID())) continue;
            if (!nearby.isEmpty()) state.viewers.add(viewer.getUUID());
            List<SandevistanSnapshotPayload.Target> targets = influences.values().stream()
                    .filter(influence -> influence.entity.distanceToSqr(viewer) <= 128 * 128)
                    .map(influence -> new SandevistanSnapshotPayload.Target(influence.entity.getId(), influence.entity.getUUID(), influence.scale)).toList();
            PacketDistributor.sendToPlayer(viewer, new SandevistanSnapshotPayload(level.dimension().location(), nearby, targets));
        }
    }

    private static boolean eligible(Entity entity, ServerPlayer owner) {
        if (!entity.isAlive() || entity instanceof Player || entity.isPassenger() || entity.isVehicle()
                || entity.getType().is(IMMUNE) || entity.isAlliedTo(owner)) return false;
        if (entity instanceof Mob mob) return mob instanceof Enemy || mob.getTarget() == owner;
        if (entity instanceof Projectile projectile && SandevistanTimeDilation.supportsProjectile(entity)) {
            Entity shooter = projectile.getOwner();
            return shooter != owner && (shooter == null || !shooter.isAlliedTo(owner))
                    && !(shooter instanceof Player player && session(player) != null);
        }
        return false;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer && event.getSource().getEntity() instanceof ServerPlayer attacker && session(attacker) != null) {
            double multiplier = SandevistanProfile.bounded(CyberwareBalance.doubleValue("sandevistan.pvp_damage_multiplier"), 0.25, 1, 0.8);
            event.setAmount((float) (event.getAmount() * multiplier));
        }
    }

    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && session(player) != null
                && event.getTarget() instanceof Mob target && SandevistanTimeDilation.scale(target) < 1
                && player.getAttackStrengthScale(0.5F) > 0.9F) {
            target.invulnerableTime = 0;
        }
    }

    public static void copyState(Player original, Player replacement) {
        if (original instanceof ServerPlayer) CyberwareAbilities.stopActiveOperatingSystem(original);
        if (original.getPersistentData().contains(DATA_KEY)) replacement.getPersistentData().put(DATA_KEY, data(original).copy());
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        CyberwareAbilities.stopActiveOperatingSystem(event.getEntity());
    }

    public static void onServerStopped(ServerStoppedEvent event) { SERVERS.remove(event.getServer()); }

    private record Active(SandevistanSession session, ResourceKey<Level> dimension) {
    }
    private record Influence(Entity entity, float scale) {
    }
    private record End(ResourceKey<Level> dimension, Vec3 position, long tick, SandevistanSnapshotPayload.User user) {
    }
    private static final class ServerState {
        private final Map<UUID, Vec3> echoPositions = new HashMap<>();
        private final Map<UUID, Float> echoStrides = new HashMap<>();
        private final Map<UUID, Active> sessions = new HashMap<>();
        private final Map<UUID, End> ends = new HashMap<>();
        private final Set<UUID> viewers = new HashSet<>();
        private final Set<ResourceKey<Level>> dirty = new HashSet<>();
    }
}
