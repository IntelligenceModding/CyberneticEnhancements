package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.HudText;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.network.FaceDialogueChoosePayload;
import de.artemis.cyberneticenhancements.common.network.FaceDialogueStatePayload;
import de.artemis.cyberneticenhancements.common.network.FaceDialogueWindowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/**
 * Talking without a screen. What they say sits low in the centre like a subtitle, and the answers hang beside the crosshair,
 * so you can keep moving and looking around while you decide. Answers you cannot give yet stay in the list, greyed out, with
 * the reason underneath.
 */
public final class FaceDialogueOverlay {
    private static final int LINE_WIDTH = 260, OPTION_WIDTH = LINE_WIDTH, PAD = 5, GAP = 2, LOOK_RANGE = 24, BAR = 4, LOCKED_TEXT = 0xFF6D7884, REASON = 0xFFD98A7A;
    private static FaceDialogueStatePayload state;
    private static int selected;
    private static float visibility;
    private static long lastFrame;

    private FaceDialogueOverlay() {}

    public static boolean active() { return state != null && state.open(); }

    /** Whether there is something to pick between, which is when the mouse wheel is better spent on the list than on the hotbar. */
    public static boolean choosing() {
        var minecraft = Minecraft.getInstance();
        return active() && !state.options().isEmpty() && minecraft.screen == null && GameplayHudVisibility.gameplay() && looking();
    }

