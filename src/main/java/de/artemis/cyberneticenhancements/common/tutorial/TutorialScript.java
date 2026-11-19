package de.artemis.cyberneticenhancements.common.tutorial;

import com.google.gson.*;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import java.util.*;

/** The tutorial's ordered chapters and steps, from data/cyberneticenhancements/tutorial/script.json. */
public record TutorialScript(List<Step> steps) {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "script");
    public enum TriggerType { TICKS, SIGNAL, AREA }
    /** Area positions are relative to the world spawn when {@code relative} is set, so the placeholder world works too. */
    public record Trigger(TriggerType type, int ticks, String signal, BlockPos pos, boolean relative, double radius) {}
    /**
     * Nothing is shown or usable before a step introduces it. {@code reveal} unlocks for the rest of the run, {@code hide}
     * takes something away again, and {@code allow} only lasts while this step is current; see {@link TutorialAccess}.
     */
    public record Step(String chapter, String id, String objective, List<String> args, Trigger trigger,
                       List<String> reveal, List<String> hide, List<String> allow) {}
    public static final Set<String> ELEMENTS = Set.of("hotbar", "status", "experience", "inventory", "chat", "compass", "markers",
            "cyberware", "activation", "scan", "calls", "quests", "contacts", "archive");

    private static volatile TutorialScript current = new TutorialScript(List.of());

    public static TutorialScript current() { return current; }

    /** Replays the script up to and including the given step, which also rebuilds access when a checkpoint is restored. */
    public TutorialAccess accessAt(int step) {
        var access = new TutorialAccess();
        for (int index = 0; index <= step && index < steps.size(); index++) access.enter(steps.get(index));
        return access;
    }

    /** What is allowed at the given step. */
    public List<String> revealed(int step) { return accessAt(step).snapshot(); }

    public static TutorialScript parse(JsonElement json) {
        var steps = new ArrayList<Step>();
        for (var chapterElement : Json.array(json.getAsJsonObject(), "chapters")) {
            var chapter = chapterElement.getAsJsonObject();
            String title = Json.string(chapter, "title");
            for (var stepElement : Json.array(chapter, "steps")) {
                var step = stepElement.getAsJsonObject();
                var args = new ArrayList<String>();
                if (step.has("args")) for (var arg : step.getAsJsonArray("args")) args.add(arg.getAsString());
                steps.add(new Step(title, Json.string(step, "id"), Json.string(step, "objective"), List.copyOf(args),
                        trigger(step.getAsJsonObject("trigger")), tokens(step, "reveal"), tokens(step, "hide"), tokens(step, "allow")));
            }
        }
        if (steps.isEmpty()) throw new IllegalArgumentException("The tutorial needs at least one step");
        return new TutorialScript(List.copyOf(steps));
    }

    private static List<String> tokens(JsonObject step, String field) {
        var tokens = new ArrayList<String>();
        if (step.has(field)) for (var element : step.getAsJsonArray(field)) {
            if (!TutorialAccess.valid(element.getAsString()))
                throw new IllegalArgumentException("Unknown interface element or key in " + field + ": " + element.getAsString());
            tokens.add(element.getAsString());
        }
        return List.copyOf(tokens);
    }

    private static Trigger trigger(JsonObject json) {
        if (json == null) throw new IllegalArgumentException("Every step needs a trigger");
        var type = TriggerType.valueOf(Json.string(json, "type").toUpperCase(Locale.ROOT));
        return switch (type) {
            case TICKS -> new Trigger(type, Math.max(1, json.get("ticks").getAsInt()), "", BlockPos.ZERO, false, 0);
            case SIGNAL -> {
                String signal = Json.string(json, "signal");
                if (signal.isBlank() || signal.length() > TutorialSignalPayload.MAX_LENGTH) throw new IllegalArgumentException("Invalid signal: " + signal);
                yield new Trigger(type, 0, signal, BlockPos.ZERO, false, 0);
            }
            case AREA -> new Trigger(type, 0, "", new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt()),
                    json.has("relative") && json.get("relative").getAsBoolean(), Math.max(0.5, json.get("radius").getAsDouble()));
        };
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() { super(new Gson(), "tutorial"); }

        public static void register(AddReloadListenerEvent event) { event.addListener(new Loader()); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
            var json = files.get(ID);
            try {
                current = json == null ? new TutorialScript(List.of()) : parse(json);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid tutorial script " + ID + ": " + exception.getMessage(), exception);
            }
        }
    }

    private static final class Json {
        static JsonArray array(JsonObject json, String key) {
            if (!json.has(key) || !json.get(key).isJsonArray()) throw new IllegalArgumentException("Missing array: " + key);
            return json.getAsJsonArray(key);
        }

        static String string(JsonObject json, String key) {
            if (!json.has(key)) throw new IllegalArgumentException("Missing field: " + key);
            return json.get(key).getAsString();
        }
    }
}
