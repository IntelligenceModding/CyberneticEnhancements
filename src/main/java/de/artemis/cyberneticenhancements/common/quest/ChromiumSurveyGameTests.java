package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.*;
import de.artemis.cyberneticenhancements.common.world.ChromiumGeology;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChromiumSurveyGameTests {
    @GameTest(template = "sandevistan_test", batch = "chromium_survey", timeoutTicks = 80)
    public static void techieSurveyFindsSeededOreWithoutCreatingAQuestCache(GameTestHelper h) {
        survey(h, false);
    }

    @GameTest(template = "sandevistan_test", batch = "chromium_dev_survey", timeoutTicks = 80)
    public static void developerSurveyCanBeAcceptedThroughTheMenuAndCompleted(GameTestHelper h) {
        survey(h, true);
    }

    private static void survey(GameTestHelper h, boolean dev) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "chrome-survey"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean hasPermissions(int permissionLevel) { return permissionLevel <= (dev ? 2 : 0); }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc = dev
                ? ModEntityTypes.DEV_TEST.get().create(h.getLevel()) : ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        npc.setNoAi(true); h.getLevel().addFreshEntity(npc); npc.ensureIdentity(); npc.markMet(player);
        player.setPos(npc.position());
        try {
            var board = PlayerQuestManager.snapshotForIssuer(player, npc);
            var offer = board.offers().stream().filter(QuestContract::chromiumSurvey).findFirst().orElseThrow();
            var source = offer.investigation().source();
            h.assertTrue(source.equals(ChromiumGeology.deposit(h.getLevel().getSeed(), source.getX() >> 4, source.getZ() >> 4)), "Survey points outside seeded world generation");
            var before = h.getLevel().getBlockState(source);
            if (dev) {
                var menu = new de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu(1, player.getInventory(), npc);
                player.containerMenu = menu;
                h.assertTrue(menu.offerSlots() == PlayerQuestManager.DEV_OFFER_COUNT && menu.acceptOfferOption(8) == 25, "Dev menu omitted its ninth contract slot");
                menu.handleChoice(player, menu.acceptOfferOption(board.offers().indexOf(offer)));
                h.assertTrue(player.containerMenu == player.inventoryMenu, "Dev survey acceptance did not close the dialogue");
            } else h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, npc, board.offers().indexOf(offer)) == PlayerQuestManager.Result.OK, "Could not accept geological survey");
            var active = PlayerQuestManager.activeContracts(player).getFirst();
            var survey = active.investigation();
            h.assertTrue(survey.record(source.offset(70, 100, 0), 1)
                    && survey.record(source.offset(0, 100, 70), 1)
                    && survey.record(source.offset(-70, 100, -70), 1), "Valid geological readings were rejected");
            active.setProgress(survey.count());
            var root = player.getPersistentData().getCompound("cyberneticenhancements.quest_state");
            root.getCompound("IssuerBoards").getCompound(npc.getUUID().toString()).put("Active", active.toTag());
            for (int i = 0; i < 20; i++) {
                player.tickCount++;
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            var ready = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(ready.isReady() && ready.targetPos().equals(source), "Three readings did not complete the survey");
            h.assertTrue(h.getLevel().getBlockState(source).equals(before), "Survey created ore or a quest cache");
            h.assertTrue(ready.rewardItemStack().is(ModItems.CHROMIUM_ALLOY.get()) && ready.rewardItemStack().getCount() == 4, "Survey lost its alloy reward");
            h.assertTrue(QuestContract.fromTag(ready.toTag()).chromiumSurvey(), "Save lost the geological survey subtype");
            h.assertTrue(de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.navigationTargets(player).isEmpty(),
                    "Survey granted its permanent marker before turn-in");
            var payout = dev ? PlayerQuestManager.turnInIssuerActive(player, npc) : PlayerQuestManager.turnInRemotely(player, ready.id());
            h.assertTrue(payout == PlayerQuestManager.TurnInResult.OK, "Survey reward could not be claimed: " + payout);
            var markers = de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.navigationTargets(player);
            h.assertTrue(markers.size() == 1 && markers.getFirst().pos().equals(source), "Survey payout did not award a permanent vein marker");
            h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty() && QuestNavigation.capture(player).targets().contains(markers.getFirst()),
                    "Vein marker disappeared when the contract completed");
            var clone = new ServerPlayer(player.server, h.getLevel(), player.getGameProfile(), cookie.clientInformation());
            de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.copyState(player, clone);
            h.assertTrue(de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.navigationTargets(clone).equals(markers), "Reward marker did not survive respawning");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.NOT_READY, "Completed survey could pay out again");
            h.assertTrue(de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.deleteMarker(player, markers.getFirst().questId()), "Reward marker could not be deleted");
            h.assertTrue(de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.rewardSurvey(player, source)
                    && !de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.rewardSurvey(player, source), "Explicit reward did not restore a deleted marker exactly once");
        } finally {
            PlayerQuestManager.clearAllQuestData(player); npc.discard();
            player.server.getPlayerList().remove(player); channel.finishAndReleaseAll();
        }
        h.succeed();
    }
}
