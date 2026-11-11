package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.world.item.ItemStack;
import java.util.List;

 public record CyberwareServicePlan(ItemStack output, List<CyberwareServiceHelper.MaterialRequirement> materials) {
    public CyberwareServicePlan {
        output = output.copy();
        materials = List.copyOf(materials);
        if (materials.size() > 2) throw new IllegalArgumentException("Servicing accepts at most two materials");
    }

    public static CyberwareServicePlan empty() { return new CyberwareServicePlan(ItemStack.EMPTY, List.of()); }
    public boolean isAvailable() { return !output.isEmpty() && !materials.isEmpty(); }
}