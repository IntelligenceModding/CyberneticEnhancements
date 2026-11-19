package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import de.artemis.cyberneticenhancements.common.tutorial.TutorialDirector;
import de.artemis.cyberneticenhancements.common.tutorial.TutorialEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipInputStream;

/** A fresh, throwaway copy of the tutorial world in a reserved save folder, deleted once the player is back at the title. */
public final class TutorialWorld {
    /** Built by hand and zipped into the jar; until it exists, a flat placeholder world is created instead. */
    private static final String BUNDLED = "/cyberneticenhancements_tutorial/world.zip";

    private TutorialWorld() {}

    public static boolean isTutorialFolder(String folder) { return TutorialDirector.WORLD_FOLDER.equals(folder); }

    public static void play(Screen parent) {
        var mc = Minecraft.getInstance();
        cleanup();
        var flows = mc.createWorldOpenFlows();
        try (InputStream bundled = TutorialWorld.class.getResourceAsStream(BUNDLED)) {
            if (bundled != null) {
                extract(bundled, folder());
                flows.openWorld(TutorialDirector.WORLD_FOLDER, () -> mc.setScreen(parent));
                return;
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not prepare the bundled tutorial world", exception);
            cleanup();
            mc.setScreen(parent);
            return;
        }
        flows.createFreshLevel(TutorialDirector.WORLD_FOLDER, placeholderSettings(), new WorldOptions(0, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                parent);
    }

    private static LevelSettings placeholderSettings() {
        var rules = new GameRules();
        // The same rules the running tutorial world enforces, so a new world starts out correct.
        TutorialEnvironment.applyRules(rules, null);
        rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, null);
        return new LevelSettings(Component.translatable("screen.cyberneticenhancements.title.tutorial.world").getString(),
                GameType.SURVIVAL, false, Difficulty.PEACEFUL, false, rules, WorldDataConfiguration.DEFAULT);
    }

    private static Path folder() {
        return Minecraft.getInstance().getLevelSource().getBaseDir().resolve(TutorialDirector.WORLD_FOLDER);
    }

    private static void extract(InputStream zip, Path target) throws IOException {
        Files.createDirectories(target);
        try (var entries = new ZipInputStream(zip)) {
            for (var entry = entries.getNextEntry(); entry != null; entry = entries.getNextEntry()) {
                Path destination = target.resolve(entry.getName()).normalize();
                // Never write outside the reserved folder, whatever the archive contains.
                if (!destination.startsWith(target)) throw new IOException("Tutorial world entry escapes its folder: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(destination);
                else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(entries, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /** Deletes the previous tutorial copy, including after a crash; only while no world is open. */
    public static void cleanup() {
        var mc = Minecraft.getInstance();
        if (mc.level != null || mc.getSingleplayerServer() != null) return;
        Path folder = folder();
        if (!Files.exists(folder)) return;
        try (var paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            JournalHistoryStore.deleteWorld(mc.gameDirectory.toPath(), folder);
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not remove the previous tutorial world; retrying next time", exception);
        }
    }
}
