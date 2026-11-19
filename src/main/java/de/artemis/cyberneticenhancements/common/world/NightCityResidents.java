package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Persistent role assignments prevent replacing residents merely because their chunks unload. */
public final class NightCityResidents extends SavedData {
    private static final Factory<NightCityResidents> FACTORY = new Factory<>(NightCityResidents::new, NightCityResidents::load, null);
    private static final String BOUNDS = "ce_city_bounds", CITY = "ce_city_id";
    private final Map<String, City> cities = new LinkedHashMap<>();

    public enum Role {
        FIXER(ModEntityTypes.FIXER::get), RIPPERDOC(ModEntityTypes.RIPPERDOC::get), TECHIE(ModEntityTypes.TECHIE::get),
        NETRUNNER(ModEntityTypes.NETRUNNER::get), MERC(ModEntityTypes.MERC::get), MED_TECH(ModEntityTypes.MED_TECH::get),
        MAXTEC(ModEntityTypes.MAXTEC_GUARD::get), MEDTEC(ModEntityTypes.TRAUMA_TEAM::get);
        private final Supplier<? extends EntityType<? extends Mob>> type;
        Role(Supplier<? extends EntityType<? extends Mob>> type) { this.type = type; }
        public EntityType<? extends Mob> type() { return type.get(); }
    }

    private static final class Slot {
        UUID entity;
        long replaceAfter = -1;
        Slot(UUID entity) { this.entity = entity; }
    }
    private static final class City {
        final BoundingBox bounds;
        final Map<Role, Slot> residents = new EnumMap<>(Role.class);
        City(BoundingBox bounds) { this.bounds = bounds; }
    }

