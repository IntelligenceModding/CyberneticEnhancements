package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.Target;
import java.util.List;

public final class QuestItemLink {
    private QuestItemLink() {}

    public static boolean matches(ItemStack stack, String questId, String requiredItemId) {
        if (stack.isEmpty() || questId.isEmpty()) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String boundQuest = tag.getString("QuestContractId");
        if (!boundQuest.isEmpty()) return questId.equals(boundQuest);
        return !requiredItemId.isEmpty() && requiredItemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static Target uniqueTarget(ItemStack stack, List<Target> targets) {
        Target match = null;
        for (var target : targets) {
            if (!matches(stack, target.questId(), target.itemId())) continue;
            if (match != null) return null;
            match = target;
        }
        return match;
    }
}
