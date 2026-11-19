package de.artemis.cyberneticenhancements.common.dialogue;

import com.google.gson.JsonParser;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.Assert.*;

public class NpcServiceTest {
    @Test
    public void everyRoleHasDistinctPaidServicesWithMatchingServerAccessRules() throws Exception {
        var language = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        var seen = new HashSet<NpcService>();
        for (NpcType type : NpcType.values()) {
            var services = NpcService.forNpc(type);
            if (type == NpcType.MAXTEC || type == NpcType.WORLD_MED_TECH) { assertTrue(services.isEmpty()); continue; }
            assertEquals(2, services.size());
            for (var service : services) {
                assertTrue(seen.add(service));
                assertEquals(type, service.type());
                assertTrue(service.basePrice() > 1);
                assertEquals(service.requiredTrust(), FixerDialogMenu.requiredTrust(type, service.optionId()));
                assertSame(service, NpcService.forOption(type, service.optionId()));
                assertTrue(language.has(service.nameKey()));
                assertTrue(language.has(service.descriptionKey()));
            }
            assertEquals(FixerDialogMenu.OPTION_STABILIZE, services.getFirst().optionId());
            assertEquals(FixerDialogMenu.OPTION_PURGE, services.getLast().optionId());
        }
        assertEquals(NpcService.values().length, seen.size());
    }
}
