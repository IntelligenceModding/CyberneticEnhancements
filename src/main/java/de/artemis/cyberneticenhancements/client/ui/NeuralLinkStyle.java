package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.GameplayHudVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

public final class NeuralLinkStyle {
    public static final int WIDTH = 212;
    public static final int MARGIN = 12;
    public static final int INSET = 9;
    public static final int HEADER_HEIGHT = 34;
    public static final int CONTENT_TOP = 40;
    public static final int GAP = 3;
    public static final int OPEN_MILLIS = 280;
    public static final int CLOSE_MILLIS = 220;

    private NeuralLinkStyle() {
    }

    public static UiViewport viewport() {
        return ResponsiveUi.viewport(1280, 720);
    }

    public static int x(UiViewport viewport, boolean left) {
        if (left) return MARGIN;
        int rightMargin = MARGIN;
        var minecraft = Minecraft.getInstance();
        if (GameplayHudVisibility.shows(GameplayHudVisibility.Element.EFFECTS, true)
                && !(minecraft.screen instanceof EffectRenderingInventoryScreen inventory && inventory.canSeeEffects())) {
            int visibleEffects = (int) minecraft.player.getActiveEffects().stream()
                    .filter(effect -> effect.showIcon() && IClientMobEffectExtensions.of(effect).isVisibleInGui(effect))
                    .count();
            int rows = StatusEffectHudLayout.rows(minecraft.getWindow().getGuiScaledHeight(), minecraft.isDemo());
            int reserved = StatusEffectHudLayout.reservedWidth(visibleEffects, rows);
            if (reserved > 0) rightMargin = Math.max(rightMargin, (int) Math.ceil(viewport.toUi(reserved + 5)));
        }
        return Math.max(MARGIN, viewport.width() - WIDTH - rightMargin);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int height, int accent) {
        GameplayHudStyle.panel(graphics, x, y, WIDTH, height, accent, 1);
        GameplayHudStyle.separator(graphics, x + INSET, y + HEADER_HEIGHT, WIDTH - INSET * 2);
    }

    public static void header(GuiGraphics graphics, int x, int y, Component detail, Component trailing, int accent) {
        var font = Minecraft.getInstance().font;
        Component label = Component.translatable("screen.cyberneticenhancements.call.header");
        graphics.drawString(font, label, x + INSET, y + 6, GameplayHudStyle.color(accent, .8F), false);
        int detailX = x + INSET;
        int right = x + WIDTH - INSET;
        if (!trailing.getString().isEmpty()) {
            graphics.drawString(font, trailing, right - font.width(trailing), y + 20, UiRenderer.TEXT_SECONDARY, false);
            right -= font.width(trailing) + 8;
        }
        int available = Math.max(1, right - detailX);
        graphics.drawString(font, HudText.trim(font, detail, available), detailX, y + 20, UiRenderer.TEXT, false);
    }
}
