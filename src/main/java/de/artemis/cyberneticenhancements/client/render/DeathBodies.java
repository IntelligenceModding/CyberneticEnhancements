package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.ui.DeathBodyMotion;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity;
import de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity;
import de.artemis.cyberneticenhancements.common.network.DeathBodyPayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.joml.Vector3f;

/** Short-lived render references, never lootable entities or copies of inventories. */
public final class DeathBodies {
    private static final Map<LivingEntity, Body> BODIES = new IdentityHashMap<>();
    private static final Map<EncounterBodyEntity, Body> SCENES = new IdentityHashMap<>();
    private static final Set<LivingEntity> SEEN = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, DeathBodyPayload> PENDING = new HashMap<>();
    private static ClientLevel level;
    private static Body rendering;
    private static float renderPartial;
    private static Runnable restoreModel;

    public static boolean enabled() {
        return ClientConfig.SPEC.isLoaded() && ClientConfig.ANIMATED_DEATH_BODIES.get();
    }

    private static boolean supported(LivingEntity entity) {
        return entity instanceof Player || entity instanceof AbstractCityNpcEntity || entity instanceof CyberpsychoEntity;
    }

    private static boolean isCorpse(LivingEntity entity) {
        var body = BODIES.get(entity);
        return de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(entity,
                body != null && !body.downed && body.recovering < 0);
    }

    private static boolean ensureLevel() {
        var current = Minecraft.getInstance().level;
        if (current != level) {
            BODIES.clear(); SCENES.clear(); SEEN.clear(); PENDING.clear(); rendering = null; level = current;
        }
        if (!enabled()) {
            BODIES.values().removeIf(body -> body.anchor == null);
            SEEN.clear(); PENDING.clear();
        }
        return level != null;
    }

    public static void receive(DeathBodyPayload payload) {
        if (!ensureLevel() || !enabled()) return;
        if (payload.phase() == DeathBodyPayload.RECOVERED) {
            PENDING.remove(payload.player());
            for (var body : BODIES.values()) if (body.source.getUUID().equals(payload.player()) && body.downed)
                body.recovering = level.getGameTime();
        } else {
            PENDING.put(payload.player(), payload);
            var entity = payloadEntity(payload);
            if (entity != null) capture(entity, payload);
        }
    }

    private static LivingEntity payloadEntity(DeathBodyPayload payload) {
        var entity = level.getEntity(payload.entityId());
        return entity instanceof LivingEntity living && entity.getUUID().equals(payload.player()) ? living : null;
    }

    public static void beginLocal(Player player) {
        if (!ensureLevel() || !enabled()) return;
        if (BODIES.containsKey(player)) return;
        if (player.isAlive()) capture(player, new DeathBodyPayload(player.getUUID(), player.getId(), DeathBodyPayload.DOWNED,
                level.getGameTime(), player.yBodyRot, equipment(player)));
        else capture(player, null);
    }

    private static Body capture(LivingEntity entity, DeathBodyPayload payload) {
        var existing = BODIES.get(entity);
        if (existing != null) {
            if (payload != null) {
                existing.fallYaw = payload.fallYaw();
                if (payload.phase() == DeathBodyPayload.DEAD) {
                    existing.downed = false; existing.deadAt = payload.started(); existing.recovering = -1;
                }
                if (!payload.equipment().isEmpty()) existing.equipment = payload.equipment().stream().map(ItemStack::copy).toList();
                if (payload.phase() == DeathBodyPayload.DEAD) PENDING.remove(payload.player());
            }
            return existing;
        }
        if (!supported(entity) || payload == null && (!isCorpse(entity) || SEEN.contains(entity))) return null;
        double now = payload == null ? level.getGameTime() - Math.max(0, entity.deathTime - 1) : payload.started();
        var body = new Body(entity, now, payload != null && payload.phase() == DeathBodyPayload.DOWNED);
        if (payload != null) body.fallYaw = payload.fallYaw();
        if (payload != null && !payload.equipment().isEmpty()) body.equipment = payload.equipment().stream().map(ItemStack::copy).toList();
        BODIES.put(entity, body); SEEN.add(entity);
        if (payload != null && payload.phase() == DeathBodyPayload.DEAD) PENDING.remove(payload.player());
        // Bound transient rendering work during unusually large simultaneous deaths.
        if (BODIES.size() > 128) BODIES.values().stream().filter(b -> !b.downed).min(Comparator.comparingDouble(b -> b.fellAt))
                .ifPresent(oldest -> BODIES.remove(oldest.source));
        return body;
    }

