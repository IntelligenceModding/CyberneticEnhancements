package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Revisit the startup notices at your own pace, using the same presentation and translations. */
public final class IntroNoticesScreen extends ResponsiveScreen {
    private static final LaunchIntroPresentation.NoticePage[] PAGES = LaunchIntroPresentation.NoticePage.values();
    private static final long FADE_MILLIS = 250L;
    private final Screen parent;
    private final long openedAt = net.minecraft.Util.getMillis();
    private long closingAt = -1L;
    private float opacity = 0;
    private int page;
    private Button previous, next, done;
    private int panelTop, panelWidth, panelHeight, pageNumberY;
    private final List<NoticeLine> lines = new ArrayList<>();
    private final List<Button> links = new ArrayList<>();
    private record NoticeLine(FormattedCharSequence text, int x, int y, int color) {}

    public IntroNoticesScreen(Screen parent) {
        super(Component.translatable("screen.cyberneticenhancements.title.notices"));
        this.parent = parent;
    }

    @Override public UiViewport viewport() {
        var window = Minecraft.getInstance().getWindow();
        return ResponsiveUi.referenceViewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @Override protected void init() {
        super.init();
        previous = button(Component.translatable("screen.cyberneticenhancements.archive.journal.previous"), width / 2 - 218, () -> changePage(-1));
        next = button(Component.translatable("screen.cyberneticenhancements.archive.journal.next"), width / 2 - 64, () -> changePage(1));
        done = button(Component.translatable("gui.done"), width / 2 + 90, this::onClose);
        updateButtons();
    }

    private Button button(Component label, int x, Runnable action) {
        return addRenderableWidget(new Button(x, height - 55, 128, 20, label, button -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
                FlatlinedPresentation.button(graphics, this, opacity);
            }
        });
    }

    private void changePage(int delta) {
        page = Math.clamp(page + delta, 0, PAGES.length - 1);
        updateButtons();
    }

    private void updateButtons() {
        previous.active = page > 0;
        next.active = page < PAGES.length - 1;
        rebuildLinks();
        if (minecraft != null) {
            var narration = PAGES[page].heading().copy();
            for (var paragraph : NoticeLinks.paragraphs(PAGES[page])) narration.append("\n\n").append(paragraph);
            minecraft.getNarrator().sayNow(narration);
        }
    }

    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!TitleSlideshow.render(graphics, 1)) {
            renderPanorama(graphics, partialTick);
            TitleScreenPresentation.background(graphics, this, 1);
        }
        long now = net.minecraft.Util.getMillis();
        opacity = Math.clamp((now - openedAt) / (float) FADE_MILLIS, 0F, 1F);
        if (closingAt >= 0) opacity = Math.min(opacity, 1F - Math.clamp((now - closingAt) / (float) FADE_MILLIS, 0F, 1F));
        VanillaMenuPresentation.promptPanel(graphics, (width - panelWidth) / 2, panelTop, panelWidth, panelHeight, opacity);
        if (opacity > 0.02F) {
            int top = panelTop + 16;
            graphics.drawCenteredString(font, PAGES[page].heading(), width / 2 + 2, top + 2, faded(0xA6000000));
            graphics.drawCenteredString(font, PAGES[page].heading(), width / 2, top, faded(0xFFFF5266));
            for (var line : lines) graphics.drawString(font, line.text(), line.x(), line.y(), faded(line.color()), true);
            graphics.drawCenteredString(font, Component.literal((page + 1) + " / " + PAGES.length), width / 2, pageNumberY, faded(VanillaMenuPresentation.MUTED));
        }
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        if (closingAt >= 0 && now - closingAt >= FADE_MILLIS) minecraft.setScreen(parent);
    }

    private int faded(int argb) {
        // Font rendering treats alpha below 4 as opaque.
        return Math.max(4, Math.round((argb >>> 24) * opacity)) << 24 | argb & 0xFFFFFF;
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 263 || key == 262) { changePage(key == 263 ? -1 : 1); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    /** Fades out first; the title screen takes over once the panel is gone. */
    @Override public void onClose() {
        if (closingAt < 0) closingAt = net.minecraft.Util.getMillis();
    }

    private void rebuildLinks() {
        if (links.contains(getFocused())) setFocused(null);
        links.forEach(this::removeWidget); links.clear(); lines.clear();
        panelWidth = Math.min(692, width - 48);
        var paragraphs = NoticeLinks.paragraphs(PAGES[page]).stream()
                .map(paragraph -> NoticeLinks.wrap(paragraph, font.getSplitter(), panelWidth - 32)).toList();
        int contentHeight = paragraphs.stream().mapToInt(group -> group.size() * 13).sum() + Math.max(0, paragraphs.size() - 1) * 14;
        panelHeight = 42 + contentHeight + 62;
        panelTop = Math.max(8, (height - panelHeight) / 2);
        pageNumberY = panelTop + 42 + contentHeight + 12;
        for (var button : List.of(previous, next, done)) button.setY(pageNumberY + 16);
        int y = panelTop + 42;
        for (int paragraph = 0; paragraph < paragraphs.size(); paragraph++) {
            for (var text : paragraphs.get(paragraph)) {
                var line = new NoticeLine(text, (width - font.width(text)) / 2, y,
                        paragraph == paragraphs.size() - 1 ? 0xFFA9707C : 0xFFE6A3AE);
                lines.add(line); addLinkSpans(line); y += 13;
            }
            y += 14;
        }
    }

    private void addLinkSpans(NoticeLine line) {
        for (var span : NoticeLinks.spans(line.text(), font.getSplitter())) {
            var button = new Button(line.x() + span.x(), line.y(), span.width(), font.lineHeight + 2,
                    span.label(), ignored -> openLink(span.style()), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float partial) {
                    if (isHoveredOrFocused()) graphics.fill(getX(), getY() + font.lineHeight,
                            getX() + getWidth(), getY() + font.lineHeight + 1, VanillaMenuPresentation.TEXT);
                }
            };
            String url = span.style().getClickEvent().getValue();
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(url.startsWith("https:")
                    ? Component.literal(url) : button.getMessage()));
            links.add(addRenderableWidget(button));
        }
    }

    private void openLink(Style style) {
        String destination = style.getClickEvent().getValue();
        if (destination.equals(NoticeLinks.SETTINGS)) minecraft.setScreen(new ModConfigScreen(this));
        else handleComponentClicked(style);
    }
}
