package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestHandoverGameTests {
    @GameTest(template = "sandevistan_test", batch = "dimensional_recovery_handover", timeoutTicks = 100)
    public static void dimensionalRecoveryUsesLocalMaterialsAndRetainsCertificationForReturn(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        var overworld = player.serverLevel();
        try {
            for (var route : new QuestDimension[]{QuestDimension.NETHER, QuestDimension.END}) {
                var destination = player.server.getLevel(route.levelKey());
                var item = route == QuestDimension.NETHER ? Items.QUARTZ : Items.CHORUS_FRUIT;
                int index = route == QuestDimension.NETHER ? 5 : 6;
                var quest = new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), npc.npcTypeId(),
                        QuestType.RECOVERY, 1, QuestStatus.ACTIVE, index, 4, 0, false, route.levelKey().location().toString(),
                        null, 1, 100, 1, 0, 0, 0, 0);
                seed(player, quest);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 4));
                h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.NOT_READY,
                        "Items in the Overworld qualified before the collection leg");
                player.setServerLevel(destination);
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                player.setServerLevel(overworld);
                h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().isReady(), "Collection required an artificial waypoint");
                h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.OK,
                        "The required items could not be handed in after returning");
                h.assertTrue(player.getInventory().countItem(item) == 0, "The wrong material was consumed");
            }
        } finally {
            player.setServerLevel(overworld);
            cleanup(player, npc);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_handover", timeoutTicks = 100)
    public static void heldItemsCompleteAllPhysicalHandoverTypesWithoutOpeningDialogue(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        try {
            for (QuestType type : new QuestType[]{QuestType.RECOVERY, QuestType.EXTRACTION, QuestType.CYBERPSYCHO}) {
                player.getInventory().clearContent();
                var quest = quest(npc, type);
                seed(player, quest);
                player.setItemInHand(InteractionHand.MAIN_HAND, type == QuestType.RECOVERY
                        ? new ItemStack(Items.IRON_INGOT, 7) : shard(player, quest));
                int balance = PlayerEurodollarManager.balance(player);
                int xp = player.totalExperience;
                npc.mobInteract(player, InteractionHand.MAIN_HAND);
                h.assertTrue(player.containerMenu == player.inventoryMenu, "Handover opened dialogue: " + type);
                h.assertTrue(PlayerEurodollarManager.balance(player) == balance + quest.rewardMoney()
                        && player.totalExperience == xp + quest.rewardExperience(), "Handover did not pay the normal reward: " + type);
                h.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == (type == QuestType.RECOVERY ? 3 : 0),
                        "Handover consumed the wrong material amount: " + type);
                h.assertTrue(PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.questId().equals(quest.id())
                        && entry.statusId().equals("completed")), "Quest did not reach completed history: " + type);
                h.assertTrue(!PlayerQuestManager.tryHandInHeldItem(player, npc, player.getMainHandItem())
                        && PlayerEurodollarManager.balance(player) == balance + quest.rewardMoney(), "Handover paid twice: " + type);
                h.assertTrue(!QuestRewardDeliveries.get(player.server).pendingContractIds(player.getUUID()).contains(quest.id()),
                        "Direct handover incorrectly queued remote delivery");
            }
        } finally { cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_handover", timeoutTicks = 100)
    public static void incompleteMaterialsStayInHandAndACompleteInventoryCanFinishImmediately(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        try {
            seed(player, quest(npc, QuestType.RECOVERY));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 1));
            int balance = PlayerEurodollarManager.balance(player);
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.containerMenu == player.inventoryMenu && player.getMainHandItem().getCount() == 1
                    && PlayerEurodollarManager.balance(player) == balance, "Incomplete handover opened dialogue, consumed goods or paid");
            player.getInventory().setItem(8, new ItemStack(Items.IRON_INGOT, 3));
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 0
                    && PlayerEurodollarManager.balance(player) == balance + 30, "Handover ignored materials elsewhere in inventory");
        } finally { cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_handover", timeoutTicks = 100)
    public static void wrongIssuersWrongContractsAndStaleShardsCannotTurnIn(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        var other = npc(h, player);
        try {
            var quest = quest(npc, QuestType.EXTRACTION);
            seed(player, quest);
            var valid = shard(player, quest);
            h.assertTrue(!PlayerQuestManager.tryHandInHeldItem(player, other, valid), "Wrong issuer accepted handover");
            var wrong = shard(player, quest(npc, QuestType.EXTRACTION));
            h.assertTrue(!PlayerQuestManager.tryHandInHeldItem(player, npc, wrong), "Shard from another contract was accepted");
            var stale = valid.copy();
            QuestItemVersions.get(player.serverLevel()).issue(valid);
            h.assertTrue(!PlayerQuestManager.tryHandInHeldItem(player, npc, stale), "Stale shard revision was accepted");
            h.assertTrue(!PlayerQuestManager.tryHandInHeldItem(player, npc, new ItemStack(Items.AMETHYST_SHARD)), "Untagged shard was accepted");
            player.setItemInHand(InteractionHand.MAIN_HAND, valid);
            int balance = PlayerEurodollarManager.balance(player);
            npc.setCurrentCustomer(UUID.randomUUID());
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().getCount() == 1 && PlayerEurodollarManager.balance(player) == balance,
                    "Handover bypassed the busy NPC guard");
            npc.setCurrentCustomer(null);
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance + quest.rewardMoney(), "Valid replacement shard was rejected");
        } finally { other.discard(); cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_handover", timeoutTicks = 100)
    public static void unrelatedHeldItemsStillOpenNormalDialogueWithoutCompletingTheQuest(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        try {
            seed(player, quest(npc, QuestType.RECOVERY));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 2));
            int balance = PlayerEurodollarManager.balance(player);
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu,
                    "Unrelated held item no longer opens dialogue");
            h.assertTrue(player.getMainHandItem().getCount() == 2 && PlayerEurodollarManager.balance(player) == balance,
                    "Normal dialogue consumed an unrelated item or completed a quest");
        } finally { cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_handover", timeoutTicks = 100)
    public static void fiveContactsCanHaveActiveContractsWhileEachContactStillAllowsOnlyOne(GameTestHelper h) {
        var player = player(h);
        var contacts = new java.util.ArrayList<AbstractCityNpcEntity>();
        try {
            for (int i = 0; i < 5; i++) {
                var contact = npc(h, player);
                contacts.add(contact);
                var offer = quest(contact, QuestType.RECOVERY);
                var nextOffer = quest(contact, QuestType.RECOVERY);
                offer.setStatus(QuestStatus.OFFERED);
                nextOffer.setStatus(QuestStatus.OFFERED);
                var thirdOffer = quest(contact, QuestType.RECOVERY);
                var fourthOffer = quest(contact, QuestType.RECOVERY);
                thirdOffer.setStatus(QuestStatus.OFFERED);
                fourthOffer.setStatus(QuestStatus.OFFERED);
                var root = player.getPersistentData().getCompound("cyberneticenhancements.quest_state");
                var boards = root.getCompound("IssuerBoards");
                var board = new CompoundTag();
                var offers = new ListTag(); offers.add(offer.toTag()); offers.add(nextOffer.toTag()); offers.add(thirdOffer.toTag()); offers.add(fourthOffer.toTag());
                board.put("Offers", offers);
                board.putLong("RefreshDay", player.level().getDayTime() / 24000);
                boards.put(contact.getStringUUID(), board); root.put("IssuerBoards", boards);
                player.getPersistentData().put("cyberneticenhancements.quest_state", root);
                if (i == 4) h.assertTrue(PlayerQuestManager.canReceiveUrgentOffer(player, contact),
                        "Other contacts' active quests blocked an urgent offer");
                var result = i == 4 ? PlayerQuestManager.acceptUrgentIssuerOffer(player, contact, offer.id())
                        : PlayerQuestManager.acceptIssuerOffer(player, contact, 0);
                h.assertTrue(result == PlayerQuestManager.Result.OK, "Global cap blocked contact " + (i + 1));
                h.assertTrue(PlayerQuestManager.activeContracts(player).size() == i + 1, "New contact overwrote another active quest");
                h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, contact, 0) == PlayerQuestManager.Result.OK,
                        "Same contact refused a second active quest");
                h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, contact, 0) == PlayerQuestManager.Result.OK,
                        "Same contact refused a third active quest");
                h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, contact, 0) == PlayerQuestManager.Result.HAS_ACTIVE,
                        "Same contact accepted a fourth active quest");
                h.assertTrue(!PlayerQuestManager.canReceiveUrgentOffer(player, contact), "Urgent offer bypassed the per-contact cap");
                h.assertTrue(PlayerQuestManager.abandonIssuerActive(player, contact, nextOffer.id()) == PlayerQuestManager.Result.OK,
                        "The second quest of a contact could not be dropped on its own");
                h.assertTrue(PlayerQuestManager.abandonIssuerActive(player, contact, thirdOffer.id()) == PlayerQuestManager.Result.OK,
                        "The third quest of a contact could not be dropped on its own");
            }
            h.assertTrue(PlayerQuestManager.activeContracts(player).stream().map(QuestContract::issuerId).distinct().count() == 5,
                    "Per-contact quest state did not stay independent");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            contacts.forEach(AbstractCityNpcEntity::discard);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static QuestContract quest(AbstractCityNpcEntity npc, QuestType type) {
        int count = type == QuestType.RECOVERY ? 4 : 1;
        return new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), "techie", type, 1,
                type == QuestType.CYBERPSYCHO ? QuestStatus.READY : QuestStatus.ACTIVE,
                type == QuestType.RECOVERY ? 1 : 0, count, count, true, "", null, 4, 30, 1,
                QuestContract.REWARD_NONE, 0, 0, -1);
    }

    private static ItemStack shard(ServerPlayer player, QuestContract quest) {
        var stack = quest.type() == QuestType.CYBERPSYCHO ? PlayerQuestManager.createCyberpsychoShard(quest) : new ItemStack(Items.AMETHYST_SHARD);
        if (quest.type() == QuestType.EXTRACTION) {
            var tag = new CompoundTag();
            tag.putString("QuestItemType", "exfil_shard");
            tag.putString("QuestContractId", quest.id());
            tag.putUUID("QuestIssuerId", quest.issuerId());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        QuestItemVersions.get(player.serverLevel()).issue(stack);
        return stack;
    }

    private static void seed(ServerPlayer player, QuestContract quest) {
        var board = new CompoundTag();
        board.put("Active", quest.toTag());
        board.put("Offers", new ListTag());
        board.putLong("RefreshDay", player.level().getDayTime() / 24000);
        var boards = new CompoundTag();
        boards.put(quest.issuerId().toString(), board);
        var root = new CompoundTag();
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static AbstractCityNpcEntity npc(GameTestHelper h, ServerPlayer player) {
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(player.position()); npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "quest-handover"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.getInventory().clearContent();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        return player;
    }

    private static void cleanup(ServerPlayer player, AbstractCityNpcEntity npc) {
        player.closeContainer();
        PlayerQuestManager.clearAllQuestData(player);
        player.server.getPlayerList().remove(player);
        npc.discard();
    }
}
