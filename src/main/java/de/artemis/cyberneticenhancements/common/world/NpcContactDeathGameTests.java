package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcContactDeathGameTests {
    @GameTest(template = "sandevistan_test", batch = "npc_contact_death", timeoutTicks = 100)
    public static void confirmedDeathsPersistAndAreSharedWithoutTreatingMissingContactsAsDead(GameTestHelper h) {
        var owner = player(h, "death-contact");
        var other = player(h, "death-known");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        var missing = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setNoAi(true);
        missing.setNoAi(true);
        npc.setPos(owner.position().add(0, 0, 2));
        missing.setPos(owner.position().add(2, 0, 2));
        h.getLevel().addFreshEntity(npc);
        h.getLevel().addFreshEntity(missing);
        npc.ensureIdentity();
        missing.ensureIdentity();
        npc.markMet(owner);
        npc.markMet(other);
        missing.markMet(owner);
        try {
            h.assertTrue(!contact(owner, npc.getUUID()).deceased(), "Living NPC was displayed as deceased");
            npc.hurt(h.getLevel().damageSources().generic(), 1);
            h.assertTrue(!contact(owner, npc.getUUID()).deceased(), "Nonlethal damage marked a contact as deceased");
            npc.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            npc.discard();
            missing.discard();
            h.assertTrue(contact(owner, npc.getUUID()).deceased() && contact(other, npc.getUUID()).deceased(), "Confirmed death was not shared with every player who knew the contact");
            h.assertTrue(!contact(owner, missing.getUUID()).deceased(), "An unavailable or removed contact was incorrectly marked dead");
            h.assertTrue(AbstractCityNpcEntity.resolveCallContact(owner, npc.getUUID()) == null, "Dead contact could still be resolved for a call");
            var data = FixerIdentitySavedData.get(h.getLevel());
            data.setNameColor(missing.getUUID(), "gold");
            data.setAppearance(missing.getUUID(), "minecraft:textures/entity/player/wide/steve.png");
            var unloaded = contact(owner, missing.getUUID());
            h.assertTrue(unloaded.nameColor().equals("aqua")
                    && unloaded.appearance().equals("minecraft:textures/entity/player/wide/steve.png"),
                    "Unloaded contact did not preserve its friendly color and customized face");
            var restored = FixerIdentitySavedData.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(restored.isDeceased(npc.getUUID()) && !restored.isDeceased(missing.getUUID())
                    && restored.identity(npc.getUUID()).displayName().equals(npc.getName().getString()), "Saving and loading lost the death state or contact identity");
            var payload = ArchiveContactsPayload.capture(owner);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                ArchiveContactsPayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(payload.equals(ArchiveContactsPayload.STREAM_CODEC.decode(buffer)), "Contact death state was lost during client synchronization");
            } finally { buffer.release(); }
        } finally {
            npc.discard();
            missing.discard();
            owner.server.getPlayerList().remove(owner);
            other.server.getPlayerList().remove(other);
        }
        h.succeed();
    }

    private static ArchiveContactsPayload.ContactEntry contact(ServerPlayer player, UUID npc) {
        return ArchiveContactsPayload.capture(player).contacts().stream().filter(contact -> contact.npcId().equals(npc.toString())).findFirst().orElseThrow();
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3))));
        return player;
    }
}
