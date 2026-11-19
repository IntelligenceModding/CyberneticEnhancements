package de.artemis.cyberneticenhancements.common.tutorial;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A client-only tutorial event, such as opening the archive, for steps the server cannot observe. */
public record TutorialSignalPayload(String signal) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 64;
    public static final Type<TutorialSignalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tutorial_signal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialSignalPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> b.writeUtf(p.signal, MAX_LENGTH), b -> new TutorialSignalPayload(b.readUtf(MAX_LENGTH)));
    @Override public Type<TutorialSignalPayload> type() { return TYPE; }
}
