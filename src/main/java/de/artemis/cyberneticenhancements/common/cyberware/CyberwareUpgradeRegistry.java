package de.artemis.cyberneticenhancements.common.cyberware;

import com.google.gson.*;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.CyberwareUpgradesPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

 public final class CyberwareUpgradeRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private record Snapshot(Map<String, CyberwareUpgradeDefinition> definitions, String json) {}
    private static volatile Snapshot server = new Snapshot(Map.of(), "{}");
    private static volatile Snapshot client = new Snapshot(Map.of(), "{}");
    private static volatile int clientRevision;
    public static int clientRevision() { return clientRevision; }
    public static final CyberwareUpgradeDefinition EMPTY = new CyberwareUpgradeDefinition(List.of());

    public CyberwareUpgradeRegistry() { super(GSON, "cyberware_upgrades"); }
    public static CyberwareUpgradeDefinition get(String id) {
        return EffectiveSide.get().isServer() ? serverDefinition(id) : client.definitions.getOrDefault(id, EMPTY);
    }
    static CyberwareUpgradeDefinition serverDefinition(String id) { return server.definitions.getOrDefault(id, EMPTY); }
    public static void registerReload(AddReloadListenerEvent event) { event.addListener(new CyberwareUpgradeRegistry()); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<String, CyberwareUpgradeDefinition> definitions = new HashMap<>();
        JsonObject sync = new JsonObject();
        for (var entry : files.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                if (!id.getNamespace().equals(CyberneticEnhancements.MOD_ID))
                    throw new IllegalArgumentException("Upgrade files must use the cyberneticenhancements namespace");
                CyberwareCatalog.get(id.getPath());
                var definition = CyberwareUpgradeDefinition.parse(entry.getValue());
                if (!CyberwareUpgradeHelper.supportsFunction(id.getPath())
                        && definition.levels().stream().anyMatch(level -> level.functionMultiplier() != 1))
                    throw new IllegalArgumentException("This implant has no function_multiplier behavior; use its supported effect or activation fields");
                for (var level : definition.levels()) for (String effect : level.effects().keySet()) {
                    if (Arrays.stream(CyberwareEffectType.values()).noneMatch(type -> type.id().equals(effect)))
                        throw new IllegalArgumentException("Unknown effect: " + effect);
                }
                var allMaterials = new ArrayList<>(definition.repairMaterials());
                for (var level : definition.levels()) allMaterials.addAll(level.materials());
                for (var material : allMaterials) {
                    if (!BuiltInRegistries.ITEM.containsKey(material.item()) || material.item().equals(ResourceLocation.withDefaultNamespace("air")))
                        throw new IllegalArgumentException("Unknown material item: " + material.item());
                    if (material.count() > BuiltInRegistries.ITEM.get(material.item()).getDefaultMaxStackSize())
                        throw new IllegalArgumentException("Material count exceeds item stack size: " + material.item());
                }
                definitions.put(id.getPath(), definition);
                sync.add(id.getPath(), entry.getValue());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid cyberware upgrade file " + id + ": " + exception.getMessage(), exception);
            }
        }
        String json = GSON.toJson(sync);
        if (json.length() > CyberwareUpgradesPayload.MAX_LENGTH) throw new IllegalArgumentException("Upgrade definitions exceed sync size limit");
        server = new Snapshot(Map.copyOf(definitions), json);
    }

    public static void receive(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, CyberwareUpgradeDefinition> definitions = new HashMap<>();
        for (var entry : root.entrySet()) definitions.put(entry.getKey(), CyberwareUpgradeDefinition.parse(entry.getValue()));
        client = new Snapshot(Map.copyOf(definitions), json);
        clientRevision++;
    }
    public static void clearClient() { client = new Snapshot(Map.of(), "{}"); clientRevision++; }

    public static void sync(OnDatapackSyncEvent event) {
        var payload = new CyberwareUpgradesPayload(server.json);
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
            CyberwareEffects.refreshPlayerCyberware(event.getPlayer());
        } else {
            for (var player : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(player, payload);

                if (player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.TechStationMenu menu)
                    menu.slotsChanged(player.getInventory());
                CyberwareEffects.refreshPlayerCyberware(player);
            }
        }
    }
}
