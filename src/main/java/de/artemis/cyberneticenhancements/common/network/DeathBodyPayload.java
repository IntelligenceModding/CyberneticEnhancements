package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Public appearance only: no inventory, personal data or gameplay state is copied. */
public record DeathBodyPayload(UUID player, int entityId, int phase, long started, float fallYaw, List<ItemStack> equipment) implements CustomPacketPayload {
    public DeathBodyPayload {
        if (phase < DOWNED || phase > RECOVERED) throw new IllegalArgumentException("Invalid death body phase");
        if (!Float.isFinite(fallYaw)) throw new IllegalArgumentException("Invalid body fall direction");
        if (!equipment.isEmpty() && equipment.size() != SLOTS.size()) throw new IllegalArgumentException("Invalid body equipment count");
        equipment = equipment.stream().map(ItemStack::copy).toList();
    }
    public static final int DOWNED = 0, DEAD = 1, RECOVERED = 2;
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    public static final Type<DeathBodyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "death_body"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeathBodyPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeUUID(p.player); b.writeVarInt(p.entityId); b.writeVarInt(p.phase); b.writeLong(p.started); b.writeFloat(p.fallYaw);
                b.writeVarInt(p.equipment.size());
                for (var stack : p.equipment) ItemStack.OPTIONAL_STREAM_CODEC.encode(b, stack);
            }, b -> {
                UUID player = b.readUUID(); int entityId = b.readVarInt(); int phase = b.readVarInt(); long started = b.readLong(); float yaw = b.readFloat();
                int count = b.readVarInt();
                if (count != 0 && count != SLOTS.size()) throw new IllegalArgumentException("Invalid body equipment count");
                var items = new java.util.ArrayList<ItemStack>(count);
                for (int i = 0; i < count; i++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
                return new DeathBodyPayload(player, entityId, phase, started, yaw, List.copyOf(items));
            });
    @Override public Type<DeathBodyPayload> type() { return TYPE; }
}
