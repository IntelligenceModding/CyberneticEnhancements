package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.config.SkillsConfig;
import de.artemis.cyberneticenhancements.common.origin.Origin;
import de.artemis.cyberneticenhancements.common.origin.OriginChoosePayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The one-time choice of a lifepath. It cannot be dismissed: the city wants to know who walked in.
 * It takes the whole screen, with a tall card per lifepath that has room for artwork above the text. The art for a lifepath is
 * read from {@code textures/gui/origin/<id>.png} (any size, cropped to fill); without one a placeholder is drawn.
 */
public final class OriginScreen extends ResponsiveScreen {
    private static final int GAP = 16, PAD = 12, LINE = 11, MARGIN = 36, HEADER = 84, FOOTER = 76, MIN_ART = 120;
    private static final int SKILLS_GAP = 8, SKILLS_HEADING = 20, SKILL_ROW = 14;
    /** Width over height of the lifepath pictures. */
    private static final float ART_RATIO = 800F / 1080F;
    // All three PNG borders start the lower-right bevel at (792, 1008), then run 60 px left over 57 px down.
    private static final float ART_RIGHT_INSET = 8F / 800F;
    private static final float ART_CUT_START = 1008F / 1080F;
    private static final float ART_CUT_SLOPE = (60F / 800F) / (57F / 1080F);
    private static final long FADE_IN = 700, CARD_DELAY = 350, CARD_STAGGER = 170, CARD_TIME = 650, EXIT_TIME = 450, EXIT_STAGGER = 110, EXIT_HEADING = 350;
    private final int count = Origin.values().length;
    private final long opened = Util.getMillis();
    private final float[] hover = new float[count], chosen = new float[count];
    private final FadingButton[] choose = new FadingButton[count];
    private final Map<Origin, Art> art = new EnumMap<>(Origin.class);
    private final Map<Origin, Lines> lines = new EnumMap<>(Origin.class);
    private float confirmation;
    private long lastFrame = opened, closing;
    private Origin selected;
    private FadingButton confirm, back;
    private List<FormattedCharSequence> subheading = List.of();
    private int cardWidth, cardHeight, top, left, artHeight, foot;

    public OriginScreen() {
        super(Component.translatable("screen.cyberneticenhancements.origin.title"));
    }

    @Override protected void init() {
        super.init();
        cardWidth = (width - 2 * MARGIN - (count - 1) * GAP) / count;
        top = HEADER;
        int available = Math.max(height - HEADER - FOOTER, 0);
        // The pictures bring their own border, so the card takes their shape exactly.
        cardWidth = Math.min(cardWidth, Math.round(available * ART_RATIO));
        int textWidth = cardWidth - 2 * PAD;
        int tallest = 0;
        for (Origin origin : Origin.values()) tallest = Math.max(tallest, contentHeight(origin, textWidth));
        // Fade the text backdrop near the card's lower edge.
        foot = Math.round(Math.min(available, cardWidth / ART_RATIO) * 0.105F);
        int needed = tallest + 20 + 2 * PAD;
        cardHeight = Math.max(Math.min(available, Math.round(cardWidth / ART_RATIO)), needed + MIN_ART);
        // The text sits at the foot of the card; the picture shows clear above it.
        artHeight = cardHeight - needed;
        left = (width - (count * cardWidth + (count - 1) * GAP)) / 2;
        subheading = wrap(Component.translatable("screen.cyberneticenhancements.origin.subheading"), width - 2 * MARGIN);
        for (Origin origin : Origin.values()) {
            if (!art.containsKey(origin)) {
                Art picture = Art.load(origin);
                art.put(origin, picture);
                // Uploaded now, while nothing is moving, rather than on the first frame of a card's entrance.
                if (picture != null) minecraft.getTextureManager().getTexture(picture.texture);
            }
            lines.put(origin, new Lines(wrap(origin.tagline(), textWidth), wrap(origin.story(), textWidth),
                    origin.basePerks().stream().map(perk -> wrap(perk, textWidth - 8)).toList()));
        }
        clearWidgets();
        for (int i = 0; i < count; i++) {
            Origin origin = Origin.values()[i];
            choose[i] = addRenderableWidget(new FadingButton(left + i * (cardWidth + GAP) + PAD, top + cardHeight - PAD - 20, textWidth, 20,
                    Component.translatable("screen.cyberneticenhancements.origin.choose", origin.title()), () -> selected = origin));
            if (art.get(origin) != null) {
                float slope = ART_CUT_SLOPE * cardWidth / cardHeight;
                float sideGap = PAD - ART_RIGHT_INSET * cardWidth;
                float bevelOffset = sideGap * (float) ((Math.hypot(1, slope) - 1) / slope);
                choose[i].cutStartY = top + ART_CUT_START * cardHeight - bevelOffset;
                choose[i].cutSlope = slope;
            }
        }
        confirm = addRenderableWidget(new FadingButton(width / 2 - 154, top + cardHeight + 36, 150, 20,
                Component.translatable("screen.cyberneticenhancements.origin.confirm"), this::confirm));
        back = addRenderableWidget(new FadingButton(width / 2 + 4, top + cardHeight + 36, 150, 20,
                Component.translatable("screen.cyberneticenhancements.origin.back"), () -> selected = null));
    }

