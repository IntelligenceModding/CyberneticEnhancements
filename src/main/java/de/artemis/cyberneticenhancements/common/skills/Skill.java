package de.artemis.cyberneticenhancements.common.skills;

import net.minecraft.network.chat.Component;

import java.util.List;

import static de.artemis.cyberneticenhancements.common.skills.SkillEffect.*;

/** The fixed set of skills. The order is the order they are listed in and the order ranks travel in over the network. */
public enum Skill {
    CHARISMA(Group.SOCIAL, SHOP_DISCOUNT, CONTRACT_PAY, TRUST_GAIN),
    INTIMIDATION(Group.SOCIAL, COMBAT_PAY),
    STREETWISE(Group.SOCIAL, COMMERCE_TRUST),
    DECEPTION(Group.SOCIAL, TRUST_LOSS_CUT),
    CHROME_TOLERANCE(Group.CYBERWARE, CHROME_CAPACITY, STRAIN_SLOWER),
    MAINTENANCE(Group.CYBERWARE, WEAR_SLOWER, SERVICE_DISCOUNT),
    NEURAL_RESISTANCE(Group.CYBERWARE, PSYCHOSIS_SLOWER),
    RIPPERDOC_RAPPORT(Group.CYBERWARE, RIPPERDOC_DISCOUNT),
    FABRICATION(Group.TECHNICAL, CRAFT_SPEED, MATERIAL_REFUND),
    SCAVENGING(Group.TECHNICAL, CACHE_QUALITY),
    ENGINEERING(Group.TECHNICAL, SALVAGE_YIELD),
    RESILIENCE(Group.SURVIVAL, MAX_HEALTH),
    ATHLETICS(Group.SURVIVAL, FALL_REDUCTION),
    STREET_MEDICINE(Group.SURVIVAL, HEALING_BOOST),
    EVASION(Group.SURVIVAL, DAMAGE_REDUCTION);

    public enum Group {
        SOCIAL, CYBERWARE, TECHNICAL, SURVIVAL;

        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
        public Component title() { return Component.translatable("screen.cyberneticenhancements.skills.group." + key()); }
    }

    private final Group group;
    private final List<SkillEffect> effects;

    Skill(Group group, SkillEffect... effects) {
        this.group = group;
        this.effects = List.of(effects);
    }

    public Group group() { return group; }
    public List<SkillEffect> effects() { return effects; }
    public boolean improves(SkillEffect effect) { return effects.contains(effect); }

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    public Component title() { return Component.translatable("skill.cyberneticenhancements." + key()); }
    public Component description() { return Component.translatable("skill.cyberneticenhancements." + key() + ".description"); }
}
