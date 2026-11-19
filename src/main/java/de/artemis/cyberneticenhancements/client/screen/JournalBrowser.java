package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.JournalClientState;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import de.artemis.cyberneticenhancements.common.network.JournalRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class JournalBrowser {
    private static final int GAP = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private UiEditBox search;
    private String query = "";
    private boolean newest = true;
    private int category, page, x, y, width, height, max, bodyTop, visible;
    private long request = -1, nextRefresh, changedAt, lastSent;

    private Component text(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.archive.journal." + key, args); }
    private void refresh() {
        if (System.nanoTime() - lastSent < 250_000_000L) { changedAt = lastSent + 250_000_000L; return; }
        lastSent = System.nanoTime();
        request = JournalClientState.nextRequest();
        PacketDistributor.sendToServer(new JournalRequestPayload(request, page, category == 0 ? "" : PlayerJournal.CATEGORIES.get(category - 1), query, newest));
        nextRefresh = System.nanoTime() + 5_000_000_000L;
        changedAt = 0;
    }

    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        if (search == null) {
            search = new UiEditBox(font, x + GAP, y + GAP, 100, 22, text("search"));
            search.setMaxLength(128); search.setHint(text("search"));
            search.setResponder(value -> { query = value; page = 0; scroll.reset(); changedAt = System.nanoTime() + 300_000_000L; });
        }
        if (changedAt > 0 ? System.nanoTime() >= changedAt : System.nanoTime() >= nextRefresh) refresh();
        var snapshot = JournalClientState.get();
        if (snapshot.request() == request && changedAt == 0) page = snapshot.page();
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);
        int typeWidth = Math.min(174, width / 3);
        search.setX(x + GAP); search.setY(y + GAP); search.setWidth(width - GAP * 3 - typeWidth); search.render(g, mx, my, 0);
        button(g, font, x + width - GAP - typeWidth, y + GAP, typeWidth, text("type", category == 0 ? text("all") : category(PlayerJournal.CATEGORIES.get(category - 1))), mx, my, true);
        button(g, font, x + GAP, y + 42, 130, text(newest ? "newest" : "oldest"), mx, my, true);
        var count = text("count", snapshot.total());
        g.drawString(font, count, x + width - GAP - font.width(count), y + 49, UiRenderer.TEXT_SECONDARY, false);
        bodyTop = y + 74; visible = Math.max(1, height - 114);
        int contentWidth = ArchiveContentLayout.contentWidth(width);
        int total = contentHeight(font, contentWidth);
        if (total > visible) { contentWidth = ArchiveContentLayout.contentWidth(width, true); total = contentHeight(font, contentWidth); }
        max = Math.max(0, total - visible); scroll.clamp(max); scroll.tick();
        int left = ArchiveContentLayout.contentX(x), rowY = bodyTop - (int) Math.round(scroll.current);
        g.enableScissor(left, bodyTop, left + contentWidth, bodyTop + visible);
        if (snapshot.entries().isEmpty()) {
            UiRenderer.panel(g, left, bodyTop, contentWidth, 74, UiRenderer.ROW);
            g.drawCenteredString(font, text(snapshot.request() < 0 ? "loading" : query.isBlank() && category == 0 ? "empty" : "no_matches"), left + contentWidth / 2, bodyTop + 30, UiRenderer.TEXT_SECONDARY);
        }
        for (var entry : snapshot.entries()) {
            var lines = font.split(entry.message(), contentWidth - 20);
            int rowHeight = 48 + lines.size() * 11;
            if (rowY + rowHeight > bodyTop && rowY < bodyTop + visible) {
                UiRenderer.panel(g, left, rowY, contentWidth, rowHeight, UiRenderer.ROW);
                g.fill(left, rowY + 8, left + 2, rowY + rowHeight - 8, entry.category().equals("combat") ? 0xFFB3786B : UiRenderer.SELECTED_EDGE);
                long clock = Math.floorMod(entry.dayTime() + 6000, 24000);
                String hour = String.format(java.util.Locale.ROOT, "%02d:%02d", clock / 1000, clock % 1000 * 60 / 1000);
                g.drawString(font, text("time", entry.dayTime() / 24000 + 1, hour, DATE.format(Instant.ofEpochMilli(entry.timestamp()))), left + 10, rowY + 9, UiRenderer.TEXT_SECONDARY, false);
                int lineY = rowY + 24;
                for (var line : lines) { g.drawString(font, line, left + 10, lineY, UiRenderer.TEXT, false); lineY += 11; }
                var location = entry.dimension().isEmpty() ? text("location_unknown", category(entry.category())) : text("location", category(entry.category()), entry.pos().toShortString(), entry.dimension());
                g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(location, contentWidth - 20)), left + 10, lineY + 8, UiRenderer.TEXT_SECONDARY, false);
            }
            rowY += rowHeight + GAP;
        }
        g.disableScissor();
        UiScrollbar.draw(g, track(), mx, my, scrollbar.dragging());
        int footer = y + height - 30;
        button(g, font, x + GAP, footer, 86, text("previous"), mx, my, page > 0);
        button(g, font, x + width - GAP - 86, footer, 86, text("next"), mx, my, (page + 1L) * PlayerJournal.PAGE_SIZE < snapshot.total());
        g.drawCenteredString(font, text("page", page + 1, Math.max(1, (snapshot.total() + PlayerJournal.PAGE_SIZE - 1) / PlayerJournal.PAGE_SIZE)), x + width / 2, footer + 7, UiRenderer.TEXT_SECONDARY);
    }

    private Component category(String id) { return text("category." + id); }
    private int contentHeight(Font font, int contentWidth) {
        return JournalClientState.get().entries().stream().mapToInt(entry -> 58 + font.split(entry.message(), contentWidth - 20).size() * 11).sum() - (JournalClientState.get().entries().isEmpty() ? 0 : GAP);
    }
    private void button(GuiGraphics g, Font font, int x, int y, int width, Component label, int mx, int my, boolean enabled) {
        UiRenderer.button(g, font, x, y, width, 22, label, enabled, false, inside(mx, my, x, y, width, 22), -1);
    }
    private UiScrollbar.Track track() { return UiScrollbar.layout(x + width - GAP - UiScrollbar.WIDTH, bodyTop, UiScrollbar.WIDTH, visible, false, visible, max, scroll.current); }
    boolean click(double mx, double my) {
        if (search != null) { boolean clicked = search.mouseClicked(mx, my, 0); search.setFocused(clicked); if (clicked) return true; }
        if (scrollbar.begin(track(), mx, my)) { scroll.set(scrollbar.value(track(), mx, my), max); return true; }
        boolean change = false;
        int typeWidth = Math.min(174, width / 3);
        if (inside(mx, my, x + width - GAP - typeWidth, y + GAP, typeWidth, 22)) { category = (category + 1) % (PlayerJournal.CATEGORIES.size() + 1); page = 0; change = true; }
        else if (inside(mx, my, x + GAP, y + 42, 130, 22)) { newest = !newest; page = 0; change = true; }
        else if (page > 0 && inside(mx, my, x + GAP, y + height - 30, 86, 22)) { page--; change = true; }
        else if ((page + 1L) * PlayerJournal.PAGE_SIZE < JournalClientState.get().total() && inside(mx, my, x + width - GAP - 86, y + height - 30, 86, 22)) { page++; change = true; }
        if (change) {
            scroll.reset(); refresh();
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
        }
        return change;
    }
    boolean drag(double mx, double my) { if (!scrollbar.dragging()) return false; scroll.set(scrollbar.value(track(), mx, my), max); return true; }
    boolean scroll(double mx, double my, double amount) { if (!inside(mx, my, x, bodyTop, width, visible)) return false; scroll.add(-amount * UiScrollState.WHEEL_STEP, max); return true; }
    void release() { scrollbar.release(); }
    void hide() { release(); if (search != null) search.setFocused(false); nextRefresh = 0; }
    boolean key(int key, int scan, int modifiers) {
        if (search == null || !search.isFocused()) return false;
        if (key == 256) { search.setFocused(false); return false; }
        search.keyPressed(key, scan, modifiers); return true;
    }
    boolean character(char c, int modifiers) { return search != null && search.isFocused() && search.charTyped(c, modifiers); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
}
