package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PsychosisPursuitController {
    private static final String WANDER_YAW_KEY = "cyberneticenhancements.psychosis_wander_yaw";
    private static final double IMMEDIATE_VISIBLE_TARGET_RANGE = 3.0D;
    private static final double MAX_HIDDEN_TARGET_DIRECT_DISTANCE = CyberwareBalance.doubleValue("cyberstrain.psychosis.hidden_target_max_direct_distance");
    private static final double MAX_HIDDEN_TARGET_PATH_DISTANCE = CyberwareBalance.doubleValue("cyberstrain.psychosis.hidden_target_max_path_distance");
    private static final long PATH_RECALC_INTERVAL_TICKS = 10L;
    private static final long PATH_COLLISION_REPATH_TICKS = 4L;
    private static final long PATH_STUCK_REPATH_TICKS = 12L;
    private static final long PATH_JUMP_HOLD_TICKS = 6L;
    private static final double PATH_NODE_DIRECTION_CHECK_DISTANCE = 2.0D;
    private static final double PATH_PROGRESS_DISTANCE_SQR = 0.18D * 0.18D;
    private static final Map<UUID, PathState> PATH_STATES = new HashMap<>();

    private PsychosisPursuitController() {
    }

    private record CachedTarget(LivingEntity entity, long until) {}
    private static final Map<UUID, CachedTarget> TARGETS = new HashMap<>();
    private static final Map<UUID, PsychosisPursuitProgress> PROGRESS = new HashMap<>();

    public static LivingEntity findTarget(Player player) {
        long now = player.level().getGameTime();
        var progress = PROGRESS.computeIfAbsent(player.getUUID(), ignored -> new PsychosisPursuitProgress());
        var cached = TARGETS.get(player.getUUID());
        boolean reuse = cached != null && now < cached.until() && (cached.entity() == null ||
                cached.entity().isAlive() && cached.entity().level() == player.level()
                && player.distanceToSqr(cached.entity()) <= 24 * 24 && isValidTarget(cached.entity())
                && !progress.skipped(cached.entity().getUUID(), now));
        LivingEntity target = reuse ? cached.entity() : selectTarget(player);
        if (target != null && progress.stalled(target.getUUID(), player.position(), player.distanceTo(target),
                player.distanceToSqr(target) <= 2.8 * 2.8 && player.hasLineOfSight(target), now)) {
            PATH_STATES.remove(player.getUUID());
            target = selectTarget(player);
        }
        TARGETS.put(player.getUUID(), new CachedTarget(target, now + 5));
        return target;
    }

    private static LivingEntity selectTarget(Player player) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(24.0D),
                entity -> entity != player && entity.isAlive() && isValidTarget(entity)
                        && !PROGRESS.get(player.getUUID()).skipped(entity.getUUID(), player.level().getGameTime()));

        LivingEntity recentAggressor = player.getLastHurtByMob();
        List<TargetCandidate> candidates = targets.stream()
                .map(entity -> createTargetCandidate(player, entity, recentAggressor))
                .filter(candidate -> candidate != null)
                .toList();

        Integer bestPriority = candidates.stream()
                .map(TargetCandidate::priority)
                .min(Integer::compareTo)
                .orElse(null);
        if (bestPriority == null) {
            return null;
        }

        List<TargetCandidate> bestPriorityCandidates = candidates.stream()
                .filter(candidate -> candidate.priority() == bestPriority)
                .toList();

        return bestPriorityCandidates.stream()
                .sorted(Comparator
                        .comparingDouble(TargetCandidate::pathDistance)
                        .thenComparingInt(candidate -> candidate.visible() ? 0 : 1)
                        .thenComparingInt(candidate -> candidate.recentAggressor() ? 0 : 1)
                        .thenComparingDouble(TargetCandidate::directDistanceSqr))
                .map(TargetCandidate::entity)
                .findFirst()
                .orElse(null);
    }

    public static PursuitIntent buildHuntIntent(Player player, LivingEntity target, long gameTime) {
        if (player.hasLineOfSight(target) && player.distanceToSqr(target) <= 2.4D * 2.4D)
            return buildCloseRangeIntent(player, target);
        PathFollowTarget pathFollowTarget = resolvePathTargetPosition(player, target, gameTime);
        Vec3 pathTargetPosition = pathFollowTarget.position();
        Vec3 moveDelta = pathTargetPosition.subtract(player.position());
        Vec3 moveHorizontal = flatten(moveDelta);
        if (moveHorizontal.lengthSqr() < 0.0001D) {
            return buildCloseRangeIntent(player, target);
        }

        boolean targetVisible = player.hasLineOfSight(target);
        boolean closeCombat = targetVisible && player.distanceToSqr(target) <= 16.0D;
        Vec3 lookTargetPosition = closeCombat
                ? target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D)
                : pathTargetPosition.add(0.0D, Math.max(0.4D, target.getBbHeight() * 0.25D), 0.0D);
        Vec3 lookDelta = lookTargetPosition.subtract(player.getEyePosition());
        Vec3 lookHorizontal = flatten(lookDelta);
        if (lookHorizontal.lengthSqr() < 0.0001D) {
            lookHorizontal = moveHorizontal;
        }

        float lookYaw = (float) (Math.toDegrees(Math.atan2(lookHorizontal.z, lookHorizontal.x)) - 90.0D);
        float pitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(lookDelta.y, Math.max(0.001D, lookHorizontal.length()))), -35.0D, 35.0D);
        boolean jump = pathFollowTarget.shouldJump() || player.horizontalCollision;
        return new PursuitIntent(lookYaw, pitch, 1.0F, 0.0F, jump, true);
    }

    public static PursuitIntent buildWanderIntent(Player player, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        float yaw = persistentData.contains(WANDER_YAW_KEY) && gameTime % 28L != 0L
                ? persistentData.getFloat(WANDER_YAW_KEY)
                : player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * 45.0F;
        persistentData.putFloat(WANDER_YAW_KEY, yaw);
        return new PursuitIntent(yaw, 0.0F, 0.85F, 0.0F, player.horizontalCollision, true);
    }

    public static void applyIntent(Player player, PursuitIntent intent) {
        player.setYRot(intent.yaw());
        player.setYHeadRot(intent.yaw());
        player.setYBodyRot(intent.yaw());
        player.setXRot(intent.pitch());
        player.setSprinting(intent.sprint());
        applyJumpIfNeeded(player, intent);
    }

    public static void clear(Player player) {
        PATH_STATES.remove(player.getUUID());
        TARGETS.remove(player.getUUID());
        PROGRESS.remove(player.getUUID());
        player.getPersistentData().remove(WANDER_YAW_KEY);
    }

    public static boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isDeadOrDying() || entity.deathTime > 0
                || de.artemis.cyberneticenhancements.common.world.DeathBodySync.fallen(entity)
                || de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.downed(entity)
                || entity instanceof net.minecraft.server.level.ServerPlayer player
                    && de.artemis.cyberneticenhancements.common.world.TraumaCoverage.downed(player)) return false;
        if (shouldIgnoreTarget(entity)) {
            return false;
        }
        if (entity instanceof Player targetPlayer) {
            return !targetPlayer.isCreative() && !targetPlayer.isSpectator();
        }
        return entity instanceof net.minecraft.world.entity.Mob;
    }

    private static boolean shouldIgnoreTarget(LivingEntity entity) {
        return PsychosisTargetingConfig.ignoredEntityTypes().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    private static int targetPriority(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity
                || entity instanceof net.minecraft.world.entity.npc.AbstractVillager) return 1;
        if (entity instanceof TamableAnimal pet && pet.isTame()
                || entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) return 2;
        if (entity instanceof net.minecraft.world.entity.monster.Enemy) return 3;
        return 4;
    }

    private static PathFollowTarget resolvePathTargetPosition(Player player, LivingEntity target, long gameTime) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return new PathFollowTarget(player.position(), false);
        }

        PathState state = PATH_STATES.computeIfAbsent(player.getUUID(), ignored -> new PathState(new Zombie(serverLevel)));
        if (state.navigator().level() != serverLevel) {
            state = new PathState(new Zombie(serverLevel));
            PATH_STATES.put(player.getUUID(), state);
        }

        Zombie navigator = state.navigator();
        navigator.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        navigator.setOnGround(true); // Detached navigation proxies never receive an entity physics tick.
        navigator.setCanPickUpLoot(false);

        boolean targetChanged = !target.getUUID().equals(state.targetId());
        boolean stalePath = state.path() == null || state.path().isDone() || gameTime - state.lastRepathGameTime() >= PATH_RECALC_INTERVAL_TICKS;
        boolean stuck = state.lastProgressGameTime() > 0L && gameTime - state.lastProgressGameTime() >= PATH_STUCK_REPATH_TICKS;
        boolean collisionRepath = player.horizontalCollision
                && state.lastProgressGameTime() > 0L
                && gameTime - state.lastProgressGameTime() >= PATH_COLLISION_REPATH_TICKS;
        if (targetChanged || stalePath || stuck || collisionRepath) {
            Path path = navigator.getNavigation().createPath(target, 0);
            state.targetId(target.getUUID());
            state.path(path);
            state.lastRepathGameTime(gameTime);
            state.lastProgressPosition(player.position());
            state.lastProgressGameTime(gameTime);
        }

        Path path = state.path();
        if (path == null || !path.canReach() || path.isDone()) {
            state.path(null);
            return new PathFollowTarget(player.position(), false);
        }

        advancePath(player, navigator, path);

        updatePathProgress(state, player, gameTime);
        if (path.isDone()) {
            return new PathFollowTarget(player.position(), false);
        }

        Vec3 next = path.getNextEntityPos(navigator);
        boolean shouldJumpNow = next.y - player.getY() > 0.35D
                || player.horizontalCollision
                || state.lastProgressGameTime() > 0L && gameTime - state.lastProgressGameTime() >= 6L && next.y > player.getY() + 0.1D
                || moveRequiresStepUp(player, next);
        if (shouldJumpNow) {
            state.jumpUntilGameTime(gameTime + PATH_JUMP_HOLD_TICKS);
        }
        boolean shouldJump = state.jumpUntilGameTime() > gameTime;
        return new PathFollowTarget(next, shouldJump);
    }

    private static void advancePath(Player player, Zombie navigator, Path path) {
        Vec3 playerPosition = getPathNavigationPosition(player);
        float pathAcceptanceRadius = player.getBbWidth() > 0.75F
                ? player.getBbWidth() / 2.0F
                : 0.75F - player.getBbWidth() / 2.0F;
        Vec3 currentNodePosition = path.getNextEntityPos(navigator);
        boolean closeEnough = Math.abs(player.getX() - currentNodePosition.x) < pathAcceptanceRadius
                && Math.abs(player.getZ() - currentNodePosition.z) < pathAcceptanceRadius
                && Math.abs(player.getY() - currentNodePosition.y) < 1.0D;
        if (closeEnough || shouldAdvanceToNextNode(playerPosition, path, navigator)) {
            path.advance();
        }
    }

    private static boolean shouldAdvanceToNextNode(Vec3 playerPosition, Path path, Zombie navigator) {
        if (path.getNextNodeIndex() + 1 >= path.getNodeCount()) {
            return false;
        }

        Vec3 currentNodeCenter = Vec3.atBottomCenterOf(path.getNextNodePos());
        if (!playerPosition.closerThan(currentNodeCenter, PATH_NODE_DIRECTION_CHECK_DISTANCE)) {
            return false;
        }

        Vec3 currentNodePosition = path.getNextEntityPos(navigator);
        Vec3 nextNodePosition = path.getEntityPosAtNode(navigator, path.getNextNodeIndex() + 1);
        Vec3 toCurrent = currentNodePosition.subtract(playerPosition);
        Vec3 toNext = nextNodePosition.subtract(playerPosition);
        boolean nextIsCloser = toNext.lengthSqr() < toCurrent.lengthSqr();
        boolean currentIsVeryClose = toCurrent.length() < 0.5D;
        if (!nextIsCloser && !currentIsVeryClose) {
            return false;
        }

        return toNext.normalize().dot(toCurrent.normalize()) < 0.0D;
    }

    private static Vec3 getPathNavigationPosition(Player player) {
        return new Vec3(player.getX(), player.getY(), player.getZ());
    }

    private static TargetCandidate createTargetCandidate(Player player, LivingEntity target, LivingEntity recentAggressor) {
        boolean visible = player.hasLineOfSight(target);
        double directDistanceSqr = target.distanceToSqr(player);
        if (visible && directDistanceSqr <= IMMEDIATE_VISIBLE_TARGET_RANGE * IMMEDIATE_VISIBLE_TARGET_RANGE) {
            return new TargetCandidate(
                    target,
                    targetPriority(target),
                    true,
                    target == recentAggressor,
                    Math.sqrt(directDistanceSqr),
                    directDistanceSqr
            );
        }

        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        if (!visible && directDistanceSqr > MAX_HIDDEN_TARGET_DIRECT_DISTANCE * MAX_HIDDEN_TARGET_DIRECT_DISTANCE) {
            return null;
        }

        Zombie navigator = new Zombie(serverLevel);
        navigator.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        navigator.setOnGround(true); // Detached navigation proxies never receive an entity physics tick.
        Path path = navigator.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach() || path.isDone()) {
            return null;
        }

        double pathDistance = computePathDistance(navigator, path, player.position());
        if (!visible && pathDistance > MAX_HIDDEN_TARGET_PATH_DISTANCE) {
            return null;
        }

        return new TargetCandidate(
                target,
                targetPriority(target),
                visible,
                target == recentAggressor,
                pathDistance,
                directDistanceSqr
        );
    }

    private static PursuitIntent buildCloseRangeIntent(Player player, LivingEntity target) {
        Vec3 lookDelta = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D).subtract(player.getEyePosition());
        Vec3 lookHorizontal = flatten(lookDelta);
        if (lookHorizontal.lengthSqr() < 0.0001D) {
            return new PursuitIntent(player.getYRot(), player.getXRot(), 0.0F, 0.0F, false, true);
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(lookHorizontal.z, lookHorizontal.x)) - 90.0D);
        float pitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(lookDelta.y, Math.max(0.001D, lookHorizontal.length()))), -45.0D, 45.0D);
        return new PursuitIntent(yaw, pitch, lookHorizontal.length() > 1.9D ? 0.4F : 0.0F, 0.0F, false, false);
    }

    private static double computePathDistance(Zombie navigator, Path path, Vec3 startPosition) {
        int nodeCount = path.getNodeCount();
        if (nodeCount <= 0) {
            return Double.MAX_VALUE;
        }

        double distance = 0.0D;
        Vec3 previous = startPosition;
        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            Vec3 nodePosition = path.getEntityPosAtNode(navigator, nodeIndex);
            distance += previous.distanceTo(nodePosition);
            previous = nodePosition;
        }
        return distance;
    }

    private static void updatePathProgress(PathState state, Player player, long gameTime) {
        Vec3 position = player.position();
        Vec3 lastProgressPosition = state.lastProgressPosition();
        if (lastProgressPosition == null || position.distanceToSqr(lastProgressPosition) >= PATH_PROGRESS_DISTANCE_SQR) {
            state.lastProgressPosition(position);
            state.lastProgressGameTime(gameTime);
        }
    }

    private static boolean moveRequiresStepUp(Player player, Vec3 next) {
        Vec3 movementDelta = next.subtract(player.position());
        Vec3 horizontal = flatten(movementDelta);
        if (horizontal.lengthSqr() < 0.04D) {
            return false;
        }
        return next.y > player.getY() + 0.1D && horizontal.length() < 1.6D;
    }

    private static void applyJumpIfNeeded(Player player, PursuitIntent intent) {
        if (!intent.jump()
                || !player.onGround()
                || player.onClimbable()
                || player.isInWaterOrBubble()) {
            return;
        }

        player.jumpFromGround();
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        PlayerMotionSyncHelper.sync(player);
    }

    private static Vec3 flatten(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    public record PursuitIntent(float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) {
    }

    private record PathFollowTarget(Vec3 position, boolean shouldJump) {
    }

    private record TargetCandidate(
            LivingEntity entity,
            int priority,
            boolean visible,
            boolean recentAggressor,
            double pathDistance,
            double directDistanceSqr
    ) {
    }

    private static final class PathState {
        private final Zombie navigator;
        private UUID targetId;
        private Path path;
        private long lastRepathGameTime;
        private Vec3 lastProgressPosition;
        private long lastProgressGameTime;
        private long jumpUntilGameTime;

        private PathState(Zombie navigator) {
            this.navigator = navigator;
        }

        private Zombie navigator() {
            return navigator;
        }

        private UUID targetId() {
            return targetId;
        }

        private void targetId(UUID targetId) {
            this.targetId = targetId;
        }

        private Path path() {
            return path;
        }

        private void path(Path path) {
            this.path = path;
        }

        private long lastRepathGameTime() {
            return lastRepathGameTime;
        }

        private void lastRepathGameTime(long lastRepathGameTime) {
            this.lastRepathGameTime = lastRepathGameTime;
        }

        private Vec3 lastProgressPosition() {
            return lastProgressPosition;
        }

        private void lastProgressPosition(Vec3 lastProgressPosition) {
            this.lastProgressPosition = lastProgressPosition;
        }

        private long lastProgressGameTime() {
            return lastProgressGameTime;
        }

        private void lastProgressGameTime(long lastProgressGameTime) {
            this.lastProgressGameTime = lastProgressGameTime;
        }

        private long jumpUntilGameTime() {
            return jumpUntilGameTime;
        }

        private void jumpUntilGameTime(long jumpUntilGameTime) {
            this.jumpUntilGameTime = jumpUntilGameTime;
        }
    }
}
