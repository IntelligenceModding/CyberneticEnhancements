package de.artemis.cyberneticenhancements.common.tarot;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Remains recorded after collection, wall removal, and chunk reloads. */
public final class TarotPlacements extends SavedData {
    private final Set<String> placed = new HashSet<>();
    public record Location(BlockPos anchor, Direction facing, String cardId) {}
    private final Map<UUID, Location> cards = new HashMap<>();
    private static final Factory<TarotPlacements> FACTORY = new Factory<>(TarotPlacements::new, TarotPlacements::load, null);
    public static TarotPlacements get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_tarot_structures"); }
    public boolean contains(String key) { return placed.contains(key); }
    public void mark(String key) { if (placed.add(key)) setDirty(); }
    public void register(TarotCardEntity card) {
        if (card.isDecoration() || card.isCollecting()) { remove(card.getUUID()); return; }
        Location location = new Location(card.anchor(), card.facing(), card.cardId());
        if (!location.equals(cards.put(card.getUUID(), location))) setDirty();
    }
    public void remove(UUID id) { if (cards.remove(id) != null) setDirty(); }
    public Optional<Location> nearest(Vec3 origin) {
        return nearest(origin, "all");
    }
    public Optional<Location> nearest(Vec3 origin, String filter) {
        return cards.values().stream().filter(card -> filter.equals("all") || card.cardId().equals(filter))
                .min(Comparator.comparingDouble(card -> Vec3.atCenterOf(card.anchor()).distanceToSqr(origin)));
    }
    static TarotPlacements load(CompoundTag tag, HolderLookup.Provider registries) {
        TarotPlacements data = new TarotPlacements();
        for (Tag entry : tag.getList("Structures", Tag.TAG_STRING)) data.placed.add(entry.getAsString());
        for (Tag entry : tag.getList("Cards", Tag.TAG_COMPOUND)) {
            CompoundTag card = (CompoundTag) entry;
            Direction facing = Direction.from3DDataValue(card.getInt("Facing"));
            if (card.hasUUID("Id") && facing.getAxis().isHorizontal() && TarotCatalog.find(card.getString("Card")) != null)
                data.cards.put(card.getUUID("Id"), new Location(BlockPos.of(card.getLong("Anchor")), facing, card.getString("Card")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag(); placed.stream().sorted().forEach(key -> list.add(StringTag.valueOf(key))); tag.put("Structures", list);
        ListTag locations = new ListTag();
        cards.forEach((id, location) -> {
            CompoundTag card = new CompoundTag(); card.putUUID("Id", id); card.putLong("Anchor", location.anchor().asLong());
            card.putInt("Facing", location.facing().get3DDataValue()); card.putString("Card", location.cardId()); locations.add(card);
        });
        tag.put("Cards", locations); return tag;
    }
}
