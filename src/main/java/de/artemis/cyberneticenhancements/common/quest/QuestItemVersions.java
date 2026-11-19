package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

final class QuestItemVersions extends SavedData {
    static final String REVISION = "QuestItemRevision";
    private static final Factory<QuestItemVersions> FACTORY = new Factory<>(QuestItemVersions::new, QuestItemVersions::load, null);
    private final Map<String, Long> revisions = new HashMap<>();
    private final java.util.Set<String> retired = new java.util.HashSet<>();
    private net.minecraft.server.MinecraftServer server;

    void retire(String id) {
        if (retired.add(id)) { setDirty(); if (server != null) QuestItemCleanup.purge(server); }
    }
    boolean retired(String id) { return retired.contains(id); }
    boolean hasHistory() { return !revisions.isEmpty() || !retired.isEmpty(); }
    boolean issued(String contract, String type) { return revisions.containsKey(contract + "/" + type); }

    static QuestItemVersions get(ServerLevel level) {
        var versions = level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_quest_items");
        versions.server = level.getServer();
        return versions;
    }

    static String key(ItemStack stack) {
        if (stack.isEmpty()) return "";
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String contract = tag.getString("QuestContractId"), type = tag.getString("QuestItemType");
        return contract.isEmpty() || type.isEmpty() ? "" : contract + "/" + type;
    }

    boolean current(ItemStack stack) {
        String key = key(stack);
        return key.isEmpty() || current(key, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong(REVISION));
    }

    boolean current(String key, long revision) {
        int separator = key.indexOf('/');
        return (separator < 0 || !retired.contains(key.substring(0, separator))) && revisions.getOrDefault(key, 0L) == revision;
    }

    void issue(ItemStack stack) {
        String key = key(stack);
        if (key.isEmpty()) return;
        long revision = revisions.getOrDefault(key, 0L) + 1;
        revisions.put(key, revision);
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(REVISION, revision);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.setCount(1);
        setDirty();
        if (server != null) QuestItemCleanup.purge(server);
    }

    static QuestItemVersions load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new QuestItemVersions();
        for (var entry : tag.getList("Retired", Tag.TAG_STRING)) result.retired.add(entry.getAsString());
        for (Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            var item = (CompoundTag) entry;
            result.revisions.put(item.getString("Key"), item.getLong("Revision"));
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        revisions.forEach((key, revision) -> {
            var item = new CompoundTag();
            item.putString("Key", key);
            item.putLong("Revision", revision);
            list.add(item);
        });
        tag.put("Items", list);
        var finished = new ListTag();
        retired.stream().sorted().forEach(id -> finished.add(net.minecraft.nbt.StringTag.valueOf(id)));
        tag.put("Retired", finished);
        return tag;
    }
}
