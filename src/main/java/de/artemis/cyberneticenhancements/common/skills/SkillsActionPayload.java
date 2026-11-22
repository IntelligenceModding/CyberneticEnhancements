package de.artemis.cyberneticenhancements.common.skills;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** What the player asks for in the Skills tab: a rank in one skill, or every rank back. The server decides whether it happens. */
public record SkillsActionPayload(int skill, int quotedCost) implements CustomPacketPayload {
    /** Sent instead of a skill index to ask for a respec. */
    public static final int RESPEC = -1;
    public static final Type<SkillsActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "skills_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillsActionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeVarInt(p.skill);
                b.writeVarInt(p.quotedCost);
            }, b -> new SkillsActionPayload(b.readVarInt(), b.readVarInt()));
    @Override public Type<SkillsActionPayload> type() { return TYPE; }

    public static void handle(SkillsActionPayload payload, ServerPlayer player) {
        if (payload.skill == RESPEC) Skills.respec(player, payload.quotedCost);
        else if (payload.skill >= 0 && payload.skill < Skill.values().length) Skills.spend(player, Skill.values()[payload.skill]);
    }
}
