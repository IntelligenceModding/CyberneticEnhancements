package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.common.config.SkillsConfig;
import de.artemis.cyberneticenhancements.common.skills.Skill;
import de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.BonusType;
import de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.Reward;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import java.util.List;

/** A lifepath: who the player was before the city. Chosen once, kept for good. */
public enum Origin {
    NOMAD("nomad", List.of(new Reward(BonusType.MOVEMENT_SPEED, .04), new Reward(BonusType.SAFE_FALL_DISTANCE, 1)),
            new StartingRank(Skill.FABRICATION, 4), new StartingRank(Skill.ENGINEERING, 3), new StartingRank(Skill.SCAVENGING, 3)),
    STREET_KID("street_kid", List.of(new Reward(BonusType.KNOCKBACK_RESISTANCE, .1), new Reward(BonusType.LUCK, .5)),
            new StartingRank(Skill.STREETWISE, 4), new StartingRank(Skill.INTIMIDATION, 4), new StartingRank(Skill.EVASION, 2)),
    CORPO("corpo", List.of(new Reward(BonusType.HEADROOM, 3), new Reward(BonusType.ARMOR, 1)),
            new StartingRank(Skill.CHARISMA, 4), new StartingRank(Skill.DECEPTION, 4), new StartingRank(Skill.CHROME_TOLERANCE, 2));

    /** Every lifepath has two dealings with the city besides its attribute bonuses. */
    public static final int CITY_PERKS = 2;
    private final String id;
    private final List<Reward> rewards;

    public record StartingRank(Skill skill, int rank) {}
    private final List<StartingRank> startingRanks;

    Origin(String id, List<Reward> rewards, StartingRank... startingRanks) {
        this.id = id;
        this.rewards = rewards;
        this.startingRanks = List.of(startingRanks);
    }

    public List<StartingRank> startingRanks() { return startingRanks; }

    public int startingRank(Skill skill) {
        for (StartingRank starting : startingRanks) {
            if (starting.skill() == skill) return starting.rank();
        }
        return 0;
    }

    public String id() { return id; }
    public List<Reward> rewards() { return rewards; }
    public Component title() { return Component.translatable("origin.cyberneticenhancements." + id); }
    public Component tagline() { return Component.translatable("origin.cyberneticenhancements." + id + ".tagline"); }
    public Component story() { return Component.translatable("origin.cyberneticenhancements." + id + ".story"); }

    /** The attribute bonuses and city perks. */
    public List<Component> basePerks() {
        var lines = new java.util.ArrayList<Component>();
        for (Reward reward : rewards) lines.add(reward.text());
        for (int i = 1; i <= CITY_PERKS; i++)
            lines.add(Component.translatable("origin.cyberneticenhancements." + id + ".perk." + i, Origins.perkArguments(this, i)));
        return List.copyOf(lines);
    }

    public List<Component> perks() {
        if (!SkillsConfig.enabled()) return basePerks();
        var lines = new java.util.ArrayList<>(basePerks());
        var ranks = ComponentUtils.formatList(startingRanks, entry -> Component.translatable(
                "screen.cyberneticenhancements.origin.starting_rank", entry.skill().title(),
                Math.min(entry.rank(), SkillsConfig.maxRank())));
        lines.add(Component.translatable("screen.cyberneticenhancements.origin.starting_skills", ranks));
        return List.copyOf(lines);
    }

    public static Origin find(String id) {
        for (Origin origin : values()) if (origin.id.equals(id)) return origin;
        return null;
    }
}
