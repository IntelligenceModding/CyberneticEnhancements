package de.artemis.cyberneticenhancements.common.journal;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Map;
import java.util.WeakHashMap;

public final class JournalEvents {
    private static final Map<ServerPlayer, Long> REQUESTS = new WeakHashMap<>();
    private JournalEvents() {}
    public static void loadoutChanged(net.minecraft.world.entity.player.Player player, net.minecraft.nbt.CompoundTag before, net.minecraft.nbt.CompoundTag after) {
        if (!(player instanceof ServerPlayer server)) return;
        var oldItems = stacks(before, server);
        var newItems = stacks(after, server);
        var slots = new java.util.HashSet<>(oldItems.keySet()); slots.addAll(newItems.keySet());
        for (int slot : slots) {
            var old = oldItems.getOrDefault(slot, net.minecraft.world.item.ItemStack.EMPTY);
            var next = newItems.getOrDefault(slot, net.minecraft.world.item.ItemStack.EMPTY);
            if (!net.minecraft.world.item.ItemStack.isSameItem(old, next)) {
                if (!old.isEmpty()) PlayerJournal.record(server, "cyberware", "removed", old.getHoverName());
                if (!next.isEmpty()) PlayerJournal.record(server, "cyberware", "installed", next.getHoverName());
            } else if (!next.isEmpty()) {
                var oldTag = old.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                var nextTag = next.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                for (String key : java.util.List.of("InstalledModules", "InstalledChipware", "SupportedModuleTiers", "SupportedChipwareTiers"))
                    if (!java.util.Objects.equals(oldTag.get(key), nextTag.get(key))) { PlayerJournal.record(server, "cyberware", "configuration", next.getHoverName()); break; }
            }
        }
        if (!java.util.Objects.equals(before.get("SupportedTiers"), after.get("SupportedTiers")) && !before.isEmpty())
            PlayerJournal.record(server, "cyberware", "capacity");
    }
    private static Map<Integer, net.minecraft.world.item.ItemStack> stacks(net.minecraft.nbt.CompoundTag tag, ServerPlayer player) {
        Map<Integer, net.minecraft.world.item.ItemStack> result = new java.util.HashMap<>();
        for (var value : tag.getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var entry = (net.minecraft.nbt.CompoundTag) value;
            result.put(entry.getInt("Slot"), net.minecraft.world.item.ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Stack")));
        }
        return result;
    }

    public static void ability(net.minecraft.world.entity.player.Player player, String key, Object... args) {
        if (player instanceof ServerPlayer server && !key.contains("cooldown") && !key.contains(".no_") && !key.endsWith(".active"))
            PlayerJournal.message(server, "cyberware", net.minecraft.network.chat.Component.translatable(key, args));
    }

    public static void enterCity(ServerPlayer player, String key, net.minecraft.core.BlockPos pos) {
        String tag = "cyberneticenhancements.journal_city";
        if (key.equals(player.getPersistentData().getString(tag))) return;
        player.getPersistentData().putString(tag, key);
        PlayerJournal.recordAt(player, pos, "world", "city_entered");
    }

    public static void onTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            var structure = player.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", "night_city"));
            var start = structure == null ? null : player.serverLevel().structureManager().getStructureAt(player.blockPosition(), structure);
            if (start == null || !start.isValid()) { player.getPersistentData().remove("cyberneticenhancements.journal_city"); continue; }
            var box = start.getBoundingBox();
            enterCity(player, player.level().dimension().location() + ":" + box.minX() + ":" + box.minZ(), box.getCenter());
        }
    }
    public static void request(ServerPlayer player, JournalRequestPayload request) {
        long now = player.server.overworld().getGameTime();
        if (now < REQUESTS.getOrDefault(player, 0L)) return;
        REQUESTS.put(player, now + 4);
        String category = PlayerJournal.CATEGORIES.contains(request.category()) ? request.category() : "";
        var page = PlayerJournal.get(player).page(request.page(), category, request.query(), request.newest());
        PacketDistributor.sendToPlayer(player, new JournalPayload(request.request(), page.page(), page.total(), page.entries()));
    }

    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player && (event.getEntity() instanceof AbstractCityNpcEntity || event.getEntity() instanceof CyberpsychoEntity))
            PlayerJournal.recordAt(player, event.getEntity().blockPosition(), "combat", "hurt", event.getEntity().getDisplayName(), String.format(java.util.Locale.ROOT, "%.1f", event.getNewDamage()));
        if (event.getEntity() instanceof ServerPlayer player && (event.getSource().getEntity() instanceof AbstractCityNpcEntity || event.getSource().getEntity() instanceof CyberpsychoEntity))
            PlayerJournal.record(player, "combat", "attacked", event.getSource().getEntity().getDisplayName(), String.format(java.util.Locale.ROOT, "%.1f", event.getNewDamage()));
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof AbstractCityNpcEntity || event.getEntity() instanceof CyberpsychoEntity) {
            var credited = event.getSource().getEntity() instanceof ServerPlayer player ? player
                    : event.getSource().getEntity() instanceof net.minecraft.world.entity.TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner ? owner
                    : event.getEntity().getKillCredit() instanceof ServerPlayer player ? player : null;
            if (credited != null) PlayerJournal.recordAt(credited, event.getEntity().blockPosition(), "combat", "killed", event.getEntity().getDisplayName());
        }
    }

    public static void onPlaced(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).getNamespace().equals("cyberneticenhancements"))
            PlayerJournal.recordAt(player, event.getPos(), "workshop", "placed", event.getPlacedBlock().getBlock().getName());
    }
    public static void onBroken(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).getNamespace().equals("cyberneticenhancements"))
            PlayerJournal.recordAt(player, event.getPos(), "workshop", "broken", event.getState().getBlock().getName());
    }
    public static void onCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).getNamespace().equals("cyberneticenhancements"))
            PlayerJournal.record(player, "workshop", "crafted", event.getCrafting().getCount(), event.getCrafting().getHoverName());
    }
}
