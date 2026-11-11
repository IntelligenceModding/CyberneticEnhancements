package de.artemis.cyberneticenhancements.common.cyberware;

import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CyberwareUpgradeDefinitionTest {
    private static final String COST = ",\"materials\":[{\"item\":\"minecraft:iron_ingot\",\"count\":4}]";
    private static CyberwareUpgradeDefinition parse(String level) {
        return CyberwareUpgradeDefinition.parse(JsonParser.parseString("{\"levels\":[{" + level + COST + "}]}"));
    }

@Test
    public void CustomLevelsUseTheirOwnBonusesAndPricesWithoutLinearScaling() {
        var definition = parse("\"effects\":{\"armor\":3.75},\"duration_multiplier\":1.8,\"cooldown_multiplier\":0.4,\"instability_multiplier\":0.3");
        var level = definition.at(1);
        assertEquals(3.75, level.effects().get("armor"), 0);
        assertEquals(288, level.duration(160));
        assertEquals(240, level.cooldown(600));
        assertEquals(0.3, level.instabilityMultiplier(), 0);
        assertEquals(4, level.materials().getFirst().count());
        assertEquals(CyberwareUpgradeDefinition.BASE, definition.at(0));
        assertEquals(level, definition.at(200));
    }

    @Test
    public void EmptyTrackDisablesUpgradingAndMalformedDataIsRejected() {
        var disabled = CyberwareUpgradeDefinition.parse(JsonParser.parseString("{\"levels\":[]}"));
        assertTrue(disabled.levels().isEmpty());
        assertEquals(CyberwareUpgradeDefinition.BASE, disabled.at(12));
        for (String value : new String[]{"\"duration_multiplier\":0", "\"cooldown_multiplier\":-1",
                "\"instability_multiplier\":1e999", "\"chip_slot_bonus\":1.5", "\"typo\":2"}) {
            assertThrows(value, RuntimeException.class, () -> parse(value));
        }
        for (String count : new String[]{"0", "65", "1.5"}) {
            String json = "{\"levels\":[{\"materials\":[{\"item\":\"minecraft:stone\",\"count\":" + count + "}]}]}";
            assertThrows(RuntimeException.class, () -> CyberwareUpgradeDefinition.parse(JsonParser.parseString(json)));
        }
    }

    @Test
    public void EveryBundledTrackIsValidAndSyncFitsOnePacket() throws Exception {
        var directory = Path.of("src/generated/resources/data/cyberneticenhancements/cyberware_upgrades");
        var root = new com.google.gson.JsonObject();
        try (var paths = Files.list(directory)) {
            var files = paths.filter(path -> path.toString().endsWith(".json")).toList();
            assertTrue(files.size() > 100);
            for (var file : files) {
                var json = JsonParser.parseString(Files.readString(file));
                var definition = CyberwareUpgradeDefinition.parse(json);
                assertFalse(file.toString(), definition.levels().isEmpty());
                assertEquals(file.toString(), 2, definition.repairMaterials().size());
                int tier = java.util.List.of("common", "uncommon", "rare", "epic", "legendary").indexOf(
                        definition.at(1).materials().getFirst().item().getPath().replace("_item_components", "")) + 1;
                assertTrue(file.toString(), tier > 0);
                assertEquals(file.toString(), Math.min(5, tier + 1), definition.levels().size());
                for (var level : definition.levels()) {
                    assertEquals(2, level.materials().size());
                    assertTrue(file.toString(), level.materials().getFirst().count() <= (tier == 1 ? 4 : tier <= 3 ? 3 : 2));
                    assertTrue(file.toString(), level.materials().get(1).count() <= (tier == 1 ? 1 : tier == 5 ? 3 : 2));
                    if (level.functionMultiplier() != 1)
                        assertTrue(file.toString(), CyberwareUpgradeHelper.supportsFunction(file.getFileName().toString().replace(".json", "")));
                }
                root.add(file.getFileName().toString(), json);
            }
        }
        assertTrue(root.toString().length() < de.artemis.cyberneticenhancements.common.network.CyberwareUpgradesPayload.MAX_LENGTH);
    }

    @Test
    public void RepairMaterialsAreExplicitValidatedServerData() {
        var definition = CyberwareUpgradeDefinition.parse(JsonParser.parseString("{\"levels\":[],\"repair_materials\":[{\"item\":\"minecraft:iron_ingot\",\"count\":2}]}"));
        assertEquals(2, definition.repairMaterials().getFirst().count());
        assertTrue(CyberwareUpgradeDefinition.parse(JsonParser.parseString("{\"levels\":[]}")).repairMaterials().isEmpty());
        for (String costs : new String[]{"[]", "[{\"item\":\"minecraft:iron_ingot\",\"count\":0}]",
                "[{\"item\":\"minecraft:iron_ingot\",\"count\":1.5}]",
                "[{\"item\":\"minecraft:iron_ingot\"},{\"item\":\"minecraft:iron_ingot\"}]"})
            assertThrows(RuntimeException.class, () -> CyberwareUpgradeDefinition.parse(JsonParser.parseString("{\"levels\":[],\"repair_materials\":" + costs + "}")));
        assertThrows(RuntimeException.class, () -> parse("\"function_multiplier\":4"));
    }
}
