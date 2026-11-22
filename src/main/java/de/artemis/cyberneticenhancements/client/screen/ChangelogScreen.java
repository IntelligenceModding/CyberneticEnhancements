package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import de.artemis.cyberneticenhancements.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Everything that changed in this release, shown once over the title screen. The text is the release's changelog file, bundled with
 * the mod as it is written, so this screen can never fall out of step with it. A new release brings a new {@link #CURRENT}.
 */
public final class ChangelogScreen extends ResponsiveScreen {
    /** The changelog this release shows. Raising it for the next release makes the popup appear once more. */
    public static final String CURRENT = "0.4-beta";
    private static final int LINE = 12, PAD = 16, HEADER = 52, FOOTER = 44;
    private static final long FADE_MILLIS = 250L;
    private final Screen parent;
    /** Opened from the title's menu rather than before it: the menu is already built, so it needs no fade-in on return. */
    private final boolean overMenu;
    private final long openedAt = net.minecraft.Util.getMillis();
    private long closingAt = -1L;
    private float opacity = 0;
    private final List<Entry> entries;
    private final List<Row> rows = new ArrayList<>();
    private final UiScrollState scroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private int panelX, panelY, panelWidth, panelHeight, bodyTop, bodyHeight, contentHeight;

    private enum Kind { TITLE, SECTION, GROUP, BULLET, TEXT }

    private record Entry(Kind kind, String text) {}
    private record Row(FormattedCharSequence text, int indent, int y, int color) {}

    public ChangelogScreen(Screen parent) {
        this(parent, false);
    }

    public ChangelogScreen(Screen parent, boolean overMenu) {
        super(Component.translatable("screen.cyberneticenhancements.changelog.title"));
        this.parent = parent;
        this.overMenu = overMenu;
        this.entries = load().map(ChangelogScreen::parse).orElse(List.of());
    }

    /** Whether there is a changelog to show for this release. */
    public static boolean available() {
        return resource() != null;
    }

    private static ResourceLocation resource() {
        var id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "changelog/" + CURRENT + ".md");
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : null;
    }

    private static Optional<String> load() {
        var id = resource();
        if (id == null) return Optional.empty();
        try (var in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).open();
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return Optional.of(String.join("\n", reader.lines().toList()));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Reads the changelog's own markdown: headings, bullets that continue on indented lines, and plain paragraphs. */
    static List<Entry> parse(String markdown) {
        var result = new ArrayList<Entry>();
        StringBuilder bullet = null;
        for (String raw : markdown.split("\n")) {
            String line = raw.stripTrailing();
            if (bullet != null && line.startsWith("  ") && !line.isBlank()) {
                bullet.append(' ').append(line.strip());
                continue;
            }
            if (bullet != null) { result.add(new Entry(Kind.BULLET, plain(bullet.toString()))); bullet = null; }
            if (line.isBlank()) continue;
            if (line.startsWith("### ")) result.add(new Entry(Kind.GROUP, plain(line.substring(4))));
            else if (line.startsWith("## ")) result.add(new Entry(Kind.SECTION, plain(line.substring(3))));
            else if (line.startsWith("# ")) result.add(new Entry(Kind.TITLE, plain(line.substring(2))));
            else if (line.startsWith("- ")) bullet = new StringBuilder(line.substring(2));
            else result.add(new Entry(Kind.TEXT, plain(line)));
        }
        if (bullet != null) result.add(new Entry(Kind.BULLET, plain(bullet.toString())));
        return result;
    }

    /** The formatting marks of markdown mean nothing in the game's font. */
    private static String plain(String text) {
        return text.replace("**", "").replace("`", "").replace("*", "").strip();
    }

    @Override public UiViewport viewport() {
        var window = Minecraft.getInstance().getWindow();
        return ResponsiveUi.referenceViewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @Override protected void init() {
        super.init();
        panelWidth = Math.min(692, width - 48);
        panelHeight = Math.max(120, height - 48);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        bodyTop = panelY + HEADER;
        bodyHeight = panelHeight - HEADER - FOOTER;
        layoutRows();
        scroll.clamp(maxScroll());
        addRenderableWidget(new Button(width / 2 - 64, panelY + panelHeight - 32, 128, 20, Component.translatable("gui.done"),
                button -> onClose(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
                FlatlinedPresentation.button(graphics, this, opacity);
            }
        });
    }

    private void layoutRows() {
        rows.clear();
        int textWidth = panelWidth - 2 * PAD - UiScrollbar.WIDTH - 8;
        int y = 0;
        for (Entry entry : entries) {
            int indent = switch (entry.kind) {
                case BULLET -> 20;
                case GROUP -> 10;
                default -> 0;
            };
            int color = switch (entry.kind) {
                case TITLE -> 0xFFFF5266;
                case SECTION -> 0xFFFF5266;
                case GROUP -> 0xFFFFD5DC;
                case BULLET -> 0xFFE6A3AE;
                case TEXT -> 0xFFA9707C;
            };
            if (entry.kind == Kind.SECTION) y += 8;
            else if (entry.kind == Kind.GROUP) y += 6;
            var wrapped = font.split(Component.literal((entry.kind == Kind.BULLET ? "• " : "") + entry.text), textWidth - indent);
            for (int i = 0; i < wrapped.size(); i++) {
                // Wrapped lines of a bullet line up under its text, not under its dot.
                rows.add(new Row(wrapped.get(i), indent + (entry.kind == Kind.BULLET && i > 0 ? font.width("• ") : 0), y, color));
                y += LINE;
            }
            y += entry.kind == Kind.BULLET ? 3 : 4;
        }
        contentHeight = y;
    }

    private double maxScroll() { return Math.max(0, contentHeight - bodyHeight); }

    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = net.minecraft.Util.getMillis();
        opacity = Math.clamp((now - openedAt) / (float) FADE_MILLIS, 0F, 1F);
        if (closingAt >= 0) opacity = Math.min(opacity, 1F - Math.clamp((now - closingAt) / (float) FADE_MILLIS, 0F, 1F));
        if (!TitleSlideshow.render(graphics, 1)) {
            renderPanorama(graphics, partialTick);
            TitleScreenPresentation.background(graphics, this, 1);
        }
        VanillaMenuPresentation.promptPanel(graphics, panelX, panelY, panelWidth, panelHeight, opacity);
        boolean visible = opacity > 0.02F;
        if (visible) {
            graphics.drawCenteredString(font, title, width / 2 + 2, panelY + 14 + 2, faded(0xA6000000));
            graphics.drawCenteredString(font, title, width / 2, panelY + 14, faded(0xFFFF5266));
            graphics.drawCenteredString(font, Component.translatable("screen.cyberneticenhancements.changelog.subtitle"),
                    width / 2, panelY + 28, faded(VanillaMenuPresentation.MUTED));
        }
        scroll.clamp(maxScroll());
        scroll.tick();
        int left = panelX + PAD, top = bodyTop, bottom = bodyTop + bodyHeight;
        graphics.enableScissor(panelX, top, panelX + panelWidth, bottom);
        int offset = (int) Math.round(scroll.current);
        for (Row row : rows) {
            int y = top + row.y - offset;
            if (!visible || y + LINE < top || y > bottom) continue;
            graphics.drawString(font, row.text, left + row.indent, y, faded(row.color), true);
        }
        graphics.disableScissor();
        var track = track();
        if (track.max() > 0 && opacity >= 1F) VanillaMenuPresentation.scrollbar(graphics, track, scrollbar.dragging() || track.contains(mouseX, mouseY));
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        if (closingAt >= 0 && now - closingAt >= FADE_MILLIS) {
            // The title was never shown, so its own first-open fade would start from black now; the menu fades in instead.
            if (parent instanceof de.artemis.cyberneticenhancements.client.TitleFadeAccess title) title.cyberneticenhancements$skipFade();
            if (!overMenu) de.artemis.cyberneticenhancements.client.TitleStartPrompt.fadeMenuIn();
            minecraft.setScreen(parent);
        }
    }

    private int faded(int argb) {
        // Font rendering treats alpha below 4 as opaque, so a nearly faded text is held at 4 and skipped once it is gone.
        return Math.max(4, Math.round((argb >>> 24) * opacity)) << 24 | argb & 0xFFFFFF;
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    private UiScrollbar.Track track() {
        return UiScrollbar.layout(panelX + panelWidth - PAD - UiScrollbar.WIDTH + 4, bodyTop, UiScrollbar.WIDTH, bodyHeight,
                false, bodyHeight, maxScroll(), scroll.current);
    }

    @Override protected boolean mouseClickedContent(double x, double y, int button) {
        if (button == 0 && scrollbar.begin(track(), x, y)) {
            scroll.set(scrollbar.value(track(), x, y), maxScroll());
            return true;
        }
        return super.mouseClickedContent(x, y, button);
    }

    @Override protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (!scrollbar.dragging()) return super.mouseDraggedContent(x, y, button, dx, dy);
        scroll.set(scrollbar.value(track(), x, y), maxScroll());
        return true;
    }

    @Override protected boolean mouseReleasedContent(double x, double y, int button) {
        scrollbar.release();
        return super.mouseReleasedContent(x, y, button);
    }

    @Override protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        scroll.add(-dy * UiScrollState.WHEEL_STEP, maxScroll());
        return true;
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        double page = bodyHeight - LINE;
        switch (key) {
            case 265 -> scroll.add(-UiScrollState.WHEEL_STEP, maxScroll());
            case 264 -> scroll.add(UiScrollState.WHEEL_STEP, maxScroll());
            case 266 -> scroll.add(-page, maxScroll());
            case 267 -> scroll.add(page, maxScroll());
            case 268 -> scroll.set(0, maxScroll());
            case 269 -> scroll.set(maxScroll(), maxScroll());
            default -> { return super.keyPressed(key, scanCode, modifiers); }
        }
        return true;
    }

    /** Fades out first; the title screen takes over once the panel is gone. */
    @Override public void onClose() {
        if (closingAt < 0) closingAt = net.minecraft.Util.getMillis();
    }
}
