package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.GameplayHudVisibility;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Choice;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Context;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Element;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContentLayout;
import de.artemis.cyberneticenhancements.client.ui.UiEditBox;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

final class HudBrowser {
    private static final int GAP = 10, ROW_HEIGHT = 28, ROW_STRIDE = 32;
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final List<Action> actions = new ArrayList<>();
    private Context context = Context.GAMEPLAY;
    private int x, y, width, height, bodyTop, maxScroll;
    private boolean drawingRows;
    private UiEditBox search;
    private String query = "";

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        actions.clear();
        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.PANEL);
        int left = x + GAP;
        int tabX = left;
        Component reset = text("reset");
        int resetWidth = font.width(reset) + 24;
        int searchWidth = Math.max(1, width - GAP * 3 - resetWidth);
        if (search == null) {
            search = new UiEditBox(font, left, y + GAP, searchWidth, 22, text("search"));
            search.setHint(text("search"));
            search.setMaxLength(80);
            search.setResponder(value -> {
                query = value.strip().toLowerCase(Locale.ROOT);
                scroll.reset();
            });
        }
        search.setX(left);
        search.setY(y + GAP);
        search.setWidth(searchWidth);
        search.render(graphics, mouseX, mouseY, 0);
        button(graphics, font, x + width - GAP - resetWidth, y + GAP, resetWidth, 22, reset, false, true, mouseX, mouseY,
                () -> GameplayHudVisibility.reset(context));
        int tabY = y + GAP + 32;
        for (var screen : Context.values()) {
            Component label = label("context.", screen);
            int size = Math.max(64, font.width(label) + 20);
            if (tabX != left && tabX + size > x + width - GAP) {
                tabX = left;
                tabY += 26;
            }
            button(graphics, font, tabX, tabY, size, 22, label, context == screen, true, mouseX, mouseY, () -> {
                context = screen;
                scroll.reset();
                actions.clear();
            });
            tabX += size + 6;
        }

        bodyTop = tabY + 32;
        int bottom = y + height - GAP;
        if (bodyTop >= bottom) return;
        var elements = java.util.Arrays.stream(Element.values())
                .filter(element -> label("element.", element).getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        maxScroll = Math.max(0, elements.size() * ROW_STRIDE - (bottom - bodyTop));
        scroll.clamp(maxScroll);
        scroll.tick();
        int contentWidth = ArchiveContentLayout.contentWidth(width, maxScroll > 0);
        int rowY = bodyTop - (int) Math.round(scroll.current);
        graphics.enableScissor(left, bodyTop, left + contentWidth, bottom);
        drawingRows = true;
        boolean lockedTooltip = false;
        if (elements.isEmpty()) {
            graphics.drawCenteredString(font, text("empty"), left + contentWidth / 2, bodyTop + 16, UiRenderer.TEXT_SECONDARY);
        }
        for (var element : elements) {
            if (rowY + ROW_HEIGHT > bodyTop && rowY < bottom) {
                var choice = GameplayHudVisibility.choice(context, element);
                boolean locked = GameplayHudVisibility.locked(element);
                UiRenderer.panel(graphics, left, rowY, contentWidth, ROW_HEIGHT, UiRenderer.ROW);
                graphics.drawString(font, label("element.", element), left + GAP, rowY + 10,
                        locked ? UiRenderer.TEXT_SECONDARY : UiRenderer.TEXT, false);
                int optionWidth = Math.min(115, (contentWidth - 2 * GAP) / 5);
                for (var option : Choice.values()) {
                    int optionX = left + contentWidth - GAP - (Choice.values().length - option.ordinal()) * (optionWidth + 5) + 5;
                    Component optionLabel = option == Choice.DEFAULT
                            ? text(GameplayHudVisibility.defaultVisible(context, element) ? "default_show" : "default_hide")
                            : label("choice.", option);
                    button(graphics, font, optionX, rowY + 4, optionWidth, 20, optionLabel, choice == option, !locked,
                            mouseX, mouseY, () -> GameplayHudVisibility.set(context, element, option));
                }
                if (locked && mouseX >= left && mouseX < left + contentWidth && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                        && graphics.containsPointInScissor(mouseX, mouseY)) {
                    lockedTooltip = true;
                }
            }
            rowY += ROW_STRIDE;
        }
        drawingRows = false;
        graphics.disableScissor();
        UiScrollbar.draw(graphics, track(), mouseX, mouseY, scrollbar.dragging());
        if (lockedTooltip) graphics.renderTooltip(font, text("locked"), mouseX, mouseY);
    }

    private void button(GuiGraphics graphics, Font font, int x, int y, int width, int height, Component label,
                        boolean selected, boolean enabled, int mouseX, int mouseY, Runnable handler) {
        var action = new Action(x, drawingRows ? Math.max(bodyTop, y) : y, x + width, y + height, handler);
        UiRenderer.button(graphics, font, x, y, width, height, label, enabled, selected && enabled,
                action.contains(mouseX, mouseY) && graphics.containsPointInScissor(mouseX, mouseY), -1);
        if (enabled) actions.add(action);
    }

    boolean click(double mouseX, double mouseY) {
        if (search != null) {
            search.setFocused(search.isMouseOver(mouseX, mouseY));
            if (search.isFocused()) return search.mouseClicked(mouseX, mouseY, 0);
        }
        if (bodyTop < y + height - GAP && scrollbar.begin(track(), mouseX, mouseY)) {
            scroll.set(scrollbar.value(track(), mouseX, mouseY), maxScroll);
            return true;
        }
        if (!contains(mouseX, mouseY)) return false;
        for (var action : actions) {
            if (!action.contains(mouseX, mouseY)) continue;
            action.handler.run();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1));
            return true;
        }
        return false;
    }

    boolean scroll(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY) || mouseY < bodyTop) return false;
        scroll.add(-amount * UiScrollState.WHEEL_STEP, maxScroll);
        return true;
    }

    boolean drag(double mouseX, double mouseY) {
        if (!scrollbar.dragging()) return search != null && search.isFocused() && search.mouseDragged(mouseX, mouseY, 0, 0, 0);
        scroll.set(scrollbar.value(track(), mouseX, mouseY), maxScroll);
        return true;
    }

    void release() { scrollbar.release(); }

    void unfocus() { if (search != null) search.setFocused(false); }

    boolean key(int key, int scanCode, int modifiers) {
        if (search == null || !search.isFocused()) return false;
        if (key == 256) unfocus();
        else search.keyPressed(key, scanCode, modifiers);
        return true;
    }

    boolean character(char value, int modifiers) {
        return search != null && search.isFocused() && search.charTyped(value, modifiers);
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height - GAP;
    }

    private UiScrollbar.Track track() {
        int viewport = Math.max(1, y + height - GAP - bodyTop);
        return UiScrollbar.layout(ArchiveContentLayout.scrollbarX(x, width), bodyTop, UiScrollbar.WIDTH,
                viewport, false, viewport, maxScroll, scroll.current);
    }

    private static Component label(String prefix, Enum<?> value) {
        return text(prefix + value.name().toLowerCase(Locale.ROOT));
    }

    private static Component text(String key) {
        return Component.translatable("screen.cyberneticenhancements.archive.hud." + key);
    }

    private record Action(int left, int top, int right, int bottom, Runnable handler) {
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
}
