package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.network.DeathBodyPayload;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.util.DeathFallDirection;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DeathBodySync {
    private static final Map<LivingEntity, DeathBodyPayload> STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, Float> LAST_HIT = new WeakHashMap<>();

    public static boolean fallen(LivingEntity entity) {
        var state = STATES.get(entity);
        return state != null && state.phase() != DeathBodyPayload.RECOVERED;
    }

    private static boolean supported(LivingEntity entity) {
        return entity instanceof ServerPlayer || entity instanceof AbstractCityNpcEntity || entity instanceof CyberpsychoEntity;
    }

    public static void hit(LivingDamageEvent.Pre event) {
        if (!event.getEntity().level().isClientSide() && supported(event.getEntity()) && event.getNewDamage() > 0)
            remember(event.getEntity(), event.getSource());
    }

    private static void remember(LivingEntity entity, DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile projectile) {
            var velocity = projectile.getDeltaMovement();
            if (velocity.horizontalDistanceSqr() > 0.000001) {
                LAST_HIT.put(entity, DeathFallDirection.yaw(velocity.x, velocity.z, entity.yBodyRot));
                return;
            }
        }
        var origin = source.getSourcePosition();
        if (origin == null && source.getEntity() != null) origin = source.getEntity().position();
        if (origin == null) return;
        var away = entity.position().subtract(origin);
        if (away.horizontalDistanceSqr() > 0.000001)
            LAST_HIT.put(entity, DeathFallDirection.yaw(away.x, away.z, entity.yBodyRot));
    }

    public static void downed(LivingEntity player, boolean downed) {
        var previous = STATES.get(player);
        if (downed) {
            if (previous == null || previous.phase() != DeathBodyPayload.DOWNED) send(player, DeathBodyPayload.DOWNED);
        } else if (previous != null && previous.phase() == DeathBodyPayload.DOWNED) {
            send(player, DeathBodyPayload.RECOVERED);
            STATES.remove(player);
            LAST_HIT.remove(player);
        }
    }

    public static void died(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!entity.level().isClientSide() && supported(entity)) {
            remember(entity, event.getSource());
            send(entity, DeathBodyPayload.DEAD);
        }
    }

    private static void send(LivingEntity player, int phase) {
        var equipment = phase == DeathBodyPayload.RECOVERED ? List.<net.minecraft.world.item.ItemStack>of()
                : DeathBodyPayload.SLOTS.stream().map(slot -> player.getItemBySlot(slot).copy()).toList();
        var payload = new DeathBodyPayload(player.getUUID(), player.getId(), phase, player.level().getGameTime(),
                LAST_HIT.getOrDefault(player, player.yBodyRot), equipment);
        STATES.put(player, payload);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity target) {
            var payload = STATES.get(target);
            if (payload != null && (payload.phase() == DeathBodyPayload.DOWNED || target.level().getGameTime() - payload.started() < 220))
                PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (STATES.containsKey(player) && STATES.get(player).phase() == DeathBodyPayload.DOWNED)
                send(player, DeathBodyPayload.RECOVERED);
            STATES.remove(player);
            LAST_HIT.remove(player);
        }
    }
    private DeathBodySync() { }
}
