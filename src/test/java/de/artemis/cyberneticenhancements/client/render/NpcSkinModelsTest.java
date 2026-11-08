package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class NpcSkinModelsTest {
    @Test
    public void detectsBothArmWidthsWithoutUsingGender() throws Exception {
        assertSlim("male/generic_1.png", true);
        assertSlim("male/kerry_eurodyne.png", true);
        assertSlim("male/maine.png", false);
        assertSlim("female/judy_alvarez.png", true);
        assertSlim("female/misty_olszewski.png", false);
        assertSlim("generic/max_tac.png", true);
        assertSlim("generic/trauma_team.png", false);
    }

    @Test
    public void eitherArmUsingWideUvSpaceRequiresWideGeometry() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        assertTrue(NpcSkinModels.hasSlimArmLayout(image));
        image.setRGB(54, 20, 0xFF112233);
        assertFalse(NpcSkinModels.hasSlimArmLayout(image));
        image.setRGB(54, 20, 0);
        image.setRGB(47, 63, 0xFF112233);
        assertFalse(NpcSkinModels.hasSlimArmLayout(image));
    }

    @Test
    public void supportsScaledSkinLayouts() {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        assertTrue(NpcSkinModels.hasSlimArmLayout(image));
        image.setRGB(109, 41, 0xFF112233);
        assertFalse(NpcSkinModels.hasSlimArmLayout(image));
    }

    @Test
    public void unsupportedImagesUseWideFallback() {
        assertFalse(NpcSkinModels.hasSlimArmLayout(null));
        assertFalse(NpcSkinModels.hasSlimArmLayout(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB)));
        assertFalse(NpcSkinModels.hasSlimArmLayout(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)));
        assertFalse(NpcSkinModels.hasSlimArmLayout(new BufferedImage(65, 65, BufferedImage.TYPE_INT_ARGB)));
    }

    private static void assertSlim(String skin, boolean expected) throws Exception {
        Path path = Path.of("src/main/resources/assets/cyberneticenhancements/textures/entity/npc", skin);
        assertEquals(skin, expected, NpcSkinModels.hasSlimArmLayout(ImageIO.read(path.toFile())));
    }
}
