package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.client.ui.NeuralLinkStyle;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCred;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.ArrayList;
import java.util.List;

/**
 * A small panel in the call and quest slot, shown whenever Street Cred changes. It ignores the HUD toggles: a change to
 * your name is always worth seeing. It waits its turn while a call or the contact list holds the slot.
 */
public final class StreetCredHud {
    private static final double SHOW_SECONDS = 4.5, LEVEL_UP_SECONDS = 7, FADE_SECONDS = .25;
    private static final int HEIGHT = 94;
    private static boolean known;
    private static boolean pending, levelUp, rankUp;
    private static final List<Cue> CUES = new ArrayList<>();

    /** A sound that plays a moment after the panel opens, so a level-up is a short phrase and not one note. */
    private record Cue(double at, SoundEvent sound, float pitch, float volume) {}
    private static int delta, shownFrom;
    private static double shownAt = Double.NaN;

    private StreetCredHud() {}

    private static double now() { return System.nanoTime() / 1_000_000_000D; }

    /** The first value after joining is not a change; later ones are. */
    public static void receive(int points) {
        int before = StreetCred.levelFor(clientPoints());
        int old = clientPoints();
        StreetCred.receive(points);
        if (!known) { known = true; return; }
        if (points == old) return;
        // Changes that arrive while the panel is up add to it rather than starting over, so a burst reads as one total.
        boolean visible = !Double.isNaN(shownAt) && now() - shownAt < duration();
        delta = (visible || pending ? delta : 0) + points - old;
        if (!visible && !pending) shownFrom = before;
        levelUp = (visible || pending ? levelUp : false) || StreetCred.levelFor(points) > shownFrom;
        pending = true;
        shownAt = Double.NaN;
    }

