package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.screen.ChangelogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opens the release's changelog over the title screen the first time it is seen, and never again for that release. */
public final class ChangelogPopup {
    /** Opened already in this launch; in a development environment this is all that holds it back. */
    private static boolean shownThisLaunch;

    private ChangelogPopup() {}

    /** The title screen, and the changelog that opens over it, which is still the title as far as music and ambience go. */
    public static boolean isTitle(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof TitleScreen || screen instanceof ChangelogScreen;
    }

    /** Whether this release's changelog is still to be shown. The title screen keeps its buttons back until it has been. */
    public static boolean due() {
        if (!ClientConfig.SPEC.isLoaded() || !ClientConfig.SHOW_CHANGELOG_POPUP.get() || !ChangelogScreen.available()) return false;
        // A development environment shows it on every launch, so it can be looked at without clearing the config.
        if (!net.neoforged.fml.loading.FMLLoader.isProduction()) return !shownThisLaunch;
        return !ChangelogScreen.CURRENT.equals(ClientConfig.LAST_SEEN_CHANGELOG.get());
    }

    public static void onTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!ClientConfig.SPEC.isLoaded() || minecraft.level != null || LaunchIntro.isPlaying()
                || minecraft.screen == null || minecraft.screen.getClass() != TitleScreen.class
                || minecraft.getOverlay() != null
                || !due()) return;
        shownThisLaunch = true;
        // Marked as seen as it opens, so a crash or a quick quit cannot bring it back on every launch.
        ClientConfig.LAST_SEEN_CHANGELOG.set(ChangelogScreen.CURRENT);
        ClientConfig.LAST_SEEN_CHANGELOG.save();
        minecraft.setScreen(new ChangelogScreen(minecraft.screen));
    }
}
