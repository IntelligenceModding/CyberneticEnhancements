package de.artemis.cyberneticenhancements.client.screen;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContentLayout;

import de.artemis.cyberneticenhancements.client.ArchiveBankingClientState;
import de.artemis.cyberneticenhancements.client.ui.BankingHistory;
import de.artemis.cyberneticenhancements.client.ui.BankingLayout;
import de.artemis.cyberneticenhancements.client.ui.UiEditBox;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingActionPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BankingBrowser {
    private static final int GAP = 10, HEADER = 72, INCOME = UiRenderer.ACCENT, EXPENSE = UiRenderer.WARNING;
    private final UiScrollState[] scrolls = {new UiScrollState(), new UiScrollState(), new UiScrollState()};
    private final UiScrollState pickerScroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar(), pickerScrollbar = new UiScrollbar();
    private final List<Action> actions = new ArrayList<>();
    private UiEditBox search, target, amount;
    private Font font;
    private int x, y, width, height, mouseX, mouseY, page, filter, period = 14, maxScroll, pickerMax;
    private String query = "";
    private boolean pickerOpen, drawingBody;
    private Rect popup, chooser;
    private ArchiveBankingPayload pendingSnapshot;
    private String pendingAction = "";
    private long sentAt;
    private Component feedback = Component.empty();
    private boolean feedbackError;
    private List<Component> tooltip = List.of();

    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        this.font = font; this.x = x; this.y = y; this.width = width; this.height = height; mouseX = mx; mouseY = my;
        initFields();
        actions.clear(); tooltip = List.of(); popup = null; chooser = null;
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);
        var snapshot = ArchiveBankingClientState.get();
        receiveResult(snapshot);
        renderHeader(g, snapshot);
        target.setVisible(page == 0 && ArchiveBankingClientState.isLoaded());
        amount.setVisible(target.isVisible());
        if (!ArchiveBankingClientState.isLoaded()) {
            g.drawCenteredString(font, text("syncing"), x + width / 2, y + HEADER + 24, UiRenderer.TEXT_SECONDARY);
            return;
        }
        int bodyWidth = ArchiveContentLayout.contentWidth(width);
        int visible = Math.max(1, height - HEADER - GAP);
        if (measuredHeight(snapshot, bodyWidth) > visible) bodyWidth = ArchiveContentLayout.contentWidth(width, true);
        maxScroll = Math.max(0, measuredHeight(snapshot, bodyWidth) - visible);
        scrolls[page].clamp(maxScroll); scrolls[page].tick();
        int cy = y + HEADER - (int) Math.round(scrolls[page].current);
        g.enableScissor(ArchiveContentLayout.contentX(x), y + HEADER, ArchiveContentLayout.contentX(x) + bodyWidth + 3, y + height - GAP);
        drawingBody = true;
        int bottom;
        if (page == 0) bottom = overview(g, snapshot, ArchiveContentLayout.contentX(x), cy, bodyWidth);
        else if (page == 1) bottom = activity(g, snapshot.entries(), ArchiveContentLayout.contentX(x), cy, bodyWidth);
        else bottom = requests(g, snapshot, ArchiveContentLayout.contentX(x), cy, bodyWidth);
        drawingBody = false;
        g.disableScissor();
        maxScroll = Math.max(0, bottom - cy - visible);
        scrolls[page].clamp(maxScroll);
        UiScrollbar.draw(g, track(), mx, my, scrollbar.dragging());
        if (pickerOpen && page == 0 && chooser != null) renderPicker(g);
        if (!tooltip.isEmpty() && !pickerOpen) g.renderComponentTooltip(font, tooltip, mx, my);
    }

    private List<ArchiveBankingPayload.LedgerEntry> filteredEntries(List<ArchiveBankingPayload.LedgerEntry> entries) {
        return entries.stream().filter(entry -> (filter == 0 || filter == 1 && entry.amount() > 0 || filter == 2 && entry.amount() < 0)
                && (entry.note() + " " + entry.counterpartyName() + " " + humanize(entry.typeId())).toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private List<ArchiveBankingPayload.RequestEntry> filteredRequests(ArchiveBankingPayload snapshot) {
        return snapshot.requests().stream().filter(request -> (filter == 0 || filter == 1 && request.incoming() || filter == 2 && !request.incoming())
                && (request.fromName() + " " + request.toName()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private int ledgerHeight(ArchiveBankingPayload.LedgerEntry entry, int width) {
        int dateWidth = Math.max(76, font.width(text("day", entry.day() + 1)) + 14);
        int amountWidth = Math.max(110, font.width(signed(entry.amount())) + 20);
        int detailsWidth = Math.max(80, width - dateWidth - amountWidth - GAP * 3);
        Component title = Component.literal(entry.note().isBlank() ? humanize(entry.typeId()) : entry.note());
        Component detail = Component.literal((entry.counterpartyName().isBlank() ? text("system").getString() : entry.counterpartyName()) + " · " + humanize(entry.typeId()));
        return Math.max(48, (font.split(title, detailsWidth).size() + font.split(detail, detailsWidth).size()) * 11 + 24);
    }

    private int measuredHeight(ArchiveBankingPayload snapshot, int width) {
        if (page == 2) {
            int banner = 0;
            if (!pendingAction.isEmpty() || !feedback.getString().isBlank()) {
                Component message = pendingAction.isEmpty() || feedbackError ? feedback : text("processing");
                banner = font.split(message, width - 20).size() * 11 + 30;
            }
            int count = filteredRequests(snapshot).size();
            return banner + (count == 0 ? 74 : ((count + (width >= 700 ? 2 : 1) - 1) / (width >= 700 ? 2 : 1)) * 128 - GAP);
        }
        if (page == 0) return BankingLayout.dashboard(width).bottom();
        var entries = filteredEntries(snapshot.entries());
        int count = entries.size();
        int result = 30;
        if (count == 0) return result + 74;
        for (int index = 0; index < count; index++) result += ledgerHeight(entries.get(index), width) + GAP;
        return result - GAP;
    }

    private void initFields() {
        if (search != null) return;
        search = new UiEditBox(font, 0, 0, 100, 22, text("search"));
        search.setMaxLength(80); search.setHint(text("search"));
        search.setResponder(value -> { query = value.strip().toLowerCase(Locale.ROOT); for (var scroll : scrolls) scroll.reset(); });
        target = new UiEditBox(font, 0, 0, 100, 22, text("recipient"));
        target.setMaxLength(36); target.setHint(text("recipient_hint"));
        amount = new UiEditBox(font, 0, 0, 100, 22, text("amount"));
        amount.setMaxLength(10); amount.setFilter(value -> value.chars().allMatch(Character::isDigit));
        amount.setHint(Component.literal("100"));
        target.setResponder(value -> clearFeedback());
        amount.setResponder(value -> clearFeedback());
    }

    private void renderHeader(GuiGraphics g, ArchiveBankingPayload snapshot) {
        int filterWidth = Math.min(210, Math.max(140, width / 4));
        search.setX(x + GAP); search.setY(y + GAP); search.setWidth(width - GAP * 3 - filterWidth);
        search.render(g, mouseX, mouseY, 0);
        String[] filters = page == 2 ? new String[]{"all_requests", "incoming_requests", "outgoing_requests"} : new String[]{"all_activity", "income", "expense"};
        button(g, new Rect(x + width - GAP - filterWidth, y + GAP, filterWidth, 22), text(filters[filter]), true, filter != 0,
                () -> { filter = (filter + 1) % 3; scrolls[page].reset(); });
        int tabX = x + GAP;
        for (int index = 0; index < 3; index++) {
            final int selected = index;
            Component label = index == 2 ? text("requests_count", snapshot.requests().size()) : text(index == 0 ? "overview" : "activity");
            int tabWidth = Math.max(84, font.width(label) + 24);
            button(g, new Rect(tabX, y + 42, tabWidth, 20), label, true, page == index, () -> switchPage(selected));
            tabX += tabWidth + GAP;
        }
        Component count = text("record_count", snapshot.entries().size());
        if (tabX + font.width(count) + GAP < x + width - GAP) g.drawString(font, count, x + width - GAP - font.width(count), y + 48, UiRenderer.TEXT_SECONDARY, false);
    }

    private int overview(GuiGraphics g, ArchiveBankingPayload snapshot, int cx, int cy, int bodyWidth) {
        var history = BankingHistory.summarize(snapshot.dailyFlow().stream().flatMap(day -> java.util.stream.Stream.of(
                new BankingHistory.Transaction(day.day(), day.income()), new BankingHistory.Transaction(day.day(), -day.expense()))).toList(), snapshot.currentDay(), period);
        var layout = BankingLayout.dashboard(bodyWidth);
        for (int index = 0; index < 4; index++) {
            var box = layout.stats().get(index);
            String label = new String[]{"balance", "income", "expense", "net"}[index];
            long value = index == 0 ? snapshot.balance() : index == 1 ? history.income() : index == 2 ? history.expense() : history.net();
            int color = index == 2 || index == 3 && value < 0 ? EXPENSE : index == 0 ? UiRenderer.TEXT : INCOME;
            stat(g, cx + box.x(), cy + box.y(), box.width(), text(label), value, color, index == 0 ? text("available") : text("period", period), index == 3);
        }
        var chart = layout.graph();
        var form = layout.transfer();
        graph(g, history, snapshot.currentDay(), cx + chart.x(), cy + chart.y(), chart.width(), chart.height());
        transfer(g, snapshot, cx + form.x(), cy + form.y(), form.width(), form.height());
        var balance = layout.balance();
        balanceGraph(g, BankingHistory.balances(history, snapshot.balance()), snapshot.currentDay(),
                cx + balance.x(), cy + balance.y(), balance.width(), balance.height());
        return cy + layout.bottom();
    }

    private void stat(GuiGraphics g, int cx, int cy, int w, Component label, long value, int color, Component caption, boolean sign) {
        UiRenderer.panel(g, cx, cy, w, 74, UiRenderer.ROW);
        g.drawString(font, label, cx + GAP, cy + 10, UiRenderer.TEXT_SECONDARY, false);
        money(g, value, cx + GAP, cy + 28, w - GAP * 2, color, 1.5F, sign);
        g.drawString(font, caption, cx + GAP, cy + 57, UiRenderer.TEXT_SECONDARY, false);
    }

    private void graph(GuiGraphics g, BankingHistory.Summary history, long today, int cx, int cy, int w, int h) {
        UiRenderer.panel(g, cx, cy, w, h, UiRenderer.ROW);
        g.drawString(font, text("cash_flow"), cx + GAP, cy + 12, UiRenderer.TEXT, false);
        int[] periods = {7, 14, 30};
        for (int index = 0; index < periods.length; index++) {
            int days = periods[index];
            button(g, new Rect(cx + w - 130 + index * 40, cy + 7, 34, 20), text("days_short", days), true, period == days, () -> period = days);
        }
        g.fill(cx + GAP, cy + 36, cx + GAP + 7, cy + 43, INCOME);
        g.drawString(font, text("income"), cx + GAP + 12, cy + 35, UiRenderer.TEXT_SECONDARY, false);
        int legendX = cx + 28 + font.width(text("income"));
        g.fill(legendX, cy + 36, legendX + 7, cy + 43, EXPENSE);
        g.drawString(font, text("expense"), legendX + 12, cy + 35, UiRenderer.TEXT_SECONDARY, false);
        long maximum = BankingHistory.axisMaximum(history.maximum());
        int axisWidth = Math.max(font.width(compact(maximum)), font.width(compact(maximum / 2))) + 15;
        int plotX = cx + GAP + axisWidth, plotY = cy + 59, plotWidth = w - GAP * 2 - axisWidth, plotHeight = h - 104;
        for (int index = 0; index < 3; index++) {
            int lineY = plotY + plotHeight * index / 2;
            String label = compact(index == 0 ? maximum : index == 1 ? maximum / 2 : 0);
            g.drawString(font, label, plotX - font.width(label) - 7, lineY - 4, UiRenderer.TEXT_SECONDARY, false);
            g.fill(plotX, lineY, plotX + plotWidth, lineY + 1, index == 2 ? UiRenderer.PANEL_EDGE_ACTIVE : UiRenderer.FIELD_EDGE);
        }
        int count = history.days().size();
        double cell = plotWidth / (double) Math.max(1, count);
        for (int index = 0; index < count; index++) {
            var day = history.days().get(index);
            int center = plotX + (int) Math.round((index + 0.5) * cell);
            int barWidth = Math.max(1, Math.min(14, (int) (cell * 0.28)));
            boolean hovered = !pickerOpen && inBody(mouseX, mouseY) && new Rect(plotX + (int) (index * cell), plotY, (int) Math.ceil(cell), plotHeight + 1).contains(mouseX, mouseY);
            if (hovered) {
                g.fill(plotX + (int) (index * cell), plotY, plotX + (int) ((index + 1) * cell), plotY + plotHeight, UiRenderer.ROW_SELECTED);
                tooltip = List.of(text("day", day.day() + 1), text("tooltip_income", currency(day.income())),
                        text("tooltip_expense", currency(day.expense())), text("tooltip_net", signed(day.net())));
            }
            int incoming = day.income() == 0 ? 0 : Math.max(1, (int) Math.round(day.income() / (double) maximum * plotHeight));
            int outgoing = day.expense() == 0 ? 0 : Math.max(1, (int) Math.round(day.expense() / (double) maximum * plotHeight));
            g.fill(center - barWidth - 1, plotY + plotHeight - incoming, center - 1, plotY + plotHeight, INCOME);
            g.fill(center + 1, plotY + plotHeight - outgoing, center + barWidth + 1, plotY + plotHeight, EXPENSE);
            if (index == 0 || index == count - 1 || count > 4 && index == count / 2) {
                Component label = day.day() == today ? text("today") : text("day", day.day() + 1);
                int labelX = Math.clamp(center - font.width(label) / 2, plotX, Math.max(plotX, plotX + plotWidth - font.width(label)));
                g.drawString(font, label, labelX, plotY + plotHeight + 10, UiRenderer.TEXT_SECONDARY, false);
            }
        }
        if (history.maximum() == 0) g.drawCenteredString(font, text("no_flow"), plotX + plotWidth / 2, plotY + plotHeight / 2 - 4, UiRenderer.TEXT_SECONDARY);
        g.drawString(font, text("graph_caption"), cx + GAP, cy + h - 17, UiRenderer.TEXT_SECONDARY, false);
    }

    private void balanceGraph(GuiGraphics g, List<BankingHistory.Balance> balances, long today, int cx, int cy, int w, int h) {
        UiRenderer.panel(g, cx, cy, w, h, UiRenderer.ROW);
        g.drawString(font, text("balance_history"), cx + GAP, cy + 12, UiRenderer.TEXT, false);
        int[] periods = {7, 14, 30};
        for (int index = 0; index < periods.length; index++) {
            int days = periods[index];
            button(g, new Rect(cx + w - 130 + index * 40, cy + 7, 34, 20), text("days_short", days), true, period == days, () -> period = days);
        }
        long minimum = Math.min(0, balances.stream().mapToLong(BankingHistory.Balance::amount).min().orElse(0));
        long maximum = BankingHistory.axisMaximum(balances.stream().mapToLong(BankingHistory.Balance::amount).max().orElse(0));
        int axisWidth = Math.max(font.width(compact(minimum)), font.width(compact(maximum))) + 15;
        int plotX = cx + GAP + axisWidth, plotY = cy + 42, plotWidth = w - GAP * 2 - axisWidth, plotHeight = h - 87;
        double range = (double) maximum - minimum;
        for (int index = 0; index < 3; index++) {
            int lineY = plotY + plotHeight * index / 2;
            String label = compact(Math.round(maximum - range * index / 2));
            g.drawString(font, label, plotX - font.width(label) - 7, lineY - 4, UiRenderer.TEXT_SECONDARY, false);
            g.fill(plotX, lineY, plotX + plotWidth, lineY + 1, UiRenderer.FIELD_EDGE);
        }
        double cell = plotWidth / (double) Math.max(1, balances.size());
        int previousX = 0, previousY = 0;
        for (int index = 0; index < balances.size(); index++) {
            var balance = balances.get(index);
            int center = plotX + (int) Math.round((index + 0.5) * cell);
            int pointY = plotY + (int) Math.round((maximum - (double) balance.amount()) / range * plotHeight);
            if (index > 0) {
                for (int px = previousX; px <= center; px++) {
                    int py = (int) Math.round(previousY + (pointY - previousY) * ((px - previousX) / (double) Math.max(1, center - previousX)));
                    g.fill(px, py, px + 1, py + 2, INCOME);
                }
            }
            boolean hovered = !pickerOpen && inBody(mouseX, mouseY)
                    && new Rect(plotX + (int) (index * cell), plotY, (int) Math.ceil(cell), plotHeight + 1).contains(mouseX, mouseY);
            if (hovered) {
                g.fill(center, plotY, center + 1, plotY + plotHeight, UiRenderer.PANEL_EDGE_ACTIVE);
                tooltip = List.of(text("day", balance.day() + 1), text("tooltip_balance", currency(balance.amount())));
            }
            g.fill(center - 2, pointY - 2, center + 3, pointY + 3, INCOME);
            if (index == 0 || index == balances.size() - 1 || balances.size() > 4 && index == balances.size() / 2) {
                Component label = balance.day() == today ? text("today") : text("day", balance.day() + 1);
                int labelX = Math.clamp(center - font.width(label) / 2, plotX, Math.max(plotX, plotX + plotWidth - font.width(label)));
                g.drawString(font, label, labelX, plotY + plotHeight + 10, UiRenderer.TEXT_SECONDARY, false);
            }
            previousX = center;
            previousY = pointY;
        }
        g.drawString(font, text("balance_caption"), cx + GAP, cy + h - 17, UiRenderer.TEXT_SECONDARY, false);
    }

    private void transfer(GuiGraphics g, ArchiveBankingPayload snapshot, int cx, int cy, int w, int h) {
        UiRenderer.panel(g, cx, cy, w, h, UiRenderer.ROW);
        g.drawString(font, text("transfer"), cx + GAP, cy + 12, UiRenderer.TEXT, false);
        g.drawString(font, text("recipient"), cx + GAP, cy + 34, UiRenderer.TEXT_SECONDARY, false);
        target.setX(cx + GAP); target.setY(cy + 47); target.setWidth(w - GAP * 2 - 32);
        target.setEditable(pendingAction.isEmpty()); amount.setEditable(pendingAction.isEmpty());
        target.render(g, mouseX, mouseY, 0);
        chooser = new Rect(cx + w - GAP - 22, cy + 47, 22, 22);
        button(g, chooser, Component.empty(), true, pickerOpen, () -> { pickerOpen = !pickerOpen; pickerScroll.reset(); });
        UiRenderer.disclosureChevron(g, chooser.x + chooser.width / 2.0F, chooser.y + chooser.height / 2.0F, pickerOpen ? -1 : 1, UiRenderer.TEXT);
        g.drawString(font, text("amount"), cx + GAP, cy + 82, UiRenderer.TEXT_SECONDARY, false);
        amount.setX(cx + GAP); amount.setY(cy + 95); amount.setWidth(w - GAP * 2);
        amount.render(g, mouseX, mouseY, 0);
        int value = parsedAmount();
        boolean recipient = players().stream().anyMatch(info -> info.online() && info.name().equalsIgnoreCase(target.getValue().strip()));
        boolean self = Minecraft.getInstance().player != null && target.getValue().strip().equalsIgnoreCase(Minecraft.getInstance().player.getGameProfile().getName());
        boolean valid = value > 0 && !target.getValue().isBlank() && !self && pendingAction.isEmpty();
        int buttonWidth = (w - GAP * 3) / 2;
        button(g, new Rect(cx + GAP, cy + 131, buttonWidth, 22), text("send"), valid && recipient && snapshot.balance() >= value, false, () -> submit(ArchiveBankingActionPayload.ACTION_SEND, target.getValue().strip(), value, ""));
        button(g, new Rect(cx + GAP * 2 + buttonWidth, cy + 131, w - GAP * 3 - buttonWidth, 22), text("request"), valid, false, () -> submit(ArchiveBankingActionPayload.ACTION_REQUEST, target.getValue().strip(), value, ""));
        Component hint = !pendingAction.isEmpty() ? feedbackError ? feedback : text("processing") : !feedback.getString().isBlank() ? feedback
                : self ? text("self_recipient") : target.getValue().isBlank() ? text(players().isEmpty() ? "no_players" : "transfer_hint")
                : !amount.getValue().isBlank() && value <= 0 ? text("valid_amount") : !recipient ? text("choose_online")
                : value > snapshot.balance() ? text("cannot_send") : text("transfer_hint");
        int textY = cy + 168;
        for (var line : font.split(hint, w - GAP * 2)) {
            if (textY + 9 > cy + h - GAP) break;
            g.drawString(font, line, cx + GAP, textY, feedbackError ? EXPENSE : UiRenderer.TEXT_SECONDARY, false);
            textY += 11;
        }
    }

    private int activity(GuiGraphics g, List<ArchiveBankingPayload.LedgerEntry> entries, int cx, int cy, int w) {
        var visible = filteredEntries(entries);
        g.drawString(font, text("activity"), cx, cy + 6, UiRenderer.TEXT, false);
        cy += 30;
        int count = visible.size();
        if (count == 0) {
            empty(g, cx, cy, w, entries.isEmpty() ? text("no_activity") : text("no_matches"));
            return cy + 74;
        }
        for (int index = 0; index < count; index++) {
            var entry = visible.get(index);
            int dateWidth = Math.max(76, font.width(text("day", entry.day() + 1)) + 14);
            int amountWidth = Math.max(110, font.width(signed(entry.amount())) + 20);
            int detailsWidth = Math.max(80, w - dateWidth - amountWidth - GAP * 3);
            Component title = Component.literal(entry.note().isBlank() ? humanize(entry.typeId()) : entry.note());
            Component detail = Component.literal((entry.counterpartyName().isBlank() ? text("system").getString() : entry.counterpartyName()) + " · " + humanize(entry.typeId()));
            var titleLines = font.split(title, detailsWidth);
            var detailLines = font.split(detail, detailsWidth);
            int rowHeight = ledgerHeight(entry, w);
            if (cy + rowHeight <= y + HEADER || cy >= y + height - GAP) { cy += rowHeight + GAP; continue; }
            UiRenderer.panel(g, cx, cy, w, rowHeight, UiRenderer.ROW);
            int color = entry.amount() > 0 ? INCOME : entry.amount() < 0 ? EXPENSE : UiRenderer.TEXT_SECONDARY;
            g.fill(cx + 1, cy + 1, cx + 3, cy + rowHeight - 1, color);
            g.drawString(font, text("day", entry.day() + 1), cx + GAP, cy + 12, UiRenderer.TEXT_SECONDARY, false);
            int ty = cy + 11;
            for (var line : titleLines) { g.drawString(font, line, cx + dateWidth + GAP, ty, UiRenderer.TEXT, false); ty += 11; }
            ty += 4;
            for (var line : detailLines) { g.drawString(font, line, cx + dateWidth + GAP, ty, UiRenderer.TEXT_SECONDARY, false); ty += 11; }
            String value = signed(entry.amount());
            g.drawString(font, value, cx + w - GAP - font.width(value), cy + 12, color, false);
            cy += rowHeight + GAP;
        }
        return cy - GAP;
    }

    private int requests(GuiGraphics g, ArchiveBankingPayload snapshot, int cx, int cy, int w) {
        if (!pendingAction.isEmpty() || !feedback.getString().isBlank()) {
            Component message = pendingAction.isEmpty() || feedbackError ? feedback : text("processing");
            var lines = font.split(message, w - 20);
            int bannerHeight = lines.size() * 11 + 20;
            UiRenderer.panel(g, cx, cy, w, bannerHeight, UiRenderer.ROW);
            int textY = cy + 10;
            for (var line : lines) { g.drawString(font, line, cx + 10, textY, feedbackError ? EXPENSE : INCOME, false); textY += 11; }
            cy += bannerHeight + GAP;
        }
        var visible = filteredRequests(snapshot);
        if (visible.isEmpty()) {
            empty(g, cx, cy, w, snapshot.requests().isEmpty() ? text("no_requests") : text("no_matches"));
            return cy + 74;
        }
        int columns = w >= 700 ? 2 : 1;
        for (int index = 0; index < visible.size(); index++) {
            var request = visible.get(index);
            int left = cx + index % columns * (w + GAP) / columns;
            int right = cx + (index % columns + 1) * (w + GAP) / columns - GAP;
            int top = cy + index / columns * 128, cardWidth = right - left;
            if (top + 118 <= y + HEADER || top >= y + height - GAP) continue;
            UiRenderer.panel(g, left, top, cardWidth, 118, UiRenderer.ROW);
            g.drawString(font, text(request.incoming() ? "incoming_request" : "outgoing_request"), left + GAP, top + 11, request.incoming() ? EXPENSE : INCOME, false);
            Component name = text(request.incoming() ? "from" : "to", request.incoming() ? request.fromName() : request.toName());
            g.drawString(font, name, left + GAP, top + 30, UiRenderer.TEXT, false);
            g.drawString(font, text("day", request.createdDay() + 1), left + GAP, top + 47, UiRenderer.TEXT_SECONDARY, false);
            money(g, request.amount(), left + GAP, top + 76, cardWidth - GAP * 2 - (request.incoming() ? 170 : 90), UiRenderer.TEXT, 1.2F, false);
            boolean ready = pendingAction.isEmpty();
            if (request.incoming()) button(g, new Rect(right - 170, top + 79, 74, 22), text("pay"), ready && snapshot.balance() >= request.amount(), false,
                    () -> submit(ArchiveBankingActionPayload.ACTION_APPROVE, "", request.amount(), request.requestId()));
            button(g, new Rect(right - 86, top + 79, 76, 22), text(request.incoming() ? "decline" : "cancel"), ready, false,
                    () -> submit(ArchiveBankingActionPayload.ACTION_DECLINE, "", 0, request.requestId()));
            if (request.incoming() && snapshot.balance() < request.amount()) g.drawString(font, text("insufficient"), left + GAP, top + 101, EXPENSE, false);
        }
        return cy + ((visible.size() + columns - 1) / columns) * 128 - GAP;
    }

    private void renderPicker(GuiGraphics g) {
        if (chooser.y < y + HEADER || chooser.y + chooser.height > y + height - GAP) { pickerOpen = false; pickerScrollbar.release(); return; }
        var players = players();
        int available = y + height - GAP - chooser.y - chooser.height - 3;
        int desired = Math.max(1, Math.min(6, players.size())) * 36 + 8;
        boolean below = available >= Math.min(80, desired);
        int popupHeight = Math.min(below ? available : chooser.y - y - HEADER - 3, desired);
        if (popupHeight < 28) { pickerOpen = false; pickerScrollbar.release(); return; }
        popup = new Rect(target.getX(), below ? chooser.y + chooser.height + 3 : chooser.y - popupHeight - 3, target.getWidth() + 32, popupHeight);
        pickerMax = Math.max(0, players.size() * 36 - popupHeight + 8);
        pickerScroll.clamp(pickerMax); pickerScroll.tick();
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        UiRenderer.panel(g, popup.x, popup.y, popup.width, popup.height, UiRenderer.FIELD, UiRenderer.PANEL_EDGE_ACTIVE);
        g.enableScissor(popup.x + 4, popup.y + 4, popup.x + popup.width - 12, popup.y + popup.height - 4);
        if (players.isEmpty()) g.drawString(font, text("picker_empty"), popup.x + 6, popup.y + 10, UiRenderer.TEXT_SECONDARY, false);
        for (int index = 0; index < players.size(); index++) {
            var row = pickerRow(index);
            boolean hover = popup.contains(mouseX, mouseY) && row.contains(mouseX, mouseY);
            UiRenderer.control(g, row.x, row.y, row.width, row.height, true, target.getValue().equalsIgnoreCase(players.get(index).name()), hover, -1);
            g.drawString(font, font.plainSubstrByWidth(players.get(index).name(), row.width - 12), row.x + 6, row.y + 5, UiRenderer.TEXT, false);
            var player = players.get(index);
            Component seen = text(player.online() ? "online" : player.daysAgo() == 0 ? "seen_today" : "last_seen", player.daysAgo());
            g.drawString(font, seen, row.x + 6, row.y + 19, player.online() ? INCOME : UiRenderer.TEXT_SECONDARY, false);
        }
        g.disableScissor();
        UiScrollbar.draw(g, pickerTrack(), mouseX, mouseY, pickerScrollbar.dragging());
        g.pose().popPose();
    }

    boolean click(double mx, double my) {
        if (!new Rect(x, y, width, height).contains(mx, my)) { hide(); return false; }
        if (pickerOpen && popup != null) {
            if (pickerScrollbar.begin(pickerTrack(), mx, my)) { pickerScroll.set(pickerScrollbar.value(pickerTrack(), mx, my), pickerMax); return true; }
            if (popup.contains(mx, my)) {
                var players = players();
                for (int index = 0; index < players.size(); index++) if (pickerRow(index).contains(mx, my)) {
                    target.setValue(players.get(index).name()); pickerOpen = false; unfocus(); clickSound(); return true;
                }
                return true;
            }
            pickerOpen = false;
            return true;
        }
        if (scrollbar.begin(track(), mx, my)) { scrolls[page].set(scrollbar.value(track(), mx, my), maxScroll); return true; }
        for (var field : fields()) field.setFocused(field.isVisible() && new Rect(field.getX(), field.getY(), field.getWidth(), field.getHeight()).contains(mx, my)
                && (field == search || inBody(mx, my)));
        for (var field : fields()) if (field.isFocused()) { field.mouseClicked(mx, my, 0); return true; }
        for (var action : actions) if (action.enabled && action.bounds.contains(mx, my) && (!action.body || inBody(mx, my))) {
            action.run.run(); clickSound(); return true;
        }
        return true;
    }

    boolean scroll(double mx, double my, double delta) {
        if (pickerOpen && popup != null && popup.contains(mx, my)) { pickerScroll.add(-delta * UiScrollState.WHEEL_STEP, pickerMax); return true; }
        if (!inBody(mx, my)) return false;
        pickerOpen = false; unfocus(); scrolls[page].add(-delta * UiScrollState.WHEEL_STEP, maxScroll); return true;
    }

    boolean drag(double mx, double my) {
        if (pickerScrollbar.dragging()) { pickerScroll.set(pickerScrollbar.value(pickerTrack(), mx, my), pickerMax); return true; }
        if (scrollbar.dragging()) { scrolls[page].set(scrollbar.value(track(), mx, my), maxScroll); return true; }
        for (var field : fields()) if (field.isFocused()) return field.mouseDragged(mx, my, 0, 0, 0);
        return false;
    }

    void release() { scrollbar.release(); pickerScrollbar.release(); }
    void hide() { unfocus(); pickerOpen = false; release(); }
    private void unfocus() { for (var field : fields()) field.setFocused(false); }
    private List<UiEditBox> fields() { return search == null ? List.of() : List.of(search, target, amount); }

    boolean key(int key, int code, int modifiers) {
        if (key == 256 && pickerOpen) { pickerOpen = false; return true; }
        if (key == 258 && search != null) {
            var visible = fields().stream().filter(UiEditBox::isVisible).toList();
            int current = -1;
            for (int index = 0; index < visible.size(); index++) if (visible.get(index).isFocused()) current = index;
            if (current < 0 && (modifiers & 1) != 0) current = 0;
            unfocus();
            visible.get(Math.floorMod(current + ((modifiers & 1) != 0 ? -1 : 1), visible.size())).setFocused(true);
            return true;
        }
        for (var field : fields()) if (field.isVisible() && field.isFocused()) {
            if (key == 256 || key == 257 || key == 335) unfocus(); else field.keyPressed(key, code, modifiers);
            return true;
        }
        return false;
    }

    boolean character(char value, int modifiers) {
        for (var field : fields()) if (field.isVisible() && field.isFocused()) { field.charTyped(value, modifiers); return true; }
        return false;
    }

    private void switchPage(int selected) { page = selected; filter = 0; maxScroll = 0; hide(); }
    private boolean inBody(double mx, double my) { return new Rect(x + GAP, y + HEADER, width - GAP * 2, Math.max(1, height - HEADER - GAP)).contains(mx, my); }
    private UiScrollbar.Track track() { return UiScrollbar.layout(ArchiveContentLayout.scrollbarX(x, width), y + HEADER, UiScrollbar.WIDTH, Math.max(1, height - HEADER - GAP), false, height - HEADER - GAP, maxScroll, scrolls[page].current); }
    private UiScrollbar.Track pickerTrack() { return UiScrollbar.layout(popup.x + popup.width - 9, popup.y + 4, UiScrollbar.WIDTH, popup.height - 8, false, popup.height - 8, pickerMax, pickerScroll.current); }
    private Rect pickerRow(int index) { return new Rect(popup.x + 4, popup.y + 4 + index * 36 - (int) Math.round(pickerScroll.current), popup.width - 17, 34); }

    private void submit(String action, String name, int value, String id) {
        if (!pendingAction.isEmpty()) return;
        pendingSnapshot = ArchiveBankingClientState.get(); pendingAction = action; sentAt = System.nanoTime();
        feedback = Component.empty(); feedbackError = false; pickerOpen = false; unfocus();
        PacketDistributor.sendToServer(new ArchiveBankingActionPayload(action, name, value, id));
    }

    private void receiveResult(ArchiveBankingPayload snapshot) {
        if (pendingAction.isEmpty()) return;
        if (snapshot != pendingSnapshot && snapshot.actionId().equals(pendingAction)) {
            feedbackError = !snapshot.feedbackKey().isEmpty();
            feedback = feedbackError ? Component.translatable(snapshot.feedbackKey()) : text("done." + pendingAction);
            if (!feedbackError && (pendingAction.equals(ArchiveBankingActionPayload.ACTION_SEND) || pendingAction.equals(ArchiveBankingActionPayload.ACTION_REQUEST))) amount.setValue("");
            pendingAction = ""; pendingSnapshot = null;
        } else if (System.nanoTime() - sentAt > 5_000_000_000L) {
            feedback = text("timeout"); feedbackError = true;
        }
    }

    private void clearFeedback() {
        if (pendingAction.isEmpty()) { feedback = Component.empty(); feedbackError = false; }
    }

    private int parsedAmount() { try { return Integer.parseInt(amount.getValue()); } catch (NumberFormatException ignored) { return 0; } }
    private List<ArchiveBankingPayload.Recipient> players() { return ArchiveBankingClientState.get().recipients(); }

    private void button(GuiGraphics g, Rect bounds, Component label, boolean enabled, boolean selected, Runnable run) {
        boolean hovered = bounds.contains(mouseX, mouseY) && (!drawingBody || inBody(mouseX, mouseY));
        UiRenderer.control(g, bounds.x, bounds.y, bounds.width, bounds.height, enabled, selected, hovered, -1);
        g.drawString(font, label, bounds.x + (bounds.width - font.width(label)) / 2, bounds.y + (bounds.height - 8) / 2, enabled ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, false);
        actions.add(new Action(bounds, enabled, drawingBody, run));
    }

    private void empty(GuiGraphics g, int cx, int cy, int w, Component message) {
        UiRenderer.panel(g, cx, cy, w, 74, UiRenderer.ROW);
        g.drawCenteredString(font, message, cx + w / 2, cy + 32, UiRenderer.TEXT_SECONDARY);
    }

    private void money(GuiGraphics g, long value, int cx, int cy, int available, int color, float preferred, boolean sign) {
        String label = sign ? signed(value) : currency(value);
        float scale = Math.min(preferred, Math.max(0.65F, available / (float) Math.max(1, font.width(label))));
        g.pose().pushPose(); g.pose().translate(cx, cy, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, label, 0, 0, color, false); g.pose().popPose();
    }

    private static String currency(long value) { return (value < 0 ? "-" : "") + "€$" + NumberFormat.getIntegerInstance().format(Math.abs(value)); }
    private static String signed(long value) { return (value > 0 ? "+" : value < 0 ? "-" : "") + currency(Math.abs(value)); }
    private static String compact(long value) {
        if (value < 1000) return "€$" + value;
        String[] suffix = {"K", "M", "B", "T"};
        double scaled = value;
        int index = -1;
        do { scaled /= 1000; index++; } while (scaled >= 1000 && index < suffix.length - 1);
        return "€$" + String.format(Locale.ROOT, scaled >= 10 || scaled == Math.floor(scaled) ? "%.0f" : "%.1f", scaled) + suffix[index];
    }
    private static String humanize(String value) {
        String label = value.replace('_', ' ');
        return label.isBlank() ? "" : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
    private static Component text(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.archive.banking.browser." + key, args); }
    private static void clickSound() { Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1)); }
    private record Rect(int x, int y, int width, int height) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private record Action(Rect bounds, boolean enabled, boolean body, Runnable run) {}
}