    private static int clientPoints() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : StreetCred.points(player);
    }

    private static double duration() { return levelUp ? LEVEL_UP_SECONDS : SHOW_SECONDS; }

    public static boolean visible() {
        return !Double.isNaN(shownAt) && now() - shownAt < duration() + FADE_SECONDS;
    }

    public static void clear() {
        known = false; pending = false; levelUp = false; rankUp = false; delta = 0; shownAt = Double.NaN; CUES.clear();
        StreetCred.clearClient();
        StreetCredBoardClient.clear();
    }

    private static void play(SoundEvent sound, float pitch, float volume, double delay) {
        CUES.add(new Cue(now() + delay, sound, pitch, volume));
    }

    /** A plain gain is a soft tick, a level a rising pair, and a new rank a fuller phrase of its own. The top rank gets the grandest one. */
    private static void cue(int level) {
        CUES.clear();
        if (!levelUp) { play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, .35F, 0); return; }
        if (!rankUp) {
            play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, .55F, 0);
            play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5F, .55F, .14);
            play(SoundEvents.BEACON_POWER_SELECT, 1.4F, .25F, .14);
            return;
        }
        if (level >= StreetCred.MAX_LEVEL) {
            play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, .8F, 0);
            play(SoundEvents.BEACON_ACTIVATE, 1.2F, .6F, .1);
            play(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0F, .6F, .6);
            play(SoundEvents.PLAYER_LEVELUP, 1.0F, .6F, .9);
            return;
        }
        play(SoundEvents.PLAYER_LEVELUP, .85F, .7F, 0);
        play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, .5F, .18);
        play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.25F, .5F, .32);
        play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5F, .5F, .46);
        play(SoundEvents.BEACON_ACTIVATE, 1.3F, .35F, .18);
    }

    private static boolean slotBusy() {
        return NpcCallOverlay.isVisible() || NpcContactsOverlay.isVisible();
    }

    public static void render(GuiGraphics original) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || (!pending && Double.isNaN(shownAt))) return;
        if (pending) {
            if (slotBusy()) return;
            pending = false;
            shownAt = now();
            int level = StreetCred.levelFor(StreetCred.points(minecraft.player));
            rankUp = levelUp && level / 10 != shownFrom / 10;
            cue(level);
        }
        for (var it = CUES.iterator(); it.hasNext(); ) {
            var cue = it.next();
            if (now() < cue.at) continue;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(cue.sound, cue.pitch, cue.volume));
            it.remove();
        }
        double age = now() - shownAt, total = duration();
        if (age >= total + FADE_SECONDS) { shownAt = Double.NaN; delta = 0; levelUp = false; rankUp = false; return; }
        float opacity = (float) Math.clamp(Math.min(age / FADE_SECONDS, (total + FADE_SECONDS - age) / FADE_SECONDS), 0, 1);

        var viewport = NeuralLinkStyle.viewport();
        var font = minecraft.font;
        int width = NeuralLinkStyle.WIDTH, inset = NeuralLinkStyle.INSET, innerWidth = width - inset * 2;
        int x = NeuralLinkStyle.x(viewport, ClientConfig.CALL_WINDOW_ON_LEFT.get()), y = NeuralLinkStyle.MARGIN;
        float scale = Math.min(1F, (viewport.height() - 2F * y) / HEIGHT);
        int points = StreetCred.points(minecraft.player), level = StreetCred.levelFor(points);
        boolean top = level >= StreetCred.MAX_LEVEL;
        float ratio = top ? 1 : (points - StreetCred.threshold(level)) / (float) StreetCred.cost(level);
        boolean left = ClientConfig.CALL_WINDOW_ON_LEFT.get();

        original.flush();
        GuiGraphics graphics = new UiGraphics(original, viewport);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left ? x : x + width, y, 0);
            graphics.pose().scale(scale, scale, 1);
            x = left ? 0 : -width;
            y = 0;
            GameplayHudStyle.panel(graphics, x, y, width, HEIGHT, UiRenderer.ACCENT, opacity);
            // A level-up lights the panel up and lets it fade; a new rank does so for longer and brighter.
            double flash = rankUp ? 1.4 : levelUp ? .7 : 0;
            if (age < flash) graphics.fill(x, y, x + width, y + HEIGHT, GameplayHudStyle.color(UiRenderer.ACCENT, (float) ((rankUp ? .4 : .25) * (1 - age / flash)) * opacity));
            graphics.drawString(font, Component.translatable("hud.cyberneticenhancements.street_cred.title"), x + inset, y + 6,
                    GameplayHudStyle.color(UiRenderer.ACCENT, .8F * opacity), false);
            Component levelText = Component.translatable("hud.cyberneticenhancements.street_cred.level", level);
            graphics.drawString(font, levelText, x + width - inset - font.width(levelText), y + 20,
                    GameplayHudStyle.color(UiRenderer.TEXT_SECONDARY, opacity), false);
            graphics.drawString(font, StreetCred.rank(level), x + inset, y + 20, GameplayHudStyle.color(UiRenderer.TEXT, opacity), false);
            graphics.fill(x + inset, y + NeuralLinkStyle.HEADER_HEIGHT, x + width - inset, y + NeuralLinkStyle.HEADER_HEIGHT + 1,
                    GameplayHudStyle.color(UiRenderer.FIELD_EDGE, .65F * opacity));

            int body = y + NeuralLinkStyle.CONTENT_TOP;
            Component change = levelUp ? Component.translatable("hud.cyberneticenhancements.street_cred.level_up", level)
                    : Component.translatable("hud.cyberneticenhancements.street_cred.gain", delta > 0 ? "+" + delta : String.valueOf(delta));
            graphics.drawString(font, change, x + inset, body + 4, GameplayHudStyle.color(levelUp ? UiRenderer.ACCENT : UiRenderer.TEXT, opacity), false);
            if (rankUp)
                graphics.drawString(font, Component.translatable("hud.cyberneticenhancements.street_cred.new_rank", StreetCred.rank(level)),
                        x + inset, body + 17, GameplayHudStyle.color(UiRenderer.TEXT_SECONDARY, opacity), false);

            int barY = body + 34;
            graphics.fill(x + inset, barY, x + inset + innerWidth, barY + 4, GameplayHudStyle.color(UiRenderer.FIELD_EDGE, .65F * opacity));
            graphics.fill(x + inset, barY, x + inset + Math.round(innerWidth * Math.clamp(ratio, 0, 1)), barY + 4,
                    GameplayHudStyle.color(UiRenderer.ACCENT, .85F * opacity));
            Component footer = top ? Component.translatable("hud.cyberneticenhancements.street_cred.max")
                    : Component.translatable("hud.cyberneticenhancements.street_cred.next", StreetCred.threshold(level + 1) - points, level + 1);
            graphics.drawString(font, footer, x + inset, barY + 9, GameplayHudStyle.color(UiRenderer.TEXT_SECONDARY, opacity), false);
        } finally { graphics.pose().popPose(); graphics.flush(); }
    }
}
