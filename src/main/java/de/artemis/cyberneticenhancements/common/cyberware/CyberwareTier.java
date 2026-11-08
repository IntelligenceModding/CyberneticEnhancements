package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public enum CyberwareTier {
    TIER_1("tier_1", Rarity.COMMON, ChatFormatting.GRAY),
    TIER_2("tier_2", Rarity.UNCOMMON, ChatFormatting.GREEN),
    TIER_3("tier_3", Rarity.UNCOMMON, ChatFormatting.BLUE),
    TIER_4("tier_4", Rarity.RARE, ChatFormatting.LIGHT_PURPLE),
    TIER_5("tier_5", Rarity.EPIC, ChatFormatting.GOLD);

    private final String id;
    private final Rarity rarity;
    private final ChatFormatting color;

    CyberwareTier(String id, Rarity rarity, ChatFormatting color) {
        this.id = id;
        this.rarity = rarity;
        this.color = color;
    }

    public CyberwareTier next() {
        CyberwareTier[] tiers = values();
        return tiers[Math.min(ordinal() + 1, tiers.length - 1)];
    }

    public String translationKey() {
        return "cyberwareTier.cyberneticenhancements." + id;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public ChatFormatting getColor() {
        return color;
    }
}
