package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.tarot.*;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Collection tiles stay visible when locked; artwork is revealed by scanning in-world. */
final class TarotBrowser {
    private TarotCatalog.Card selected;
    private record Tile(TarotCatalog.Card card, int x, int y, int w, int h) {}
    private record Bonus(TarotCatalog.BonusType type, double collected, double total) {}
    private final List<Tile> tiles = new ArrayList<>();
    private int backX, backY, backWidth;
    private int placeX, placeY, placeWidth;
    private boolean placeEnabled;
    private String placementRequest;
    private final UiScrollState descriptionScroll = new UiScrollState();
    private final UiScrollbar descriptionScrollbar = new UiScrollbar();
    private int descriptionX, descriptionY, descriptionWidth, descriptionHeight, descriptionMaxScroll;
    private final UiScrollState gridScroll = new UiScrollState();
    private final UiScrollbar gridScrollbar = new UiScrollbar();
    private int gridX, gridY, gridWidth, gridHeight, gridMaxScroll;
    private final UiScrollState bonusScroll = new UiScrollState();
    private final UiScrollbar bonusScrollbar = new UiScrollbar();
    private int bonusX, bonusY, bonusWidth, bonusHeight, bonusMaxScroll;
    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);
        if (selected != null) { detail(g, font, x, y, width, height, mx, my); return; }
        tiles.clear();
        long collected = TarotCatalog.CARDS.stream().filter(card -> TarotKnowledge.clientKnows(card.id())).count();
        g.drawString(font, Component.translatable("tarot.cyberneticenhancements.collection"), x + 12, y + 12, UiRenderer.ACCENT, true);
        Component count = Component.translatable("tarot.cyberneticenhancements.count", collected, TarotCatalog.CARDS.size());
        g.drawString(font, count, x + width - 12 - font.width(count), y + 12, UiRenderer.TEXT_SECONDARY, true);
        var help = Component.translatable("tarot.cyberneticenhancements.reward_help");
        int contentY = y + 28;
        for (var line : font.split(help, width - 24)) { g.drawString(font, line, x + 12, contentY, UiRenderer.TEXT_SECONDARY, true); contentY += 12; }
        contentY += 10;
        List<Bonus> bonuses = bonuses();
        boolean besideCards = width >= 520;
        int summaryWidth = besideCards ? Math.min(240, width / 3) : width - 24;
        int summaryX = besideCards ? x + width - 12 - summaryWidth : x + 12;
        int availableHeight = Math.max(0, y + height - 12 - contentY);
        int summaryHeight = besideCards ? availableHeight : Math.min(30 + bonuses.size() * 32, Math.max(62, availableHeight / 3));
        renderBonuses(g, font, bonuses, summaryX, contentY, summaryWidth, summaryHeight, mx, my);
        int cardsY = besideCards ? contentY : contentY + summaryHeight + 12;
        int gridRight = besideCards ? summaryX - 12 : x + width - 12;
        gridX = x + 12; gridY = cardsY; gridWidth = Math.max(0, gridRight - gridX); gridHeight = Math.max(0, y + height - 12 - cardsY);
        if (gridHeight == 0 || gridWidth < 70) return;
        int countTiles = TarotCatalog.CARDS.size();
        int contentWidth = Math.max(1, gridWidth - UiScrollbar.WIDTH - 12);
        int viewportHeight = Math.max(1, gridHeight);
        int columns = Math.max(1, Math.min(5, (contentWidth + 12) / (110 + 12)));
        int pageRows = Math.max(1, (Math.min(10, countTiles) + columns - 1) / columns);
        double tileWidth = (contentWidth - (columns - 1) * 12) / (double) columns;
        double tileHeight = (viewportHeight - (pageRows - 1) * 12) / (double) pageRows;
        int rows = (countTiles + columns - 1) / columns;
        gridMaxScroll = Math.max(0, (int) Math.round(rows * tileHeight + (rows - 1) * 12) - viewportHeight);
        gridScroll.clamp(gridMaxScroll); gridScroll.tick();
        g.enableScissor(gridX, gridY, gridRight - 12, gridY + gridHeight + UiRenderer.SHADOW_OFFSET_Y);
        for (int i = 0; i < countTiles; i++) {
            TarotCatalog.Card card = TarotCatalog.CARDS.get(i);
            double columnStart = i % columns * (tileWidth + 12), rowStart = i / columns * (tileHeight + 12);
            int xx = gridX + (int) Math.round(columnStart), yy = gridY + (int) Math.round(rowStart) - (int) Math.round(gridScroll.current);
            int w = (int) Math.round(columnStart + tileWidth) - (int) Math.round(columnStart);
            int h = (int) Math.round(rowStart + tileHeight) - (int) Math.round(rowStart);
            if (yy + h <= gridY || yy >= gridY + gridHeight) continue;
            tiles.add(new Tile(card, xx, yy, w, h));
            boolean known = TarotKnowledge.clientKnows(card.id());
            boolean hovered = insideGrid(mx, my) && mx >= xx && mx < xx + w && my >= yy && my < yy + h;
            UiRenderer.panel(g, xx, yy, w, h, hovered ? UiRenderer.ROW_SELECTED : UiRenderer.ROW);
            if (known) image(g, card, xx + 8, yy + 7, w - 16, h - 32);
            else {
                g.fill(xx + 8, yy + 7, xx + w - 8, yy + h - 25, UiRenderer.FIELD);
                g.drawCenteredString(font, "?", xx + w / 2, yy + h / 2 - 12, UiRenderer.ACCENT);
            }
            g.drawCenteredString(font, font.plainSubstrByWidth(card.name().getString(), w - 10), xx + w / 2, yy + h - 17, known ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY);
        }
        g.disableScissor();
        UiScrollbar.draw(g, gridTrack(), mx, my, gridScrollbar.dragging());
    }
    private boolean insideGrid(double x, double y) { return x >= gridX && x < gridX + gridWidth && y >= gridY && y < gridY + gridHeight; }
    private UiScrollbar.Track gridTrack() {
        return UiScrollbar.layout(gridX + gridWidth - UiScrollbar.WIDTH, gridY, UiScrollbar.WIDTH, gridHeight,
                false, gridHeight, gridMaxScroll, gridScroll.current);
    }
    private List<Bonus> bonuses() {
        List<Bonus> rows = new ArrayList<>();
        for (var type : TarotCatalog.BonusType.values()) {
            double total = TarotCatalog.total(type, id -> true);
            if (total > 0) rows.add(new Bonus(type, TarotCatalog.total(type, TarotKnowledge::clientKnows), total));
        }
        return rows;
    }
    private void renderBonuses(GuiGraphics g, Font font, List<Bonus> bonuses, int x, int y, int w, int h, int mx, int my) {
        UiRenderer.panel(g, x, y, w, h, UiRenderer.ROW);
        g.drawString(font, Component.translatable("tarot.cyberneticenhancements.bonuses"), x + 10, y + 10, UiRenderer.ACCENT, true);
        bonusX = x + 10; bonusY = y + 30; bonusWidth = w - 20; bonusHeight = Math.max(0, h - 30);
        int rowHeight = Math.max(32, bonusHeight / Math.max(1, bonuses.size()));
        bonusMaxScroll = Math.max(0, bonuses.size() * rowHeight - bonusHeight);
        bonusScroll.clamp(bonusMaxScroll); bonusScroll.tick();
        int right = bonusX + bonusWidth - (bonusMaxScroll > 0 ? 14 : 0);
        int rowY = bonusY - (int) Math.round(bonusScroll.current);
        g.enableScissor(bonusX, bonusY, right, bonusY + bonusHeight);
        for (var bonus : bonuses) {
            String value = bonus.type().format(bonus.collected()) + " / " + bonus.type().format(bonus.total());
            g.drawString(font, bonus.type().label(), bonusX, rowY, UiRenderer.TEXT, true);
            g.drawString(font, value, right - font.width(value), rowY, UiRenderer.ACCENT, true);
            int barY = rowY + 16;
            g.fill(bonusX, barY, right, barY + 2, UiRenderer.FIELD_EDGE);
            g.fill(bonusX, barY, bonusX + (int) Math.round((right - bonusX) * Math.min(1, bonus.collected() / bonus.total())), barY + 2, UiRenderer.ACCENT);
            rowY += rowHeight;
        }
        g.disableScissor();
        UiScrollbar.draw(g, bonusTrack(), mx, my, bonusScrollbar.dragging());
    }
    private UiScrollbar.Track bonusTrack() {
        return UiScrollbar.layout(bonusX + bonusWidth - UiScrollbar.WIDTH, bonusY, UiScrollbar.WIDTH, bonusHeight,
                false, bonusHeight, bonusMaxScroll, bonusScroll.current);
    }
    private void detail(GuiGraphics g, Font font, int x, int y, int w, int h, int mx, int my) {
        backX = x + 12; backY = y + 10;
        int frameX = backX, frameY = y + 44, padding = 3;
        int imageX = frameX + padding, imageY = frameY + padding;
        int imageH = h - 68 - padding * 2, imageW = Math.round(imageH * TarotCatalog.ASPECT_RATIO);
        List<FormattedCharSequence> credit = List.of();
        for (int pass = 0; pass < 3; pass++) {
            credit = font.split(Component.translatable("tarot.cyberneticenhancements.art_credit"), imageW);
            imageH = h - 58 - credit.size() * 10 - padding * 2;
            imageW = Math.round(imageH * TarotCatalog.ASPECT_RATIO);
        }
        int frameWidth = imageW + padding * 2, frameHeight = imageH + padding * 2;
        int buttonSpace = frameWidth - 12;
        backWidth = placeWidth = buttonSpace / 2;
        placeX = backX + frameWidth - placeWidth; placeY = backY;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        placeEnabled = TarotKnowledge.clientKnows(selected.id()) && player != null && player.isAlive() && !player.isSpectator() && player.mayBuild();
        UiRenderer.button(g, font, backX, backY, backWidth, 22, Component.translatable("gui.back"), true, false,
                mx >= backX && mx < backX + backWidth && my >= backY && my < backY + 22, UiRenderer.TEXT);
        UiRenderer.button(g, font, placeX, placeY, placeWidth, 22, Component.translatable("tarot.cyberneticenhancements.place_decoration"),
                placeEnabled, false, mx >= placeX && mx < placeX + placeWidth && my >= placeY && my < placeY + 22, UiRenderer.TEXT);
        UiRenderer.panel(g, frameX, frameY, frameWidth, frameHeight, UiRenderer.FIELD, UiRenderer.PANEL_EDGE_ACTIVE);
        boolean known = TarotKnowledge.clientKnows(selected.id());
        if (known) image(g, selected, imageX, imageY, imageW, imageH);
        else {
            g.drawCenteredString(font, "?", imageX + imageW / 2, imageY + imageH / 2, UiRenderer.ACCENT);
        }
        int tx = frameX + frameWidth + 22, tw = Math.max(30, x + w - tx - 16), ty = backY + (22 - font.lineHeight) / 2;
        Component bonus = selected.reward();
        String title = font.plainSubstrByWidth(selected.number() + " // " + selected.name().getString(), Math.max(0, tw - font.width(bonus) - 24));
        g.drawString(font, title, tx, ty, UiRenderer.ACCENT, true);
        g.drawString(font, bonus, tx + font.width(title) + 24, ty, known ? 0xFF88E9B0 : UiRenderer.ACCENT, true);
        ty = frameY + 6;
        renderDescription(g, font, tx, ty, tw, y + h - 12 - ty, mx, my);
        int cy = frameY + frameHeight + 5;
        for (var line : credit) { g.drawCenteredString(font, line, frameX + frameWidth / 2, cy, UiRenderer.TEXT_SECONDARY); cy += 10; }
    }
    private void renderDescription(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        final float scale = 1.25F;
        final int lineStep = 13;
        descriptionX = x; descriptionY = y; descriptionWidth = Math.min(440, width); descriptionHeight = Math.max(0, height);
        int contentWidth = Math.max(1, descriptionWidth - 14);
        var lines = font.split(selected.description(), Math.max(1, (int) (contentWidth / scale)));
        int contentHeight = (int) Math.ceil((Math.max(0, lines.size() - 1) * lineStep + font.lineHeight) * scale);
        descriptionMaxScroll = Math.max(0, contentHeight - descriptionHeight);
        descriptionScroll.clamp(descriptionMaxScroll); descriptionScroll.tick();
        g.enableScissor(x, y, x + contentWidth, y + descriptionHeight);
        g.pose().pushPose();
        g.pose().translate(x, y - descriptionScroll.current, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), 0, i * lineStep, UiRenderer.TEXT, true);
        g.pose().popPose();
        g.disableScissor();
        UiScrollbar.draw(g, descriptionTrack(), mx, my, descriptionScrollbar.dragging());
    }
    private UiScrollbar.Track descriptionTrack() {
        return UiScrollbar.layout(descriptionX + descriptionWidth - UiScrollbar.WIDTH, descriptionY, UiScrollbar.WIDTH,
                descriptionHeight, false, descriptionHeight, descriptionMaxScroll, descriptionScroll.current);
    }
    boolean scroll(double x, double y, double delta) {
        if (selected == null) {
            if (x >= bonusX && x < bonusX + bonusWidth && y >= bonusY && y < bonusY + bonusHeight) {
                bonusScroll.add(-delta * UiScrollState.WHEEL_STEP, bonusMaxScroll); return true;
            }
            if (!insideGrid(x, y)) return false;
            gridScroll.add(-delta * UiScrollState.WHEEL_STEP, gridMaxScroll); return true;
        }
        if (x < descriptionX || x >= descriptionX + descriptionWidth || y < descriptionY || y >= descriptionY + descriptionHeight) return false;
        descriptionScroll.add(-delta * UiScrollState.WHEEL_STEP, descriptionMaxScroll); return true;
    }
    boolean drag(double x, double y) {
        if (selected == null && bonusScrollbar.dragging()) {
            bonusScroll.set(bonusScrollbar.value(bonusTrack(), x, y), bonusMaxScroll); return true;
        }
        if (selected == null && gridScrollbar.dragging()) {
            gridScroll.set(gridScrollbar.value(gridTrack(), x, y), gridMaxScroll); return true;
        }
        if (!descriptionScrollbar.dragging()) return false;
        descriptionScroll.set(descriptionScrollbar.value(descriptionTrack(), x, y), descriptionMaxScroll); return true;
    }
    void release() { descriptionScrollbar.release(); gridScrollbar.release(); bonusScrollbar.release(); }
    String takePlacementRequest() { String id = placementRequest; placementRequest = null; return id; }
    private void image(GuiGraphics g, TarotCatalog.Card card, int x, int y, int w, int h) {
        int fittedW = Math.min(w, Math.round(h * TarotCatalog.ASPECT_RATIO)), fittedH = Math.round(fittedW / TarotCatalog.ASPECT_RATIO);
        g.blit(card.texture(), x + (w - fittedW) / 2, y + (h - fittedH) / 2, fittedW, fittedH, 0, 0,
                TarotCatalog.TEXTURE_WIDTH, TarotCatalog.TEXTURE_HEIGHT, TarotCatalog.TEXTURE_WIDTH, TarotCatalog.TEXTURE_HEIGHT);
    }
    boolean click(double x, double y) {
        if (selected != null) {
            if (x >= backX && x < backX + backWidth && y >= backY && y < backY + 22) { selected = null; descriptionScroll.reset(); release(); return true; }
            if (placeEnabled && x >= placeX && x < placeX + placeWidth && y >= placeY && y < placeY + 22) {
                placementRequest = selected.id(); return true;
            }
            if (descriptionScrollbar.begin(descriptionTrack(), x, y)) {
                descriptionScroll.set(descriptionScrollbar.value(descriptionTrack(), x, y), descriptionMaxScroll); return true;
            }
            return false;
        }
        if (bonusScrollbar.begin(bonusTrack(), x, y)) {
            bonusScroll.set(bonusScrollbar.value(bonusTrack(), x, y), bonusMaxScroll); return true;
        }
        if (gridScrollbar.begin(gridTrack(), x, y)) {
            gridScroll.set(gridScrollbar.value(gridTrack(), x, y), gridMaxScroll); return true;
        }
        if (!insideGrid(x, y)) return false;
        for (Tile tile : tiles) if (x >= tile.x && x < tile.x + tile.w && y >= tile.y && y < tile.y + tile.h) { selected = tile.card; descriptionScroll.reset(); release(); return true; }
        return false;
    }
}
