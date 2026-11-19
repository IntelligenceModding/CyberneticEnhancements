package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload;
import de.artemis.cyberneticenhancements.common.quest.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static de.artemis.cyberneticenhancements.common.dialogue.NpcCallGameTests.*;
import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LostQuestItemGameTests {
    @GameTest(template = "sandevistan_test", batch = "lost_return_idle_call", timeoutTicks = 100)
    public static void idlingInALossCallDoesNotFailTheQuestOrCostTrustOrImmediatelyRedial(GameTestHelper h) {
        var owner = player(h, "idle-evidence");
        var npc = issuer(h, owner.player());
        npc.raiseTrust(owner.player(), 100);
        var quest = callQuest(npc, QuestType.EXTRACTION, QuestStatus.READY);
        quest.markReturnItemCollected();
        seedCallBoard(owner.player(), npc, List.of(), quest);
        int trust = npc.trustPoints(owner.player());
        try {
            ticks(owner.player(), 221);
            var ringing = state(drain(owner));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), ANSWER_INCOMING));
            drain(owner);
            NpcCallManager.expire(owner.player().server, owner.player().server.overworld().getGameTime() + 200);
            var active = PlayerQuestManager.activeContracts(owner.player()).getFirst();
            h.assertTrue(active.itemLossPending() && active.itemLossCallDeferred() && npc.trustPoints(owner.player()) == trust,
                    "Idle loss-call hang-up failed the quest or charged trust");
            h.assertTrue(QuestContract.fromTag(active.toTag()).itemLossCallDeferred(), "Idle deferral did not survive serialization");
            ticks(owner.player(), 60);
            h.assertTrue(!NpcCallManager.hasCall(owner.player().getUUID()), "NPC immediately called back after the idle hang-up");
            h.assertTrue(PlayerQuestManager.confirmLostReturnItem(owner.player(), npc, quest.id()), "Player could not resolve the loss after an idle hang-up");
        } finally { cleanup(owner, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "lost_return_item", timeoutTicks = 100)
    public static void externalStorageReplyResumesTheQuestWithoutReplacingOrInvalidatingItsItem(GameTestHelper h) {
        var owner = player(h, "stored-evidence");
        var npc = issuer(h, owner.player());
        var quest = callQuest(npc, QuestType.CYBERPSYCHO, QuestStatus.READY);
        quest.markReturnItemCollected();
        seedCallBoard(owner.player(), npc, List.of(), quest);
        var original = PlayerQuestManager.createCyberpsychoShard(quest);
        owner.player().getInventory().add(original.copy());
        ticks(owner.player(), 1);
        owner.player().getInventory().clearContent();
        drain(owner);
        try {
            ticks(owner.player(), 200);
            h.assertTrue(!PlayerQuestManager.activeContracts(owner.player()).getFirst().itemLossPending(), "Loss call started before ten full seconds missing");
            ticks(owner.player(), 21);
            var ringing = state(drain(owner));
            h.assertTrue(ringing.incoming(), "Missing return item did not trigger an incoming call");
            h.assertTrue(owner.player().getInventory().countItem(Items.AMETHYST_SHARD) == 0, "Lost evidence was automatically replaced");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), ANSWER_INCOMING));
            var answered = state(drain(owner));
            h.assertTrue(answered.choices().stream().anyMatch(choice -> choice.action() == CONFIRM_ITEM_KEPT), "Angry call omitted the external-storage reply");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), CONFIRM_ITEM_KEPT));
            drain(owner);
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), END));
            ticks(owner.player(), 500);
            var resumed = PlayerQuestManager.activeContracts(owner.player()).getFirst();
            h.assertTrue(resumed.externalItemConfirmed() && !resumed.itemLossPending() && !NpcCallManager.hasCall(owner.player().getUUID()),
                    "External storage caused repeated angry calls");
            h.assertTrue(QuestContract.fromTag(resumed.toTag()).externalItemConfirmed(), "External-storage reply did not survive serialization");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(owner.player(), npc) == PlayerQuestManager.TurnInResult.MISSING_ITEMS,
                    "Claiming external storage paid out without the real item");
            owner.player().getInventory().add(original.copy());
            ticks(owner.player(), 1);
            h.assertTrue(!PlayerQuestManager.activeContracts(owner.player()).getFirst().externalItemConfirmed(), "Retrieving the item did not reset loss tracking");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(owner.player(), npc) == PlayerQuestManager.TurnInResult.OK,
                    "Original externally stored item could not complete its quest");
        } finally { cleanup(owner, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "lost_return_declined", timeoutTicks = 100)
    public static void decliningTheLossCallFailsTheQuestAndCostsTrust(GameTestHelper h) { failure(h, false); }

    @GameTest(template = "sandevistan_test", batch = "lost_return_ignored", timeoutTicks = 100)
    public static void ignoringTheLossCallFailsTheQuestAndCostsTrust(GameTestHelper h) { failure(h, true); }

    @GameTest(template = "sandevistan_test", batch = "lost_return_issuer_dead", timeoutTicks = 100)
    public static void issuerDeathDuringALossCallCancelsWithoutChargingThePlayerForTheLoss(GameTestHelper h) {
        var owner = player(h, "evidence-issuer-dead");
        var npc = issuer(h, owner.player());
        npc.raiseTrust(owner.player(), 100);
        var quest = callQuest(npc, QuestType.EXTRACTION, QuestStatus.READY);
        quest.markReturnItemCollected();
        seedCallBoard(owner.player(), npc, List.of(), quest);
        int trust = npc.trustPoints(owner.player());
        try {
            ticks(owner.player(), 221);
            h.assertTrue(NpcCallManager.hasCall(owner.player().getUUID()), "Loss call was not pending");
            de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(h.getLevel()).markDeceased(npc.getUUID());
            NpcCallManager.interruptForNpc(npc, true);
            ticks(owner.player(), 1);
            h.assertTrue(npc.trustPoints(owner.player()) == trust, "Issuer death incorrectly applied a lost-item penalty");
            h.assertTrue(PlayerQuestManager.questLog(owner.player()).stream()
                    .anyMatch(entry -> entry.questId().equals(quest.id()) && entry.statusId().equals("cancelled")),
                    "Issuer death did not use normal contract cancellation");
        } finally { cleanup(owner, npc); }
        h.succeed();
    }

    private static void failure(GameTestHelper h, boolean ignored) {
        var owner = player(h, "lost-evidence");
        var npc = issuer(h, owner.player());
        npc.raiseTrust(owner.player(), 100);
        var quest = callQuest(npc, QuestType.EXTRACTION, QuestStatus.READY);
        quest.markReturnItemCollected();
        seedCallBoard(owner.player(), npc, List.of(), quest);
        drain(owner);
        int trust = npc.trustPoints(owner.player());
        try {
            ticks(owner.player(), 221);
            var ringing = state(drain(owner));
            if (ignored) NpcCallManager.expire(owner.player().server, owner.player().server.overworld().getGameTime() + 601);
            else NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), END));
            h.assertTrue(PlayerQuestManager.activeContracts(owner.player()).isEmpty(), "Lost return item left its contract active");
            h.assertTrue(PlayerQuestManager.questLog(owner.player()).stream().anyMatch(entry -> entry.questId().equals(quest.id()) && entry.statusId().equals("failed")),
                    "Lost-item failure was not recorded as a failed quest");
            h.assertTrue(npc.trustPoints(owner.player()) < trust, "Lost-item failure did not cost trust");
            h.assertTrue(owner.player().getInventory().isEmpty(), "Failed evidence was replaced");
        } finally { cleanup(owner, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "courier_replacement_call", timeoutTicks = 100)
    public static void replacementRequiresCallingAndHalvesTheActualCompletionRewards(GameTestHelper h) {
        var owner = player(h, "lost-parcel");
        var player = owner.player();
        var npc = issuer(h, player);
        var target = h.absolutePos(new BlockPos(6, 2, 6));
        h.getLevel().setBlockAndUpdate(target.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(target.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var offer = new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), npc.npcTypeId(),
                QuestType.DELIVERY, 1, QuestStatus.OFFERED, 0, 1, 0, false, h.getLevel().dimension().location().toString(),
                target, 1, 100, 4, QuestContract.REWARD_MAXDOC, 4, 0, 0);
        offer.setStatus(QuestStatus.ACTIVE);
        seedCallBoard(player, npc, List.of(), offer);
        try {
            var old = new ItemStack(ModItems.COURIER_PACKAGE.get());
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("QuestContractId", offer.id());
            tag.putString("QuestItemType", "courier_package");
            old.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            player.getInventory().add(old.copy());
            ticks(player, 1);
            player.getInventory().clearContent();
            ticks(player, 221);
            h.assertTrue(player.getInventory().countItem(ModItems.COURIER_PACKAGE.get()) == 0, "Delivery parcel was automatically restored");
            h.assertTrue(!NpcCallManager.hasCall(player.getUUID()), "Missing outbound parcel incorrectly triggered an angry call");
            var token = UUID.randomUUID();
            drain(owner);
            NpcCallManager.handle(player, new NpcCallRequestPayload(token, npc.getUUID(), START));
            drain(owner);
            NpcCallManager.answerPending(player.server, player.server.overworld().getGameTime() + 200);
            var connected = state(drain(owner));
            h.assertTrue(connected.choices().stream().anyMatch(choice -> choice.action() == REQUEST_REPLACEMENT), "Call menu omitted the replacement request");
            NpcCallManager.handle(player, new NpcCallRequestPayload(token, npc.getUUID(), REQUEST_REPLACEMENT));
            var active = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(active.rewardMoney() == 50 && active.rewardTrust() == 2 && active.rewardItemCount() == 2
                    && active.rewardExperience() == offer.rewardExperience() / 2, "Replacement did not halve all numeric rewards");
            h.assertTrue(QuestContract.fromTag(active.toTag()).replacementPenalty(), "Replacement penalty did not survive serialization");
            h.assertTrue(!PlayerQuestManager.requestDeliveryReplacement(player, npc), "Another replacement was issued while a parcel was present");
            player.getInventory().add(old);
            ticks(player, 1);
            h.assertTrue(player.getInventory().countItem(ModItems.COURIER_PACKAGE.get()) == 1, "Old parcel remained valid after replacement");
            var replacement = parcel(player).copy();
            player.getInventory().clearContent();
            ((net.minecraft.world.Container) h.getLevel().getBlockEntity(target)).setItem(0, replacement);
            ticks(player, 1);
            int balance = de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player);
            int trust = npc.trustPoints(player), xp = player.totalExperience;
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.OK, "Replacement parcel could not finish delivery");
            h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == balance + 50
                    && npc.trustPoints(player) == trust + 2 && player.totalExperience == xp + offer.rewardExperience() / 2,
                    "Actual payout ignored the replacement penalty");
            h.assertTrue(player.getInventory().countItem(ModItems.consumable("maxdoc_mk2").get()) == 2, "Supply payout ignored the replacement penalty");
        } finally { cleanup(owner, npc); }
        h.succeed();
    }

    private static ItemStack parcel(ServerPlayer player) {
        return player.getInventory().items.stream().filter(stack -> stack.is(ModItems.COURIER_PACKAGE.get())).findFirst().orElseThrow();
    }

    private static AbstractCityNpcEntity issuer(GameTestHelper h, ServerPlayer player) {
        var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(player.position().add(0, 0, 4));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        return npc;
    }

    private static void ticks(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            player.tickCount++;
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
    }

    private static void cleanup(CallPlayer owner, AbstractCityNpcEntity npc) {
        NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner.player()));
        PlayerQuestManager.clearAllQuestData(owner.player());
        npc.discard();
        owner.player().server.getPlayerList().remove(owner.player());
    }
}
