package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

public final class UiEncryptedText {
    private static final String GLYPHS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789#%?";

    private UiEncryptedText() {
    }

    public static void draw(GuiGraphics graphics, Font font, FormattedCharSequence original, int x, int y, int seed, int color) {
        int[] cursor = {x};
        original.accept((index, style, codePoint) -> {
            String source = new String(Character.toChars(codePoint));
            int advance = font.width(source);
            if (!Character.isWhitespace(codePoint) && advance > 0) {
                int hash = seed * 31 + index * 0x45d9f3b;
                hash ^= hash >>> 16;
                String glyph = String.valueOf(GLYPHS.charAt(Math.floorMod(hash, GLYPHS.length())));
                graphics.pose().pushPose();
                graphics.pose().translate(cursor[0], y, 0);
                graphics.pose().scale(advance / (float) Math.max(1, font.width(glyph)), 1, 1);
                graphics.drawString(font, glyph, 0, 0, color, false);
                graphics.pose().popPose();
            }
            cursor[0] += advance;
            return true;
        });
    }
}
