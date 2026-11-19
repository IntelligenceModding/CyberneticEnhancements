package de.artemis.cyberneticenhancements.common.tutorial;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The tutorial world is always clear, still noon with the sun overhead, and nothing spreads or spawns, whatever the saved
 * world says. Applied before the first player joins, then held for as long as the world runs.
 */
public final class TutorialEnvironment {
    /** Minecraft's noon: the sun stands at its highest point, straight above the player. */
    public static final long NOON = 6000;
    private static final int CLEAR_TICKS = 1_000_000;
    public static final Map<GameRules.Key<GameRules.BooleanValue>, Boolean> RULES = rules();
    private static MinecraftServer checked;
    private static boolean tutorial;

    private TutorialEnvironment() {}

    private static Map<GameRules.Key<GameRules.BooleanValue>, Boolean> rules() {
        var rules = new LinkedHashMap<GameRules.Key<GameRules.BooleanValue>, Boolean>();
        rules.put(GameRules.RULE_DAYLIGHT, false);
        rules.put(GameRules.RULE_WEATHER_CYCLE, false);
        rules.put(GameRules.RULE_DOFIRETICK, false);
        rules.put(GameRules.RULE_DOMOBSPAWNING, false);
        rules.put(GameRules.RULE_DO_PATROL_SPAWNING, false);
        rules.put(GameRules.RULE_DO_TRADER_SPAWNING, false);
        rules.put(GameRules.RULE_DO_WARDEN_SPAWNING, false);
        rules.put(GameRules.RULE_DOINSOMNIA, false);
        rules.put(GameRules.RULE_KEEPINVENTORY, true);
        rules.put(GameRules.RULE_ANNOUNCE_ADVANCEMENTS, false);
        return java.util.Collections.unmodifiableMap(rules);
    }

    public static boolean applies(MinecraftServer server) {
        if (server != checked) {
            checked = server;
            tutorial = TutorialDirector.isTutorialWorld(server);
        }
        return tutorial;
    }

    /** Sets the tutorial's game rules on the given rules; {@code server} may be null while a world is being created. */
    public static void applyRules(GameRules rules, MinecraftServer server) {
        for (var rule : RULES.entrySet()) {
            var value = rules.getRule(rule.getKey());
            if (value.get() != rule.getValue()) value.set(rule.getValue(), server);
        }
    }

    /** Puts the sky back to clear noon; returns whether anything had to change. */
    public static boolean applySky(ServerLevel overworld) {
        boolean timeChanged = overworld.getDayTime() != NOON, changed = timeChanged || overworld.isRaining() || overworld.isThundering();
        if (changed) {
            overworld.setDayTime(NOON);
            overworld.setWeatherParameters(CLEAR_TICKS, 0, false, false);
        }
        // The server only syncs time once a second; do not leave a player looking at the old sky until then.
        if (timeChanged) PacketDistributor.sendToPlayersInDimension(overworld, new ClientboundCustomSetTimePayload(overworld.getGameTime(), NOON,
                overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), overworld.getDayTimeFraction(), overworld.getDayTimePerTick()));
        return changed;
    }

    public static void onStarted(ServerStartedEvent event) {
        var server = event.getServer();
        if (!applies(server)) return;
        // Before any player joins, so their first time packet is already noon.
        applyRules(server.getGameRules(), server);
        applySky(server.overworld());
    }

    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (!applies(server)) return;
        boolean changed = applySky(server.overworld());
        // Commands or other mods could change rules in a world that allows cheats.
        if (changed || server.getTickCount() % 20 == 0) applyRules(server.getGameRules(), server);
    }

    public static void onStop(ServerStoppedEvent event) {
        checked = null;
        tutorial = false;
    }
}
