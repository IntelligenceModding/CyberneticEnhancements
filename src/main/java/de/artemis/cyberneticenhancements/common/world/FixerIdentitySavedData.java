package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NameEntry;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FixerIdentitySavedData extends SavedData {
    private static final String DATA_NAME = CyberneticEnhancements.MOD_ID + "_fixer_identities";
    private static final String ASSIGNMENTS_TAG = "Assignments";
    private static final String UUID_TAG = "Uuid";
    private static final String NAME_TAG = "Name";
    private static final String NICKNAME_TAG = "Nickname";
    private static final String NAME_COLOR_TAG = "NameColor";
    private static final String APPEARANCE_TAG = "Appearance";
    private static final String CATEGORY_TAG = "Category";
    private static final String TYPE_TAG = "Type";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String HOME_X_TAG = "HomeX";
    private static final String HOME_Y_TAG = "HomeY";
    private static final String HOME_Z_TAG = "HomeZ";
    private static final String HAS_HOME_TAG = "HasHome";
    private static final String DEFAULT_TYPE_ID = "fixer";
    private static final Factory<FixerIdentitySavedData> FACTORY =
            new Factory<>(FixerIdentitySavedData::new, FixerIdentitySavedData::load, null);
    private final Map<UUID, IdentityAssignment> assignedIdentities = new LinkedHashMap<>();
    private final java.util.Set<UUID> deceased = new java.util.HashSet<>();

    public static FixerIdentitySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    static FixerIdentitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FixerIdentitySavedData data = new FixerIdentitySavedData();
        ListTag assignments = tag.getList(ASSIGNMENTS_TAG, Tag.TAG_COMPOUND);
        for (Tag entry : assignments) {
            if (!(entry instanceof CompoundTag compound) || !compound.hasUUID(UUID_TAG) || !compound.contains(NAME_TAG, Tag.TAG_STRING)) {
                continue;
            }
            String name = compound.getString(NAME_TAG);
            NpcCategory category = compound.contains(CATEGORY_TAG, Tag.TAG_STRING)
                    ? NpcCategory.fromId(compound.getString(CATEGORY_TAG))
                    : NpcIdentityConfig.categoryForName(name);
            String typeId = compound.contains(TYPE_TAG, Tag.TAG_STRING) ? compound.getString(TYPE_TAG) : DEFAULT_TYPE_ID;
            String dimensionId = compound.contains(DIMENSION_TAG, Tag.TAG_STRING) ? compound.getString(DIMENSION_TAG) : "";
            String nickname = compound.contains(NICKNAME_TAG, Tag.TAG_STRING) ? compound.getString(NICKNAME_TAG) : "";
            String nameColor = compound.contains(NAME_COLOR_TAG, Tag.TAG_STRING) ? compound.getString(NAME_COLOR_TAG) : "aqua";
            String appearance = compound.contains(APPEARANCE_TAG, Tag.TAG_STRING) ? compound.getString(APPEARANCE_TAG) : "";
            BlockPos homePos = compound.getBoolean(HAS_HOME_TAG)
                    ? new BlockPos(compound.getInt(HOME_X_TAG), compound.getInt(HOME_Y_TAG), compound.getInt(HOME_Z_TAG))
                    : null;
            data.assignedIdentities.put(compound.getUUID(UUID_TAG), new IdentityAssignment(name, nickname, nameColor, appearance, category, typeId, dimensionId, homePos));
        }
        for (var entry : tag.getList("Deceased", Tag.TAG_COMPOUND)) {
            var npc = (CompoundTag) entry;
            if (npc.hasUUID(UUID_TAG)) data.deceased.add(npc.getUUID(UUID_TAG));
        }
        return data;
    }

    public boolean isDeceased(UUID npcId) { return deceased.contains(npcId); }

    public void markDeceased(UUID npcId) {
        if (deceased.add(npcId)) setDirty();
    }

    public IdentityAssignment getOrAssignIdentity(UUID fixerId, String npcTypeId, RandomSource random) {
        IdentityAssignment existing = assignedIdentities.get(fixerId);
        if (existing != null && !existing.name().isBlank()) {
            if (!npcTypeId.isBlank() && !npcTypeId.equals(existing.npcTypeId())) {
                IdentityAssignment updated = existing.withNpcType(npcTypeId);
                assignedIdentities.put(fixerId, updated);
                setDirty();
                return updated;
            }
            return existing;
        }

        List<NameEntry> allEntries = NpcIdentityConfig.categories().flattened();
        List<NameEntry> source = allEntries.isEmpty() ? defaultEntries() : allEntries;
        List<NameEntry> available = new ArrayList<>(source);
        assignedIdentities.values().stream()
                .map(IdentityAssignment::name)
                .forEach(usedName -> available.removeIf(entry -> entry.name().equals(usedName)));
        List<NameEntry> finalSource = available.isEmpty() ? source : available;
        NameEntry chosen = finalSource.get(random.nextInt(finalSource.size()));
        IdentityAssignment assignment = new IdentityAssignment(
                chosen.name(),
                existing == null ? "" : existing.nickname(),
                existing == null ? "aqua" : existing.nameColorId(),
                existing == null ? "" : existing.appearance(),
                chosen.category(),
                npcTypeId.isBlank() ? DEFAULT_TYPE_ID : npcTypeId,
                existing == null ? "" : existing.dimensionId(),
                existing == null ? null : existing.homePos()
        );
        assignedIdentities.put(fixerId, assignment);
        setDirty();
        return assignment;
    }

    public IdentityAssignment updateHome(UUID fixerId, String dimensionId, BlockPos homePos) {
        IdentityAssignment existing = assignedIdentities.get(fixerId);
        if (existing == null) {
            IdentityAssignment created = new IdentityAssignment("", "", "aqua", "", NpcCategory.GENERIC, DEFAULT_TYPE_ID, dimensionId, homePos.immutable());
            assignedIdentities.put(fixerId, created);
            setDirty();
            return created;
        }
        IdentityAssignment updated = existing.withHome(dimensionId, homePos);
        if (!updated.equals(existing)) {
            assignedIdentities.put(fixerId, updated);
            setDirty();
        }
        return updated;
    }

    public IdentityAssignment identity(UUID fixerId) {
        return assignedIdentities.get(fixerId);
    }

    public IdentityAssignment renameIdentity(UUID fixerId, String nickname) {
        IdentityAssignment existing = assignedIdentities.get(fixerId);
        if (existing == null) {
            return null;
        }
        IdentityAssignment updated = existing.withNickname(nickname);
        if (!updated.equals(existing)) {
            assignedIdentities.put(fixerId, updated);
            setDirty();
        }
        return updated;
    }

    public IdentityAssignment setAppearance(UUID fixerId, String appearance) {
        IdentityAssignment existing = assignedIdentities.get(fixerId);
        if (existing == null) {
            return null;
        }
        IdentityAssignment updated = existing.withAppearance(appearance);
        if (!updated.equals(existing)) {
            assignedIdentities.put(fixerId, updated);
            setDirty();
        }
        return updated;
    }

    public IdentityAssignment setNameColor(UUID fixerId, String nameColorId) {
        IdentityAssignment existing = assignedIdentities.get(fixerId);
        if (existing == null) {
            return null;
        }
        IdentityAssignment updated = existing.withNameColor(nameColorId);
        if (!updated.equals(existing)) {
            assignedIdentities.put(fixerId, updated);
            setDirty();
        }
        return updated;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag assignments = new ListTag();
        for (Map.Entry<UUID, IdentityAssignment> entry : assignedIdentities.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putUUID(UUID_TAG, entry.getKey());
            compound.put(NAME_TAG, StringTag.valueOf(entry.getValue().name()));
            if (!entry.getValue().nickname().isBlank()) {
                compound.put(NICKNAME_TAG, StringTag.valueOf(entry.getValue().nickname()));
            }
            if (!entry.getValue().nameColorId().isBlank()) {
                compound.put(NAME_COLOR_TAG, StringTag.valueOf(entry.getValue().nameColorId()));
            }
            if (!entry.getValue().appearance().isBlank()) {
                compound.put(APPEARANCE_TAG, StringTag.valueOf(entry.getValue().appearance()));
            }
            compound.put(CATEGORY_TAG, StringTag.valueOf(entry.getValue().category().id()));
            compound.put(TYPE_TAG, StringTag.valueOf(entry.getValue().npcTypeId()));
            if (!entry.getValue().dimensionId().isBlank()) {
                compound.put(DIMENSION_TAG, StringTag.valueOf(entry.getValue().dimensionId()));
            }
            if (entry.getValue().homePos() != null) {
                compound.putBoolean(HAS_HOME_TAG, true);
                compound.putInt(HOME_X_TAG, entry.getValue().homePos().getX());
                compound.putInt(HOME_Y_TAG, entry.getValue().homePos().getY());
                compound.putInt(HOME_Z_TAG, entry.getValue().homePos().getZ());
            }
            assignments.add(compound);
        }
        tag.put(ASSIGNMENTS_TAG, assignments);
        var deaths = new ListTag();
        for (var npcId : deceased) {
            var entry = new CompoundTag();
            entry.putUUID(UUID_TAG, npcId);
            deaths.add(entry);
        }
        tag.put("Deceased", deaths);
        return tag;
    }

    private static List<NameEntry> defaultEntries() {
        return NpcIdentityConfig.defaultCategories().flattened();
    }

    public record IdentityAssignment(String name, String nickname, String nameColorId, String appearance, NpcCategory category, String npcTypeId, String dimensionId, BlockPos homePos) {
        public IdentityAssignment {
            nameColorId = "light_purple".equals(nameColorId) ? "light_purple" : "aqua";
            if (NpcAppearanceRules.reserved(appearance)) appearance = "";
        }
        public String displayName() {
            return nickname == null || nickname.isBlank() ? name : nickname;
        }

        public IdentityAssignment withNickname(String newNickname) {
            return new IdentityAssignment(name, newNickname == null ? "" : newNickname.trim(), nameColorId, appearance, category, npcTypeId, dimensionId, homePos);
        }

        public IdentityAssignment withNameColor(String newNameColorId) {
            String sanitized = newNameColorId == null || newNameColorId.isBlank() ? "aqua" : newNameColorId.trim();
            return new IdentityAssignment(name, nickname, sanitized, appearance, category, npcTypeId, dimensionId, homePos);
        }

        public IdentityAssignment withAppearance(String newAppearance) {
            return new IdentityAssignment(name, nickname, nameColorId, newAppearance == null ? "" : newAppearance.trim(), category, npcTypeId, dimensionId, homePos);
        }

        public IdentityAssignment withNpcType(String newNpcTypeId) {
            return new IdentityAssignment(name, nickname, nameColorId, appearance, category, newNpcTypeId == null || newNpcTypeId.isBlank() ? DEFAULT_TYPE_ID : newNpcTypeId, dimensionId, homePos);
        }

        public IdentityAssignment withHome(String newDimensionId, BlockPos newHomePos) {
            return new IdentityAssignment(name, nickname, nameColorId, appearance, category, npcTypeId, newDimensionId == null ? "" : newDimensionId, newHomePos == null ? null : newHomePos.immutable());
        }
    }
}
