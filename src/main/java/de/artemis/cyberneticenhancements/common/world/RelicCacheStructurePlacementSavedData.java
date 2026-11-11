package de.artemis.cyberneticenhancements.common.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public final class RelicCacheStructurePlacementSavedData extends SavedData {
    private static final String DATA_NAME = "cyberneticenhancements_relic_cache_structures";
    private static final String PLACED_STRUCTURES_TAG = "PlacedStructures";
    private static final Factory<RelicCacheStructurePlacementSavedData> FACTORY = new Factory<>(
            RelicCacheStructurePlacementSavedData::new,
            RelicCacheStructurePlacementSavedData::load,
            null
    );

    private final Set<String> placedStructures = new HashSet<>();
    private final Map<BlockPos, RelicCacheTier> caches = new HashMap<>();

    public static RelicCacheStructurePlacementSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    static RelicCacheStructurePlacementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RelicCacheStructurePlacementSavedData data = new RelicCacheStructurePlacementSavedData();
        ListTag placed = tag.getList(PLACED_STRUCTURES_TAG, Tag.TAG_STRING);
        for (Tag entry : placed) {
            data.placedStructures.add(entry.getAsString());
        }
        for (Tag entry : tag.getList("Caches", Tag.TAG_COMPOUND)) {
            CompoundTag cache = (CompoundTag) entry;
            try {
                data.caches.put(BlockPos.of(cache.getLong("Pos")), RelicCacheTier.valueOf(cache.getString("Tier")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public void register(BlockPos pos, RelicCacheTier tier) {
        if (caches.put(pos.immutable(), tier) != tier) setDirty();
    }

    public void remove(BlockPos pos) {
        if (caches.remove(pos) != null) setDirty();
    }

    public List<BlockPos> cachePositions() {
        return List.copyOf(caches.keySet());
    }

    public RelicCacheTier tier(BlockPos pos) {
        return caches.get(pos);
    }

    public boolean isPlaced(String structureKey) {
        return placedStructures.contains(structureKey);
    }

    void forgetPlacement(String structureKey) {
        if (placedStructures.remove(structureKey)) setDirty();
    }

    public void markPlaced(String structureKey) {
        if (placedStructures.add(structureKey)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag placed = new ListTag();
        for (String entry : placedStructures) {
            placed.add(StringTag.valueOf(entry));
        }
        tag.put(PLACED_STRUCTURES_TAG, placed);
        ListTag cacheList = new ListTag();
        caches.forEach((pos, tier) -> {
            CompoundTag cache = new CompoundTag();
            cache.putLong("Pos", pos.asLong());
            cache.putString("Tier", tier.name());
            cacheList.add(cache);
        });
        tag.put("Caches", cacheList);
        return tag;
    }
}
