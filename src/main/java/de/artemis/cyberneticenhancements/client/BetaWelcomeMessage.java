package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class BetaWelcomeMessage {
    private static final String ISSUES = "https://github.com/IntelligenceModding/Cybernetic-Enhancements/issues/new/choose";
    private static final String DISCORD = "https://discord.intelligence-modding.de/";
    private static final int ACCENT = 0x8BDAF8, MUTED = 0xAAB8C5, BETA = 0xF2C66D;
    private static final String DISABLE_COMMAND = "cyberneticenhancements-client disable-welcome";
    private static Connection pendingConnection;
    private static int readyTicks;
    private static long noticeGeneration;

    private BetaWelcomeMessage() {
    }

    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cyberneticenhancements-client")
                .then(Commands.literal("disable-welcome").executes(context -> {
                    ClientConfig.SHOW_BETA_WELCOME_MESSAGE.set(false);
                    ClientConfig.SHOW_BETA_WELCOME_MESSAGE.save();
                    pendingConnection = null;
                    readyTicks = 0;
                    noticeGeneration++;
                    Minecraft.getInstance().gui.getChat().addMessage(
                            Component.translatable("message.cyberneticenhancements.beta.disabled").withColor(MUTED));
                    return 1;
                })));
    }

    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        noticeGeneration++;
        pendingConnection = event.getConnection();
        readyTicks = 0;
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        noticeGeneration++;
        pendingConnection = null;
        readyTicks = 0;
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (pendingConnection == null) return;
        // The tutorial world has its own guidance; no welcome sound or chat notice there.
        if (!ClientConfig.SHOW_BETA_WELCOME_MESSAGE.get() || TutorialClient.inTutorialWorld()) {
            pendingConnection = null;
            readyTicks = 0;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null
                || minecraft.getConnection().getConnection() != pendingConnection
                || minecraft.screen instanceof ReceivingLevelScreen || minecraft.getOverlay() != null) return;

        if (++readyTicks < 20) return;
        Connection joinedConnection = pendingConnection;
        long joinedGeneration = noticeGeneration;
        pendingConnection = null;
        String version = ModList.get().getModContainerById(CyberneticEnhancements.MOD_ID)
                .orElseThrow().getModInfo().getVersion().toString();
        minecraft.gui.getChat().addMessage(message(version, SharedConstants.getCurrentVersion().getName()));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.1F, 0.3F));
        if (ClientConfig.CHECK_FOR_UPDATES.get()) {
            CurseForgeUpdateChecker.check(SharedConstants.getCurrentVersion().getName(), version).thenAccept(update ->
                    minecraft.execute(() -> {
                        if (minecraft.player == null || minecraft.getConnection() == null
                                || minecraft.getConnection().getConnection() != joinedConnection || noticeGeneration != joinedGeneration
                                || !ClientConfig.SHOW_BETA_WELCOME_MESSAGE.get() || !ClientConfig.CHECK_FOR_UPDATES.get()) return;
                        update.ifPresent(release -> minecraft.gui.getChat().addMessage(updateMessage(version, release)));
                    }));
        }
    }

    private static Component message(String version, String minecraftVersion) {
        Component body = welcomeBody(version, minecraftVersion);
        Minecraft minecraft = Minecraft.getInstance();
        var chat = minecraft.gui.getChat();
        int chatWidth = Math.max(1, (int) Math.floor(chat.getWidth() / chat.getScale()));
        int textWidth = net.minecraft.client.gui.components.ComponentRenderUtils.wrapComponents(body, chatWidth, minecraft.font)
                .stream().mapToInt(minecraft.font::width).max().orElse(0);
        int glyphWidth = Math.max(1, minecraft.font.width("\u2501"));
        int length = Math.max(1, Math.min(chatWidth / glyphWidth, (textWidth + glyphWidth - 1) / glyphWidth));
        Component separator = Component.literal("\u2501".repeat(length)).withColor(0x425D6C);
        return Component.empty().append(separator).append("\n").append(body).append("\n").append(separator.copy());
    }

    private static Component updateMessage(String version, CurseForgeUpdateChecker.Release release) {
        return Component.empty()
                .append(Component.translatable("message.cyberneticenhancements.beta.update", version, release.version()).withColor(BETA))
                .append("\n").append(Component.translatable("message.cyberneticenhancements.beta.update_advice").withColor(MUTED))
                .append("\n").append(link("message.cyberneticenhancements.beta.update_link", "message.cyberneticenhancements.beta.update_hover", release.url(), ACCENT))
                .append("\n").append(disableLink());
    }

    private static Component welcomeBody(String version, String minecraftVersion) {
        return Component.empty()
                .append(Component.literal("Cybernetic Enhancements ").withColor(ACCENT).withStyle(net.minecraft.ChatFormatting.BOLD))
                .append(Component.literal("[BETA]\n").withColor(BETA).withStyle(net.minecraft.ChatFormatting.BOLD))
                .append(Component.translatable("message.cyberneticenhancements.beta.version", version, minecraftVersion).withColor(MUTED)).append("\n")
                .append(Component.translatable("message.cyberneticenhancements.beta.notice").withColor(BETA)).append("\n")
                .append(Component.translatable("message.cyberneticenhancements.beta.feedback").withColor(MUTED)).append("\n")
                .append(link("message.cyberneticenhancements.beta.issues", "message.cyberneticenhancements.beta.issues_hover", ISSUES, 0x81D89B))
                .append("  ")
                .append(link("message.cyberneticenhancements.beta.discord", "message.cyberneticenhancements.beta.discord_hover", DISCORD, ACCENT))
                .append("\n")
                .append(Component.translatable("message.cyberneticenhancements.beta.community").withColor(MUTED))
                .append("\n")
                .append(disableLink());
    }

    private static Component disableLink() {
        return Component.translatable("message.cyberneticenhancements.beta.disable").withStyle(style -> style
                        .withColor(MUTED).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + DISABLE_COMMAND))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.cyberneticenhancements.beta.disable_hover"))));
    }
    private static Component link(String label, String tooltip, String url, int color) {
        return Component.translatable(label).withStyle(style -> style.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(tooltip))));
    }
}
