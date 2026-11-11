package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class UiButton extends Button {
    private boolean selected;

    public UiButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(message));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        setTooltip(Tooltip.create(message));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        UiRenderer.button(graphics, font, getX(), getY(), getWidth(), getHeight(),
                getMessage(), active, selected, isHoveredOrFocused(), 0xFFFFFFFF);
    }
}
