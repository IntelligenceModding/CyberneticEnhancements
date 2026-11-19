package de.artemis.cyberneticenhancements.common.tutorial;

import com.google.gson.JsonParser;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static de.artemis.cyberneticenhancements.common.dialogue.NpcCallGameTests.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TutorialGameTests {
    private static TutorialScript script() {
        return TutorialScript.parse(JsonParser.parseString("""
                { "chapters": [ { "title": "chapter", "steps": [
                  { "id": "wait", "objective": "wait", "trigger": { "type": "ticks", "ticks": 5 } },
                  { "id": "open", "objective": "open", "args": ["key:key.cyberneticenhancements.open_archive"], "reveal": ["archive"],
                    "trigger": { "type": "signal", "signal": "archive_opened" } },
                  { "id": "walk", "objective": "walk", "trigger": { "type": "area", "x": 0, "y": 0, "z": 0, "relative": true, "radius": 2 } } ] } ] }
                """));
    }

    private static void tick(net.minecraft.server.level.ServerPlayer player) {
        TutorialDirector.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
    }

    @GameTest(template = "sandevistan_test", batch = "tutorial_director", timeoutTicks = 100)
    public static void stepsAdvanceOnlyThroughTheirOwnTriggerAndFinishTheRun(GameTestHelper h) {
        var player = player(h, "tutorial-runner");
        try {
            TutorialDirector.start(player.player(), script());
            var first = drain(player).stream().filter(TutorialStatePayload.class::isInstance).map(TutorialStatePayload.class::cast).findFirst().orElse(null);
            h.assertTrue(first != null && first.step() == 0 && first.total() == 3 && first.objective().equals("wait"), "Start did not sync the first step");
            TutorialDirector.signal(player.player(), "archive_opened");
            h.assertTrue(TutorialDirector.step(player.player()) == 0, "A signal completed a timed step");
            h.runAfterDelay(8, () -> {
                // Mock-connection players are not ticked by the server; tick the director as a real player tick would.
                tick(player.player());
                h.assertTrue(TutorialDirector.step(player.player()) == 1, "The timed step did not advance");
                TutorialDirector.signal(player.player(), "something_else");
                h.assertTrue(TutorialDirector.step(player.player()) == 1, "The wrong signal advanced the step");
                var revealing = drain(player).stream().filter(TutorialStatePayload.class::isInstance).map(TutorialStatePayload.class::cast)
                        .reduce((a, b) -> b).orElse(null);
                h.assertTrue(revealing != null && revealing.revealed().equals(java.util.List.of("archive")), "The archive step did not reveal the archive");
                TutorialDirector.signal(player.player(), "archive_opened");
                h.assertTrue(TutorialDirector.step(player.player()) == 2, "The matching signal did not advance");
                // The area is the world spawn; standing there completes the last step on the next tick.
                var spawn = player.player().serverLevel().getSharedSpawnPos();
                player.player().teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                drain(player);
            });
            h.runAfterDelay(12, () -> {
                try {
                    tick(player.player());
                    var finished = drain(player).stream().filter(TutorialStatePayload.class::isInstance).map(TutorialStatePayload.class::cast)
                            .anyMatch(TutorialStatePayload::finished);
                    h.assertTrue(finished && !TutorialDirector.active(player.player()), "Reaching the area did not finish the tutorial");
                } finally {
                    player.player().server.getPlayerList().remove(player.player());
                }
                h.succeed();
            });
        } catch (RuntimeException failure) {
            player.player().server.getPlayerList().remove(player.player());
            throw failure;
        }
    }

    @GameTest(template = "sandevistan_test", batch = "tutorial_access", timeoutTicks = 60)
    public static void accessCanBeChangedAtWillAndAllowancesEndWithTheirStep(GameTestHelper h) {
        var player = player(h, "tutorial-access");
        try {
            TutorialDirector.start(player.player(), script());
            drain(player);
            h.assertFalse(TutorialDirector.allows(player.player(), "chat"), "Chat was available before anything allowed it");
            TutorialDirector.allow(player.player(), "chat");
            var allowed = lastState(drain(player));
            h.assertTrue(allowed != null && allowed.revealed().contains("chat") && TutorialDirector.allows(player.player(), "chat"),
                    "Allowing chat did not reach the player");
            TutorialDirector.reveal(player.player(), "key:key.togglePerspective");
            TutorialDirector.hide(player.player(), "chat");
            var after = lastState(drain(player));
            h.assertTrue(after != null && after.revealed().equals(java.util.List.of("key:key.togglePerspective")), "Reveal and hide were not synced");
            TutorialDirector.allow(player.player(), "inventory");
            drain(player);
            h.runAfterDelay(8, () -> {
                try {
                    tick(player.player());
                    var next = lastState(drain(player));
                    h.assertTrue(next != null && next.step() == 1, "The timed step did not advance");
                    h.assertFalse(TutorialDirector.allows(player.player(), "inventory"), "A temporary allowance outlived its step");
                    h.assertTrue(TutorialDirector.allows(player.player(), "key:key.togglePerspective"), "A permanent reveal was lost between steps");
                    boolean rejected = false;
                    try { TutorialDirector.allow(player.player(), "minimap"); } catch (IllegalArgumentException expected) { rejected = true; }
                    h.assertTrue(rejected, "An unknown element was accepted");
                } finally {
                    player.player().server.getPlayerList().remove(player.player());
                }
                h.succeed();
            });
        } catch (RuntimeException failure) {
            player.player().server.getPlayerList().remove(player.player());
            throw failure;
        }
    }

    private static TutorialStatePayload lastState(java.util.List<Object> packets) {
        return packets.stream().filter(TutorialStatePayload.class::isInstance).map(TutorialStatePayload.class::cast).reduce((a, b) -> b).orElse(null);
    }

    @GameTest(template = "sandevistan_test", batch = "tutorial_environment", timeoutTicks = 40)
    public static void theEnvironmentIsForcedBackToClearNoonWithoutSpreadOrSpawning(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var rules = server.getGameRules();
        var overworld = server.overworld();
        long time = overworld.getDayTime();
        boolean raining = overworld.isRaining(), thundering = overworld.isThundering();
        var saved = new java.util.LinkedHashMap<net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue>, Boolean>();
        for (var rule : TutorialEnvironment.RULES.keySet()) saved.put(rule, rules.getBoolean(rule));
        try {
            for (var rule : TutorialEnvironment.RULES.entrySet()) rules.getRule(rule.getKey()).set(!rule.getValue(), server);
            overworld.setDayTime(18000);
            overworld.setWeatherParameters(0, 1000, true, true);
            h.assertTrue(TutorialEnvironment.applySky(overworld), "A midnight storm was not reported as changed");
            TutorialEnvironment.applyRules(rules, server);
            h.assertTrue(overworld.getDayTime() == TutorialEnvironment.NOON, "The sky was not set to noon");
            h.assertTrue(!overworld.isRaining() && !overworld.isThundering(), "The weather was not cleared");
            for (var rule : TutorialEnvironment.RULES.entrySet())
                h.assertTrue(rules.getBoolean(rule.getKey()) == rule.getValue(), "Game rule " + rule.getKey().getId() + " was not enforced");
            h.assertTrue(!rules.getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK), "Fire can still spread");
            h.assertTrue(!rules.getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT), "Time can still advance");
            h.assertTrue(!TutorialEnvironment.applySky(overworld), "A correct sky was reported as changed");
        } finally {
            for (var rule : saved.entrySet()) rules.getRule(rule.getKey()).set(rule.getValue(), server);
            overworld.setDayTime(time);
            overworld.setWeatherParameters(0, 0, raining, thundering);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "tutorial_isolation", timeoutTicks = 40)
    public static void ordinaryWorldsNeverStartTheTutorial(GameTestHelper h) {
        h.assertFalse(TutorialDirector.isTutorialWorld(h.getLevel().getServer()), "The game test world was treated as the tutorial");
        // Outside the tutorial, advancements must still be awarded normally.
        var outsider = player(h, "tutorial-advancer");
        try {
            var root = outsider.player().server.getAdvancements().get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("story/root"));
            h.assertTrue(root != null && outsider.player().getAdvancements().award(root, "crafting_table") || outsider.player().getAdvancements().getOrStartProgress(root).isDone(),
                    "Advancements were blocked outside the tutorial");
        } finally {
            outsider.player().server.getPlayerList().remove(outsider.player());
        }
        var player = player(h, "tutorial-outsider");
        try {
            TutorialDirector.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player.player()));
            h.assertFalse(TutorialDirector.active(player.player()), "Logging into a normal world started the tutorial");
        } finally {
            player.player().server.getPlayerList().remove(player.player());
        }
        h.succeed();
    }
}
