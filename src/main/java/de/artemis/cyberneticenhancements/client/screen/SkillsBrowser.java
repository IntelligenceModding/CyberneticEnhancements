package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.ArchiveContentLayout;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import de.artemis.cyberneticenhancements.common.config.SkillsConfig;
import de.artemis.cyberneticenhancements.common.dialogue.SkillChecks;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.origin.Origins;
import de.artemis.cyberneticenhancements.common.skills.Skill;
import de.artemis.cyberneticenhancements.common.skills.SkillEffect;
import de.artemis.cyberneticenhancements.common.skills.Skills;
import de.artemis.cyberneticenhancements.common.skills.SkillsActionPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

final class SkillsBrowser {
    private static final int GAP = ArchiveContentLayout.GAP;
    private static final int BUTTON_HEIGHT = 22;
    private static final int ROW_HEIGHT = 28;
    private static final int GROUP_HEADER = 28;
    private static final int PLUS_WIDTH = 28;
    private static final int RANK_FILL_MS = 260;
    private static final int PURCHASED_FILL = UiRenderer.blend(UiRenderer.PANEL_EDGE_ACTIVE, UiRenderer.ACCENT, 0.65F);
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final EnumMap<Skill, ScreenRectangle> rows = new EnumMap<>(Skill.class);
    private final EnumMap<Skill, RankFill> rankFills = new EnumMap<>(Skill.class);
    private Skill expandedSkill;

    private record RankFill(float from, float target, long start) {
        float value(long now) {
            float remaining = 1.0F - Math.clamp((now - start) / (float) RANK_FILL_MS, 0.0F, 1.0F);
            return target + (from - target) * remaining * remaining * remaining;
        }
    }
    private ScreenRectangle respecButton;
    private ScreenRectangle viewport;
    private UiScrollbar.Track track;
    private int maxScroll;
    private Component tooltip;

