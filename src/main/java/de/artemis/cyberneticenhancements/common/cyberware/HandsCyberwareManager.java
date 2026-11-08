package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HandsCyberwareManager {
    private static final String BALLISTIC_COPROCESSOR_ID = "ballistic_coprocessor";
    private static final String MICROGENERATOR_ID = "microgenerator";
    private static final String SHOCK_ABSORBER_ID = "shock_absorber";
    private static final String IMMOVABLE_FORCE_ID = "immovable_force";
    private static final String SMART_LINK_ID = "smart_link";
    private static final String PRECISION_MINER_ID = "precision_miner";
    private static final String HARVESTER_HANDS_ID = "harvester_hands";
    private static final long MICROGENERATOR_COOLDOWN_TICKS = CyberwareBalance.intValue("hands.microgenerator.cooldown_ticks");
    private static final long MICROGENERATOR_SURGE_TICKS = CyberwareBalance.intValue("hands.microgenerator.surge_ticks");
    private static final double MICROGENERATOR_ARC_RADIUS = CyberwareBalance.doubleValue("hands.microgenerator.arc_radius");
    private static final int MICROGENERATOR_MAX_ARC_TARGETS = CyberwareBalance.intValue("hands.microgenerator.max_arc_targets");

    private static final Map<UUID, Long> MICROGENERATOR_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, UUID> BALLISTIC_LAST_TARGET = new HashMap<>();
    private static final Map<UUID, Long> BALLISTIC_LAST_TARGET_UNTIL_TICK = new HashMap<>();

    private HandsCyberwareManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case BALLISTIC_COPROCESSOR_ID,
                 MICROGENERATOR_ID,
                 SHOCK_ABSORBER_ID,
                 IMMOVABLE_FORCE_ID,
                 SMART_LINK_ID,
                 PRECISION_MINER_ID,
                 HARVESTER_HANDS_ID -> true;
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

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            applyShockAbsorberDefense(event, player);
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || event.getEntity() == player) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        LivingEntity target = event.getEntity();
        Entity directEntity = source.getDirectEntity();
        boolean rangedHit = isRangedHit(player, directEntity);
        boolean meleeHit = isMeleeHit(player, directEntity);

        if (rangedHit) {
            int ballisticCount = inventory.countInstalledCyberware(BALLISTIC_COPROCESSOR_ID);
            if (ballisticCount > 0) {
                applyBallisticCoprocessor(player, target, event, ballisticCount);
            }

            int smartLinkCount = countSmartLinkFamily(inventory);
            if (smartLinkCount > 0) {
                event.setAmount(event.getAmount() + (float) (CyberwareBalance.doubleValue("hands.smart_link.damage_bonus") * smartLinkCount));
                CombatStatusManager.applyStatus(target, CombatStatusType.MARK, CyberwareBalance.intValue("hands.smart_link.mark_ticks") + smartLinkCount * 20L, 1 + Math.max(0, smartLinkCount - 1), true);
                sendHighlight(player, target, CyberwareBalance.intValue("hands.smart_link.highlight_ttl_ticks"));
            }
        }

        if (meleeHit) {
            int microgeneratorCount = inventory.countInstalledCyberware(MICROGENERATOR_ID);
            if (microgeneratorCount > 0) {
                triggerMicrogenerator(player, target, event, microgeneratorCount);
            }
        }

        if (rangedHit || meleeHit) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int shockAbsorberCount = inventory.countInstalledCyberware(SHOCK_ABSORBER_ID);
        int immovableForceCount = inventory.countInstalledCyberware(IMMOVABLE_FORCE_ID);
        if (shockAbsorberCount <= 0 && immovableForceCount <= 0) {
            return;
        }

        float strength = event.getStrength();
        if (shockAbsorberCount > 0) {
            strength *= Math.max((float) CyberwareBalance.doubleValue("hands.shock_absorber.knockback_min_factor"),
                    1.0F - (float) CyberwareBalance.doubleValue("hands.shock_absorber.knockback_per_stack") * shockAbsorberCount);
        }
        if (immovableForceCount > 0) {
            strength *= player.isCrouching()
                    ? (float) CyberwareBalance.doubleValue("hands.immovable_force.crouch_knockback_factor")
                    : (float) CyberwareBalance.doubleValue("hands.immovable_force.knockback_factor");
        }
        event.setStrength(strength);
    }

    public static void mergePassiveEffects(Player player, PlayerCyberwareInventory inventory, java.util.EnumMap<CyberwareEffectType, Double> totals) {
    }

    public static boolean requiresRealtimeRefresh(Player player) {
        return false;
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int precisionMinerCount = inventory.countInstalledCyberware(PRECISION_MINER_ID);
        if (precisionMinerCount <= 0 || !isPrecisionMiningTarget(player, event.getState())) {
            return;
        }

        float multiplier = 1.0F + (float) (CyberwareBalance.doubleValue("hands.precision_miner.break_speed_bonus") * precisionMinerCount);
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        if (!inventory.hasInstalledCyberware(HARVESTER_HANDS_ID)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        if (!isSupportedHarvestTarget(state)) {
            return;
        }

        if (harvestAndReplant(serverLevel, player, pos, state, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void clearCooldowns(Player player) {
        MICROGENERATOR_READY_UNTIL_TICK.remove(player.getUUID());
        BALLISTIC_LAST_TARGET.remove(player.getUUID());
        BALLISTIC_LAST_TARGET_UNTIL_TICK.remove(player.getUUID());
    }

    private static void applyShockAbsorberDefense(LivingIncomingDamageEvent event, Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int shockAbsorberCount = inventory.countInstalledCyberware(SHOCK_ABSORBER_ID);
        if (shockAbsorberCount <= 0) {
            return;
        }

        DamageSource source = event.getSource();
        if (!isShockAbsorberDamage(source, source.getDirectEntity(), player)) {
            return;
        }

        float dampened = event.getAmount();
        if (source.getDirectEntity() instanceof Projectile) {
            dampened *= Math.max((float) CyberwareBalance.doubleValue("hands.shock_absorber.projectile_min_factor"),
                    1.0F - (float) CyberwareBalance.doubleValue("hands.shock_absorber.projectile_per_stack") * shockAbsorberCount);
        } else if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            dampened *= Math.max((float) CyberwareBalance.doubleValue("hands.shock_absorber.explosion_min_factor"),
                    1.0F - (float) CyberwareBalance.doubleValue("hands.shock_absorber.explosion_per_stack") * shockAbsorberCount);
        } else {
            dampened *= Math.max((float) CyberwareBalance.doubleValue("hands.shock_absorber.impact_min_factor"),
                    1.0F - (float) CyberwareBalance.doubleValue("hands.shock_absorber.impact_per_stack") * shockAbsorberCount);
        }
        event.setAmount(dampened);
    }

    private static void triggerMicrogenerator(Player player, LivingEntity primaryTarget, LivingIncomingDamageEvent event, int installedCount) {
        long gameTime = player.level().getGameTime();
        if (MICROGENERATOR_READY_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) > gameTime) {
            return;
        }

        MICROGENERATOR_READY_UNTIL_TICK.put(player.getUUID(), gameTime + MICROGENERATOR_COOLDOWN_TICKS);
        event.setAmount(event.getAmount() + (float) (CyberwareBalance.doubleValue("hands.microgenerator.extra_damage") * installedCount));

        TemporaryCyberwareEffectManager.addEffects(player, MICROGENERATOR_SURGE_TICKS,
                CyberwareEffect.bonusAbsorption(CyberwareBalance.doubleValue("hands.microgenerator.absorption_bonus") * installedCount),
                CyberwareEffect.attackSpeed(CyberwareBalance.doubleValue("hands.microgenerator.attack_speed_bonus") * installedCount),
                CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("hands.microgenerator.move_speed_bonus") * installedCount));
        ArmCyberwareManager.reduceTrackedCooldowns(player, 25L * installedCount, 0.10D);

        CombatStatusManager.applyStatus(primaryTarget, CombatStatusType.SHOCK,
                CyberwareBalance.intValue("hands.microgenerator.primary_shock_ticks") + installedCount * 10L, 1, true);

        List<UUID> highlighted = new java.util.ArrayList<>();
        highlighted.add(primaryTarget.getUUID());
        int remainingArcTargets = MICROGENERATOR_MAX_ARC_TARGETS + Math.max(0, installedCount - 1);

        for (LivingEntity nearby : primaryTarget.level().getEntitiesOfClass(LivingEntity.class,
                primaryTarget.getBoundingBox().inflate(MICROGENERATOR_ARC_RADIUS + installedCount * CyberwareBalance.doubleValue("hands.microgenerator.arc_radius_per_extra")),
                candidate -> candidate.isAlive() && candidate != player && candidate != primaryTarget)) {
            if (remainingArcTargets-- <= 0) {
                break;
            }

            CombatStatusManager.applyStatus(nearby, CombatStatusType.SHOCK,
                    CyberwareBalance.intValue("hands.microgenerator.arc_shock_ticks") + installedCount * 10L, 1, true);
            highlighted.add(nearby.getUUID());
        }

        sendHighlight(player, highlighted, CyberwareBalance.intValue("hands.microgenerator.highlight_ttl_ticks"));
    }

    private static int countSmartLinkFamily(PlayerCyberwareInventory inventory) {
        return inventory.countInstalledCyberware(SMART_LINK_ID);
    }

    private static void applyBallisticCoprocessor(Player player, LivingEntity target, LivingIncomingDamageEvent event, int installedCount) {
        long gameTime = player.level().getGameTime();
        double distance = Math.sqrt(player.distanceToSqr(target));
        boolean sameTargetChain = target.getUUID().equals(BALLISTIC_LAST_TARGET.get(player.getUUID()))
                && BALLISTIC_LAST_TARGET_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) > gameTime;
        boolean unawareTarget = target instanceof Mob mob && mob.getTarget() != player;

        float damageBonus = (float) (CyberwareBalance.doubleValue("hands.ballistic.base_damage_bonus") * installedCount);
        if (distance >= CyberwareBalance.doubleValue("hands.ballistic.mid_range_threshold")) {
            damageBonus += (float) (CyberwareBalance.doubleValue("hands.ballistic.mid_range_damage_bonus") * installedCount);
        }
        if (distance >= CyberwareBalance.doubleValue("hands.ballistic.long_range_threshold")) {
            damageBonus += (float) (CyberwareBalance.doubleValue("hands.ballistic.long_range_damage_bonus") * installedCount);
        }
        if (sameTargetChain) {
            damageBonus += (float) (CyberwareBalance.doubleValue("hands.ballistic.chain_damage_bonus") * installedCount);
        }
        if (unawareTarget) {
            damageBonus += (float) (CyberwareBalance.doubleValue("hands.ballistic.unaware_damage_bonus") * installedCount);
        }

        event.setAmount(event.getAmount() + damageBonus);

        int traumaStacks = sameTargetChain ? 2 : 1;
        long traumaDuration = distance >= CyberwareBalance.doubleValue("hands.ballistic.trauma_long_range_threshold")
                ? CyberwareBalance.intValue("hands.ballistic.trauma_long_ticks")
                : CyberwareBalance.intValue("hands.ballistic.trauma_base_ticks");
        if (distance >= CyberwareBalance.doubleValue("hands.ballistic.mid_range_threshold") || unawareTarget || sameTargetChain) {
            CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, traumaDuration, traumaStacks, true);
        }

        if (distance >= CyberwareBalance.doubleValue("hands.ballistic.trauma_long_range_threshold") || sameTargetChain) {
            sendHighlight(player, target, CyberwareBalance.intValue("hands.ballistic.highlight_ttl_ticks"));
        }

        TemporaryCyberwareEffectManager.addEffects(player, CyberwareBalance.intValue("hands.ballistic.handling_buff_ticks"),
                CyberwareEffect.attackSpeed(CyberwareBalance.doubleValue("hands.ballistic.attack_speed_bonus") * installedCount),
                CyberwareEffect.knockbackResistance(CyberwareBalance.doubleValue("hands.ballistic.knockback_resistance_bonus") * installedCount));
        BALLISTIC_LAST_TARGET.put(player.getUUID(), target.getUUID());
        BALLISTIC_LAST_TARGET_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("hands.ballistic.chain_window_ticks"));
    }

    private static boolean isRangedHit(Player player, Entity directEntity) {
        return directEntity instanceof Projectile projectile && projectile.getOwner() == player;
    }

    private static boolean isMeleeHit(Player player, Entity directEntity) {
        return directEntity == player;
    }

    private static boolean isShockAbsorberDamage(DamageSource source, Entity directEntity, Player player) {
        return directEntity instanceof Projectile projectile && projectile.getOwner() != player
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FALLING_BLOCK);
    }

    private static boolean isPrecisionMiningTarget(Player player, BlockState state) {
        if (!isNaturalStoneOrOre(state)) {
            return false;
        }

        ItemStack heldItem = player.getMainHandItem();
        return !heldItem.isEmpty() && heldItem.isCorrectToolForDrops(state);
    }

    private static boolean isNaturalStoneOrOre(BlockState state) {
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            return true;
        }

        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.endsWith("_ore") || path.equals("ancient_debris");
    }

    private static boolean isSupportedHarvestTarget(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
    }

    private static boolean harvestAndReplant(ServerLevel level, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        Block block = state.getBlock();
        ItemStack replantStack = resolveReplantStack(block);
        if (replantStack.isEmpty()) {
            return false;
        }

        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        ItemStack replantRemainder = replantStack.copy();
        boolean consumedReplant = false;
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || !ItemStack.isSameItemSameComponents(drop, replantRemainder)) {
                continue;
            }

            int removed = Math.min(drop.getCount(), replantRemainder.getCount());
            drop.shrink(removed);
            replantRemainder.shrink(removed);
            if (replantRemainder.isEmpty()) {
                consumedReplant = true;
                break;
            }
        }

        if (!consumedReplant) {
            return false;
        }

        BlockState replantedState = resolveReplantedState(state);
        level.setBlock(pos, replantedState, Block.UPDATE_ALL_IMMEDIATE);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        level.levelEvent(2001, pos, Block.getId(state));
        return true;
    }

    private static ItemStack resolveReplantStack(Block block) {
        return switch (BuiltInRegistries.BLOCK.getKey(block).getPath()) {
            case "wheat" -> new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS);
            case "beetroots" -> new ItemStack(net.minecraft.world.item.Items.BEETROOT_SEEDS);
            case "carrots" -> new ItemStack(net.minecraft.world.item.Items.CARROT);
            case "potatoes" -> new ItemStack(net.minecraft.world.item.Items.POTATO);
            case "nether_wart" -> new ItemStack(net.minecraft.world.item.Items.NETHER_WART);
            default -> ItemStack.EMPTY;
        };
    }

    private static BlockState resolveReplantedState(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.getStateForAge(0);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.setValue(NetherWartBlock.AGE, 0);
        }
        return state;
    }

    private static void sendHighlight(Player player, LivingEntity target, int ttlTicks) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(List.of(), List.of(target.getUUID()), ttlTicks));
    }

    private static void sendHighlight(Player player, List<UUID> entityIds, int ttlTicks) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) || entityIds.isEmpty()) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(List.of(), entityIds, ttlTicks));
    }
}
