package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload;
import de.artemis.cyberneticenhancements.common.network.FastTravelRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The map of the terminals found so far, opened at one: click another to travel to it. */
public final class FastTravelScreen extends Screen {
    private final MapBrowser map = new MapBrowser();
    private final BlockPos origin;
    private final int cost;

    public FastTravelScreen(BlockPos origin, int cost) {
        super(Component.translatable("screen.cyberneticenhancements.fast_travel.title"));
        this.origin = origin;
        this.cost = cost;
    }

    @Override
    protected void init() { map.travel(new MapBrowser.Travel(origin, cost, this::go)); }

    private void go(FastTravelPointsPayload.Point point) {
        PacketDistributor.sendToServer(new FastTravelRequestPayload(point.dimension(), point.pos()));
        onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD0040A10);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        map.render(graphics, font, 0, 0, width, height, mouseX, mouseY);
        // The title and price sit on a dark strip over the top edge of the map.
        var price = Component.translatable("screen.cyberneticenhancements.fast_travel.cost", cost);
        int label = font.width(title) + 12 + font.width(price), left = (width - label) / 2;
        graphics.fill(left - 8, 4, left + label + 8, 4 + font.lineHeight + 8, 0xB0040A10);
        graphics.drawString(font, title, left, 8, UiRenderer.TEXT, false);
        graphics.drawString(font, price, left + font.width(title) + 12, 8, UiRenderer.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return map.click(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button); }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return map.drag(mouseX, mouseY, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        map.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return map.scroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && map.closeSettings()) return true;
        return map.key(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() { map.hide(); }
}
