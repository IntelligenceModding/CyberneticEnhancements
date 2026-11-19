package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.render.TarotCardRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.tarot.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Local preview only; the server independently validates every placement. */
public final class TarotDecorationClient {
    private static String selected;
    private static boolean pending;
    private static TarotDecorations.Placement rejected;
    public static boolean active() { return selected != null; }
    public static void start(String id) {
        cancel();
        if (TarotCatalog.find(id) != null && TarotKnowledge.clientKnows(id)) selected = id;
    }
    private static void cancel() { selected = null; pending = false; rejected = null; }
    public static void receive(TarotDecorationResultPayload payload) {
        if (!payload.cardId().equals(selected)) return;
        pending = false;
        if (payload.placed()) { Minecraft.getInstance().options.keyUse.setDown(false); cancel(); }
        else if (Minecraft.getInstance().player != null) rejected = TarotDecorations.preview(Minecraft.getInstance().player);
    }
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (active() && (mc.screen != null || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator() || !mc.player.mayBuild()
                || !TarotKnowledge.clientKnows(selected))) cancel();
    }
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { cancel(); }
    public static void opening(ScreenEvent.Opening event) {
        if (!active() || event.getNewScreen() == null) return;
        boolean escape = event.getNewScreen() instanceof PauseScreen;
        cancel();
        if (escape) event.setCanceled(true);
    }
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active() || Minecraft.getInstance().screen != null) return;
        event.setCanceled(true); event.setSwingHand(false);
        if (event.isAttack()) { Minecraft.getInstance().options.keyAttack.setDown(false); cancel(); return; }
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || pending) return;
        var mc = Minecraft.getInstance(); if (mc.player == null) return;
        var at = TarotDecorations.preview(mc.player);
        if (at == null || !TarotDecorations.fits(mc.level, at)) return;
        pending = true;
        PacketDistributor.sendToServer(new TarotDecorationPayload(selected, at));
    }
    public static void render(RenderLevelStageEvent event) {
        if (!active() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance(); if (mc.level == null || mc.player == null || mc.screen != null) return;
        var at = TarotDecorations.preview(mc.player); if (at == null) return;
        boolean valid = TarotDecorations.fits(mc.level, at) && !at.equals(rejected);
        var origin = TarotCardEntity.attachmentPosition(at.anchor(), at.facing());
        var camera = event.getCamera().getPosition();
        var pose = new PoseStack(); pose.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        var buffers = mc.renderBuffers().bufferSource();
        var type = TarotCardRenderer.preview(selected, at.facing(), valid, mc.player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false), pose, buffers);
        if (type != null) { buffers.endBatch(type); buffers.endBatch(net.minecraft.client.renderer.RenderType.lines()); }
    }
    public static void renderHud(GuiGraphics g) {
        var mc = Minecraft.getInstance(); if (!active() || mc.player == null || mc.screen != null) return;
        var at = TarotDecorations.preview(mc.player);
        boolean valid = at != null && TarotDecorations.fits(mc.level, at) && !at.equals(rejected);
        int width = Math.min(350, g.guiWidth() - 24), x = (g.guiWidth() - width) / 2, y = g.guiHeight() - 128;
        UiRenderer.panel(g, x, y, width, 60, 0xD0181F28);
        g.drawCenteredString(mc.font, Component.translatable("tarot.cyberneticenhancements.placement.title", TarotCatalog.find(selected).name()), x + width / 2, y + 7, UiRenderer.ACCENT);
        String state = pending ? "pending" : valid ? "ready" : at != null && at.equals(rejected) ? "rejected" : "wall";
        g.drawCenteredString(mc.font, Component.translatable("tarot.cyberneticenhancements.placement." + state), x + width / 2, y + 23, valid ? 0xFF88E9B0 : UiRenderer.WARNING);
        g.drawCenteredString(mc.font, Component.translatable("tarot.cyberneticenhancements.placement.controls",
                mc.options.keyUse.getTranslatedKeyMessage(), mc.options.keyAttack.getTranslatedKeyMessage()), x + width / 2, y + 41, UiRenderer.TEXT_SECONDARY);
    }
    private TarotDecorationClient() {}
}
