package de.artemis.cyberneticenhancements.common.tutorial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Runs the tutorial script for players in the tutorial world only; every other world is untouched. */
public final class TutorialDirector {
    /** The reserved save folder; the client extracts or creates the tutorial world under this name. */
    public static final String WORLD_FOLDER = "cyberneticenhancements-tutorial";
    private record Progress(TutorialScript script, int step, long started, TutorialAccess access) {}
    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    private TutorialDirector() {}

    public static boolean isTutorialWorld(MinecraftServer server) {
        if (server.isDedicatedServer()) return false;
        var folder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
        return folder != null && WORLD_FOLDER.equals(folder.toString());
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isTutorialWorld(player.server)) start(player, TutorialScript.current());
    }

    public static void start(ServerPlayer player, TutorialScript script) {
        if (script.steps().isEmpty()) return;
        PROGRESS.put(player.getUUID(), new Progress(script, 0, player.level().getGameTime(), script.accessAt(0)));
        sync(player);
    }

    /** Lets the player use these from now on; tokens are interface elements or {@code key:<mapping>}. */
    public static void reveal(ServerPlayer player, String... tokens) { change(player, access -> access.reveal(tokens(tokens))); }

    /** Takes these away again, including any temporary allowance. */
    public static void hide(ServerPlayer player, String... tokens) { change(player, access -> access.hide(tokens(tokens))); }

    /** Lets the player use these until the current step ends. */
    public static void allow(ServerPlayer player, String... tokens) { change(player, access -> access.allow(tokens(tokens))); }

    public static boolean allows(ServerPlayer player, String token) {
        var progress = PROGRESS.get(player.getUUID());
        return progress != null && progress.access.allows(token);
    }

    private static java.util.List<String> tokens(String[] tokens) {
        for (String token : tokens) if (!TutorialAccess.valid(token)) throw new IllegalArgumentException("Unknown interface element or key: " + token);
        return java.util.List.of(tokens);
    }

    private static void change(ServerPlayer player, java.util.function.Consumer<TutorialAccess> change) {
        var progress = PROGRESS.get(player.getUUID());
        if (progress == null) return;
        change.accept(progress.access);
        sync(player);
    }

    public static boolean active(ServerPlayer player) { return PROGRESS.containsKey(player.getUUID()); }

    public static int step(ServerPlayer player) {
        var progress = PROGRESS.get(player.getUUID());
        return progress == null ? -1 : progress.step;
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var progress = PROGRESS.get(player.getUUID());
        if (progress == null) return;
        var trigger = progress.script.steps().get(progress.step).trigger();
        boolean complete = switch (trigger.type()) {
            case TICKS -> player.level().getGameTime() - progress.started >= trigger.ticks();
            case AREA -> player.position().distanceToSqr(target(player, trigger).getCenter()) <= trigger.radius() * trigger.radius();
            case SIGNAL -> false;
        };
        if (complete) advance(player);
    }

    public static void signal(ServerPlayer player, String signal) {
        var progress = PROGRESS.get(player.getUUID());
        if (progress == null) return;
        var trigger = progress.script.steps().get(progress.step).trigger();
        if (trigger.type() == TutorialScript.TriggerType.SIGNAL && trigger.signal().equals(signal)) advance(player);
    }

    private static void advance(ServerPlayer player) {
        var progress = PROGRESS.get(player.getUUID());
        int next = progress.step + 1;
        if (next >= progress.script.steps().size()) {
            PROGRESS.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, new TutorialStatePayload("", "", java.util.List.of(), next, next, Optional.empty(),
                    java.util.List.copyOf(TutorialScript.ELEMENTS), true));
            return;
        }
        PROGRESS.put(player.getUUID(), new Progress(progress.script, next, player.level().getGameTime(),
                progress.access.enter(progress.script.steps().get(next))));
        sync(player);
    }

    private static BlockPos target(ServerPlayer player, TutorialScript.Trigger trigger) {
        return trigger.relative() ? player.serverLevel().getSharedSpawnPos().offset(trigger.pos()) : trigger.pos();
    }

    private static void sync(ServerPlayer player) {
        var progress = PROGRESS.get(player.getUUID());
        var step = progress.script.steps().get(progress.step);
        var target = step.trigger().type() == TutorialScript.TriggerType.AREA ? Optional.of(target(player, step.trigger())) : Optional.<BlockPos>empty();
        PacketDistributor.sendToPlayer(player, new TutorialStatePayload(step.chapter(), step.objective(), step.args(),
                progress.step, progress.script.steps().size(), target, progress.access.snapshot(), false));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { PROGRESS.remove(event.getEntity().getUUID()); }

    public static void onStop(ServerStoppedEvent event) { PROGRESS.clear(); }
}
