package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager;
import de.artemis.cyberneticenhancements.common.item.ChipwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import de.artemis.cyberneticenhancements.common.network.CyberwareHudPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Set;

public final class CyberwareEffects {
    private static final int EFFECT_DURATION = 240;
    private static final String LAST_ABSORPTION_BUFFER_KEY = "cyberneticenhancements.last_absorption_buffer";
    private static final String LAST_MAX_HEALTH_BONUS_KEY = "cyberneticenhancements.last_max_health_bonus";
    private static final String LAST_ON_GROUND_KEY = "cyberneticenhancements.last_on_ground";
    private static final String MANAGED_MOB_EFFECTS_KEY = "cyberneticenhancements.managed_mob_effects";
    private static final double MAX_DAMAGE_REDUCTION = 0.80D;

    private CyberwareEffects() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }

        CyberwareConditionManager.onPlayerTick(player);
        CyberstrainManager.onPlayerTick(player);
        CyberwareAbilities.onPlayerTick(player);
        FaceCyberwareManager.onPlayerTick(player);
        ArmCyberwareManager.onPlayerTick(player);
        SkeletonCyberwareManager.onPlayerTick(player);
        NervousSystemCyberwareManager.onPlayerTick(player);
        IntegumentaryCyberwareManager.onPlayerTick(player);
        LegCyberwareManager.onPlayerTick(player);
        handleJumpAugmentation(player);
        boolean shouldRunSecondTasks = player.tickCount % 20 == 0;
        if (shouldRunSecondTasks) {
            CyberConsumableManager.onPlayerTick(player);
        }

        boolean shouldRefreshNow = shouldRunSecondTasks
                || TemporaryCyberwareEffectManager.hasStoredEffects(player)
                || hasManagedMobEffects(player)
                || LegCyberwareManager.requiresRealtimeRefresh(player)
                || HandsCyberwareManager.requiresRealtimeRefresh(player)
                || CirculatoryCyberwareManager.requiresRealtimeRefresh(player);
        if (shouldRefreshNow) {
            EnumMap<CyberwareEffectType, Double> totals = refreshPlayerCyberware(player);
            if (shouldRunSecondTasks) {
                applyPassiveHealing(player, totals);
            }
        }

        if (player instanceof ServerPlayer serverPlayer && player.tickCount % 4 == 0) {
            PacketDistributor.sendToPlayer(serverPlayer, CyberwareHudPayload.capture(serverPlayer));
        }
    }

    public static EnumMap<CyberwareEffectType, Double> refreshPlayerCyberware(Player player) {
        if (player.level().isClientSide()) {
            return new EnumMap<>(CyberwareEffectType.class);
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        EnumMap<CyberwareEffectType, Double> totals = collectEffectTotals(player, inventory, true);

        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            if (type.isAttributeEffect()) {
                applyAttributeModifier(player, type, totals.getOrDefault(type, 0.0D));
            }
        }
        syncMaxHealthBuffer(player, totals.getOrDefault(CyberwareEffectType.MAX_HEALTH, 0.0D));
        syncManagedMobEffects(player, totals);
        applyAbsorptionBuffer(player, totals.getOrDefault(CyberwareEffectType.BONUS_ABSORPTION, 0.0D));

        double maxHealth = player.getMaxHealth();
        if (player.getHealth() > maxHealth) {
            player.setHealth((float) maxHealth);
        }
        return totals;
    }

    private static void syncMaxHealthBuffer(Player player, double maxHealthBonus) {
        CompoundTag persistentData = player.getPersistentData();
        double targetBonus = Math.max(0.0D, maxHealthBonus);
        double previousBonus = persistentData.getDouble(LAST_MAX_HEALTH_BONUS_KEY);
        double gainedBonus = targetBonus - previousBonus;
        if (gainedBonus > 0.0001D && player.isAlive()) {
            player.heal((float) gainedBonus);
        }

        if (targetBonus > 0.0001D) {
            persistentData.putDouble(LAST_MAX_HEALTH_BONUS_KEY, targetBonus);
        } else {
            persistentData.remove(LAST_MAX_HEALTH_BONUS_KEY);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) {
            return;
        }

        if (living instanceof Player player) {
            EnumMap<CyberwareEffectType, Double> totals = collectEffectTotals(player);
            double reduction = Mth.clamp(totals.getOrDefault(CyberwareEffectType.DAMAGE_REDUCTION, 0.0D), 0.0D, MAX_DAMAGE_REDUCTION);
            if (reduction > 0.0001D) {
                event.setAmount((float) (event.getAmount() * (1.0D - reduction)));
            }
            CyberstrainManager.onIncomingDamage(player, event.getAmount(), event.getSource().getEntity() instanceof LivingEntity attacker ? attacker : null);
            SkeletonCyberwareManager.onIncomingDamage(event);
            LegCyberwareManager.onIncomingDamage(event);
        }
        CirculatoryCyberwareManager.onIncomingDamage(event);
        IntegumentaryCyberwareManager.onIncomingDamage(event);
        NervousSystemCyberwareManager.onIncomingDamage(event);
        ArmCyberwareManager.onIncomingDamage(event);
        HandsCyberwareManager.onIncomingDamage(event);
        CombatStatusManager.onIncomingDamage(event);
    }

    public static EnumMap<CyberwareEffectType, Double> collectEffectTotals(Player player) {
        return collectEffectTotals(player, new PlayerCyberwareInventory(player), false);
    }

    public static EnumMap<CyberwareEffectType, Double> collectEffectTotals(Player player, PlayerCyberwareInventory inventory, boolean applyTriggers) {
        EnumMap<CyberwareEffectType, Double> totals = new EnumMap<>(CyberwareEffectType.class);

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
                continue;
            }

            double integrityScale = CyberwareConditionHelper.getPerformanceRatio(stack, cyberwareItem.getDefinition());
            mergeEffects(totals, cyberwareItem.getEffects(stack), integrityScale);
        }
        for (CyberwareModuleItem moduleItem : inventory.getOperationalModuleItems()) {
            mergeEffects(totals, moduleItem.getDefinition().effects());
        }
        for (ChipwareItem chipwareItem : inventory.getOperationalChipwareItems()) {
            mergeEffects(totals, chipwareItem.getDefinition().effects());
        }

        if (applyTriggers) {
            CyberwareAbilities.applyTriggeredAbilities(player, inventory);
        }
        TemporaryCyberwareEffectManager.mergeActiveEffects(player, totals, player.level().getGameTime());
        FrontalCortexManager.mergePassiveEffects(player, inventory, totals);
        HandsCyberwareManager.mergePassiveEffects(player, inventory, totals);
        CirculatoryCyberwareManager.mergePassiveEffects(player, inventory, totals);
        SkeletonCyberwareManager.mergePassiveEffects(player, inventory, totals);
        IntegumentaryCyberwareManager.mergePassiveEffects(player, inventory, totals);
        LegCyberwareManager.mergePassiveEffects(player, inventory, totals);
        CyberwareAbilities.mergeActiveEffects(player, inventory, totals);
        CyberstrainManager.applyPenalties(player, inventory, totals);
        CombatStatusManager.mergePlayerEffects(player, totals);
        return totals;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player player) || event.getEntity() == player) {
            return;
        }

        CyberstrainManager.onKill(player, event.getEntity());
        CyberwareAbilities.onKill(player, event.getEntity());
        FrontalCortexManager.onKill(player, event.getEntity());
        CirculatoryCyberwareManager.onKill(player, event.getEntity());
        NervousSystemCyberwareManager.onKill(player, event.getEntity());
    }

    private static void applyAttributeModifier(Player player, CyberwareEffectType type, double amount) {
        AttributeInstance attributeInstance = player.getAttribute(type.getAttribute());
        if (attributeInstance == null) {
            return;
        }
        attributeInstance.removeModifier(type.modifierId());
        if (Math.abs(amount) > 0.0001D) {
            attributeInstance.addOrUpdateTransientModifier(new AttributeModifier(type.modifierId(), amount, type.getOperation()));
        }
    }

    private static void applyMobEffect(Player player, CyberwareEffectType type, int amplifier) {
        if (amplifier < 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(type.getMobEffect(), EFFECT_DURATION, amplifier, true, false, false));
    }

    private static void syncManagedMobEffects(Player player, EnumMap<CyberwareEffectType, Double> totals) {
        CompoundTag persistentData = player.getPersistentData();
        Set<CyberwareEffectType> previouslyManaged = getManagedMobEffects(persistentData);
        Set<CyberwareEffectType> currentlyManaged = EnumSet.noneOf(CyberwareEffectType.class);

        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            if (!type.isMobEffect()) {
                continue;
            }

            if (totals.containsKey(type)) {
                applyMobEffect(player, type, (int) Math.round(totals.get(type)));
                currentlyManaged.add(type);
            } else if (previouslyManaged.contains(type)) {
                clearManagedMobEffect(player, type);
            }
        }

        storeManagedMobEffects(persistentData, currentlyManaged);
    }

    private static void clearManagedMobEffect(Player player, CyberwareEffectType type) {
        MobEffectInstance activeEffect = player.getEffect(type.getMobEffect());
        if (activeEffect == null || !isManagedMobEffectInstance(activeEffect)) {
            return;
        }
        player.removeEffect(type.getMobEffect());
    }

    private static boolean isManagedMobEffectInstance(MobEffectInstance effectInstance) {
        return effectInstance.isAmbient()
                && !effectInstance.isVisible()
                && !effectInstance.showIcon()
                && effectInstance.getDuration() <= EFFECT_DURATION + 20;
    }

    private static boolean hasManagedMobEffects(Player player) {
        return !player.getPersistentData().getList(MANAGED_MOB_EFFECTS_KEY, Tag.TAG_STRING).isEmpty();
    }

    private static Set<CyberwareEffectType> getManagedMobEffects(CompoundTag persistentData) {
        Set<CyberwareEffectType> managedEffects = EnumSet.noneOf(CyberwareEffectType.class);
        for (Tag tag : persistentData.getList(MANAGED_MOB_EFFECTS_KEY, Tag.TAG_STRING)) {
            if (!(tag instanceof StringTag stringTag)) {
                continue;
            }
            try {
                managedEffects.add(CyberwareEffectType.valueOf(stringTag.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return managedEffects;
    }

    private static void storeManagedMobEffects(CompoundTag persistentData, Set<CyberwareEffectType> managedEffects) {
        if (managedEffects.isEmpty()) {
            persistentData.remove(MANAGED_MOB_EFFECTS_KEY);
            return;
        }

        ListTag storedTypes = new ListTag();
        for (CyberwareEffectType type : managedEffects) {
            storedTypes.add(StringTag.valueOf(type.name()));
        }
        persistentData.put(MANAGED_MOB_EFFECTS_KEY, storedTypes);
    }

    private static void mergeEffects(EnumMap<CyberwareEffectType, Double> totals, Iterable<CyberwareEffect> effects) {
        mergeEffects(totals, effects, 1.0D);
    }

    private static void mergeEffects(EnumMap<CyberwareEffectType, Double> totals, Iterable<CyberwareEffect> effects, double scale) {
        if (scale <= 0.0001D) {
            return;
        }

        for (CyberwareEffect effect : effects) {
            if (effect.type().isMobEffect()) {
                if (scale >= 0.35D) {
                    totals.merge(effect.type(), effect.amount(), Math::max);
                }
                continue;
            }

            totals.merge(effect.type(), effect.amount() * scale, Double::sum);
        }
    }

    private static void applyPassiveHealing(Player player, EnumMap<CyberwareEffectType, Double> totals) {
        double regenerationPerSecond = totals.getOrDefault(CyberwareEffectType.HEALTH_REGEN, 0.0D);
        if (regenerationPerSecond > 0.0001D && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) regenerationPerSecond);
        }
    }

    private static void applyAbsorptionBuffer(Player player, double desiredAbsorption) {
        double target = Math.max(0.0D, desiredAbsorption);
        double previousTarget = player.getPersistentData().getDouble(LAST_ABSORPTION_BUFFER_KEY);
        double delta = target - previousTarget;
        if (Math.abs(delta) > 0.0001D) {
            player.setAbsorptionAmount((float) Math.max(0.0D, player.getAbsorptionAmount() + delta));
        }

        if (target > 0.0001D) {
            player.getPersistentData().putDouble(LAST_ABSORPTION_BUFFER_KEY, target);
        } else {
            player.getPersistentData().remove(LAST_ABSORPTION_BUFFER_KEY);
        }
    }

    private static void handleJumpAugmentation(Player player) {
        boolean wasOnGround = player.getPersistentData().getBoolean(LAST_ON_GROUND_KEY);
        boolean onGround = player.onGround();
        if (!onGround && wasOnGround && player.getDeltaMovement().y > 0.0D) {
            double jumpPower = collectEffectTotals(player).getOrDefault(CyberwareEffectType.JUMP_POWER, 0.0D);
            if (jumpPower > 0.0001D) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, motion.y + 0.42D * jumpPower, motion.z);
                player.hasImpulse = true;
                PlayerMotionSyncHelper.sync(player);
            }
        }
        player.getPersistentData().putBoolean(LAST_ON_GROUND_KEY, onGround);
    }
}
