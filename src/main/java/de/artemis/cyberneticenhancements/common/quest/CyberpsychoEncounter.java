package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CyberpsychoEncounter extends SavedData {
    private static final Factory<CyberpsychoEncounter> FACTORY = new Factory<>(CyberpsychoEncounter::new, CyberpsychoEncounter::load, null);
    private static final String[] FIRST_NAMES = {"Mara", "Niko", "Vera", "Leon", "Iris", "Dante", "Rin", "Jonah", "Sasha", "Kai", "Tessa", "Ren"};
    private static final String[] LAST_NAMES = {"Voss", "Sato", "Reyes", "Ward", "Novak", "Park", "Cross", "Vale", "Kessler", "Ortiz", "Kane", "Mercer"};
    private final Map<String, Death> deaths = new HashMap<>();
    private final Set<String> retired = new HashSet<>();

    public record Death(String dimension, BlockPos pos, UUID shard) {}

    public static CyberpsychoEncounter get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_cyberpsycho_encounters");
    }

    public static String subjectName(String id) {
        var random = new java.util.Random(id.hashCode() * 341873128712L);
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }

    public Death death(String id) { return deaths.get(id); }
    public boolean retired(String id) { return retired.contains(id); }

    static QuestContract repairRepeatedContract(ServerPlayer player, QuestContract contract) {
        if (contract.type() != QuestType.CYBERPSYCHO || contract.status() != QuestStatus.ACTIVE
                || contract.destinationReached() || !get(player.serverLevel()).retired(contract.id())) return contract;
        var tag = contract.toTag();
        tag.putString("Id", UUID.randomUUID().toString());
        tag.putBoolean("PsychoSpawned", false); tag.putBoolean("PsychoShardCollected", false);
        tag.putInt("Progress", 0);
        var repaired = QuestContract.fromTag(tag);
        for (var level : player.server.getAllLevels()) if (level.dimension().location().toString().equals(repaired.dimensionId())) {
            QuestSiteReservations.get(level).reserve(repaired, true, level.getGameTime());
            break;
        }
        return repaired;
    }

    public static boolean trySpawn(ServerPlayer player, QuestContract contract) {
        var level = player.serverLevel();
        var center = contract.targetPos();
        if (contract.psychoSpawned() || contract.destinationReached() || contract.status() != QuestStatus.ACTIVE || center == null
                || !level.dimension().location().toString().equals(contract.dimensionId()) || !player.blockPosition().closerThan(center, 80)
                || get(level).retired(contract.id()) || get(level).death(contract.id()) != null) return false;
        if (level.getEntity(contract.psychoId()) instanceof CyberpsychoEntity) {
            contract.markPsychoSpawned();
            return true;
        }
        boolean dimensional = QuestDimension.of(contract.dimensionId()) != QuestDimension.STANDARD;
        for (int attempt = 0; attempt < 40; attempt++) {
            var column = dimensional && attempt == 0 ? center : center.offset(level.random.nextInt(57) - 28, 0, level.random.nextInt(57) - 28);
            if (!level.hasChunkAt(column)) continue;
            var pos = dimensional ? DimensionalQuestSites.nearbySpawn(level, column)
                    : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (pos == null || !pos.closerThan(center, 44) || !level.getWorldBorder().isWithinBounds(pos)
                    || level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.LOGS)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.getFluidState(pos).isEmpty() || player.distanceToSqr(Vec3.atBottomCenterOf(pos)) < 16 * 16) continue;
            var psycho = ModEntityTypes.CYBERPSYCHO.get().create(level);
            if (psycho == null) return false;
            psycho.setUUID(contract.psychoId());
            psycho.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            psycho.configure(contract, player.getUUID(), center);
            if (!level.noCollision(psycho)) { psycho.discard(); continue; }
            if (level.addFreshEntity(psycho)) {
                contract.markPsychoSpawned();
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, net.minecraft.network.chat.Component.translatable("message.cyberneticenhancements.quest.cyberpsycho.nearby", contract.psychoName())
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                return true;
            }
        }
        return false;
    }

    public static void onDeath(CyberpsychoEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var contract = entity.contract();
        if (contract == null || entity.questOwner() == null) return;
        var data = get(level);
        if (data.retired(contract.id()) || data.deaths.containsKey(contract.id())) return;
        var stack = PlayerQuestManager.createCyberpsychoShard(contract);
        QuestItemVersions.get(level).issue(stack);
        var shardPos = entity.blockPosition();
        Vec3 drop = entity.position().add(0, 0.3, 0);
        if (QuestDimension.of(contract.dimensionId()) != QuestDimension.STANDARD) {
            shardPos = DimensionalQuestSites.dossierSite(level, shardPos, contract.targetPos());
            drop = Vec3.atBottomCenterOf(shardPos).add(0, 0.3, 0);
        }
        var shard = new ItemEntity(level, drop.x, drop.y, drop.z, stack);
        shard.setTarget(entity.questOwner());
        shard.setUnlimitedLifetime();
        shard.setDefaultPickUpDelay();
        level.addFreshEntity(shard);
        data.deaths.put(contract.id(), new Death(level.dimension().location().toString(), shardPos, shard.getUUID()));
        data.setDirty();
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT) && entity.getRandom().nextFloat() < 0.25F) {
            var item = ModItems.cyberware(CyberpsychoLoadout.fromIndex(contract.targetIndex()).cyberwareId()).get();
            var salvage = new ItemStack(item);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(salvage, item.getDefinition(), 55 + entity.getRandom().nextInt(35));
            var loot = entity.spawnAtLocation(salvage);
            if (loot != null) loot.setTarget(entity.questOwner());
        }
    }

    public static void cleanup(ServerPlayer player, QuestContract contract) {
        var data = get(player.serverLevel());
        var death = data.deaths.remove(contract.id());
        data.retired.add(contract.id());
        data.setDirty();
        for (var level : player.server.getAllLevels()) {
            if (level.getEntity(contract.psychoId()) instanceof CyberpsychoEntity entity) entity.discard();
            if (death != null && level.getEntity(death.shard()) instanceof ItemEntity shard) shard.discard();
        }
    }

    static CyberpsychoEncounter load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new CyberpsychoEncounter();
        for (var entry : tag.getList("Deaths", Tag.TAG_COMPOUND)) {
            var death = (CompoundTag) entry;
            if (!death.hasUUID("Shard")) continue;
            data.deaths.put(death.getString("Id"), new Death(death.getString("Dimension"), BlockPos.of(death.getLong("Pos")), death.getUUID("Shard")));
        }
        for (var entry : tag.getList("Retired", Tag.TAG_STRING)) data.retired.add(entry.getAsString());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        deaths.forEach((id, death) -> {
            var entry = new CompoundTag();
            entry.putString("Id", id);
            entry.putString("Dimension", death.dimension());
            entry.putLong("Pos", death.pos().asLong());
            entry.putUUID("Shard", death.shard());
            list.add(entry);
        });
        tag.put("Deaths", list);
        var ended = new ListTag();
        retired.forEach(id -> ended.add(StringTag.valueOf(id)));
        tag.put("Retired", ended);
        return tag;
    }
}
