package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ArchiveQuestsClientState;
import de.artemis.cyberneticenhancements.client.CompassPreferences;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestActionPayload;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ContractBrowser {
    private static final int GAP = 10, HEADER = 62;
    private static final String[] CATEGORIES = {"active", "pending_delivery", "completed", "abandoned", "failed", "cancelled", "hidden"};
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final List<Card> cards = new ArrayList<>();
    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private int headerHeight = HEADER;
    private final Runnable closeArchive;
    private int setupButtonX, setupButtonY, setupButtonWidth;
    private boolean setupClaimPending, setupInventoryFull;
    private UiEditBox search;
    private String query = "";
    private int category;
    private int type, x, y, width, height, mouseX, mouseY, maxScroll;

    ContractBrowser(Runnable closeArchive) { this.closeArchive = closeArchive; }

    private boolean setupRequired() { return !ArchiveQuestsClientState.isLoaded() || !ArchiveQuestsClientState.get().dropoffPlaced(); }

    private Component text(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.archive.quests." + key, args);
    }

    private int typeWidth(Font font) {
        int result = font.width(text("all_types"));
        for (QuestType value : QuestType.values()) result = Math.max(result, font.width(typeLabel(value)));
        return result + 54;
    }

    private Component typeLabel(QuestType value) {
        return Component.translatable("quest.cyberneticenhancements.type." + value.id());
    }

    private boolean active(ArchiveQuestsPayload.QuestEntry quest) {
        return quest.statusId().equals("active") || quest.statusId().equals("ready");
    }

    private List<ArchiveQuestsPayload.QuestEntry> visible() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        String selected = CATEGORIES[category];
        return ArchiveQuestsClientState.get().quests().stream()
                .filter(quest -> switch (selected) {
                    case "active" -> active(quest) && !hidden(quest);
                    case "hidden" -> active(quest) && hidden(quest);
                    default -> quest.statusId().equals(selected);
                })
                .filter(quest -> type == 0 || quest.typeId().equals(QuestType.values()[type - 1].id()))
                .filter(quest -> (quest.title() + " " + quest.issuerName() + " " + quest.objective()).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private static boolean hidden(ArchiveQuestsPayload.QuestEntry quest) {
        return CompassPreferences.entry(quest.questId()).hidden();
    }

    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        mouseX = mx; mouseY = my;
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);
        if (setupRequired()) {
            cards.clear();
            maxScroll = 0;
            scroll.reset();
            release();
            unfocus();
            renderSetup(g, font);
            return;
        }
        int searchWidth = Math.max(80, width - GAP * 3 - typeWidth(font));
        if (search == null) {
            search = new UiEditBox(font, x + GAP, y + GAP, searchWidth, 22, text("search"));
            search.setMaxLength(80);
            search.setHint(text("search"));
            search.setResponder(value -> { query = value; scroll.reset(); });
        }
        search.setX(x + GAP); search.setY(y + GAP); search.setWidth(searchWidth);
        search.render(g, mx, my, 0);
        button(g, font, x + width - GAP - typeWidth(font), y + GAP, typeWidth(font), 22,
                text("type_filter", type == 0 ? text("all_types") : typeLabel(QuestType.values()[type - 1])), type > 0);
        int categoryX = x + GAP, categoryY = y + 42;
        categoryTabs.clear();
        for (int index = 0; index < CATEGORIES.length; index++) {
            int tabWidth = categoryWidth(font, index);
            if (categoryX > x + GAP && categoryX + tabWidth > x + width - GAP) {
                categoryX = x + GAP; categoryY += 20 + GAP;
            }
            categoryTabs.add(new CategoryTab(index, categoryX, categoryY, tabWidth));
            button(g, font, categoryX, categoryY, tabWidth, 20, text("category." + CATEGORIES[index]), category == index);
            categoryX += tabWidth + GAP;
        }
        headerHeight = categoryY - y + 20;
        var quests = visible();
        Component count = text("count", quests.size());
        if (categoryX + font.width(count) <= x + width - GAP) g.drawString(font, count, x + width - GAP - font.width(count), categoryY + 6, UiRenderer.TEXT_SECONDARY, false);
        int cardWidth = ArchiveContentLayout.contentWidth(width);
        var layout = new ArrayList<Card>();
        int total = 0;
        for (int pass = 0; pass < 2; pass++) {
            layout.clear(); total = 0;
            int columns = ArchiveContactLayout.columns(cardWidth);
            for (int row = 0; row < quests.size(); row += columns) {
                var contents = new ArrayList<Content>();
                int rowHeight = 0;
                for (int column = 0; column < columns && row + column < quests.size(); column++) {
                    var bounds = ArchiveContactLayout.card(column, cardWidth);
                    var content = content(font, quests.get(row + column), bounds.width());
                    contents.add(content);
                    rowHeight = Math.max(rowHeight, content.flow.height());
                }
                for (int column = 0; column < contents.size(); column++) {
                    var bounds = ArchiveContactLayout.card(column, cardWidth);
                    layout.add(new Card(quests.get(row + column), ArchiveContentLayout.contentX(x) + bounds.x(), listTop() + total,
                            bounds.width(), rowHeight, contents.get(column)));
                }
                total += rowHeight + GAP;
            }
            if (pass == 0 && Math.max(0, total - GAP) > viewportHeight()) cardWidth = ArchiveContentLayout.contentWidth(width, true);
            else break;
        }
        maxScroll = Math.max(0, Math.max(0, total - GAP) - viewportHeight());
        scroll.clamp(maxScroll); scroll.tick(); cards.clear();
        g.enableScissor(ArchiveContentLayout.contentX(x), listTop(), ArchiveContentLayout.contentX(x) + cardWidth + 3, listTop() + viewportHeight());
        if (!ArchiveQuestsClientState.isLoaded() || quests.isEmpty()) {
            Component empty = !ArchiveQuestsClientState.isLoaded() ? text("syncing")
                    : !query.isBlank() || type != 0 ? text("no_matches") : text(category == 0 ? "empty" : "empty." + CATEGORIES[category]);
            UiRenderer.panel(g, ArchiveContentLayout.contentX(x), listTop(), cardWidth, 74, UiRenderer.ROW);
            g.drawCenteredString(font, empty, ArchiveContentLayout.contentX(x) + cardWidth / 2, listTop() + 32, UiRenderer.TEXT_SECONDARY);
        }
        for (var entry : layout) {
            var card = new Card(entry.quest, entry.x, entry.y - (int) Math.round(scroll.current), entry.width, entry.height, entry.content);
            if (card.y + card.height >= listTop() && card.y < listTop() + viewportHeight()) {
                drawCard(g, font, card);
                cards.add(card);
            }
        }
        g.disableScissor();
        UiScrollbar.draw(g, track(), mx, my, scrollbar.dragging());
    }

    private void renderSetup(GuiGraphics graphics, Font font) {
        setupButtonWidth = 0;
        if (!ArchiveQuestsClientState.isLoaded()) {
            graphics.drawCenteredString(font, text("syncing"), x + width / 2, y + height / 2 - 4, UiRenderer.TEXT_SECONDARY);
            return;
        }
        int popupWidth = Math.min(440, width - GAP * 4);
        Component message = text(ArchiveQuestsClientState.get().setupItemGiven() ? "setup.instructions"
                : setupInventoryFull ? "setup.inventory_full" : "setup.claim_instructions");
        var lines = font.split(message, popupWidth - 40);
        var help = font.split(text("setup.replacement"), popupWidth - 40);
        int popupHeight = 150 + (lines.size() + help.size()) * 11;
        int left = x + (width - popupWidth) / 2, top = y + (height - popupHeight) / 2;
        UiRenderer.panel(graphics, left, top, popupWidth, popupHeight, UiRenderer.ROW);
        graphics.drawCenteredString(font, text("setup.title"), left + popupWidth / 2, top + 16, UiRenderer.TEXT);
        graphics.pose().pushPose();
        graphics.pose().translate(left + popupWidth / 2.0F - 16, top + 40, 0);
        graphics.pose().scale(2, 2, 1);
        de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer.drawWithShadow(graphics,
                new net.minecraft.world.item.ItemStack(de.artemis.cyberneticenhancements.common.registry.ModBlocks.DROPOFF_POINT.get()), 0, 0, true);
        graphics.pose().popPose();
        int textY = top + 88;
        for (var line : lines) {
            graphics.drawString(font, line, left + (popupWidth - font.width(line)) / 2, textY, UiRenderer.TEXT, false);
            textY += 11;
        }
        textY += GAP;
        for (var line : help) {
            graphics.drawString(font, line, left + (popupWidth - font.width(line)) / 2, textY, UiRenderer.TEXT_SECONDARY, false);
            textY += 11;
        }
        Component buttonLabel = text(setupClaimPending ? "setup.claiming" : ArchiveQuestsClientState.get().setupItemGiven() ? "setup.return_to_world" : "setup.claim");
        setupButtonWidth = Math.min(popupWidth - 40, Math.max(150, font.width(buttonLabel) + 24));
        setupButtonX = left + (popupWidth - setupButtonWidth) / 2;
        setupButtonY = top + popupHeight - 34;
        UiRenderer.button(graphics, font, setupButtonX, setupButtonY, setupButtonWidth, 22, buttonLabel, !setupClaimPending, false,
                inside(mouseX, mouseY, setupButtonX, setupButtonY, setupButtonWidth, 22), -1);
    }

    private Content content(Font font, ArchiveQuestsPayload.QuestEntry quest, int width) {
        int innerWidth = width - GAP * 2;
        int statusWidth = Math.min(110, innerWidth / 3);
        Component title = Component.literal(quest.title()), status = quest.statusId().equals("pending_delivery")
                ? text("category.pending_delivery") : Component.literal(quest.statusLabel());
        Component issuer = text("issuer", quest.issuerName());
        var paragraphs = new ArrayList<Component>();
        paragraphs.add(text("objective", quest.objective()));
        paragraphs.add(text("reward", quest.reward()));
        if (quest.statusId().equals("pending_delivery") && !quest.typeId().isEmpty()) paragraphs.add(text("pending.note"));
        if (active(quest)) paragraphs.add(text(QuestType.fromId(quest.typeId()).returnsItems() ? "in_person_required" : QuestType.fromId(quest.typeId()).requiresInPersonTurnIn() ? "report_required" : "remote_rewards"));
        if (quest.hasLocation()) paragraphs.add(text("location",
                de.artemis.cyberneticenhancements.common.quest.QuestDimension.name(quest.dimensionId()), quest.x(), quest.y(), quest.z()));
        int headingLines = Math.max(font.split(title, innerWidth - statusWidth - GAP).size(), font.split(status, statusWidth).size());
        var flow = ArchiveCardLayout.flow(headingLines, font.split(issuer, innerWidth).size(),
                paragraphs.stream().map(line -> font.split(line, innerWidth).size()).toList(), active(quest));
        return new Content(title, status, issuer, List.copyOf(paragraphs), statusWidth, flow);
    }

    private void drawCard(GuiGraphics g, Font font, Card card) {
        UiRenderer.panel(g, card.x, card.y, card.width, card.height, UiRenderer.ROW);
        var content = card.content;
        int left = card.x + GAP, innerWidth = card.width - GAP * 2;
        drawLines(g, font, content.title, left, card.y + content.flow.headingY(), innerWidth - content.statusWidth - GAP, UiRenderer.TEXT);
        var statusLines = font.split(content.status, content.statusWidth);
        for (int index = 0; index < statusLines.size(); index++) {
            var line = statusLines.get(index);
            g.drawString(font, line, card.x + card.width - GAP - font.width(line), card.y + content.flow.headingY() + index * ArchiveCardLayout.LINE,
                    card.quest.statusId().equals("ready") ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
        }
        drawLines(g, font, content.issuer, left, card.y + content.flow.subtitleY(), innerWidth, UiRenderer.TEXT_SECONDARY);
        g.fill(left, card.y + content.flow.dividerY(), left + innerWidth, card.y + content.flow.dividerY() + 1, UiRenderer.FIELD_EDGE);
        for (int index = 0; index < content.paragraphs.size(); index++) {
            drawLines(g, font, content.paragraphs.get(index), left, card.y + content.flow.paragraphs().get(index), innerWidth, UiRenderer.TEXT_SECONDARY);
        }
        if (active(card.quest)) {
            int buttonY = actionY(card);
            int ruleY = buttonY - GAP - 1;
            g.fill(left, ruleY, left + innerWidth, ruleY + 1, UiRenderer.FIELD_EDGE);
            boolean physicalReturn = QuestType.fromId(card.quest.typeId()).requiresInPersonTurnIn();
            int actionWidth = actionWidth(card);
            UiRenderer.button(g, font, actionX(card, 0), buttonY, actionWidth, ArchiveCardLayout.BUTTON,
                    text(physicalReturn ? "button.in_person" : "button.turn_in"), card.quest.statusId().equals("ready") && !physicalReturn, false,
                    actionHit(card, 0, mouseX, mouseY) && g.containsPointInScissor(mouseX, mouseY), -1);
            button(g, font, actionX(card, 1), buttonY, actionWidth, ArchiveCardLayout.BUTTON,
                    text(hidden(card.quest) ? "button.show" : "button.hide"), false);
            button(g, font, actionX(card, 2), buttonY, actionWidth, ArchiveCardLayout.BUTTON, text("button.discard"), false);
        }
    }

    private static int actionWidth(Card card) { return (card.width - GAP * 4) / 3; }
    private static int actionX(Card card, int index) { return card.x + GAP + index * (actionWidth(card) + GAP); }
    private static int actionY(Card card) { return card.y + card.height - GAP - ArchiveCardLayout.BUTTON; }
    private static boolean actionHit(Card card, int index, double mx, double my) {
        return inside(mx, my, actionX(card, index), actionY(card), actionWidth(card), ArchiveCardLayout.BUTTON);
    }

    private void drawLines(GuiGraphics g, Font font, Component value, int x, int y, int width, int color) {
        for (var line : font.split(value, width)) {
            g.drawString(font, line, x, y, color, false);
            y += ArchiveCardLayout.LINE;
        }
    }
    private void button(GuiGraphics g, Font font, int x, int y, int width, int height, Component label, boolean selected) {
        UiRenderer.button(g, font, x, y, width, height, label, true, selected,
                inside(mouseX, mouseY, x, y, width, height) && g.containsPointInScissor(mouseX, mouseY), 0xFFFFFFFF);
    }

    private int listTop() { return y + headerHeight + GAP; }
    private int categoryWidth(Font font, int index) { return Math.max(84, font.width(text("category." + CATEGORIES[index])) + 24); }
    private int viewportHeight() { return Math.max(1, height - headerHeight - GAP * 2); }
    private UiScrollbar.Track track() {
        return UiScrollbar.layout(ArchiveContentLayout.scrollbarX(x, width), listTop(), UiScrollbar.WIDTH,
                viewportHeight(), false, viewportHeight(), maxScroll, scroll.current);
    }

    boolean click(double mx, double my) {
        if (!inside(mx, my, x, y, width, height)) return false;
        if (setupRequired()) {
            if (!setupClaimPending && setupButtonWidth > 0 && inside(mx, my, setupButtonX, setupButtonY, setupButtonWidth, 22)) {
                clickSound();
                if (ArchiveQuestsClientState.get().setupItemGiven()) closeArchive.run();
                else {
                    setupClaimPending = true;
                    setupInventoryFull = false;
                    PacketDistributor.sendToServer(new ArchiveQuestActionPayload(ArchiveQuestActionPayload.ACTION_CLAIM_DROPOFF, ""));
                }
            }
            return true;
        }
        if (scrollbar.begin(track(), mx, my)) { unfocus(); scroll.set(scrollbar.value(track(), mx, my), maxScroll); return true; }
        if (search != null) {
            search.setFocused(search.isMouseOver(mx, my));
            if (search.isFocused()) return search.mouseClicked(mx, my, 0);
        }
        Font font = Minecraft.getInstance().font;
        for (var tab : categoryTabs) {
            if (inside(mx, my, tab.x, tab.y, tab.width, 20)) {
                if (category != tab.index) { category = tab.index; scroll.reset(); clickSound(); }
                return true;
            }
        }
        if (inside(mx, my, x + width - GAP - typeWidth(font), y + GAP, typeWidth(font), 22)) {
            type = (type + 1) % (QuestType.values().length + 1); scroll.reset(); clickSound(); return true;
        }
        if (my >= listTop() && my < listTop() + viewportHeight()) for (Card card : cards) {
            if (!active(card.quest)) continue;
            if (card.quest.statusId().equals("ready") && !QuestType.fromId(card.quest.typeId()).requiresInPersonTurnIn()
                    && actionHit(card, 0, mx, my)) {
                PacketDistributor.sendToServer(new ArchiveQuestActionPayload(ArchiveQuestActionPayload.ACTION_TURN_IN, card.quest.questId()));
                clickSound(); return true;
            }
            if (actionHit(card, 1, mx, my)) {
                CompassPreferences.toggleHidden(card.quest.questId());
                cards.clear();
                clickSound(); return true;
            }
            if (actionHit(card, 2, mx, my)) {
                PacketDistributor.sendToServer(new ArchiveQuestActionPayload(ArchiveQuestActionPayload.ACTION_DISCARD, card.quest.questId()));
                clickSound(); return true;
            }
        }
        return true;
    }

    boolean scroll(double mx, double my, double delta) {
        if (setupRequired()) return inside(mx, my, x, y, width, height);
        if (!inside(mx, my, x, listTop(), width, viewportHeight())) return false;
        scroll.add(-delta * UiScrollState.WHEEL_STEP, maxScroll); return true;
    }
    boolean drag(double mx, double my) {
        if (setupRequired()) return true;
        if (!scrollbar.dragging()) return search != null && search.isFocused() && search.mouseDragged(mx, my, 0, 0, 0);
        scroll.set(scrollbar.value(track(), mx, my), maxScroll); return true;
    }
    void release() { scrollbar.release(); }
    void unfocus() { if (search != null) search.setFocused(false); }
    void reset() { category = 0; type = 0; scroll.reset(); unfocus(); setupClaimPending = false; setupInventoryFull = false; }
    boolean dropoffClaimResult(boolean granted) {
        if (!setupClaimPending) return false;
        setupClaimPending = false;
        setupInventoryFull = !granted;
        return granted;
    }
    boolean key(int key, int code, int modifiers) {
        if (setupRequired()) return false;
        if (key == 256) { unfocus(); return false; }
        if (search == null || !search.isFocused()) return false;
        search.keyPressed(key, code, modifiers);
        return true;
    }
    boolean character(char value, int modifiers) { return !setupRequired() && search != null && search.isFocused() && search.charTyped(value, modifiers); }
    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
    }
    private record Content(Component title, Component status, Component issuer, List<Component> paragraphs, int statusWidth, ArchiveCardLayout.Flow flow) {}
    private record Card(ArchiveQuestsPayload.QuestEntry quest, int x, int y, int width, int height, Content content) {}
    private record CategoryTab(int index, int x, int y, int width) {}
}
