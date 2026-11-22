package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.Set;

/** Collection rewards are free attributes, separate from installed cyberware and chrome cost. */
public final class TarotRewards {
    public static void apply(Player player) {
        if (player.level().isClientSide) return;
        Set<String> known = TarotKnowledge.known(player);
        for (var type : TarotCatalog.BonusType.values()) {
            var attribute = attribute(type);
            if (attribute == null) continue; // Headroom is applied by PlayerCyberwareInventory.
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            double bonus = TarotCatalog.total(type, known::contains);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_" + type.id());
            var operation = type.multipliesBase() ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
            AttributeModifier previous = instance.getModifier(id);
            if (previous != null && previous.amount() == bonus && previous.operation() == operation) continue;
            if (previous != null) instance.removeModifier(id);
            if (bonus > 0) instance.addPermanentModifier(new AttributeModifier(id, bonus, operation));
        }
    }
    public static net.minecraft.core.Holder<Attribute> attribute(TarotCatalog.BonusType type) {
        return switch (type) {
            case HEALTH -> Attributes.MAX_HEALTH;
            case HEADROOM -> null;
            case ARMOR -> Attributes.ARMOR;
            case ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case LUCK -> Attributes.LUCK;
            case MINING_SPEED -> Attributes.BLOCK_BREAK_SPEED;
            case BLOCK_REACH -> Attributes.BLOCK_INTERACTION_RANGE;
            case KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE;
            case SAFE_FALL_DISTANCE -> Attributes.SAFE_FALL_DISTANCE;
        };
    }
    public static int chromeHeadroom(Player player) {
        int origin = de.artemis.cyberneticenhancements.common.origin.Origins.headroom(player);
        if (player.level().isClientSide) return origin + (int) Math.round(TarotCatalog.total(TarotCatalog.BonusType.HEADROOM, TarotKnowledge::clientKnows));
        Set<String> known = TarotKnowledge.known(player);
        return origin + (int) Math.round(TarotCatalog.total(TarotCatalog.BonusType.HEADROOM, known::contains));
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { apply(event.getEntity()); }
    private TarotRewards() {}
}
