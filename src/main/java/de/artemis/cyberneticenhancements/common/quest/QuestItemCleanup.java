package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.compat.Ae2QuestItemCleanup;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.*;

/** Actively removes obsolete revisions, including closed and portable inventories. Never loads distant chunks. */
public final class QuestItemCleanup {
    private static final Map<ServerLevel, Set<LevelChunk>> CHUNKS = new IdentityHashMap<>();
    private static final Set<MinecraftServer> PURGING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ServerLevel, Set<LevelChunk>> PENDING = new IdentityHashMap<>();
    private static final Map<MinecraftServer, Sweep> SWEEPS = new IdentityHashMap<>();
    private record ChunkWork(ServerLevel level, LevelChunk chunk) {}
    private record Sweep(ArrayDeque<ChunkWork> chunks, ArrayDeque<Entity> entities, Set<Object> pass) {}

    private QuestItemCleanup() {}

    public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            level.getServer().execute(() -> {
                CHUNKS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(chunk);
                PENDING.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(chunk);
            });
        }
    }

    public static void unloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) level.getServer().execute(() -> {
            var chunks = CHUNKS.get(level);
            if (chunks != null) chunks.remove(event.getChunk());
            var pending = PENDING.get(level);
            if (pending != null) pending.remove(event.getChunk());
        });
    }

    public static void levelUnloaded(LevelEvent.Unload event) { CHUNKS.remove(event.getLevel()); PENDING.remove(event.getLevel()); }
    public static void stopped(ServerStoppedEvent event) {
        CHUNKS.keySet().removeIf(level -> level.getServer() == event.getServer());
        PURGING.remove(event.getServer());
        PENDING.keySet().removeIf(level -> level.getServer() == event.getServer());
        SWEEPS.remove(event.getServer());
    }

    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            cleanEntity(level, event.getEntity(), newPass());
            if (event.getEntity().isRemoved()) event.setCanceled(true);
        }
    }

    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var pendingPass = newPass();
        for (var level : server.getAllLevels()) {
            var pending = PENDING.remove(level);
            if (pending != null) for (var chunk : pending) cleanChunk(level, chunk, pendingPass);
        }
        // Spread background checks over ticks rather than scanning a large modpack in one frame.
        if (!SWEEPS.containsKey(server) && server.getTickCount() % 200 == 0) {
            var chunks = new ArrayDeque<ChunkWork>();
            var entities = new ArrayDeque<Entity>();
            for (var level : server.getAllLevels()) {
                for (var chunk : CHUNKS.getOrDefault(level, Set.of())) chunks.add(new ChunkWork(level, chunk));
                level.getAllEntities().forEach(entities::add);
            }
            SWEEPS.put(server, new Sweep(chunks, entities, newPass()));
        }
        var sweep = SWEEPS.get(server);
        if (sweep == null) return;
        for (int i = 0; i < 8 && !sweep.chunks().isEmpty(); i++) {
            var work = sweep.chunks().removeFirst();
            if (CHUNKS.getOrDefault(work.level(), Set.of()).contains(work.chunk())) cleanChunk(work.level(), work.chunk(), sweep.pass());
        }
        for (int i = 0; i < 32 && !sweep.entities().isEmpty(); i++) {
            var entity = sweep.entities().removeFirst();
            if (!entity.isRemoved() && entity.level() instanceof ServerLevel level) cleanEntity(level, entity, sweep.pass());
        }
        if (sweep.chunks().isEmpty() && sweep.entities().isEmpty()) SWEEPS.remove(server);
    }

    public static void purge(MinecraftServer server) {
        if (!PURGING.add(server)) return;
        try {
            var pass = newPass();
            for (var level : server.getAllLevels()) {
                for (var chunk : List.copyOf(CHUNKS.getOrDefault(level, Set.of()))) cleanChunk(level, chunk, pass);
                var entities = new ArrayList<Entity>();
                level.getAllEntities().forEach(entities::add);
                for (var entity : entities) cleanEntity(level, entity, pass);
            }
        } finally { PURGING.remove(server); }
    }

    private static Set<Object> newPass() { return Collections.newSetFromMap(new IdentityHashMap<>()); }

    private static void cleanChunk(ServerLevel level, LevelChunk chunk, Set<Object> pass) {
        if (!QuestItemVersions.get(level).hasHistory()) return;
        for (var block : List.copyOf(chunk.getBlockEntities().values())) {
            if (block.isRemoved()) continue;
            boolean changed = block instanceof Container container && cleanContainer(level, container, pass, 0);
            changed |= cleanHandler(level, level.getCapability(Capabilities.ItemHandler.BLOCK, block.getBlockPos(), null), pass, 0);
            for (var side : Direction.values())
                changed |= cleanHandler(level, level.getCapability(Capabilities.ItemHandler.BLOCK, block.getBlockPos(), side), pass, 0);
            if (ModList.get().isLoaded("ae2"))
                Ae2QuestItemCleanup.network(level, block.getBlockPos(), pass, stack -> !QuestItemVersions.get(level).current(stack));
            if (changed) block.setChanged();
        }
    }

    private static void cleanEntity(ServerLevel level, Entity entity, Set<Object> pass) {
        if (!QuestItemVersions.get(level).hasHistory()) return;
        if (entity instanceof ItemEntity item) {
            var stack = item.getItem().copy();
            if (cleanStack(level, stack, pass, 0)) {
                if (stack.isEmpty()) item.discard();
                else item.setItem(stack);
            }
            return;
        }
        boolean changed = entity instanceof Container container && cleanContainer(level, container, pass, 0);
        if (entity instanceof LivingEntity living) {
            for (var slot : EquipmentSlot.values()) {
                var stack = living.getItemBySlot(slot).copy();
                if (cleanStack(level, stack, pass, 0)) { living.setItemSlot(slot, stack); changed = true; }
            }
        }
        changed |= cleanHandler(level, entity.getCapability(Capabilities.ItemHandler.ENTITY), pass, 0);
        if (entity instanceof ServerPlayer player) {
            changed |= cleanContainer(level, player.getInventory(), pass, 0);
            changed |= cleanContainer(level, player.getEnderChestInventory(), pass, 0);
            for (var slot : player.containerMenu.slots) {
                var stack = slot.getItem().copy();
                if (cleanStack(level, stack, pass, 0)) { slot.set(stack); changed = true; }
            }
            var carried = player.containerMenu.getCarried().copy();
            if (cleanStack(level, carried, pass, 0)) { player.containerMenu.setCarried(carried); changed = true; }
            if (player.containerMenu != player.inventoryMenu) {
                var inventoryCarried = player.inventoryMenu.getCarried().copy();
                if (cleanStack(level, inventoryCarried, pass, 0)) { player.inventoryMenu.setCarried(inventoryCarried); changed = true; }
            }
            if (changed) {
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
            }
        }
    }

    private static boolean cleanContainer(ServerLevel level, Container container, Set<Object> pass, int depth) {
        if (!pass.add(container)) return false;
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot).copy();
            if (cleanStack(level, stack, pass, depth)) { container.setItem(slot, stack); changed = true; }
        }
        if (changed) container.setChanged();
        return changed;
    }

    private static boolean cleanHandler(ServerLevel level, IItemHandler handler, Set<Object> pass, int depth) {
        if (handler == null || depth > 16 || !pass.add(handler)) return false;
        boolean changed = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            var original = handler.getStackInSlot(slot);
            var stack = original.copy();
            if (!cleanStack(level, stack, pass, depth)) continue;
            if (handler instanceof IItemHandlerModifiable writable) { writable.setStackInSlot(slot, stack); changed = true; }
            else if (stack.isEmpty()) changed |= !handler.extractItem(slot, original.getCount(), false).isEmpty();
        }
        return changed;
    }

    private static boolean cleanStack(ServerLevel level, ItemStack stack, Set<Object> pass, int depth) {
        if (stack.isEmpty() || depth > 16) return false;
        if (!QuestItemVersions.get(level).current(stack)) { stack.setCount(0); return true; }
        boolean changed = false;
        var contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            var items = contents.stream().map(ItemStack::copy).toList();
            boolean nestedChanged = false;
            for (var item : items) nestedChanged |= cleanStack(level, item, pass, depth + 1);
            if (nestedChanged) { stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items)); changed = true; }
        }
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            var items = bundle.itemCopyStream().toList();
            boolean nestedChanged = false;
            for (var item : items) nestedChanged |= cleanStack(level, item, pass, depth + 1);
            if (nestedChanged) { stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items.stream().filter(item -> !item.isEmpty()).toList())); changed = true; }
        }
        changed |= cleanHandler(level, stack.getCapability(Capabilities.ItemHandler.ITEM), pass, depth + 1);
        if (ModList.get().isLoaded("ae2")) changed |= Ae2QuestItemCleanup.cell(stack, item -> !QuestItemVersions.get(level).current(item));
        return changed;
    }
}
