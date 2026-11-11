package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class FabricationDiscoveries {

    public static void reward(ServerPlayer player, int tier) {
        if (player.getRandom().nextFloat() > 0.4F) return;

        int designTier = tier >= 3 ? 5 : tier + 1;
        var candidates = FabricationContent.SCHEMATICS.keySet().stream().filter(id -> CyberwareCatalog.get(id).tier().ordinal() + 1 <= designTier && !SchematicKnowledge.knows(player, id)).toList();
        ItemStack reward;
        if (!candidates.isEmpty())
            reward = new ItemStack(FabricationContent.SCHEMATICS.get(candidates.get(player.getRandom().nextInt(candidates.size()))).get());
        else
            reward = new ItemStack(FabricationContent.part(tier >= 3
                    ? List.of("proprietary_neural_core", "military_actuator_core", "experimental_control_core").get(player.getRandom().nextInt(3))
                    : "neural_processor"));
        if (!player.addItem(reward)) player.drop(reward, false);
    }

}
