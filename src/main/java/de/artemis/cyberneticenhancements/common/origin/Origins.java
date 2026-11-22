package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.dialogue.NpcService;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.tarot.TarotCatalog;
import de.artemis.cyberneticenhancements.common.tarot.TarotRewards;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** What each lifepath is worth: attribute bonuses, plus a standing with the city's money and its people. */
public final class Origins {
    public static final String KEY = "cyberneticenhancements.origin";
    /** Percentages. */
    public static final int CORPO_QUEST_PAY = 10, CORPO_COVERAGE_DISCOUNT = 40, NOMAD_ROAD_PAY = 15, NOMAD_TECHIE_DISCOUNT = 15,
            STREET_TRUST_GAIN = 50, STREET_DISCOUNT_BOOST = 40, DISCOUNT_CAP = 35;
    private static @Nullable Origin client;

    private Origins() {}

    public static @Nullable Origin of(Player player) {
        if (player.level().isClientSide) return client;
        return Origin.find(player.getPersistentData().getString(KEY));
    }

    public static boolean chosen(Player player) { return of(player) != null; }
    public static void receive(@Nullable Origin origin) { client = origin; }
    public static void clearClient() { client = null; }

    /** Sets a lifepath, or clears it with null. Players pick once; only commands and tests clear. */
    public static void set(ServerPlayer player, @Nullable Origin origin) {
        if (origin == null) player.getPersistentData().remove(KEY); else player.getPersistentData().putString(KEY, origin.id());
        sync(player);
    }

    /** The player's one choice. Ignored once a lifepath is held, so a replayed packet changes nothing. */
    public static boolean choose(ServerPlayer player, @Nullable Origin origin) {
        if (origin == null || chosen(player)) return false;
        set(player, origin);
        return true;
    }

    public static void sync(ServerPlayer player) {
        apply(player);
        Origin origin = of(player);
        OriginRegistry.get(player.server).record(player.getUUID(), player.getGameProfile().getName(), origin);
        PacketDistributor.sendToPlayer(player, new OriginStatePayload(origin == null ? "" : origin.id()));
    }

    public static void apply(Player player) {
        if (player.level().isClientSide) return;
        Origin origin = of(player);
        for (var type : TarotCatalog.BonusType.values()) {
            var attribute = TarotRewards.attribute(type);
            if (attribute == null) continue;
            var instance = player.getAttribute(attribute);
            if (instance == null) continue;
            double bonus = origin == null ? 0 : origin.rewards().stream().filter(reward -> reward.type() == type).mapToDouble(TarotCatalog.Reward::amount).sum();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "origin_" + type.id());
            var operation = type.multipliesBase() ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
            AttributeModifier previous = instance.getModifier(id);
            if (previous != null && previous.amount() == bonus && previous.operation() == operation) continue;
            if (previous != null) instance.removeModifier(id);
            if (bonus > 0) instance.addPermanentModifier(new AttributeModifier(id, bonus, operation));
        }
    }

    public static int headroom(Player player) {
        Origin origin = of(player);
        return origin == null ? 0 : (int) Math.round(origin.rewards().stream()
                .filter(reward -> reward.type() == TarotCatalog.BonusType.HEADROOM).mapToDouble(TarotCatalog.Reward::amount).sum());
    }

    /** A contract's money once the lifepath has had its say. */
    public static int questPay(Player player, QuestType type, int money) {
        Origin origin = of(player);
        int percent = 100;
        if (origin == Origin.CORPO) percent += CORPO_QUEST_PAY;
        else if (origin == Origin.NOMAD && roadWork(type)) percent += NOMAD_ROAD_PAY;
        return percent == 100 ? money : Math.max(1, Math.round(money * percent / 100.0F));
    }

    /** Jobs that mean a long way on the road. */
    public static boolean roadWork(QuestType type) {
        return type == QuestType.DELIVERY || type == QuestType.EXTRACTION || type == QuestType.SALVAGE || type == QuestType.LOCATE;
    }

    /** Trust earned with a contact; the street remembers who looked out for it. */
    public static int trustGain(Player player, int amount) {
        return amount > 0 && of(player) == Origin.STREET_KID ? Math.max(amount + 1, Math.round(amount * (100 + STREET_TRUST_GAIN) / 100.0F)) : amount;
    }

    /** The trust discount in percent, given the plain one. */
    public static int discount(Player player, int percent) {
        return percent > 0 && of(player) == Origin.STREET_KID ? Math.min(DISCOUNT_CAP, Math.round(percent * (100 + STREET_DISCOUNT_BOOST) / 100.0F)) : percent;
    }

    /** Multiplier on a price that is not a trust discount: corpo cover and nomad workshops. */
    public static float priceMultiplier(Player player, NpcType npc, @Nullable NpcService service) {
        Origin origin = of(player);
        if (origin == Origin.CORPO && service == NpcService.TRAUMA_COVERAGE) return (100 - CORPO_COVERAGE_DISCOUNT) / 100.0F;
        if (origin == Origin.NOMAD && npc == NpcType.TECHIE) return (100 - NOMAD_TECHIE_DISCOUNT) / 100.0F;
        return 1.0F;
    }

    static Object[] perkArguments(Origin origin, int index) {
        return switch (origin) {
            case NOMAD -> new Object[]{index == 1 ? NOMAD_ROAD_PAY : NOMAD_TECHIE_DISCOUNT};
            case STREET_KID -> new Object[]{index == 1 ? STREET_TRUST_GAIN : STREET_DISCOUNT_BOOST, DISCOUNT_CAP};
            case CORPO -> new Object[]{index == 1 ? CORPO_COVERAGE_DISCOUNT : CORPO_QUEST_PAY};
        };
    }

    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) sync(event.getPlayer()); else event.getPlayerList().getPlayers().forEach(Origins::sync);
    }

    public static void onClone(PlayerEvent.Clone event) {
        var saved = event.getOriginal().getPersistentData().get(KEY);
        if (saved != null) event.getEntity().getPersistentData().put(KEY, saved.copy());
        apply(event.getEntity());
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { apply(event.getEntity()); }
}
