package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class UiEditBox extends EditBox {
    public UiEditBox(Font font, int x, int y, int width, int height, Component title) {
        super(font, x, y, width, height, title);
        setBordered(false);
        setTextColor(UiRenderer.TEXT);
        setTextColorUneditable(UiRenderer.TEXT_SECONDARY);
    }

    @Override
    public int getInnerWidth() {
        return Math.max(1, getWidth() - 12);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiRenderer.panel(graphics, getX(), getY(), getWidth(), getHeight(), UiRenderer.FIELD,
                isFocused() ? UiRenderer.ACCENT : isMouseOver(mouseX, mouseY) && isActive()
                        ? UiRenderer.PANEL_EDGE_ACTIVE : UiRenderer.FIELD_EDGE);
        graphics.enableScissor(getX() + 4, getY() + 2, getX() + getWidth() - 4, getY() + getHeight() - 2);
        graphics.pose().pushPose();
        graphics.pose().translate(6, (getHeight() - 8) / 2.0F, 0);
        super.renderWidget(graphics, mouseX - 6, mouseY, partialTick);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(Math.max(getX(), Math.min(getX() + getInnerWidth(), mouseX - 6)), mouseY, button);
    }
}
