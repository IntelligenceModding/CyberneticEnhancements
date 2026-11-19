package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.CompassHudOverlay;
import de.artemis.cyberneticenhancements.client.CompassPreferences;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContactLayout;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContentLayout;
import de.artemis.cyberneticenhancements.client.ui.CompassLandmark;
import de.artemis.cyberneticenhancements.client.ui.UiEditBox;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiPixelSnap;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class CompassBrowser {
    private static final int GAP = 10, SETTING_HEIGHT = 64, MARKER_HEIGHT = 108;
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final List<Action> actions = new ArrayList<>();
    private Font font;
    private int x, y, width, height, mouseX, mouseY, page, typeFilter, visibilityFilter, bodyTop, maxScroll;
    private boolean body, modalDrawing;
    private UiEditBox search, renameField;
    private String query = "";
    private CompassLandmark editing;
    private List<Component> tooltip = List.of();

    private Component text(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.archive.compass." + key, args);
    }

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        this.font = font; this.x = x; this.y = y; this.width = width; this.height = height;
        this.mouseX = mouseX; this.mouseY = mouseY;
        actions.clear(); tooltip = List.of(); body = false; modalDrawing = false;
        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.PANEL);
        button(graphics, x + GAP, y + GAP, 96, 22, text("display"), page == 0, () -> selectPage(0));
        button(graphics, x + GAP + 106, y + GAP, 96, 22, text("markers"), page == 1, () -> selectPage(1));
        button(graphics, x + width - GAP - 148, y + GAP, 148, 22, text(page == 0 ? "reset_display" : "restore_markers"), false, () -> {
            if (page == 0) {
                settings().forEach(setting -> setting.reset.run());
                ClientConfig.SPEC.save();
            } else CompassPreferences.restoreMarkers();
        });
        if (page == 1) renderSearch(graphics);
        bodyTop = y + (page == 0 ? 42 : 104);
        int fullWidth = ArchiveContentLayout.contentWidth(width);
        var settings = settings();
        var markers = filteredMarkers();
        int contentWidth = fullWidth;
        int total = contentHeight(page == 0 ? settings.size() : markers.size(), contentWidth);
        if (total > visibleHeight()) {
            contentWidth = ArchiveContentLayout.contentWidth(width, true);
            total = contentHeight(page == 0 ? settings.size() : markers.size(), contentWidth);
        }
        maxScroll = Math.max(0, total - visibleHeight());
        scroll.clamp(maxScroll); scroll.tick();
        int left = ArchiveContentLayout.contentX(x), top = bodyTop - (int) Math.round(scroll.current);
        graphics.enableScissor(left, bodyTop, left + contentWidth + 3, y + height - GAP);
        body = true;
        if (page == 0) {
            preview(graphics, left, top, contentWidth);
            top += 88;
            for (int index = 0; index < settings.size(); index++) {
                var bounds = ArchiveContactLayout.card(index, contentWidth);
                int rowY = top + index / ArchiveContactLayout.columns(contentWidth) * (SETTING_HEIGHT + GAP);
                if (rowY + SETTING_HEIGHT >= bodyTop && rowY < y + height - GAP) {
                    UiPixelSnap.push(graphics, left + bounds.x(), rowY);
                    try { setting(graphics, left + bounds.x(), rowY, bounds.width(), settings.get(index)); }
                    finally { graphics.pose().popPose(); }
                }
            }
        } else {
            if (markers.isEmpty()) {
                UiRenderer.panel(graphics, left, top, contentWidth, 74, UiRenderer.ROW);
                graphics.drawCenteredString(font, text(CompassPreferences.all().isEmpty() ? "empty" : "no_matches"), left + contentWidth / 2, top + 32, UiRenderer.TEXT_SECONDARY);
            }
            for (int index = 0; index < markers.size(); index++) {
                var bounds = ArchiveContactLayout.card(index, contentWidth);
                int rowY = top + index / ArchiveContactLayout.columns(contentWidth) * (MARKER_HEIGHT + GAP);
                if (rowY + MARKER_HEIGHT >= bodyTop && rowY < y + height - GAP) {
                    UiPixelSnap.push(graphics, left + bounds.x(), rowY);
                    try { marker(graphics, left + bounds.x(), rowY, bounds.width(), markers.get(index)); }
                    finally { graphics.pose().popPose(); }
                }
            }
        }
        body = false;
        graphics.disableScissor();
        UiScrollbar.draw(graphics, track(), mouseX, mouseY, scrollbar.dragging());
        if (editing != null) renderRename(graphics);
        else if (!tooltip.isEmpty()) graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private int visibleHeight() {
        return Math.max(1, y + height - GAP - bodyTop);
    }

    private int contentHeight(int count, int contentWidth) {
        int rows = (count + ArchiveContactLayout.columns(contentWidth) - 1) / ArchiveContactLayout.columns(contentWidth);
        return page == 0 ? 88 + rows * (SETTING_HEIGHT + GAP) - GAP : rows == 0 ? 74 : rows * (MARKER_HEIGHT + GAP) - GAP;
    }

    private void preview(GuiGraphics graphics, int x, int y, int width) {
        UiRenderer.panel(graphics, x, y, width, 78, UiRenderer.ROW);
        graphics.drawString(font, text("preview"), x + GAP, y + 9, UiRenderer.TEXT, false);
        Component state = text(ClientConfig.SHOW_COMPASS.get() ? "enabled" : "disabled");
        graphics.drawString(font, state, x + width - GAP - font.width(state), y + 9, UiRenderer.ACCENT, false);
        graphics.enableScissor(x + 4, y + 23, x + width - 4, y + 75);
        CompassHudOverlay.renderPreview(graphics, x + width / 2, y + 27);
        graphics.disableScissor();
    }

    private void setting(GuiGraphics graphics, int x, int y, int width, Setting setting) {
        UiRenderer.panel(graphics, x, y, width, SETTING_HEIGHT, UiRenderer.ROW);
        graphics.drawString(font, trim(text("setting." + setting.id), width - 124), x + GAP, y + 13, UiRenderer.TEXT, false);
        button(graphics, x + width - 104, y + GAP, 94, 20, setting.value.get(), false, () -> {
            setting.change.run();
            ClientConfig.SPEC.save();
        });
        Component help = text("setting." + setting.id + ".help");
        var lines = font.split(help, width - GAP * 2);
        for (int line = 0; line < Math.min(2, lines.size()); line++)
            graphics.drawString(font, lines.get(line), x + GAP, y + 36 + line * 11, UiRenderer.TEXT_SECONDARY, false);
        if (lines.size() > 2 && inside(mouseX, mouseY, x, y, width, SETTING_HEIGHT)
                && graphics.containsPointInScissor(mouseX, mouseY)) tooltip = List.of(help);
    }

    private List<Setting> settings() {
        return List.of(toggle("enabled", ClientConfig.SHOW_COMPASS), toggle("quests", ClientConfig.COMPASS_QUESTS),
                toggle("turn_ins", ClientConfig.COMPASS_TURN_INS), toggle("banners", ClientConfig.COMPASS_BANNERS),
                toggle("cities", ClientConfig.COMPASS_CITIES),
                toggle("max_tac", ClientConfig.COMPASS_MAX_TAC),
                toggle("medtec", ClientConfig.COMPASS_MEDTEC),
                toggle("cardinals", ClientConfig.COMPASS_CARDINALS), toggle("ticks", ClientConfig.COMPASS_TICKS),
                toggle("center", ClientConfig.COMPASS_CENTER), toggle("names", ClientConfig.COMPASS_NAMES),
                toggle("distances", ClientConfig.COMPASS_DISTANCES), toggle("height", ClientConfig.COMPASS_HEIGHT_ARROWS),
                toggle("edges", ClientConfig.COMPASS_EDGE_ARROWS), number("width", ClientConfig.COMPASS_WIDTH, 32, 128, 320, "pixels"),
                number("margin", ClientConfig.COMPASS_TOP_MARGIN, 6, 0, 60, "pixels"),
                number("focus", ClientConfig.COMPASS_FOCUS_ANGLE, 2, 3, 30, "degrees"),
                new Setting("opacity", () -> text("percent", Math.round(ClientConfig.COMPASS_OPACITY.get() * 100)),
                        () -> ClientConfig.COMPASS_OPACITY.set(ClientConfig.COMPASS_OPACITY.get() >= 0.99 ? 0.4
                                : Math.min(1, Math.round((ClientConfig.COMPASS_OPACITY.get() + 0.1) * 10) / 10.0)),
                        () -> ClientConfig.COMPASS_OPACITY.set(ClientConfig.COMPASS_OPACITY.getDefault())));
    }

    private Setting toggle(String id, ModConfigSpec.BooleanValue value) {
        return new Setting(id, () -> text(value.get() ? "enabled" : "disabled"), () -> value.set(!value.get()), () -> value.set(value.getDefault()));
    }

    private Setting number(String id, ModConfigSpec.IntValue value, int step, int min, int max, String unit) {
        return new Setting(id, () -> text(unit, value.get()), () -> value.set(value.get() + step > max ? min : value.get() + step),
                () -> value.set(value.getDefault()));
    }

    private void renderSearch(GuiGraphics graphics) {
        if (search == null) {
            search = new UiEditBox(font, x + GAP, y + 42, width - 180, 22, text("search"));
            search.setHint(text("search")); search.setMaxLength(80);
            search.setResponder(value -> { query = value.strip().toLowerCase(Locale.ROOT); scroll.reset(); });
        }
        search.setX(x + GAP); search.setY(y + 42); search.setWidth(Math.max(80, width - GAP * 3 - 160));
        search.render(graphics, mouseX, mouseY, 0);
        button(graphics, x + width - GAP - 160, y + 42, 160, 22, text("type_filter", text("type." + typeFilter)), typeFilter > 0,
                () -> { typeFilter = (typeFilter + 1) % (CompassLandmark.Kind.values().length + 1); scroll.reset(); });
        for (int index = 0; index < 3; index++) {
            int selected = index;
            button(graphics, x + GAP + index * 94, y + 74, 84, 20, text("filter." + index), visibilityFilter == index,
                    () -> { visibilityFilter = selected; scroll.reset(); });
        }
    }

    private List<CompassLandmark> filteredMarkers() {
        return CompassPreferences.all().stream().filter(point -> typeFilter == 0 || typeFilter - 1 == point.kind().ordinal())
                .filter(point -> visibilityFilter == 0 || CompassPreferences.entry(point.id()).hidden() == (visibilityFilter == 2))
                .filter(point -> (CompassPreferences.customized(point).title() + " " + point.title() + " " + point.dimension())
                        .toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(point -> CompassPreferences.customized(point).title(), String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void marker(GuiGraphics graphics, int x, int y, int width, CompassLandmark original) {
        var point = CompassPreferences.customized(original);
        var preference = CompassPreferences.entry(point.id());
        UiRenderer.panel(graphics, x, y, width, MARKER_HEIGHT, UiRenderer.ROW);
        if (point.kind() == CompassLandmark.Kind.BANNER) CompassHudOverlay.renderBannerIcon(graphics, x + GAP, y + 9, point.color());
        else if (point.kind() == CompassLandmark.Kind.CITY) CompassHudOverlay.renderCityIcon(graphics, x + GAP, y + 9, point.color());
        else if (point.kind() == CompassLandmark.Kind.MAX_TAC) CompassHudOverlay.renderMaxTacIcon(graphics, x + GAP, y + 9, point.color());
        else if (point.kind() == CompassLandmark.Kind.MEDTEC) CompassHudOverlay.renderMedtecIcon(graphics, x + GAP, y + 9, point.color());
        else graphics.fill(x + GAP, y + 12, x + GAP + 6, y + 18, point.color());
        graphics.drawString(font, trim(Component.literal(point.title()), width - 36), x + 24, y + GAP, UiRenderer.TEXT, false);
        graphics.drawString(font, text("type." + (point.kind().ordinal() + 1)), x + GAP, y + 26, UiRenderer.ACCENT, false);
        String position = point.pos().getX() + ", " + point.pos().getY() + ", " + point.pos().getZ() + " · " + point.dimension();
        graphics.drawString(font, trim(Component.literal(position), width - GAP * 2), x + GAP, y + 40, UiRenderer.TEXT_SECONDARY, false);
        Component status = text(preference.hidden() ? "hidden" : !ClientConfig.SHOW_COMPASS.get() ? "compass_disabled"
                : CompassPreferences.categoryEnabled(point) ? "visible" : "category_disabled");
        if (point.kind() == CompassLandmark.Kind.CITY) status = text("city_status", text(
                de.artemis.cyberneticenhancements.client.CityNavigationClientState.get().visited() ? "visited" : "unvisited"), status);
        graphics.drawString(font, status, x + GAP, y + 54, UiRenderer.TEXT_SECONDARY, false);
        int buttonWidth = (width - GAP * 5) / 4;
        button(graphics, x + GAP, y + 78, buttonWidth, 20, text("rename"), false, () -> {
            editing = original; renameField = null; if (search != null) search.setFocused(false);
        });
        button(graphics, x + GAP * 2 + buttonWidth, y + 78, buttonWidth, 20, text("color"), preference.color() != null,
                () -> CompassPreferences.cycleColor(point.id()));
        button(graphics, x + GAP * 3 + buttonWidth * 2, y + 78, buttonWidth, 20,
                text(preference.hidden() ? "show" : "hide"), preference.hidden(), () -> CompassPreferences.toggleHidden(point.id()));
        int deleteX = x + GAP * 4 + buttonWidth * 3;
        button(graphics, deleteX, y + 78, x + width - GAP - deleteX, 20, text("delete"), false,
                () -> CompassPreferences.delete(original));
    }

    private void renderRename(GuiGraphics graphics) {
        modalDrawing = true;
        actions.clear(); tooltip = List.of();
        graphics.fill(x, y, x + width, y + height, 0xA0081018);
        int modalWidth = Math.min(420, width - GAP * 4), left = x + (width - modalWidth) / 2, top = y + (height - 126) / 2;
        UiRenderer.panel(graphics, left, top, modalWidth, 126, UiRenderer.PANEL);
        graphics.drawString(font, trim(text("rename_title", editing.title()), modalWidth - 20), left + GAP, top + 12, UiRenderer.TEXT, false);
        if (renameField == null) {
            renameField = new UiEditBox(font, left + GAP, top + 34, modalWidth - GAP * 2, 22, text("rename"));
            renameField.setMaxLength(80); renameField.setHint(Component.literal(editing.title()));
            renameField.setValue(CompassPreferences.entry(editing.id()).label()); renameField.setFocused(true);
        }
        renameField.setX(left + GAP); renameField.setY(top + 34); renameField.setWidth(modalWidth - GAP * 2);
        renameField.render(graphics, mouseX, mouseY, 0);
        graphics.drawString(font, trim(text("rename_help"), modalWidth - 20), left + GAP, top + 64, UiRenderer.TEXT_SECONDARY, false);
        int bw = (modalWidth - GAP * 4) / 3;
        button(graphics, left + GAP, top + 94, bw, 22, text("save"), false, this::saveName);
        button(graphics, left + GAP * 2 + bw, top + 94, bw, 22, text("default_name"), false, () -> {
            if (editing != null) CompassPreferences.rename(editing.id(), ""); editing = null;
        });
        button(graphics, left + GAP * 3 + bw * 2, top + 94, bw, 22, text("cancel"), false, () -> editing = null);
        modalDrawing = false;
    }

    private void saveName() {
        if (editing != null && renameField != null) CompassPreferences.rename(editing.id(), renameField.getValue());
        editing = null;
    }

    private void selectPage(int value) {
        page = value; scroll.reset(); hide();
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean selected, Runnable action) {
        boolean hovered = (editing == null || modalDrawing) && inside(mouseX, mouseY, x, y, width, height) && graphics.containsPointInScissor(mouseX, mouseY);
        UiRenderer.button(graphics, font, x, y, width, height, label, true, selected, hovered, -1);
        actions.add(new Action(x, y, width, height, body, action));
    }

    private net.minecraft.util.FormattedCharSequence trim(Component value, int width) {
        return Language.getInstance().getVisualOrder(font.substrByWidth(value, Math.max(1, width)));
    }

    private UiScrollbar.Track track() {
        return UiScrollbar.layout(x + width - GAP - UiScrollbar.WIDTH, bodyTop, UiScrollbar.WIDTH, visibleHeight(), false,
                visibleHeight(), maxScroll, scroll.current);
    }

    boolean click(double mouseX, double mouseY) {
        if (editing != null) {
            if (renameField != null) renameField.mouseClicked(mouseX, mouseY, 0);
        } else {
            if (page == 1 && search != null) {
                boolean clicked = search.mouseClicked(mouseX, mouseY, 0); search.setFocused(clicked);
                if (clicked) return true;
            }
            if (scrollbar.begin(track(), mouseX, mouseY)) { scroll.set(scrollbar.value(track(), mouseX, mouseY), maxScroll); return true; }
        }
        for (var action : actions) {
            if (action.body && (mouseY < bodyTop || mouseY >= y + height - GAP)) continue;
            if (inside(mouseX, mouseY, action.x, action.y, action.width, action.height)) {
                action.run.run();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
                return true;
            }
        }
        return editing != null;
    }

    boolean scroll(double mouseX, double mouseY, double amount) {
        if (editing != null || !inside(mouseX, mouseY, x, bodyTop, width, visibleHeight())) return false;
        scroll.add(-amount * UiScrollState.WHEEL_STEP, maxScroll); return true;
    }

    boolean drag(double mouseX, double mouseY) {
        if (!scrollbar.dragging()) return false;
        scroll.set(scrollbar.value(track(), mouseX, mouseY), maxScroll); return true;
    }

    void release() { scrollbar.release(); }
    void hide() { release(); if (search != null) search.setFocused(false); editing = null; }

    boolean key(int key, int scan, int modifiers) {
        if (editing != null) {
            if (key == 256) editing = null;
            else if (key == 257 || key == 335) saveName();
            else if (renameField != null) renameField.keyPressed(key, scan, modifiers);
            return true;
        }
        if (key == 256) { if (search != null) search.setFocused(false); return false; }
        if (page != 1 || search == null || !search.isFocused()) return false;
        search.keyPressed(key, scan, modifiers); return true;
    }

    boolean character(char character, int modifiers) {
        if (editing != null) return renameField != null && renameField.charTyped(character, modifiers);
        return page == 1 && search != null && search.isFocused() && search.charTyped(character, modifiers);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Setting(String id, Supplier<Component> value, Runnable change, Runnable reset) {
    }

    private record Action(int x, int y, int width, int height, boolean body, Runnable run) {
    }
}
