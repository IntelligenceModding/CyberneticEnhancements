package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.world.item.ItemStack;

 public final class SandevistanUpgrades {
    private SandevistanUpgrades() {}

    public static long duration(long ticks, ItemStack stack) {
        if (!(stack.getItem() instanceof CyberwareItem item)) return ticks;
        return CyberwareUpgradeRegistry.get(item.getDefinition().id()).at(CyberwareUpgradeHelper.getUpgradeLevel(stack)).duration(ticks);
    }

    public static long cooldown(long ticks, ItemStack stack) {
        if (!(stack.getItem() instanceof CyberwareItem item)) return ticks;
        return CyberwareUpgradeRegistry.get(item.getDefinition().id()).at(CyberwareUpgradeHelper.getUpgradeLevel(stack)).cooldown(ticks);
    }

    public static SandevistanProfile profile(SandevistanProfile base, int level) {
        double load = CyberwareUpgradeRegistry.get(base.id()).at(level).instabilityMultiplier();
        return new SandevistanProfile(base.id(), base.timeScale(), base.movement(), base.attackSpeed(),
                base.activationStrain() * load, base.runningStrain() * load, base.wear(), base.radius());
    }
}