    /** A card's text, wrapped once per layout instead of on every frame. */
    private record Lines(List<FormattedCharSequence> tagline, List<FormattedCharSequence> story, List<List<FormattedCharSequence>> perks) {}

    /** A lifepath's artwork; the size is kept so it can be cropped to fill. */
    private record Art(ResourceLocation texture, int width, int height) {
        static Art load(Origin origin) {
            var id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/gui/origin/" + origin.id() + ".png");
            var resource = Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) return null;
            try (var in = resource.get().open(); var image = NativeImage.read(in)) {
                return new Art(id, image.getWidth(), image.getHeight());
            } catch (java.io.IOException | RuntimeException e) {
                return null;
            }
        }
    }

    private final class FadingButton extends Button {
        /** Vertical offset in fractional pixels, so motion never snaps to whole GUI pixels. */
        float shift;
        float cutStartY = Float.POSITIVE_INFINITY, cutSlope;

        FadingButton(int x, int y, int width, int height, Component label, Runnable action) {
            super(x, y, width, height, label, button -> { if (closing == 0) action.run(); }, narration -> narration.get());
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            if (alpha <= 0.02F) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0, shift, 0);
            if (cutSlope > 0) {
                isHovered = isMouseOver(mx, my);
                FlatlinedPresentation.cutCornerButton(graphics, this, alpha, cutStartY, cutSlope);
            } else {
                FlatlinedPresentation.button(graphics, this, alpha);
            }
            graphics.pose().popPose();
        }

        @Override public boolean isMouseOver(double x, double y) {
            return cutSlope == 0 ? super.isMouseOver(x, y)
                    : super.isMouseOver(x, y - shift) && insideCorner(x, y - shift);
        }

        @Override protected boolean clicked(double x, double y) {
            return cutSlope == 0 ? super.clicked(x, y)
                    : super.clicked(x, y - shift) && insideCorner(x, y - shift);
        }

        private boolean insideCorner(double x, double y) {
            return x < FlatlinedPresentation.cutCornerRight(getX() + getWidth(), y, cutStartY, cutSlope);
        }
    }

    private void confirm() {
        if (selected == null) return;
        PacketDistributor.sendToServer(new OriginChoosePayload(selected.id()));
        closing = Util.getMillis();
    }

    private List<FormattedCharSequence> wrap(Component text, int width) { return font.split(text, width); }

    private int contentHeight(Origin origin, int textWidth) {
        int lines = 1 + wrap(origin.tagline(), textWidth).size() + wrap(origin.story(), textWidth).size();
        for (Component perk : origin.basePerks()) lines += wrap(perk, textWidth - 8).size();
        int startingSkillsHeight = SkillsConfig.enabled() ? startingSkillsHeight(origin) + 2 * SKILLS_GAP : SKILLS_GAP;
        return lines * LINE + 18 + startingSkillsHeight;
    }

    private static int startingSkillsHeight(Origin origin) {
        return SKILLS_HEADING + origin.startingRanks().size() * SKILL_ROW + 4;
    }

    private void renderStartingSkills(GuiGraphics graphics, Origin origin, int x, int buttonY, int width, float alpha) {
        int y = buttonY - SKILLS_GAP - startingSkillsHeight(origin);
        int right = x + width;
        int bottom = buttonY - SKILLS_GAP;
        graphics.fill(x, y, right, bottom, tint(0xE622111B, alpha));
        graphics.fill(x, y, right, y + 1, tint(0xFF774052, alpha));
        text(graphics, Component.translatable("screen.cyberneticenhancements.origin.starting_skills_heading"),
                x + 8, y + 6, 0xFFFFB3BC, alpha, false);
        y += SKILLS_HEADING;
        for (var starting : origin.startingRanks()) {
            graphics.fill(x + 8, y - 1, right - 8, y, tint(0xFF462A37, alpha));
            String rank = "+" + Math.min(starting.rank(), SkillsConfig.maxRank());
            int rankX = right - 8 - font.width(rank);
            String name = font.plainSubstrByWidth(starting.skill().title().getString(), Math.max(0, rankX - x - 20));
            text(graphics, name, x + 8, y + 2, VanillaMenuPresentation.TEXT, alpha, false);
            text(graphics, rank, rankX, y + 2, 0xFF5CE0FF, alpha, false);
            y += SKILL_ROW;
        }
    }

    private static float ease(float t) { t = Math.clamp(t, 0, 1); return 1 - (1 - t) * (1 - t) * (1 - t); }

    private static int tint(int argb, float alpha) {
        return Math.round((argb >>> 24) * Math.clamp(alpha, 0, 1)) << 24 | argb & 0xFFFFFF;
    }

    private static boolean visible(int argb) { return (argb >>> 24) > 3; }

    private void text(GuiGraphics graphics, Object text, int x, int y, int color, float alpha, boolean shadow) {
        int shown = tint(color, alpha);
        if (!visible(shown)) return;
        if (text instanceof FormattedCharSequence sequence) graphics.drawString(font, sequence, x, y, shown, shadow);
        else if (text instanceof Component component) graphics.drawString(font, component, x, y, shown, shadow);
        else graphics.drawString(font, (String) text, x, y, shown, shadow);
    }

    /** Eases every animated value a little closer to where it belongs, independent of the frame rate. */
    private void advance(int mouseX, int mouseY, long now) {
        float step = 1 - (float) Math.exp(-(now - lastFrame) / 110.0);
        lastFrame = now;
        confirmation += ((selected != null ? 1 : 0) - confirmation) * step;
        for (int i = 0; i < count; i++) {
            int x = left + i * (cardWidth + GAP);
            boolean over = selected == null && closing == 0 && mouseX >= x && mouseX < x + cardWidth && mouseY >= top - 8 && mouseY < top + cardHeight;
            hover[i] += ((over ? 1 : 0) - hover[i]) * step;
            chosen[i] += ((Origin.values()[i] == selected ? 1 : 0) - chosen[i]) * step;
        }
    }

    /** Draws the picture cropped to fill the box, or a placeholder where there is none yet. */
    private void artwork(GuiGraphics graphics, Origin origin, int x, int y, int w, int h, float alpha, float glow) {
        var picture = art.get(origin);
        if (picture == null) {
            graphics.fillGradient(x, y, x + w, y + h, tint(0xFF3A1420, alpha), tint(0xFF120810, alpha));
            for (int line = y + 2; line < y + h; line += 4) graphics.fill(x, line, x + w, line + 1, tint(0x14FFFFFF, alpha));
            String initial = origin.title().getString().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.pose().pushPose();
            graphics.pose().translate(x + w / 2F, y + h / 2F - 24, 0);
            graphics.pose().scale(6, 6, 1);
            text(graphics, initial, -font.width(initial) / 2, 0, 0xFFFF5266, alpha * (0.35F + 0.25F * glow), false);
            graphics.pose().popPose();
        } else {
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha * (0.78F + 0.22F * glow));
            graphics.blit(picture.texture, x, y, w, h, 0, 0, picture.width, picture.height, picture.width, picture.height);
            graphics.flush();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        // The picture is the card's backdrop; it darkens behind the text so that stays readable, inside the picture's own border.
        int inset = Math.max(2, Math.round(w * ART_RIGHT_INSET)), fade = y + artHeight, right = x + w - inset, solid = y + h - foot;
        int dark = tint(0xD8100610, alpha);
        graphics.fillGradient(x + inset, fade - 70, right, fade, tint(0x00100610, alpha), dark);
        graphics.fill(x + inset, fade, right, solid, dark);
        // Leaves the picture's own bottom edge, marker and cut corner showing.
        graphics.fillGradient(x + inset, solid, right, solid + foot / 2, dark, tint(0x00100610, alpha));
    }

    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        advance(mouseX, mouseY, now);
        float screenIn = ease((now - opened) / (float) FADE_IN);
        // Leaving: the cards you passed over go first, the chosen one last, then the heading follows.
        long sinceClose = closing == 0 ? 0 : now - closing;
        if (closing != 0 && sinceClose >= EXIT_STAGGER * (count - 1) + EXIT_TIME + EXIT_HEADING) { onClose(); return; }
        float screenOut = closing == 0 ? 1 : 1 - ease((sinceClose - EXIT_STAGGER * (count - 1) - EXIT_TIME / 2F) / (float) EXIT_HEADING);
        // The whole screen is this one choice: the world is covered, and gives way again once it is made.
        float cover = screenIn * screenOut;
        var window = minecraft.getWindow();
        UiViewport backdrop = viewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        graphics.fillGradient(0, 0, backdrop.width(), backdrop.height(),
                tint(0xFF0A0F18, cover), tint(0xFF04060A, cover));
        // The title screen's slideshow runs behind the choice, fading with the screen so the world still gives way to it.
        de.artemis.cyberneticenhancements.client.TitleSlideshow.renderTransition(
                de.artemis.cyberneticenhancements.client.TitleSlideshow.nativeGraphics(graphics), cover);
        float title = ease((now - opened - 150) / 600F) * screenOut, sub = ease((now - opened - 400) / 600F) * screenOut;
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, 16 + (1 - title) * -8, 0);
        graphics.pose().scale(2, 2, 1);
        var heading = Component.translatable("screen.cyberneticenhancements.origin.heading");
        text(graphics, heading, -font.width(heading) / 2, 0, 0xFFFF5266, title, false);
        graphics.pose().popPose();
        for (int line = 0; line < subheading.size(); line++) {
            var sequence = subheading.get(line);
            text(graphics, sequence, (width - font.width(sequence)) / 2, 38 + line * LINE, VanillaMenuPresentation.MUTED, sub, false);
        }
        for (int i = 0; i < count; i++) {
            Origin origin = Origin.values()[i];
            float arrive = ease((now - opened - CARD_DELAY - i * CARD_STAGGER) / (float) CARD_TIME);
            float dim = 1 - 0.6F * confirmation * (1 - chosen[i]);
            int order = selected == null ? i : origin == selected ? count - 1 : i < selected.ordinal() ? i : i - 1;
            float leave = closing == 0 ? 0 : ease((sinceClose - order * EXIT_STAGGER) / (float) EXIT_TIME);
            float alpha = arrive * dim * (1 - leave);
            int x = left + i * (cardWidth + GAP);
            float lift = (1 - arrive) * 26 - hover[i] * 4 - chosen[i] * 6 + leave * (origin == selected ? -14 : 16);
            float glow = Math.max(hover[i] * 0.55F, chosen[i]);
            graphics.pose().pushPose();
            graphics.pose().translate(0, lift, 0);
            artwork(graphics, origin, x, top, cardWidth, cardHeight, alpha, glow);
            int y = top + artHeight + PAD;
            text(graphics, origin.title(), x + PAD, y, 0xFFFF5266, alpha, true);
            y += LINE + 2;
            var wrapped = lines.get(origin);
            for (var line : wrapped.tagline) { text(graphics, line, x + PAD, y, VanillaMenuPresentation.MUTED, alpha, false); y += LINE; }
            y += 8;
            for (var line : wrapped.story) { text(graphics, line, x + PAD, y, VanillaMenuPresentation.TEXT, alpha, false); y += LINE; }
            y += 8;
            for (var perk : wrapped.perks) {
                text(graphics, "+", x + PAD, y, 0xFF5CE0FF, alpha, false);
                for (var line : perk) { text(graphics, line, x + PAD + 8, y, 0xFFE6A3AE, alpha, false); y += LINE; }
            }
            if (SkillsConfig.enabled()) {
                renderStartingSkills(graphics, origin, x + PAD, choose[i].getY(), cardWidth - 2 * PAD, alpha);
            }
            graphics.pose().popPose();
            choose[i].shift = lift;
            choose[i].setAlpha(arrive * (1 - confirmation) * (1 - leave));
            choose[i].active = closing == 0 && selected == null && arrive >= 1;
        }
        float lift = (1 - confirmation) * 8;
        confirm.shift = back.shift = lift;
        float exit = closing == 0 ? 1 : 1 - ease(sinceClose / (float) EXIT_TIME);
        confirm.setAlpha(confirmation * exit);
        back.setAlpha(confirmation * exit);
        confirm.visible = back.visible = confirmation > 0.02F;
        confirm.active = back.active = selected != null && closing == 0 && confirmation > 0.6F;
        if (selected != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, lift, 0);
            var notice = Component.translatable("screen.cyberneticenhancements.origin.permanent", selected.title());
            text(graphics, notice, (width - font.width(notice)) / 2, top + cardHeight + 18, 0xFFFFD5DC, confirmation * exit, false);
            graphics.pose().popPose();
        }
        super.renderContent(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && closing == 0) {
            if (selected != null) selected = null; else de.artemis.cyberneticenhancements.client.OriginClient.pause();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
