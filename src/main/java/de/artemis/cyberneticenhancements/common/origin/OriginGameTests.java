package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.dialogue.NpcService;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.skills.Skill;
import de.artemis.cyberneticenhancements.common.skills.Skills;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.tarot.TarotRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OriginGameTests {
    @GameTest(template = "sandevistan_test", batch = "origins", timeoutTicks = 60)
    public static void aLifepathIsChosenOnceAndGrantsItsBonuses(GameTestHelper h) {
        var p = player(h, "lifepath-once");
        try {
            h.assertTrue(Origins.of(p) == null, "A fresh player already has a lifepath");
            h.assertTrue(Origins.questPay(p, QuestType.DELIVERY, 200) == 200 && Origins.trustGain(p, 4) == 4, "No lifepath must change nothing");
            double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            h.assertTrue(Origins.choose(p, Origin.NOMAD), "The first choice was refused");
            h.assertTrue(!Origins.choose(p, Origin.CORPO) && Origins.of(p) == Origin.NOMAD, "A second choice replaced the first");
            h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed, "Nomad did not make the player faster");
            h.assertTrue(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) >= 4, "Nomad did not extend safe falls");
            Origins.set(p, Origin.CORPO);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-9, "Switching lifepath kept the old bonuses");
            h.assertTrue(TarotRewards.chromeHeadroom(p) == 3 && p.getAttributeValue(Attributes.ARMOR) >= 1, "Corpo did not grant headroom and armor");
            Origins.set(p, null);
            h.assertTrue(TarotRewards.chromeHeadroom(p) == 0 && p.getAttributeValue(Attributes.ARMOR) == 0, "Clearing the lifepath kept its bonuses");
        } finally {
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "origins", timeoutTicks = 60)
    public static void originSkillsStackWithPurchasedRanksAndSurviveRespec(GameTestHelper h) {
        var player = player(h, "lifepath-skills");
        try {
            h.assertTrue(Skills.spend(player, Skill.CHARISMA) && Skills.available(player) == 0,
                    "A purchased rank could not be recorded before choosing an origin");
            for (Origin origin : Origin.values()) {
                Origins.set(player, origin);
                int total = 0;
                for (Skill skill : Skill.values()) total += Skills.originRank(player, skill);
                h.assertTrue(total == 10 && Skills.spent(player) == 1 && Skills.available(player) == 0,
                        "Origin ranks are missing or changed the purchased budget");
            }

            h.assertTrue(Skills.rank(player, Skill.CHARISMA) == 5, "Corpo ranks did not stack with the purchased rank");
            int cost = Skills.respecCost(player);
            PlayerEurodollarManager.setBalance(player, cost);
            h.assertTrue(Skills.respec(player, cost) && Skills.available(player) == 1
                            && Skills.rank(player, Skill.CHARISMA) == 4 && !Skills.canRespec(player),
                    "Respec removed origin ranks or refunded them as spendable points");
            Origins.set(player, Origin.NOMAD);
            h.assertTrue(Skills.rank(player, Skill.CHARISMA) == 0 && Skills.rank(player, Skill.FABRICATION) == 4,
                    "Changing origin kept the former lifepath ranks");
            h.succeed();
        } finally {
            player.server.getPlayerList().remove(player);
        }
    }

    @GameTest(template = "sandevistan_test", batch = "origins", timeoutTicks = 60)
    public static void eachLifepathDealsWithMoneyAndTrustItsOwnWay(GameTestHelper h) {
        var p = player(h, "lifepath-deals");
        try {
            Origins.set(p, Origin.CORPO);
            h.assertTrue(Origins.questPay(p, QuestType.ELIMINATION, 200) == 220, "Corpo contracts do not pay 10% more");
            h.assertTrue(Origins.priceMultiplier(p, NpcType.MED_TECH, NpcService.TRAUMA_COVERAGE) == 0.6F, "Corpo coverage is not 40% cheaper");
            h.assertTrue(Origins.priceMultiplier(p, NpcType.MED_TECH, NpcService.MEDICAL_STIMS) == 1.0F, "Corpo discount leaked to other services");
            Origins.set(p, Origin.STREET_KID);
            h.assertTrue(Origins.questPay(p, QuestType.DELIVERY, 200) == 200, "Street kid contracts changed their pay");
            h.assertTrue(Origins.trustGain(p, 4) == 6 && Origins.trustGain(p, 1) == 2 && Origins.trustGain(p, 0) == 0, "Street kid trust gain is wrong");
            h.assertTrue(Origins.discount(p, 10) == 14 && Origins.discount(p, 25) == 35 && Origins.discount(p, 0) == 0, "Street kid discount is wrong");
            Origins.set(p, Origin.NOMAD);
            h.assertTrue(Origins.questPay(p, QuestType.SALVAGE, 200) == 230 && Origins.questPay(p, QuestType.BREACH, 200) == 200, "Nomad road pay is wrong");
            h.assertTrue(Origins.priceMultiplier(p, NpcType.TECHIE, null) == 0.85F && Origins.priceMultiplier(p, NpcType.FIXER, null) == 1.0F, "Nomad workshop prices are wrong");
            Origins.set(p, null);
        } finally {
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "origins", timeoutTicks = 60)
    public static void aLifepathSurvivesDeathAndTheChoiceIsNotReopened(GameTestHelper h) {
        var original = player(h, "lifepath-clone");
        var clone = player(h, "lifepath-clone-2");
        try {
            Origins.set(original, Origin.STREET_KID);
            Origins.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone, original, true));
            h.assertTrue(Origins.of(clone) == Origin.STREET_KID, "The lifepath was lost on respawn");
            h.assertTrue(clone.getAttributeValue(Attributes.LUCK) >= .5, "The respawned player lost the lifepath bonuses");
        } finally {
            original.server.getPlayerList().remove(original);
            clone.server.getPlayerList().remove(clone);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "origins", timeoutTicks = 100)
    public static void theRosterStaysSortedAndPagedWithThousandsOfPlayers(GameTestHelper h) {
        var registry = new OriginRegistry();
        int players = 5000;
        var ids = new UUID[players];
        for (int i = 0; i < players; i++) {
            ids[i] = new UUID(0, i);
            registry.record(ids[i], "Player" + String.format("%05d", players - i), Origin.values()[i % 3]);
        }
        h.assertTrue(registry.count(Origin.NOMAD) == 1667 && registry.count(Origin.STREET_KID) == 1667 && registry.count(Origin.CORPO) == 1666, "The roster counts are wrong");
        var first = registry.page(Origin.NOMAD, 0);
        h.assertTrue(first.size() == OriginRegistry.PAGE && first.equals(first.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()), "A roster page is not alphabetical");
        h.assertTrue(registry.page(Origin.NOMAD, 9999).size() == 1667 % OriginRegistry.PAGE, "A page past the end is not the last page");
        registry.record(ids[0], "Player00000", Origin.CORPO);
        h.assertTrue(registry.count(Origin.NOMAD) == 1666 && registry.count(Origin.CORPO) == 1667 && registry.page(Origin.CORPO, 0).getFirst().equals("Player00000"),
                "Changing a lifepath did not move the player");
        registry.record(ids[0], "Player00000", null);
        h.assertTrue(registry.count(Origin.CORPO) == 1666, "Clearing a lifepath kept the player in the roster");
        var saved = registry.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        var loaded = OriginRegistry.load(saved, h.getLevel().registryAccess());
        h.assertTrue(registry.page(Origin.STREET_KID, 3).equals(loaded.page(Origin.STREET_KID, 3)) && loaded.count(Origin.CORPO) == 1666, "The roster changed after saving and loading");
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        p.server.getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        return p;
    }
}