    /** Checked against the camera every time it is asked, so the panel and the clicks follow the crosshair without waiting for the server. */
    private static boolean looking() {
        var minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null || minecraft.player == null) return false;
        var npc = minecraft.level.getEntity(state.npcId());
        if (npc == null) return false;
        var eye = minecraft.player.getEyePosition();
        return npc.getBoundingBox().inflate(0.2).clip(eye, eye.add(minecraft.player.getViewVector(1F).scale(LOOK_RANGE))).isPresent();
    }

    public static void receive(FaceDialogueStatePayload payload) {
        if (!payload.open()) { clear(); return; }
        boolean same = state != null && state.npcId() == payload.npcId() && state.line().equals(payload.line());
        state = payload;
        if (!same || selected >= entries()) selected = firstAvailable();
        if (!same) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.3F));
    }

    public static void clear() {
        state = null;
        selected = 0;
        drainKeys();
    }

    private static int firstAvailable() {
        for (int index = 0; index < state.options().size(); index++) if (state.options().get(index).available()) return index;
        return 0;
    }

    private static void drainKeys() {
        while (ModKeyMappings.DIALOGUE_UP.consumeClick()) {}
        while (ModKeyMappings.DIALOGUE_DOWN.consumeClick()) {}
        while (ModKeyMappings.DIALOGUE_SELECT.consumeClick()) {}
    }

    private static void move(int step) {
        int count = entries();
        if (count < 2) return;
        selected = Math.floorMod(selected + step, count);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9F, 0.2F));
    }

    /** Whether their windows can be opened by key: only with them in view, since the keys then mean something. */
    private static boolean windowsAvailable() {
        var minecraft = Minecraft.getInstance();
        return active() && state.header() != null && state.header().windows() != 0 && minecraft.screen == null && GameplayHudVisibility.gameplay() && looking();
    }

    /** The windows they have, as the indexes of the keys that open them; they are shown as answers of their own below the others. */
    private static List<Integer> windowEntries() {
        var list = new ArrayList<Integer>();
        var header = state.header();
        if (header == null || state.options().isEmpty()) return list;
        for (int index = 0; index < WINDOW_NAMES.length; index++) if ((header.windows() & (1 << index)) != 0) list.add(index);
        return list;
    }

    /** Answers from them, then their windows. */
    private static int entries() { return state.options().size() + windowEntries().size(); }

    private static void openWindow(int index) {
        var sounds = Minecraft.getInstance().getSoundManager();
        if ((state.header().locked() & (1 << index)) != 0) {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.5F));
            return;
        }
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.5F));
        PacketDistributor.sendToServer(new FaceDialogueWindowPayload(index + 1));
    }

    private static void openWindows() {
        var header = state.header();
        for (int index = 0; index < ModKeyMappings.DIALOGUE_WINDOWS.size(); index++) {
            int bit = 1 << index;
            while (ModKeyMappings.DIALOGUE_WINDOWS.get(index).consumeClick()) {
                if ((header.windows() & bit) == 0) continue;
                openWindow(index);
                return;
            }
        }
    }

    public static void tick() {
        if (windowsAvailable()) openWindows();
        else for (var key : ModKeyMappings.DIALOGUE_WINDOWS) while (key.consumeClick()) {}
        if (!choosing()) { drainKeys(); return; }
        while (ModKeyMappings.DIALOGUE_UP.consumeClick()) move(-1);
        while (ModKeyMappings.DIALOGUE_DOWN.consumeClick()) move(1);
        while (ModKeyMappings.DIALOGUE_SELECT.consumeClick()) give();
    }

    private static void give() {
        if (selected >= state.options().size()) {
            var windows = windowEntries();
            if (selected - state.options().size() < windows.size()) openWindow(windows.get(selected - state.options().size()));
            return;
        }
        var option = state.options().get(selected);
        var sounds = Minecraft.getInstance().getSoundManager();
        if (!option.available()) {
            // Still sent: the server refuses a locked answer, but a contract item in hand is handed over all the same.
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.5F));
            PacketDistributor.sendToServer(new FaceDialogueChoosePayload(selected));
            return;
        }
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.5F));
        PacketDistributor.sendToServer(new FaceDialogueChoosePayload(selected));
    }

    /** Either click gives the answer that is picked (or hands over a contract item, which the server decides) and sneaking with it steps back; while they are talking to you, left click never hits them. */
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        if (event.isAttack() && active() && minecraft.screen == null && GameplayHudVisibility.gameplay() && looking()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!choosing()) return;
        } else if (!choosing() || !event.isUseItem()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!Minecraft.getInstance().player.isShiftKeyDown()) give();
        else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.9F, 0.4F));
            PacketDistributor.sendToServer(new FaceDialogueChoosePayload(de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.BACK));
        }
    }

    /** The mouse wheel moves through the answers while a conversation is open, in place of the hotbar. */
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!choosing() || entries() < 2 || event.getScrollDeltaY() == 0) return;
        move(event.getScrollDeltaY() > 0 ? -1 : 1);
        event.setCanceled(true);
    }

    public static void render(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        float step = lastFrame == 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        boolean show = active() && minecraft.screen == null && !minecraft.options.hideGui && GameplayHudVisibility.gameplay() && looking();
        visibility = Math.clamp(visibility + (show ? step / 0.1F : -step / 0.08F), 0F, 1F); // Quick in and quicker out.
        if (!active() || visibility <= 0 || minecraft.screen != null) return;
        var font = minecraft.font;
        float fade = visibility;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        // One column in the centre, above the hotbar: what they say, and the answers directly below it.
        int optionsHeight = rows().isEmpty() ? 0 : optionsHeight(font) - GAP + 4; // The last row has no gap after it.
        int lineHeight = lineHeight(font);
        // The lowest panel ends just above the status bars (the armour row starts 49 up), leaving room for its shadow and a small gap.
        int top = height - BOTTOM_CLEARANCE - optionsHeight - lineHeight;
        renderLine(graphics, font, width, top, lineHeight, fade);
        if (optionsHeight > 0) renderOptions(graphics, font, width, top + lineHeight + 4, fade);
    }

    /** Distance from the bottom of the screen to the bottom of the dialogue: 49 to the armour row, the panel shadow, and a gap of two. */
    private static final int BOTTOM_CLEARANCE = 49 + UiRenderer.SHADOW_OFFSET_Y + 2;

    /** How many lines of what they say fit; more than that scrolls. */
    private static final int MAX_LINES = 5;

    private static final String[] WINDOW_NAMES = {"shop", "services", "contracts", "customize"};

    /** One line of the list: an answer from them, or a window of theirs, which also shows the key that opens it. */
    private record Row(Component label, @org.jetbrains.annotations.Nullable Component locked, @org.jetbrains.annotations.Nullable Component key) {
        boolean available() { return locked == null; }
    }

    private static List<Row> rows() {
        var rows = new ArrayList<Row>();
        for (var option : state.options()) rows.add(new Row(option.label(), option.locked(), null));
        for (int index : windowEntries()) {
            boolean locked = (state.header().locked() & (1 << index)) != 0;
            rows.add(new Row(Component.translatable("hud.cyberneticenhancements.dialogue.window." + WINDOW_NAMES[index]),
                    locked ? Component.translatable("screen.cyberneticenhancements.npc.identity.locked") : null,
                    Component.literal("[").append(ModKeyMappings.DIALOGUE_WINDOWS.get(index).getTranslatedKeyMessage()).append("]")));
        }
        return rows;
    }

    private static int lineHeight(Font font) {
        return PAD * 2 + 12 + Math.min(MAX_LINES, font.split(state.line(), LINE_WIDTH - PAD * 2).size()) * 10 + (state.header() == null ? 0 : BAR + 3);
    }

    private static int optionsHeight(Font font) {
        int textWidth = OPTION_WIDTH - PAD * 2 - 12, total = 0;
        for (var row : rows()) {
            int lines = font.split(row.label(), textWidth).size();
            if (!row.available()) lines += font.split(row.locked(), textWidth).size();
            total += PAD * 2 - 2 + lines * 10 + GAP;
        }
        return total;
    }

    private static int faded(int color, float fade) {
        int alpha = Math.round((color >>> 24) * fade);
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /** What they are saying: the speaker's name above it, as a subtitle above the hotbar. */
    private static void renderLine(GuiGraphics graphics, Font font, int width, int y, int boxHeight, float fade) {
        List<FormattedCharSequence> lines = font.split(state.line(), LINE_WIDTH - PAD * 2);
        int x = (width - LINE_WIDTH) / 2;
        UiRenderer.panel(graphics, x, y, LINE_WIDTH, boxHeight, faded(UiRenderer.PANEL, fade), faded(UiRenderer.PANEL_EDGE_ACTIVE, fade));
        graphics.drawString(font, state.speaker().copy().withStyle(style -> style.withBold(true)), x + PAD, y + PAD, faded(UiRenderer.ACCENT, fade), false);
        int textY = y + PAD + 12;
        // Longer than the window: it stays that tall and the text scrolls like in the quest HUD, instead of the panel growing.
        boolean scrolls = lines.size() > MAX_LINES;
        float scroll = scrolls ? (float) HudText.scrollOffset((lines.size() - MAX_LINES) * 10, System.nanoTime() / 1_000_000_000D) : 0;
        if (scrolls) graphics.enableScissor(x, textY - 1, x + LINE_WIDTH, textY + MAX_LINES * 10 - 1);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scroll, 0);
        for (var line : lines) {
            graphics.drawString(font, line, x + PAD, textY, faded(UiRenderer.TEXT, fade), false);
            textY += 10;
        }
        graphics.pose().popPose();
        if (scrolls) graphics.disableScissor();
        var header = state.header();
        if (header == null) return;
        // Top right: what they are and how far they trust you; along the bottom: that trust as a bar.
        var tag = Component.empty().append(header.type()).append(Component.literal("  ").append(header.trustLabel()).withStyle(net.minecraft.ChatFormatting.GRAY));
        graphics.drawString(font, tag, x + LINE_WIDTH - PAD - font.width(tag), y + PAD, faded(UiRenderer.ACCENT, fade), false);
        int barX = x + PAD, barY = y + boxHeight - PAD - BAR + 1, barWidth = LINE_WIDTH - PAD * 2;
        graphics.fill(barX, barY, barX + barWidth, barY + BAR, faded(UiRenderer.ROW_DISABLED, fade));
        graphics.fill(barX, barY + BAR - 1, barX + barWidth, barY + BAR, faded(UiRenderer.PANEL_EDGE, fade));
        graphics.fill(barX, barY, barX + Math.round(barWidth * header.trustPercent() / 100F), barY + BAR - 1, faded(UiRenderer.ACCENT, fade));
    }

    private static void renderOptions(GuiGraphics graphics, Font font, int width, int top, float fade) {
        var rows = rows();
        int textWidth = OPTION_WIDTH - PAD * 2 - 12;
        int[] heights = new int[rows.size()];
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            int lines = font.split(row.label(), textWidth).size();
            if (!row.available()) lines += font.split(row.locked(), textWidth).size();
            heights[index] = PAD * 2 - 2 + lines * 10;
        }
        int x = (width - OPTION_WIDTH) / 2, y = top;
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            boolean chosen = index == selected, available = row.available();
            UiRenderer.panel(graphics, x, y, OPTION_WIDTH, heights[index], faded(chosen ? UiRenderer.ROW_SELECTED : available ? UiRenderer.ROW : UiRenderer.ROW_DISABLED, fade),
                    faded(chosen ? UiRenderer.SELECTED_EDGE : UiRenderer.PANEL_EDGE, fade));
            if (chosen) graphics.drawString(font, ">", x + 3, y + PAD - 1, faded(available ? UiRenderer.ACCENT : LOCKED_TEXT, fade), false);
            int textY = y + PAD - 1;
            for (var line : font.split(row.label(), textWidth)) {
                graphics.drawString(font, line, x + PAD + 9, textY, faded(available ? UiRenderer.TEXT : LOCKED_TEXT, fade), false);
                textY += 10;
            }
            if (!available) for (var line : font.split(row.locked(), textWidth)) {
                graphics.drawString(font, line, x + PAD + 9, textY, faded(REASON, fade), false);
                textY += 10;
            }
            if (row.key() != null) {
                int keyWidth = font.width(row.key()) + 6, keyX = x + OPTION_WIDTH - PAD - keyWidth, keyY = y + PAD - 3;
                UiRenderer.panel(graphics, keyX, keyY, keyWidth, 12, faded(UiRenderer.ROW_DISABLED, fade), faded(UiRenderer.PANEL_EDGE_ACTIVE, fade));
                graphics.drawString(font, row.key(), keyX + 3, keyY + 2, faded(available ? UiRenderer.ACCENT : LOCKED_TEXT, fade), false);
            }
            y += heights[index] + GAP;
        }
    }
}
