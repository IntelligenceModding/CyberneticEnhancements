package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanProfile;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SandevistanGameTests {
    private SandevistanGameTests() {
    }

    @GameTest(template = "sandevistan_test", batch = "sandevistan_local", timeoutTicks = 60)
    public static void localDilationPreservesPlayersAndRestoresMobs(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = player(helper, SandevistanProfile.EXPERIMENTAL, new BlockPos(4, 1, 2));
        ServerPlayer other = mockPlayer(helper);
        other.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 5));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 1, 5));
        villager.setNoAi(true);
        villager.setNoGravity(true);
        int mobAge = zombie.tickCount;
        int playerAge = other.tickCount;
        CyberwareAbilities.activate(owner);
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(SandevistanTimeDilation.scale(zombie) < 0.2, "Hostile mob was not dilated");
            helper.assertTrue(zombie.tickCount - mobAge <= 4, "Hostile AI advanced at normal speed");
            helper.assertTrue(other.tickCount - playerAge >= 18, "An ordinary player's simulation was slowed");
            helper.assertTrue(SandevistanTimeDilation.scale(other) == 1, "Player entered the dilation target set");
            helper.assertTrue(SandevistanTimeDilation.scale(villager) == 1, "A friendly NPC was slowed");
            CyberwareAbilities.activate(owner);
            helper.assertTrue(SandevistanManager.recoveryRemaining(owner) > 0, "Cancellation erased hardware recovery");
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(SandevistanTimeDilation.scale(zombie) == 1, "Mob remained slowed after cancellation");
            remove(owner);
            remove(other);
            helper.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "sandevistan_overlap", timeoutTicks = 50)
    public static void strongestFieldWinsAndActiveUsersKeepTheirShots(GameTestHelper helper) {
        floor(helper);
        ServerPlayer first = player(helper, "dynalar_sandevistan", new BlockPos(2, 1, 2));
        ServerPlayer second = player(helper, SandevistanProfile.EXPERIMENTAL, new BlockPos(6, 1, 2));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 6));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        CyberwareAbilities.activate(first);
        CyberwareAbilities.activate(second);
        Arrow outgoing = arrow(helper, second, new BlockPos(1, 4, 1), 0.02);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(Math.abs(SandevistanTimeDilation.scale(zombie) - 0.08F) < 0.001, "Overlapping fields multiplied or selected the wrong rate");
            helper.assertTrue(SandevistanTimeDilation.scale(outgoing) == 1, "Another accelerated user's projectile was slowed");
            CyberwareAbilities.activate(second);
        });
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(Math.abs(SandevistanTimeDilation.scale(zombie) - 0.55F) < 0.001, "Ending one field removed the remaining user's influence");
            CyberwareAbilities.activate(first);
        });
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(SandevistanTimeDilation.scale(zombie) == 1, "Last field left a persistent slowdown");
            remove(first);
            remove(second);
            helper.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "sandevistan_projectiles", timeoutTicks = 110)
    public static void slowedArrowsKeepImpactDamageAndHitWalls(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = player(helper, SandevistanProfile.EXPERIMENTAL, new BlockPos(4, 1, 1));
        var shooter = helper.spawn(EntityType.SKELETON, new BlockPos(1, 1, 7));
        shooter.setNoAi(true);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 4));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        helper.setBlock(new BlockPos(5, 3, 4), Blocks.STONE);
        CyberwareAbilities.activate(owner);
        Arrow attack = arrow(helper, shooter, new BlockPos(1, 2, 4), 1);
        attack.setBaseDamage(10);
        Arrow wall = arrow(helper, shooter, new BlockPos(1, 3, 4), 1);
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(attack.getX() - helper.absolutePos(new BlockPos(1, 2, 4)).getX() < 3, "Arrow did not move at the local time scale");
            helper.assertTrue(attack.getDeltaMovement().x > 0.9, "Dilation permanently reduced physical projectile energy");
        });
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(target.getHealth() <= 12, "Slowing an arrow reduced its normal impact damage");
            double wallX = wall.getX() - helper.absolutePos(new BlockPos(5, 3, 4)).getX();
            helper.assertTrue(wallX > -0.2 && wallX < 0.1, "Arrow stopped short of the wall or passed through it");
            remove(owner);
            helper.succeed();
        });
    }

    private static ServerPlayer player(GameTestHelper helper, String id, BlockPos pos) {
        ServerPlayer player = mockPlayer(helper);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos)));
        player.setNoGravity(true);
        new PlayerCyberwareInventory(player).setStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal(), new ItemStack(ModItems.cyberware(id).get()));
        return player;
    }

    @GameTest(template = "sandevistan_test", batch = "sandevistan_recovery", timeoutTicks = 30)
    public static void replacementAndEquipmentChangesCannotEraseRecovery(GameTestHelper helper) {
        floor(helper);
        ServerPlayer original = player(helper, SandevistanProfile.EXPERIMENTAL, new BlockPos(4, 1, 2));
        CyberwareAbilities.activate(original);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(SandevistanManager.session(original) != null, "Initial activation failed");
            long recovery = SandevistanManager.recoveryRemaining(original);
            ServerPlayer replacement = player(helper, "dynalar_sandevistan", new BlockPos(4, 1, 2));
            SandevistanManager.copyState(original, replacement);
            helper.assertTrue(SandevistanManager.session(original) == null, "Replacement left the old acceleration running");
            helper.assertTrue(SandevistanManager.recoveryRemaining(replacement) >= recovery, "Player replacement discarded persistent recovery");
            helper.assertTrue(SandevistanManager.recentUses(replacement) == 1, "Player replacement discarded neural usage history");
            CyberwareAbilities.activate(replacement);
            helper.assertTrue(SandevistanManager.session(replacement) == null, "Changing implant or player entity bypassed recovery");
            remove(original);
            remove(replacement);
            helper.succeed();
        });
    }

    private static Arrow arrow(GameTestHelper helper, LivingEntity shooter, BlockPos pos, double speed) {
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(Vec3.atCenterOf(helper.absolutePos(pos)));
        arrow.setOwner(shooter);
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(speed, 0, 0);
        helper.getLevel().addFreshEntity(arrow);
        return arrow;
    }

    @GameTest(template = "sandevistan_test", batch = "sandevistan_melee", timeoutTicks = 30)
    public static void recoveredAcceleratedMeleeCanConnectBeforeNormalDamageProtectionEnds(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = player(helper, SandevistanProfile.EXPERIMENTAL, new BlockPos(4, 1, 2));
        helper.onEachTick(owner::doTick);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 4));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        CyberwareAbilities.activate(owner);
        float[] afterFirstHit = new float[1];
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(owner.getAttackStrengthScale(0.5F) > 0.9F, "Accelerated attack did not recover at its intended rate");
            owner.attack(target);
            afterFirstHit[0] = target.getHealth();
            helper.assertTrue(afterFirstHit[0] < target.getMaxHealth(), "Initial melee attack failed");
        });
        helper.runAtTickTime(17, () -> {
            helper.assertTrue(target.invulnerableTime > 10, "Test did not exercise vanilla damage protection");
            owner.attack(target);
            helper.assertTrue(target.getHealth() < afterFirstHit[0], "Recovered accelerated melee was blocked by normal damage protection");
            remove(owner);
            helper.succeed();
        });
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "sandevistan-test"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static void remove(ServerPlayer player) {
        CyberwareAbilities.onPlayerLogout(player);
        player.server.getPlayerList().remove(player);
    }
}