    private static List<ItemStack> equipment(LivingEntity entity) {
        return DeathBodyPayload.SLOTS.stream().map(slot -> entity.getItemBySlot(slot).copy()).toList();
    }

    private static void captureScene(EncounterBodyEntity anchor) {
        var existing = SCENES.get(anchor);
        if (existing != null) {
            if (existing.sceneAppearance == anchor.appearance()) return;
            BODIES.remove(existing.source); SCENES.remove(anchor);
        }
        var source = anchor.createVisual();
        if (source == null) return;
        var body = new Body(source, level.getGameTime() - 20, true);
        body.anchor = anchor; body.fallYaw = anchor.fallYaw();
        body.sceneAppearance = anchor.appearance();
        body.restPose = DeathBodyMotion.restPose(anchor.getUUID().getMostSignificantBits() ^ anchor.getUUID().getLeastSignificantBits());
        SCENES.put(anchor, body); BODIES.put(source, body);
    }

    private static double sceneDeadAt(EncounterBodyEntity anchor) {
        return !enabled() && anchor.ownerDeath() >= 0 ? anchor.ownerDeath() - DeathBodyMotion.HOLD_TICKS
                : anchor.departure() - DeathBodyMotion.HOLD_TICKS;
    }

    private static boolean retainScene(EncounterBodyEntity anchor) {
        return anchor.ownerDeath() >= 0 && anchor.departure() >= 0 && level != null && anchor.level() == level
                && level.getGameTime() - sceneDeadAt(anchor) < DeathBodyMotion.HOLD_TICKS + DeathBodyMotion.DISSOLVE_TICKS;
    }

    public static void tick(ClientTickEvent.Post event) {
        if (!ensureLevel()) return;
        for (var payload : List.copyOf(PENDING.values())) {
            var entity = payloadEntity(payload);
            if (entity != null) capture(entity, payload);
            else if (payload.phase() == DeathBodyPayload.DEAD && level.getGameTime() - payload.started() > 220) PENDING.remove(payload.player());
        }
        for (var entity : level.entitiesForRendering()) {
            if (entity instanceof EncounterBodyEntity anchor) captureScene(anchor);
            else if (enabled() && entity instanceof LivingEntity living && supported(living) && isCorpse(living)) capture(living, null);
        }
        var iterator = BODIES.values().iterator();
        while (iterator.hasNext()) {
            var body = iterator.next();
            if (body.anchor != null) {
                if (body.anchor.isRemoved() && !retainScene(body.anchor)) { SCENES.remove(body.anchor); iterator.remove(); continue; }
                body.position = body.anchor.position();
                body.fallYaw = body.anchor.fallYaw();
                if (body.anchor.departure() >= 0) {
                    body.downed = false;
                    body.deadAt = sceneDeadAt(body.anchor);
                }
            }
            if (body.downed && de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.isCorpse(body.source, false)) {
                body.downed = false; body.deadAt = level.getGameTime(); body.recovering = -1;
                PENDING.remove(body.source.getUUID());
            }
            if (body.anchor == null && !body.source.isRemoved()) body.position = body.source.position();
            if (body.recovering >= 0 && level.getGameTime() - body.recovering >= DeathBodyMotion.RECOVERY_TICKS) {
                SEEN.remove(body.source); iterator.remove(); continue;
            }
            if (!body.downed && level.getGameTime() - body.deadAt >= DeathBodyMotion.HOLD_TICKS + DeathBodyMotion.DISSOLVE_TICKS) {
                iterator.remove(); continue;
            }
            if (!body.downed && level.getGameTime() - body.deadAt >= DeathBodyMotion.HOLD_TICKS) body.particles();
        }
    }

