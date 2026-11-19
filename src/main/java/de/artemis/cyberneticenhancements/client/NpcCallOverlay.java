package de.artemis.cyberneticenhancements.client;

import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.ui.NeuralLinkStyle;
import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.client.ui.UiVisibilityTransition;
import de.artemis.cyberneticenhancements.client.ui.CallHudLayout;
import de.artemis.cyberneticenhancements.client.ui.HudText;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import de.artemis.cyberneticenhancements.client.ui.UiOpeningAnimation;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class NpcCallOverlay {
    private static final String PREFIX = "screen.cyberneticenhancements.call.";
    private static final int WIDTH = NeuralLinkStyle.WIDTH;
    private static NpcCallOverlay active;
    private static boolean windowToggleDown;
    private UUID callId;
    private final UUID npcId;
    private boolean playerCall;
    private boolean windowHidden;
    private final UiVisibilityTransition windowVisibility = new UiVisibilityTransition(true);
    private final String name;
    private String turnInQuestId = "";
    private final long openedAt = System.nanoTime();
    private final UiOpeningAnimation opening = new UiOpeningAnimation(openedAt, 1, NeuralLinkStyle.OPEN_MILLIS);
    private final UiFadeLayer fade = new UiFadeLayer();
    private UiOpeningAnimation closing;
    private long closingAt;
    private NpcCallStatePayload state;
    private Component contactName;
    private AbstractCityNpcEntity portrait;
    private net.minecraft.client.player.RemotePlayer playerPortrait;
    private boolean awaiting, connectedSound;
    private final boolean[] answerDown = new boolean[5];
    private long repliedAt, endedAt, requestedAt, heartbeatAt;
    private CallRingtone ringtone;

    private NpcCallOverlay(ArchiveContactsPayload.ContactEntry contact) {
        this(UUID.randomUUID(), UUID.fromString(contact.npcId()), contact.name());
        contactName = contact.displayName();
    }

    private NpcCallOverlay(UUID callId, UUID npcId, String name) {
        this.callId = callId;
        this.npcId = npcId;
        this.name = name;
        this.contactName = Component.literal(name);
    }

    public static void start(ArchiveContactsPayload.ContactEntry contact) {
        var call = new NpcCallOverlay(contact);
        call.playerCall = contact.isPlayer();
        start(call);
    }

    public static void startTurnIn(String questId, de.artemis.cyberneticenhancements.common.network.QuestProgressPayload.CallTarget issuer) {
        var call = new NpcCallOverlay(UUID.randomUUID(), issuer.issuerId(), issuer.name().getString());
        call.contactName = issuer.name();
        call.turnInQuestId = questId;
        start(call);
    }

    private static void start(NpcCallOverlay call) {
        NpcContactsOverlay.reset();
        close();
        reset();
        for (var key : ModKeyMappings.CALL_ANSWERS) while (key.consumeClick()) {}
        while (ModKeyMappings.END_CALL.consumeClick()) {}
        active = call;
        active.request(NpcCallRequestPayload.START);
    }

    public static boolean isActive() { return active != null && active.closing == null; }
    private float windowOpacity() { return windowVisibility.sample(!windowHidden, System.nanoTime() / 1_000_000_000D); }
    public static boolean isVisible() { return active != null && active.windowOpacity() > .001F; }
    public static boolean canTogglePlayerWindow() {
        return isActive() && active.playerCall && active.connected() && !active.state.ended();
    }
    public static boolean hasLiveCall() { return isActive() && (active.state == null || !active.state.ended()); }

    public static void receivePlayer(de.artemis.cyberneticenhancements.common.network.PlayerCallStatePayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (payload.incoming()) {
            if (!ClientConfig.ENABLE_PLAYER_CALLS.get() || mc.getPlayerSocialManager().isBlocked(payload.peerId())
                    || mc.getPlayerSocialManager().isHidden(payload.peerId())
                    || hasLiveCall() && (!active.playerCall || !active.callId.equals(payload.callId()))) {
                PacketDistributor.sendToServer(new de.artemis.cyberneticenhancements.common.network.PlayerCallRequestPayload(payload.callId(), payload.peerId(), 0));
                return;
            }
            if (!hasLiveCall()) {
                NpcContactsOverlay.reset();
                reset();
                active = new NpcCallOverlay(payload.callId(), payload.peerId(), payload.name());
                active.playerCall = true;
                for (var key : ModKeyMappings.CALL_ANSWERS) while (key.consumeClick()) {}
                while (ModKeyMappings.END_CALL.consumeClick()) {}
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(mc.player, Component.translatable(PREFIX + "incoming.notice", payload.name()));
            }
        }
        if (!isActive() || !active.playerCall || !active.npcId.equals(payload.peerId())) return;
        if (active.state == null) {
            if (!payload.incoming() && !active.callId.equals(payload.requestId())) return;
            active.callId = payload.callId();
        }
        if (!active.callId.equals(payload.callId())) return;
        boolean connected = payload.status().equals("connected");
        boolean ended = !connected && !payload.status().equals("ringing");
        var choices = payload.incoming() ? List.of(
                new NpcCallStatePayload.Choice(15, Component.translatable(PREFIX + "player.answer")),
                new NpcCallStatePayload.Choice(0, Component.translatable(PREFIX + "player.decline")))
                : connected ? List.of(
                new NpcCallStatePayload.Choice(1, Component.translatable(PREFIX + "player." + (payload.muted() ? "unmute" : "mute"))),
                new NpcCallStatePayload.Choice(2, Component.translatable(PREFIX + "player.block")),
                new NpcCallStatePayload.Choice(-99, Component.empty()), new NpcCallStatePayload.Choice(-99, Component.empty()),
                new NpcCallStatePayload.Choice(0, Component.translatable(PREFIX + "player.hang_up"))) : List.<NpcCallStatePayload.Choice>of();
        active.state = new NpcCallStatePayload(payload.callId(), payload.peerId(), "player", payload.name(), "generic", "", "aqua",
                Component.translatable(PREFIX + "player." + (connected && payload.muted() ? "microphone_muted" : payload.status())), choices, ended, payload.incoming());
        active.awaiting = false;
        active.repliedAt = System.nanoTime();
        if (ended) { active.endedAt = active.repliedAt; active.stopRingtone(); }
    }

    public static void receive(NpcCallStatePayload payload) {
        if (payload.incoming() && !payload.ended()) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) return;
            if (active == null || active.closing != null || (active.state != null && active.state.ended())) {
                NpcContactsOverlay.reset();
                reset();
                for (var key : ModKeyMappings.CALL_ANSWERS) while (key.consumeClick()) {}
                while (ModKeyMappings.END_CALL.consumeClick()) {}
                active = new NpcCallOverlay(payload.callId(), payload.npcId(), payload.name());
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(minecraft.player, Component.translatable(PREFIX + "incoming.notice", payload.displayName()));
            } else if (!active.callId.equals(payload.callId())) {
                PacketDistributor.sendToServer(new NpcCallRequestPayload(payload.callId(), payload.npcId(), NpcCallRequestPayload.END));
                return;
            }
        }
        if (!isActive() || !active.callId.equals(payload.callId()) || !active.npcId.equals(payload.npcId())) return;
        active.state = payload;
        active.awaiting = false;
        active.repliedAt = System.nanoTime();
        active.portrait = null;
        if (payload.ended()) { active.endedAt = active.repliedAt; active.stopRingtone(); }
    }

    private void request(int action) {
        if (Minecraft.getInstance().getConnection() == null) return;
        if (playerCall) PacketDistributor.sendToServer(new de.artemis.cyberneticenhancements.common.network.PlayerCallRequestPayload(callId, npcId, action));
        else PacketDistributor.sendToServer(new NpcCallRequestPayload(callId, npcId, action,
                action == NpcCallRequestPayload.START ? turnInQuestId : ""));
        if (action != NpcCallRequestPayload.KEEPALIVE) requestedAt = System.nanoTime();
    }

    public static void answer(int index) {
        if (!isActive() || active.windowHidden || active.state == null || (!active.connected() && !active.state.incoming()) || active.state.ended()
                || active.awaiting || index < 0 || index >= active.state.choices().size()) return;
        int action = active.state.choices().get(index).action();
        if (action == -99) return;
        if (active.playerCall && action == 1 && System.nanoTime() - active.requestedAt < 300_000_000L) return;
        active.stopRingtone();
        if (action == NpcCallRequestPayload.END) close();
        else {
            active.awaiting = true;
            active.request(action);
        }
    }

    public static void tick() {
        var toggleKey = ModKeyMappings.TOGGLE_PLAYER_CALL_WINDOW;
        boolean toggleClicked = toggleKey.consumeClick();
        while (toggleKey.consumeClick()) {}
        boolean toggleRequested = toggleClicked && !windowToggleDown;
        windowToggleDown = toggleKey.isDown();
        if (active == null) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !minecraft.player.isAlive()) {
            close();
            reset();
            return;
        }
        boolean controlsVisible = GameplayHudVisibility.gameplay()
                && GameplayHudVisibility.shows(GameplayHudVisibility.Element.CALLS, true);
        if (toggleRequested && canTogglePlayerWindow() && controlsVisible) {
            active.windowOpacity();
            active.windowHidden = !active.windowHidden;
            active.windowOpacity();
        }
        if (active.closing != null) {
            if (active.closing.sample(System.nanoTime()).complete()) reset();
            return;
        }
        if (minecraft.player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                && minecraft.level.getEntity(menu.entityId()) instanceof AbstractCityNpcEntity npc && active.npcId.equals(npc.getUUID())) {
            close();
            return;
        }
        if (controlsVisible && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof AbstractCityNpcEntity npc && active.npcId.equals(npc.getUUID())
                && active.state != null && !active.state.ended() && !active.state.incoming()) {
            active.request(NpcCallRequestPayload.TALK_IN_PERSON);
            close();
            return;
        }
        for (int index = 0; index < ModKeyMappings.CALL_ANSWERS.size(); index++) {
            var key = ModKeyMappings.CALL_ANSWERS.get(index);
            boolean clicked = key.consumeClick();
            while (key.consumeClick()) {}
            if (controlsVisible && clicked && !active.answerDown[index]) answer(index);
            if (!isActive()) return;
            active.answerDown[index] = key.isDown();
        }
        while (ModKeyMappings.END_CALL.consumeClick()) {
            if (controlsVisible) close();
        }
        if (!isActive()) return;
        long now = System.nanoTime();
        boolean ringing = active.ringing() && !active.awaiting;
        boolean incoming = active.state != null && active.state.incoming();
        if (ringing && (active.ringtone == null || active.ringtone.incoming != incoming)) {
            active.stopRingtone();
            active.ringtone = new CallRingtone(incoming);
            minecraft.getSoundManager().play(active.ringtone);
        } else if (!ringing) active.stopRingtone();
        if ((active.state == null || active.awaiting) && now - active.requestedAt > 10_000_000_000L) {
            active.state = new NpcCallStatePayload(active.callId, active.npcId, "", active.name, "generic", "", "aqua",
                    Component.translatable(PREFIX + "lost"), List.of(), true);
            active.endedAt = now;
            active.awaiting = false;
        }
        if (active.connected() && !active.connectedSound) {
            active.connectedSound = true;
            sound(active.state.ended() ? 0.6F : 1.2F);
        }
        if (active.endedAt != 0 && now - active.endedAt > 3_000_000_000L) {
            close();
            return;
        }
        if (now - active.heartbeatAt > 15_000_000_000L && active.state != null && !active.state.ended()) {
            active.request(NpcCallRequestPayload.KEEPALIVE);
            active.heartbeatAt = now;
        }
    }

    public static void render(GuiGraphics original) {
        var minecraft = Minecraft.getInstance();
        if (!isVisible() || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;
        var viewport = NeuralLinkStyle.viewport();
        original.flush();
        var call = active;
        long now = System.nanoTime();
        var entry = call.opening.sample(call.closing == null ? now : call.closingAt);
        float exit = call.closing == null ? 0 : call.closing.sample(now).wikiOpacity();
        float opacity = entry.wikiOpacity() * (1 - exit) * call.windowOpacity();
        if (opacity <= 0) return;
        int x = NeuralLinkStyle.x(viewport, ClientConfig.CALL_WINDOW_ON_LEFT.get());
        float renderScale = (float) viewport.renderScale();
        var content = call.connected() ? call.content() : null;
        int panelHeight = content == null ? call.ringingHeight() : content.layout().height();
        float fitScale = Math.min(1F, (viewport.height() - 2F * NeuralLinkStyle.MARGIN) / panelHeight);
        float anchorX = ClientConfig.CALL_WINDOW_ON_LEFT.get() ? x : x + WIDTH;
        call.fade.render(original, opacity, entry.wikiScale() * (1 - 0.04F * exit) * fitScale,
                0, anchorX * renderScale, NeuralLinkStyle.MARGIN * renderScale, () -> {
                    GuiGraphics graphics = new UiGraphics(original, viewport);
                    try { call.draw(graphics, x, NeuralLinkStyle.MARGIN, content); }
                    finally { graphics.flush(); }
                });
    }

    private int keyColumnWidth() {
        var font = Minecraft.getInstance().font;
        return CallHudLayout.keyWidth(ModKeyMappings.CALL_ANSWERS.stream()
                .mapToInt(key -> font.width(key.getTranslatedKeyMessage())).max().orElse(0), WIDTH - NeuralLinkStyle.INSET * 2);
    }

    private int ringingHeight() {
        int height = NeuralLinkStyle.CONTENT_TOP + 92;
        if (state != null && state.incoming()) {
            int labelWidth = WIDTH - NeuralLinkStyle.INSET * 2 - keyColumnWidth() - CallHudLayout.GAP - 8;
            for (var choice : state.choices()) height += Math.max(22, Minecraft.getInstance().font.split(choice.label(), labelWidth).size() * 11 + 10) + CallHudLayout.GAP;
            return height - CallHudLayout.GAP + NeuralLinkStyle.INSET;
        }
        return height + 9 + NeuralLinkStyle.INSET;
    }

    private record CallContent(CallHudLayout layout, List<net.minecraft.util.FormattedCharSequence> dialogue,
                               List<List<net.minecraft.util.FormattedCharSequence>> labels, int keyWidth) {}

    private CallContent content() {
        var font = Minecraft.getInstance().font;
        Component dialogue = state.dialogue();
        if (playerCall && !state.ended() && de.artemis.cyberneticenhancements.common.dialogue.CallVoiceBridge.microphoneMuted())
            dialogue = Component.translatable(PREFIX + "player.external_microphone_muted");
        else if (playerCall && !state.ended() && de.artemis.cyberneticenhancements.common.dialogue.CallVoiceBridge.talking(npcId))
            dialogue = Component.translatable(PREFIX + "player.speaking", name);
        var lines = font.split(dialogue, WIDTH - NeuralLinkStyle.INSET * 2 - 12);
        int keyWidth = keyColumnWidth();
        var labels = state.choices().stream().map(choice -> choice.action() == -99 ? List.<net.minecraft.util.FormattedCharSequence>of()
                : font.split(choice.label(), WIDTH - NeuralLinkStyle.INSET * 2 - keyWidth - CallHudLayout.GAP - 8)).toList();
        var layout = CallHudLayout.connected(lines.size(), labels.stream().mapToInt(List::size).toArray(), playerCall, state.ended());
        return new CallContent(layout, lines, labels, keyWidth);
    }

    private void drawOption(GuiGraphics graphics, int x, int y, int height, int index, int keyWidth,
                            List<net.minecraft.util.FormattedCharSequence> lines) {
        var font = Minecraft.getInstance().font;
        int innerWidth = WIDTH - NeuralLinkStyle.INSET * 2;
        GameplayHudStyle.option(graphics, x, y, innerWidth, height, !awaiting);
        GameplayHudStyle.shortcutKey(graphics, font, ModKeyMappings.CALL_ANSWERS.get(index).getTranslatedKeyMessage(),
                x, y, height, keyWidth, !awaiting, 1);
        int textY = y + (height - lines.size() * 11) / 2 + 1;
        for (var line : lines) {
            graphics.drawString(font, line, x + keyWidth + CallHudLayout.GAP, textY, UiRenderer.TEXT, false);
            textY += 11;
        }
    }

    private void draw(GuiGraphics graphics, int x, int y, CallContent content) {
        var font = Minecraft.getInstance().font;
        if (!connected()) {
            boolean incoming = state != null && state.incoming();
            NeuralLinkStyle.panel(graphics, x, y, ringingHeight(), UiRenderer.ACCENT);
            renderHeader(graphics, x, y, UiRenderer.ACCENT);
            Component end = ModKeyMappings.END_CALL.getTranslatedKeyMessage();
            for (int index = 0; index < 3; index++) {
                int size = 12 + index * 14;
                int color = (elapsed() / 350) % 3 == index ? UiRenderer.ACCENT : UiRenderer.PANEL_EDGE;
                UiRenderer.outline(graphics, x + (WIDTH - size) / 2, y + NeuralLinkStyle.CONTENT_TOP + 28 - size / 2, size, size, color);
            }
            Component status = Component.translatable(PREFIX + (state == null || awaiting ? "connecting" : incoming ? "incoming.ringing" : "ringing"));
            var statusLines = HudText.wrap(font, status, WIDTH - NeuralLinkStyle.INSET * 2, 2);
            for (int line = 0; line < statusLines.size(); line++)
                graphics.drawString(font, statusLines.get(line), x + (WIDTH - font.width(statusLines.get(line))) / 2,
                        y + NeuralLinkStyle.CONTENT_TOP + 62 + line * 11, UiRenderer.TEXT, false);
            if (incoming) {
                int rowY = y + NeuralLinkStyle.CONTENT_TOP + 92;
                for (int index = 0; index < state.choices().size(); index++) {
                    int keyWidth = keyColumnWidth();
                    var lines = font.split(state.choices().get(index).label(), WIDTH - NeuralLinkStyle.INSET * 2 - keyWidth - CallHudLayout.GAP - 8);
                    int height = Math.max(22, lines.size() * 11 + 10);
                    drawOption(graphics, x + NeuralLinkStyle.INSET, rowY, height, index, keyWidth, lines);
                    rowY += height + CallHudLayout.GAP;
                }
            } else line(graphics, Component.translatable(PREFIX + "hang_up", end), x + NeuralLinkStyle.INSET,
                    y + NeuralLinkStyle.CONTENT_TOP + 92, WIDTH - NeuralLinkStyle.INSET * 2, UiRenderer.TEXT_SECONDARY);
            return;
        }
        var layout = content.layout();
        int inset = NeuralLinkStyle.INSET, innerWidth = WIDTH - inset * 2;
        int accent = failedCall() ? 0xFFF17883 : UiRenderer.ACCENT;
        NeuralLinkStyle.panel(graphics, x, y, layout.height(), accent);
        renderHeader(graphics, x, y, accent);
        if (state.ended()) {
            renderCallStatus(graphics, x + inset, y + layout.dialogueY(), innerWidth, layout.dialogueHeight(), accent, content.dialogue());
            line(graphics, Component.translatable(PREFIX + "ended"), x + inset, y + layout.footerY(), innerWidth, UiRenderer.TEXT_SECONDARY);
            return;
        }
        renderPortrait(graphics, x + inset, y + layout.portraitY(), innerWidth, CallHudLayout.PORTRAIT_HEIGHT);
        GameplayHudStyle.surface(graphics, x + inset, y + layout.dialogueY(), innerWidth, layout.dialogueHeight());
        for (int index = 0; index < content.dialogue().size(); index++) graphics.drawString(font, content.dialogue().get(index),
                x + inset + 6, y + layout.dialogueY() + 6 + index * 11, UiRenderer.TEXT, false);
        for (int index = 0; index < layout.rows().size(); index++) {
            var row = layout.rows().get(index);
            if (row.height() == 0) continue;
            drawOption(graphics, x + inset, y + row.y(), row.height(), index, content.keyWidth(), content.labels().get(index));
        }
        if (playerCall) line(graphics, Component.translatable(PREFIX + "player.hide_window",
                ModKeyMappings.TOGGLE_PLAYER_CALL_WINDOW.getTranslatedKeyMessage()), x + inset, y + layout.footerY(), innerWidth, UiRenderer.TEXT_SECONDARY);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int color) {
        Component npcName = state == null || state.name().isBlank() ? contactName : state.displayName();
        NeuralLinkStyle.header(graphics, x, y, npcName, Component.empty(), color);
    }

    private boolean failedCall() {
        if (!state.ended()) return false;
        if (state.dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text) {
            return !text.getKey().startsWith(PREFIX + "meeting") && !text.getKey().equals(PREFIX + "ended")
                    && !text.getKey().equals(PREFIX + "idle_hangup") && !text.getKey().equals(PREFIX + "player.ended");
        }
        return true;
    }

    private void renderCallStatus(GuiGraphics graphics, int x, int y, int width, int height, int color, List<net.minecraft.util.FormattedCharSequence> lines) {
        var font = Minecraft.getInstance().font;
        GameplayHudStyle.surface(graphics, x, y, width, height);
        for (int column = x + 10; column < x + width - 1; column += 20) {
            graphics.fill(column, y + 1, column + 1, y + height - 1, (color & 0xFFFFFF) | 0x10000000);
        }
        for (int row = y + 2; row < y + height - 1; row += 3) {
            graphics.fill(x + 1, row, x + width - 1, row + 1, (color & 0xFFFFFF) | 0x08000000);
        }
        int sweep = y + 1 + (int) ((elapsed() % 2400) * (height - 3) / 2400);
        graphics.fill(x + 1, sweep, x + width - 1, sweep + 1, (color & 0xFFFFFF) | 0x20000000);
        int textY = y + (height - lines.size() * 11) / 2;
        for (var line : lines) {
            graphics.drawString(font, line, x + (width - font.width(line)) / 2, textY, color, false);
            textY += 11;
        }
        GameplayHudStyle.separator(graphics, x, y + height - 1, width);
    }

    private AbstractCityNpcEntity portrait() {
        var minecraft = Minecraft.getInstance();
        if (portrait == null || portrait.level() != minecraft.level) {
            portrait = switch (NpcType.fromId(state.role())) {
                case FIXER -> ModEntityTypes.FIXER.get().create(minecraft.level);
                case RIPPERDOC -> ModEntityTypes.RIPPERDOC.get().create(minecraft.level);
                case STREET_RIPPERDOC -> ModEntityTypes.STREET_RIPPERDOC.get().create(minecraft.level);
                case TECHIE -> ModEntityTypes.TECHIE.get().create(minecraft.level);
                case NETRUNNER -> ModEntityTypes.NETRUNNER.get().create(minecraft.level);
                case MAXTEC -> ModEntityTypes.MAXTEC_GUARD.get().create(minecraft.level);
                case WORLD_MED_TECH -> ModEntityTypes.MED_TECH.get().create(minecraft.level);
                case MERC -> ModEntityTypes.MERC.get().create(minecraft.level); case MED_TECH -> ModEntityTypes.MED_TECH.get().create(minecraft.level);
            };
            portrait.setUUID(npcId);
            portrait.applyClientPreviewIdentity(state.appearance(), NpcCategory.fromId(state.category()), state.name(), state.nameColor());
            portrait.setCustomNameVisible(false);
        }
        return portrait;
    }

    private void renderPortrait(GuiGraphics graphics, int x, int y, int width, int height) {
        GameplayHudStyle.surface(graphics, x, y, width, height);
        for (int column = x + 10; column < x + width; column += 20) graphics.fill(column, y + 1, column + 1, y + height - 1, 0x102C4B63);
        if (playerCall) {
            var mc = Minecraft.getInstance();
            if (playerPortrait == null || playerPortrait.level() != mc.level)
                playerPortrait = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(npcId, name));
            playerPortrait.tickCount = mc.player.tickCount;
            playerPortrait.yHeadRot = playerPortrait.yHeadRotO = (float) Math.sin(elapsed() / 1700.0) * 4;
            playerPortrait.yBodyRot = playerPortrait.yBodyRotO = 0;
            playerPortrait.setYRot(0);
            graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
            Quaternionf rotation = Axis.ZP.rotationDegrees(180).mul(Axis.YP.rotationDegrees(180)).mul(Axis.XP.rotationDegrees(3));
            InventoryScreen.renderEntityInInventory(graphics, x + width / 2.0F, y + playerPortrait.getBbHeight() * 92 + 8,
                    92, new Vector3f(), new Quaternionf(rotation).conjugate(), rotation, playerPortrait);
            graphics.disableScissor();
        } else if (!state.role().isBlank()) {
            var entity = portrait();
            entity.tickCount = Minecraft.getInstance().player.tickCount;
            entity.setYRot(0);
            entity.yRotO = 0;
            entity.setYBodyRot(0);
            entity.yBodyRotO = 0;
            entity.setYHeadRot(0);
            entity.yHeadRotO = entity.getYHeadRot();
            entity.setXRot(0);
            entity.xRotO = 0;
            entity.walkAnimation.setSpeed(0);
            graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
            float scale = 92;
            float jitter = elapsed() % 4200 < 90 ? (float) Math.sin(elapsed() * 0.08) : 0;
            Quaternionf rotation = Axis.ZP.rotationDegrees(180).mul(Axis.YP.rotationDegrees(180)).mul(Axis.XP.rotationDegrees(3));
            var motion = de.artemis.cyberneticenhancements.client.render.NpcConversationMotion.sample(
                    elapsed() / 1000.0, npcId.hashCode(), (System.nanoTime() - repliedAt) / 1_000_000_000.0);
            de.artemis.cyberneticenhancements.client.render.NpcConversationPoses.renderPortrait(entity, motion,
                    () -> InventoryScreen.renderEntityInInventory(graphics, x + width / 2.0F + jitter, y + entity.getBbHeight() * scale + 8,
                            scale, new Vector3f(), new Quaternionf(rotation).conjugate(), rotation, entity));
            graphics.disableScissor();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x0839C6DE);
        for (int row = y + 2; row < y + height - 1; row += 3) graphics.fill(x + 1, row, x + width - 1, row + 1, 0x100B1721);
        int sweep = y + 1 + (int) ((elapsed() % 2400) * (height - 3) / 2400);
        graphics.fill(x + 1, sweep, x + width - 1, sweep + 1, 0x328BDAF8);
        GameplayHudStyle.separator(graphics, x, y + height - 1, width);
        graphics.pose().popPose();
    }

    private static void line(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(text, width);
        if (!lines.isEmpty()) graphics.drawString(font, lines.getFirst(), x, y, color, false);
    }

    private boolean connected() { return state != null && (state.ended() || (!state.incoming() && !state.choices().isEmpty())); }
    private boolean ringing() { return !connected(); }
    private long elapsed() { return (System.nanoTime() - openedAt) / 1_000_000L; }

    private static void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.16F));
    }

    private void stopRingtone() {
        if (ringtone == null) return;
        ringtone.finish();
        Minecraft.getInstance().getSoundManager().stop(ringtone);
        ringtone = null;
    }

    private static final class CallRingtone extends AbstractTickableSoundInstance {
        private final boolean incoming;
        private CallRingtone(boolean incoming) {
            super(incoming ? ModSounds.INCOMING_CALL.get() : ModSounds.OUTGOING_CALL.get(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.incoming = incoming;
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0.20F;
        }
        @Override public void tick() { }
        private void finish() { stop(); }
    }

    public static void close() {
        if (isActive()) {
            active.stopRingtone();
            active.request(NpcCallRequestPayload.END);
            active.closingAt = System.nanoTime();
            active.closing = new UiOpeningAnimation(active.closingAt, 1, NeuralLinkStyle.CLOSE_MILLIS);
        }
    }

    public static void reset() {
        if (active != null) {
            active.stopRingtone();
            active.fade.close();
            active = null;
        }
    }
}
