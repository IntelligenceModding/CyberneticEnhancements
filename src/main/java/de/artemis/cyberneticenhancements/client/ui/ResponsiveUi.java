package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public interface ResponsiveUi {
    UiViewport viewport();

    default GuiGraphics graphics(GuiGraphics source) {
        return new UiGraphics(source, viewport());
    }

    static UiViewport referenceViewport() {
        return viewport(960, 600);
    }

    static UiViewport viewport(int referenceWidth, int referenceHeight) {
        var window = Minecraft.getInstance().getWindow();
        return UiViewport.fit(window.getWidth(), window.getHeight(), window.getGuiScale(), referenceWidth, referenceHeight);
    }
}
