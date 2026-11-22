package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.UiCloseControl;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The map's settings, laid out like the mod's config screen (categories on the left, a scrolling list of settings on the
 * right) but in the archive's blues. It sits on top of the map instead of being a screen of its own, so closing it lands
 * back exactly where the map was left.
 */
final class MapSettingsPanel {
    private static final int MARGIN = 16, ROW_HEIGHT = 66, CONTROL_WIDTH = 142, RESET_WIDTH = 66;
    private static MapSettings.Category category = MapSettings.Category.DISPLAY;

    private boolean open;
    private double scroll;
    private MapSettings.Setting dragging;
    private boolean draggingScrollbar;
    private Font font;
    private int panelX, panelY, panelWidth, panelHeight, listX, listY, listWidth, listHeight;
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x, int y, int width, int height, Runnable run, MapSettings.Setting slider, MapSettings.Setting stepper, boolean list) {}

    boolean open() { return open; }

    void open(boolean value) {
        open = value;
        if (value) { scroll = 0; MapSettings.load(); }
        else { dragging = null; draggingScrollbar = false; }
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.archive.map.settings." + key, args);
    }

    private static boolean inside(double px, double py, int x, int y, int width, int height) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    private List<MapSettings.Setting> rows() { return MapSettings.of(category); }

    private double maximum() { return Math.max(0, rows().size() * ROW_HEIGHT - listHeight); }

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (!open) return;
        this.font = font;
        hits.clear();
        // The whole map view becomes the settings page, like the config screen: a dark backdrop, a title plate, a category
        // panel on the left and the list of settings on the right, here in the archive's blues.
        graphics.fillGradient(x, y, x + width, y + height, 0xF0050A10, 0xF00A1420);
        for (int line = y; line < y + height; line += 4) graphics.fill(x, line, x + width, line + 1, 0x10000000);
        int navWidth = Math.min(200, width / 4);
        int titleY = y + MARGIN;
        int panelsTop = titleY + 24 + 10, footerY = y + height - MARGIN - 22, panelsBottom = footerY - 8;
        panelX = x; panelY = y; panelWidth = width; panelHeight = height;

        UiRenderer.panel(graphics, x + MARGIN, titleY, width - MARGIN * 2 - UiCloseControl.WIDTH - 10, 24, UiRenderer.PANEL, UiRenderer.PANEL_EDGE_ACTIVE);
        graphics.drawString(font, text("title"), x + MARGIN + 10, titleY + 8, UiRenderer.TEXT, false);
        button(graphics, x + width - MARGIN - UiCloseControl.WIDTH, titleY, UiCloseControl.WIDTH, 24, UiCloseControl.label(), false, mouseX, mouseY, () -> open(false));

        UiRenderer.panel(graphics, x + MARGIN, panelsTop, navWidth, panelsBottom - panelsTop, UiRenderer.PANEL, UiRenderer.PANEL_EDGE_ACTIVE);
        int navY = panelsTop + 10;
        for (var entry : MapSettings.Category.values()) {
            var picked = entry;
            button(graphics, x + MARGIN + 8, navY, navWidth - 16, 24, text("category." + entry.name().toLowerCase(java.util.Locale.ROOT)),
                    entry == category, mouseX, mouseY, () -> { category = picked; scroll = 0; });
            navY += 30;
        }

        int rightX = x + MARGIN + navWidth + 10, rightWidth = x + width - MARGIN - rightX;
        UiRenderer.panel(graphics, rightX, panelsTop, rightWidth, panelsBottom - panelsTop, UiRenderer.PANEL, UiRenderer.PANEL_EDGE_ACTIVE);
        graphics.drawString(font, text("category." + category.name().toLowerCase(java.util.Locale.ROOT)), rightX + 12, panelsTop + 12, UiRenderer.TEXT, false);

        listX = rightX + 6;
        listY = panelsTop + 32;
        listWidth = rightWidth - 6 - 20;
        listHeight = panelsBottom - 6 - listY;
        scroll = Mth.clamp(scroll, 0, maximum());
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        try {
            int rowY = listY - (int) Math.round(scroll);
            for (var setting : rows()) {
                if (rowY + ROW_HEIGHT >= listY && rowY <= listY + listHeight) row(graphics, setting, rowY, mouseX, mouseY);
                rowY += ROW_HEIGHT;
            }
        } finally {
            graphics.disableScissor();
        }
        scrollbar(graphics, mouseX, mouseY);

        button(graphics, x + width - MARGIN - 130, footerY, 130, 22, text("done"), false, mouseX, mouseY, () -> open(false));
        button(graphics, x + width - MARGIN - 130 - 8 - 150, footerY, 150, 22, text("reset"), false, mouseX, mouseY, () -> MapSettings.reset(category));
    }

    private void row(GuiGraphics graphics, MapSettings.Setting setting, int rowY, int mouseX, int mouseY) {
        int rowHeight = ROW_HEIGHT - 6;
        graphics.fill(listX, rowY, listX + listWidth, rowY + rowHeight, UiRenderer.ROW);
        int resetX = listX + listWidth - 8 - RESET_WIDTH, controlX = resetX - 8 - CONTROL_WIDTH, controlY = rowY + (rowHeight - 22) / 2;
        int textWidth = Math.max(40, controlX - listX - 24);
        graphics.drawString(font, font.plainSubstrByWidth(setting.title().getString(), textWidth), listX + 8, rowY + 7, UiRenderer.TEXT, false);
        var description = font.split(setting.description(), textWidth);
        for (int line = 0; line < Math.min(3, description.size()); line++)
            graphics.drawString(font, description.get(line), listX + 8, rowY + 20 + line * 11, UiRenderer.TEXT_SECONDARY, false);

        boolean visible = controlY + 22 > listY && controlY < listY + listHeight;
        if (setting.type == MapSettings.Type.RANGE) {
            graphics.fill(controlX, controlY, controlX + CONTROL_WIDTH, controlY + 22, UiRenderer.FIELD);
            float fraction = (setting.value() - setting.min) / (float) (setting.max - setting.min);
            int thumb = controlX + Math.round(fraction * (CONTROL_WIDTH - 4));
            graphics.fill(controlX + 1, controlY + 1, thumb + 2, controlY + 21, UiRenderer.ROW_SELECTED);
            graphics.fill(thumb, controlY, thumb + 4, controlY + 22, dragging == setting ? UiRenderer.TEXT : UiRenderer.ACCENT);
            UiRenderer.outline(graphics, controlX, controlY, CONTROL_WIDTH, 22, UiRenderer.PANEL_EDGE_ACTIVE);
            graphics.drawCenteredString(font, setting.shown(), controlX + CONTROL_WIDTH / 2, controlY + 7, UiRenderer.TEXT);
            if (visible) hits.add(new Hit(controlX, controlY, CONTROL_WIDTH, 22, () -> {}, setting, null, true));
        } else {
            boolean on = setting.type == MapSettings.Type.CHOICE || setting.on();
            UiRenderer.button(graphics, font, controlX, controlY, CONTROL_WIDTH, 22, setting.shown(), true, on,
                    visible && inside(mouseX, mouseY, controlX, controlY, CONTROL_WIDTH, 22), -1);
            if (visible) hits.add(new Hit(controlX, controlY, CONTROL_WIDTH, 22, () -> setting.step(1), null, setting, true));
        }
        boolean changed = setting.value() != setting.fallback;
        UiRenderer.button(graphics, font, resetX, controlY, RESET_WIDTH, 22, text("reset_one"), changed, false,
                changed && visible && inside(mouseX, mouseY, resetX, controlY, RESET_WIDTH, 22), -1);
        if (changed && visible) hits.add(new Hit(resetX, controlY, RESET_WIDTH, 22, () -> setting.set(setting.fallback), null, null, true));
    }

    private void scrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        double maximum = maximum();
        if (maximum <= 0) return;
        int trackX = listX + listWidth + 6, trackWidth = 6;
        int thumbHeight = Math.max(20, (int) (listHeight * (listHeight / (listHeight + maximum))));
        int thumbY = listY + (int) ((listHeight - thumbHeight) * (scroll / maximum));
        boolean hovered = inside(mouseX, mouseY, trackX - 2, listY, trackWidth + 4, listHeight);
        graphics.fill(trackX, listY, trackX + trackWidth, listY + listHeight, UiRenderer.FIELD);
        UiRenderer.outline(graphics, trackX, listY, trackWidth, listHeight, UiRenderer.FIELD_EDGE);
        graphics.fill(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, draggingScrollbar || hovered ? UiRenderer.SELECTED_EDGE : UiRenderer.PANEL_EDGE_ACTIVE);
        UiRenderer.outline(graphics, trackX, thumbY, trackWidth, thumbHeight, UiRenderer.ACCENT);
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean selected, int mouseX, int mouseY, Runnable run) {
        UiRenderer.button(graphics, font, x, y, width, height, label, true, selected, inside(mouseX, mouseY, x, y, width, height), -1);
        hits.add(new Hit(x, y, width, height, run, null, null, false));
    }

    private void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private void setFromPointer(MapSettings.Setting setting, double mouseX) {
        for (var hit : hits) {
            if (hit.slider != setting) continue;
            double fraction = Mth.clamp((mouseX - hit.x - 3) / (CONTROL_WIDTH - 6.0), 0, 1);
            int raw = setting.min + (int) Math.round(fraction * (setting.max - setting.min) / setting.step) * setting.step;
            if (raw != setting.value()) setting.set(raw);
            return;
        }
    }

    boolean click(double mouseX, double mouseY, int button) {
        if (!open) return false;
        if (button == 0 && maximum() > 0 && inside(mouseX, mouseY, listX + listWidth + 4, listY, 10, listHeight)) {
            draggingScrollbar = true;
            dragScrollbar(mouseY);
            return true;
        }
        // Only what is visible in the list can be hit, so a row scrolled out of sight never catches a click.
        boolean inList = inside(mouseX, mouseY, listX, listY, listWidth, listHeight);
        for (int index = hits.size() - 1; index >= 0; index--) {
            var hit = hits.get(index);
            if (!inside(mouseX, mouseY, hit.x, hit.y, hit.width, hit.height)) continue;
            if (hit.list && !inList) continue;
            if (hit.slider != null) {
                if (button != 0) return true;
                dragging = hit.slider;
                setFromPointer(hit.slider, mouseX);
                sound(1.4F);
            } else if (hit.stepper != null && button == 1) {
                hit.stepper.step(-1); // A right click steps a choice backwards.
                sound(0.9F);
            } else if (button == 0) {
                hit.run.run();
                sound(1F);
            }
            return true;
        }
        // The page swallows every click while it is up.
        return true;
    }

    private void dragScrollbar(double mouseY) {
        double maximum = maximum();
        if (maximum <= 0) return;
        scroll = Mth.clamp((mouseY - listY) / listHeight * (maximum + listHeight) - listHeight / 2.0, 0, maximum);
    }

    boolean drag(double mouseX, double mouseY) {
        if (!open) return false;
        if (dragging != null) setFromPointer(dragging, mouseX);
        else if (draggingScrollbar) dragScrollbar(mouseY);
        return true;
    }

    boolean release() {
        if (!open) return false;
        dragging = null;
        draggingScrollbar = false;
        return true;
    }

    boolean scroll(double amount) {
        if (!open) return false;
        scroll = Mth.clamp(scroll - amount * ROW_HEIGHT, 0, maximum());
        return true;
    }

    boolean key(int key) {
        if (!open) return false;
        if (key == 256) open(false);
        return true;
    }
}
