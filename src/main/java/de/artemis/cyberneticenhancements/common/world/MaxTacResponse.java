package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.MaxTacEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MaxTacResponse extends SavedData {
    private static final Factory<MaxTacResponse> FACTORY = new Factory<>(MaxTacResponse::new, MaxTacResponse::load, null);
    public static final int SQUAD_SIZE = 4;
    private static final long PURSUIT_TICKS = 5 * 60 * 20L;
    private final Map<UUID, Pursuit> pursuits = new HashMap<>();
    private static final class Pursuit {
        private UUID deployment = UUID.randomUUID();
        private String dimension = "";
        private long until;
        private long started;
        private long retryAt;
        private int deployed;
        private boolean spawnSoundPlayed;
        private final java.util.List<UUID> officers = new ArrayList<>();
    }

    public static MaxTacResponse get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_maxtac");
    }

    public static void onNpcKilled(AbstractCityNpcEntity npc, DamageSource source) {
        if (!(npc.level() instanceof ServerLevel level) || npc instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity) return;
        var responsible = source.getEntity() instanceof ServerPlayer player ? player
                : source.getEntity() instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner ? owner
                : npc.getKillCredit() instanceof ServerPlayer credited ? credited : null;
        if (responsible == null) return;
        var data = get(level);
        boolean newPursuit = !data.pursuits.containsKey(responsible.getUUID());
        var pursuit = data.pursuits.computeIfAbsent(responsible.getUUID(), ignored -> new Pursuit());
        if (newPursuit) pursuit.started = level.getServer().overworld().getGameTime();
        pursuit.deployed = pursuit.officers.size();
        pursuit.until = level.getServer().overworld().getGameTime() + PURSUIT_TICKS;
        data.setDirty();
        data.deploy(responsible, pursuit);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(responsible, "combat", newPursuit ? "maxtac_dispatched" : "maxtac_extended");
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(responsible, Component.translatable("message.cyberneticenhancements.maxtac.dispatched").withStyle(net.minecraft.ChatFormatting.RED));
    }

    public boolean active(UUID offender, UUID deployment) {
        var pursuit = pursuits.get(offender);
        return pursuit != null && pursuit.deployment.equals(deployment);
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        var data = get(event.getServer().overworld());
        for (var entry : new ArrayList<>(data.pursuits.entrySet())) {
            var player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            var pursuit = entry.getValue();
            if (event.getServer().overworld().getGameTime() >= pursuit.until || (player != null && !player.isAlive())) {
                if (player != null) de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "combat", !player.isAlive() ? "maxtac_player_dead" : "maxtac_expired",
                        Math.max(0, (event.getServer().overworld().getGameTime() - pursuit.started) / 20));
                data.dismiss(event.getServer(), pursuit);
                data.pursuits.remove(entry.getKey());
                data.setDirty();
            } else if (player == null) {
                if (pursuit.deployed > 0) data.suspend(event.getServer(), pursuit);
            } else {
                if (!pursuit.dimension.isEmpty() && !pursuit.dimension.equals(player.level().dimension().location().toString()))
                    data.suspend(event.getServer(), pursuit);
                if (pursuit.deployed < SQUAD_SIZE && event.getServer().overworld().getGameTime() >= pursuit.retryAt) data.deploy(player, pursuit);
            }
        }
    }

    private void deploy(ServerPlayer player, Pursuit pursuit) {
        var level = player.serverLevel();
        pursuit.dimension = level.dimension().location().toString();
        pursuit.retryAt = level.getServer().overworld().getGameTime() + 100;
        int pathChecks = 0;
        for (int attempts = 0; attempts < 96 && pathChecks < 16 && pursuit.deployed < SQUAD_SIZE; attempts++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int distance = 18 + level.random.nextInt(11);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            for (int y = Math.min(level.getMaxBuildHeight() - 2, player.getBlockY() + 4); y >= Math.max(level.getMinBuildHeight() + 1, player.getBlockY() - 10); y--) {
                var pos = new BlockPos(x, y, z);
                if (!level.getWorldBorder().isWithinBounds(pos) || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.LOGS)
                        || !level.getEntitiesOfClass(MaxTacEntity.class, new AABB(pos).inflate(3)).isEmpty()) continue;
                var officer = ModEntityTypes.MAX_TAC.get().create(level);
                if (officer == null) break;
                officer.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
                if (!level.noCollision(officer)) { officer.discard(); continue; }
                officer.setOnGround(true);
                pathChecks++;
                var path = reachablePath(officer, player);
                if (path == null) { officer.discard(); break; }
                officer.deploy(player, pursuit.deployment, pursuit.deployed, pos);
                if (level.addFreshEntity(officer)) {
                    if (!pursuit.spawnSoundPlayed) {
                        player.playNotifySound(ModSounds.MAXTEC_SPAWN.get(), net.minecraft.sounds.SoundSource.HOSTILE, 0.10F, 1.0F);
                        pursuit.spawnSoundPlayed = true;
                    }
                    officer.getNavigation().moveTo(path, 1.1);
                    pursuit.officers.add(officer.getUUID());
                    pursuit.deployed++;
                    setDirty();
                }
                break;
            }
        }
    }

    private void dismiss(net.minecraft.server.MinecraftServer server, Pursuit pursuit) {
        for (var level : server.getAllLevels()) for (var id : pursuit.officers)
            if (level.getEntity(id) instanceof MaxTacEntity officer) de.artemis.cyberneticenhancements.common.entity.HolographicDeployment.dismiss(officer);
    }

    static net.minecraft.world.level.pathfinder.Path reachablePath(MaxTacEntity officer, ServerPlayer player) {
        var path = officer.getNavigation().createPath(player, 1);
        return path != null && path.canReach() ? path : null;
    }

    private void suspend(net.minecraft.server.MinecraftServer server, Pursuit pursuit) {
        dismiss(server, pursuit);
        pursuit.deployment = UUID.randomUUID();
        pursuit.officers.clear();
        pursuit.deployed = 0;
        pursuit.spawnSoundPlayed = false;
        pursuit.retryAt = 0;
        pursuit.dimension = "";
        setDirty();
    }

    public static void onLogout(ServerPlayer player) {
        var data = get(player.serverLevel());
        var pursuit = data.pursuits.get(player.getUUID());
        if (pursuit != null) data.suspend(player.server, pursuit);
    }

    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (get(player.serverLevel()).pursuits.containsKey(player.getUUID()))
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "combat", "maxtac_player_dead");
            clear(player);
        }
    }

    public static void officerKilled(ServerLevel level, UUID offender, UUID officer) {
        var data = get(level);
        var pursuit = data.pursuits.get(offender);
        if (pursuit == null) return;
        pursuit.officers.remove(officer);
        if (pursuit.officers.isEmpty() && pursuit.deployed >= SQUAD_SIZE) {
            var player = level.getServer().getPlayerList().getPlayer(offender);
            if (player != null) {
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "combat", "maxtac_defeated",
                        Math.max(0, (level.getServer().overworld().getGameTime() - pursuit.started) / 20));
                de.artemis.cyberneticenhancements.common.streetcred.StreetCred.gain(player, de.artemis.cyberneticenhancements.common.streetcred.StreetCredRewards.MAX_TAC_SQUAD);
            }
            data.pursuits.remove(offender);
        }
        data.setDirty();
    }

    public static void clear(ServerPlayer player) {
        var data = get(player.serverLevel());
        var pursuit = data.pursuits.remove(player.getUUID());
        if (pursuit != null) { data.dismiss(player.server, pursuit); data.setDirty(); }
    }

    static MaxTacResponse load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new MaxTacResponse();
        for (var value : tag.getList("Pursuits", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (!entry.hasUUID("Offender") || !entry.hasUUID("Deployment")) continue;
            var pursuit = new Pursuit();
            pursuit.deployment = entry.getUUID("Deployment");
            pursuit.dimension = entry.getString("Dimension");
            pursuit.until = entry.getLong("Until");
            pursuit.started = entry.contains("Started") ? entry.getLong("Started") : pursuit.until - PURSUIT_TICKS;
            pursuit.deployed = Math.clamp(entry.getInt("Deployed"), 0, SQUAD_SIZE);
            pursuit.spawnSoundPlayed = entry.contains("SpawnSoundPlayed") ? entry.getBoolean("SpawnSoundPlayed") : pursuit.deployed > 0;
            for (var officer : entry.getList("Officers", Tag.TAG_COMPOUND)) {
                var member = (CompoundTag) officer;
                if (member.hasUUID("Id")) pursuit.officers.add(member.getUUID("Id"));
            }
            data.pursuits.put(entry.getUUID("Offender"), pursuit);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        pursuits.forEach((offender, pursuit) -> {
            var entry = new CompoundTag();
            entry.putUUID("Offender", offender);
            entry.putUUID("Deployment", pursuit.deployment);
            entry.putString("Dimension", pursuit.dimension);
            entry.putLong("Until", pursuit.until);
            entry.putLong("Started", pursuit.started);
            entry.putInt("Deployed", pursuit.deployed);
            entry.putBoolean("SpawnSoundPlayed", pursuit.spawnSoundPlayed);
            var officers = new ListTag();
            pursuit.officers.forEach(id -> { var member = new CompoundTag(); member.putUUID("Id", id); officers.add(member); });
            entry.put("Officers", officers);
            list.add(entry);
        });
        tag.put("Pursuits", list);
        return tag;
    }
}
