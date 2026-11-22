package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SalvageGameTests {
    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.tickCount++;
            PlayerQuestManager.onPlayerTick(new PlayerTickEvent.Post(player));
        }
    }

    @GameTest(template = "sandevistan_test", batch = "salvage", timeoutTicks = 100)
    public static void aFoundCargoBelongsToAContactAndPaysOnHandoverButLosingItCostsNothing(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        try {
            npc.markMet(player);
            npc.ensureIdentity();
            player.getInventory().setItem(4, new ItemStack(ModItems.LOST_CARGO.get()));
            tick(player, 40);
            var quests = PlayerQuestManager.activeContracts(player);
            h.assertTrue(quests.size() == 1 && quests.getFirst().type() == QuestType.SALVAGE && quests.getFirst().isReady(),
                    "The found cargo did not become a ready salvage contract: " + quests);
            var quest = quests.getFirst();
            h.assertTrue(quest.issuerId().equals(npc.getUUID()), "The cargo was not matched to the only contact");
            var stack = player.getInventory().getItem(4);
            h.assertTrue(QuestItemVersions.get(player.serverLevel()).current(stack) && !QuestItemVersions.key(stack).isEmpty(), "The cargo was not tagged to its contract");
            int balance = PlayerEurodollarManager.balance(player);
            int trust = npc.trustPoints(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().removeItemNoUpdate(4));
            npc.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance + quest.rewardMoney(), "Handing over the cargo did not pay");
            h.assertTrue(npc.trustPoints(player) > trust, "Handing over the cargo earned no trust");
            h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty() && player.getInventory().countItem(ModItems.LOST_CARGO.get()) == 0, "The cargo stayed after the handover");
        } finally { cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "salvage", timeoutTicks = 100)
    public static void aLostFoundCargoCancelsTheContractWithoutPenalty(GameTestHelper h) {
        var player = player(h);
        var npc = npc(h, player);
        try {
            npc.markMet(player);
            npc.ensureIdentity();
            player.getInventory().setItem(2, new ItemStack(ModItems.LOST_CARGO.get()));
            tick(player, 40);
            h.assertTrue(PlayerQuestManager.activeContracts(player).size() == 1, "No contract for the found cargo");
            int trust = npc.trustPoints(player);
            player.getInventory().clearContent();
            tick(player, 260);
            h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty(), "The lost cargo kept the contract open");
            h.assertTrue(npc.trustPoints(player) == trust, "Losing the cargo cost trust");
            h.assertTrue(PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.statusId().equals("cancelled")), "The lost cargo was not logged as cancelled");
        } finally { cleanup(player, npc); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "salvage", timeoutTicks = 100)
    public static void aCommissionedCargoIsHiddenInTheContainersStashCrate(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(0, 1, 0));
        var piece = new ShippingContainerPiece(level.getServer().getStructureManager(), 3, Rotation.NONE, origin, 5L,
                ShippingContainerPiece.Damage.SPLIT, ShippingContainerPiece.Cargo.METALS, ShippingContainerPiece.Growth.NONE, false, false);
        var box = piece.getBoundingBox().inflatedBy(1);
        level.getChunk(origin);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, new ChunkPos(origin), origin);
        BlockPos cell = piece.stashCell();
        var cargo = new ItemStack(ModItems.LOST_CARGO.get());
        h.assertTrue(SalvageQuests.stash(level, piece, cargo), "The stash could not be prepared");
        h.assertTrue(level.getBlockState(cell).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.CRATE.get()), "The stash cell holds no crate");
        h.assertTrue(level.getBlockState(cell.above()).getCollisionShape(level, cell.above()).isEmpty()
                && level.getBlockState(cell.above(2)).getCollisionShape(level, cell.above(2)).isEmpty(), "Something covers the stash crate's marker");
        var barrel = (Container) level.getBlockEntity(cell);
        int cargos = 0;
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) if (barrel.getItem(slot).is(ModItems.LOST_CARGO.get())) cargos++;
        h.assertTrue(cargos == 1, "Expected exactly one cargo in the stash crate, found " + cargos);
        h.succeed();
    }

    private static AbstractCityNpcEntity npc(GameTestHelper h, ServerPlayer player) {
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(player.position()); npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "salvage"), false);
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
