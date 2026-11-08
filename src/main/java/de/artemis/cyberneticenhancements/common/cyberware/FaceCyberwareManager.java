package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class FaceCyberwareManager {
    private static final String BASIC_KIROSHI_OPTICS_ID = "basic_kiroshi_optics";
    private static final String CLAIRVOYANT_ID = "clairvoyant";
    private static final String DOOMSAYER_ID = "doomsayer";
    private static final String SENTRY_ID = "sentry";
    private static final String STALKER_ID = "stalker";
    private static final String THE_ORACLE_ID = "the_oracle";
    private static final String COCKATRICE_ID = "cockatrice";
    private static final String FACEPLATE_ID = "behavioral_imprint_synced_faceplate";
    private static final String FORAGER_LENS_ID = "forager_lens";
    private static final String VEIN_READER_OPTICS_ID = "vein_reader_optics";
    private static final String RELIC_SCANNER_ID = "relic_scanner";
    private static final double ORACLE_RANGE = CyberwareBalance.doubleValue("face.oracle.range");
    private static final int ORACLE_MAX_HOSTILES = CyberwareBalance.intValue("face.oracle.max_hostiles");
    private static final int ORACLE_VERTICAL_SCAN = CyberwareBalance.intValue("face.oracle.vertical_scan");
    private static final int ORACLE_MAX_BLOCKS = CyberwareBalance.intValue("face.oracle.max_blocks");
    private static final int COCKATRICE_LOCK_TICKS = CyberwareBalance.intValue("face.cockatrice.lock_ticks");
    private static final int COCKATRICE_TRACK_REFRESH_TICKS = CyberwareBalance.intValue("face.cockatrice.track_refresh_ticks");
    private static final double COCKATRICE_TARGET_RANGE = CyberwareBalance.doubleValue("face.cockatrice.target_range");
    private static final int FACEPLATE_DURATION_TICKS = CyberwareBalance.intValue("face.faceplate.duration_ticks");
    private static final double FACEPLATE_SCRAMBLE_RADIUS = CyberwareBalance.doubleValue("face.faceplate.scramble_radius");
    private static final double FACEPLATE_SAFE_BUBBLE_SQR = CyberwareBalance.doubleValue("face.faceplate.safe_bubble_sqr");
    private static final DustParticleOptions COCKATRICE_TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(0.12F, 0.89F, 0.71F), 0.9F);
    private static final DustParticleOptions COCKATRICE_TRAIL_LEAD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.38F, 1.0F, 0.86F), 1.15F);

    private static final Map<UUID, String> ACTIVE_FACE_IDS = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_FACE_UNTIL_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_FACE_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, UUID> ACTIVE_FACE_TARGETS = new HashMap<>();
    private static final Map<UUID, Long> FACE_COOLDOWN_UNTIL_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FACE_COOLDOWN_TOTAL_SECONDS = new HashMap<>();

    private FaceCyberwareManager() {
    }

    public static void activate(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CyberwareItem faceware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.FACE);
        if (faceware == null) {
            notify(player, "message.cyberneticenhancements.face.no_cyberware");
            return;
        }

        String id = faceware.getDefinition().id();
        if (!hasActiveAbility(id)) {
            notify(player, "message.cyberneticenhancements.face.no_active_ability");
            return;
        }

        int cooldownSeconds = getCooldownSecondsRemaining(player);
        if (cooldownSeconds > 0) {
            notify(player, "message.cyberneticenhancements.face.cooldown", Component.translatable(itemTranslationKey(id)), cooldownSeconds);
            return;
        }

        if (getActiveSecondsRemaining(player) > 0) {
            notify(player, "message.cyberneticenhancements.face.active", Component.translatable(itemTranslationKey(id)));
            return;
        }

        switch (id) {
            case BASIC_KIROSHI_OPTICS_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case CLAIRVOYANT_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case DOOMSAYER_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case SENTRY_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case STALKER_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case THE_ORACLE_ID -> activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                    "message.cyberneticenhancements.face.scan");
            case COCKATRICE_ID -> {
                LivingEntity target = findViewTarget(player, COCKATRICE_TARGET_RANGE, 0.12D, false);
                if (target == null) {
                    target = findViewTarget(player, CyberwareBalance.doubleValue("face.cockatrice.fallback_target_range"), -0.20D, true);
                }
                if (target != null) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.MARK, COCKATRICE_LOCK_TICKS, 2, true);
                    sendHighlightPayload(player, List.of(), List.of(target.getUUID()), COCKATRICE_LOCK_TICKS);
                }
                TemporaryCyberwareEffectManager.addEffects(player, COCKATRICE_LOCK_TICKS,
                        CyberwareEffect.damage(CyberwareBalance.doubleValue("face.cockatrice.damage_bonus")),
                        CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("face.cockatrice.move_speed_bonus")),
                        CyberwareEffect.attackSpeed(CyberwareBalance.doubleValue("face.cockatrice.attack_speed_bonus")),
                        CyberwareEffect.entityReach(CyberwareBalance.doubleValue("face.cockatrice.entity_reach_bonus")));
                activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), target == null ? null : target.getUUID(),
                        "message.cyberneticenhancements.face.cockatrice");
            }
            case FACEPLATE_ID -> {
                scrambleNearbyAggro(player, FACEPLATE_SCRAMBLE_RADIUS, true);
                TemporaryCyberwareEffectManager.addEffects(player, FACEPLATE_DURATION_TICKS,
                        CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("face.faceplate.move_speed_bonus")),
                        CyberwareEffect.stepHeight(CyberwareBalance.doubleValue("face.faceplate.step_height_bonus")),
                        CyberwareEffect.damageReduction(CyberwareBalance.doubleValue("face.faceplate.damage_reduction_bonus")));
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, false));
                activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                        "message.cyberneticenhancements.face.faceplate");
            }
            case VEIN_READER_OPTICS_ID, RELIC_SCANNER_ID ->
                    activateScan(player, inventory, id, faceActiveSeconds(id), faceCooldownSeconds(id), null,
                            "message.cyberneticenhancements.face.scan");
            default -> notify(player, "message.cyberneticenhancements.face.no_active_ability");
        }
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        if (inventory.countInstalledCyberware(FORAGER_LENS_ID) > 0 && player.tickCount % 20 == 0) {
            pulseForagerLens(player);
        }

        String activeId = ACTIVE_FACE_IDS.get(player.getUUID());
        if (activeId == null) {
            return;
        }

        long gameTime = player.level().getGameTime();
        long until = ACTIVE_FACE_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L);
        if (until <= gameTime || !player.isAlive()) {
            clearActive(player);
            return;
        }

        switch (activeId) {
            case BASIC_KIROSHI_OPTICS_ID -> {
                if (shouldPulse(gameTime, 10)) {
                    pulseBasicKiroshi(player);
                }
            }
            case CLAIRVOYANT_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseClairvoyant(player);
                }
            }
            case DOOMSAYER_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseDoomsayer(player);
                }
            }
            case SENTRY_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseSentry(player);
                }
            }
            case STALKER_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseStalker(player);
                }
            }
            case THE_ORACLE_ID -> {
                if (shouldPulse(gameTime, 6)) {
                    pulseOracle(player);
                }
            }
            case COCKATRICE_ID -> {
                if (shouldPulse(gameTime, 6)) {
                    pulseCockatrice(player);
                }
            }
            case FACEPLATE_ID -> {
                if (shouldPulse(gameTime, 5)) {
                    pulseFaceplate(player);
                }
            }
            case VEIN_READER_OPTICS_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseVeinReader(player);
                }
            }
            case RELIC_SCANNER_ID -> {
                if (shouldPulse(gameTime, 8)) {
                    pulseRelicScanner(player);
                }
            }
            default -> clearActive(player);
        }
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case BASIC_KIROSHI_OPTICS_ID,
                 CLAIRVOYANT_ID,
                 DOOMSAYER_ID,
                 SENTRY_ID,
                 STALKER_ID,
                 THE_ORACLE_ID,
                 COCKATRICE_ID,
                 FACEPLATE_ID,
                 FORAGER_LENS_ID,
                 VEIN_READER_OPTICS_ID,
                 RELIC_SCANNER_ID -> true;
            default -> false;
        };
    }

    public static void appendBehaviorTooltip(CyberwareDefinition definition, List<Component> tooltipComponents) {
        if (!hasSpecialBehavior(definition.id())) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.special." + definition.id())
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    public static String getInstalledFaceTranslationKey(PlayerCyberwareInventory inventory) {
        CyberwareItem faceware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.FACE);
        if (faceware == null || !hasActiveAbility(faceware.getDefinition().id())) {
            return "";
        }
        return itemTranslationKey(faceware.getDefinition().id());
    }

    public static int getActiveSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, ACTIVE_FACE_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static int getActiveTotalSeconds(Player player) {
        return ACTIVE_FACE_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getCooldownSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, FACE_COOLDOWN_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static int getCooldownTotalSeconds(Player player) {
        return FACE_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static void clearCooldowns(Player player) {
        clearActive(player);
        FACE_COOLDOWN_UNTIL_TICKS.remove(player.getUUID());
        FACE_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    public static void reduceTrackedCooldowns(Player player, long flatTicks, double percentRefund) {
        long gameTime = player.level().getGameTime();
        long until = FACE_COOLDOWN_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L);
        if (until <= gameTime) {
            return;
        }

        long remaining = until - gameTime;
        if (percentRefund > 0.0D) {
            remaining = Math.max(0L, Math.round(remaining * (1.0D - percentRefund)));
        }
        if (flatTicks > 0L) {
            remaining = Math.max(0L, remaining - flatTicks);
        }

        if (remaining <= 0L) {
            FACE_COOLDOWN_UNTIL_TICKS.remove(player.getUUID());
            FACE_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
            return;
        }
        FACE_COOLDOWN_UNTIL_TICKS.put(player.getUUID(), gameTime + remaining);
    }

    private static boolean hasActiveAbility(String id) {
        return switch (id) {
            case BASIC_KIROSHI_OPTICS_ID,
                 CLAIRVOYANT_ID,
                 DOOMSAYER_ID,
                 SENTRY_ID,
                 STALKER_ID,
                 THE_ORACLE_ID,
                 COCKATRICE_ID,
                 FACEPLATE_ID,
                 VEIN_READER_OPTICS_ID,
                 RELIC_SCANNER_ID -> true;
            default -> false;
        };
    }

    private static void activateScan(
            Player player,
            PlayerCyberwareInventory inventory,
            String id,
            int activeSeconds,
            int cooldownSeconds,
            UUID targetId,
            String messageKey
    ) {
        long gameTime = player.level().getGameTime();
        ACTIVE_FACE_IDS.put(player.getUUID(), id);
        ACTIVE_FACE_UNTIL_TICKS.put(player.getUUID(), gameTime + activeSeconds * 20L);
        ACTIVE_FACE_TOTAL_SECONDS.put(player.getUUID(), activeSeconds);
        if (targetId != null) {
            ACTIVE_FACE_TARGETS.put(player.getUUID(), targetId);
        } else {
            ACTIVE_FACE_TARGETS.remove(player.getUUID());
        }

        long adjustedCooldown = FrontalCortexManager.adjustCyberwareCooldownTicks(player, inventory, cooldownSeconds * 20L, true);
        FACE_COOLDOWN_UNTIL_TICKS.put(player.getUUID(), gameTime + adjustedCooldown);
        FACE_COOLDOWN_TOTAL_SECONDS.put(player.getUUID(), (int) Math.max(1L, (adjustedCooldown + 19L) / 20L));
        CyberstrainManager.onCyberwareActivated(player, Math.max(4.0D, activeSeconds * 0.45D));
        CyberwareEffects.refreshPlayerCyberware(player);
        playActivationFeedback(player, id);
        notify(player, messageKey, Component.translatable(itemTranslationKey(id)), activeSeconds);
    }

    private static void pulseBasicKiroshi(Player player) {
        revealHostiles(player, 14.0D, 4, 0.05D, false, false, 50, false, true);
        spawnEyePulse(player, ParticleTypes.END_ROD, 10, 0.02D);
    }

    private static void pulseClairvoyant(Player player) {
        revealHostiles(player, 20.0D, 8, -1.0D, false, true, 70, false, true);
        spawnEyePulse(player, ParticleTypes.ENCHANT, 12, 0.05D);
    }

    private static void pulseDoomsayer(Player player) {
        revealExplosives(player, 18, 5, 12);
        spawnEyePulse(player, ParticleTypes.CRIT, 10, 0.04D);
    }

    private static void pulseSentry(Player player) {
        revealSentries(player, 18, 5, 12);
        spawnEyePulse(player, ParticleTypes.SOUL, 10, 0.03D);
    }

    private static void pulseStalker(Player player) {
        revealHostiles(player, 22.0D, 8, -1.0D, true, true, 80, false, true);
        spawnEyePulse(player, ParticleTypes.ENCHANT, 14, 0.06D);
    }

    private static void pulseOracle(Player player) {
        revealHostiles(player, ORACLE_RANGE, ORACLE_MAX_HOSTILES, -1.0D, false, true, 90, false, true);
        revealExplosives(player, (int) ORACLE_RANGE, ORACLE_VERTICAL_SCAN, ORACLE_MAX_BLOCKS);
        revealSentries(player, (int) ORACLE_RANGE, ORACLE_VERTICAL_SCAN, ORACLE_MAX_BLOCKS);
        spawnEyePulse(player, ParticleTypes.GLOW, 14, 0.05D);
    }

    private static void pulseCockatrice(Player player) {
        LivingEntity target = resolveTrackedTarget(player);
        if (target == null) {
            target = findViewTarget(player, COCKATRICE_TARGET_RANGE, -0.35D, true);
            if (target != null) {
                ACTIVE_FACE_TARGETS.put(player.getUUID(), target.getUUID());
                CombatStatusManager.applyStatus(target, CombatStatusType.MARK, COCKATRICE_LOCK_TICKS, 2, true);
            }
        }

        if (target != null) {
            CombatStatusManager.applyStatus(target, CombatStatusType.MARK, COCKATRICE_TRACK_REFRESH_TICKS, 1, true);
            sendHighlightPayload(player, List.of(), List.of(target.getUUID()), 40);
            spawnTargetPulse(player, target, ParticleTypes.ENCHANT, 8, 0.05D);
            spawnTrackingTrail(player, target);
        }
        spawnEyePulse(player, ParticleTypes.ENCHANT, 12, 0.05D);
    }

    private static void pulseFaceplate(Player player) {
        scrambleNearbyAggro(player, FACEPLATE_SCRAMBLE_RADIUS, true);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, false));
        spawnDisguisePulse(player);
        spawnEyePulse(player, ParticleTypes.CLOUD, 8, 0.02D);
    }

    private static void pulseForagerLens(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<BlockPos> targets = collectMatchingBlocks(
                level,
                player,
                CyberwareBalance.intValue("face.forager.range"),
                CyberwareBalance.intValue("face.forager.vertical_scan"),
                CyberwareBalance.intValue("face.forager.max_blocks"),
                FaceCyberwareManager::isForagerTarget
        );
        if (!targets.isEmpty()) {
            sendHighlightPayload(player, targets, List.of(), CyberwareBalance.intValue("face.passive_highlight_ttl_ticks"));
        }
    }

    private static void pulseVeinReader(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<BlockPos> highlightedBlocks = scanBlocks(
                level,
                player,
                CyberwareBalance.intValue("face.vein_reader.radius"),
                CyberwareBalance.intValue("face.vein_reader.vertical_scan"),
                CyberwareBalance.intValue("face.vein_reader.max_blocks"),
                FaceCyberwareManager::isOreBlock,
                ParticleTypes.GLOW,
                false
        );
        if (!highlightedBlocks.isEmpty()) {
            sendHighlightPayload(player, highlightedBlocks, List.of(), 30);
        }
    }

    private static void pulseRelicScanner(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<BlockPos> highlightedBlocks = scanBlocks(
                level,
                player,
                CyberwareBalance.intValue("face.relic_scanner.radius"),
                CyberwareBalance.intValue("face.relic_scanner.vertical_scan"),
                CyberwareBalance.intValue("face.relic_scanner.max_blocks"),
                FaceCyberwareManager::isRelicTarget,
                ParticleTypes.ENCHANT,
                true
        );
        if (!highlightedBlocks.isEmpty()) {
            sendHighlightPayload(player, highlightedBlocks, List.of(), 35);
        }
    }

    private static void revealHostiles(
            Player player,
            double range,
            int maxTargets,
            double dotThreshold,
            boolean hiddenOnly,
            boolean throughWalls,
            int glowTicks,
            boolean mark,
            boolean outline
    ) {
        List<LivingEntity> targets = findHostiles(player, range, maxTargets, dotThreshold, hiddenOnly, throughWalls);
        List<UUID> highlightedEntities = outline ? new ArrayList<>() : null;
        for (LivingEntity target : targets) {
            if (mark) {
                CombatStatusManager.applyStatus(target, CombatStatusType.MARK, glowTicks, 1, true);
            }
            spawnTargetPulse(player, target, ParticleTypes.END_ROD, 4, 0.02D);
            if (highlightedEntities != null) {
                highlightedEntities.add(target.getUUID());
            }
        }
        if (highlightedEntities != null && !highlightedEntities.isEmpty()) {
            sendHighlightPayload(player, List.of(), highlightedEntities, Math.max(20, glowTicks));
        }
    }

    private static void revealExplosives(Player player, int radius, int vertical, int maxBlocks) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<UUID> highlightedEntities = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                living -> living != player && living.isAlive() && living instanceof Creeper)) {
            highlightHazardEntity(level, entity);
            highlightedEntities.add(entity.getUUID());
        }
        for (Entity entity : player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate.getType() == EntityType.TNT_MINECART)) {
            highlightHazardPoint(level, entity.position().add(0.0D, 0.35D, 0.0D), true);
            highlightedEntities.add(entity.getUUID());
        }
        for (EndCrystal crystal : player.level().getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(radius))) {
            highlightHazardPoint(level, crystal.position().add(0.0D, 0.6D, 0.0D), true);
            highlightedEntities.add(crystal.getUUID());
        }
        for (PrimedTnt primedTnt : player.level().getEntitiesOfClass(PrimedTnt.class, player.getBoundingBox().inflate(radius))) {
            highlightHazardPoint(level, primedTnt.position().add(0.0D, 0.3D, 0.0D), true);
            highlightedEntities.add(primedTnt.getUUID());
        }

        List<BlockPos> highlightedBlocks = scanBlocks(level, player, radius, vertical, maxBlocks,
                state -> state.is(Blocks.TNT)
                        || state.is(Blocks.RESPAWN_ANCHOR)
                        || state.is(BlockTags.CAMPFIRES)
                        || state.is(BlockTags.PRESSURE_PLATES)
                        || state.is(Blocks.TRIPWIRE)
                        || state.is(Blocks.TRIPWIRE_HOOK),
                ParticleTypes.CRIT,
                true);
        if (!highlightedBlocks.isEmpty() || !highlightedEntities.isEmpty()) {
            sendHighlightPayload(player, highlightedBlocks, highlightedEntities, 30);
        }
    }

    private static void revealSentries(Player player, int radius, int vertical, int maxBlocks) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<UUID> highlightedEntities = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                living -> living != player
                        && living.isAlive()
                        && (living instanceof RangedAttackMob || living instanceof Shulker || living instanceof AbstractGolem))) {
            highlightHazardEntity(level, entity);
            highlightedEntities.add(entity.getUUID());
        }

        List<BlockPos> highlightedBlocks = scanBlocks(level, player, radius, vertical, maxBlocks,
                state -> state.is(Blocks.DISPENSER) || state.is(Blocks.DROPPER) || state.is(Blocks.OBSERVER),
                ParticleTypes.SOUL,
                false);
        if (!highlightedBlocks.isEmpty() || !highlightedEntities.isEmpty()) {
            sendHighlightPayload(player, highlightedBlocks, highlightedEntities, 30);
        }
    }

    private static List<BlockPos> collectMatchingBlocks(
            ServerLevel level,
            Player player,
            int radius,
            int vertical,
            int maxResults,
            Predicate<BlockState> matcher
    ) {
        BlockPos origin = player.blockPosition();
        List<BlockPos> results = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -vertical, -radius),
                origin.offset(radius, vertical, radius))) {
            if (!matcher.test(level.getBlockState(pos))) {
                continue;
            }
            results.add(pos.immutable());
            if (results.size() >= maxResults) {
                break;
            }
        }
        return results;
    }

    private static boolean isForagerTarget(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (state.is(Blocks.COCOA) && state.hasProperty(BlockStateProperties.AGE_2)) {
            return state.getValue(BlockStateProperties.AGE_2) >= 2;
        }
        if ((state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) && state.hasProperty(BlockStateProperties.BERRIES)) {
            return state.getValue(BlockStateProperties.BERRIES);
        }
        if (state.is(Blocks.PITCHER_CROP) && state.hasProperty(BlockStateProperties.AGE_4)) {
            return state.getValue(BlockStateProperties.AGE_4) >= 4;
        }
        if (state.is(Blocks.TORCHFLOWER_CROP) && state.hasProperty(BlockStateProperties.AGE_1)) {
            return state.getValue(BlockStateProperties.AGE_1) >= 1;
        }
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            return true;
        }
        return (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE))
                && state.hasProperty(BlockStateProperties.LEVEL_HONEY)
                && state.getValue(BlockStateProperties.LEVEL_HONEY) >= 5;
    }

    private static boolean isOreBlock(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.endsWith("_ore") || path.equals("ancient_debris");
    }

    private static boolean isRelicTarget(BlockState state) {
        return state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.SPAWNER)
                || state.is(Blocks.SUSPICIOUS_SAND)
                || state.is(Blocks.SUSPICIOUS_GRAVEL)
                || state.is(Blocks.DECORATED_POT)
                || state.is(Blocks.TRIAL_SPAWNER)
                || state.is(Blocks.VAULT)
                || state.is(ModBlocks.RELIC_CACHE.get());
    }

    private static void scrambleNearbyAggro(Player player, double radius, boolean immediate) {
        AggroControlHelper.scrambleNearbyAggro(
                player,
                radius,
                mob -> AggroControlHelper.hasDirectAggro(mob, player)
                        || mob.hasLineOfSight(player) && mob.distanceToSqr(player) > FACEPLATE_SAFE_BUBBLE_SQR,
                  immediate ? 0.0D : 16.0D,
                  (mob, nearbyMobs) -> chooseScrambleTarget(player, mob, nearbyMobs)
          );
    }

    private static int faceActiveSeconds(String id) {
        return CyberwareBalance.intValue("face.active_seconds." + id);
    }

    private static int faceCooldownSeconds(String id) {
        return CyberwareBalance.intValue("face.cooldown_seconds." + id);
    }

    private static LivingEntity chooseScrambleTarget(Player player, Mob mob, List<Mob> nearbyMobs) {
        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob candidate : nearbyMobs) {
            if (candidate == mob || !candidate.isAlive() || candidate.isAlliedTo(mob)) {
                continue;
            }
            double distance = mob.distanceToSqr(candidate);
            if (distance < bestDistance && distance <= 10.0D * 10.0D) {
                bestDistance = distance;
                bestTarget = candidate;
            }
        }

        if (bestTarget != null) {
            return bestTarget;
        }

        for (Player otherPlayer : player.level().players()) {
            if (otherPlayer == player || otherPlayer.isCreative() || otherPlayer.isSpectator() || otherPlayer.isAlliedTo(mob)) {
                continue;
            }
            double distance = mob.distanceToSqr(otherPlayer);
            if (distance < bestDistance && distance <= 12.0D * 12.0D) {
                bestDistance = distance;
                bestTarget = otherPlayer;
            }
        }
        return bestTarget;
    }

    private static List<LivingEntity> findHostiles(
            Player player,
            double range,
            int maxTargets,
            double dotThreshold,
            boolean hiddenOnly,
            boolean throughWalls
    ) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive() && isHostileCandidate(player, entity)));
        targets.removeIf(entity -> {
            Vec3 delta = entity.getBoundingBox().getCenter().subtract(origin);
            if (delta.lengthSqr() > range * range) {
                return true;
            }
            if (dotThreshold > -1.0D && delta.normalize().dot(look) < dotThreshold) {
                return true;
            }
            boolean hasLineOfSight = player.hasLineOfSight(entity);
            if (hiddenOnly && hasLineOfSight) {
                return true;
            }
            return !throughWalls && !hiddenOnly && !hasLineOfSight;
        });
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        if (targets.size() > maxTargets) {
            return new ArrayList<>(targets.subList(0, maxTargets));
        }
        return targets;
    }

    private static LivingEntity findViewTarget(Player player, double range, double dotThreshold, boolean allowWalls) {
        List<LivingEntity> targets = findHostiles(player, range, 1, dotThreshold, false, allowWalls);
        return targets.isEmpty() ? null : targets.get(0);
    }

    private static LivingEntity resolveTrackedTarget(Player player) {
        UUID targetId = ACTIVE_FACE_TARGETS.get(player.getUUID());
        if (targetId == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) {
            ACTIVE_FACE_TARGETS.remove(player.getUUID());
            return null;
        }
        return target;
    }

    private static boolean isHostileCandidate(Player player, LivingEntity entity) {
        if (entity instanceof Player targetPlayer) {
            return !targetPlayer.isCreative() && !targetPlayer.isSpectator();
        }
        if (entity instanceof Monster) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() == player;
    }

    private static boolean shouldPulse(long gameTime, int interval) {
        return interval > 0 && gameTime % interval == 0L;
    }

    private static void sendHighlightPayload(Player player, List<BlockPos> blockPositions, List<UUID> entityIds, int ttlTicks) {
        if ((blockPositions.isEmpty() && entityIds.isEmpty()) || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(blockPositions, entityIds, ttlTicks));
    }

    private static void clearActive(Player player) {
        ACTIVE_FACE_IDS.remove(player.getUUID());
        ACTIVE_FACE_UNTIL_TICKS.remove(player.getUUID());
        ACTIVE_FACE_TOTAL_SECONDS.remove(player.getUUID());
        ACTIVE_FACE_TARGETS.remove(player.getUUID());
    }

    private static void playActivationFeedback(Player player, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 eye = player.getEyePosition().add(player.getLookAngle().scale(0.35D));
        ParticleOptions particle = switch (id) {
            case DOOMSAYER_ID -> ParticleTypes.CRIT;
            case SENTRY_ID -> ParticleTypes.SOUL;
            case STALKER_ID, COCKATRICE_ID -> ParticleTypes.ENCHANT;
            case FACEPLATE_ID -> ParticleTypes.CLOUD;
            default -> ParticleTypes.END_ROD;
        };
        level.sendParticles(particle, eye.x, eye.y, eye.z, 14, 0.12D, 0.08D, 0.12D, 0.03D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.55F, 1.35F);
    }

    private static void spawnEyePulse(Player player, ParticleOptions particle, int count, double speed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 eye = player.getEyePosition().add(player.getLookAngle().scale(0.35D));
        level.sendParticles(particle, eye.x, eye.y, eye.z, count, 0.18D, 0.10D, 0.18D, speed);
    }

    private static void spawnTargetPulse(Player player, LivingEntity target, ParticleOptions particle, int count, double speed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(particle, center.x, center.y + 0.25D, center.z, count, 0.18D, 0.22D, 0.18D, speed);
    }

    private static void spawnTrackingTrail(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.35D));
        Vec3 end = target.getBoundingBox().getCenter().add(0.0D, 0.25D, 0.0D);
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 1.0D) {
            return;
        }

        int steps = Mth.clamp((int) Math.ceil(distance / 0.85D), 8, 28);
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (double) steps;
            Vec3 point = start.lerp(end, progress);
            level.sendParticles(COCKATRICE_TRAIL_PARTICLE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        Vec3 leadPoint = start.lerp(end, Math.min(0.22D, 2.5D / Math.max(2.5D, distance)));
        level.sendParticles(COCKATRICE_TRAIL_LEAD_PARTICLE, leadPoint.x, leadPoint.y, leadPoint.z, 2, 0.03D, 0.03D, 0.03D, 0.0D);
    }

    private static void spawnDisguisePulse(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.35D, center.z, 10, 0.45D, 0.55D, 0.45D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.20D, center.z, 6, 0.28D, 0.40D, 0.28D, 0.01D);
    }

    private static void spawnPointPulse(ServerLevel level, Vec3 point, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, point.x, point.y, point.z, count, 0.18D, 0.18D, 0.18D, speed);
    }

    private static void highlightHazardEntity(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.getBoundingBox().getCenter().add(0.0D, 0.25D, 0.0D);
        highlightHazardPoint(level, center, true);
    }

    private static void highlightHazardPoint(ServerLevel level, Vec3 point, boolean playSound) {
        level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 8, 0.22D, 0.22D, 0.22D, 0.04D);
        level.sendParticles(ParticleTypes.GLOW, point.x, point.y, point.z, 6, 0.10D, 0.10D, 0.10D, 0.01D);
        if (playSound) {
            level.playSound(null, point.x, point.y, point.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.18F, 1.65F);
        }
    }

    private static List<BlockPos> scanBlocks(
            ServerLevel level,
            Player player,
            int radius,
            int vertical,
            int maxResults,
            Predicate<BlockState> matcher,
            ParticleOptions particle,
            boolean audible
    ) {
        BlockPos origin = player.blockPosition();
        int emitted = 0;
        List<BlockPos> results = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -vertical, -radius),
                origin.offset(radius, vertical, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!matcher.test(state)) {
                continue;
            }

            Vec3 point = hazardPulsePosition(pos, state);
            spawnPointPulse(level, point, particle, 5, 0.02D);
            if (audible) {
                highlightHazardPoint(level, point, emitted < 4);
            }
            results.add(pos.immutable());
            emitted++;
            if (emitted >= maxResults) {
                return results;
            }
        }
        return results;
    }

    private static Vec3 hazardPulsePosition(BlockPos pos, BlockState state) {
        if (state.is(Blocks.TRIPWIRE)) {
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.06D, pos.getZ() + 0.5D);
        }
        if (state.is(BlockTags.PRESSURE_PLATES)) {
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.10D, pos.getZ() + 0.5D);
        }
        if (state.is(Blocks.TRIPWIRE_HOOK)) {
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.38D, pos.getZ() + 0.5D);
        }
        return Vec3.atCenterOf(pos);
    }

    private static String itemTranslationKey(String id) {
        return "item.cyberneticenhancements." + id;
    }

    private static void notify(Player player, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args).withStyle(ChatFormatting.AQUA), true);
    }
}
