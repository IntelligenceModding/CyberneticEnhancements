package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows what a {@code #name} typed in the chat box will link to, the way Discord does: a list of matching articles above
 * the box, or a note when nothing matches. Tab or a click completes the highlighted one; Up and Down choose.
 */
public final class WikiLinkPreview {
    private static final Pattern TOKEN = Pattern.compile("(?<![\\w#&/])#([A-Za-z0-9_]*)(?:/([A-Za-z0-9_]*))?$");
    private static final int MAX_ROWS = 6, ROW = 12, PAD = 4;
    private static EditBox input;
    private static int selected;
    private static String lastToken = "";
    private static State state;

    private WikiLinkPreview() {
    }

    /** What the player is typing right now and what it would complete to. */
    private record State(int start, int end, boolean section, String typed, List<WikiLinks.Suggestion> rows, Component opens, boolean empty) {
    }

    public static void onInit(ScreenEvent.Init.Post event) {
        input = null;
        state = null;
        if (!(event.getScreen() instanceof ChatScreen)) return;
        for (var child : event.getScreen().children()) if (child instanceof EditBox box) input = box;
    }

    private static State read() {
        if (input == null) return null;
        String value = input.getValue();
        int cursor = Math.min(input.getCursorPosition(), value.length());
        if (value.startsWith("/")) return null;
        Matcher matcher = TOKEN.matcher(value.substring(0, cursor));
        if (!matcher.find()) return null;
        String name = matcher.group(1), part = matcher.group(2);
        if (part != null) {
            var rows = WikiLinks.suggestSections(name, part);
            return new State(matcher.start(2), cursor, true, name, rows, WikiLinks.describe(name, part), rows.isEmpty());
        }
        var rows = WikiLinks.suggest(name, MAX_ROWS);
        return new State(matcher.start(1), cursor, false, name, rows, WikiLinks.describe(name, null), rows.isEmpty());
    }

    /** Left, top, width and height of the list, sitting directly on top of the chat box. */
    private static int[] box(ScreenEvent event) {
        int screenHeight = event.getScreen().height;
        int x = input.getX() - 2, width = Math.min(270, event.getScreen().width - x - 4);
        int height = Math.max(1, state.rows.size()) * ROW + PAD * 2 + (state.opens != null && !state.section && !state.empty ? ROW : 0);
        int bottom = screenHeight - 16;
        return new int[]{x, bottom - height, width, height};
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen) || input == null) return;
        state = read();
        if (state == null) return;
        String token = state.start + ":" + state.typed + ":" + state.rows.size();
        if (!token.equals(lastToken)) { selected = 0; lastToken = token; }
        selected = Math.clamp(selected, 0, Math.max(0, state.rows.size() - 1));

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int[] box = box(event);
        int x = box[0], top = box[1], width = box[2], height = box[3];
        // Chat lines are drawn at a raised depth, so the list has to sit above them to cover them.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        UiRenderer.panel(graphics, x, top, width, height, 0xFF181F28, UiRenderer.PANEL_EDGE_ACTIVE);
        int y = top + PAD;
        if (state.empty) {
            graphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.link.none"), x + PAD + 4, y + 2, UiRenderer.WARNING, false);
        }
        for (int i = 0; i < state.rows.size(); i++) {
            var row = state.rows.get(i);
            if (i == selected) {
                graphics.fill(x + 2, y, x + width - 2, y + ROW, UiRenderer.ROW_SELECTED);
                graphics.fill(x + 2, y, x + 4, y + ROW, UiRenderer.ACCENT);
            }
            String kind = font.plainSubstrByWidth(row.kind().getString(), 90);
            graphics.drawString(font, font.plainSubstrByWidth(row.title().getString(), width - font.width(kind) - 24), x + PAD + 4, y + 2, i == selected ? UiRenderer.TEXT : 0xFFD3DCE4, false);
            graphics.drawString(font, kind, x + width - PAD - 4 - font.width(kind), y + 2, UiRenderer.TEXT_SECONDARY, false);
            y += ROW;
        }
        if (state.opens != null && !state.section && !state.empty) {
            graphics.fill(x + 2, y, x + width - 2, y + 1, UiRenderer.PANEL_EDGE);
            graphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.link.opens", state.opens), x + PAD + 4, y + 3, UiRenderer.ACCENT, false);
        }
        graphics.pose().popPose();
    }

    private static boolean open() {
        return state != null && !state.rows.isEmpty() && input != null && Minecraft.getInstance().screen instanceof ChatScreen;
    }

    private static void complete(int index) {
        var row = state.rows.get(index);
        String value = input.getValue();
        input.setValue(value.substring(0, state.start) + row.insert() + value.substring(state.end));
        input.setCursorPosition(state.start + row.insert().length());
        input.setHighlightPos(input.getCursorPosition());
    }

    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!open()) return;
        int key = event.getKeyCode();
        if (key == InputConstants.KEY_TAB) {
            complete(selected);
        } else if (key == InputConstants.KEY_UP) {
            selected = (selected + state.rows.size() - 1) % state.rows.size();
        } else if (key == InputConstants.KEY_DOWN) {
            selected = (selected + 1) % state.rows.size();
        } else {
            return;
        }
        event.setCanceled(true);
    }

    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!open() || event.getButton() != 0) return;
        int[] box = box(event);
        double mouseX = event.getMouseX(), mouseY = event.getMouseY();
        if (mouseX < box[0] || mouseX >= box[0] + box[2]) return;
        int index = (int) Math.floor((mouseY - box[1] - PAD) / ROW);
        if (mouseY < box[1] + PAD || index < 0 || index >= state.rows.size()) return;
        complete(index);
        event.setCanceled(true);
    }
}