    public static NightCityResidents get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_city_residents");
    }

    public void register(BoundingBox bounds) {
        String key = key(bounds);
        if (!cities.containsKey(key)) { cities.put(key, new City(new BoundingBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()))); setDirty(); }
    }
    void forget(BoundingBox bounds) { if (cities.remove(key(bounds)) != null) setDirty(); }

    private static String key(BoundingBox b) { return b.minX() + ":" + b.minZ() + ":" + b.maxX() + ":" + b.maxZ(); }
    public static AABB area(BoundingBox bounds) { return AABB.of(bounds).inflate(0, 4, 0); }

    public static BoundingBox bounds(Mob mob) {
        int[] values = mob.getPersistentData().getIntArray(BOUNDS);
        if (values.length == 6) return new BoundingBox(values[0], values[1], values[2], values[3], values[4], values[5]);
        if (mob.level() instanceof ServerLevel level)
            for (var city : get(level).cities.values()) if (area(city.bounds).contains(mob.position())) return city.bounds;
        return null;
    }

    public static boolean assigned(Mob mob) { return mob.getPersistentData().getIntArray(BOUNDS).length == 6; }

    /** Upgrade legacy resident officers while preserving their UUID, health and city assignment. */
    public static boolean migrateGuard(MaxTacEntity old) {
        if (!(old.level() instanceof ServerLevel level) || !old.isCityGuard() || !old.isAlive()) return false;
        var bounds = bounds(old);
        var guard = ModEntityTypes.MAXTEC_GUARD.get().create(level);
        if (bounds == null || guard == null) return false;
        var saved = new CompoundTag(); old.saveWithoutId(saved);
        guard.load(saved);
        guard.setCustomName(null);
        float health = old.getHealth();
        old.discard();
        if (!level.addFreshEntity(guard)) return true;
        guard.configureCityGuard(bounds);
        guard.setHealth(Math.min(health, guard.getMaxHealth()));
        var data = get(level);
        for (var city : data.cities.values()) {
            var slot = city.residents.get(Role.MAXTEC);
            if (slot != null && slot.entity.equals(guard.getUUID())) { slot.replaceAfter = -1; data.setDirty(); }
        }
        NpcChunkLoading.track(guard);
        return true;
    }

    private static void assign(Mob mob, BoundingBox bounds) {
        mob.getPersistentData().putIntArray(BOUNDS, new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        mob.getPersistentData().putString(CITY, key(bounds)); mob.setPersistenceRequired();
        if (mob instanceof AbstractCityNpcEntity npc) { npc.setHome(mob.blockPosition()); npc.ensureIdentity(); }
        if (mob instanceof MaxTacEntity guard) guard.configureCityGuard(bounds);
        if (mob instanceof MaxTecGuardEntity guard) guard.configureCityGuard(bounds);
        NpcChunkLoading.track(mob);
    }

    public void maintain(ServerLevel level) {
        for (var city : cities.values()) {
            if (!level.hasChunkAt(city.bounds.getCenter())) continue;
            for (var role : Role.values()) {
                var slot = city.residents.get(role);
                if (slot != null && (slot.replaceAfter < 0 || level.getGameTime() < slot.replaceAfter)) continue;
                var candidate = level.getEntitiesOfClass(Mob.class, area(city.bounds), mob -> mob.getType() == role.type() && mob.isAlive()
                        && !mob.getTags().contains("cyberneticenhancements.trauma_dispatch")
                        && (!(mob instanceof MaxTacEntity officer) || officer.offender() == null)
                        && (!assigned(mob) || key(city.bounds).equals(mob.getPersistentData().getString(CITY))))
                        .stream().findFirst().orElse(null);
                if (candidate == null) {
                    var pos = findSpawn(level, city.bounds, role.ordinal());
                    if (pos == null) continue;
                    candidate = role.type().create(level);
                    if (candidate == null) continue;
                    candidate.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
                    assign(candidate, city.bounds);
                    if (!level.addFreshEntity(candidate)) continue;
                } else assign(candidate, city.bounds);
                city.residents.put(role, new Slot(candidate.getUUID())); setDirty();
            }
        }
    }

    private static BlockPos findSpawn(ServerLevel level, BoundingBox bounds, int role) {
        var center = bounds.getCenter();
        for (int attempt = 0; attempt < 64; attempt++) {
            int x, z;
            if (attempt < 24) {
                double angle = role * 2.39996 + attempt * 0.8;
                double radius = 3 + attempt * 0.5;
                x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
                z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            } else {
                x = bounds.minX() + level.random.nextInt(Math.max(1, bounds.getXSpan()));
                z = bounds.minZ() + level.random.nextInt(Math.max(1, bounds.getZSpan()));
            }
            if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ()
                    || level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, center.getY(), z)).getY();
            // Prefer street-level floors over the roofs picked by the surface heightmap.
            for (int y = Math.min(surface, bounds.minY() + 8); y >= bounds.minY() - 1; y--)
                if (safe(level, x, y, z)) return new BlockPos(x, y, z);
            if (surface <= bounds.maxY() + 1 && safe(level, x, surface, z)) return new BlockPos(x, surface, z);
        }
        return null;
    }

    private static boolean safe(ServerLevel level, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        return level.getWorldBorder().isWithinBounds(pos) && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && level.noCollision(new AABB(x + 0.18, y, z + 0.18, x + 0.82, y + 1.96, z + 0.82))
                && level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(0.5)).isEmpty();
    }

    private void lost(Mob mob) {
        var city = cities.get(mob.getPersistentData().getString(CITY));
        if (city == null) return;
        for (var slot : city.residents.values()) if (slot.entity.equals(mob.getUUID()) && slot.replaceAfter < 0) {
            slot.replaceAfter = mob.level().getGameTime() + 600; setDirty();
        }
    }

    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level) get(level).lost(mob);
    }
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level && mob.isRemoved()
                && mob.getRemovalReason() != Entity.RemovalReason.UNLOADED_TO_CHUNK) get(level).lost(mob);
    }
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 100 == 0) get(level).maintain(level);
    }

    static NightCityResidents load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new NightCityResidents();
        for (var entry : tag.getList("Cities", Tag.TAG_COMPOUND)) {
            var saved = (CompoundTag) entry; int[] b = saved.getIntArray("Bounds"); if (b.length != 6) continue;
            var city = new City(new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            for (var role : Role.values()) {
                var value = saved.getCompound(role.name()); if (!value.hasUUID("Entity")) continue;
                var slot = new Slot(value.getUUID("Entity")); slot.replaceAfter = value.getLong("ReplaceAfter"); city.residents.put(role, slot);
            }
            data.cities.put(key(city.bounds), city);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (var city : cities.values()) {
            var saved = new CompoundTag(); var b = city.bounds;
            saved.putIntArray("Bounds", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            city.residents.forEach((role, slot) -> { var value = new CompoundTag(); value.putUUID("Entity", slot.entity); value.putLong("ReplaceAfter", slot.replaceAfter); saved.put(role.name(), value); });
            list.add(saved);
        }
        tag.put("Cities", list); return tag;
    }
}
