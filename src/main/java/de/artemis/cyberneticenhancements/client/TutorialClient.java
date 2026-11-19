package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.TutorialKeyRules;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import de.artemis.cyberneticenhancements.common.tutorial.TutorialSignalPayload;
import de.artemis.cyberneticenhancements.common.tutorial.TutorialStatePayload;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The objective panel and client side of a tutorial session; inactive in every other world. */
public final class TutorialClient {
    private static final long FINISH_HOLD_MILLIS = 3500;
    private static final int ACCENT = 0xFFFF5266, BODY = 0xFFE6A3AE, MUTED = 0xFFA9707C, WIDTH = 190;
    private static TutorialStatePayload state;
    private static long finishedAt = -1;

    private TutorialClient() {}

    /** Vanilla HUD layers grouped by the element that introduces them. */
    private static final java.util.Map<net.minecraft.resources.ResourceLocation, String> LAYERS = java.util.Map.ofEntries(
            java.util.Map.entry(VanillaGuiLayers.HOTBAR, "hotbar"), java.util.Map.entry(VanillaGuiLayers.SELECTED_ITEM_NAME, "hotbar"),
            java.util.Map.entry(VanillaGuiLayers.PLAYER_HEALTH, "status"), java.util.Map.entry(VanillaGuiLayers.ARMOR_LEVEL, "status"),
            java.util.Map.entry(VanillaGuiLayers.FOOD_LEVEL, "status"), java.util.Map.entry(VanillaGuiLayers.AIR_LEVEL, "status"),
            java.util.Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, "status"), java.util.Map.entry(VanillaGuiLayers.EFFECTS, "status"),
            java.util.Map.entry(VanillaGuiLayers.EXPERIENCE_BAR, "experience"), java.util.Map.entry(VanillaGuiLayers.EXPERIENCE_LEVEL, "experience"),
            java.util.Map.entry(VanillaGuiLayers.CHAT, "chat"));

    public static boolean active() { return state != null; }

    private static net.minecraft.server.MinecraftServer checkedServer;
    private static boolean tutorialServer;

    /** True from the first frame of the tutorial world, before its first objective has arrived; cached per server for the per-tick key checks. */
    public static boolean inTutorialWorld() {
        var minecraft = Minecraft.getInstance();
        var server = minecraft == null ? null : minecraft.getSingleplayerServer();
        if (server != checkedServer) {
            checkedServer = server;
            tutorialServer = server != null && de.artemis.cyberneticenhancements.common.tutorial.TutorialDirector.isTutorialWorld(server);
        }
        return tutorialServer;
    }

    /** Whether the tutorial is limiting the interface: from the world's first frame until the run is completed. */
    public static boolean restricting() { return inTutorialWorld() && (state == null || !state.finished()); }

    /** Outside the tutorial everything shows; inside it, only what the script has introduced so far. */
    public static boolean shows(String element) {
        return !restricting() || state != null && state.revealed().contains(element);
    }

    /** Whether a key mapping is blocked right now; used by the key mixin on every poll. */
    public static boolean blocks(KeyMapping mapping) {
        return restricting() && TutorialKeyRules.blocked(mapping.getName(), true, TutorialClient::shows);
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        String element = LAYERS.get(event.getName());
        if (element == null || shows(element)) return;
        event.setCanceled(true);
        // The mod draws its overlays, including scripted calls, after the hotbar layer; keep them when only the hotbar is hidden.
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()))
            ClientModEvents.onRenderGuiLayer(new RenderGuiLayerEvent.Post(event.getGuiGraphics(), event.getPartialTick(), event.getName(), event.getLayer()));
    }

    public static void receive(TutorialStatePayload payload) {
        state = payload;
        // Presses made while an action was still blocked must not fire all at once when it unlocks.
        for (var mapping : Minecraft.getInstance().options.keyMappings) while (mapping.consumeClick()) { }
        if (!payload.finished()) return;
        // Only a completed run counts as done; leaving early keeps the tutorial open.
        TutorialProgress.set(TutorialProgress.State.DONE);
        finishedAt = Util.getMillis();
        Minecraft.getInstance().getNarrator().sayNow(Component.translatable("tutorial.cyberneticenhancements.complete"));
    }

    public static void signal(String signal) {
        if (active() && !state.finished()) PacketDistributor.sendToServer(new TutorialSignalPayload(signal));
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (finishedAt >= 0 && Util.getMillis() - finishedAt >= FINISH_HOLD_MILLIS) leave();
    }

    /** The same path as the pause menu's quit for a local world; the folder is removed back at the title. */
    public static void leave() {
        var mc = Minecraft.getInstance();
        finishedAt = -1;
        if (mc.level == null) return;
        mc.level.disconnect();
        mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        mc.setScreen(new TitleScreen());
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) TutorialWorld.cleanup();
        if (event.getNewScreen() instanceof de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen) signal("archive_opened");
        if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            if (!shows("chat")) event.setCanceled(true);
            else signal("chat_opened");
        }
    }

    /** Back to Game, Options and Leave Tutorial only; advancements, stats, LAN, feedback and mod lists do not apply here. */
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!active() || !(event.getScreen() instanceof PauseScreen screen)) return;
        Button resume = null, options = null, leave = null;
        for (var child : java.util.List.copyOf(screen.children())) {
            if (!(child instanceof net.minecraft.client.gui.components.AbstractButton widget)) continue;
            String key = widget.getMessage().getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
            switch (key) {
                case "menu.returnToGame" -> resume = (Button) widget;
                case "menu.options" -> options = (Button) widget;
                case "menu.returnToMenu" -> leave = (Button) widget;
                default -> event.removeListener(widget);
            }
        }
        if (resume == null || options == null || leave == null) return;
        leave.setMessage(Component.translatable("tutorial.cyberneticenhancements.leave"));
        int x = screen.width / 2 - 102, y = resume.getY();
        for (var button : java.util.List.of(resume, options, leave)) {
            button.setWidth(204);
            button.setPosition(x, y);
            y += 24;
        }
    }

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
        finishedAt = -1;
    }

    public static void render(GuiGraphics graphics) {
        if (state == null) return;
        var mc = Minecraft.getInstance();
        var font = mc.font;
        Component heading = Component.translatable(state.finished() ? "tutorial.cyberneticenhancements.complete" : state.chapter());
        var lines = state.finished() ? java.util.List.<net.minecraft.util.FormattedCharSequence>of()
                : font.split(Component.translatable(state.objective(), args()), WIDTH - 16);
        Component progress = state.finished() ? Component.empty()
                : Component.translatable("tutorial.cyberneticenhancements.progress", state.step() + 1, state.total());
        Component distance = state.target().filter(pos -> mc.player != null)
                .map(pos -> (Component) Component.translatable("tutorial.cyberneticenhancements.distance",
                        Math.round(Math.sqrt(mc.player.distanceToSqr(pos.getCenter()))))).orElse(null);
        int x = 8, y = 8, height = 24 + lines.size() * 11 + (distance != null ? 11 : 0) + (state.finished() ? 0 : 11);
        VanillaMenuPresentation.menuPanel(graphics, x, y, WIDTH, height);
        UiRenderer.outline(graphics, x, y, WIDTH, height, VanillaMenuPresentation.EDGE);
        graphics.drawString(font, heading, x + 8, y + 8, ACCENT, true);
        int line = y + 22;
        for (var text : lines) {
            graphics.drawString(font, text, x + 8, line, BODY, true);
            line += 11;
        }
        if (distance != null) {
            graphics.drawString(font, distance, x + 8, line, MUTED, true);
            line += 11;
        }
        if (!state.finished()) graphics.drawString(font, progress, x + 8, line, MUTED, true);
    }

    /** "key:<mapping>" shows the player's current binding; anything else is a translation key. */
    private static Object[] args() {
        var options = Minecraft.getInstance().options;
        return state.args().stream().map(arg -> {
            if (!arg.startsWith("key:")) return (Object) Component.translatable(arg);
            String name = arg.substring(4);
            for (var mapping : options.keyMappings)
                if (mapping.getName().equals(name)) return mapping.getTranslatedKeyMessage().copy().withStyle(net.minecraft.ChatFormatting.WHITE);
            return Component.literal(name);
        }).toArray();
    }
}
