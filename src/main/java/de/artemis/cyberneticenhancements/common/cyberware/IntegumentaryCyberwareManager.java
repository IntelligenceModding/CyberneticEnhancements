package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class IntegumentaryCyberwareManager {
    private static final String CARAPACE_ID = "carapace";
    private static final String CELLULAR_ADAPTER_ID = "cellular_adapter";
    private static final String COGITO_LATTICE_ID = "cogito_lattice";
    private static final String COUNTERSHELL_ID = "countershell";
    private static final String DEFENZIKOV_ID = "defenzikov";
    private static final String NANO_PLATING_ID = "nano_plating";
    private static final String OPTICAL_CAMO_ID = "optical_camo";
    private static final String PAIN_EDITOR_ID = "pain_editor";
    private static final String PAINDUCER_ID = "painducer";
    private static final String PROXISHIELD_ID = "proxishield";
    private static final String PERIPHERAL_INVERSE_ID = "peripheral_inverse";
    private static final String RANGEGUARD_ID = "rangeguard";
    private static final String SHOCK_N_AWE_ID = "shock_n_awe";
    private static final String SUBDERMAL_ARMOR_ID = "subdermal_armor";
    private static final String CHITIN_ID = "chitin";
    private static final double OPTICAL_CAMO_SCRAMBLE_RADIUS = 24.0D;

    private static final Map<UUID, Long> LAST_COMBAT_TICK = new HashMap<>();
    private static final Map<UUID, Long> COUNTERSHELL_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> DEFENZIKOV_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> PAINDUCER_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> SHOCK_N_AWE_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> OPTICAL_CAMO_DISABLED_UNTIL_TICK = new HashMap<>();

    private IntegumentaryCyberwareManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case CARAPACE_ID,
                 CELLULAR_ADAPTER_ID,
                 COGITO_LATTICE_ID,
                 COUNTERSHELL_ID,
                 DEFENZIKOV_ID,
                 NANO_PLATING_ID,
                 OPTICAL_CAMO_ID,
                 PAIN_EDITOR_ID,
                 PAINDUCER_ID,
                 PROXISHIELD_ID,
                 PERIPHERAL_INVERSE_ID,
                 RANGEGUARD_ID,
                 SHOCK_N_AWE_ID,
                 SUBDERMAL_ARMOR_ID,
                 CHITIN_ID -> true;
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

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int opticalCamoCount = inventory.countInstalledCyberware(OPTICAL_CAMO_ID);
        updateOpticalCamo(player, opticalCamoCount);
    }

    public static void mergePassiveEffects(Player player, PlayerCyberwareInventory inventory, EnumMap<CyberwareEffectType, Double> totals) {
        long gameTime = player.level().getGameTime();
        int nearbyThreats = countNearbyThreats(player, 5.0D);
        int extendedThreats = countNearbyThreats(player, 8.0D);
        double nearestThreatDistance = nearestThreatDistance(player, 16.0D);

        int carapaceCount = inventory.countInstalledCyberware(CARAPACE_ID);
        if (carapaceCount > 0 && (player.isCrouching() || nearbyThreats >= 2)) {
            merge(totals, CyberwareEffectType.ARMOR, 2.0D * carapaceCount);
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.06D * carapaceCount);
        }

        int cellularAdapterCount = inventory.countInstalledCyberware(CELLULAR_ADAPTER_ID);
        if (cellularAdapterCount > 0 && gameTime - LAST_COMBAT_TICK.getOrDefault(player.getUUID(), 0L) >= 100L) {
            merge(totals, CyberwareEffectType.HEALTH_REGEN, 0.60D * cellularAdapterCount);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.03D * cellularAdapterCount);
        }

        int cogitoLatticeCount = inventory.countInstalledCyberware(COGITO_LATTICE_ID);
        if (cogitoLatticeCount > 0) {
            int effectiveCyberstrain = CyberstrainManager.getEffectiveCyberstrain(player, inventory);
            if (effectiveCyberstrain >= 20) {
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.05D * cogitoLatticeCount);
                merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.08D * cogitoLatticeCount);
            }
        }

        int nanoPlatingCount = inventory.countInstalledCyberware(NANO_PLATING_ID);
        if (nanoPlatingCount > 0 && player.getHealth() >= player.getMaxHealth() * 0.75F) {
            merge(totals, CyberwareEffectType.ARMOR, 1.5D * nanoPlatingCount);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.03D * nanoPlatingCount);
        }

        int proxiShieldCount = inventory.countInstalledCyberware(PROXISHIELD_ID);
        if (proxiShieldCount > 0 && nearbyThreats > 0) {
            merge(totals, CyberwareEffectType.ARMOR, 2.0D * proxiShieldCount);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.04D * proxiShieldCount);
        }

        int peripheralInverseCount = inventory.countInstalledCyberware(PERIPHERAL_INVERSE_ID);
        if (peripheralInverseCount > 0 && nearbyThreats > 0) {
            merge(totals, CyberwareEffectType.ARMOR, 3.0D * peripheralInverseCount);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.06D * peripheralInverseCount);
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.10D * peripheralInverseCount);
        }

        int rangeguardCount = inventory.countInstalledCyberware(RANGEGUARD_ID);
        if (rangeguardCount > 0 && nearestThreatDistance >= 6.0D && nearestThreatDistance <= 16.0D) {
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.05D * rangeguardCount);
        }

        int chitinCount = inventory.countInstalledCyberware(CHITIN_ID);
        if (chitinCount > 0 && (extendedThreats >= 2 || player.isCrouching())) {
            merge(totals, CyberwareEffectType.ARMOR, 3.0D * chitinCount);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.05D * chitinCount);
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.08D * chitinCount);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            handleIncomingPlayerDamage(event, player);
        }

        if (event.getSource().getEntity() instanceof Player player && event.getEntity() != player) {
            handleOutgoingPlayerDamage(event, player);
        }
    }

    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int carapaceCount = inventory.countInstalledCyberware(CARAPACE_ID);
        int painEditorCount = inventory.countInstalledCyberware(PAIN_EDITOR_ID);
        int chitinCount = inventory.countInstalledCyberware(CHITIN_ID);
        if (carapaceCount <= 0 && painEditorCount <= 0 && chitinCount <= 0) {
            return;
        }

        float strength = event.getStrength();
        if (carapaceCount > 0) {
            strength *= Math.max(0.35F, 1.0F - 0.15F * carapaceCount);
        }
        if (painEditorCount > 0) {
            strength *= Math.max(0.30F, 1.0F - 0.18F * painEditorCount);
        }
        if (chitinCount > 0) {
            strength *= player.isCrouching() ? 0.18F : Math.max(0.40F, 1.0F - 0.15F * chitinCount);
        }
        event.setStrength(strength);
    }

    public static void clearCooldowns(Player player) {
        UUID playerId = player.getUUID();
        LAST_COMBAT_TICK.remove(playerId);
        COUNTERSHELL_READY_UNTIL_TICK.remove(playerId);
        DEFENZIKOV_READY_UNTIL_TICK.remove(playerId);
        PAINDUCER_READY_UNTIL_TICK.remove(playerId);
        SHOCK_N_AWE_READY_UNTIL_TICK.remove(playerId);
        OPTICAL_CAMO_DISABLED_UNTIL_TICK.remove(playerId);
        clearManagedOpticalCamo(player);
    }

    private static void handleIncomingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        float incomingDamage = event.getAmount();
        if (incomingDamage <= 0.0F) {
            return;
        }

        long gameTime = player.level().getGameTime();
        markCombat(player, gameTime);
        breakOpticalCamo(player, gameTime, 100L);

        float healthAfterHit = player.getHealth() - incomingDamage;
        int nearbyThreats = countNearbyThreats(player, 5.0D);
        boolean refreshRequired = false;

        int subdermalArmorCount = inventory.countInstalledCyberware(SUBDERMAL_ARMOR_ID);
        if (subdermalArmorCount > 0 && isPhysicalThreat(event.getSource(), player)) {
            event.setAmount(event.getAmount() * Math.max(0.65F, 1.0F - 0.05F * subdermalArmorCount));
        }

        int carapaceCount = inventory.countInstalledCyberware(CARAPACE_ID);
        if (carapaceCount > 0 && (player.isCrouching() || nearbyThreats >= 2)) {
            event.setAmount(event.getAmount() * Math.max(0.60F, 1.0F - 0.06F * carapaceCount));
        }

        int nanoPlatingCount = inventory.countInstalledCyberware(NANO_PLATING_ID);
        if (nanoPlatingCount > 0 && player.getHealth() >= player.getMaxHealth() * 0.75F) {
            event.setAmount(event.getAmount() * Math.max(0.60F, 1.0F - 0.05F * nanoPlatingCount));
        }

        int proxiShieldCount = inventory.countInstalledCyberware(PROXISHIELD_ID);
        if (proxiShieldCount > 0 && nearbyThreats > 0) {
            event.setAmount(event.getAmount() * Math.max(0.58F, 1.0F - 0.05F * proxiShieldCount));
        }

        int peripheralInverseCount = inventory.countInstalledCyberware(PERIPHERAL_INVERSE_ID);
        if (peripheralInverseCount > 0 && nearbyThreats > 0) {
            event.setAmount(event.getAmount() * Math.max(0.50F, 1.0F - 0.07F * peripheralInverseCount));
        }

        int rangeguardCount = inventory.countInstalledCyberware(RANGEGUARD_ID);
        if (rangeguardCount > 0 && isRangedThreat(event.getSource().getDirectEntity(), player)) {
            event.setAmount(event.getAmount() * Math.max(0.55F, 1.0F - 0.08F * rangeguardCount));
        }

        int chitinCount = inventory.countInstalledCyberware(CHITIN_ID);
        if (chitinCount > 0 && nearbyThreats >= 2) {
            event.setAmount(event.getAmount() * Math.max(0.48F, 1.0F - 0.07F * chitinCount));
        }

        int painEditorCount = inventory.countInstalledCyberware(PAIN_EDITOR_ID);
        if (painEditorCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.45F, 1.0F - 0.10F * painEditorCount));
            if (incomingDamage >= 4.0F || healthAfterHit <= player.getMaxHealth() * 0.50F) {
                applyTimedEffects(player, 80L,
                        CyberwareEffect.bonusAbsorption(1.5D * painEditorCount),
                        CyberwareEffect.damageReduction(0.05D * painEditorCount));
                refreshRequired = true;
            }
        }

        int countershellCount = inventory.countInstalledCyberware(COUNTERSHELL_ID);
        if (countershellCount > 0
                && isReady(COUNTERSHELL_READY_UNTIL_TICK, player, gameTime)
                && (incomingDamage >= 2.0F || healthAfterHit <= player.getMaxHealth() * 0.80F)) {
            applyTimedEffects(player, 80L,
                    CyberwareEffect.armor(2.0D * countershellCount),
                    CyberwareEffect.damageReduction(0.08D * countershellCount));
            COUNTERSHELL_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 12L * 20L);
            refreshRequired = true;
        }

        int defenzikovCount = inventory.countInstalledCyberware(DEFENZIKOV_ID);
        if (defenzikovCount > 0
                && isReady(DEFENZIKOV_READY_UNTIL_TICK, player, gameTime)
                && (isRangedThreat(event.getSource().getDirectEntity(), player)
                || incomingDamage >= 3.0F
                || healthAfterHit <= player.getMaxHealth() * 0.60F)) {
            applyTimedEffects(player, 80L,
                    CyberwareEffect.moveSpeed(0.12D * defenzikovCount),
                    CyberwareEffect.attackSpeed(0.04D * defenzikovCount),
                    CyberwareEffect.damageReduction(0.06D * defenzikovCount));
            DEFENZIKOV_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 16L * 20L);
            refreshRequired = true;
        }

        int painducerCount = inventory.countInstalledCyberware(PAINDUCER_ID);
        if (painducerCount > 0
                && isReady(PAINDUCER_READY_UNTIL_TICK, player, gameTime)
                && (incomingDamage >= 3.5F || healthAfterHit <= player.getMaxHealth() * 0.70F)) {
            event.setAmount(event.getAmount() * Math.max(0.50F, 1.0F - 0.08F * painducerCount));
            applyTimedEffects(player, 100L,
                    CyberwareEffect.healthRegen(0.45D * painducerCount),
                    CyberwareEffect.bonusAbsorption(2.0D * painducerCount));
            PAINDUCER_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 14L * 20L);
            refreshRequired = true;
        }

        int shockNAweCount = inventory.countInstalledCyberware(SHOCK_N_AWE_ID);
        if (shockNAweCount > 0
                && isReady(SHOCK_N_AWE_READY_UNTIL_TICK, player, gameTime)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && player.distanceToSqr(attacker) <= 25.0D) {
            retaliateShock(player, attacker, shockNAweCount);
            SHOCK_N_AWE_READY_UNTIL_TICK.put(player.getUUID(), gameTime + Math.round(8L * 20L / CyberwareUpgradeHelper.functionMultiplier(inventory, SHOCK_N_AWE_ID)));
        }

        if (refreshRequired) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void handleOutgoingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        long gameTime = player.level().getGameTime();
        markCombat(player, gameTime);
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        if (inventory.hasInstalledCyberware(OPTICAL_CAMO_ID)) {
            breakOpticalCamo(player, gameTime, 120L);
        }
    }

    private static void updateOpticalCamo(Player player, int installedCount) {
        if (installedCount <= 0) {
            clearManagedOpticalCamo(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        boolean canCloak = player.isCrouching()
                && !player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() <= 0.12D
                && OPTICAL_CAMO_DISABLED_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) <= gameTime
                && gameTime - LAST_COMBAT_TICK.getOrDefault(player.getUUID(), 0L) >= 80L;

        if (canCloak) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false, false));
            suppressOpticalCamoAggro(player, (OPTICAL_CAMO_SCRAMBLE_RADIUS + installedCount * 2.0D)
                    * CyberwareUpgradeHelper.functionMultiplier(new PlayerCyberwareInventory(player), OPTICAL_CAMO_ID));
        } else {
            clearManagedOpticalCamo(player);
        }
    }

    private static void breakOpticalCamo(Player player, long gameTime, long disableTicks) {
        OPTICAL_CAMO_DISABLED_UNTIL_TICK.put(player.getUUID(), gameTime + disableTicks);
        clearManagedOpticalCamo(player);
    }

    private static void clearManagedOpticalCamo(Player player) {
        MobEffectInstance effect = player.getEffect(MobEffects.INVISIBILITY);
        if (effect != null && isManagedOpticalCamoEffect(effect)) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    private static boolean isManagedOpticalCamoEffect(MobEffectInstance effect) {
        return effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()
                && effect.getDuration() <= 40;
    }

    private static void suppressOpticalCamoAggro(Player player, double radius) {
        AggroControlHelper.clearNearbyAggro(player, radius, candidate -> shouldForgetCloakedPlayer(candidate, player));
    }

    private static boolean shouldForgetCloakedPlayer(Mob mob, Player player) {
        return mob instanceof Enemy
                || AggroControlHelper.hasDirectAggro(mob, player)
                || mob.hasLineOfSight(player);
    }

    private static void retaliateShock(Player player, LivingEntity attacker, int installedCount) {
        CombatStatusManager.applyStatus(attacker, CombatStatusType.SHOCK, 60L + installedCount * 20L, 1 + Math.max(0, installedCount - 1), true);

        List<UUID> highlighted = new ArrayList<>();
        highlighted.add(attacker.getUUID());
        Vec3 attackerOffset = attacker.position().subtract(player.position());
        attacker.knockback(0.25D + installedCount * 0.10D, attackerOffset.x, attackerOffset.z);

        for (LivingEntity nearby : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(3.5D + installedCount),
                entity -> entity != player && entity != attacker && isThreat(player, entity))) {
            CombatStatusManager.applyStatus(nearby, CombatStatusType.SHOCK, 40L + installedCount * 10L, 1, true);
            highlighted.add(nearby.getUUID());
        }

        if (!highlighted.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(List.of(), highlighted, 24));
        }
    }

    private static void applyTimedEffects(Player player, long durationTicks, CyberwareEffect... effects) {
        TemporaryCyberwareEffectManager.addEffects(player, durationTicks, effects);
    }

    private static void markCombat(Player player, long gameTime) {
        LAST_COMBAT_TICK.put(player.getUUID(), gameTime);
    }

    private static int countNearbyThreats(Player player, double radius) {
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && isThreat(player, entity)
        ).size();
    }

    private static double nearestThreatDistance(Player player, double radius) {
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                target -> target != player && isThreat(player, target))) {
            best = Math.min(best, Math.sqrt(player.distanceToSqr(entity)));
        }
        return best == Double.MAX_VALUE ? Double.MAX_VALUE : best;
    }

    private static boolean isThreat(Player player, LivingEntity target) {
        return target instanceof Enemy
                || target instanceof Mob mob && mob.getTarget() == player
                || target.getLastHurtByMob() == player;
    }

    private static boolean isPhysicalThreat(DamageSource source, Player player) {
        return source.getDirectEntity() == player
                || source.getDirectEntity() instanceof LivingEntity
                || source.isDirect();
    }

    private static boolean isRangedThreat(Entity directEntity, Player player) {
        return directEntity != null && directEntity != player;
    }

    private static boolean isReady(Map<UUID, Long> cooldowns, Player player, long gameTime) {
        return cooldowns.getOrDefault(player.getUUID(), 0L) <= gameTime;
    }

    private static void merge(EnumMap<CyberwareEffectType, Double> totals, CyberwareEffectType type, double amount) {
        totals.merge(type, amount, type.isMobEffect() ? Math::max : Double::sum);
    }
}
