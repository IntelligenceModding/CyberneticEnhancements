package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class FabricationDiscoveries {

    public static ItemStack rollReward(ServerPlayer player, int tier, de.artemis.cyberneticenhancements.common.quest.QuestType type) {
        boolean restricted = type == de.artemis.cyberneticenhancements.common.quest.QuestType.CYBERPSYCHO && tier >= 3;
        if (restricted && player.getRandom().nextInt(100) < de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("discovery.psycho_core"))
            return new ItemStack(FabricationContent.part(List.of("proprietary_neural_core", "military_actuator_core", "experimental_control_core").get(player.getRandom().nextInt(3))));
        if (player.getRandom().nextInt(100) >= de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("discovery." + type.id())) return ItemStack.EMPTY;
        int designTier = restricted ? 5 : Math.min(4, tier + 1);
        var candidates = FabricationContent.SCHEMATICS.keySet().stream().filter(id -> CyberwareCatalog.get(id).tier().ordinal() + 1 <= designTier && !SchematicKnowledge.knows(player, id)).toList();
        ItemStack reward;
        if (!candidates.isEmpty())
            reward = new ItemStack(FabricationContent.SCHEMATICS.get(candidates.get(player.getRandom().nextInt(candidates.size()))).get());
        else
            reward = new ItemStack(FabricationContent.part(tier >= 3 ? "reinforced_alloy" : "neural_processor"));
        return reward;
    }

}
