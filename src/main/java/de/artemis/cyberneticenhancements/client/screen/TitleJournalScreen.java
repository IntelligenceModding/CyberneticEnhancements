package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.JournalHistoryStore;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;

/** Offline journal browser, styled like the red title menu. */
public final class TitleJournalScreen extends ResponsiveScreen {
    private static final int TEXT = VanillaMenuPresentation.TEXT, MUTED = VanillaMenuPresentation.MUTED, EDGE = VanillaMenuPresentation.EDGE;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
    private final Screen parent;
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private List<JournalHistoryStore.Source> sources = List.of(), visibleSources = List.of();
    private List<JournalHistoryStore.Entry> entries = List.of(), pageEntries = List.of();
    private JournalHistoryStore.Source selected;
    private JournalHistoryStore.Snapshot snapshot;
    private EditBox search;
    private Button categoryButton, orderButton, previousButton, nextButton;
    private String query = "";
    private int category, maximum, page, selectedRow = -1;
    private boolean newest = true, loading = true, started, scrolling;
    private Component error;

    public TitleJournalScreen(Screen parent) {
        super(Component.translatable("screen.cyberneticenhancements.title.journal"));
        this.parent = parent;
    }
    @Override public UiViewport viewport() {
        var window = Minecraft.getInstance().getWindow();
        return ResponsiveUi.referenceViewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // renderContent draws the backdrop before the journal. Screen.render must
        // not paint the inherited dark gradient over the entries a second time.
    }
    private Component text(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.title.journal." + key, args); }
    @Override protected void init() {
        super.init();
        int previousPage = page;
        search = addRenderableWidget(new EditBox(font, 50, 79, layout().searchWidth(selected != null) - 12, 12, text("search")));
        search.setBordered(false);
        search.setTextColor(TEXT); search.setHint(text("search")); search.setMaxLength(128); search.setValue(query);
        search.setResponder(value -> { query = value; filter(); });
        categoryButton = button(text("all"), layout().categoryX(), 72, 190, () -> { category = (category + 1) % (PlayerJournal.CATEGORIES.size() + 1); filter(); });
        orderButton = button(text("newest"), layout().orderX(), 72, 128, () -> { newest = !newest; filter(); });
        button(Component.translatable("gui.back"), 44, height - 42, 130, this::onClose);
        previousButton = button(archiveText("previous"), layout().right() - 266, height - 42, 128, () -> changePage(page - 1));
        nextButton = button(archiveText("next"), layout().orderX(), height - 42, 128, () -> changePage(page + 1));
        filter();
        changePage(previousPage);
        if (!started) { started = true; loadSources(); }
    }
    private Button button(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(new Button(x, y, w, 22, label, ignored -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float delta) {
                FlatlinedPresentation.button(graphics, this);
            }
        });
    }
    private void loadSources() {
        var servers = new ServerList(minecraft); servers.load();
        var known = new ArrayList<JournalHistoryStore.Source>();
        for (int i = 0; i < servers.size(); i++) {
            var server = servers.get(i);
            known.add(new JournalHistoryStore.Source(JournalHistoryStore.serverKey(server.ip), server.name, "server", null));
        }
        var game = minecraft.gameDirectory.toPath();
        var saves = minecraft.getLevelSource().getBaseDir();
        var player = minecraft.getUser().getProfileId();
        CompletableFuture.supplyAsync(() -> {
            try { return JournalHistoryStore.sources(game, saves, player, known); }
            catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, Util.ioPool()).whenCompleteAsync((result, failure) -> {
            loading = false;
            if (failure != null) error = text("load_error");
            else { sources = result; filter(); }
        }, minecraft);
    }
    private void open(JournalHistoryStore.Source source) {
        if (loading) return;
        selected = source; snapshot = null; error = null; loading = true; query = ""; search.setValue(""); filter();
        var game = minecraft.gameDirectory.toPath(); var player = minecraft.getUser().getProfileId();
        CompletableFuture.supplyAsync(() -> {
            try { return JournalHistoryStore.read(game, player, source); }
            catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, Util.ioPool()).whenCompleteAsync((result, failure) -> {
            if (selected != source) return;
            loading = false;
            if (failure != null) error = text("load_error"); else snapshot = result;
            filter();
        }, minecraft);
    }
    private void filter() {
        page = 0; selectedRow = -1;
        String needle = query.strip().toLowerCase(Locale.ROOT);
        visibleSources = sources.stream().filter(s -> (s.name() + " " + s.kind()).toLowerCase(Locale.ROOT).contains(needle)).toList();
        entries = snapshot == null ? List.of() : JournalHistoryStore.filter(snapshot.entries(), query,
                category == 0 ? "" : PlayerJournal.CATEGORIES.get(category - 1), newest);
        if (categoryButton != null) {
            categoryButton.visible = orderButton.visible = selected != null;
            search.setWidth(layout().searchWidth(selected != null) - 12);
            categoryButton.setMessage(category == 0 ? text("all") : Component.translatable("screen.cyberneticenhancements.archive.journal.category." + PlayerJournal.CATEGORIES.get(category - 1)));
            orderButton.setMessage(Component.translatable("screen.cyberneticenhancements.archive.journal." + (newest ? "newest" : "oldest")));
        }
        changePage(page);
    }
    private Component archiveText(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.archive.journal." + key, args); }
    private TitleJournalLayout layout() { return new TitleJournalLayout(width, height); }
    private void changePage(int requested) {
        page = Math.clamp(requested, 0, TitleJournalLayout.pageCount(entries.size()) - 1);
        int start = TitleJournalLayout.pageStart(entries.size(), page);
        pageEntries = entries.subList(start, Math.min(entries.size(), start + TitleJournalLayout.PAGE_SIZE));
        scroll.reset(); scrollbar.release(); maximum = 0;
        if (previousButton != null) {
            previousButton.visible = nextButton.visible = selected != null && entries.size() > TitleJournalLayout.PAGE_SIZE;
            previousButton.active = page > 0;
            nextButton.active = page + 1 < TitleJournalLayout.pageCount(entries.size());
        }
    }
    private int left() { return 44; }
    private int top() { return layout().top(); }
    private int contentWidth() { return layout().contentWidth(scrolling); }
    private int visibleHeight() { return layout().bodyHeight(); }
    private int rowHeight(JournalHistoryStore.Entry entry) { return 44 + font.split(Component.literal(entry.message()), contentWidth() - 24).size() * 12; }
    private UiScrollbar.Track track() {
        return UiScrollbar.layout(layout().scrollbarX(), top(), UiScrollbar.WIDTH, visibleHeight(), false, visibleHeight(), maximum, scroll.current);
    }

    @Override protected void renderContent(GuiGraphics g, int mx, int my, float delta) {
        if (!TitleSlideshow.render(g, 1)) {
            renderPanorama(g, delta);
            de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation.background(g, this, 1);
        }
        g.drawString(font, title, 44, 25, 0xFFFF5266);
        Component subtitle = selected == null ? text("select") : Component.literal(selected.name());
        g.drawString(font, font.plainSubstrByWidth(subtitle.getString(), width - 88), 44, 45, MUTED, false);
        UiRenderer.panel(g, 44, 72, layout().searchWidth(selected != null), 22, 0xE01C0810, search.isFocused() ? VanillaMenuPresentation.HIGHLIGHT : EDGE);
        scrolling = false;
        int total = contentHeight();
        if (total > visibleHeight()) { scrolling = true; total = contentHeight(); }
        maximum = Math.max(0, total - visibleHeight()); scroll.clamp(maximum); scroll.tick();
        g.enableScissor(left(), top(), left() + contentWidth(), top() + visibleHeight());
        if (loading || error != null) g.drawCenteredString(font, error == null ? text("loading") : error, width / 2, top() + 35, MUTED);
        else if (selected == null) {
            if (visibleSources.isEmpty()) g.drawCenteredString(font, text("no_sources"), width / 2, top() + 35, MUTED);
            int y = top() - (int) Math.round(scroll.current);
            for (int i = 0; i < visibleSources.size(); i++) {
                var source = visibleSources.get(i);
                if (y + 44 >= top() && y < top() + visibleHeight()) {
                    boolean hovered = mx >= left() && mx < left() + contentWidth() && my >= Math.max(y, top()) && my < Math.min(y + 44, top() + visibleHeight());
                    UiRenderer.panel(g, left(), y, contentWidth(), 44, hovered || selectedRow == i ? 0xE04A1220 : 0xC01C0810,
                            selectedRow == i ? VanillaMenuPresentation.ENTRY_SELECTED : hovered ? VanillaMenuPresentation.ENTRY_HOVER : VanillaMenuPresentation.ENTRY_EDGE);
                    g.drawString(font, font.plainSubstrByWidth(source.name(), contentWidth() - 24), left() + 12, y + 8, TEXT, false);
                    g.drawString(font, text(source.kind()), left() + 12, y + 25, MUTED, false);
                }
                y += 54;
            }
        } else {
            if (entries.isEmpty()) {
                Component message = snapshot != null && snapshot.updated() > 0 ? text("no_entries") : text(selected.kind().equals("server") ? "visit_server" : "visit_world");
                int y = top() + 24;
                for (var line : font.split(message, contentWidth() - 40)) { g.drawString(font, line, left() + 20, y, MUTED, false); y += 13; }
            }
            int y = top() - (int) Math.round(scroll.current);
            for (var entry : pageEntries) {
                int h = rowHeight(entry);
                if (y + h >= top() && y < top() + visibleHeight()) {
                    UiRenderer.panel(g, left(), y, contentWidth(), h, 0xD01C0810, VanillaMenuPresentation.ENTRY_EDGE);
                    long clock = Math.floorMod(entry.dayTime() + 6000, 24000);
                    String time = String.format(Locale.ROOT, "%02d:%02d", clock / 1000, clock % 1000 * 60 / 1000);
                    var heading = Component.translatable("screen.cyberneticenhancements.archive.journal.time", entry.dayTime() / 24000 + 1, time, DATE.format(Instant.ofEpochMilli(entry.timestamp())));
                    g.drawString(font, heading, left() + 12, y + 8, MUTED, false);
                    int row = y + 23;
                    for (var line : font.split(Component.literal(entry.message()), contentWidth() - 24)) { g.drawString(font, line, left() + 12, row, TEXT, false); row += 12; }
                    String location = entry.category() + " // " + entry.x() + ", " + entry.y() + ", " + entry.z() + " // " + entry.dimension();
                    g.drawString(font, font.plainSubstrByWidth(location, contentWidth() - 24), left() + 12, row + 7, MUTED, false);
                }
                y += h + 10;
            }
        }
        g.disableScissor();
        drawScrollbar(g, mx, my);
        if (selected != null && snapshot != null && snapshot.updated() > 0) {
            var status = text(snapshot.complete() ? "snapshot" : "partial", entries.size(), DATE.format(Instant.ofEpochMilli(snapshot.updated())));
            g.drawString(font, font.plainSubstrByWidth(status.getString(), width - 88), left(), height - 72, MUTED, false);
        }
        if (selected != null && entries.size() > TitleJournalLayout.PAGE_SIZE) {
            var label = archiveText("page", page + 1, TitleJournalLayout.pageCount(entries.size()));
            g.drawString(font, label, layout().right() - font.width(label), height - 58, MUTED, false);
        }
        super.renderContent(g, mx, my, delta);
    }
    private void drawScrollbar(GuiGraphics g, int mx, int my) {
        var track = track();
        if (track.max() <= 0) return;
        boolean highlighted = scrollbar.dragging() || track.contains(mx, my);
        g.fill(track.x(), track.y(), track.x() + track.width(), track.y() + track.height(), 0xFF200B13);
        UiRenderer.outline(g, track.x(), track.y(), track.width(), track.height(), VanillaMenuPresentation.SCROLL_EDGE);
        g.fill(track.x(), track.thumbStart(), track.x() + track.width(), track.thumbStart() + track.thumbLength(),
                highlighted ? 0xFFB94A63 : 0xFF803047);
        UiRenderer.outline(g, track.x(), track.thumbStart(), track.width(), track.thumbLength(),
                highlighted ? VanillaMenuPresentation.HIGHLIGHT : VanillaMenuPresentation.SCROLL_THUMB_EDGE);
    }
    private int contentHeight() {
        int total = selected == null ? visibleSources.size() * 54 : pageEntries.stream().mapToInt(e -> rowHeight(e) + 10).sum();
        return Math.max(0, total - 10);
    }
    @Override protected boolean mouseClickedContent(double x, double y, int button) {
        if (super.mouseClickedContent(x, y, button)) return true;
        if (button != 0 || loading) return false;
        if (scrollbar.begin(track(), x, y)) { scroll.set(scrollbar.value(track(), x, y), maximum); return true; }
        if (selected == null && x >= left() && x < left() + contentWidth() && y >= top() && y < top() + visibleHeight()) {
            double offset = y - top() + scroll.current;
            int index = (int) (offset / 54);
            if (offset % 54 < 44 && index >= 0 && index < visibleSources.size()) { open(visibleSources.get(index)); return true; }
        }
        return false;
    }
    @Override protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        if (y >= top() && y < top() + visibleHeight()) { scroll.add(-dy * UiScrollState.WHEEL_STEP, maximum); return true; }
        return super.mouseScrolledContent(x, y, dx, dy);
    }
    @Override protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (scrollbar.dragging()) { scroll.set(scrollbar.value(track(), x, y), maximum); return true; }
        return super.mouseDraggedContent(x, y, button, dx, dy);
    }
    @Override protected boolean mouseReleasedContent(double x, double y, int button) { scrollbar.release(); return super.mouseReleasedContent(x, y, button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (selected == null && !loading && !search.isFocused() && !visibleSources.isEmpty()) {
            if (key == 264 || key == 265) {
                selectedRow = Math.clamp(selectedRow + (key == 264 ? 1 : -1), 0, visibleSources.size() - 1);
                scroll.set(Math.clamp(selectedRow * 54 - visibleHeight() / 2.0, 0, maximum), maximum);
                return true;
            }
            if (key == 257 && selectedRow >= 0) { open(visibleSources.get(selectedRow)); return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void onClose() {
        if (selected == null) minecraft.setScreen(parent);
        else { selected = null; snapshot = null; error = null; loading = false; query = ""; search.setValue(""); filter(); }
    }
}
