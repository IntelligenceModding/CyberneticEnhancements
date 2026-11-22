package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.StreetCredBoardClient;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCredBoardPayload;
import de.artemis.cyberneticenhancements.common.origin.Origin;
import de.artemis.cyberneticenhancements.common.origin.Origins;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCred;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The Reputation tab: where the player stands in Street Cred, and the ladder of ranks above and below. */
final class ReputationBrowser {
    private static final int GAP = 10, CARD = 104, ROW = 22, HEAD = 34;
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private int x, y, width, height, max;

    private static Component text(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.archive.reputation." + key, args); }

    private int bodyTop() { return y + GAP; }
    private int visible() { return Math.max(1, height - GAP * 2); }
    private int tiers() { return StreetCred.MAX_LEVEL / 10 + 1; }
    /** A section is a card with its title inside and its rows on a darker backdrop, as the banking tab lays out its panels. */
    private static final int PERKS = 4;
    private static int sectionHeight(int rows) { return HEAD + rows * ROW + 8; }
    private int boardHeight(StreetCredBoardPayload board) {
        int rows = board == null ? 1 : Math.max(1, board.rows().size());
        return sectionHeight(rows + (board != null && board.own() > board.rows().size() ? 1 : 0));
    }
    private int contentHeight(boolean streetKid, StreetCredBoardPayload board) { return CARD + GAP + sectionHeight(PERKS) + GAP + boardHeight(board) + GAP + sectionHeight(tiers()) + (streetKid ? GAP + 40 : 0); }

    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int points = StreetCred.points(player), level = StreetCred.levelFor(points);
        boolean top = level >= StreetCred.MAX_LEVEL, streetKid = Origins.of(player) == Origin.STREET_KID;
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);

        int contentWidth = ArchiveContentLayout.contentWidth(width);
        var board = StreetCredBoardClient.board();
        int total = contentHeight(streetKid, board);
        if (total > visible()) contentWidth = ArchiveContentLayout.contentWidth(width, true);
        max = Math.max(0, total - visible());
        scroll.clamp(max); scroll.tick();
        int left = ArchiveContentLayout.contentX(x), rowY = bodyTop() - (int) Math.round(scroll.current);
        g.enableScissor(left, bodyTop(), left + contentWidth, bodyTop() + visible());

        // The card: rank, level and progress to the next level.
        UiRenderer.panel(g, left, rowY, contentWidth, CARD, UiRenderer.ROW);
        g.fill(left, rowY + 8, left + 2, rowY + CARD - 8, UiRenderer.ACCENT);
        g.drawString(font, text("title"), left + 12, rowY + 10, UiRenderer.ACCENT, false);
        g.pose().pushPose();
        g.pose().translate(left + 12, rowY + 26, 0);
        g.pose().scale(2, 2, 1);
        g.drawString(font, StreetCred.rank(level), 0, 0, UiRenderer.TEXT, false);
        g.pose().popPose();
        Component levelText = text("level", level, StreetCred.MAX_LEVEL);
        g.drawString(font, levelText, left + contentWidth - 12 - font.width(levelText), rowY + 10, UiRenderer.TEXT_SECONDARY, false);
        int barX = left + 12, barY = rowY + 58, barWidth = contentWidth - 24;
        float ratio = top ? 1 : (points - StreetCred.threshold(level)) / (float) StreetCred.cost(level);
        g.fill(barX, barY, barX + barWidth, barY + 6, UiRenderer.FIELD_EDGE);
        g.fill(barX, barY, barX + Math.round(barWidth * Math.clamp(ratio, 0, 1)), barY + 6, UiRenderer.ACCENT);
        Component progress = top ? text("max") : text("progress", points - StreetCred.threshold(level), StreetCred.cost(level));
        g.drawString(font, progress, barX, barY + 11, UiRenderer.TEXT_SECONDARY, false);
        Component next = top ? Component.empty() : text("next_level", StreetCred.threshold(level + 1) - points, level + 1);
        g.drawString(font, next, barX + barWidth - font.width(next), barY + 11, UiRenderer.TEXT_SECONDARY, false);
        Component totalText = text("total", points);
        g.drawString(font, totalText, barX, barY + 25, UiRenderer.TEXT_SECONDARY, false);
        rowY += CARD + GAP;

        // The perks: what the current level is worth right now.
        UiRenderer.panel(g, left, rowY, contentWidth, sectionHeight(PERKS), UiRenderer.ROW);
        g.drawString(font, text("perks"), left + GAP, rowY + 12, UiRenderer.TEXT, false);
        Component[] perks = {text("perk_pay", StreetCredEffects.payBonus(player)), text("perk_trust", StreetCredEffects.trustBonus(player)),
                text("perk_prices", StreetCredEffects.priceDiscount(player)), text("perk_tier", StreetCredEffects.contractTier(player))};
        int perkY = rowY + HEAD;
        for (Component perk : perks) {
            UiRenderer.panel(g, left + GAP, perkY, contentWidth - GAP * 2, ROW - 2, UiRenderer.FIELD);
            g.drawString(font, perk, left + GAP + 12, perkY + 7, UiRenderer.TEXT, false);
            perkY += ROW;
        }
        rowY += sectionHeight(PERKS) + GAP;