    public static void leaving(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof EncounterBodyEntity anchor) {
            // Entity-removal packets can precede the client's final corpse frame slightly.
            if (retainScene(anchor)) return;
            var body = SCENES.remove(anchor);
            if (body != null) BODIES.remove(body.source);
            return;
        }
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity) || !ensureLevel()) return;
        if (isCorpse(entity)) capture(entity, null);
        else {
            BODIES.remove(entity); PENDING.remove(entity.getUUID()); SEEN.remove(entity);
        }
    }

    public static void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!enabled() || rendering != null) return;
        if (supported(event.getEntity()) && isCorpse(event.getEntity()) && ensureLevel()) capture(event.getEntity(), null);
        if (BODIES.containsKey(event.getEntity()) || supported(event.getEntity()) && isCorpse(event.getEntity())) event.setCanceled(true);
    }

    public static void name(RenderNameTagEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && rendering(entity)) { event.setCanRender(TriState.FALSE); return; }
        if (enabled() && event.getEntity() instanceof LivingEntity entity && supported(entity)
                && (BODIES.containsKey(entity) || isCorpse(entity))) event.setCanRender(TriState.FALSE);
    }

    public static void hand(RenderHandEvent event) {
        if (enabled() && BODIES.containsKey(Minecraft.getInstance().player)) event.setCanceled(true);
    }

    public static boolean rendering(LivingEntity entity) { return rendering != null && rendering.source == entity; }

    public static DeathBodyMotion.Pose motion(LivingEntity entity, float partial) {
        var body = BODIES.get(entity);
        return body == null ? null : body.motion(partial);
    }

    public static float fallYaw(LivingEntity entity) {
        var body = BODIES.get(entity);
        return body == null ? entity.yBodyRot : body.fallYaw;
    }

    public static Vec3 position(LivingEntity entity) {
        var body = BODIES.get(entity);
        return body == null ? entity.position() : body.position;
    }

    public static void rotations(LivingEntity entity, PoseStack pose, float partial) {
        var motion = rendering.motion(partial);
        pose.translate(0, motion.lift(), 0);
        pose.mulPose(motion.rotation(rendering.fallYaw));
        pose.mulPose(Axis.YP.rotationDegrees(180 - rendering.yaw));
    }

    public static void poseModel(EntityModel<?> model, LivingEntity entity) {
        if (!rendering(entity) || !(model instanceof HumanoidModel<?> humanoid)) return;
        var motion = rendering.motion(renderPartial);
        boolean headVisible = humanoid.head.visible, hatVisible = humanoid.hat.visible;
        float headZ = humanoid.head.z, hatZ = humanoid.hat.z;
        restoreModel = () -> {
            humanoid.head.visible = headVisible; humanoid.hat.visible = hatVisible;
            humanoid.head.z = headZ; humanoid.hat.z = hatZ;
        };
        humanoid.attackTime = 0; humanoid.riding = false; humanoid.crouching = false; humanoid.swimAmount = 0;
        humanoid.head.y = humanoid.body.y = 0;
        humanoid.rightArm.y = humanoid.leftArm.y = 2;
        humanoid.rightLeg.y = humanoid.leftLeg.y = 12;
        humanoid.rightLeg.z = humanoid.leftLeg.z = 0;
        humanoid.body.xRot = humanoid.body.yRot = humanoid.body.zRot = 0;
        // Accommodate Minecraft's deeper head cube without lifting the entire torso off the ground.
        humanoid.head.z = (float) motion.headOffset(rendering.yaw, rendering.fallYaw);
        var limbs = rendering.restPose.sample(rendering.seconds(renderPartial), reducedMotion());
        joint(humanoid.head, limbs.head());
        humanoid.head.xRot += (float) Math.toRadians(rendering.pitch * (1 - motion.progress()));
        joint(humanoid.rightArm, limbs.rightArm()); joint(humanoid.leftArm, limbs.leftArm());
        joint(humanoid.rightLeg, limbs.rightLeg()); joint(humanoid.leftLeg, limbs.leftLeg());
        // Spread along the ground when falling onto a side, rather than through it.
        float spread = (float) motion.spread(rendering.yaw, rendering.fallYaw);
        humanoid.rightArm.zRot *= spread; humanoid.leftArm.zRot *= spread;
        humanoid.rightLeg.zRot *= spread; humanoid.leftLeg.zRot *= spread;
        humanoid.head.visible = !hideHead(entity); humanoid.hat.visible = humanoid.head.visible;
        humanoid.hat.copyFrom(humanoid.head);
        if (model instanceof PlayerModel<?> player) {
            player.jacket.copyFrom(player.body); player.rightSleeve.copyFrom(player.rightArm); player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg); player.leftPants.copyFrom(player.leftLeg);
        }
    }

    private static void joint(ModelPart part, DeathBodyMotion.Joint joint) {
        part.xRot = (float) joint.pitch(); part.yRot = (float) joint.yaw(); part.zRot = (float) joint.roll();
    }

    private static boolean reducedMotion() {
        return Minecraft.getInstance().options.screenEffectScale().get() <= 0;
    }

    private static boolean hideHead(LivingEntity entity) {
        var mc = Minecraft.getInstance();
        return entity == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    @SuppressWarnings("unchecked")
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !ensureLevel()
                || BODIES.isEmpty() || ShaderPackCompatibility.isRenderingShadowPass()) return;
        var mc = Minecraft.getInstance();
        var camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();
        var pose = new PoseStack();
        for (var body : List.copyOf(BODIES.values())) {
            if (body.position.distanceToSqr(camera) > 80 * 80 || !level.hasChunkAt(BlockPos.containing(body.position))) continue;
            float visibility = body.downed ? 1 : DeathBodyMotion.visibility(level.getGameTime() + partial - body.deadAt);
            if (visibility <= 0) continue;
            var entity = body.source;
            var originalEquipment = equipment(entity);
            Pose originalPose = entity.getPose();
            rendering = body; renderPartial = partial;
            pose.pushPose();
            try {
                entity.setPose(Pose.STANDING);
                for (int i = 0; i < DeathBodyPayload.SLOTS.size(); i++) entity.setItemSlot(DeathBodyPayload.SLOTS.get(i),
                        i == 5 && hideHead(entity) ? ItemStack.EMPTY : body.equipment.get(i));
                pose.translate(body.position.x - camera.x, body.position.y - camera.y, body.position.z - camera.z);
                var target = visibility >= 1 ? buffers : DeploymentHologram.translucentBuffers(visibility,
                        entity instanceof TraumaTeamEntity ? 0x66FFC1 : 0x55CEFF, (float) (level.getGameTime() + partial), pose, buffers);
                EntityRenderer<LivingEntity> renderer = (EntityRenderer<LivingEntity>) mc.getEntityRenderDispatcher().getRenderer(entity);
                renderer.render(entity, body.yaw, partial, pose, target, LevelRenderer.getLightColor(level, BlockPos.containing(body.position.add(0, 0.3, 0))));
                buffers.endBatch();
            } finally {
                if (restoreModel != null) { restoreModel.run(); restoreModel = null; }
                for (int i = 0; i < DeathBodyPayload.SLOTS.size(); i++) entity.setItemSlot(DeathBodyPayload.SLOTS.get(i), originalEquipment.get(i));
                entity.setPose(originalPose);
                pose.popPose(); rendering = null;
            }
        }
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        BODIES.clear(); SCENES.clear(); PENDING.clear(); SEEN.clear(); rendering = null; level = null;
    }

    private static final class Body {
        private final LivingEntity source;
        private final double fellAt;
        private double deadAt, recovering = -1;
        private boolean downed;
        private final float yaw, pitch;
        private float fallYaw;
        private DeathBodyMotion.RestPose restPose;
        private EncounterBodyEntity anchor;
        private int sceneAppearance;
        private Vec3 position;
        private List<ItemStack> equipment;

        private Body(LivingEntity entity, double now, boolean downed) {
            source = entity; fellAt = deadAt = now; this.downed = downed;
            yaw = entity.yBodyRot; pitch = entity.getXRot(); position = entity.position(); equipment = equipment(entity);
            fallYaw = yaw;
            restPose = DeathBodyMotion.restPose(entity.getUUID().getMostSignificantBits()
                    ^ entity.getUUID().getLeastSignificantBits() ^ (long) now);
        }

        private double seconds(float partial) {
            double now = level.getGameTime() + partial;
            return recovering < 0 ? (now - fellAt) / 20.0 : DeathBodyMotion.recoverySeconds(now - recovering);
        }

        private DeathBodyMotion.Pose motion(float partial) {
            if (recovering >= 0 && !reducedMotion()) {
                // Unroll before rising; do not snap out of the settled pose when revival starts.
                double t = Math.clamp((level.getGameTime() + partial - recovering) / DeathBodyMotion.RECOVERY_TICKS, 0, 1);
                double remaining = 1 - t * t * (3 - 2 * t);
                var motion = DeathBodyMotion.sample(seconds(partial), false);
                return new DeathBodyMotion.Pose(motion.angle(), motion.lift(), motion.progress(), remaining, 0).forDirection(yaw, fallYaw);
            }
            return DeathBodyMotion.sample(seconds(partial), reducedMotion()).forDirection(yaw, fallYaw);
        }

        private void particles() {
            if (level.getGameTime() % 2 != 0) return;
            var random = level.random;
            int tint = source instanceof TraumaTeamEntity ? 0x66FFC1 : 0x55CEFF;
            var dust = new DustParticleOptions(new Vector3f((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F), 0.65F);
            double radians = Math.toRadians(fallYaw);
            for (int i = 0; i < 3; i++) {
                double along = random.nextDouble() * 1.7;
                level.addParticle(dust, position.x + Math.sin(radians) * along + random.nextGaussian() * 0.16,
                        position.y + 0.25 + random.nextDouble() * 0.16, position.z - Math.cos(radians) * along + random.nextGaussian() * 0.16, 0, 0.01, 0);
            }
            if (level.getGameTime() % 4 == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    position.x + Math.sin(radians) * 0.8, position.y + 0.3, position.z - Math.cos(radians) * 0.8, 0, 0.02, 0);
        }
    }
    private DeathBodies() { }
}
