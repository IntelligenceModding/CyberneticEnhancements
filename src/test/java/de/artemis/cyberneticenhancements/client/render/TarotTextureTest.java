package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class TarotTextureTest {
    @Test
    public void artworkIsAnActualDecodablePngWithTheExpectedCardDimensions() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/assets/cyberneticenhancements/textures/tarot"))) {
            for (Path texture : files.filter(file -> file.toString().endsWith(".png")).toList()) checkTexture(texture);
        }
    }
    private void checkTexture(Path texture) throws Exception {
        byte[] bytes = Files.readAllBytes(texture);
        assertArrayEquals("A renamed WebP cannot be loaded by Minecraft's NativeImage",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}, java.util.Arrays.copyOf(bytes, 8));
        var image = ImageIO.read(texture.toFile());
        assertNotNull("PNG must decode completely", image);
        assertEquals(160, image.getWidth());
        assertEquals(240, image.getHeight());
    }
}
