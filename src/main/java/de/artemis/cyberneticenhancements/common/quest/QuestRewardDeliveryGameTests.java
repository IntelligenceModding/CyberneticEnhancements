package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestRewardDeliveryGameTests {
    @GameTest(template = "sandevistan_test", batch = "dropoff_debug", timeoutTicks = 100)
    public static void dropoffCommandsRequireOperatorAndOnlyAffectTheSelectedPlayer(GameTestHelper h) {
        var owner = player(h, "dropoff-debug");
        var other = player(h, "dropoff-other");
        try {
            var barrel = barrel(h, owner, new BlockPos(2, 2, 2));
            var dispatcher = owner.server.getCommands().getDispatcher();
            var source = owner.createCommandSourceStack().withPermission(2);
            var node = dispatcher.getRoot().getChild("cyberneticenhancements").getChild("dev").getChild("dropoff");
            h.assertTrue(node.canUse(source) && !node.canUse(source.withPermission(0)), "Normal players can access dropoff debug commands");
            String prefix = "cyberneticenhancements dev dropoff ";
            var deliveries = QuestRewardDeliveries.get(owner.server);
            dispatcher.execute(prefix + "queue 600", source);
            dispatcher.execute(prefix + "queue 600 dropoff-other", source);
            h.assertTrue(deliveries.pendingParcels(owner.getUUID()) == 1 && deliveries.pendingParcels(other.getUUID()) == 1, "Queue command targeted the wrong player");
            dispatcher.execute(prefix + "deliver", source);
            h.assertTrue(barrel.hasUnreadDelivery() && count(barrel, new ItemStack(Items.EMERALD)) == 3
                    && deliveries.pendingParcels(owner.getUUID()) == 0 && deliveries.nextDeliveryIn(other.getUUID(), owner.server.overworld().getGameTime()) == 12000,
                    "Immediate delivery did not use the normal pipeline or changed another player's due time");
            dispatcher.execute(prefix + "read", source);
            h.assertTrue(!barrel.hasUnreadDelivery() && count(barrel, new ItemStack(Items.EMERALD)) == 3, "Read command changed stored rewards");
            dispatcher.execute(prefix + "test", source);
            dispatcher.execute(prefix + "reset", source);
            h.assertTrue(!deliveries.hasPlacedDropoff(owner.getUUID()) && deliveries.registeredPoint(owner.getUUID()) == null
                    && deliveries.pendingParcels(owner.getUUID()) == 1 && count(barrel, new ItemStack(Items.EMERALD)) == 3, "Setup reset lost pending or stored items");
            dispatcher.execute(prefix + "give", source);
            dispatcher.execute(prefix + "give", source);
            h.assertTrue(owner.getInventory().countItem(ModBlocks.DROPOFF_POINT.asItem()) == 1, "Give command bypassed normal starter claim tracking");
            h.assertTrue(dispatcher.execute(prefix + "status", source) == 1, "Status command failed");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new AssertionError("Dropoff debug commands did not parse", exception);
        } finally {
            owner.server.getPlayerList().remove(owner);
            other.server.getPlayerList().remove(other);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_dropoff_setup", timeoutTicks = 100)
    public static void questTabRequiresPlacementAndTheStarterBarrelCannotBeClaimedTwice(GameTestHelper h) {
        var player = player(h, "dropoff-setup");
        var other = player(h, "dropoff-locked");
        try {
            var deliveries = QuestRewardDeliveries.get(player.server);
            var locked = de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player);
            h.assertTrue(!locked.dropoffPlaced() && !locked.setupItemGiven() && locked.quests().isEmpty(), "Quest tab exposed content before dropoff setup");
            for (int refresh = 0; refresh < 20; refresh++) de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player);
            h.assertTrue(player.getInventory().isEmpty() && !deliveries.setupItemGiven(player.getUUID()), "Opening or refreshing the archive granted a barrel without pressing the claim button");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            h.assertTrue(!deliveries.claimSetupItem(player), "A full inventory was acknowledged as a successful claim");
            h.assertTrue(!deliveries.setupItemGiven(player.getUUID()), "Full creative inventory silently consumed the free barrel");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            for (int request = 0; request < 20; request++) h.assertTrue(deliveries.claimSetupItem(player), "A valid or repeated claim was not acknowledged");
            h.assertTrue(player.getInventory().countItem(ModBlocks.DROPOFF_POINT.asItem()) == 1 && deliveries.setupItemGiven(player.getUUID())
                    && !deliveries.hasPlacedDropoff(player.getUUID()), "Repeated setup requests gave extra barrels or unlocked before placement");
            player.getInventory().clearContent();
            var restored = QuestRewardDeliveries.load(deliveries.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            restored.claimSetupItem(player);
            h.assertTrue(player.getInventory().isEmpty(), "Saving and reloading allowed another starter barrel");
            h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
            h.setBlock(new BlockPos(2, 2, 2), ModBlocks.DROPOFF_POINT.get());
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            var state = h.getLevel().getBlockState(pos);
            ModBlocks.DROPOFF_POINT.get().setPlacedBy(h.getLevel(), pos, state, player, new ItemStack(ModBlocks.DROPOFF_POINT.get()));
            var unlocked = de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player);
            h.assertTrue(unlocked.dropoffPlaced() && !de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(other).dropoffPlaced(), "Placing a barrel did not unlock only its owner's Quest tab");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.STREAM_CODEC.encode(buffer, unlocked);
                h.assertTrue(unlocked.equals(de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.STREAM_CODEC.decode(buffer)), "Quest setup state was lost during synchronization");
            } finally { buffer.release(); }
            h.getLevel().removeBlock(pos, false);
            restored = QuestRewardDeliveries.load(deliveries.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(restored.hasPlacedDropoff(player.getUUID()), "Moving a barrel or restarting erased completed setup");
        } finally {
            player.server.getPlayerList().remove(player);
            other.server.getPlayerList().remove(other);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_reward_delivery", timeoutTicks = 100)
    public static void remoteTurnInsQueueEveryItemPayOnceAndDirectTurnInsGiveItemsImmediately(GameTestHelper h) {
        var player = player(h, "reward-owner");
        var other = player(h, "reward-other");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setNoAi(true);
        npc.setPos(player.position().add(0, 0, 2));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(player);
        try {
            var remote = quest(npc.getUUID(), npc.getName().getString(), QuestType.INVESTIGATION);
            seed(player, remote);
            var reward = remote.rewardItemStack();
            int balance = PlayerEurodollarManager.balance(player);
            h.assertTrue(PlayerQuestManager.turnInRemotely(other, remote.id()) == PlayerQuestManager.TurnInResult.NOT_READY, "Another player could turn in this contract");
            h.assertTrue(PlayerQuestManager.turnInRemotely(player, remote.id()) == PlayerQuestManager.TurnInResult.OK, "Ready remote contract could not be turned in");
            h.assertTrue(player.getInventory().isEmpty() && PlayerEurodollarManager.balance(player) == balance + remote.rewardMoney(), "Remote rewards were not separated into immediate money and delayed items");
            var deliveries = QuestRewardDeliveries.get(player.server);
            h.assertTrue(deliveries.pendingContractIds(player.getUUID()).contains(remote.id())
                    && !deliveries.pendingContractIds(other.getUUID()).contains(remote.id()), "Pending contract was not linked privately to its reward parcel");
            h.assertTrue(PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.questId().equals(remote.id()) && entry.statusId().equals("pending_delivery")),
                    "Remote turn-in did not enter the pending delivery category");
            var saved = deliveries.save(new CompoundTag(), h.getLevel().registryAccess());
            long due = -1;
            int queuedRewards = 0;
            for (var entry : saved.getList("Parcels", Tag.TAG_COMPOUND)) {
                var parcel = (CompoundTag) entry;
                if (!player.getUUID().equals(parcel.getUUID("Owner"))) continue;
                h.assertTrue(parcel.getString("QuestId").equals(remote.id()), "Saving a queued reward lost its contract link");
                due = parcel.getLong("Due");
                for (var item : parcel.getList("Items", Tag.TAG_COMPOUND)) {
                    var stack = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) item);
                    if (ItemStack.isSameItemSameComponents(stack, reward)) queuedRewards += stack.getCount();
                }
            }
            long now = player.server.overworld().getGameTime();
            h.assertTrue(due >= now + 6000 && due <= now + 18000 && queuedRewards == reward.getCount(), "Reward delivery was not queued exactly once within one Minecraft day");
            h.assertTrue(PlayerQuestManager.turnInRemotely(player, remote.id()) == PlayerQuestManager.TurnInResult.NOT_READY
                    && PlayerEurodollarManager.balance(player) == balance + remote.rewardMoney(), "Repeated turn-in paid twice");
            var barrel = barrel(h, player, new BlockPos(2, 2, 2));
            deliveries.deliver(player.server, due - 1);
            h.assertTrue(barrel.isEmpty() && !barrel.hasUnreadDelivery(), "Reward or arrival marker appeared before delivery time");
            deliveries.deliver(player.server, due);
            h.assertTrue(!deliveries.pendingContractIds(player.getUUID()).contains(remote.id())
                    && PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.questId().equals(remote.id()) && entry.statusId().equals("completed")),
                    "Fully delivered contract did not leave pending delivery for completed");
            h.assertTrue(count(barrel, reward) == reward.getCount(), "Due remote reward did not arrive in the owner's barrel");
            h.assertTrue(barrel.hasUnreadDelivery(), "Quest delivery did not activate the holographic arrival marker");
            var savedBarrel = (DropoffPointBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(barrel.getBlockPos(), barrel.getBlockState(),
                    barrel.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(savedBarrel != null && savedBarrel.hasUnreadDelivery(), "Unread delivery marker did not survive saving");
            var update = barrel.getUpdateTag(h.getLevel().registryAccess());
            h.assertTrue(update.getBoolean("UnreadDelivery") && update.getUUID("Owner").equals(player.getUUID()) && !update.contains("Items"), "Marker synchronization was missing or exposed barrel contents");
            player.openMenu(barrel);
            h.assertTrue(!barrel.hasUnreadDelivery(), "Checking the delivery barrel did not dismiss its marker");
            player.closeContainer();
            deliveries.deliver(player.server, due + 1);
            h.assertTrue(count(barrel, reward) == reward.getCount() && !barrel.hasUnreadDelivery(), "Unchanged inventory retriggered the arrival marker or duplicated delivery");
            var direct = quest(npc.getUUID(), npc.getName().getString(), QuestType.INVESTIGATION);
            seed(player, direct);
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.OK
                    && player.getInventory().countItem(reward.getItem()) == reward.getCount(), "In-person turn-in did not give its item reward immediately");
            h.assertTrue(count(barrel, reward) == reward.getCount(), "In-person reward was also sent to the barrel");
            h.assertTrue(PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.questId().equals(direct.id()) && entry.statusId().equals("completed")),
                    "In-person turn-in was incorrectly shown as awaiting delivery");
            var missing = quest(npc.getUUID(), npc.getName().getString(), QuestType.RECOVERY);
            seed(player, missing);
            h.assertTrue(PlayerQuestManager.turnInRemotely(player, missing.id()) != PlayerQuestManager.TurnInResult.OK, "Remote turn-in bypassed missing required materials");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            player.server.getPlayerList().remove(player);
            other.server.getPlayerList().remove(other);
            npc.discard();
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_reward_delivery", timeoutTicks = 100)
    public static void deliveriesSurviveSavingFullMissingAndUnloadedBarrelsAndKeepOwnersSeparate(GameTestHelper h) {
        var owner = player(h, "parcel-owner");
        var other = player(h, "parcel-other");
        try {
            var barrel = barrel(h, owner, new BlockPos(2, 2, 2));
            h.assertTrue(!barrel.bind(other) && !barrel.canOpen(other) && barrel.canOpen(owner), "Dropoff ownership was not enforced");
            var restoredBarrel = (DropoffPointBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(barrel.getBlockPos(), barrel.getBlockState(),
                    barrel.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(restoredBarrel != null && owner.getUUID().equals(restoredBarrel.owner()), "Saving the barrel lost its owner");
            var deliveries = new QuestRewardDeliveries();
            deliveries.bind(owner.getUUID(), h.getLevel().dimension(), barrel.getBlockPos());
            for (int slot = 0; slot < 27; slot++) barrel.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            barrel.setItem(0, new ItemStack(Items.DIAMOND, 63));
            h.assertTrue(!barrel.hasUnreadDelivery(), "Ordinary inventory changes triggered a quest arrival marker");
            deliveries.queue(owner.getUUID(), "partial-contract", 100, List.of(new ItemStack(Items.DIAMOND, 70)));
            deliveries.deliver(owner.server, 99);
            h.assertTrue(barrel.getItem(0).getCount() == 63, "Delivery ignored its due time");
            deliveries.deliver(owner.server, 100);
            h.assertTrue(barrel.getItem(0).getCount() == 64 && barrel.hasUnreadDelivery(), "Partial delivery did not fill available space and mark new items");
            h.assertTrue(deliveries.pendingContractIds(owner.getUUID()).contains("partial-contract"), "Partly delivered contract stopped being pending");
            var legacySave = deliveries.save(new CompoundTag(), h.getLevel().registryAccess());
            for (var entry : legacySave.getList("Parcels", Tag.TAG_COMPOUND)) ((CompoundTag) entry).remove("QuestId");
            var legacy = QuestRewardDeliveries.load(legacySave, h.getLevel().registryAccess());
            h.assertTrue(legacy.pendingContractIds(owner.getUUID()).isEmpty() && legacy.unlinkedPendingRewards(owner.getUUID()).size() == 1
                    && legacy.unlinkedPendingRewards(other.getUUID()).isEmpty(), "Legacy parcels were lost, guessed a contract, or exposed to another player");
            deliveries = QuestRewardDeliveries.load(deliveries.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(deliveries.pendingContractIds(owner.getUUID()).contains("partial-contract"), "Reloading a partial delivery lost its contract link");
            barrel.setItem(1, ItemStack.EMPTY);
            barrel.setItem(2, ItemStack.EMPTY);
            deliveries.deliver(owner.server, 101);
            h.assertTrue(count(barrel, new ItemStack(Items.DIAMOND)) == 133, "Saving a partial delivery lost or duplicated its remainder");
            h.assertTrue(deliveries.pendingContractIds(owner.getUUID()).isEmpty(), "Fully delivered parcel remained pending");
            deliveries.deliver(owner.server, 102);
            h.assertTrue(count(barrel, new ItemStack(Items.DIAMOND)) == 133, "Completed parcel was delivered again");
            deliveries.queue(owner.getUUID(), 0, List.of(new ItemStack(Items.EMERALD, 3)));
            var unloaded = new BlockPos(2000000, 64, 2000000);
            h.assertTrue(!h.getLevel().hasChunkAt(unloaded), "Unloaded fixture was already loaded");
            deliveries.bind(owner.getUUID(), h.getLevel().dimension(), unloaded);
            deliveries.deliver(owner.server, 200);
            h.assertTrue(!h.getLevel().hasChunkAt(unloaded), "Delivery force-loaded a remote chunk");
            deliveries.unbind(owner.getUUID(), h.getLevel().dimension(), unloaded);
            deliveries.deliver(owner.server, 201);
            var replacement = barrel(h, owner, new BlockPos(5, 2, 2));
            deliveries.bind(owner.getUUID(), h.getLevel().dimension(), replacement.getBlockPos());
            deliveries = QuestRewardDeliveries.load(deliveries.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            owner.server.getPlayerList().remove(owner);
            deliveries.deliver(other.server, 202);
            h.assertTrue(count(replacement, new ItemStack(Items.EMERALD)) == 3 && count(barrel, new ItemStack(Items.EMERALD)) == 0,
                    "Offline delivery did not follow the owner's new dropoff point");
        } finally {
            owner.server.getPlayerList().remove(owner);
            other.server.getPlayerList().remove(other);
        }
        h.succeed();
    }

    private static DropoffPointBlockEntity barrel(GameTestHelper h, ServerPlayer player, BlockPos relative) {
        h.setBlock(relative.below(), Blocks.STONE);
        h.setBlock(relative, ModBlocks.DROPOFF_POINT.get());
        var barrel = (DropoffPointBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(relative));
        barrel.bind(player);
        return barrel;
    }

    private static int count(net.minecraft.world.Container container, ItemStack item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, item)) count += stack.getCount();
        }
        return count;
    }

    private static QuestContract quest(UUID issuer, String name, QuestType type) {
        return new QuestContract(UUID.randomUUID().toString(), issuer, name, "techie", type, 1, QuestStatus.READY,
                0, 2, 2, true, "", null, 4, 30, 1, QuestContract.REWARD_MAXDOC, 2, 0, -1);
    }

    private static void seed(ServerPlayer player, QuestContract quest) {
        var board = new CompoundTag();
        board.put("Active", quest.toTag());
        board.put("Offers", new ListTag());
        board.putLong("RefreshDay", player.level().getDayTime() / 24000L);
        var boards = new CompoundTag();
        boards.put(quest.issuerId().toString(), board);
        var root = new CompoundTag();
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        return player;
    }
}
