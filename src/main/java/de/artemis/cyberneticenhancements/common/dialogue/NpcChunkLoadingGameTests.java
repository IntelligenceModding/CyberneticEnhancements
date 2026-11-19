package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcChunkLoadingGameTests {
    @GameTest(template = "sandevistan_test", batch = "npc_chunk_availability", timeoutTicks = 160)
    public static void ticketsFollowContactsAndReleaseOnlyTheirOwnOwnershipOnDeathOrRemoval(GameTestHelper h) {
        var caller = NpcCallGameTests.player(h, "remote-caller");
        var first = ModEntityTypes.FIXER.get().create(h.getLevel());
        var second = ModEntityTypes.TECHIE.get().create(h.getLevel());
        var origin = h.absolutePos(new BlockPos(2, 2, 2));
        first.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        second.setPos(first.position()); first.setNoAi(true); second.setNoAi(true); first.setNoGravity(true); second.setNoGravity(true);
        h.getLevel().addFreshEntity(first); h.getLevel().addFreshEntity(second); first.markMet(caller.player());
        long initial = first.chunkPosition().toLong();
        h.runAfterDelay(25, () -> {
            h.assertTrue(has(h.getLevel(), first.getUUID(), initial) && has(h.getLevel(), second.getUUID(), initial), "Contacts did not acquire entity-owned ticking tickets");
            first.setPos(first.position().add(32, 0, 0));
        });
        h.runAfterDelay(65, () -> {
            h.assertTrue(has(h.getLevel(), first.getUUID(), first.chunkPosition().toLong()) && !has(h.getLevel(), first.getUUID(), initial), "Moving contact left the ticket behind");
            caller.player().setPos(first.position().add(500, 0, 0));
            h.assertTrue(de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.resolveCallContact(caller.player(), first.getUUID()) == first,
                    "A distant contact could not be resolved for calling");
            first.discard();
            h.assertTrue(!any(h.getLevel(), first.getUUID()) && has(h.getLevel(), second.getUUID(), initial), "Removing one contact affected another's shared chunk ticket");
            second.setHealth(0); second.die(second.damageSources().genericKill());
            h.assertTrue(!any(h.getLevel(), second.getUUID()), "A dead NPC retained its ticking ticket");
            second.discard(); caller.player().server.getPlayerList().remove(caller.player()); h.succeed();
        });
    }
    private static ForcedChunksSavedData data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks"); }
    private static boolean has(ServerLevel level, UUID owner, long chunk) {
        return owned(level, owner).contains(chunk);
    }
    private static boolean any(ServerLevel level, UUID owner) {
        return !owned(level, owner).isEmpty();
    }
    private static java.util.List<Long> owned(ServerLevel level, UUID owner) {
        var saved = data(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        var result = new java.util.ArrayList<Long>();
        for (var entry : saved.getList("ModForced", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var controller = (net.minecraft.nbt.CompoundTag) entry;
            if (!controller.getString("Controller").equals("cyberneticenhancements:resident_npcs")) continue;
            for (var chunkTag : controller.getList("ModForced", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var chunk = (net.minecraft.nbt.CompoundTag) chunkTag;
                for (var entity : chunk.getList("TickingEntities", net.minecraft.nbt.Tag.TAG_INT_ARRAY))
                    if (net.minecraft.nbt.NbtUtils.loadUUID(entity).equals(owner)) result.add(chunk.getLong("Chunk"));
            }
        }
        return result;
    }
    private NpcChunkLoadingGameTests() { }
}
