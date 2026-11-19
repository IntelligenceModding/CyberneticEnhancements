package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record ArchiveContactsPayload(List<ContactEntry> contacts) implements CustomPacketPayload {
    public static final Type<ArchiveContactsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "archive_contacts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveContactsPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {
                buffer.writeVarInt(payload.contacts.size());
                for (ContactEntry contact : payload.contacts) {
                    buffer.writeUtf(contact.npcId());
                    buffer.writeUtf(contact.npcTypeId());
                    buffer.writeUtf(contact.name());
                    buffer.writeUtf(contact.categoryId());
                    buffer.writeVarInt(contact.trustLevel());
                    buffer.writeBoolean(contact.pinned());
                    buffer.writeBoolean(contact.hidden());
                    buffer.writeBoolean(contact.purgeReady());
                    buffer.writeBoolean(contact.surchargeActive());
                    buffer.writeBoolean(contact.refusing());
                    buffer.writeUtf(contact.dimensionId());
                    buffer.writeBoolean(contact.hasLocation());
                    buffer.writeInt(contact.x());
                    buffer.writeInt(contact.y());
                    buffer.writeInt(contact.z());
                    buffer.writeBoolean(contact.deceased());
                    buffer.writeUtf(contact.appearance());
                    buffer.writeUtf(contact.nameColor());
                    buffer.writeUtf(contact.callStatus(), 32);
                }
            }, buffer -> {
                int count = buffer.readVarInt();
                List<ContactEntry> contacts = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    contacts.add(new ContactEntry(
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readUtf(),
                            buffer.readBoolean(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(32)
                    ));
                }
                return new ArchiveContactsPayload(List.copyOf(contacts));
            });

    @Override
    public Type<ArchiveContactsPayload> type() {
        return TYPE;
    }

    public static ArchiveContactsPayload capture(ServerPlayer player) {
        List<ContactEntry> contacts = new ArrayList<>();
        for (AbstractCityNpcEntity.ArchiveContact contact : AbstractCityNpcEntity.archiveContacts(player)) {
            var identity = FixerIdentitySavedData.get(player.serverLevel()).identity(contact.npcId());
            AbstractCityNpcEntity.ArchiveContactLocation location = contact.deceased()
                    ? new AbstractCityNpcEntity.ArchiveContactLocation(contact.dimensionId(), contact.homePos())
                    : AbstractCityNpcEntity.archiveContactLocation(player, contact.npcId(), contact.dimensionId(), contact.homePos());
            BlockPos position = location.pos();
            contacts.add(new ContactEntry(
                    contact.npcId().toString(),
                    contact.npcTypeId(),
                    contact.name(),
                    contact.category().id(),
                    contact.trustLevel(),
                    contact.pinned(),
                    contact.hidden(),
                    contact.purgeReady(),
                    contact.surchargeActive(),
                    contact.refusing(),
                    location.dimensionId() == null ? "" : location.dimensionId(),
                    position != null,
                    position == null ? 0 : position.getX(),
                    position == null ? 0 : position.getY(),
                    position == null ? 0 : position.getZ(),
                    contact.deceased(),
                    identity == null ? "" : identity.appearance(),
                    de.artemis.cyberneticenhancements.common.world.NpcDisposition.color(player, contact.npcId())
            ));
        }
        contacts.addAll(de.artemis.cyberneticenhancements.common.dialogue.PlayerCallManager.contacts(player));
        return new ArchiveContactsPayload(List.copyOf(contacts));
    }

    public record ContactEntry(
            String npcId,
            String npcTypeId,
            String name,
            String categoryId,
            int trustLevel,
            boolean pinned,
            boolean hidden,
            boolean purgeReady,
            boolean surchargeActive,
            boolean refusing,
            String dimensionId,
            boolean hasLocation,
            int x,
            int y,
            int z,
            boolean deceased,
            String appearance,
            String nameColor,
            String callStatus
    ) {
        public ContactEntry(String npcId, String npcTypeId, String name, String categoryId, int trustLevel, boolean pinned,
                            boolean hidden, boolean purgeReady, boolean surchargeActive, boolean refusing, String dimensionId,
                            boolean hasLocation, int x, int y, int z, boolean deceased, String appearance, String nameColor) {
            this(npcId, npcTypeId, name, categoryId, trustLevel, pinned, hidden, purgeReady, surchargeActive, refusing,
                    dimensionId, hasLocation, x, y, z, deceased, appearance, nameColor, "");
        }
        public boolean isPlayer() { return "player".equals(npcTypeId); }
        public Component displayName() {
            ChatFormatting color = ChatFormatting.getByName(nameColor);
            return Component.literal(name).withStyle(color != null && color.isColor() ? color
                    : "hidden".equals(nameColor) ? ChatFormatting.WHITE : ChatFormatting.AQUA);
        }
    }
}
