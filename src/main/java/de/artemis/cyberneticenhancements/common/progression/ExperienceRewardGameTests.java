package de.artemis.cyberneticenhancements.common.progression;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.fabrication.FabricationContent;
import de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.world.MaxTacResponse;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExperienceRewardGameTests {
    @GameTest(template = "sandevistan_test", batch = "experience_rewards", timeoutTicks = 60)
    public static void combatUsesVanillaXpRulesAndSchematicsOnlyRewardNewKnowledge(GameTestHelper h) {
        var level = h.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "experience-test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 14, 3)));
        player.setPos(pos.add(0, 0, 5));
        var region = new AABB(pos, pos).inflate(3);
        var mobLoot = level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        boolean previous = mobLoot.get();
        var npc = ModEntityTypes.FIXER.get().create(level);
        var officer = ModEntityTypes.MAX_TAC.get().create(level);
        var psycho = ModEntityTypes.CYBERPSYCHO.get().create(level);
        var environmental = ModEntityTypes.CYBERPSYCHO.get().create(level);
        var noLoot = ModEntityTypes.MAX_TAC.get().create(level);
        try {
            mobLoot.set(true, player.server);
            var saved = new CompoundTag();
            psycho.saveWithoutId(saved);
            saved.putInt("PsychoTier", 3);
            psycho.load(saved);
            for (var entity : java.util.List.of(npc, officer, psycho, environmental, noLoot)) {
                entity.setPos(pos);
                entity.setNoAi(true);
                level.addFreshEntity(entity);
            }
            LivingEntity[] enemies = {npc, officer, psycho};
            int[] rewards = {2, 25, 100};
            for (int index = 0; index < enemies.length; index++) {
                enemies[index].hurt(level.damageSources().playerAttack(player), 100000);
                h.assertTrue(orbValue(h, region) == rewards[index], "Wrong combat XP reward for " + enemies[index].getType());
                level.getEntitiesOfClass(ExperienceOrb.class, region).forEach(net.minecraft.world.entity.Entity::discard);
                enemies[index].discard();
            }
            environmental.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            h.assertTrue(orbValue(h, region) == 0, "An uncredited environmental death dropped XP");
            mobLoot.set(false, player.server);
            noLoot.hurt(level.damageSources().playerAttack(player), 100000);
            h.assertTrue(orbValue(h, region) == 0, "Combat XP ignored doMobLoot");
            String design = FabricationContent.SCHEMATICS.keySet().iterator().next();
            int experience = player.totalExperience;
            h.assertTrue(SchematicKnowledge.learn(player, design) && player.totalExperience == experience + 10, "First schematic discovery did not award XP");
            h.assertTrue(!SchematicKnowledge.learn(player, design) && player.totalExperience == experience + 10, "Repeated schematic awarded XP twice");
        } finally {
            mobLoot.set(previous, player.server);
            MaxTacResponse.clear(player);
            for (var entity : java.util.List.of(npc, officer, psycho, environmental, noLoot)) entity.discard();
            level.getEntitiesOfClass(ExperienceOrb.class, region).forEach(net.minecraft.world.entity.Entity::discard);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static int orbValue(GameTestHelper h, AABB region) {
        return h.getLevel().getEntitiesOfClass(ExperienceOrb.class, region).stream().mapToInt(orb -> {
            var tag = new CompoundTag();
            orb.saveWithoutId(tag);
            return orb.getValue() * tag.getInt("Count");
        }).sum();
    }
}
