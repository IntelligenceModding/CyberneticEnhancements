package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

public final class QuestProgressOverlay {
    private static QuestProgressPayload snapshot = new QuestProgressPayload(List.of());
    private static final QuestHudPresentation PRESENTATION = new QuestHudPresentation();
    private static final int DETAIL_LINES = 2;
    private static boolean pageDown;
    private static final boolean[] callDown = new boolean[QuestHudPresentation.PAGE_SIZE];
    private static List<QuestProgressPayload.Objective> visibleObjectives = List.of();
    private QuestProgressOverlay() {}

    private static boolean isHidden(String questId) {
        return CompassPreferences.entry(questId).hidden();
    }

    private static int objectiveCount() {
        return (int) snapshot.objectives().stream().filter(objective -> !isHidden(objective.id())).count();
    }

    public static void receive(QuestProgressPayload payload) { snapshot = payload; }
    public static void clear() {
        snapshot = new QuestProgressPayload(List.of()); PRESENTATION.clear(); pageDown = false;
        visibleObjectives = List.of(); java.util.Arrays.fill(callDown, false);
    }

    private static QuestProgressPayload.Objective callable(int index) {
        if (index < 0 || index >= visibleObjectives.size()) return null;
        String id = visibleObjectives.get(index).id();
        if (isHidden(id)) return null;
        return snapshot.objectives().stream().filter(objective -> objective.id().equals(id) && objective.callTarget() != null).findFirst().orElse(null);
    }

    public static boolean canCall(int index) {
        return controlsVisible() && canShow() && !NpcCallOverlay.hasLiveCall() && callable(index) != null;
    }

    private static boolean canShow() {
        var minecraft = Minecraft.getInstance();
        return GameplayHudVisibility.shows(GameplayHudVisibility.Element.QUESTS, true) && !minecraft.player.isSpectator()
                && !(GameplayHudVisibility.shows(GameplayHudVisibility.Element.CALLS, true) && NpcCallOverlay.isVisible())
                && !(GameplayHudVisibility.shows(GameplayHudVisibility.Element.CONTACTS, true) && NpcContactsOverlay.isVisible());
    }

    private static boolean controlsVisible() {
        return GameplayHudVisibility.gameplay()
                && GameplayHudVisibility.shows(GameplayHudVisibility.Element.QUESTS, HudVisibilityController.isHudEnabled());
    }

    public static boolean canPage() { return controlsVisible() && canShow() && objectiveCount() > QuestHudPresentation.PAGE_SIZE; }

