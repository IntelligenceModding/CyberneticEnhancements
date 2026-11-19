package de.artemis.cyberneticenhancements.common.economy;

import com.google.gson.*;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class EconomyBalance extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "balance");
    private static final Map<String, Integer> DEFAULTS = defaults();
    private static volatile Map<String, Integer> server = DEFAULTS, client = DEFAULTS;
    private static int clientRevision;

    private EconomyBalance() { super(GSON, "economy"); }

    private static Map<String, Integer> defaults() {
        try (var stream = EconomyBalance.class.getResourceAsStream("/data/cyberneticenhancements/economy/balance.json")) {
            if (stream == null) throw new IllegalStateException("Missing bundled economy definitions");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Integer> result = new TreeMap<>();
            json.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
            return Map.copyOf(result);
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }

    public static Map<String, Integer> parse(JsonElement json) {
        if (!json.isJsonObject()) throw new IllegalArgumentException("Economy definition must be an object");
        Map<String, Integer> result = new HashMap<>(DEFAULTS);
        for (var entry : json.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            if (!DEFAULTS.containsKey(key)) throw new IllegalArgumentException("Unknown economy key: " + key);
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Economy values must be whole numbers: " + key);
            int value;
            try { value = entry.getValue().getAsBigDecimal().intValueExact(); }
            catch (ArithmeticException | NumberFormatException exception) { throw new IllegalArgumentException("Invalid integer: " + key, exception); }
            int maximum = key.startsWith("access.") ? 4 : key.startsWith("discovery.") ? 100 : key.startsWith("trust.") ? 300 : 1_000_000;
            if (value < 0 || value > maximum || ((key.startsWith("shop.") || key.startsWith("service.")) && value == 0))
                throw new IllegalArgumentException("Economy value out of bounds: " + key);
            result.put(key, value);
        }
        if (result.get("support.minimum") > result.get("support.maximum")) throw new IllegalArgumentException("Support minimum exceeds maximum");
        return Map.copyOf(result);
    }

    public static int value(String key) {
        Integer value = (EffectiveSide.get().isClient() ? client : server).get(key);
        if (value == null) throw new IllegalArgumentException("Unknown economy key: " + key);
        return value;
    }

    public static int tier(String key, int tier) { return value(key + "." + Math.clamp(tier, 1, 3)); }
    public static void registerReload(AddReloadListenerEvent event) { event.addListener(new EconomyBalance()); }
    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resources, ProfilerFiller profiler) {
        var resource = resources.getResource(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "economy/balance.json"));
        if (resource.isEmpty()) return Map.of();
        try (var reader = resource.get().openAsReader()) {
            return Map.of(ID, JsonParser.parseReader(reader));
        } catch (java.io.IOException | JsonParseException exception) {
            throw new IllegalArgumentException("Invalid economy/balance.json: " + exception.getMessage(), exception);
        }
    }
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        server = files.containsKey(ID) ? parse(files.get(ID)) : DEFAULTS;
    }
    public static int clientRevision() { return clientRevision; }
    public static void receive(String json) { client = parse(JsonParser.parseString(json)); clientRevision++; }
    public static void clearClient() { client = DEFAULTS; clientRevision++; }
    public static void sync(OnDatapackSyncEvent event) {
        var payload = new de.artemis.cyberneticenhancements.common.network.EconomyBalancePayload(GSON.toJson(server));
        if (event.getPlayer() != null) PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        else for (var player : event.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, payload);
    }
}
