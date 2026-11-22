package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.screen.OriginScreen;
import de.artemis.cyberneticenhancements.common.origin.Origin;
import de.artemis.cyberneticenhancements.common.origin.OriginStatePayload;
import de.artemis.cyberneticenhancements.common.origin.Origins;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Opens the lifepath choice once the world is showing, for a player who has none. */
public final class OriginClient {
    private static boolean pending;
    /** Set while the pause menu is open over the choice, so the choice comes back when it closes. */
    private static boolean resume;
    private static int calm;
    /** Ticks the HUD is still kept away after the choice is made; negative while there is no choice yet. */
    private static int hold;
    private static final int HUD_DELAY = 30;

    private OriginClient() {}

    public static void receive(OriginStatePayload payload) {
        Origin origin = Origin.find(payload.id());
        Origins.receive(origin);
        pending = origin == null;
        calm = 0;
        // The HUD stays away until a life origin is picked, and for a moment after.
        if (origin == null) hold = -1;
        else if (hold < 0) hold = HUD_DELAY;
    }

    /** Whether the HUD is kept away: while there is no life origin yet, and for a second or two once one is picked. */
    public static boolean hidesHud() {
        if (TutorialClient.inTutorialWorld()) return false;
        // With no choice yet it is only held while the prompt is coming or on screen, so it can never stay away for good.
        if (hold < 0) return pending || resume || Minecraft.getInstance().screen instanceof OriginScreen;
        return hold > 0;
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (hold > 0) hold--;
        if (resume) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || hold >= 0) resume = false;
            else if (minecraft.screen == null) { resume = false; minecraft.setScreen(new OriginScreen()); }
        }
        if (!pending) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getOverlay() != null) return;
        if (TutorialClient.inTutorialWorld()) { pending = false; return; }
        // At once, over the loading screen if it is still up: the world is never seen, heard or played before the choice.
        pending = false;
        calm = 0;
        minecraft.setScreen(new OriginScreen());
    }

    /** Whether the world is still to be kept silent: no lifepath yet, so nothing but the screen's own sounds are heard. */
    private static boolean silent() {
        if (TutorialClient.inTutorialWorld()) return false;
        return hold < 0 && (pending || resume || Minecraft.getInstance().screen instanceof OriginScreen);
    }

    public static void onSound(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound != null && sound.getSource() != SoundSource.MASTER && silent()) event.setSound(null);
    }

    /** Esc on the choice: the pause menu opens over it, and the choice returns once that is closed. */
    public static void pause() {
        resume = true;
        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        pending = false;
        resume = false;
        calm = 0;
        hold = 0;
        Origins.clearClient();
        de.artemis.cyberneticenhancements.client.StreetCredHud.clear();
        de.artemis.cyberneticenhancements.common.skills.Skills.clearClient();
    }
}
