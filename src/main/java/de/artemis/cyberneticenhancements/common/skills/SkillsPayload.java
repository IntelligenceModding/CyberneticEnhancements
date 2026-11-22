package de.artemis.cyberneticenhancements.common.skills;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The player's rank in every skill, in the order of {@link Skill}. */
public record SkillsPayload(int[] ranks) implements CustomPacketPayload {
    public static final Type<SkillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "skills"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillsPayload> STREAM_CODEC = CustomPacketPayload.codec((p, b) -> {
        for (int rank : p.ranks) b.writeVarInt(rank);
    }, b -> {
        int[] ranks = new int[Skill.values().length];
        for (int i = 0; i < ranks.length; i++) ranks[i] = b.readVarInt();
        return new SkillsPayload(ranks);
    });
    @Override public Type<SkillsPayload> type() { return TYPE; }
}
