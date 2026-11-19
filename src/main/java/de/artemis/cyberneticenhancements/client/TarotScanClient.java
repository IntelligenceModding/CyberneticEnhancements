package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.tarot.*;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.TarotScanAnimation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TarotScanClient {
    private static int target = -1, elapsed;
    private static long lastTickMillis;
    private static TarotCardEntity displayed;
    private static final TarotScanAnimation animation = new TarotScanAnimation();
    public static boolean available() {
        return scannableCard() != null;
    }
    private static TarotCardEntity scannableCard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator() || TarotDecorationClient.active()) return null;
        TarotCardEntity card = TarotScanning.target(mc.player);
        return card != null && !TarotKnowledge.clientKnows(card.cardId()) ? card : null;
    }
    public static boolean capturesVanillaAction(net.minecraft.client.KeyMapping mapping) {
        Minecraft mc = Minecraft.getInstance();
        return (mapping == mc.options.keyInventory || mapping == mc.options.keySwapOffhand)
                && available() && ModKeyMappings.SCAN_TAROT.isDown() && ModKeyMappings.SCAN_TAROT.getKey().equals(mapping.getKey());
    }
    public static void tick(ClientTickEvent.Post event) {
        lastTickMillis = Util.getMillis();
        var mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !mc.player.isAlive() || TarotDecorationClient.active()) clearPresentation();
        TarotCardEntity card = scannableCard();
        boolean holding = card != null && ModKeyMappings.SCAN_TAROT.isDown();
        while (ModKeyMappings.SCAN_TAROT.consumeClick()) { /* held interaction */ }
        if (!holding) { cancel(); return; }
        if (target != card.getId()) { cancel(); target = card.getId(); }
        if (elapsed % 5 == 0) PacketDistributor.sendToServer(new TarotScanPayload(target, true));
        elapsed++;
    }
    private static void cancel() {
        if (target != -1 && Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new TarotScanPayload(target, false));
        target = -1; elapsed = 0;
    }
    private static void clearPresentation() { displayed = null; animation.reset(); }
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { target = -1; elapsed = 0; clearPresentation(); TarotKnowledge.clearClient(); }
    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || TarotDecorationClient.active()) { clearPresentation(); return; }
        TarotCardEntity card = scannableCard();
        if (displayed != null && displayed.level() != mc.level) clearPresentation();
        if (displayed == null && card != null) displayed = card;
        if (displayed == null) return;
        long now = Util.getMillis();
        boolean completed = displayed.isCollecting() || TarotKnowledge.clientKnows(displayed.cardId());
        boolean visible = card == displayed;
        float fraction = Math.clamp((now - lastTickMillis) / 50F, 0, 1);
        float progress = target == displayed.getId() ? Math.min(1, (elapsed + fraction) / TarotScanning.SCAN_TICKS) : 0;
        animation.update(now, visible, progress, completed);
        if (animation.opacity() < .01F && (!visible || animation.completed())) { clearPresentation(); return; }
        float opacity = animation.opacity();
        var definition = TarotCatalog.find(displayed.cardId());
        int panelHeight = 38 + definition.rewards().size() * 12;
        int w = Math.min(260, g.guiWidth() - 24), x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 56 - panelHeight;
        g.pose().pushPose();
        g.pose().translate(0, (1 - opacity) * 8, 0);
        UiRenderer.panel(g, x, y, w, panelHeight, fade(0xD0181F28, opacity), fade(UiRenderer.PANEL_EDGE, opacity));
        Component label = animation.completed() ? Component.translatable("tarot.cyberneticenhancements.archived", definition.name())
                : Component.translatable("tarot.cyberneticenhancements.hold", ModKeyMappings.SCAN_TAROT.getTranslatedKeyMessage(), definition.name());
        // Font treats alpha values below four as fully opaque; skip text at that threshold.
        if (opacity >= .02F) {
            g.drawCenteredString(mc.font, label, x + w / 2, y + 7, fade(UiRenderer.ACCENT, opacity));
            for (int i = 0; i < definition.rewards().size(); i++)
                g.drawCenteredString(mc.font, definition.rewards().get(i).text(), x + w / 2, y + 21 + i * 12, fade(UiRenderer.TEXT_SECONDARY, opacity));
        }
        int barY = y + panelHeight - 12;
        g.fill(x + 10, barY, x + w - 10, barY + 3, fade(0xFF303C48, opacity));
        g.fill(x + 10, barY, x + 10 + Math.round((w - 20) * animation.progress()), barY + 3,
                fade(UiRenderer.blend(UiRenderer.ACCENT, 0xFF9AE8BB, animation.completion()), opacity));
        g.pose().popPose();
    }
    private static int fade(int color, float opacity) { return Math.round((color >>> 24) * opacity) << 24 | color & 0xFFFFFF; }
    private TarotScanClient() {}
}
