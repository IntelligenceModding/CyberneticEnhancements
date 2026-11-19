package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class NpcCallDialogue {
    private static final String PREFIX = "screen.cyberneticenhancements.call.";

    private NpcCallDialogue() {}

    public static boolean isNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return level.dimensionType().hasSkyLight() && time >= 13000 && time < 23000;
    }

    public static Component greeting(AbstractCityNpcEntity npc, ServerPlayer player) {
        String context;
        if (npc.isAnnoyed(player)) context = "annoyed";
        else if (isNight(npc.level())) context = npc.npcType() == NpcType.NETRUNNER || npc.npcType() == NpcType.MERC || npc.npcType() == NpcType.MAXTEC ? "night_awake" : "night_sleepy";
        else if (npc.level().isRainingAt(npc.blockPosition())) context = npc.level().isThundering() ? "storm" : "rain";
        else if (npc.trustLevel(player) >= 2) context = "trusted";
        else context = npc.npcTypeId();
        return variant(npc, "greeting." + context);
    }

    public static Component meeting(AbstractCityNpcEntity npc, ServerPlayer player) {
        String context = player.serverLevel().isRainingAt(player.blockPosition())
                ? player.serverLevel().isThundering() ? "storm" : "rain"
                : isNight(npc.level()) ? "night" : "normal";
        return variant(npc, "meeting." + context);
    }

    public static Component smallTalk(AbstractCityNpcEntity npc, ServerPlayer player) {
        String context = npc.isAnnoyed(player) ? "annoyed" : isNight(npc.level()) ? "night"
                : npc.level().isRainingAt(npc.blockPosition()) ? "rain" : "normal";
        return variant(npc, "small_talk." + context);
    }

    public static Component variant(AbstractCityNpcEntity npc, String group, Object... arguments) {
        return Component.translatable(PREFIX + group + "." + npc.getRandom().nextInt(3), arguments);
    }
}
