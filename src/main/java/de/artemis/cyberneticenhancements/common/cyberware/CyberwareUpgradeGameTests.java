package de.artemis.cyberneticenhancements.common.cyberware;

import com.google.gson.JsonParser;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.Map;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyberwareUpgradeGameTests {
    private static final String ID = "militech_experimental_sandevistan";
    private static final String JSON = "{\"levels\":[{\"duration_multiplier\":10,\"materials\":[{\"item\":\"minecraft:iron_ingot\",\"count\":1}]}]}";

    @GameTest(template = "sandevistan_test", batch = "upgrade_data", timeoutTicks = 20)
    public static void serverDatapacksOwnBonusesAndCosts(GameTestHelper helper) {
        var original = CyberwareUpgradeRegistry.serverDefinition(ID);
        helper.assertTrue(original.levels().size() == 5, "Bundled server upgrade definitions did not load");
        helper.assertTrue(original.at(5).duration(160) == 200, "Bundled experimental tuning changed");
        CyberwareUpgradeRegistry.receive("{\"" + ID + "\":" + JSON + "}");
        helper.assertTrue(CyberwareUpgradeRegistry.get(ID).equals(original), "Client display sync changed server rules");
        helper.assertTrue(CyberwareUpgradeRegistry.get(ID).at(1).materials().getFirst().count() == 1, "Client changed server costs");
        CyberwareUpgradeRegistry.clearClient();
        var loader = new CyberwareUpgradeRegistry();
        var id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, ID);
        for (String invalid : new String[]{JSON.replace("minecraft:iron_ingot", "minecraft:not_an_item"),
                JSON.replace("minecraft:iron_ingot", "minecraft:air"), JSON.replace("\"duration_multiplier\":10", "\"effects\":{\"typo\":1}"),
                JSON.replace("\"duration_multiplier\":10", "\"duration_multiplier\":-1")}) {
            boolean rejected = false;
            try { loader.apply(Map.of(id, JsonParser.parseString(invalid)), null, null); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Invalid upgrade definition was accepted: " + invalid);
            helper.assertTrue(CyberwareUpgradeRegistry.get(ID).equals(original), "Failed reload changed live server definitions");
        }
        helper.succeed();
    }
}
