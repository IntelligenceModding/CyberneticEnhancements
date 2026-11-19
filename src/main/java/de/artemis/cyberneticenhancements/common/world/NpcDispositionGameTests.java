package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.MaxTacEntity;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashMap;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcDispositionGameTests {
    @GameTest(template = "sandevistan_test", batch = "npc_disposition", timeoutTicks = 80)
    public static void angerIsPrivateAndOnlyCriticalNpcsDefendThemselves(GameTestHelper h) {
        var attacker = player(h, "anger-attacker");
        var bystander = player(h, "anger-bystander");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(attacker.position().add(0, 0, 2));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(attacker);
        npc.markMet(bystander);
        try {
            npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 1);
            h.assertTrue(NpcDisposition.color(attacker, npc.getUUID()).equals("yellow")
                    && NpcDisposition.color(bystander, npc.getUUID()).equals("aqua"), "Anger was not private to the attacker");
            h.assertTrue(npc.getTarget() == null, "Healthy NPC retaliated");
            var contact = ArchiveContactsPayload.capture(attacker).contacts().stream().filter(c -> c.npcId().equals(npc.getUUID().toString())).findFirst().orElseThrow();
            h.assertTrue(contact.nameColor().equals("yellow"), "Contacts did not show the personal yellow disposition");
            NpcDisposition.copyState(attacker, bystander);
            h.assertTrue(NpcDisposition.angry(bystander, npc.getUUID()), "Respawn lost disposition");
            bystander.getPersistentData().remove("cyberneticenhancements.npc_disposition");
            npc.setHealth(8);
            npc.invulnerableTime = 0;
            npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 2);
            h.assertTrue(npc.getTarget() == null && NpcDisposition.color(attacker, npc.getUUID()).equals("yellow"), "NPC became aggressive at exactly 25% health");
            npc.setHealth(7);
            npc.invulnerableTime = 0;
            npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 2);
            h.assertTrue(npc.getTarget() == attacker && NpcDisposition.color(attacker, npc.getUUID()).equals("red"), "Critical NPC did not defend itself with a red disposition");
            h.assertTrue(NpcDisposition.color(bystander, npc.getUUID()).equals("aqua"), "Self-defense made the NPC hostile to innocent players");
            h.assertTrue(ArchiveContactsPayload.capture(attacker).contacts().stream().anyMatch(c -> c.npcId().equals(npc.getUUID().toString()) && c.nameColor().equals("red")), "Contacts did not show active self-defense");
            var saved = new CompoundTag();
            npc.saveWithoutId(saved);
            var restored = ModEntityTypes.TECHIE.get().create(h.getLevel());
            restored.load(saved);
            h.assertTrue(!restored.isDefendingAgainst(attacker) && restored.getTarget() == null && restored.aggressionRemaining(0) == 0, "Reload left an NPC permanently hostile");
            restored.discard();
            npc.setHealth(npc.getMaxHealth());
            npc.aiStep();
            h.assertTrue(npc.getTarget() == null, "Healed NPC continued attacking");
            h.assertTrue(NpcDisposition.color(attacker, npc.getUUID()).equals("yellow"), "Healed defender stayed red");
            var identity = FixerIdentitySavedData.get(h.getLevel());
            identity.setNameColor(npc.getUUID(), "red");
            identity.setAppearance(npc.getUUID(), "cyberneticenhancements:textures/entity/npc/generic/max_tac.png");
            h.assertTrue(identity.identity(npc.getUUID()).nameColorId().equals("aqua") && identity.identity(npc.getUUID()).appearance().isEmpty(), "Identity customization bypassed status colors or reserved MaxTac skin");
            var tag = attacker.getPersistentData().getCompound("cyberneticenhancements.npc_disposition");
            tag.putLong(npc.getUUID().toString(), attacker.server.overworld().getGameTime());
            h.assertTrue(NpcDisposition.capture(attacker).angry().isEmpty(), "Expired anger did not clear");
        } finally {
            npc.discard();
            attacker.server.getPlayerList().remove(attacker);
            bystander.server.getPlayerList().remove(bystander);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_defense_cooldown", timeoutTicks = 650)
    public static void woundedNpcStopsDefendingAndAllowsInteractionAfterAttacksStop(GameTestHelper h) {
        var attacker = player(h, "defense-cooldown");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(attacker.position().add(0, 0, 2));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(attacker);
        npc.setHealth(5);
        npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 1);
        h.assertTrue(npc.getTarget() == attacker && npc.isRefusing(attacker), "Self-defense did not temporarily block interaction");
        h.assertTrue(npc.aggressionRemaining(0) == 1, "Initial aggression did not fill its bar");
        h.runAtTickTime(50, () -> h.assertTrue(npc.aggressionRemaining(0) > 0.4F && npc.aggressionRemaining(0) < 0.6F,
                "Aggression bar did not drain with the actual cooldown"));
        h.runAtTickTime(90, () -> h.assertTrue(npc.isDefendingAgainst(attacker), "NPC stopped defending before the quiet period ended"));
        h.runAtTickTime(110, () -> {
            try {
                h.assertTrue(npc.getTarget() == null && !npc.isDefendingAgainst(attacker) && !npc.isAggressive(), "NPC continued attacking after five quiet seconds");
                h.assertTrue(npc.aggressionRemaining(0) == 0, "Calmed NPC kept an aggression bar value");
                h.assertTrue(NpcDisposition.color(attacker, npc.getUUID()).equals("yellow"), "Calmed NPC stayed red");
                h.assertTrue(!npc.isRefusing(attacker), "An isolated assault made the NPC unusable");
                npc.mobInteract(attacker, net.minecraft.world.InteractionHand.MAIN_HAND);
                h.assertTrue(attacker.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu, "Wounded NPC could not be used again after backing off");
            } catch (RuntimeException error) {
                attacker.closeContainer();
                npc.discard();
                attacker.server.getPlayerList().remove(attacker);
                throw error;
            }
            attacker.closeContainer();
            npc.invulnerableTime = 0;
            npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 0.5F);
            h.assertTrue(npc.aggressionRemaining(0) == 1 && npc.isDefendingAgainst(attacker), "Hitting a calmed NPC did not refill aggression");
        });
        h.runAtTickTime(160, () -> {
            h.assertTrue(npc.aggressionRemaining(0) > 0.4F && npc.aggressionRemaining(0) < 0.6F, "Refilled aggression did not continue draining");
            npc.invulnerableTime = 0;
            npc.hurt(h.getLevel().damageSources().playerAttack(attacker), 0.1F);
            h.assertTrue(npc.aggressionRemaining(0) == 1, "Another hit during aggression did not reset the bar");
        });
        h.runAtTickTime(220, () -> h.assertTrue(npc.isDefendingAgainst(attacker) && npc.aggressionRemaining(0) > 0,
                "Aggression ended before the renewed cooldown"));
        h.runAtTickTime(270, () -> h.assertTrue(npc.getTarget() == null && npc.aggressionRemaining(0) == 0,
                "Renewed aggression did not fully drain and clear"));
        h.runAtTickTime(610, () -> {
            try {
                h.assertTrue(npc.getHealth() > 4 && npc.getTarget() == null, "Surviving NPC did not recover health after sustained peace");
            } finally {
                npc.discard();
                attacker.server.getPlayerList().remove(attacker);
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "maxtac_response", timeoutTicks = 100)
    public static void murderDeploysOnePersistentSquadAgainstOnlyTheKiller(GameTestHelper h) {
        var owner = player(h, "maxtac-offender");
        var innocent = player(h, "maxtac-innocent");
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(3, 14, 3));
        owner.setPos(Vec3.atBottomCenterOf(center));
        innocent.setPos(owner.position().add(0, 0, 3));
        var ground = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var npc = ModEntityTypes.FIXER.get().create(level);
        npc.setPos(owner.position().add(2, 0, 0));
        npc.setNoAi(true);
        level.addFreshEntity(npc);
        npc.ensureIdentity();
        try {
            for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) {
                var pos = center.offset(x, -1, z);
                ground.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            npc.hurt(level.damageSources().playerAttack(owner), 100000);
            var squad = level.getEntitiesOfClass(MaxTacEntity.class, new AABB(center).inflate(40));
            h.assertTrue(squad.size() == MaxTacResponse.SQUAD_SIZE, "NPC murder did not deploy four officers on valid ground");
            MaxTacResponse.onNpcKilled(npc, level.damageSources().playerAttack(owner));
            h.assertTrue(level.getEntitiesOfClass(MaxTacEntity.class, new AABB(center).inflate(40)).size() == 4, "Repeated crimes bypassed the squad cap");
            for (var officer : squad) {
                h.assertTrue(officer.getTarget() == owner && officer.canAttack(owner) && !officer.canAttack(innocent), "Squad targeted an innocent player");
                h.assertTrue(officer.getName().getStyle().getColor().getValue() == 0xFF5555 && officer.getMaxHealth() == 70, "Squad lost its red identity or combat stats");
                h.assertTrue(level.noCollision(officer) && level.getBlockState(officer.blockPosition().below()).isFaceSturdy(level, officer.blockPosition().below(), net.minecraft.core.Direction.UP), "Squad spawned inside terrain or in midair");
                h.assertTrue(officer.maxUpStep() == 1.5F && MaxTacResponse.reachablePath(officer, owner) != null, "Squad spawned without a route to the offender or lost fence traversal");
                var saved = new CompoundTag();
                officer.saveWithoutId(saved);
                var response = MaxTacResponse.load(MaxTacResponse.get(level).save(new CompoundTag(), level.registryAccess()), level.registryAccess());
                h.assertTrue(response.active(owner.getUUID(), saved.getUUID("MaxTacDeployment")), "Wanted deployment did not survive saving");
                var restored = ModEntityTypes.MAX_TAC.get().create(level);
                restored.load(saved);
                h.assertTrue(restored.maxUpStep() == 1.5F, "Officer reload lost the enhanced step height");
                h.assertTrue(owner.getUUID().equals(restored.offender()) && restored.getName().getStyle().getColor().getValue() == 0xFF5555 && restored.getMaxHealth() == 70, "Officer reload lost its binding, red name or stats");
                restored.discard();
            }
            for (var officer : squad) officer.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            var remaining = MaxTacResponse.get(level).save(new CompoundTag(), level.registryAccess()).getList("Pursuits", net.minecraft.nbt.Tag.TAG_COMPOUND);
            h.assertTrue(remaining.stream().noneMatch(v -> ((CompoundTag) v).getUUID("Offender").equals(owner.getUUID())), "Defeating the squad did not end the pursuit");
        } finally {
            MaxTacResponse.clear(owner);
            level.getEntitiesOfClass(MaxTacEntity.class, new AABB(center).inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
            npc.discard();
            ground.forEach((pos, state) -> level.setBlock(pos, state, 2));
            owner.server.getPlayerList().remove(owner);
            innocent.server.getPlayerList().remove(innocent);
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        return player;
    }
}
