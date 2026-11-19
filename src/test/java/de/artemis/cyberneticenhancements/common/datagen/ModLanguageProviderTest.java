package de.artemis.cyberneticenhancements.common.datagen;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class ModLanguageProviderTest {
    @Test
    public void everyNpcHasAllReachablePhoneDialogueVariants() throws Exception {
        var language = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        for (var type : de.artemis.cyberneticenhancements.common.entity.NpcType.values()) {
            if (type == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH) continue; // Never phoned: it is no contact.
            var groups = new java.util.ArrayList<>(java.util.List.of("greeting", "services", "advice", "incoming.money"));
            if (type.supportsContracts()) groups.add("incoming.urgent");
            for (String group : groups) for (int variant = 0; variant < 3; variant++) {
                String key = "screen.cyberneticenhancements.call." + group + "." + type.id() + "." + variant;
                assertTrue("Missing phone dialogue: " + key, language.has(key));
                String text = language.get(key).getAsString();
                assertFalse("Empty phone dialogue: " + key, text.isBlank());
                if (group.equals("incoming.money"))
                    assertEquals("Money request must include its amount: " + key, 1, text.split("%s", -1).length - 1);
            }
        }
    }

    @Test
    public void everyNpcHasTranslatedConversationChromeAndDialogue() throws Exception {
        var language = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"))).getAsJsonObject();
        for (var type : de.artemis.cyberneticenhancements.common.entity.NpcType.values()) {
            String id = type.id();
            for (String suffix : new String[]{"type." + id, "section.choices." + id,
                    "line." + id + ".greeting", "line." + id + ".back", "player." + id + ".open", "player." + id + ".back"})
                assertTrue("Missing NPC translation: " + suffix, language.has("screen.cyberneticenhancements.npc." + suffix));
            var tree = de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueTrees.tree(type);
            for (var node : tree.nodes().values()) {
                assertTrue("Missing NPC line: " + node.npcLineKey(), language.has(node.npcLineKey()));
                for (var choice : node.choices())
                    assertTrue("Missing player line: " + choice.playerLineKey(), language.has(choice.playerLineKey()));
            }
        }
    }

    @Test
    public void literalTranslationKeysAreUnique() throws Exception {
        String source = Files.readString(Path.of("src/main/java/de/artemis/cyberneticenhancements/common/datagen/ModLanguageProvider.java"));
        var matcher = Pattern.compile("\\badd\\(\\s*\"([^\"]+)\"").matcher(source);
        Set<String> keys = new HashSet<>();
        while (matcher.find()) {
            assertTrue("Duplicate translation key: " + matcher.group(1), keys.add(matcher.group(1)));
        }
        assertTrue("Expected the full language provider to be checked", keys.size() > 1000);
    }
}
