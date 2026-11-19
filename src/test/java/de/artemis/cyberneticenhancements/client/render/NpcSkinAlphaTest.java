package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class NpcSkinAlphaTest {
    @Test
    public void glassPixelsKeepTheirColorAndAlphaWithoutEnteringTheOpaquePass() {
        int[] source = new int[256];
        for (int alpha = 0; alpha < 256; alpha++) source[alpha] = alpha << 24 | 0x123456;
        var layers = NpcSkinAlpha.split(source);
        assertTrue(layers.hasTranslucency());
        assertEquals(0, layers.opaque()[0]);
        assertEquals(0, layers.translucent()[0]);
        for (int alpha = 1; alpha < 255; alpha++) {
            assertEquals(0, layers.opaque()[alpha]);
            assertEquals(source[alpha], layers.translucent()[alpha]);
        }
        assertEquals(source[255], layers.opaque()[255]);
        assertEquals(0, layers.translucent()[255]);
    }

    @Test
    public void ordinarySkinsDoNotRequireAnExtraTranslucentPass() {
        var layers = NpcSkinAlpha.split(new int[]{0, 0xFF000000, 0xFFFFFFFF, 0xFF123456});
        assertFalse(layers.hasTranslucency());
        assertArrayEquals(new int[]{0, 0xFF000000, 0xFFFFFFFF, 0xFF123456}, layers.opaque());
        assertArrayEquals(new int[4], layers.translucent());
    }

    @Test
    public void bundledSkinsRetainEveryVisiblePixelAcrossBothPasses() throws Exception {
        int checked = 0;
        try (var files = Files.walk(Path.of("src/main/resources/assets/cyberneticenhancements/textures/entity/npc"))) {
            for (Path path : files.filter(file -> file.toString().endsWith(".png")).toList()) {
                var image = ImageIO.read(path.toFile());
                int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
                var layers = NpcSkinAlpha.split(pixels);
                for (int i = 0; i < pixels.length; i++) {
                    int visible = pixels[i] >>> 24 == 0 ? 0 : pixels[i];
                    assertEquals(path + " pixel " + i, visible, layers.opaque()[i] | layers.translucent()[i]);
                    assertTrue(layers.opaque()[i] == 0 || layers.translucent()[i] == 0);
                }
                checked++;
            }
        }
        assertTrue("No actual skins were checked", checked > 0);
    }
}