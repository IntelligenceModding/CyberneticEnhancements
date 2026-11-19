package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestSiteReservations extends SavedData {
    private static final Factory<QuestSiteReservations> FACTORY = new Factory<>(QuestSiteReservations::new, QuestSiteReservations::load, null);
    private final Map<String, Site> sites = new LinkedHashMap<>();

    public static QuestSiteReservations get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_quest_sites");
    }

    static QuestSiteReservations load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new QuestSiteReservations();
        for (Tag entry : tag.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag site = (CompoundTag) entry;
            data.sites.put(site.getString("Id"), new Site(BlockPos.of(site.getLong("Pos")), Math.max(32, site.getInt("Radius")), site.getLong("Until")));
        }
        return data;
    }

    public boolean available(BlockPos pos, int radius, String ignoredId, long now) {
        if (sites.entrySet().removeIf(entry -> entry.getValue().until > 0 && entry.getValue().until <= now)) setDirty();
        for (var entry : sites.entrySet()) {
            if (entry.getKey().equals(ignoredId)) continue;
            Site site = entry.getValue();
            long dx = (long) pos.getX() - site.pos.getX();
            long dz = (long) pos.getZ() - site.pos.getZ();
            long clearance = Math.max(32, radius) + site.radius;
            if (dx * dx + dz * dz < clearance * clearance) return false;
        }
        return true;
    }

    public void reserve(QuestContract contract, boolean active, long now) {
        // Geological surveys observe existing terrain and do not occupy a quest construction site.
        if (contract.chromiumSurvey() || contract.targetPos() == null) return;
        Site previous = sites.get(contract.id());
        if (!active && previous != null && previous.until <= 0) return;
        if (previous != null && previous.until <= 0 && !previous.pos.equals(contract.targetPos())) {
            sites.put(contract.id() + "@" + previous.pos.asLong(), new Site(previous.pos, previous.radius, 0));
        }
        Site site = new Site(contract.targetPos().immutable(), Math.max(32, contract.areaRadius() + 16), active ? -1 : now + 48000);
        if (!site.equals(sites.put(contract.id(), site))) setDirty();
    }

    public void finish(String id) {
        Site site = sites.get(id);
        if (site == null) return;
        sites.put(id, new Site(site.pos, site.radius, 0));
        sites.replaceAll((key, value) -> key.startsWith(id + "@") ? new Site(value.pos, value.radius, 0) : value);
        int recent = (int) sites.values().stream().filter(value -> value.until == 0).count();
        var iterator = sites.entrySet().iterator();
        while (recent > 256 && iterator.hasNext()) {
            if (iterator.next().getValue().until == 0) {
                iterator.remove();
                recent--;
            }
        }
        setDirty();
    }

    public void reserveAdditional(String id, String suffix, BlockPos pos, int radius) {
        sites.put(id + "@" + suffix, new Site(pos.immutable(), Math.max(32, radius), -1));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        sites.forEach((id, site) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", id);
            entry.putLong("Pos", site.pos.asLong());
            entry.putInt("Radius", site.radius);
            entry.putLong("Until", site.until);
            list.add(entry);
        });
        tag.put("Sites", list);
        return tag;
    }

    private record Site(BlockPos pos, int radius, long until) {
    }
}
