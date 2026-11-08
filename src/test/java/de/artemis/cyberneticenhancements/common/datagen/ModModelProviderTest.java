package de.artemis.cyberneticenhancements.common.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import static org.junit.Assert.*;

public class ModModelProviderTest {
    private static final Path MODELS = Path.of("src/generated/resources/assets/cyberneticenhancements/models");
    private static final Path SOURCES = Path.of("src/main/java/de/artemis/cyberneticenhancements/common");

    @Test
    public void shippedModelsCoverRegisteredItemAndCatalogNames() throws Exception {
        Pattern literal = Pattern.compile("(?:register[A-Za-z]*|def|chipSocket)\\(\\s*\"([a-z0-9_]+)\"");
        int checked = 0;
        for (String source : new String[]{"registry/ModItems.java", "registry/ModBlocks.java", "cyberware/CyberwareCatalog.java",
                "cyberware/ChipwareCatalog.java", "cyberware/CyberwareModuleCatalog.java", "consumable/CyberConsumableCatalog.java"}) {
            var matcher = literal.matcher(Files.readString(SOURCES.resolve(source)));
            while (matcher.find()) {
                String id = matcher.group(1);
                assertTrue("Missing item model for " + id, Files.isRegularFile(MODELS.resolve("item/" + id + ".json")));
                checked++;
            }
        }
        assertTrue("Expected coverage of the full catalogs", checked > 150);
    }

    @Test
    public void everyItemModelResolvesWithoutVanillaEntityRenderers() throws Exception {
        Map<String, JsonObject> models = new HashMap<>();

        Path artifacts = Path.of("build/moddev/artifacts");
        Path resources;
        try (var files = Files.list(artifacts)) {
            resources = files.filter(p -> p.getFileName().toString().endsWith("client-extra-aka-minecraft-resources.jar")).findFirst().orElseThrow();
        }
        try (ZipFile zip = new ZipFile(resources.toFile())) {
            for (var entry : zip.stream().filter(e -> e.getName().startsWith("assets/minecraft/models/") && e.getName().endsWith(".json")).toList()) {
                try (var reader = new java.io.InputStreamReader(zip.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8)) {
                    models.put("minecraft:" + entry.getName().substring("assets/minecraft/models/".length(), entry.getName().length() - 5), JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        }
        try (var files = Files.walk(MODELS)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String id = MODELS.relativize(path).toString().replace('\\', '/').replace(".json", "");
                models.put("cyberneticenhancements:" + id, JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            }
        }
        for (String id : models.keySet().stream().filter(k -> k.startsWith("cyberneticenhancements:item/")).toList()) {
            String current = id;
            Set<String> visited = new HashSet<>();
            while (true) {
                assertTrue("Model cycle: " + id, visited.add(current));
                JsonObject model = models.get(current);
                assertNotNull("Missing parent " + current + " for " + id, model);
                if (!model.has("parent")) break;
                current = model.get("parent").getAsString();
                if (!current.contains(":")) current = "minecraft:" + current;
                assertNotEquals("Invisible item using a vanilla-only renderer: " + id, "minecraft:builtin/entity", current);
                if (current.equals("minecraft:builtin/generated")) break;
            }
        }
    }

    @Test
    public void normalModelsRemainUnchanged() {
        assertEquals("minecraft:item/iron_sword", ModModelProvider.visibleItemModelParent("minecraft:item/iron_sword"));
        assertEquals("minecraft:item/clock", ModModelProvider.visibleItemModelParent("minecraft:item/clock"));
    }
}
