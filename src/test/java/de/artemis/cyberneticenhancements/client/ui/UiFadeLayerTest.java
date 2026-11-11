package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.Font;
import org.junit.Test;

import static org.junit.Assert.*;

public class UiFadeLayerTest {
    @Test
    public void fontRestoresNearTransparentTextToFullOpacity() throws Exception {
        var adjustColor = Font.class.getDeclaredMethod("adjustColor", int.class);
        adjustColor.setAccessible(true);
        for (int alpha = 0; alpha < 4; alpha++) {
            int adjusted = (int) adjustColor.invoke(null, (alpha << 24) | 0x8BDAF8);
            assertEquals("Nearly transparent text is treated as legacy RGB", 255, adjusted >>> 24);
        }
        assertEquals(4, ((int) adjustColor.invoke(null, 0x048BDAF8)) >>> 24);
    }
}
