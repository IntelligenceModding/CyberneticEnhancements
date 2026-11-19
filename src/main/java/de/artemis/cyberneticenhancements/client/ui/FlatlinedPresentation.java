package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.FlatlinedDeathCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlatlinedPresentation {
    private static final String[] SKULL = {
            "    XXXXXXXX    ", "  XXXXXXXXXXXX  ", " XXXXXXXXXXXXXX ", " XXXXXXXXXXXXXX ",
            "XXXXXXXXXXXXXXXX", "XXX   XXXX   XXX", "XX     XX     XX", "XX     XX     XX",
            "XXX   XXXX   XXX", " XXXXXX  XXXXXX ", "  XXXX    XXXX  ", "   XXXXXXXXXX   ",
            "   XX X XX XX   ", "   XX X XX XX   ", "    XXXXXXXX    ", "     XXXXXX     "
    };

    private FlatlinedPresentation() {
    }

    public static boolean enabled(Screen screen) {
        return screen != null && screen.getClass() == DeathScreen.class && ClientConfig.CUSTOM_DEATH_SCREEN.get();
    }

    public static boolean confirmationEnabled(Screen screen) {
        return screen != null && screen.getClass() == DeathScreen.TitleConfirmScreen.class && ClientConfig.CUSTOM_DEATH_SCREEN.get();
    }

    public static boolean hidesGameplayUi() {
        Screen screen = Minecraft.getInstance().screen;
        return enabled(screen) || confirmationEnabled(screen);
    }

    public static void confirmationBackground(GuiGraphics graphics, Screen screen) {
        int width = screen.width;
        int height = screen.height;
        var font = Minecraft.getInstance().font;
        var layout = FlatlinedLayout.of(width, height);
        int margin = Math.min(24, width / 16);
        graphics.fill(0, 0, width, height, 0xE00C080D);
        graphics.fillGradient(0, 0, width, height, 0x201E0006, 0x5039000B);
        graphics.fill(margin, 0, margin + 1, height, 0xFF612334);
        graphics.fill(width - margin - 1, 0, width - margin, height, 0xFF612334);
        for (int y = 0; y < height; y += 5) {
            graphics.fill(margin + 1, y, width - margin - 1, y + 1, 0x08000000);
        }
        var lines = font.split(screen.getTitle(), layout.columnWidth());
        int titleY = layout.buttonTop() - 22 - lines.size() * 11;
        int center = layout.center();
        int iconY = titleY - 26;
        graphics.fill(center - 8, iconY, center + 3, iconY + 1, 0xFFC85061);
        graphics.fill(center - 8, iconY, center - 7, iconY + 15, 0xFFC85061);
        graphics.fill(center - 8, iconY + 14, center + 3, iconY + 15, 0xFFC85061);
        graphics.fill(center - 1, iconY + 7, center + 9, iconY + 8, 0xFFC85061);
        for (int i = 0; i < 4; i++) {
            graphics.fill(center + 5 + i, iconY + 4 + i, center + 6 + i, iconY + 5 + i, 0xFFC85061);
            graphics.fill(center + 5 + i, iconY + 10 - i, center + 6 + i, iconY + 11 - i, 0xFFC85061);
        }
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index);
            graphics.drawString(font, line, center - font.width(line) / 2, titleY + index * 11, 0xFFFFB3BC, false);
        }
        graphics.fill(layout.left(), layout.buttonTop() - 7, layout.left() + layout.columnWidth(), layout.buttonTop() - 6, 0xFF612334);
        int right = width - margin - 7;
        Component medical = Component.translatable("screen.cyberneticenhancements.flatlined.medical_center");
        graphics.drawString(font, medical, right - font.width(medical), 12, 0xFFAB4A5B, false);
        Component record = Component.translatable("screen.cyberneticenhancements.flatlined.record", FlatlinedDeathCounter.record());
        graphics.drawString(font, record, right - font.width(record), 32, 0xFF9D5360, false);
        Component status = Component.translatable("screen.cyberneticenhancements.flatlined.confirmation");
        graphics.drawString(font, status, right - font.width(status), height - 20, 0xFFAB4A5B, false);
    }

    public static void background(GuiGraphics graphics, int width, int height, double seconds, boolean hardcore) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        double motion = minecraft.options.screenEffectScale().get();
        double intensity = motion <= 0 ? 1 : FlatlinedTransition.intensity(seconds);
        double veil = (motion <= 0 ? 1 : FlatlinedTransition.intensity(seconds - 0.35)) * (1 - de.artemis.cyberneticenhancements.client.TraumaRescueClient.rise());
        long frame = (long) (seconds * 18.0 * motion);
        int margin = Math.min(24, width / 16);
        graphics.fill(0, 0, width, height, ((int) (24 + veil * 136) << 24) | 0x090408);
        int scanOffset = (int) (seconds * 28.0 * motion) % 4;
        for (int band = 0; band < FlatlinedTransition.BANDS; band++) {
            double coverage = motion <= 0 ? 1 : FlatlinedTransition.coverage(seconds, band);
            int revealed = (int) Math.ceil(width * coverage);
            if (revealed <= 0) continue;
            int left = (band & 1) == 0 ? 0 : width - revealed;
            int right = left + revealed;
            int top = band * height / FlatlinedTransition.BANDS;
            int bottom = (band + 1) * height / FlatlinedTransition.BANDS;
            graphics.enableScissor(left, top, right, bottom);
            graphics.fill(0, top, width, bottom, ((int) (40 + veil * 200) << 24) | 0x090408);
            graphics.fillGradient(0, 0, width, height,
                    (int) (112 * (0.15 + veil * 0.85)) << 24 | 0x1E0006,
                    (int) (192 * (0.15 + veil * 0.85)) << 24 | 0x39000B);
            graphics.fill(0, top, margin, bottom, 0xFF370914);
            graphics.fill(width - margin, top, width, bottom, 0xFF370914);
            graphics.fill(margin, top, margin + 1, bottom, 0xFF9C2035);
            graphics.fill(width - margin - 1, top, width - margin, bottom, 0xFF9C2035);
            for (int y = top + Math.floorMod(scanOffset - top, 4); y < bottom; y += 4) {
                graphics.fill(margin + 1, y, width - margin - 1, y + 1, 0x18000000);
            }
            if (coverage < 1) {
                int edge = (band & 1) == 0 ? right - 2 : left;
                graphics.fill(edge, top, edge + 2, bottom, 0xAFAA293E);
            }
            graphics.disableScissor();
        }
        for (int i = 0; i < (int) ((12 + 36 * motion) * intensity); i++) {
            int noise = noise(frame, i);
            int y = Math.floorMod(noise, Math.max(1, height));
            int length = 3 + (int) (Math.floorMod(noise >>> 8, Math.max(4, width / 4)) * intensity);
            int x = Math.floorMod(noise >>> 16, Math.max(1, width - length));
            int alpha = (int) ((20 + Math.floorMod(noise >>> 4, 65)) * intensity);
            graphics.fill(x, y, x + length, Math.min(height, y + 1 + (i % 4)), alpha << 24 | 0xC62B47);
            if (motion > 0 && i % 5 == 0) {
                graphics.fill(x + 3, y + 1, x + length + 3, Math.min(height, y + 2), (alpha / 3) << 24 | 0x73C1CC);
            }
        }
        if (motion > 0) {
            int sweep = (int) (seconds * 95 * motion) % Math.max(1, height + 24) - 24;
            graphics.fillGradient(margin + 1, sweep, width - margin - 1, sweep + 24,
                    0x00CE3550, (int) (30 * intensity * motion) << 24 | 0xCE3550);
            for (int i = 0; i < 3; i++) {
                int seed = noise(frame / 3, 70 + i);
                int y = Math.floorMod(seed, Math.max(1, height));
                int offset = (int) ((Math.floorMod(seed >>> 8, 17) - 8) * motion * intensity);
                int alpha = (int) (intensity * motion * (20 + Math.floorMod(seed >>> 16, 24)));
                graphics.fill(margin + offset, y, width - margin + offset, Math.min(height, y + 2 + i), alpha << 24 | 0xE83E59);
            }
        }

        var layout = FlatlinedLayout.of(width, height);
        if (hardcore) {
            graphics.drawString(font, Component.translatable("deathScreen.title.hardcore"), layout.left(),
                    height >= 220 ? height - 51 : height - 14, 0xFFFF6977, false);
        }
        int skullScale = layout.skullScale();
        int skullX = layout.center() / 2 * 2 - 8 * skullScale;
        int skullY = layout.skullY();
        skull(graphics, skullX + 2, skullY, skullScale, (int) (70 * intensity * motion) << 24 | 0x3E94A3, seconds, motion, intensity);
        boolean rescuing = de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueActive();
        int skullColor = rescuing ? de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueColor() : 0xFFF24D5E;
        skull(graphics, skullX, skullY, skullScale, skullColor, seconds, motion, intensity);
        if (rescuing) {
            int crossX = skullX + 16 * skullScale + 6;
            graphics.fill(crossX + 3, skullY + 3, crossX + 6, skullY + 12, skullColor);
            graphics.fill(crossX, skullY + 6, crossX + 9, skullY + 9, skullColor);
        }

        graphics.fill(layout.left(), layout.buttonTop() - 7, layout.left() + layout.columnWidth(), layout.buttonTop() - 6, 0xFF7A1B2D);
        diagnostics(graphics, width, height, layout, seconds * motion);
    }

    public static void button(GuiGraphics graphics, AbstractButton button) {
        button(graphics, button, 1);
    }

    private static int buttonAlpha(int color, float opacity) {
        return Math.round((color >>> 24) * Math.clamp(opacity, 0, 1)) << 24 | color & 0xFFFFFF;
    }

    public static void button(GuiGraphics graphics, AbstractButton button, float opacity) {
        if (opacity <= 0.02F) return;
        int textColor = buttonFrame(graphics, button, opacity, false);
        button.renderString(graphics, Minecraft.getInstance().font, textColor);
    }

    public static int buttonFrame(GuiGraphics graphics, AbstractWidget button, float opacity, boolean selected) {
        if (opacity <= 0.02F) return 0;
        boolean highlighted = button.active && (button.isHoveredOrFocused() || selected);
        int x = button.getX();
        int y = button.getY();
        int right = x + button.getWidth();
        int bottom = y + button.getHeight();
        graphics.fill(x + 2, y + 2, right + 2, bottom + 2, buttonAlpha(0x90000000, opacity));
        graphics.fill(x, y, right, bottom, buttonAlpha(!button.active ? 0xFF171717 : highlighted ? 0xFF66172A : 0xFF310C18, opacity));
        int border = buttonAlpha(!button.active ? 0xFF454045 : highlighted ? VanillaMenuPresentation.HIGHLIGHT : VanillaMenuPresentation.EDGE, opacity);
        graphics.fill(x, y, right, y + 1, border);
        graphics.fill(x, bottom - 1, right, bottom, border);
        graphics.fill(x, y, x + 1, bottom, border);
        graphics.fill(right - 1, y, right, bottom, border);
        return buttonAlpha(button.active ? 0xFFFFB3BC : 0xFF898989, opacity);
    }

    public static void titleGlitch(GuiGraphics graphics, int width, int height, double seconds) {
        double motion = Minecraft.getInstance().options.screenEffectScale().get();
        if (motion <= 0) return;
        double intensity = FlatlinedTransition.intensity(seconds);
        if (intensity < 0.15) return;
        long frame = (long) (seconds * 18.0 * motion);
        if (Math.floorMod(noise(frame / 4, 91), 5) >= 2) return;
        var font = Minecraft.getInstance().font;
        Component title = de.artemis.cyberneticenhancements.client.TraumaRescueClient.deathScreenTitle();
        var layout = FlatlinedLayout.of(width, height);
        int center = layout.center() / 2 * 2;
        int titleY = layout.titleY() / 2 * 2;
        int halfWidth = font.width(title) + 10;
        for (int slice = 0; slice < 3; slice++) {
            int seed = noise(frame, 100 + slice);
            int y = titleY + Math.floorMod(seed, 16);
            int thickness = 1 + Math.floorMod(seed >>> 8, 3);
            int offset = (int) ((Math.floorMod(seed >>> 16, 23) - 11) * intensity * motion);
            graphics.enableScissor(center - halfWidth, y, center + halfWidth, y + thickness);
            graphics.fill(center - halfWidth, y, center + halfWidth, y + thickness, 0xFF200810);
            graphics.pose().pushPose();
            graphics.pose().translate(offset, 0, 0);
            graphics.pose().scale(2, 2, 1);
            graphics.drawCenteredString(font, title, center / 2, titleY / 2,
                    de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueActive()
                            ? de.artemis.cyberneticenhancements.client.TraumaRescueClient.rescueColor() : 0xFFFF7B8B);
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private static void diagnostics(GuiGraphics graphics, int width, int height, FlatlinedLayout layout, double time) {
        var font = Minecraft.getInstance().font;
        int right = width - Math.min(24, width / 16) - 7;
        Component medical = Component.translatable("screen.cyberneticenhancements.flatlined.medical_center");
        int medicalWidth = font.width(medical);
        int medicalX = right - medicalWidth;
        int top = 12;
        graphics.drawString(font, medical, medicalX, top, 0xFFCD4253, false);
        graphics.fill(medicalX, top + 13, right, top + 14, 0xFF75192A);
        Component record = Component.translatable("screen.cyberneticenhancements.flatlined.record", FlatlinedDeathCounter.record());
        graphics.drawString(font, record, right - font.width(record), top + 20, 0xFFFF7B8B, false);
        if (width >= 480) {
            Component monitor = Component.translatable("screen.cyberneticenhancements.flatlined.monitor");
            graphics.drawString(font, monitor, right - font.width(monitor), top + 34, 0xFF9D3546, false);
            for (int i = 0; i < 8; i++) {
                int x = right - 4 - i * 8;
                int barHeight = 2 + (int) ((Math.sin(time * 3 + i * 0.8) + 1) * 3);
                graphics.fill(x, top + 58 - barHeight, x + 3, top + 58, 0x704B97A5);
            }
            graphics.fill(medicalX, top + 54, right - 70, top + 55, 0xFF9D3546);
        }
        int statusY = height - 63;
        Component[] messages = {
                Component.translatable("screen.cyberneticenhancements.flatlined.system"),
                Component.translatable("screen.cyberneticenhancements.flatlined.status"),
                Component.translatable("screen.cyberneticenhancements.flatlined.brain_damage"),
                Component.translatable("screen.cyberneticenhancements.flatlined.offline")
        };
        int[] colors = {0xFFCD4253, 0xD99D3546, 0xB37D3B46, 0x8C64434A};
        int widest = 1;
        for (Component message : messages) widest = Math.max(widest, font.width(message));
        int available = right - layout.left() - layout.columnWidth() - 12;
        float statusScale = Math.min(1.0F, Math.max(0.5F, available / (float) widest));
        graphics.pose().pushPose();
        graphics.pose().translate(right, statusY, 0);
        graphics.pose().scale(statusScale, statusScale, 1);
        for (int index = 0; index < messages.length; index++) {
            Component message = messages[index];
            graphics.drawString(font, message, -font.width(message), Math.round(index * 13 / statusScale), colors[index], false);
        }
        graphics.pose().popPose();
    }

    private static void skull(GuiGraphics graphics, int x, int y, int scale, int color, double seconds, double motion, double intensity) {
        double time = seconds * motion;
        double flicker = 0.94 + motion * (0.04 * Math.sin(time * 4.712389) + 0.02 * Math.sin(time * 18.849556));
        double sweep = (time % 2.0) / 2.0 * SKULL.length;
        long frame = (long) (time * 18);
        for (int row = 0; row < SKULL.length; row++) {
            int seed = noise(frame, row + 140);
            int shift = motion > 0 && Math.floorMod(seed, 11) == 0
                    ? (int) ((Math.floorMod(seed >>> 8, 7) - 3) * scale * intensity * motion) : 0;
            double stripe = 1 - motion * 0.28 * (0.5 + 0.5 * Math.sin(row * 0.9 - time * 12.56637));
            double scan = motion * Math.exp(-Math.pow((row - sweep) / 1.4, 2));
            int alpha = (int) Math.min(255, (color >>> 24) * flicker * stripe + (color >>> 24) * scan * 0.12);
            int rowColor = alpha << 24 | (scan > 0.5 && (color & 0xFFFFFF) == 0xF24D5E ? 0xFF9AAB : color & 0xFFFFFF);
            for (int column = 0; column < SKULL[row].length(); column++) {
                if (SKULL[row].charAt(column) == 'X') {
                    graphics.fill(x + column * scale + shift, y + row * scale, x + (column + 1) * scale + shift, y + (row + 1) * scale, rowColor);
                }
            }
        }
    }

    private static int noise(long frame, int index) {
        long value = (frame + 1) * 0x9E3779B9L + index * 0x85EBCA6BL;
        value ^= value >>> 16;
        value *= 0xC2B2AE35L;
        return (int) (value ^ value >>> 13);
    }
}
