package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.client.ui.ClientSettingsModel.Entry;
import de.artemis.cyberneticenhancements.client.ui.LockIcon;
import de.artemis.cyberneticenhancements.client.ui.ClientSettingsModel.Section;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/** Archive-style navigation over the existing client spec; editing never renames config keys. */
public final class ModConfigScreen extends ResponsiveScreen {
    private static final int TEXT = VanillaMenuPresentation.TEXT, MUTED = VanillaMenuPresentation.MUTED,
            WARNING = 0xFFFFA5AD, PANEL = 0xE01C0810, ROW = 0xA0230C16, EDGE = VanillaMenuPresentation.PANEL_EDGE;
    private static final int LEFT = 24, NAV_WIDTH = 216, RIGHT = 250, TOP = 64, LIST_TOP = 112, ROW_HEIGHT = 82;
    private final Screen parent;
    private final ClientSettingsModel model = new ClientSettingsModel();
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final UiFadeLayer contentFade = new UiFadeLayer();
    private final Map<String, Boolean> expanded = new HashMap<>();
    private final Map<String, Float> expansion = new HashMap<>();
    private final List<NavButton> navigation = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<AbstractWidget> footer = new ArrayList<>();
    private Section section = Section.SCREENS;
    private String query = "";
    private ConfigEditBox search;
    private ConfigButton close;
    private ConfigButton apply, done, reset;
    private Component error;
    private long changedAt = System.nanoTime(), lastFrame = System.nanoTime();

