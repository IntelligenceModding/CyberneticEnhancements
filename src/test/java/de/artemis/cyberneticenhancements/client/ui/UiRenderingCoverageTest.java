package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class UiRenderingCoverageTest {
    @Test
    public void everyModScreenUsesTheResponsiveInputAndRenderingAdapter() throws Exception {
        Path screens = Path.of("src/main/java/de/artemis/cyberneticenhancements/client/screen");
        int count = 0;
        try (var files = Files.list(screens)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Screen.java")).toList()) {
                String source = Files.readString(file);
                assertTrue("Screen bypasses the shared viewport: " + file,
                        source.contains("extends ResponsiveContainerScreen<") || source.contains("extends ResponsiveScreen"));
                assertFalse("Screen bypasses the shared render wrapper: " + file,
                        source.contains("public void render(GuiGraphics"));
                count++;
            }
        }
        assertEquals(7, count);
    }

    @Test
    public void clientUiDoesNotDependOnDecorativeGuiTextures() throws Exception {
        Path client = Path.of("src/main/java/de/artemis/cyberneticenhancements/client");
        try (var files = Files.walk(client)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse("Decorative GUI texture dependency: " + file,
                        Files.readString(file).contains("textures/gui/"));
            }
        }
    }

    @Test
    public void responsiveContainerKeepsTheMinecraftBackgroundRenderingContract() throws Exception {
        Path client = Path.of("src/main/java/de/artemis/cyberneticenhancements/client");
        String adapter = Files.readString(client.resolve("ui/ResponsiveContainerScreen.java"));
        int background = adapter.indexOf("public void renderBackground(");
        int backgroundEnd = adapter.indexOf("\n    }", background);
        assertTrue("Container panels and previews must be rendered by the background callback",
                adapter.substring(background, backgroundEnd).contains("renderBg(graphics, partialTick, mouseX, mouseY)"));
        try (var files = Files.list(client.resolve("screen"))) {
            for (Path file : files.filter(p -> p.toString().endsWith("Screen.java")).toList()) {
                String source = Files.readString(file);
                if (source.contains("extends ResponsiveContainerScreen<")) {
                    assertFalse("The container already renders its background: " + file,
                            source.contains("renderBackground(guiGraphics, mouseX, mouseY, partialTick)"));
                }
            }
        }
    }
}