        // The leaderboard: the biggest names on the server, with the player's own place if it is further down.
        UiRenderer.panel(g, left, rowY, contentWidth, boardHeight(board), UiRenderer.ROW);
        g.drawString(font, text("leaderboard"), left + GAP, rowY + 12, UiRenderer.TEXT, false);
        if (board != null) {
            Component count = text("board_total", board.total());
            g.drawString(font, count, left + contentWidth - GAP - font.width(count), rowY + 12, UiRenderer.TEXT_SECONDARY, false);
        }
        int inner = left + GAP, innerWidth = contentWidth - GAP * 2;
        int listY = rowY + HEAD;
        rowY += boardHeight(board) + GAP;
        if (board == null || board.rows().isEmpty()) {
            UiRenderer.panel(g, inner, listY, innerWidth, ROW - 2, UiRenderer.FIELD);
            g.drawString(font, text(board == null ? "board_loading" : "board_empty"), inner + 12, listY + 7, UiRenderer.TEXT_SECONDARY, false);
        } else {
            for (int i = 0; i < board.rows().size(); i++) {
                boardRow(g, font, inner, listY, innerWidth, i + 1, board.rows().get(i), board.own() == i + 1);
                listY += ROW;
            }
            if (board.own() > board.rows().size())
                // The player's own place is not in the top, so it is worked out from their points and shown below the list.
                boardRow(g, font, inner, listY, innerWidth, board.own(), new StreetCredBoardPayload.Row(player.getUUID(), player.getGameProfile().getName(), points, ""), true);
        }

        // The ladder: every rank, with the one the player holds picked out.
        UiRenderer.panel(g, left, rowY, contentWidth, sectionHeight(tiers()), UiRenderer.ROW);
        g.drawString(font, text("ranks"), left + GAP, rowY + 12, UiRenderer.TEXT, false);
        int tierY = rowY + HEAD;
        rowY += sectionHeight(tiers());
        int current = level / 10;
        for (int tier = 0; tier < tiers(); tier++) {
            int from = Math.max(1, tier * 10), to = Math.min(StreetCred.MAX_LEVEL, tier * 10 + 9);
            boolean here = tier == current, reached = tier < current;
            UiRenderer.panel(g, inner, tierY, innerWidth, ROW - 2, here ? UiRenderer.ROW_SELECTED : UiRenderer.FIELD);
            if (here) g.fill(inner, tierY + 2, inner + 2, tierY + ROW - 4, UiRenderer.ACCENT);
            int color = here ? UiRenderer.TEXT : reached ? 0xFFD3DCE4 : UiRenderer.TEXT_SECONDARY;
            g.drawString(font, StreetCred.rank(from), inner + 12, tierY + 7, color, false);
            Component range = from == to ? text("range_single", from) : text("range", from, to);
            g.drawString(font, range, inner + innerWidth - 12 - font.width(range), tierY + 7, UiRenderer.TEXT_SECONDARY, false);
            if (here) {
                Component marker = text("current");
                g.drawString(font, marker, inner + innerWidth - 12 - font.width(range) - 14 - font.width(marker), tierY + 7, UiRenderer.ACCENT, false);
            }
            tierY += ROW;
        }
        if (streetKid) {
            rowY += GAP;
            var lines = font.split(text("street_kid", StreetCred.STREET_KID_GAIN), contentWidth - 24);
            UiRenderer.panel(g, left, rowY, contentWidth, 40, UiRenderer.ROW);
            int lineY = rowY + 10;
            for (var line : lines) { g.drawString(font, line, left + 12, lineY, UiRenderer.TEXT_SECONDARY, false); lineY += 11; }
        }
        g.disableScissor();
        UiScrollbar.draw(g, track(), mx, my, scrollbar.dragging());
    }

    /** An online player's own skin if the client has it; otherwise the one the server saved, which the skin manager fetches and caches. */
    private static PlayerSkin skin(StreetCredBoardPayload.Row row) {
        var minecraft = Minecraft.getInstance();
        var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(row.id());
        if (info != null) return info.getSkin();
        GameProfile profile = new GameProfile(row.id(), row.name());
        if (!row.texture().isEmpty()) profile.getProperties().put("textures", new Property("textures", row.texture()));
        return minecraft.getSkinManager().getInsecureSkin(profile);
    }

    private void boardRow(GuiGraphics g, Font font, int left, int rowY, int contentWidth, int place, StreetCredBoardPayload.Row row, boolean own) {
        UiRenderer.panel(g, left, rowY, contentWidth, ROW - 2, own ? UiRenderer.ROW_SELECTED : UiRenderer.FIELD);
        if (own) g.fill(left, rowY + 2, left + 2, rowY + ROW - 4, UiRenderer.ACCENT);
        g.drawString(font, "#" + place, left + 12, rowY + 7, place <= 3 ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
        PlayerFaceRenderer.draw(g, skin(row), left + 38, rowY + 4, 12);
        g.drawString(font, row.name(), left + 56, rowY + 7, own ? UiRenderer.TEXT : 0xFFD3DCE4, false);
        int level = StreetCred.levelFor(row.points());
        Component right = text("board_entry", StreetCred.rank(level), level);
        g.drawString(font, right, left + contentWidth - 12 - font.width(right), rowY + 7, UiRenderer.TEXT_SECONDARY, false);
    }

    private UiScrollbar.Track track() { return UiScrollbar.layout(x + width - GAP - UiScrollbar.WIDTH, bodyTop(), UiScrollbar.WIDTH, visible(), false, visible(), max, scroll.current); }

    boolean click(double mx, double my) {
        if (scrollbar.begin(track(), mx, my)) { scroll.set(scrollbar.value(track(), mx, my), max); return true; }
        return false;
    }
    boolean drag(double mx, double my) { if (!scrollbar.dragging()) return false; scroll.set(scrollbar.value(track(), mx, my), max); return true; }
    boolean scroll(double mx, double my, double amount) {
        if (mx < x || mx >= x + width || my < bodyTop() || my >= bodyTop() + visible()) return false;
        scroll.add(-amount * UiScrollState.WHEEL_STEP, max);
        return true;
    }
    void release() { scrollbar.release(); }
    void hide() { release(); }
}
