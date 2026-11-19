package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.NpcDispositionPayload;
import net.minecraft.client.Minecraft;
import java.util.Set;
import java.util.UUID;

public final class NpcDispositionClientState {
    private static Set<UUID> angry = Set.of();
    private NpcDispositionClientState() {}
    public static void update(NpcDispositionPayload payload) { angry = Set.copyOf(payload.angry()); tick(); }
    public static void clear() { angry = Set.of(); }
    public static void tick() {
        var level = Minecraft.getInstance().level;
        var player = Minecraft.getInstance().player;
        if (level == null || player == null) return;
        for (var entity : level.entitiesForRendering())
            if (entity instanceof AbstractCityNpcEntity npc) npc.applyClientDisposition(angry.contains(npc.getUUID()), player);
    }
}