    /** What the hovered rank button says, drawn by the screen on top of everything else; null when there is none. */
    Component tooltip() {
        return tooltip;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.skills." + key, args);
    }

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        rows.clear();
        respecButton = null;
        viewport = null;
        track = null;
        tooltip = null;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.PANEL);
        int left = ArchiveContentLayout.contentX(x);
        int contentWidth = ArchiveContentLayout.contentWidth(width, true);
        int top = y + GAP;
        int bottom = y + height - GAP;

        int listTop = renderSummary(graphics, font, player, left, top, contentWidth, mouseX, mouseY) + GAP;
        int visible = bottom - listTop;
        if (visible <= 0) return;
        viewport = new ScreenRectangle(left, listTop, contentWidth, visible);
        scroll.clamp(maxScroll);
        scroll.tick();
        int start = listTop - (int) Math.round(scroll.current);
        int rowY = start;
        long now = Util.getMillis();
        graphics.enableScissor(left, listTop, left + contentWidth, bottom);
        for (Skill.Group group : Skill.Group.values()) {
            var skills = Arrays.stream(Skill.values()).filter(skill -> skill.group() == group).toList();
            int innerWidth = contentWidth - GAP * 2;
            int rank = expandedSkill == null ? 0 : Skills.rank(player, expandedSkill);
            var details = expandedSkill != null && expandedSkill.group() == group
                    ? detailLines(font, expandedSkill, rank, Skills.originRank(player, expandedSkill), Math.max(1, innerWidth - 24))
                    : List.<FormattedCharSequence>of();
            int detailsHeight = details.isEmpty() ? 0 : 14 + details.size() * (font.lineHeight + 2);
            int groupHeight = GROUP_HEADER + skills.size() * ROW_HEIGHT + detailsHeight + 6;
            UiRenderer.panel(graphics, left, rowY, contentWidth, groupHeight, UiRenderer.ROW);
            graphics.drawString(font, group.title(), left + GAP, rowY + 10, UiRenderer.ACCENT, false);
            int groupRanks = skills.stream().mapToInt(skill -> Skills.rank(player, skill)).sum();
            String groupTotal = groupRanks + "/" + skills.size() * SkillsConfig.maxRank();
            graphics.drawString(font, groupTotal, left + contentWidth - GAP - font.width(groupTotal), rowY + 10,
                    UiRenderer.TEXT_SECONDARY, false);
            rowY += GROUP_HEADER;
            for (Skill skill : skills) {
                var row = new ScreenRectangle(left + GAP, rowY, innerWidth, ROW_HEIGHT - 2);
                rows.put(skill, row);
                renderRow(graphics, font, player, skill, row, mouseX, mouseY, now);
                rowY += ROW_HEIGHT;
                if (skill == expandedSkill) {
                    UiRenderer.panel(graphics, row.left(), rowY, row.width(), detailsHeight - 2, UiRenderer.FIELD);
                    int lineY = rowY + 6;
                    for (var line : details) {
                        graphics.drawString(font, line, row.left() + 12, lineY, UiRenderer.TEXT_SECONDARY, false);
                        lineY += font.lineHeight + 2;
                    }
                    rowY += detailsHeight;
                }
            }
            rowY += GAP + 6;
        }
        rowY = renderOverview(graphics, font, left, rowY, contentWidth, text("combined_buffs"),
                combinedBuffs(player), text("no_buffs")) + GAP;
        rowY = renderOverview(graphics, font, left, rowY, contentWidth, text("special_behaviour"),
                specialBehaviour(player), text("no_special_behaviour")) + GAP;
        graphics.disableScissor();
        maxScroll = Math.max(0, rowY - start - GAP - visible);
        track = UiScrollbar.layout(ArchiveContentLayout.scrollbarX(x, width), listTop, UiScrollbar.WIDTH,
                visible, false, visible, maxScroll, scroll.current);
        UiScrollbar.draw(graphics, track, mouseX, mouseY, scrollbar.dragging());
    }

    private int renderSummary(GuiGraphics graphics, Font font, Player player, int left, int top, int width,
                              int mouseX, int mouseY) {
        boolean enabled = SkillsConfig.enabled();
        Component summary = enabled ? text("points", Skills.available(player), Skills.spent(player)) : text("disabled");
        Component reset = text("respec", Skills.respecCost(player));
        boolean showReset = enabled && SkillsConfig.respecEnabled();
        int innerWidth = Math.max(1, width - GAP * 2);
        int buttonWidth = Math.min(innerWidth, font.width(reset) + 18);
        boolean sameLine = showReset && font.width(summary) + GAP + buttonWidth <= innerWidth;
        int summaryWidth = sameLine ? innerWidth - buttonWidth - GAP : innerWidth;
        int summaryHeight = font.split(summary, summaryWidth).size() * (font.lineHeight + 2);
        Component progression = Origins.chosen(player) ? text("origin_progression") : text("progression");
        int progressionOffset = sameLine ? Math.max(summaryHeight + 3, BUTTON_HEIGHT - 2) : summaryHeight + 3;
        int textHeight = enabled ? progressionOffset + font.split(progression, innerWidth).size() * (font.lineHeight + 2)
                : summaryHeight;
        int bodyHeight = showReset && !sameLine ? textHeight + 5 + BUTTON_HEIGHT : Math.max(textHeight, BUTTON_HEIGHT);
        int height = bodyHeight + GAP * 2;
        UiRenderer.panel(graphics, left, top, width, height, UiRenderer.ROW);
        graphics.fill(left, top + 6, left + 2, top + height - 6, UiRenderer.ACCENT);
        drawLines(graphics, font, summary, left + GAP, top + GAP, summaryWidth, UiRenderer.ACCENT);
        if (enabled) drawLines(graphics, font, progression, left + GAP, top + GAP + progressionOffset,
                innerWidth, UiRenderer.TEXT_SECONDARY);
        if (showReset) {
            int buttonX = sameLine ? left + width - GAP - buttonWidth : left + GAP;
            int buttonY = sameLine ? top + 5 : top + GAP + textHeight + 5;
            var bounds = new ScreenRectangle(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
            boolean canReset = Skills.canRespec(player);
            drawButton(graphics, font, bounds, reset, canReset, mouseX, mouseY);
            if (canReset) respecButton = bounds;
        }
        return top + height;
    }

    private void renderRow(GuiGraphics graphics, Font font, Player player, Skill skill, ScreenRectangle row,
                           int mouseX, int mouseY, long now) {
        boolean hovered = viewport.containsPoint(mouseX, mouseY) && row.containsPoint(mouseX, mouseY);
        boolean selected = skill == expandedSkill;
        int fill = selected || hovered ? UiRenderer.ROW_SELECTED : UiRenderer.FIELD;
        UiRenderer.panel(graphics, row.left(), row.top(), row.width(), row.height(), fill);
        int centerY = row.top() + row.height() / 2;
        UiRenderer.disclosureChevron(graphics, row.left() + 10, centerY, selected ? 1 : 0, UiRenderer.TEXT_SECONDARY);

        var plus = plusBounds(row);
        int rank = Skills.rank(player, skill);
        int cap = SkillsConfig.maxRank();
        String rankLabel = rank + "/" + cap;
        int rankRight = plus.left() - GAP;
        int rankColumnX = rankRight - font.width(cap + "/" + cap);
        int nameX = row.left() + 21;
        int textY = centerY - font.lineHeight / 2;
        int nameAndBarWidth = Math.max(0, rankColumnX - nameX - GAP);
        int nameWidth = Math.min(150, nameAndBarWidth * 65 / 100);
        graphics.drawString(font, font.plainSubstrByWidth(skill.title().getString(), nameWidth),
                nameX, textY, UiRenderer.TEXT, false);
        int barX = nameX + nameWidth + GAP;
        drawRankBar(graphics, barX, centerY - 4, rankColumnX - GAP - barX, skill, rank, cap, Skills.originRank(player, skill), now);
        graphics.drawString(font, rankLabel, rankRight - font.width(rankLabel), textY,
                rank == cap ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
        boolean canSpend = Skills.canSpend(player, skill);
        UiRenderer.control(graphics, plus.left(), plus.top(), plus.width(), plus.height(), canSpend, canSpend,
                hovered && plus.containsPoint(mouseX, mouseY), -1);
        if (hovered && plus.containsPoint(mouseX, mouseY) && SkillsConfig.enabled()) {
            tooltip = text(canSpend ? "rank_up" : rank >= cap ? "max_rank" : "no_points");
        }
        graphics.pose().pushPose();
        graphics.pose().translate(plus.left() + plus.width() / 2.0F, centerY, 0);
        graphics.pose().scale(2, 2, 1);
        graphics.drawString(font, "+", -font.width("+") / 2.0F, -font.lineHeight / 2.0F,
                canSpend ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
        graphics.pose().popPose();
    }

    private void drawRankBar(GuiGraphics graphics, int x, int y, int width, Skill skill, int rank, int cap,
                             int originRank, long now) {
        if (width < 4) return;
        float target = rank / (float) cap;
        RankFill fill = rankFills.computeIfAbsent(skill, ignored -> new RankFill(target, target, now));
        if (fill.target() != target) {
            fill = new RankFill(fill.value(now), target, now);
            rankFills.put(skill, fill);
        }

        graphics.fill(x, y, x + width, y + 8, UiRenderer.FIELD_EDGE);
        int innerWidth = width - 2;
        graphics.fill(x + 1, y + 1, x + width - 1, y + 7, UiRenderer.PANEL);
        int filled = Math.round(innerWidth * fill.value(now));
        if (filled > 0) {
            int start = x + 1;
            int originFill = Math.min(filled, Math.round(innerWidth * originRank / (float) cap));
            if (originFill > 0) graphics.fill(start, y + 1, start + originFill, y + 7, UiRenderer.PANEL_EDGE_ACTIVE);
            if (filled > originFill) graphics.fill(start + originFill, y + 1, start + filled, y + 7, PURCHASED_FILL);
            graphics.fill(start, y + 1, start + filled, y + 2, UiRenderer.ACCENT);
        }
        if (innerWidth >= cap * 5) {
            for (int step = 1; step < cap; step++) {
                int notch = x + 1 + innerWidth * step / cap;
                int notchColor = notch < x + 1 + filled ? UiRenderer.PANEL : UiRenderer.FIELD_EDGE;
                graphics.fill(notch, y + 2, notch + 1, y + 7, notchColor);
            }
        }
    }

    private static List<Component> combinedBuffs(Player player) {
        var lines = new ArrayList<Component>();
        for (SkillEffect effect : SkillEffect.values()) {
            double amount = effect.activeBonus(player);
            if (amount > 0) lines.add(effect.describe(amount));
        }
        return lines;
    }

    private static List<Component> specialBehaviour(Player player) {
        var lines = new ArrayList<Component>();
        for (NpcType type : List.of(NpcType.FIXER, NpcType.RIPPERDOC)) {
            for (Skill skill : SkillChecks.unlockedSkills(type, player)) {
                lines.add(text("dialogue_unlock", type.displayName(), skill.title()));
            }
        }
        if (Skills.percent(player, SkillEffect.MATERIAL_REFUND) > 0) lines.add(text("special_refund"));
        if (Skills.percent(player, SkillEffect.CACHE_QUALITY) > 0) lines.add(text("special_cache"));
        return lines;
    }

    private static int renderOverview(GuiGraphics graphics, Font font, int x, int y, int width,
                                      Component title, List<Component> entries, Component empty) {
        int lineWidth = Math.max(1, width - GAP * 2 - 16);
        List<Component> visible = entries.isEmpty() ? List.of(empty) : entries;
        var rows = visible.stream().map(entry -> font.split(entry, lineWidth)).toList();
        int lineHeight = font.lineHeight + 2;
        int contentHeight = rows.stream().mapToInt(row -> row.size() * lineHeight + 8).sum();
        int height = GROUP_HEADER + contentHeight + 6;
        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.ROW);
        graphics.drawString(font, title, x + GAP, y + 10, UiRenderer.ACCENT, false);
        UiRenderer.panel(graphics, x + GAP, y + GROUP_HEADER, width - GAP * 2, contentHeight - 2, UiRenderer.FIELD);
        int lineY = y + GROUP_HEADER + 4;
        for (var row : rows) {
            for (var line : row) {
                graphics.drawString(font, line, x + GAP + 8, lineY, UiRenderer.TEXT_SECONDARY, false);
                lineY += lineHeight;
            }
            lineY += 8;
        }
        return y + height;
    }

    private static ScreenRectangle plusBounds(ScreenRectangle row) {
        return new ScreenRectangle(row.right() - PLUS_WIDTH - 2, row.top() + 2, PLUS_WIDTH, BUTTON_HEIGHT);
    }

    private static List<FormattedCharSequence> detailLines(Font font, Skill skill, int rank, int originRank, int width) {
        var lines = new ArrayList<>(font.split(skill.description(), width));
        if (originRank > 0) lines.addAll(font.split(text("origin_ranks", originRank), width));
        for (var effect : skill.effects()) {
            Component value = effect.describe(effect.at(rank));
            if (rank < SkillsConfig.maxRank()) value = text("effect_next", value, effect.describe(effect.at(rank + 1)));
            lines.addAll(font.split(value, width));
        }
        return lines;
    }

    private static void drawLines(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 2;
        }
    }

    private static void drawButton(GuiGraphics graphics, Font font, ScreenRectangle bounds, Component label,
                                   boolean enabled, int mouseX, int mouseY) {
        UiRenderer.button(graphics, font, bounds.left(), bounds.top(), bounds.width(), bounds.height(),
                label, enabled, false, bounds.containsPoint(mouseX, mouseY), -1);
    }

    boolean click(double x, double y, Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return false;
        if (track != null && scrollbar.begin(track, x, y)) return drag(x, y);
        if (respecButton != null && respecButton.containsPoint((int) x, (int) y) && Skills.canRespec(player)) {
            playClick(minecraft);
            int cost = Skills.respecCost(player);
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed && minecraft.player == player && minecraft.getConnection() != null)
                    PacketDistributor.sendToServer(new SkillsActionPayload(SkillsActionPayload.RESPEC, cost));
                minecraft.setScreen(parent);
            }, text("respec_title"), text("respec_confirm", cost)));
            return true;
        }
        if (viewport == null || !viewport.containsPoint((int) x, (int) y)) return false;
        for (var entry : rows.entrySet()) {
            var row = entry.getValue();
            if (!row.containsPoint((int) x, (int) y)) continue;
            Skill skill = entry.getKey();
            if (plusBounds(row).containsPoint((int) x, (int) y)) {
                if (Skills.canSpend(player, skill)) {
                    playClick(minecraft);
                    PacketDistributor.sendToServer(new SkillsActionPayload(skill.ordinal(), 0));
                }
            } else {
                playClick(minecraft);
                expandedSkill = expandedSkill == skill ? null : skill;
            }
            return true;
        }
        return false;
    }

    private static void playClick(Minecraft minecraft) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    boolean scroll(double x, double y, double delta) {
        if (track == null || !(track.contains(x, y) || viewport.containsPoint((int) x, (int) y))) return false;
        scroll.add(-delta * UiScrollState.WHEEL_STEP, maxScroll);
        return true;
    }

    boolean drag(double x, double y) {
        if (track == null || !scrollbar.dragging()) return false;
        scroll.set(scrollbar.value(track, x, y), maxScroll);
        return true;
    }

    void release() {
        scrollbar.release();
    }
}