    public ModConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        for (String group : List.of("presentation", "interface", "compass", "advanced")) {
            boolean open = !group.equals("advanced");
            expanded.put(group, open); expansion.put(group, open ? 1F : 0F);
        }
    }

    @Override public UiViewport viewport() {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        return ResponsiveUi.referenceViewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    private static void titlePlate(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int maximum, Component title) {
        UiRenderer.panel(g, x, y, maximum, 24, PANEL, EDGE);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), maximum - 20), x + 10, y + 8, TEXT, false);
    }

    private static void drawScrollbar(GuiGraphics g, UiScrollbar.Track track, double mx, double my, boolean dragging) {
        if (track.max() <= 0) return;
        g.fill(track.x(), track.y(), track.x() + track.width(), track.y() + track.height(), VanillaMenuPresentation.FIELD);
        UiRenderer.outline(g, track.x(), track.y(), track.width(), track.height(), VanillaMenuPresentation.SCROLL_EDGE);
        boolean hovered = track.contains(mx, my);
        g.fill(track.x(), track.thumbStart(), track.x() + track.width(), track.thumbStart() + track.thumbLength(),
                dragging || hovered ? 0xFF91485D : 0xFF633044);
        UiRenderer.outline(g, track.x(), track.thumbStart(), track.width(), track.thumbLength(), VanillaMenuPresentation.SCROLL_THUMB_EDGE);
    }

    private static final class ConfigButton extends Button {
        private boolean selected;
        ConfigButton(int x, int y, int width, int height, Component title, OnPress action) { super(x, y, width, height, title, action, DEFAULT_NARRATION); }
        void setSelected(boolean selected) { this.selected = selected; }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            renderString(g, net.minecraft.client.Minecraft.getInstance().font, FlatlinedPresentation.buttonFrame(g, this, 1, selected));
        }
    }

    private static final class ConfigEditBox extends EditBox {
        ConfigEditBox(net.minecraft.client.gui.Font font, int x, int y, int width, int height, Component title) {
            super(font, x, y, width, height, title); setBordered(false); setTextColor(TEXT); setTextColorUneditable(MUTED);
        }
        @Override public int getInnerWidth() { return Math.max(1, getWidth() - 12); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), VanillaMenuPresentation.FIELD);
            UiRenderer.outline(g, getX(), getY(), getWidth(), getHeight(), isFocused() && active ? VanillaMenuPresentation.HIGHLIGHT : EDGE);
            g.enableScissor(getX() + 4, getY() + 2, getX() + getWidth() - 4, getY() + getHeight() - 2);
            g.pose().pushPose(); g.pose().translate(6, (getHeight() - 8) / 2F, 0);
            super.renderWidget(g, mx - 6, my, partial);
            g.pose().popPose(); g.disableScissor();
        }
        @Override public boolean mouseClicked(double x, double y, int button) {
            if (!isMouseOver(x, y)) return super.mouseClicked(x, y, button);
            return super.mouseClicked(Math.max(getX(), Math.min(getX() + getInnerWidth(), x - 6)), y, button);
        }
    }

    private static Component text(String key, Object... args) { return Component.translatable("screen.cyberneticenhancements.config." + key, args); }
    private int rightWidth() { return width - RIGHT - 24; }
    private int bottom() { return height - 74; }
    private double maximum() { return Math.max(0, rows.size() * ROW_HEIGHT - (bottom() - LIST_TOP)); }

    @Override protected void init() {
        super.init();
        navigation.clear(); rows.clear(); footer.clear();
        search = addRenderableWidget(new ConfigEditBox(font, RIGHT, 20, rightWidth() - UiCloseControl.WIDTH - 12, 24, text("search")));
        close = addRenderableWidget(new ConfigButton(width - 24 - UiCloseControl.WIDTH, 20, UiCloseControl.WIDTH, 24,
                UiCloseControl.label(), button -> onClose()));
        search.setHint(text("search")); search.setMaxLength(120); search.setValue(query);
        search.setResponder(value -> {
            query = value;
            scroll.reset();
            // Live filtering should not replay the category transition for each keystroke.
            changedAt = 0;
            rebuildRows();
        });
        for (String group : List.of("presentation", "interface", "compass", "advanced")) {
            navigation.add(addRenderableWidget(new NavButton(group, null)));
            for (Section child : Section.values()) if (child.group.equals(group))
                navigation.add(addRenderableWidget(new NavButton(group, child)));
        }
        reset = addRenderableWidget(new ConfigButton(width - 412, height - 42, 120, 22, text("reset"), button -> {
            model.resetAll(); rebuildRows();
        }));
        apply = addRenderableWidget(new ConfigButton(width - 280, height - 42, 120, 22, text("apply"), button -> save(false)));
        done = addRenderableWidget(new ConfigButton(width - 148, height - 42, 124, 22, Component.translatable("gui.done"), button -> save(true)));
        footer.addAll(List.of(reset, apply, done));
        rebuildRows(); layout(0);
    }

    private void select(Section next) {
        section = next; query = ""; search.setValue(""); scroll.reset();
        changedAt = System.nanoTime(); rebuildRows();
        minecraft.getNarrator().sayNow(text("section." + next.id()));
    }

    private List<Entry> shown() {
        String filter = query.trim().toLowerCase(Locale.ROOT);
        return model.entries().stream().filter(entry -> filter.isEmpty() ? ClientSettingsModel.section(entry.key) == section
                : Component.translatable(entry.translation()).getString().toLowerCase(Locale.ROOT).contains(filter)
                || description(entry).getString().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    private Component description(Entry entry) {
        return Component.translatableWithFallback(entry.translation() + ".tooltip", entry.spec.getComment() == null ? "" : entry.spec.getComment());
    }

    private void rebuildRows() {
        for (var row : rows) for (var control : row.controls) removeWidget(control);
        rows.clear();
        if (getFocused() != search) setFocused(null);
        for (var entry : shown()) rows.add(new Row(entry));
        scroll.clamp(maximum());
        layoutRows();
    }

    private void layoutRows() {
        int index = 0;
        for (var row : rows) {
            row.y = LIST_TOP + index++ * ROW_HEIGHT - (int) Math.round(scroll.current);
            int controlY = row.y + 26;
            for (int i = 0; i < row.controls.size(); i++) {
                var widget = row.controls.get(i);
                widget.setY(controlY);
                widget.visible = controlY >= LIST_TOP && controlY + widget.getHeight() <= bottom();
                if (row.entry.locked()) widget.active = false;
                else if (i == 1) widget.active = !Objects.equals(row.entry.draft(), row.entry.value.getDefault()) || !row.entry.valid();
            }
        }
    }

    private void layout(double seconds) {
        double rate = 1 - Math.exp(-seconds / 0.08);
        scroll.current += (scroll.target - scroll.current) * rate;
        if (Math.abs(scroll.current - scroll.target) < 0.1) scroll.current = scroll.target;
        scroll.clamp(maximum());
        for (var group : expanded.keySet()) {
            float value = expansion.get(group), target = expanded.get(group) ? 1 : 0;
            value += (target - value) * (float) rate;
            if (Math.abs(value - target) < 0.001F) value = target;
            expansion.put(group, value);
        }
        float navY = TOP + 12;
        for (var button : navigation) {
            float amount = button.child == null ? 1 : expansion.get(button.group);
            button.setX(LEFT + (button.child == null ? 8 : 20));
            button.setY(Math.round(navY));
            button.visible = amount > 0.02F;
            button.active = amount > 0.9F;
            button.setAlpha(amount);
            navY += button.child == null ? 30 : 26 * amount;
            button.emphasis += ((button.isHoveredOrFocused() ? 1 : 0) - button.emphasis) * (float) rate;
        }
        layoutRows();
        apply.active = ClientConfig.SPEC.isLoaded() && model.dirty() && model.valid();
        done.active = ClientConfig.SPEC.isLoaded() && model.valid();
        reset.active = model.entries().stream().anyMatch(entry -> !entry.locked() && (!Objects.equals(entry.draft(), entry.value.getDefault()) || !entry.valid()));
    }

    @Override protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        layout(Math.clamp((now - lastFrame) / 1_000_000_000.0, 0, 0.1)); lastFrame = now;
        g.fillGradient(0, 0, width, height, 0xF00D050A, 0xF0180810);
        titlePlate(g, font, LEFT, 20, NAV_WIDTH, title);
        for (int line = 0; line < height; line += 4) g.fill(0, line, width, line + 1, 0x10000000);
        UiRenderer.panel(g, LEFT, TOP, NAV_WIDTH, height - TOP - 64, PANEL, EDGE);
        UiRenderer.panel(g, RIGHT, TOP, rightWidth(), height - TOP - 64, PANEL, EDGE);
        for (var button : navigation) button.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, query.isBlank() ? text("section." + section.id()) : text("results"), RIGHT + 12, TOP + 16, TEXT, false);
        if (model.locked() > 0) {
            // The server's rules, not a fault: shown in the header so locked rows are never a surprise.
            Component notice = text("enforced", model.locked());
            int noticeX = width - 36 - font.width(notice);
            LockIcon.render(g, noticeX - LockIcon.WIDTH - 6, TOP + 14, false);
            g.drawString(font, notice, noticeX, TOP + 16, WARNING, false);
        }
        search.render(g, mouseX, mouseY, partialTick);
        close.render(g, mouseX, mouseY, partialTick);
        float t = (float) Math.clamp((now - changedAt) / 180_000_000.0, 0, 1);
        float opacity = minecraft.options.screenEffectScale().get() <= 0 ? 1 : 1 - (float) Math.pow(1 - t, 3);
        if (opacity < 1) contentFade.render(g, opacity, 1, 0, () -> renderRows(g, mouseX, mouseY, partialTick));
        else renderRows(g, mouseX, mouseY, partialTick);
        for (var button : footer) button.render(g, mouseX, mouseY, partialTick);
        Component status = error != null ? error : !model.valid() ? text("invalid") : text("changes", model.changes());
        g.drawString(font, status, LEFT, height - 59, error != null || !model.valid() ? WARNING : MUTED, false);
    }

    private void renderRows(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.enableScissor(RIGHT + 6, LIST_TOP, width - 30, bottom());
        if (rows.isEmpty()) g.drawString(font, text(ClientConfig.SPEC.isLoaded() ? "no_results" : "not_loaded"), RIGHT + 12, LIST_TOP + 12, MUTED, false);
        for (var row : rows) {
            if (row.y + ROW_HEIGHT <= LIST_TOP || row.y >= bottom()) continue;
            int x = RIGHT + 10, labelWidth = rightWidth() - 266;
            g.fill(x, row.y, width - 42, row.y + ROW_HEIGHT - 6, ROW);
            Component label = Component.translatable(row.entry.translation());
            g.drawString(font, font.plainSubstrByWidth(label.getString(), labelWidth), x + 8, row.y + 9, TEXT, false);
            var lines = font.split(description(row.entry), labelWidth);
            for (int line = 0; line < Math.min(3, lines.size()); line++)
                g.drawString(font, lines.get(line), x + 8, row.y + 25 + line * 11, MUTED, false);
            if (!row.entry.valid()) g.drawString(font, text("invalid_value"), width - 262, row.y + 55, WARNING, false);
            for (var control : row.controls) control.render(g, mouseX, mouseY, partialTick);
            if (row.entry.locked()) {
                int lockX = width - 262 - LockIcon.WIDTH - 8, lockY = row.y + 26 + (22 - LockIcon.HEIGHT) / 2;
                boolean over = LockIcon.contains(lockX, lockY, mouseX, mouseY) && mouseY >= LIST_TOP && mouseY < bottom();
                LockIcon.render(g, lockX, lockY, over);
                if (over) setTooltipForNextRenderPass(text("locked"));
            }
        }
        g.disableScissor();
        drawScrollbar(g, track(), mouseX, mouseY, scrollbar.dragging());
    }

    private UiScrollbar.Track track() { return UiScrollbar.layout(width - 36, LIST_TOP, 6, bottom() - LIST_TOP, false, bottom() - LIST_TOP, maximum(), scroll.current); }

    private void save(boolean close) {
        try { model.save(); error = null; rebuildRows(); if (close) minecraft.setScreen(parent); }
        catch (RuntimeException failure) { error = text("save_failed"); com.mojang.logging.LogUtils.getLogger().error("Unable to save client settings", failure); }
    }

    @Override public void onClose() {
        if (!model.dirty()) minecraft.setScreen(parent);
        else minecraft.setScreen(new DiscardScreen());
    }
    @Override public void removed() { contentFade.close(); }
    @Override protected boolean mouseClickedContent(double x, double y, int button) {
        if (button == 0 && scrollbar.begin(track(), x, y)) { scroll.set(scrollbar.value(track(), x, y), maximum()); layoutRows(); return true; }
        if (search != null && !search.isMouseOver(x, y) && getFocused() == search) setFocused(null);
        return super.mouseClickedContent(x, y, button);
    }
    @Override protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (button == 0 && scrollbar.dragging()) { scroll.set(scrollbar.value(track(), x, y), maximum()); layoutRows(); return true; }
        return super.mouseDraggedContent(x, y, button, dx, dy);
    }
    @Override protected boolean mouseReleasedContent(double x, double y, int button) { scrollbar.release(); return super.mouseReleasedContent(x, y, button); }
    @Override protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        if (x >= RIGHT && y >= LIST_TOP && y < bottom()) { scroll.add(-dy * 36, maximum()); return true; }
        return super.mouseScrolledContent(x, y, dx, dy);
    }

    private final class Row {
        final Entry entry;
        final List<AbstractWidget> controls = new ArrayList<>();
        int y;
        Row(Entry entry) {
            this.entry = entry;
            AbstractWidget value;
            if (entry.value.getDefault() instanceof Boolean) {
                var toggle = new ConfigButton(width - 262, 0, 142, 22, toggleLabel(entry), button -> {
                    entry.set(!Boolean.TRUE.equals(entry.draft())); button.setMessage(toggleLabel(entry)); ((ConfigButton) button).setSelected(Boolean.TRUE.equals(entry.draft()));
                });
                toggle.setSelected(Boolean.TRUE.equals(entry.draft())); value = toggle;
            } else if (entry.value.getDefault() instanceof List<?>) {
                value = new ConfigButton(width - 262, 0, 142, 22, text("edit_list", ((List<?>) entry.draft()).size()),
                        button -> minecraft.setScreen(new ListEditor(entry)));
            } else {
                var field = new ConfigEditBox(font, width - 262, 0, 142, 22, Component.translatable(entry.translation()));
                field.setMaxLength(32); field.setValue(String.valueOf(entry.draft()));
                field.setResponder(valueText -> { entry.number(valueText); field.setTextColor(entry.valid() ? TEXT : WARNING); });
                value = field;
            }
            var tooltip = entry.locked() ? text("locked").copy().withStyle(net.minecraft.ChatFormatting.RED).append("\n\n").append(description(entry)) : description(entry).copy();
            if (entry.spec.getRange() != null) tooltip.append("\n").append(text("range", entry.spec.getRange().toString()));
            value.setTooltip(Tooltip.create(tooltip));
            controls.add(addRenderableWidget(value));
            controls.add(addRenderableWidget(new ConfigButton(width - 112, 0, 66, 22, text("reset"), button -> { entry.reset(); rebuildRows(); })));
        }
    }

    private Component toggleLabel(Entry entry) { return Component.translatable(Boolean.TRUE.equals(entry.draft()) ? "options.on" : "options.off"); }

    private final class NavButton extends Button {
        final String group;
        final Section child;
        float emphasis;
        NavButton(String group, Section child) {
            super(0, 0, child == null ? NAV_WIDTH - 16 : NAV_WIDTH - 28, 24,
                    text((child == null ? "group." + group : "section." + child.id())), ignored -> {}, DEFAULT_NARRATION);
            this.group = group; this.child = child;
        }
        @Override public void onPress() {
            if (child == null) expanded.put(group, !expanded.get(group)); else select(child);
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            int mask = Math.round(alpha * 255) << 24 | 0xFFFFFF;
            int fill = UiRenderer.blend(ROW, 0xE05C1A30, child == section ? 0.8F : emphasis * 0.5F);
            int fadedFill = Math.round((fill >>> 24) * alpha) << 24 | fill & 0xFFFFFF;
            UiRenderer.panel(g, getX(), getY(), getWidth(), getHeight(), fadedFill,
                    (child == section ? VanillaMenuPresentation.ENTRY_SELECTED : VanillaMenuPresentation.ENTRY_EDGE) & mask);
            int textX = getX() + 8;
            if (child == null) { UiRenderer.disclosureChevron(g, textX + 4, getY() + 12, expansion.get(group), MUTED); textX += 16; }
            g.drawString(font, getMessage(), textX, getY() + 8, (child == section ? TEXT : MUTED) & mask, false);
        }
    }

    private final class DiscardScreen extends ResponsiveScreen {
        DiscardScreen() { super(text("discard_title")); }
        @Override public UiViewport viewport() { return ModConfigScreen.this.viewport(); }
        @Override protected void init() {
            super.init();
            addRenderableWidget(new ConfigButton(width / 2 - 160, height / 2 + 28, 150, 22, text("keep_editing"), button -> onClose()));
            addRenderableWidget(new ConfigButton(width / 2 + 10, height / 2 + 28, 150, 22, text("discard"), button -> minecraft.setScreen(parent)));
        }
        @Override protected void renderContent(GuiGraphics g, int mx, int my, float partial) {
            renderBackground(g, mx, my, partial);
            UiRenderer.panel(g, width / 2 - 190, height / 2 - 65, 380, 132, PANEL, EDGE);
            g.drawCenteredString(font, title, width / 2, height / 2 - 42, TEXT);
            g.drawCenteredString(font, text("discard_message"), width / 2, height / 2 - 18, MUTED);
            // Buttons render through the native focus/input contract.
            for (var widget : renderables) widget.render(g, mx, my, partial);
        }
        @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) { g.fill(0, 0, width, height, 0xF00D050A); }
        @Override public void onClose() { minecraft.setScreen(ModConfigScreen.this); }
    }

    private final class ListEditor extends ResponsiveScreen {
        final Entry entry;
        final List<String> values;
        final List<AbstractWidget> controls = new ArrayList<>();
        final UiScrollState listScroll = new UiScrollState();
        final UiScrollbar listBar = new UiScrollbar();
        long previousFrame = System.nanoTime();
        ConfigButton accept;
        ListEditor(Entry entry) {
            super(Component.translatable(entry.translation())); this.entry = entry;
            values = new ArrayList<>(((List<?>) entry.draft()).stream().map(String::valueOf).toList());
        }
        @Override public UiViewport viewport() { return ModConfigScreen.this.viewport(); }
        int listBottom() { return height - 84; }
        double max() { return Math.max(0, values.size() * 32 - (listBottom() - 100)); }
        @Override protected void init() {
            super.init(); controls.clear(); listScroll.clamp(max());
            for (int i = 0; i < values.size(); i++) {
                int index = i;
                var field = new ConfigEditBox(font, 46, 0, width - 270, 22, text("list_entry", i + 1));
                field.setMaxLength(32767); field.setValue(values.get(i)); field.setResponder(value -> values.set(index, value));
                controls.add(addRenderableWidget(field));
                controls.add(addRenderableWidget(new ConfigButton(width - 214, 0, 42, 22, Component.literal("\u2191"), button -> move(index, -1))));
                controls.add(addRenderableWidget(new ConfigButton(width - 166, 0, 42, 22, Component.literal("\u2193"), button -> move(index, 1))));
                controls.add(addRenderableWidget(new ConfigButton(width - 118, 0, 66, 22, text("delete"), button -> { values.remove(index); rebuild(); })));
            }
            addRenderableWidget(new ConfigButton(36, height - 52, 138, 22, text("add_entry"), button -> { values.add(""); listScroll.set(max(), max()); rebuild(); }));
            addRenderableWidget(new ConfigButton(width - 316, height - 52, 132, 22, Component.translatable("gui.cancel"), button -> onClose()));
            accept = addRenderableWidget(new ConfigButton(width - 174, height - 52, 138, 22, text("use_changes"), button -> { entry.set(values); onClose(); }));
            position();
        }
        void rebuild() { setFocused(null); clearWidgets(); init(); }
        void move(int index, int delta) { int next = index + delta; if (next >= 0 && next < values.size()) { Collections.swap(values, index, next); rebuild(); } }
        void position() {
            for (int i = 0; i < controls.size(); i++) {
                var widget = controls.get(i); int row = i / 4;
                widget.setY(100 + row * 32 - (int) Math.round(listScroll.current));
                widget.visible = widget.getY() >= 100 && widget.getY() + 22 <= listBottom();
                widget.active = i % 4 == 1 ? row > 0 : i % 4 != 2 || row + 1 < values.size();
            }
            accept.active = entry.spec.test(values);
        }
        @Override protected void renderContent(GuiGraphics g, int mx, int my, float partial) {
            long now = System.nanoTime(); double dt = Math.clamp((now - previousFrame) / 1_000_000_000.0, 0, 0.1); previousFrame = now;
            listScroll.current += (listScroll.target - listScroll.current) * (1 - Math.exp(-dt / 0.08)); position();
            g.fill(0, 0, width, height, 0xF00D050A);
            titlePlate(g, font, 36, 22, width - 72, title);
            UiRenderer.panel(g, 36, 78, width - 72, height - 146, PANEL, EDGE);
            g.drawString(font, text("list_help"), 46, 58, MUTED, false);
            for (var widget : renderables) widget.render(g, mx, my, partial);
            drawScrollbar(g, listTrack(), mx, my, listBar.dragging());
        }
        UiScrollbar.Track listTrack() { return UiScrollbar.layout(width - 48, 100, 6, listBottom() - 100, false, listBottom() - 100, max(), listScroll.current); }
        @Override protected boolean mouseScrolledContent(double x, double y, double dx, double dy) { listScroll.add(-dy * 36, max()); return true; }
        @Override protected boolean mouseClickedContent(double x, double y, int button) {
            if (button == 0 && listBar.begin(listTrack(), x, y)) { listScroll.set(listBar.value(listTrack(), x, y), max()); position(); return true; }
            return super.mouseClickedContent(x, y, button);
        }
        @Override protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
            if (button == 0 && listBar.dragging()) { listScroll.set(listBar.value(listTrack(), x, y), max()); position(); return true; }
            return super.mouseDraggedContent(x, y, button, dx, dy);
        }
        @Override protected boolean mouseReleasedContent(double x, double y, int button) { listBar.release(); return super.mouseReleasedContent(x, y, button); }
        @Override public void onClose() { minecraft.setScreen(ModConfigScreen.this); }
    }
}
