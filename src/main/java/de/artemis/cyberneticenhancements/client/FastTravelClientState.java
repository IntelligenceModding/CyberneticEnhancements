package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.screen.FastTravelScreen;
import de.artemis.cyberneticenhancements.common.fasttravel.FastTravel;
import de.artemis.cyberneticenhancements.common.network.FastTravelOpenPayload;
import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/** The fast travel terminals this player has found, and the screen flash that covers a trip. */
public final class FastTravelClientState {
    private static final int FLASH_RGB = 0x5DDEFF, ARRIVAL_TICKS = 24;
    private static List<FastTravelPointsPayload.Point> points = List.of();
    private static long departed = -1, arrived = -1;

    private FastTravelClientState() {}

    public static void update(FastTravelPointsPayload payload) { points = payload.points(); }

    public static List<FastTravelPointsPayload.Point> points() { return points; }

    public static void open(FastTravelOpenPayload payload) {
        var minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FAST_TRAVEL_OPEN.get(), 1F));
        minecraft.setScreen(new FastTravelScreen(payload.origin(), payload.cost()));
    }

    /** The player's own trip began or ended, at the given game time. */
    public static void trip(boolean departing, long gameTime) {
        if (departing) { departed = gameTime; arrived = -1; }
        else { arrived = gameTime; departed = -1; }
    }

    public static void clear() {
        points = List.of();
        departed = arrived = -1;
    }

    /** A cyan wash with scanlines that builds while the player dissolves, and clears when they arrive. */
    public static void overlay(RenderGuiEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || departed < 0 && arrived < 0) return;
        float motion = minecraft.options.screenEffectScale().get().floatValue();
        float now = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float strength;
        if (departed >= 0) strength = Math.min(1, (now - departed) / FastTravel.DELAY);
        else {
            strength = 1 - (now - arrived) / ARRIVAL_TICKS;
            if (strength <= 0) { arrived = -1; return; }
        }
        strength = strength * strength * motion;
        if (strength <= 0) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, (int) (strength * 150) << 24 | FLASH_RGB);
        int scan = (int) (now * 3) % 6;
        for (int y = -scan; y < height; y += 6) graphics.fill(0, y, width, y + 2, (int) (strength * 90) << 24 | 0x0A1016);
        // A bright line sweeps down the screen.
        int sweep = (int) ((now * 14) % (height + 40)) - 20;
        graphics.fill(0, sweep, width, sweep + 3, (int) (strength * 200) << 24 | 0xCFF6FF);
    }
}
