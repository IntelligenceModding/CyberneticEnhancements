package de.artemis.cyberneticenhancements.client.screen;

import net.minecraft.client.Minecraft;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiMotion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class WikiLaunchButton extends AbstractButton {
    private final Runnable onPress;
    private final UiMotion highlight = new UiMotion();

    public WikiLaunchButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        float scale = minecraft.screen instanceof ResponsiveUi ? 1 : (float) ResponsiveUi.referenceViewport().renderScale();
        int w = Math.round(width / scale);
        int h = Math.round(height / scale);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(), getY(), 0);
        guiGraphics.pose().scale(scale, scale, 1);
        UiRenderer.action(guiGraphics, 0, 0, w, h, active, highlight.update(active && isHoveredOrFocused()));
        guiGraphics.drawString(font, getMessage(), (w - font.width(getMessage())) / 2, (h - font.lineHeight) / 2,
                active ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
