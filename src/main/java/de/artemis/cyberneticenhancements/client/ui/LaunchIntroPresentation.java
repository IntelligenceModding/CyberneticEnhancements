package de.artemis.cyberneticenhancements.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LaunchIntroPresentation {
    public enum NoticePage {
        COPYRIGHT("disclaimer_title", "disclaimer_minecraft", "disclaimer_neoforge", "disclaimer_artwork", "disclaimer_unofficial"),
        SCREEN_CHOICE("settings_title", "settings_open", "settings_loading", "settings_title_screen", "settings_intro", "settings_compatibility"),
        THANK_YOU("thanks_title", "thanks_message", "thanks_more", "thanks_platforms"),
        BETA("beta_title", "beta_message", "beta_reports", "beta_disclaimers"),
        TRANSPARENCY("transparency_title", "transparency_message", "transparency_review", "transparency_testing", "transparency_artists"),
        CREDITS("credits_title", "credits_message", "credits_structure");

        private final String heading;
        private final String[] paragraphs;

        NoticePage(String heading, String... paragraphs) { this.heading = heading; this.paragraphs = paragraphs; }

        public Component heading() { return text(heading); }
        public java.util.List<String> paragraphKeys() { return java.util.List.of(paragraphs); }

        public Component narration() {
            var narration = text(heading).copy();
            for (var paragraph : paragraphs) narration.append(". ").append(text(paragraph));
            return narration;
        }
    }
    private static final int CHROME_TEXT = 0xFF713541;
    private static final ResourceLocation MOJANG = ResourceLocation.withDefaultNamespace("textures/gui/title/mojangstudios.png");
    private static final ResourceLocation NEOFORGE = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/intro/neoforge.png");
    private static final ResourceLocation INTELLIGENCE_LOGO = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/intro/intelligence_logo.png");
    private static final ResourceLocation DOOMED_ARTEMIS = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/intro/doomed_artemis.png");
    private LaunchIntroPresentation() {}

    public static java.util.concurrent.CompletableFuture<Void> preload(
            net.minecraft.client.renderer.texture.TextureManager textures, java.util.concurrent.Executor executor) {
        if (!de.artemis.cyberneticenhancements.client.ClientConfig.SPEC.isLoaded()
                || !de.artemis.cyberneticenhancements.client.ClientConfig.SHOW_LAUNCH_INTRO.get())
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        // Prepare the small logo during loading, never on its first visible frame.
        // SimpleTexture reloads this conversion when resource packs are reloaded.
        if (!(textures.getTexture(NEOFORGE, null) instanceof MonochromeLogoTexture))
            textures.register(NEOFORGE, new MonochromeLogoTexture(NEOFORGE));
        return java.util.concurrent.CompletableFuture.allOf(textures.preload(MOJANG, executor),
                        textures.preload(INTELLIGENCE_LOGO, executor),
                        textures.preload(DOOMED_ARTEMIS, executor))
                .exceptionally(error -> null);
    }

    private static Component text(String key) {
        return Component.translatable("screen.cyberneticenhancements.intro." + key);
    }

    private static int color(int rgb, float alpha) {
        return Math.clamp(Math.round(alpha * 255), 4, 255) << 24 | rgb & 0xFFFFFF;
    }

    public static void render(GuiGraphics graphics, LaunchIntroTimeline timeline) {
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0xFF000000);
        var phase = timeline.phase();
        double motion = Minecraft.getInstance().options.screenEffectScale().get();
        if (timeline.showingLogo()) {
            float opacity = timeline.logoOpacity();
            float glitch = timeline.logoGlitch() * (float) motion;
            scans(graphics, width, height, motion, opacity <= 0 ? 0.55F : 0.14F);
            brand(graphics, phase, width / 2, height / 2, Math.min(560, width * 0.65F), opacity);
            glitchBands(graphics, glitch, shift ->
                    brand(graphics, phase, width / 2 + shift, height / 2, Math.min(560, width * 0.65F), opacity));
        } else {
            boolean entering = phase == LaunchIntroTimeline.Phase.ENTERING;
            // The fourth hit switches from Minecraft to the disclaimer.
            float hit = entering || phase != LaunchIntroTimeline.Phase.DISCLAIMER ? 0
                    : Math.clamp(1 - timeline.phaseMillis() / 220.0F, 0, 1) * (float) motion;
            scans(graphics, width, height, motion, 0.12F + hit * 0.65F);
            notices(graphics, entering ? LaunchIntroTimeline.Phase.BETA_NOTICE : phase,
                    entering ? LaunchIntroTimeline.PRESENTATION_MILLIS - LaunchIntroTimeline.BETA_START_MILLIS : timeline.phaseMillis());
        }
    }

    public static void renderMenuTransition(GuiGraphics graphics, LaunchIntroTimeline timeline, UiFadeLayer layer, boolean skipping) {
        double motion = Minecraft.getInstance().options.screenEffectScale().get();
        if (motion <= 0) {
            // Keep a quiet dissolve when the player has disabled screen effects.
            if (skipping) layer.drawCaptured(graphics, 1 - timeline.exitProgress());
            else layer.render(graphics, 1 - timeline.exitProgress(), 1, 0, () -> render(graphics, timeline));
            return;
        }
        // The native title was already rendered this frame. After the cut, reuse
        // the same prepared target to tear its live pixels without rendering widgets twice.
        if (timeline.menuVisible()) layer.capture();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
        float glitch = timeline.menuGlitch() * (float) motion;
        scans(graphics, graphics.guiWidth(), graphics.guiHeight(), motion, 0.14F + glitch * 0.4F);
        float opacity = timeline.menuOpacity();
        if (timeline.menuVisible() || skipping) layer.drawCaptured(graphics, opacity);
        else layer.render(graphics, opacity, 1, 0, () -> render(graphics, timeline));
        glitchBands(graphics, glitch, shift -> {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(shift, 0, 0);
                layer.drawCaptured(graphics, opacity);
            } finally { graphics.pose().popPose(); }
        });
    }

    private static void glitchBands(GuiGraphics graphics, float glitch, java.util.function.IntConsumer draw) {
        if (glitch <= 0) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        long frame = Util.getMillis() / 45;
        for (int band = 0; band < 5; band++) {
            int top = height / 2 - 75 + Math.floorMod((int) frame * 13 + band * 37, 150);
            int shift = Math.round((band % 2 == 0 ? -18 : 14) * glitch);
            graphics.enableScissor(0, top, width, top + 5);
            try {
                graphics.fill(0, top, width, top + 5, 0xFF000000);
                draw.accept(shift);
            } finally { graphics.disableScissor(); }
        }
    }

    private static void brand(GuiGraphics graphics, LaunchIntroTimeline.Phase phase, int center, int middle, float width, float opacity) {
        brandPass(graphics, phase, center + 4, middle + 5, width, opacity * 0.18F, true);
        brandPass(graphics, phase, center + 2, middle + 3, width, opacity * 0.42F, true);
        brandPass(graphics, phase, center, middle, width, opacity, false);
    }

    private static void brandPass(GuiGraphics graphics, LaunchIntroTimeline.Phase phase, int center, int middle, float width, float opacity, boolean shadow) {
        if (opacity <= 0) return;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(shadow ? 0 : 1, shadow ? 0 : 0.24F, shadow ? 0 : 0.34F, opacity);
        try {
            if (phase == LaunchIntroTimeline.Phase.MOJANG) {
                if (shadow) {
                    // Mojang's texture has black backing rather than transparent
                    // backing: darken through its white silhouette, not its rectangle.
                    RenderSystem.blendFunc(0, 769);
                    graphics.setColor(opacity, opacity, opacity, 1);
                } else RenderSystem.blendFunc(770, 1);
                int half = Math.round(width / 2), height = Math.round(width / 4);
                int top = middle - height / 2;
                graphics.blit(MOJANG, center - half, top, half, height, -0.0625F, 0, 120, 60, 120, 120);
                graphics.blit(MOJANG, center, top, half, height, 0.0625F, 60, 120, 60, 120, 120);
            } else if (phase == LaunchIntroTimeline.Phase.NEOFORGE) {
                int height = Math.round(width / 4);
                graphics.blit(NEOFORGE, center - Math.round(width / 2), middle - height / 2,
                        Math.round(width), height, 0, 0, 480, 120, 480, 120);
            } else if (phase == LaunchIntroTimeline.Phase.INTELLIGENCE) {
                intelligence(graphics, center, middle, width, opacity, shadow);
            } else {
                float scale = width / 256;
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(center - width / 2, middle - 25 * scale, 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.blit(LogoRenderer.MINECRAFT_LOGO, 0, 0, 0, 0, 256, 44, 256, 64);
                    graphics.blit(LogoRenderer.MINECRAFT_EDITION, 64, 37, 0, 0, 128, 14, 128, 16);
                } finally { graphics.pose().popPose(); }
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void intelligence(GuiGraphics graphics, int center, int middle, float width, float opacity, boolean shadow) {
        // Use tight UV regions of the supplied originals, without their transparent canvas margins.
        float scale = width / 560;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center - width / 2, middle, 0);
            graphics.pose().scale(scale, scale, 1);
            // Both complete identities share a 250-wide block, a 221-high
            // artwork area, and a matching name row below it.
            graphics.blit(INTELLIGENCE_LOGO, 2, -132, 246, 221, 112, 135, 447, 401, 671, 671);
            graphics.setColor(shadow ? 0 : 1, shadow ? 0 : 1, shadow ? 0 : 1, opacity);
            graphics.pose().pushPose();
            try {
                // The two identity centers are 125 and 435 within the 560-wide canvas.
                graphics.pose().translate(435 - 221 / 2.0F, -132, 0);
                graphics.blit(DOOMED_ARTEMIS, 0, 0, 221, 221, 0, 0, 1664, 1664, 1664, 1664);
            } finally { graphics.pose().popPose(); }
            graphics.setColor(1, 1, 1, 1);
            if (!shadow) {
                var group = text("group");
                var developer = text("developer");
                var font = Minecraft.getInstance().font;
                float nameScale = Math.min(250.0F / Math.max(1, Math.max(font.width(group), font.width(developer))),
                        32.0F / font.lineHeight);
                identityName(graphics, group, 125, nameScale, opacity);
                identityName(graphics, developer, 435, nameScale, opacity);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void identityName(GuiGraphics graphics, Component name, int center, float scale, float opacity) {
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center - font.width(name) * scale / 2.0F, 105, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, name, 0, 0, color(0xFF3D57, opacity), true);
        } finally { graphics.pose().popPose(); }
    }

    private static void notices(GuiGraphics graphics, LaunchIntroTimeline.Phase phase, long age) {
        float opacity = Math.clamp(age / 180.0F, 0, 1);
        long duration = switch (phase) {
            case DISCLAIMER -> LaunchIntroTimeline.THANK_YOU_START_MILLIS - LaunchIntroTimeline.MINECRAFT_END_MILLIS;
            case THANK_YOU -> LaunchIntroTimeline.BETA_START_MILLIS - LaunchIntroTimeline.THANK_YOU_START_MILLIS;
            default -> Long.MAX_VALUE;
        };
        opacity *= Math.clamp((duration - age) / 180.0F, 0, 1);
        if (opacity <= 0.03F) return;
        noticePage(graphics, phase, opacity);
    }

    public static void renderNoticePage(GuiGraphics graphics, NoticePage page) {
        renderNoticeBackdrop(graphics);
        noticeColumn(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2 - 115,
                Math.min(660, graphics.guiWidth() - 80), 1, page.heading, page.paragraphs);
    }

    public static void renderNoticeBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
        scans(graphics, graphics.guiWidth(), graphics.guiHeight(), Minecraft.getInstance().options.screenEffectScale().get(), 0.12F);
    }

    private static void noticePage(GuiGraphics graphics, LaunchIntroTimeline.Phase phase, float opacity) {
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        if (phase == LaunchIntroTimeline.Phase.DISCLAIMER) {
            int totalWidth = Math.min(900, width - 80);
            int gap = 48, columnWidth = (totalWidth - gap) / 2;
            int left = (width - totalWidth) / 2;
            int top = height / 2 - 115;
            noticeColumn(graphics, left + columnWidth / 2, top, columnWidth, opacity,
                    NoticePage.COPYRIGHT.heading, NoticePage.COPYRIGHT.paragraphs);
            noticeColumn(graphics, left + columnWidth + gap + columnWidth / 2, top, columnWidth, opacity,
                    NoticePage.SCREEN_CHOICE.heading, NoticePage.SCREEN_CHOICE.paragraphs);
        } else {
            var page = phase == LaunchIntroTimeline.Phase.THANK_YOU ? NoticePage.THANK_YOU : NoticePage.BETA;
            noticeColumn(graphics, width / 2, height / 2 - 65, Math.min(660, width - 80), opacity,
                    page.heading, page.paragraphs);
        }
    }

    private static void noticeColumn(GuiGraphics graphics, int center, int y, int width, float opacity,
                                     String heading, String[] paragraphs) {
        var font = Minecraft.getInstance().font;
        shadowedCentered(graphics, text(heading), center, y, color(0xFF5266, opacity));
        y += 34;
        for (int index = 0; index < paragraphs.length; index++) {
            for (var line : font.split(text(paragraphs[index]), width)) {
                graphics.drawString(font, line, center - font.width(line) / 2, y,
                        color(index == paragraphs.length - 1 ? 0xA9707C : 0xE6A3AE, opacity), true);
                y += 13;
            }
            y += 14;
        }
    }

    /** Persistent chrome, drawn after all captures, fades and glitch composites. */
    public static void renderCopyrightNotices(GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        Component artwork = Component.translatableWithFallback("screen.cyberneticenhancements.title.artwork_credit",
                "Cyberpunk 2077 artwork \u00A9 CD PROJEKT S.A. All rights reserved.");
        Component minecraft = Component.translatable("title.credits");
        // Keep the notices in opposite bottom corners using native GUI font size.
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, artwork, 2, graphics.guiHeight() - 10, CHROME_TEXT);
            graphics.drawString(font, minecraft, graphics.guiWidth() - font.width(minecraft) - 2,
                    graphics.guiHeight() - 10, CHROME_TEXT);
        } finally { graphics.pose().popPose(); }
    }

    /** Persistent chrome, drawn after all captures, fades and glitch composites. */
    public static void renderSkipHint(GuiGraphics graphics) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            shadowedCentered(graphics, text("skip"),
                    graphics.guiWidth() / 2, graphics.guiHeight() - 36, CHROME_TEXT);
        } finally { graphics.pose().popPose(); }
    }

    private static void shadowedCentered(GuiGraphics graphics, Component label, int x, int y, int tint) {
        var font = Minecraft.getInstance().font;
        int shadow = Math.max(4, Math.round((tint >>> 24) * 0.65F)) << 24;
        graphics.drawCenteredString(font, label, x + 2, y + 2, shadow);
        graphics.drawCenteredString(font, label, x, y, tint);
    }

    static void transitionBackdrop(GuiGraphics graphics, float alpha) {
        transitionBackdrop(graphics, 0, graphics.guiHeight(), alpha);
    }

    static void transitionBackdrop(GuiGraphics graphics, int top, int bottom, float alpha) {
        if (top >= bottom || alpha <= 0.02F) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.enableScissor(0, top, width, bottom);
        try {
            graphics.fillGradient(0, 0, width, height, color(0x210D17, alpha), color(0x401522, alpha));
            scanLines(graphics, width, top, bottom, alpha * 0.55F);
        } finally {
            graphics.disableScissor();
        }
    }

    private static void scans(GuiGraphics graphics, int width, int height, double motion, float strength) {
        scanLines(graphics, width, 0, height, strength);
        if (motion <= 0) return;
        int sweep = (int) (Util.getMillis() / 12 * motion) % (height + 24) - 24;
        graphics.fillGradient(0, sweep, width, sweep + 24, 0, color(0xFF5266, strength * 0.6F));
    }

    private static void scanLines(GuiGraphics graphics, int width, int top, int bottom, float strength) {
        int lineColor = color(0x581926, strength);
        for (int y = (top + 3) / 4 * 4; y < bottom; y += 4) {
            graphics.fill(0, y, width, y + 1, lineColor);
        }
    }
}
