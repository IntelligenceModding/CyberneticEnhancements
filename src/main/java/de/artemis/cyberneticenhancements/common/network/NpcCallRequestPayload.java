package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record NpcCallRequestPayload(UUID callId, UUID npcId, int action, String questId) implements CustomPacketPayload {
    public static final int START = -1, END = 0, LOCATE = 1, MEET = 2, WORK = 3, ADVICE = 4, BACK = 5, KEEPALIVE = 6, TALK_IN_PERSON = 7;
    public static final int QUEST_STATUS = 8, QUEST_HELP = 9, QUEST_LOCATION = 10, REPORT_CONTRACT = 11,
            SMALL_TALK = 12, SERVICES = 13, MORE_CONTRACTS = 14, OFFER_FIRST = 20, OFFER_SECOND = 21, ACCEPT_CONTRACT = 22;
    public static final int ANSWER_INCOMING = 15, SEND_MONEY = 16, DECLINE_MONEY = 17, ACKNOWLEDGE_REMINDER = 18;
    public static final int ACCEPT_URGENT = 23, DECLINE_URGENT = 24;
    public static final int REQUEST_REPLACEMENT = 25, CONFIRM_ITEM_KEPT = 26, ADMIT_ITEM_LOST = 27;
    public static final Type<NpcCallRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc_call_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcCallRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeUUID(payload.callId);
                buffer.writeUUID(payload.npcId);
                buffer.writeVarInt(payload.action);
                if (payload.action == START) buffer.writeUtf(payload.questId, 256);
            }, buffer -> {
                UUID callId = buffer.readUUID(), npcId = buffer.readUUID();
                int action = buffer.readVarInt();
                return new NpcCallRequestPayload(callId, npcId, action, action == START ? buffer.readUtf(256) : "");
            });

    public NpcCallRequestPayload(UUID callId, UUID npcId, int action) {
        this(callId, npcId, action, "");
    }

    @Override
    public Type<NpcCallRequestPayload> type() { return TYPE; }
}
