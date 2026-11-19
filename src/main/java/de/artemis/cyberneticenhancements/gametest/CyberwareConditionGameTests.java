package de.artemis.cyberneticenhancements.gametest;

import com.mojang.authlib.GameProfile;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyberwareConditionGameTests {
    private CyberwareConditionGameTests() {
    }

    @GameTest(template = "sandevistan_test", batch = "condition", timeoutTicks = 20)
    public static void damagedImplantsShortCircuitWithGraceAndSharedCooldown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long originalTime = level.getGameTime();
        WetPlayer player = new WetPlayer(level);
        try {
            player.wet = true;
            player.water = true;
            ItemStack implant = new ItemStack(ModItems.cyberware("dynalar_sandevistan").get());
            PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
            inventory.setStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal(), implant);
            sample(level, player, 200);
            sample(level, player, 220);
            sample(level, player, 240);
            h.assertTrue(player.getHealth() == 20, "Healthy cyberware shorted out");
            CyberwareItem item = (CyberwareItem) implant.getItem();
            CyberwareConditionHelper.damage(implant, item.getDefinition(), CyberwareConditionHelper.getMaxIntegrity(item.getDefinition()));
            inventory.save();
            sample(level, player, 260);
            sample(level, player, 280);
            h.assertTrue(player.getHealth() == 20, "Water grace period was skipped");
            sample(level, player, 300);
            h.assertTrue(player.getHealth() == 19, "Short circuit did not deal half a heart");
            var statuses = CombatStatusManager.collectActiveStatuses(player, 300);
            h.assertTrue(statuses.size() == 1 && statuses.getFirst().type() == CombatStatusType.SHOCK
                    && statuses.getFirst().stacks() == 1 && statuses.getFirst().remainingSeconds() == 1,
                    "Short circuit did not apply one brief Shock stack");
            long next = player.getPersistentData().getLong("cyberneticenhancements.next_cyberware_short_circuit");
            h.assertTrue(next >= 900 && next < 1200, "Cooldown fell outside 30–45 seconds");
            player.invulnerableTime = 0;
            for (long time = 320; time <= 880; time += 20) sample(level, player, time);
            h.assertTrue(player.getHealth() == 19, "Continued exposure bypassed the shared cooldown");
            CyberwareConditionHelper.fullyRepair(implant, item.getDefinition());
            inventory.save();
            sample(level, player, 1200);
            sample(level, player, 1220);
            sample(level, player, 1240);
            h.assertTrue(player.getHealth() == 19, "Repair did not remove the hazard");
            h.succeed();
        } finally {
            CombatStatusManager.clearStatuses(player);
            player.server.getPlayerList().remove(player);
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
        }
    }

    @GameTest(template = "sandevistan_test", batch = "condition", timeoutTicks = 20)
    public static void rainResetsWhenDryAndDoesNotStrengthenExistingShock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long originalTime = level.getGameTime();
        WetPlayer player = new WetPlayer(level);
        try {
            ItemStack implant = new ItemStack(ModItems.cyberware("dynalar_sandevistan").get());
            CyberwareItem item = (CyberwareItem) implant.getItem();
            CyberwareConditionHelper.damage(implant, item.getDefinition(), CyberwareConditionHelper.getMaxIntegrity(item.getDefinition()));
            new PlayerCyberwareInventory(player).setStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal(), implant);
            player.wet = true;
            for (long time = 200; time <= 260; time += 20) sample(level, player, time);
            h.assertTrue(player.getHealth() == 20, "Rain did not allow five seconds of grace");
            player.wet = false;
            sample(level, player, 280);
            player.wet = true;
            for (long time = 300; time <= 360; time += 20) sample(level, player, time);
            h.assertTrue(player.getHealth() == 20, "Shelter did not reset rain exposure");
            CombatStatusManager.applyStatus(player, CombatStatusType.SHOCK, 200, 1);
            sample(level, player, 380);
            h.assertTrue(player.getHealth() == 19, "Sustained rain did not short out broken cyberware");
            var status = CombatStatusManager.collectActiveStatuses(player, 380).getFirst();
            h.assertTrue(status.stacks() == 1 && status.remainingSeconds() == 9, "Environmental Shock stacked or extended combat Shock");
            player.setGameMode(GameType.CREATIVE);
            sample(level, player, 1400);
            h.assertTrue(!player.getPersistentData().contains("cyberneticenhancements.cyberware_wet_exposure"), "Creative player accumulated exposure");
            player.setGameMode(GameType.SPECTATOR);
            sample(level, player, 1420);
            h.assertTrue(!player.getPersistentData().contains("cyberneticenhancements.cyberware_wet_exposure"), "Spectator accumulated exposure");
            h.succeed();
        } finally {
            CombatStatusManager.clearStatuses(player);
            player.server.getPlayerList().remove(player);
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
        }
    }

    private static void sample(ServerLevel level, WetPlayer player, long time) {
        level.getServer().getWorldData().overworldData().setGameTime(time);
        CyberwareConditionManager.onPlayerTick(player);
    }

    private static final class WetPlayer extends ServerPlayer {
        private boolean wet;
        private boolean water;

        private WetPlayer(ServerLevel level) {
            this(level, CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "condition-test"), false));
        }

        private WetPlayer(ServerLevel level, CommonListenerCookie cookie) {
            super(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, this, cookie);
            setGameMode(GameType.SURVIVAL);
            for (int tick = 0; tick < 61; tick++) tick();
        }

        @Override
        public boolean isInWaterOrRain() {
            return wet;
        }

        @Override
        public boolean isInWater() {
            return water;
        }
    }
}
