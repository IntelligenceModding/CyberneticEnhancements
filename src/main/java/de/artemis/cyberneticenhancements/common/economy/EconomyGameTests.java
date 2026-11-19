package de.artemis.cyberneticenhancements.common.economy;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EconomyGameTests {
    @GameTest(template = "sandevistan_test", batch = "economy_cache", timeoutTicks = 60)
    public static void interruptedCacheRewardsConserveStacksAndCannotPayTwice(GameTestHelper h) {
        var p = player(h, "cache-economy");
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        p.setPos(Vec3.atCenterOf(pos.offset(0, 0, 2)));
        h.setBlock(new BlockPos(4, 2, 4), ModBlocks.UNCOMMON_RELIC_CACHE.get());
        var cache = (RelicCacheBlockEntity) level.getBlockEntity(pos);
        try {
            for (int slot = 0; slot < 36; slot++) p.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 63));
            h.assertTrue(cache.startAnimation(p.getUUID(), List.of(new ItemStack(Items.DIAMOND)), List.of(new ItemStack(Items.DIAMOND, 4))), "Reward fixture did not start");
            level.destroyBlock(pos, true, p);
            cache.releasePendingRewards(level, pos);
            int dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4),
                    item -> item.getItem().is(Items.DIAMOND)).stream().mapToInt(item -> item.getItem().getCount()).sum();
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 64 && dropped == 3, "Partial inventory insertion lost or duplicated cache rewards");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4),
                    item -> item.getItem().is(ModBlocks.UNCOMMON_RELIC_CACHE.get().asItem())).isEmpty(), "Broken cache dropped a farmable cache block");
        } finally {
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4)).forEach(net.minecraft.world.entity.Entity::discard);
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "economy_relocated_cache", timeoutTicks = 60)
    public static void oldCacheItemsPlacedElsewhereCannotMintNewRewards(GameTestHelper h) {
        var p = player(h, "relocated-cache");
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        var level = h.getLevel();
        h.setBlock(new BlockPos(4, 2, 4), ModBlocks.UNCOMMON_RELIC_CACHE.get());
        var state = level.getBlockState(pos);
        try {
            state.getBlock().setPlacedBy(level, pos, state, p, new ItemStack(state.getBlock()));
            var cache = (RelicCacheBlockEntity) level.getBlockEntity(pos);
            var saved = cache.saveWithFullMetadata(level.registryAccess());
            cache.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(!cache.ensureHackSession(p) && p.totalExperience == 0 && p.getInventory().isEmpty(), "Relocated cache enabled a new survival reward run");
        } finally {
            level.removeBlock(pos, false);
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "economy_affinity", timeoutTicks = 60)
    public static void commerceTrustIsCappedPerPlayerAndDayAndSurvivesNpcReload(GameTestHelper h) {
        var p = player(h, "commerce-owner");
        var other = player(h, "commerce-other");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(p.position());
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        long day = h.getLevel().getDayTime();
        try {
            for (int i = 0; i < 20; i++) npc.rewardCommerceTrust(p, 2);
            h.assertTrue(npc.trustPoints(p) == 4 && npc.trustPoints(other) == 0, "Commerce bypassed daily cap or leaked affinity to another player");
            var saved = new CompoundTag();
            npc.saveWithoutId(saved);
            npc.load(saved);
            npc.rewardCommerceTrust(p, 2);
            h.assertTrue(npc.trustPoints(p) == 4, "Reload reset commerce budget");
            h.getLevel().setDayTime(day + 24000);
            npc.rewardCommerceTrust(p, 2);
            h.assertTrue(npc.trustPoints(p) == 6, "A new day did not renew commerce budget");
            npc.raiseTrust(p, 8);
            h.assertTrue(npc.trustPoints(p) == 14, "Commerce cap suppressed earned contract trust");
            de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.copyRelations(p, other);
            h.assertTrue(npc.trustPoints(other) == 14, "Respawn lost NPC relationships");
            npc.rewardCommerceTrust(other, 20);
            h.assertTrue(npc.trustPoints(other) == 16, "Respawn reset the remaining daily commerce budget");
            var cachePos = h.absolutePos(new BlockPos(4, 2, 4));
            de.artemis.cyberneticenhancements.common.block.RelicCacheBlock.markPlayerOpened(p, h.getLevel(), cachePos);
            de.artemis.cyberneticenhancements.common.block.RelicCacheBlock.copyState(p, other);
            h.assertTrue(de.artemis.cyberneticenhancements.common.block.RelicCacheBlock.hasPlayerOpened(other, h.getLevel(), cachePos), "Respawn reset cache rewards");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                var payload = new de.artemis.cyberneticenhancements.common.network.EconomyBalancePayload("{\"shop.fixer.maxdoc\":60}");
                de.artemis.cyberneticenhancements.common.network.EconomyBalancePayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(payload.equals(de.artemis.cyberneticenhancements.common.network.EconomyBalancePayload.STREAM_CODEC.decode(buffer)), "Economy sync did not round-trip");
            } finally { buffer.release(); }
        } finally {
            h.getLevel().setDayTime(day);
            npc.discard();
            p.server.getPlayerList().remove(p);
            other.server.getPlayerList().remove(other);
        }
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