    public static void tick() {
        for (int index = 0; index < callDown.length; index++) {
            var call = ModKeyMappings.CALL_QUEST_ISSUERS.get(index);
            boolean clicked = call.consumeClick();
            while (call.consumeClick()) {}
            if (clicked && !callDown[index] && canCall(index)) {
                var objective = callable(index);
                NpcCallOverlay.startTurnIn(objective.id(), objective.callTarget());
            }
            callDown[index] = call.isDown();
        }
        var key = ModKeyMappings.NEXT_QUEST_PAGE;
        boolean clicked = key.consumeClick();
        while (key.consumeClick()) {}
        if (clicked && !pageDown && canPage()) {
            PRESENTATION.nextPage(objectiveCount());
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.1F, .12F));
        }
        pageDown = key.isDown();
    }

    static void render(GuiGraphics original) {
        var minecraft = Minecraft.getInstance();
        if (!canShow()) return;
        var frame = PRESENTATION.sample(snapshot.objectives(), System.nanoTime() / 1_000_000_000D, QuestProgressOverlay::isHidden);
        visibleObjectives = frame.objectives();
        if (!frame.visible()) return;
        var viewport = NeuralLinkStyle.viewport();
        var font = minecraft.font;
        int width = NeuralLinkStyle.WIDTH;
        boolean left = ClientConfig.CALL_WINDOW_ON_LEFT.get();
        int x = NeuralLinkStyle.x(viewport, left);
        int y = NeuralLinkStyle.MARGIN;
        int inset = NeuralLinkStyle.INSET, innerWidth = width - inset * 2;
        // Every quest gets one line for its name and a fixed two-line window for its text; longer text scrolls rather than growing the panel.
        var details = frame.objectives().stream().map(objective -> HudText.wrap(font, objective.detail(), innerWidth, Integer.MAX_VALUE)).toList();
        int[] rowHeights = java.util.stream.IntStream.range(0, details.size())
                .map(index -> 25 + DETAIL_LINES * 11 + (frame.objectives().get(index).counter() ? 6 : 0)
                        + (frame.objectives().get(index).callTarget() == null ? 0 : 26)).toArray();
        int height = 26 + java.util.Arrays.stream(rowHeights).sum() + (frame.pages() > 1 ? 32 : 7);
        float scale = Math.min(1F, (viewport.height() - 2F * y) / height);
        original.flush();
        GuiGraphics graphics = new UiGraphics(original, viewport);
        graphics.pose().pushPose();
        try {
            int originX = left ? x : x + width, originY = y;
            graphics.pose().translate(originX, originY, 0);
            graphics.pose().scale(scale, scale, 1);
            x = left ? 0 : -width;
            y = 0;
            GameplayHudStyle.panel(graphics, x, y, width, height, UiRenderer.ACCENT, frame.opacity());
            graphics.drawString(font, net.minecraft.network.chat.Component.translatable("hud.cyberneticenhancements.quest_progress.title"),
                    x + inset, 8, color(UiRenderer.ACCENT, frame.opacity()), false);
            graphics.fill(x + inset, 24, x + width - inset, 25, color(UiRenderer.FIELD_EDGE, .65F * frame.opacity()));
            y = 26;
            for (int index = 0; index < frame.objectives().size(); index++) {
                var objective = frame.objectives().get(index);
                int rowHeight = rowHeights[index];
                graphics.drawString(font, HudText.trim(font, objective.title(), innerWidth), x + inset, y + 6,
                        color(UiRenderer.TEXT_SECONDARY, frame.opacity()), false);
                var lines = details.get(index);
                boolean scrolls = lines.size() > DETAIL_LINES;
                // Kept fractional and applied through the pose, so the text glides instead of jumping a whole pixel at a time.
                float scroll = scrolls ? (float) HudText.scrollOffset((lines.size() - DETAIL_LINES) * 11, System.nanoTime() / 1_000_000_000D) : 0;
                // A scissor ignores the pose, so the window is worked out in screen space: the panel is translated and scaled.
                // Only the top and bottom matter; the text is already wrapped to fit, so the sides are left wide open.
                if (scrolls) graphics.enableScissor((int) Math.floor(originX + (x - 40) * scale), (int) Math.floor(originY + (y + 19) * scale),
                        (int) Math.ceil(originX + (x + width + 40) * scale), (int) Math.ceil(originY + (y + 20 + DETAIL_LINES * 11 - 1) * scale));
                graphics.pose().pushPose();
                graphics.pose().translate(0, -scroll, 0);
                for (int line = 0; line < lines.size(); line++)
                    graphics.drawString(font, lines.get(line), x + inset, y + 20 + line * 11, color(UiRenderer.TEXT, frame.opacity()), false);
                graphics.pose().popPose();
                if (scrolls) graphics.disableScissor();
                if (objective.counter()) {
                    int fill = Math.round(innerWidth * objective.current() / (float) objective.required());
                    graphics.fill(x + inset, y + rowHeight - 7, x + width - inset, y + rowHeight - 6, color(UiRenderer.FIELD_EDGE, .65F * frame.opacity()));
                    graphics.fill(x + inset, y + rowHeight - 7, x + inset + fill, y + rowHeight - 6, color(UiRenderer.ACCENT, .65F * frame.opacity()));
                }
                if (objective.callTarget() != null) GameplayHudStyle.shortcut(graphics, font, x + inset, y + rowHeight - 26, innerWidth,
                        ModKeyMappings.CALL_QUEST_ISSUERS.get(index).getTranslatedKeyMessage(), net.minecraft.network.chat.Component.translatable(
                                "hud.cyberneticenhancements.quest_progress.call_issuer"), net.minecraft.network.chat.Component.empty(), true, frame.opacity());
                y += rowHeight;
                if (index + 1 < frame.objectives().size() || frame.pages() > 1)
                    graphics.fill(x + inset, y, x + width - inset, y + 1, color(UiRenderer.FIELD_EDGE, .4F * frame.opacity()));
            }
            if (frame.pages() > 1) {
                String pages = (frame.page() + 1) + "/" + frame.pages();
                GameplayHudStyle.shortcut(graphics, font, x + inset, y + 4, innerWidth,
                        ModKeyMappings.NEXT_QUEST_PAGE.getTranslatedKeyMessage(), net.minecraft.network.chat.Component.translatable(
                                "hud.cyberneticenhancements.quest_progress.next"), net.minecraft.network.chat.Component.literal(pages), true, frame.opacity());
            }
        } finally { graphics.pose().popPose(); graphics.flush(); }
    }

    private static int color(int rgb, float alpha) { return GameplayHudStyle.color(rgb, alpha); }
}
