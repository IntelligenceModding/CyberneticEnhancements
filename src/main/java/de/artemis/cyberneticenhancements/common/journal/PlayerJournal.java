package de.artemis.cyberneticenhancements.common.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class PlayerJournal extends SavedData {
    public static final List<String> CATEGORIES = List.of("world", "contacts", "combat", "commerce", "quests", "cyberware", "workshop", "calls", "banking");
    public static final int PAGE_SIZE = 40;
    private static final Factory<PlayerJournal> FACTORY = new Factory<>(PlayerJournal::new, PlayerJournal::load, null);
    private final List<Entry> entries = new ArrayList<>();
    private long sequence;
    private static final Map<String, String> ENGLISH = english();

    private static Map<String, String> english() {
        try (var stream = PlayerJournal.class.getResourceAsStream("/assets/cyberneticenhancements/lang/en_us.json")) {
            if (stream == null) return Map.of();
            var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new HashMap<>(); json.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString())); return Map.copyOf(result);
        } catch (java.io.IOException | RuntimeException exception) { return Map.of(); }
    }
    private static String searchable(Component component) {
        StringBuilder result = new StringBuilder(component.getString());
        if (component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents content) {
            result.append(' ').append(ENGLISH.getOrDefault(content.getKey(), content.getKey()));
            for (Object arg : content.getArgs()) result.append(' ').append(arg instanceof Component text ? searchable(text) : arg);
        }
        for (var sibling : component.getSiblings()) result.append(' ').append(searchable(sibling));
        return result.toString();
    }

    public record Entry(long id, long timestamp, long gameTime, long dayTime, String category, Component message, String dimension, BlockPos pos) {}
    public record Page(int page, int total, List<Entry> entries) {}

    private static Component eventText(String event, Object[] args) {
        Object[] safe = Arrays.stream(args).map(value -> value instanceof Component || value instanceof Number || value instanceof Boolean || value instanceof String
                ? value : value == null ? "" : value.toString()).toArray();
        return Component.translatable("journal.cyberneticenhancements.event." + event, safe);
    }

    public static PlayerJournal get(ServerPlayer player) {
        return get(player.server, player.getUUID());
    }
    public static PlayerJournal get(net.minecraft.server.MinecraftServer server, UUID player) { return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_journal_" + player); }

    public static void recordFor(net.minecraft.server.MinecraftServer server, UUID player, String dimension, BlockPos pos, String category, String event, Object... args) {
        var data = get(server, player);
        data.entries.add(new Entry(++data.sequence, System.currentTimeMillis(), server.overworld().getGameTime(), server.overworld().getDayTime(), category,
                eventText(event, args), dimension, pos.immutable()));
        data.setDirty();
    }

    public static void record(net.minecraft.world.entity.player.Player player, String category, String event, Object... arguments) {
        if (player instanceof ServerPlayer server) recordAt(server, server.blockPosition(), category, event, arguments);
    }

    public static void message(ServerPlayer player, String category, Component message) { append(player, player.blockPosition(), category, message.copy()); }

    public static void recordAt(ServerPlayer player, BlockPos pos, String category, String event, Object... arguments) {
        append(player, pos, category, eventText(event, arguments));
    }

    private static void append(ServerPlayer player, BlockPos pos, String category, Component message) {
        var data = get(player);
        data.entries.add(new Entry(++data.sequence, System.currentTimeMillis(), player.server.overworld().getGameTime(), player.level().getDayTime(),
                category, message, player.level().dimension().location().toString(), pos.immutable()));
        data.setDirty();
    }

    public Page page(int requested, String category, String query, boolean newest) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        var filtered = entries.stream().filter(entry -> category.isEmpty() || entry.category.equals(category))
                .filter(entry -> needle.isEmpty() || (searchable(entry.message) + " " + entry.category + " " + entry.dimension
                        + " " + entry.pos.toShortString()).toLowerCase(Locale.ROOT).contains(needle)).toList();
        int page = Math.clamp(requested, 0, Math.max(0, (filtered.size() - 1) / PAGE_SIZE));
        List<Entry> result = new ArrayList<>();
        for (int index = page * PAGE_SIZE; index < Math.min(filtered.size(), (page + 1) * PAGE_SIZE); index++)
            result.add(filtered.get(newest ? filtered.size() - 1 - index : index));
        return new Page(page, filtered.size(), List.copyOf(result));
    }

    public static PlayerJournal load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new PlayerJournal();
        data.sequence = tag.getLong("Sequence");
        for (var value : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            Component message;
            try { message = Component.Serializer.fromJson(entry.getString("Message"), registries); }
            catch (RuntimeException exception) { message = Component.literal(entry.getString("Message")); }
            if (message == null) message = Component.empty();
            data.entries.add(new Entry(entry.getLong("Id"), entry.getLong("Timestamp"), entry.getLong("GameTime"), entry.getLong("DayTime"),
                    entry.getString("Category"), message, entry.getString("Dimension"), BlockPos.of(entry.getLong("Pos"))));
            data.sequence = Math.max(data.sequence, entry.getLong("Id"));
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("Sequence", sequence);
        var list = new ListTag();
        for (var entry : entries) {
            var value = new CompoundTag();
            value.putLong("Id", entry.id); value.putLong("Timestamp", entry.timestamp); value.putLong("GameTime", entry.gameTime); value.putLong("DayTime", entry.dayTime);
            value.putString("Category", entry.category); value.putString("Message", Component.Serializer.toJson(entry.message, registries));
            value.putString("Dimension", entry.dimension); value.putLong("Pos", entry.pos.asLong()); list.add(value);
        }
        tag.put("Entries", list);
        return tag;
    }
}
