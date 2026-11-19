package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.*;

/** A snapshot of one upgrade step from the server-synchronized upgrade definitions. */
public record UpgradeJeiRecipe(ResourceLocation item, int level, ItemStack input, CyberwareServicePlan plan) {
    public List<Component> changes() {
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) input.getItem()).getDefinition();
        var lines = new ArrayList<Component>();
        for (var line : CyberwareUpgradeHelper.getUpgradePreviewLines(input, definition)) {
            if (line.getContents() instanceof TranslatableContents translation
                    && translation.getKey().equals("tooltip.cyberneticenhancements.sandevistan_tuning")) {
                var upgrades = CyberwareUpgradeRegistry.get(definition.id());
                var before = upgrades.at(level);
                var after = upgrades.at(level + 1);
                addChange(lines, "duration", before.durationMultiplier(), after.durationMultiplier());
                addChange(lines, "cooldown", before.cooldownMultiplier(), after.cooldownMultiplier());
                addChange(lines, "instability", before.instabilityMultiplier(), after.instabilityMultiplier());
            } else {
                // The regular station preview prefixes stat changes with an arrow; the JEI list needs no bullets.
                if (line.getString().startsWith("-> ")) {
                    var text = Component.empty();
                    line.getSiblings().forEach(text::append);
                    lines.add(text);
                } else lines.add(line);
            }
        }
        return List.copyOf(lines);
    }

    private static void addChange(List<Component> lines, String key, double before, double after) {
        if (Math.abs(after - before) < .0001) return;
        lines.add(Component.translatable("jei.cyberneticenhancements.upgrading." + key,
                percentage(before), percentage(after)));
    }

    private static String percentage(double multiplier) {
        return String.format(Locale.ROOT, "%.1f", multiplier * 100).replaceAll("\\.0$", "");
    }

    public static List<UpgradeJeiRecipe> all() {
        var recipes = new ArrayList<UpgradeJeiRecipe>();
        for (var entry : ModItems.cyberwareItems()) {
            var item = entry.get();
            var id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack input = UpgradeRecipeTransfer.displayInput(id, 0);
            int max = CyberwareUpgradeHelper.getMaxUpgrades(item.getDefinition());
            for (int level = 0; level < max && !input.isEmpty(); level++) {
                var plan = CyberwareServiceHelper.getUpgradePlan(input);
                if (!plan.isAvailable()) break;
                recipes.add(new UpgradeJeiRecipe(id, level, input.copy(), plan));
                input = plan.output().copy();
            }
        }
        return List.copyOf(recipes);
    }
}
