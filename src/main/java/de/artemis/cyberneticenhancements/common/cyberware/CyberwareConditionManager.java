package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class CyberwareConditionManager {
    private static final String LAST_WEAR_TICK_KEY = "cyberneticenhancements.last_cyberware_wear_tick";
    private static final long WEAR_INTERVAL_TICKS = 1200L;
    private static final String WET_EXPOSURE_KEY = "cyberneticenhancements.cyberware_wet_exposure";
    private static final String LAST_WET_CHECK_KEY = "cyberneticenhancements.last_cyberware_wet_check";
    private static final String NEXT_SHORT_CIRCUIT_KEY = "cyberneticenhancements.next_cyberware_short_circuit";

    private CyberwareConditionManager() {
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        long gameTime = player.level().getGameTime();
        tickWaterExposure(player, gameTime);
        long lastWearTick = player.getPersistentData().getLong(LAST_WEAR_TICK_KEY);
        if (lastWearTick == 0L) {
            player.getPersistentData().putLong(LAST_WEAR_TICK_KEY, gameTime);
            return;
        }
        if (gameTime - lastWearTick < WEAR_INTERVAL_TICKS) {
            return;
        }

        player.getPersistentData().putLong(LAST_WEAR_TICK_KEY, gameTime);
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int wearAmount = 1 + Math.max(0, inventory.getCyberstrain() / 20);
        if (CyberstrainManager.isPsychosisActive(player, gameTime)) {
            wearAmount += 1;
        }

        if (de.artemis.cyberneticenhancements.common.skills.SkillEffects.skipsWear(player)) {
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
                continue;
            }
            changed |= CyberwareConditionHelper.wear(stack, cyberwareItem.getDefinition(), wearAmount);
        }

        if (changed) {
            inventory.save();
        }
    }

    public static java.util.List<ItemStack> inducePsychosisFaults(Player player, net.minecraft.util.RandomSource random) {
        var inventory = new PlayerCyberwareInventory(player);
        var candidates = new java.util.ArrayList<ItemStack>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof CyberwareItem && !CyberwareConditionHelper.hasNeuralFault(stack)) candidates.add(stack);
        }
        if (candidates.isEmpty() || random.nextFloat() >= .35F) return java.util.List.of();
        int count = candidates.size() >= 3 && random.nextFloat() < .2F ? 2 : 1;
        var affected = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < count; i++) {
            var stack = candidates.remove(random.nextInt(candidates.size()));
            var definition = ((CyberwareItem) stack.getItem()).getDefinition();
            int damage = Math.round(CyberwareConditionHelper.getMaxIntegrity(definition) * (.35F + random.nextFloat() * .25F));
            CyberwareConditionHelper.applyNeuralFault(stack, definition, damage);
            affected.add(stack);
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, net.minecraft.network.chat.Component.translatable("message.cyberneticenhancements.neural_fault", stack.getHoverName()));
        }
        inventory.save();
        player.playNotifySound(ModSounds.CYBERWARE_MALFUNCTION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return java.util.List.copyOf(affected);
    }

    private static void tickWaterExposure(Player player, long gameTime) {
        if (gameTime % 20L != 0L) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        long lastCheck = data.getLong(LAST_WET_CHECK_KEY);
        data.putLong(LAST_WET_CHECK_KEY, gameTime);
        if (!player.isAlive() || player.isCreative() || player.isSpectator() || !player.isInWaterOrRain()
                || !hasExposedCyberware(player)) {
            data.remove(WET_EXPOSURE_KEY);
            return;
        }

        int exposure = lastCheck == gameTime - 20L ? data.getInt(WET_EXPOSURE_KEY) : 0;
        int delay = Math.max(20, CyberwareBalance.intValue(player.isInWater()
                ? "condition.short_circuit.water_exposure_ticks" : "condition.short_circuit.rain_exposure_ticks"));
        exposure = (int) Math.min(delay, (long) exposure + 20L);
        data.putInt(WET_EXPOSURE_KEY, exposure);
        if (exposure < delay || gameTime < data.getLong(NEXT_SHORT_CIRCUIT_KEY)) {
            return;
        }

        int cooldown = Math.max(20, CyberwareBalance.intValue("condition.short_circuit.cooldown_ticks"));
        int variation = Math.max(0, CyberwareBalance.intValue("condition.short_circuit.cooldown_variation_ticks"));
        data.putLong(NEXT_SHORT_CIRCUIT_KEY, gameTime + cooldown + (long) (player.getRandom().nextDouble() * variation));
        if (CombatStatusManager.collectActiveStatuses(player, gameTime).stream()
                .noneMatch(status -> status.type() == CombatStatusType.SHOCK)) {
            CombatStatusManager.applyStatus(player, CombatStatusType.SHOCK,
                    Math.max(1, CyberwareBalance.intValue("condition.short_circuit.shock_ticks")), 1);
        }
        float damage = (float) Math.max(0, CyberwareBalance.doubleValue("condition.short_circuit.damage"));
        if (damage > 0) {
            player.hurt(player.damageSources().generic(), damage);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_BURN,
                SoundSource.PLAYERS, 0.35F, 1.6F);
    }

    private static boolean hasExposedCyberware(Player player) {
        double threshold = Math.clamp(CyberwareBalance.doubleValue("condition.short_circuit.integrity_threshold"), 0, 1);
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof CyberwareItem implant
                    && CyberwareConditionHelper.getIntegrityRatio(stack, implant.getDefinition()) <= threshold) {
                return true;
            }
        }
        return false;
    }
}
