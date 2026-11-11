package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import com.google.gson.JsonParser;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class SandevistanUpgradesTest {
@Test
    public void ShippedExperimentalTrackImprovesEveryLevelAndPreservesBalance() throws Exception {
        var definition = CyberwareUpgradeDefinition.parse(JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/data/cyberneticenhancements/cyberware_upgrades/militech_experimental_sandevistan.json"))));
        assertEquals(5, definition.levels().size());
        for (int level = 1; level <= 5; level++) {
            var previous = definition.at(level - 1);
            var next = definition.at(level);
            assertTrue(next.duration(160) > previous.duration(160));
            assertTrue(next.cooldown(600) < previous.cooldown(600));
            assertTrue(next.instabilityMultiplier() < previous.instabilityMultiplier());
        }
        assertEquals(200, definition.at(5).duration(160));
        assertEquals(480, definition.at(5).cooldown(600));
        assertEquals(18, 24 * definition.at(5).instabilityMultiplier(), 0.000001);
        assertEquals(2.625, 3.5 * definition.at(5).instabilityMultiplier(), 0.000001);
        assertEquals(160, definition.at(0).duration(160));
    }
}
