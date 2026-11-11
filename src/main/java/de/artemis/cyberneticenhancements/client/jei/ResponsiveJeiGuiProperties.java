package de.artemis.cyberneticenhancements.client.jei;

import mezz.jei.api.gui.handlers.IGuiProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

record ResponsiveJeiGuiProperties(Class<? extends Screen> screenClass, int guiLeft, int guiTop,
                                  int guiXSize, int guiYSize, int screenWidth, int screenHeight) implements IGuiProperties {
    static IGuiProperties of(AbstractContainerScreen<?> screen) {
        var window = Minecraft.getInstance().getWindow();
        return new ResponsiveJeiGuiProperties(screen.getClass(), screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }
}
