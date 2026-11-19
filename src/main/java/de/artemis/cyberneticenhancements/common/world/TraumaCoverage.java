package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import de.artemis.cyberneticenhancements.common.network.TraumaRescuePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.damagesource.DamageTypes;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.TemporaryCyberwareEffectManager;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public final class TraumaCoverage {
    public static final String COVERAGE = "cyberneticenhancements.trauma_coverage";
    private static final String DOWNED = "cyberneticenhancements.trauma_downed";
    private static final String REMAINING = "cyberneticenhancements.trauma_remaining";
    private static final String CALLED = "cyberneticenhancements.trauma_called";
    private static final Map<ServerPlayer, Rescue> RESCUES = new WeakHashMap<>();

    private TraumaCoverage() {
    }

    public static void registerInteractions(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> onInteract(event));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) -> onInteract(event));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) -> onInteract(event));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) -> onInteract(event));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) -> onInteract(event));
    }

    public static boolean downed(ServerPlayer player) {
        return player.getPersistentData().getBoolean(DOWNED);
    }

    public static boolean assigned(TraumaTeamEntity medic) {
        return RESCUES.values().stream().anyMatch(rescue -> rescue.team.contains(medic));
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (downed(player)
                || de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.isPsychosisActive(player)
                || !player.getPersistentData().getBoolean(COVERAGE) || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                || event.getSource().is(DamageTypes.GENERIC_KILL)) {
            send(player, 5, -1, 0);
            return;
        }
        player.getPersistentData().remove(CALLED);
        player.getPersistentData().putBoolean(DOWNED, true);
        player.getPersistentData().putInt(REMAINING, 2400);
        player.setHealth(1);
        player.clearFire();
        event.setCanceled(true);
        start(player);
    }

    public static void callMedtec(ServerPlayer player) {
        if (de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.isPsychosisActive(player)) return;
        if (!downed(player) || !player.isAlive() || !player.getPersistentData().getBoolean(COVERAGE)
                || player.getPersistentData().getBoolean(CALLED)) return;
        player.getPersistentData().putBoolean(COVERAGE, false);
        player.getPersistentData().putBoolean(CALLED, true);
        player.getPersistentData().putInt(REMAINING, 2400);
        // Continue the existing downed session; closing a container here also closes
        // the client's death screen and would replay its opening presentation.
        Rescue rescue = RESCUES.computeIfAbsent(player,
                key -> new Rescue(player.position(), player.level().dimension().location().toString()));
        rescue.ticks = 0;
        send(player, 1, -1, 120);
    }

    private static void start(ServerPlayer player) {
        Rescue rescue = new Rescue(player.position(), player.level().dimension().location().toString());
        rescue.ticks = Math.max(0, 2400 - player.getPersistentData().getInt(REMAINING));
        RESCUES.put(player, rescue);
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        send(player, player.getPersistentData().getBoolean(CALLED) ? 1 : 0, -1, 120);
    }

    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && downed(player)) event.setCanceled(true);
    }

    public static void onTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) if (downed(player)) update(player);
    }

    private static void update(ServerPlayer player) {
        if (de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.isPsychosisActive(player)) {
            abandon(player);
            return;
        }
        if (!RESCUES.containsKey(player)) {
            if (player.tickCount > 40) start(player);
            return;
        }
        Rescue rescue = RESCUES.get(player);
        if (!rescue.dimension.equals(player.level().dimension().location().toString())) {
            abandon(player);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setAirSupply(player.getMaxAirSupply());
        if (player.position().distanceToSqr(rescue.origin) > 0.01)
            player.connection.teleport(rescue.origin.x, rescue.origin.y, rescue.origin.z, player.getYRot(), player.getXRot());
        if (!player.getPersistentData().getBoolean(CALLED)) return;
        if (++rescue.ticks >= 2400 || !player.isAlive()) {
            abandon(player);
            return;
        }
        player.getPersistentData().putInt(REMAINING, 2400 - rescue.ticks);
        rescue.team.removeIf(medic -> !medic.isAlive() || medic.isRemoved());
        if (rescue.ticks % 40 == 1 && rescue.team.size() < 3 && rescue.ticks < 1200) deploy(player, rescue);
        double distance = Double.POSITIVE_INFINITY;
        boolean attending = false;
        for (TraumaTeamEntity medic : rescue.team) {
            if (medic.level() != player.level()) continue;
            double range = medic.distanceTo(player);
            distance = Math.min(distance, range);
            medic.getNavigation().moveTo(player, 1.25);
            medic.getLookControl().setLookAt(player);
            if (range < 2.8 && medic.hasLineOfSight(player)) attending = true;
        }
        rescue.treatment = attending ? rescue.treatment + 1 : 0;
        if (rescue.ticks % 10 == 0)
            send(player, attending ? 2 : 1, Double.isFinite(distance) ? (float) distance : -1, Math.max(0, (2400 - rescue.ticks) / 20));
        if (rescue.treatment >= 60) revive(player);
    }

    private static void deploy(ServerPlayer player, Rescue rescue) {
        var level = player.serverLevel();
        int pathChecks = 0;
        for (int attempt = 0; attempt < 24 && rescue.team.size() < 3; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int radius = 12 + level.random.nextInt(12);
            BlockPos column = player.blockPosition().offset((int) (Math.sin(angle) * radius), 0, (int) (Math.cos(angle) * radius));
            if (!level.hasChunkAt(column)) continue;
            for (int dy = 5; dy >= -8; dy--) {
                BlockPos pos = column.offset(0, dy, 0);
                if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                        || !level.getFluidState(pos).isEmpty()) continue;
                TraumaTeamEntity medic = ModEntityTypes.TRAUMA_TEAM.get().create(level);
                if (medic == null) return;
                medic.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                medic.setOnGround(true);
                if (!level.noCollision(medic)) {
                    medic.discard();
                    continue;
                }
                if (++pathChecks > 8) { medic.discard(); return; }
                var path = medic.getNavigation().createPath(player, 1);
                if (path == null || !path.canReach()) {
                    medic.discard();
                    continue;
                }
                medic.setPersistenceRequired();
                medic.addTag("cyberneticenhancements.trauma_dispatch");
                level.addFreshEntity(medic);
                medic.getNavigation().moveTo(path, 1.25);
                rescue.team.add(medic);
                break;
            }
        }
    }

    private static void revive(ServerPlayer player) {
        if (de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.isPsychosisActive(player)) {
            abandon(player);
            return;
        }
        Rescue rescue = RESCUES.get(player);
        if (rescue != null) {
            rescue.team.forEach(medic -> medic.beginAftercare(player));
            rescue.team.clear();
        }
        clear(player);
        player.setHealth(Math.max(1, player.getMaxHealth() * 0.65F));
        player.clearFire();
        player.removeAllEffects();
        TemporaryCyberwareEffectManager.addEffects(player, 200,
                CyberwareEffect.healthRegen(1.0), CyberwareEffect.damageReduction(0.8));
        TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.moveSpeed(-0.15), 60);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 35, 0.35, 0.6, 0.35, 0.12);
        send(player, 3, 0, 0);
        player.playNotifySound(ModSounds.HEALING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.message(player, "body", Component.translatable("trauma.cyberneticenhancements.revived"));
    }

    public static void abandon(ServerPlayer player) {
        if (!downed(player)) return;
        clear(player);
        send(player, 4, 0, 0);
        if (player.isAlive()) player.kill();
    }

    private static void clear(ServerPlayer player) {
        player.getPersistentData().remove(DOWNED);
        player.getPersistentData().remove(REMAINING);
        player.getPersistentData().remove(CALLED);
        Rescue rescue = RESCUES.remove(player);
        if (rescue != null) rescue.team.forEach(de.artemis.cyberneticenhancements.common.entity.HolographicDeployment::dismiss);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Rescue rescue = RESCUES.remove(player);
            if (rescue != null) rescue.team.forEach(de.artemis.cyberneticenhancements.common.entity.HolographicDeployment::dismiss);
        }
    }

    public static void onStop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        RESCUES.clear();
    }

    public static void copy(Player original, Player clone) {
        clone.getPersistentData().putBoolean(COVERAGE, original.getPersistentData().getBoolean(COVERAGE));
    }

    public static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && downed(player)
                && event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) cancellable.setCanceled(true);
    }

    public static void onAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && downed(player)) event.setCanceled(true);
    }

    private static void send(ServerPlayer player, int phase, float distance, int seconds) {
        // Abandoning commits a death; only a successful revival releases the held body.
        if (downed(player) || phase == 3) DeathBodySync.downed(player, downed(player));
        Rescue rescue = RESCUES.get(player);
        PacketDistributor.sendToPlayer(player, new TraumaRescuePayload(phase, distance, seconds,
                phase == 3 ? 60 : rescue == null ? 0 : rescue.treatment));
    }

    private static final class Rescue {
        final Vec3 origin;
        final String dimension;
        final List<TraumaTeamEntity> team = new ArrayList<>();
        int ticks, treatment;

        Rescue(Vec3 origin, String dimension) {
            this.origin = origin;
            this.dimension = dimension;
        }
    }
}
