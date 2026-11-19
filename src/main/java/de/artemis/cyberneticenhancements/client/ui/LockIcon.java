package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** A small padlock in the menu's colors, marking things that are switched off or locked, such as tutorial-disabled keys and server-enforced settings. */
public final class LockIcon {
    public static final int WIDTH = 9, HEIGHT = 11;

    private LockIcon() {}

    public static boolean contains(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public static void render(GuiGraphics graphics, int x, int y, boolean hovered) {
        int color = hovered ? VanillaMenuPresentation.TEXT : VanillaMenuPresentation.HIGHLIGHT;
        // Shackle, then the body with a keyhole.
        graphics.fill(x + 2, y, x + 7, y + 1, color);
        graphics.fill(x + 2, y + 1, x + 3, y + 5, color);
        graphics.fill(x + 6, y + 1, x + 7, y + 5, color);
        graphics.fill(x, y + 5, x + WIDTH, y + HEIGHT, color);
        graphics.fill(x + 4, y + 7, x + 5, y + 9, VanillaMenuPresentation.FIELD);
    }
}
