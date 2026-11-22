package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestRewardDeliveries extends SavedData {
    private static final Factory<QuestRewardDeliveries> FACTORY = new Factory<>(QuestRewardDeliveries::new, QuestRewardDeliveries::load, null);
    private final Map<UUID, GlobalPos> points = new HashMap<>();
    private final List<Cargo> cargos = new ArrayList<>();
    private final java.util.Set<UUID> setupPlaced = new java.util.HashSet<>();
    private final java.util.Set<UUID> setupItemsGiven = new java.util.HashSet<>();

    public static QuestRewardDeliveries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_quest_deliveries");
    }

    public boolean bind(UUID owner, ResourceKey<Level> dimension, BlockPos pos) {
        if (setupPlaced.add(owner)) setDirty();
        var point = GlobalPos.of(dimension, pos.immutable());
        if (point.equals(points.put(owner, point))) return false;
        setDirty();
        return true;
    }

    public boolean hasPlacedDropoff(UUID owner) { return setupPlaced.contains(owner); }

    public boolean setupItemGiven(UUID owner) { return setupItemsGiven.contains(owner); }

    public boolean claimSetupItem(ServerPlayer player) {
        UUID owner = player.getUUID();
        if (hasPlacedDropoff(owner) || setupItemGiven(owner)) return true;
        var item = de.artemis.cyberneticenhancements.common.registry.ModBlocks.DROPOFF_POINT.asItem();
        if (player.getInventory().countItem(item) == 0) {
            if (player.getInventory().getFreeSlot() < 0) return false;
            if (!player.addItem(new ItemStack(item))) return false;
        }
        setupItemsGiven.add(owner);
        setDirty();
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    public void unbind(UUID owner, ResourceKey<Level> dimension, BlockPos pos) {
        if (points.remove(owner, GlobalPos.of(dimension, pos))) setDirty();
    }

    public void dispatch(ServerPlayer player, String questId, List<ItemStack> rewards) {
        if (rewards.stream().allMatch(ItemStack::isEmpty)) return;
        queue(player.getUUID(), questId, player.server.overworld().getGameTime() + 6000 + player.getRandom().nextInt(12001), rewards);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "cargo_dispatched");
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable(points.containsKey(player.getUUID())
                ? "message.cyberneticenhancements.dropoff.dispatched" : "message.cyberneticenhancements.dropoff.needs_barrel").withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    void queue(UUID owner, long due, List<ItemStack> rewards) {
        queue(owner, "", due, rewards);
    }

    void queue(UUID owner, String questId, long due, List<ItemStack> rewards) {
        var items = rewards.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (items.isEmpty()) return;
        cargos.add(new Cargo(owner, questId, due, items));
        setDirty();
    }

    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now % 20 == 0) get(server).deliver(server, now);
    }

    void deliver(MinecraftServer server, long now) {
        deliver(server, now, null);
    }

    private void deliver(MinecraftServer server, long now, UUID onlyOwner) {
        var notified = new java.util.HashSet<UUID>();
        var completed = new java.util.HashSet<UUID>();
        var iterator = cargos.iterator();
        while (iterator.hasNext()) {
            var cargo = iterator.next();
            if (onlyOwner != null && !onlyOwner.equals(cargo.owner)) continue;
            if (cargo.due > now) continue;
            var point = points.get(cargo.owner);
            if (point == null) continue;
            var level = server.getLevel(point.dimension());
            if (level == null || !level.hasChunkAt(point.pos())) continue;
            if (!(level.getBlockEntity(point.pos()) instanceof DropoffPointBlockEntity barrel) || !cargo.owner.equals(barrel.owner())) continue;
            boolean delivered = false;
            for (var stack : cargo.items) {
                int before = stack.getCount();
                insert(barrel, stack);
                if (before != stack.getCount()) { setDirty(); delivered = true; }
            }
            if (delivered) {
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordFor(server, cargo.owner, point.dimension().location().toString(), point.pos(), "quests", "cargo_delivered");
                barrel.markDeliveryArrived();
                var player = server.getPlayerList().getPlayer(cargo.owner);
                if (player != null && notified.add(cargo.owner)) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new de.artemis.cyberneticenhancements.common.network.DropoffDeliveryNoticePayload());
                    player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 1.1F);
                }
            }
            cargo.items.removeIf(ItemStack::isEmpty);
            if (!cargo.items.isEmpty()) continue;
            iterator.remove();
            completed.add(cargo.owner);
            setDirty();
        }
        for (var owner : completed) {
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player));
        }
    }

    public GlobalPos registeredPoint(UUID owner) { return points.get(owner); }

    public int pendingCargos(UUID owner) { return (int) cargos.stream().filter(cargo -> cargo.owner.equals(owner)).count(); }

    public java.util.Set<String> pendingContractIds(UUID owner) {
        return cargos.stream().filter(cargo -> cargo.owner.equals(owner) && !cargo.questId.isBlank())
                .map(Cargo::questId).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public List<List<ItemStack>> unlinkedPendingRewards(UUID owner) {
        return cargos.stream().filter(cargo -> cargo.owner.equals(owner) && cargo.questId.isBlank())
                .map(cargo -> cargo.items.stream().map(ItemStack::copy).toList()).toList();
    }

    public long nextDeliveryIn(UUID owner, long now) {
        return cargos.stream().filter(cargo -> cargo.owner.equals(owner)).mapToLong(cargo -> Math.max(0, cargo.due - now)).min().orElse(-1);
    }

    public void queueDebugDelivery(ServerPlayer player, int seconds) {
        queue(player.getUUID(), player.server.overworld().getGameTime() + seconds * 20L, List.of(new ItemStack(net.minecraft.world.item.Items.EMERALD, 3)));
    }

    public void deliverNow(ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        for (int index = 0; index < cargos.size(); index++) {
            var cargo = cargos.get(index);
            if (cargo.owner.equals(player.getUUID())) cargos.set(index, new Cargo(cargo.owner, cargo.questId, now, cargo.items));
        }
        setDirty();
        deliver(player.server, now, player.getUUID());
    }

    public void resetSetup(UUID owner) {
        points.remove(owner);
        setupPlaced.remove(owner);
        setupItemsGiven.remove(owner);
        setDirty();
    }

    private static void insert(Container container, ItemStack remaining) {
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                var existing = container.getItem(slot);
                if (pass == 0 && (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining))) continue;
                if (pass == 1 && !existing.isEmpty()) continue;
                int limit = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
                int moved = Math.min(remaining.getCount(), limit - existing.getCount());
                if (moved <= 0) continue;
                if (existing.isEmpty()) container.setItem(slot, remaining.copyWithCount(moved));
                else existing.grow(moved);
                remaining.shrink(moved);
                container.setChanged();
            }
        }
    }

    static QuestRewardDeliveries load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new QuestRewardDeliveries();
        for (var entry : tag.getList("Points", Tag.TAG_COMPOUND)) {
            var point = (CompoundTag) entry;
            var id = ResourceLocation.tryParse(point.getString("Dimension"));
            if (point.hasUUID("Owner") && id != null) result.points.put(point.getUUID("Owner"), GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), BlockPos.of(point.getLong("Pos"))));
        }
        result.setupPlaced.addAll(result.points.keySet());
        for (var entry : tag.getList("SetupOwners", Tag.TAG_COMPOUND)) {
            var owner = (CompoundTag) entry;
            if (!owner.hasUUID("Owner")) continue;
            if (owner.getBoolean("Placed")) result.setupPlaced.add(owner.getUUID("Owner"));
            if (owner.getBoolean("Given")) result.setupItemsGiven.add(owner.getUUID("Owner"));
        }
        for (var entry : tag.getList("Cargos", Tag.TAG_COMPOUND)) {
            var cargo = (CompoundTag) entry;
            if (!cargo.hasUUID("Owner")) continue;
            var items = new ArrayList<ItemStack>();
            for (var item : cargo.getList("Items", Tag.TAG_COMPOUND)) {
                var stack = ItemStack.parseOptional(registries, (CompoundTag) item);
                if (!stack.isEmpty()) items.add(stack);
            }
            if (!items.isEmpty()) result.cargos.add(new Cargo(cargo.getUUID("Owner"), cargo.getString("QuestId"), cargo.getLong("Due"), items));
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var pointTags = new ListTag();
        points.forEach((owner, point) -> {
            var entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            entry.putString("Dimension", point.dimension().location().toString());
            entry.putLong("Pos", point.pos().asLong());
            pointTags.add(entry);
        });
        tag.put("Points", pointTags);
        var setupTags = new ListTag();
        var owners = new java.util.HashSet<>(setupPlaced);
        owners.addAll(setupItemsGiven);
        for (var owner : owners) {
            var entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            entry.putBoolean("Placed", setupPlaced.contains(owner));
            entry.putBoolean("Given", setupItemsGiven.contains(owner));
            setupTags.add(entry);
        }
        tag.put("SetupOwners", setupTags);
        var cargoTags = new ListTag();
        for (var cargo : cargos) {
            var entry = new CompoundTag();
            entry.putUUID("Owner", cargo.owner);
            entry.putString("QuestId", cargo.questId);
            entry.putLong("Due", cargo.due);
            var items = new ListTag();
            for (var stack : cargo.items) if (!stack.isEmpty()) items.add(stack.save(registries));
            entry.put("Items", items);
            cargoTags.add(entry);
        }
        tag.put("Cargos", cargoTags);
        return tag;
    }

    private record Cargo(UUID owner, String questId, long due, List<ItemStack> items) {}
}
