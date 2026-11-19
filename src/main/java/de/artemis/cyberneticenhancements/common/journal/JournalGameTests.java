package de.artemis.cyberneticenhancements.common.journal;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JournalGameTests {
    @GameTest(template = "sandevistan_test", batch = "journal_storage", timeoutTicks = 100)
    public static void completeHistorySurvivesReloadAndRespawnWithStablePagingFilteringAndSearch(GameTestHelper h) {
        var owner = player(h, "journal-owner");
        try {
            String needle = UUID.randomUUID().toString();
            for (int index = 0; index < 185; index++) PlayerJournal.record(owner.player, index % 2 == 0 ? "world" : "quests", "talked", needle + "-" + index);
            var journal = PlayerJournal.get(owner.player);
            var first = journal.page(0, "", needle, false);
            h.assertTrue(first.total() == 185 && first.entries().size() == 40, "Journal lost history or sent too much in a page");
            long firstId = first.entries().getFirst().id();
            var last = journal.page(Integer.MAX_VALUE, "", needle, false);
            h.assertTrue(last.page() == 4 && last.entries().size() == 25 && last.entries().getLast().id() == firstId + 184, "History could not be read to its final page");
            var newest = journal.page(0, "", needle, true);
            h.assertTrue(newest.entries().getFirst().id() == firstId + 184, "Newest-first order is incorrect");
            h.assertTrue(journal.page(0, "quests", needle, true).total() == 92, "Category filtering counted the wrong entries");
            PlayerJournal.record(owner.player, "commerce", "purchased", 2, "Implant", "Seller", 100);
            PlayerJournal.record(owner.player, "world", "cache_cleared", net.minecraft.core.Direction.NORTH);
            h.assertTrue(journal.page(0, "world", "NORTH", true).total() == 1, "Enum metadata was not converted into serializable event text");
            h.assertTrue(journal.page(0, "commerce", "Bought", true).total() == 1, "Dedicated-server search cannot find readable event text");
            var loaded = PlayerJournal.load(journal.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(loaded.page(0, "", needle, false).equals(first), "Reload changed event IDs, timestamps, text or location");
            var clone = new ServerPlayer(owner.player.server, h.getLevel(), owner.player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            h.assertTrue(PlayerJournal.get(clone) == journal, "Respawn did not retain the same UUID-owned history");
            PlayerJournal.recordFor(owner.player.server, owner.player.getUUID(), "minecraft:overworld", new BlockPos(10, 70, 20), "workshop", "fabricated", 1, "Offline item");
            h.assertTrue(journal.page(0, "workshop", "Offline item", true).total() == 1, "Offline machine completion was lost");
        } finally { owner.close(); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "journal_access", timeoutTicks = 100)
    public static void journalRequestsOnlyReturnTheSendersHistoryAndPacketsRoundTrip(GameTestHelper h) {
        var owner = player(h, "journal-reader");
        var observer = player(h, "journal-other");
        try {
            PlayerJournal.record(owner.player, "contacts", "talked", "Private owner contact");
            PlayerJournal.record(observer.player, "contacts", "talked", "Other player secret");
            owner.drain(); observer.drain();
            var request = new JournalRequestPayload(7, -10, "contacts", "", true);
            JournalEvents.request(owner.player, request);
            var payload = owner.drain().stream().filter(JournalPayload.class::isInstance).map(JournalPayload.class::cast).findFirst().orElseThrow();
            h.assertTrue(payload.request() == 7 && payload.page() == 0 && payload.entries().size() == 1
                    && payload.entries().getFirst().message().getString().contains("Private owner contact"), "Journal query returned another player's data");
            h.assertTrue(observer.drain().isEmpty(), "Journal history was broadcast to another player");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                JournalPayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(payload.equals(JournalPayload.STREAM_CODEC.decode(buffer)), "Journal page did not preserve metadata or components");
                JournalRequestPayload.STREAM_CODEC.encode(buffer, request);
                h.assertTrue(request.equals(JournalRequestPayload.STREAM_CODEC.decode(buffer)), "Journal request did not round-trip");
            } finally { buffer.release(); }
        } finally { owner.close(); observer.close(); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "journal_gameplay", timeoutTicks = 100)
    public static void realNpcInteractionPurchaseDamageAndMurderProduceAnOrderedStory(GameTestHelper h) {
        var owner = player(h, "journal-story");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setPos(owner.player.position().add(2, 0, 0)); npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc); npc.ensureIdentity(); npc.markMet(owner.player);
        try {
            de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.setBalance(owner.player, 1000);
            JournalEvents.enterCity(owner.player, "story-city", owner.player.blockPosition());
            JournalEvents.enterCity(owner.player, "story-city", owner.player.blockPosition());
            npc.mobInteract(owner.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            var menu = (de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu) owner.player.containerMenu;
            menu.purchaseShopOffer(owner.player, 0);
            owner.player.closeContainer();
            npc.hurt(h.getLevel().damageSources().playerAttack(owner.player), Float.MAX_VALUE);
            var events = PlayerJournal.get(owner.player).page(0, "", "", false).entries();
            var keys = events.stream().map(entry -> entry.message().getContents()).filter(TranslatableContents.class::isInstance)
                    .map(TranslatableContents.class::cast).map(TranslatableContents::getKey).toList();
            int city = keys.indexOf("journal.cyberneticenhancements.event.city_entered"), talk = keys.indexOf("journal.cyberneticenhancements.event.talked"),
                    bought = keys.indexOf("journal.cyberneticenhancements.event.purchased"), hurt = keys.indexOf("journal.cyberneticenhancements.event.hurt"),
                    killed = keys.indexOf("journal.cyberneticenhancements.event.killed"), dispatched = keys.indexOf("journal.cyberneticenhancements.event.maxtac_dispatched");
            h.assertTrue(city >= 0 && talk > city && bought > talk && hurt > bought && killed > hurt && dispatched > killed, "NPC story is incomplete or out of causal order: " + keys);
            h.assertTrue(keys.stream().filter(key -> key.endsWith(".city_entered")).count() == 1, "Standing inside a city spammed arrival events");
            h.assertTrue(events.stream().filter(entry -> entry.category().equals("combat")).allMatch(entry -> entry.dimension().equals("minecraft:overworld")), "Combat events lost their dimension");
        } finally {
            de.artemis.cyberneticenhancements.common.world.MaxTacResponse.clear(owner.player);
            npc.discard(); owner.close();
        }
        h.succeed();
    }

    private static Actor player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return new Actor(player, channel);
    }

    private record Actor(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) implements AutoCloseable {
        List<Object> drain() {
            channel.runPendingTasks(); List<Object> payloads = new ArrayList<>(); Object next;
            while ((next = channel.readOutbound()) != null) if (next instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet) payloads.add(packet.payload());
            return payloads;
        }
        public void close() { player.server.getPlayerList().remove(player); }
    }
}
