package de.artemis.cyberneticenhancements.client.screen;

import com.google.gson.JsonParser;
import org.junit.Test;
import java.nio.file.*;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class WikiReferenceCoverageTest {
    @Test
    public void referencedWikiHeadingsAndTextHaveTranslations() throws Exception {
        var language = JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        String source = Files.readString(Path.of("src/main/java/de/artemis/cyberneticenhancements/client/screen/CyberwareWikiData.java"));
        var matcher = Pattern.compile("(wiki|sectionKey)\\(\\\"([a-z0-9_.]+)\\\"").matcher(source);
        while (matcher.find()) {
            String suffix = matcher.group(2);
            if (suffix.endsWith(".")) continue;
            String key = "wiki.cyberneticenhancements." + (matcher.group(1).equals("sectionKey") ? "section." : "") + suffix;
            assertTrue("Untranslated wiki reference: " + key, language.has(key));
        }
        for (String role : new String[]{"fixer", "ripperdoc", "techie", "netrunner", "merc"}) {
            assertTrue(language.has("wiki.cyberneticenhancements.npc.role." + role));
            assertTrue(language.has("wiki.cyberneticenhancements.npc.services." + role));
        }
    }
}