package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.network.BarredSitesPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/**
 * What a player has done to a world encounter: striking its people, or breaking the build itself. Either is remembered
 * against that site for good, and its leader will not give them the quest again. Kept on the player, per site.
 */
public final class WorldEncounterOffenses {
    private static final String KEY = "WorldEncounterOffenses";
    /** Pairs of (site position, game time the lockout ends) for a failed quest: the leader refuses for a while, then deals again. */
    private static final String LOCKOUT_KEY = "WorldEncounterLockouts";
    /** How long a failed quest keeps the leader from dealing with that player: fifteen minutes. */
    public static final int LOCKOUT_TICKS = 15 * 60 * 20;
    /** Offences this close to a leader are held against that site; sites are far further apart than this. */
    private static final int SITE_RADIUS = 64;
    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "chests/med_tec_av");
    /** How far from the loot its people notice and answer. */
    private static final int ALARM_RADIUS = 40;
    private static final ResourceLocation MED_TEC_AV = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "med_tec_av");

    private WorldEncounterOffenses() {}

    /** Whether this player has ever wronged the site this leader stands at. */
    public static boolean refuses(Player player, WorldEncounterNpcEntity leader) {
        return heldAgainst(player, leader.getRestrictCenter()) || lockoutTicks(player, leader.getRestrictCenter()) > 0;
    }

    /** How much longer the leader at this site refuses this player for a failed quest, or 0 if they do not. */
    public static long lockoutTicks(Player player, BlockPos site) {
        long[] pairs = player.getPersistentData().getLongArray(LOCKOUT_KEY), longest = {0};
        long now = player.level().getGameTime();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            var at = BlockPos.of(pairs[i]);
            long dx = at.getX() - site.getX(), dz = at.getZ() - site.getZ();
            if (dx * dx + dz * dz <= (long) SITE_RADIUS * SITE_RADIUS) longest[0] = Math.max(longest[0], pairs[i + 1] - now);
        }
        return longest[0];
    }

    /** A failed quest: the leader at this site will not deal with the player for {@link #LOCKOUT_TICKS}. */
    public static void lockOut(ServerPlayer player, BlockPos site) {
        long now = player.level().getGameTime();
        var kept = new java.util.ArrayList<Long>();
        long[] pairs = player.getPersistentData().getLongArray(LOCKOUT_KEY);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            var at = BlockPos.of(pairs[i]);
            long dx = at.getX() - site.getX(), dz = at.getZ() - site.getZ();
            if (pairs[i + 1] > now && dx * dx + dz * dz > (long) SITE_RADIUS * SITE_RADIUS) { kept.add(pairs[i]); kept.add(pairs[i + 1]); }
        }
        kept.add(site.asLong());
        kept.add(now + LOCKOUT_TICKS);
        player.getPersistentData().put(LOCKOUT_KEY, new LongArrayTag(kept.stream().mapToLong(Long::longValue).toArray()));
        sync(player);
    }

    /** Whether this player has ever wronged the site around this position. */
    public static boolean heldAgainst(Player player, BlockPos site) {
        for (long packed : player.getPersistentData().getLongArray(KEY)) {
            var at = BlockPos.of(packed);
            long dx = at.getX() - site.getX(), dz = at.getZ() - site.getZ();
            if (dx * dx + dz * dz <= (long) SITE_RADIUS * SITE_RADIUS) return true;
        }
        return false;
    }

    public static void record(Player player, BlockPos where) {
        if (player.isSpectator()) return;
        long packed = where.asLong();
        long[] known = player.getPersistentData().getLongArray(KEY);
        if (Arrays.stream(known).anyMatch(value -> value == packed)) return;
        long[] updated = Arrays.copyOf(known, known.length + 1);
        updated[known.length] = packed;
        player.getPersistentData().put(KEY, new LongArrayTag(updated));
        if (player instanceof ServerPlayer serverPlayer) sync(serverPlayer);
    }

    /** Tells the player's client where it is no longer welcome, so the doorways there can show it. */
    public static void sync(ServerPlayer player) {
        long now = player.level().getGameTime();
        long[] pairs = player.getPersistentData().getLongArray(LOCKOUT_KEY);
        var remaining = new java.util.ArrayList<Long>();
        for (int i = 0; i + 1 < pairs.length; i += 2) if (pairs[i + 1] > now) { remaining.add(pairs[i]); remaining.add(pairs[i + 1] - now); }
        PacketDistributor.sendToPlayer(player, new BarredSitesPayload(player.getPersistentData().getLongArray(KEY),
                remaining.stream().mapToLong(Long::longValue).toArray()));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    /** True for the build itself; the scorched ground around it is not part of what they would defend. */
    public static boolean isPartOfBuild(ServerLevel level, BlockPos pos) {
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(MED_TEC_AV);
        if (structure == null) return false;
        var start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (!start.isValid()) return false;
        for (var piece : start.getPieces())
            if (!(piece instanceof ScorchedGroundPiece) && piece.getBoundingBox().isInside(pos)) return true;
        return false;
    }

    /** Every kind of block the prefab is made of; the ground it stands on is not among them. */
    private static java.util.Set<net.minecraft.world.level.block.Block> buildBlocks;

    private static synchronized java.util.Set<net.minecraft.world.level.block.Block> buildBlocks(ServerLevel level) {
        if (buildBlocks != null) return buildBlocks;
        var blocks = new java.util.HashSet<net.minecraft.world.level.block.Block>();
        var template = level.getServer().getStructureManager().get(MED_TEC_AV);
        if (template.isEmpty()) return blocks; // Not cached, so a later try can still find it.
        var saved = template.get().save(new net.minecraft.nbt.CompoundTag());
        var palettes = saved.contains("palettes", Tag.TAG_LIST) ? saved.getList("palettes", Tag.TAG_LIST) : new net.minecraft.nbt.ListTag();
        if (saved.contains("palette", Tag.TAG_LIST)) palettes.add(saved.getList("palette", Tag.TAG_COMPOUND));
        for (int i = 0; i < palettes.size(); i++) {
            var palette = palettes.getList(i);
            for (int j = 0; j < palette.size(); j++) {
                var name = ResourceLocation.tryParse(((net.minecraft.nbt.CompoundTag) palette.get(j)).getString("Name"));
                if (name != null) level.registryAccess().registryOrThrow(Registries.BLOCK).getOptional(name).ifPresent(blocks::add);
            }
        }
        return buildBlocks = blocks;
    }

    /**
     * Breaking a block the prefab is made of earns the same fifteen-minute lockout as any other trouble there. The ground
     * around and under it lies inside the same bounds, but is not what they would defend, so it does not count.
     */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player && !player.isSpectator() && event.getLevel() instanceof ServerLevel level
                && isPartOfBuild(level, event.getPos()) && buildBlocks(level).contains(event.getState().getBlock())) lockOut(player, event.getPos());
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(KEY, Tag.TAG_LONG_ARRAY))
            event.getEntity().getPersistentData().put(KEY, event.getOriginal().getPersistentData().get(KEY).copy());
        if (event.getOriginal().getPersistentData().contains(LOCKOUT_KEY, Tag.TAG_LONG_ARRAY))
            event.getEntity().getPersistentData().put(LOCKOUT_KEY, event.getOriginal().getPersistentData().get(LOCKOUT_KEY).copy());
    }

    /**
     * Opening the site's chest sets everyone there on the player, every time. The loot table is gone once the chest has been
     * opened, so afterwards it is recognised by standing inside the build instead.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getBlockEntity(event.getPos()) instanceof RandomizableContainerBlockEntity chest
                && (chest.getLootTable() != null ? chest.getLootTable().location().equals(LOOT) : isPartOfBuild(level, event.getPos()))) alarm(player, level, event.getPos());
    }

    /** The site's loot has been taken: the player is held against it for good, and everyone standing guard goes for them. */
    public static void alarm(ServerPlayer player, ServerLevel level, BlockPos where) {
        if (player.isCreative() || player.isSpectator() || !isPartOfBuild(level, where)) return;
        record(player, where);
        boolean first = true;
        for (var npc : level.getEntitiesOfClass(WorldEncounterNpcEntity.class, new AABB(where).inflate(ALARM_RADIUS),
                npc -> npc.isAlive() && npc.role() != WorldEncounterNpcEntity.Role.PATIENT)) {
            npc.provoke(player, first);
            first = false;
        }
    }
}
