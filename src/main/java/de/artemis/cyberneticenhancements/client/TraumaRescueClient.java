package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.TraumaRescuePayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class TraumaRescueClient {
    private static TraumaRescuePayload state;
    private static long recovering;

    private TraumaRescueClient() {
    }

    public static void receive(TraumaRescuePayload payload) {
        boolean openingSession = state == null || state.phase() >= 3;
        state = payload;
        recovering = payload.phase() == 3 ? Util.getMillis() : 0;
        var mc = Minecraft.getInstance();
        // Dispatch and treatment updates only change progress. Only a new downed
        // session (including reconnecting to a rescue) may open the death screen.
        if ((payload.phase() == 0 || openingSession && (payload.phase() == 1 || payload.phase() == 2)) && mc.player != null && mc.level != null
                && !(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen)) {
            mc.setScreen(new net.minecraft.client.gui.screens.DeathScreen(Component.translatable("trauma.cyberneticenhancements.downed"), mc.level.getLevelData().isHardcore()));
        }
    }

    public static boolean canCallMedtec() {
        return state != null && state.phase() == 0;
    }

    public static boolean rescueActive() {
        return state != null && state.phase() >= 1 && state.phase() <= 3;
    }

    public static int rescueColor() {
        return state != null && state.phase() == 1 ? 0xFFFFC766 : 0xFF66F5A5;
    }

    public static Component deathScreenTitle() {
        String key = !rescueActive() ? "flatlined.title"
                : state.phase() == 1 ? "recovering" : "reviving";
        return Component.translatable("screen.cyberneticenhancements." + key);
    }

    public static int deathScreenTitleColor() {
        return rescueActive() ? rescueColor() : 0xFFFF5266;
    }

    public static void callMedtec() {
        if (canCallMedtec())
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new de.artemis.cyberneticenhancements.common.network.CallMedtecPayload());
    }

    public static double rise() {
        return recovering == 0 ? 0 : Math.clamp((Util.getMillis() - recovering) / 850.0, 0, 1);
    }

    public static void tick(ClientTickEvent.Post event) {
        if (state == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            state = null;
            recovering = 0;
            return;
        }
        if (recovering != 0 && rise() >= 1) {
            if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen || mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen)
                mc.setScreen(null);
            if (rise() >= 1 && Util.getMillis() - recovering > 1800) {
                state = null;
                recovering = 0;
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        if (state == null || state.phase() == 0 || state.phase() == 5) return;
        var mc = Minecraft.getInstance();
        if (state.phase() == 4) {
            state = null;
            return;
        }
        if (recovering != 0) {
            double elapsed = (Util.getMillis() - recovering) / 1800.0;
            int alpha = (int) (100 * Math.sin(Math.min(1, elapsed) * Math.PI));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0x25ED81);
        }
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen || mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen))
            return;
        if (!ClientConfig.CUSTOM_DEATH_SCREEN.get()) {
            renderVanillaProgress(graphics);
            return;
        }
        var layout = de.artemis.cyberneticenhancements.client.ui.FlatlinedLayout.of(graphics.guiWidth(), graphics.guiHeight());
        int x = layout.left();
        int y = layout.titleY() + 26;
        int width = layout.columnWidth();
        int height = graphics.guiHeight() < 200 ? 32 : 44;
        int color = rescueColor();
        boolean treating = state.phase() >= 2;
        float progress = Math.clamp(state.treatmentTicks() / 60.0F, 0, 1);
        graphics.fill(x, y, x + width, y + height, 0xE0081511);
        graphics.fill(x, y, x + width, y + 1, 0xFF315347);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF315347);
        graphics.fill(x, y, x + 1, y + height, 0xFF315347);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF315347);
        Component heading = Component.translatable("trauma.cyberneticenhancements.panel."
                + (state.phase() == 3 ? "revived" : treating ? "treating" : "dispatch"));
        graphics.drawString(mc.font, heading, x + 8, y + 5, color, false);
        if (treating) {
            String percent = Math.round(progress * 100) + "%";
            graphics.drawString(mc.font, percent, x + width - 8 - mc.font.width(percent), y + 5, color, false);
        }
        if (height > 32) {
            Component detail = Component.translatable("trauma.cyberneticenhancements.panel."
                    + (state.phase() == 3 ? "restored" : state.distance() < 0 ? "searching" : "distance"), Math.round(state.distance()), state.seconds());
            graphics.drawString(mc.font, detail, x + 8, y + 18, 0xFFA5BDB3, false);
        }
        int barLeft = x + 8;
        int barWidth = width - 16;
        int barY = y + height - 9;
        graphics.fill(barLeft, barY, barLeft + barWidth, barY + 4, 0xFF233C32);
        if (treating) {
            graphics.fill(barLeft, barY, barLeft + Math.round(barWidth * progress), barY + 4, color);
        } else {
            // A moving marker means responders are en route, rather than a fake ETA.
            double motion = mc.options.screenEffectScale().get();
            int offset = motion <= 0 ? 0 : (int) ((Util.getMillis() / 12) % Math.max(1, barWidth - 24));
            graphics.fill(barLeft + offset, barY, barLeft + offset + 24, barY + 4, color);
        }
    }

    private static void renderVanillaProgress(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        int center = graphics.guiWidth() / 2;
        boolean treating = state.phase() >= 2;
        float progress = Math.clamp(state.treatmentTicks() / 60.0F, 0, 1);
        Component heading = Component.translatable("trauma.cyberneticenhancements.vanilla."
                + (state.phase() == 3 ? "revived" : treating ? "treating" : "dispatch"));
        if (treating) heading = heading.copy().append(" (" + Math.round(progress * 100) + "%)");
        Component detail = Component.translatable("trauma.cyberneticenhancements.panel."
                + (state.phase() == 3 ? "restored" : state.distance() < 0 ? "searching" : "distance"),
                Math.round(state.distance()), state.seconds());
        graphics.drawCenteredString(mc.font, heading, center, 80, 0xFFFFFFFF);
        graphics.drawCenteredString(mc.font, detail, center, 93, 0xFFAAAAAA);

        int left = center - 91;
        int top = 107;
        var background = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
        var fill = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");
        graphics.blitSprite(background, left, top, 182, 5);
        int offset = treating || mc.options.screenEffectScale().get() <= 0
                ? 0 : (int) ((Util.getMillis() / 12) % (182 - 24));
        int filled = treating ? Math.round(182 * progress) : 24;
        if (filled > 0) {
            graphics.enableScissor(left + offset, top, left + offset + filled, top + 5);
            graphics.blitSprite(fill, left, top, 182, 5);
            graphics.disableScissor();
        }
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
        recovering = 0;
    }
}
