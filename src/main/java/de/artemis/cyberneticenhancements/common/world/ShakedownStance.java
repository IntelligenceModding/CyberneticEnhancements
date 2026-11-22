package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.streetcred.StreetCred;
import net.minecraft.world.entity.player.Player;

/**
 * How a gang sizes a player up, which is set by their Street Cred. A nobody is easy money and is treated like it: a bigger
 * gang, a heavier toll, a short fuse and nothing to bargain with. A name changes the whole encounter: the gang shrinks, the
 * toll falls and there is time to think, but the people who come for a known name are better armed. At the top they do not come
 * to take at all, they come to pay their respects.
 */
public enum ShakedownStance {
    /** Levels 1-19. */
    PREY("prey", 1, 4, 0.60F, 15, 40, 600, 20, 0.5, 0, 0.85F, 0),
    /** Levels 20-49. */
    STREET("street", 20, 3, 0.40F, 10, 25, 400, 30, 1, 0, 1.0F, 0),
    /** Levels 50-79. */
    KNOWN("known", 50, 2, 0.30F, 6, 15, 200, 45, 1, 0.15, 1.3F, 0),
    /** Levels 80-100. */
    FEARED("feared", 80, 2, 0.30F, 3, 10, 100, 60, 1, 0.25, 1.7F, 0.6);

    /** What the answers that need a name ask for. */
    public static final int NAME_LEVEL = 20, STARE_LEVEL = 50;

    private final String key;
    private final int minLevel, size, percent, minDemand, maxDemand, answerSeconds;
    private final float chance, power;
    private final double chrome, bluffBonus, tributeChance;

    ShakedownStance(String key, int minLevel, int size, float chance, int percent, int minDemand, int maxDemand, int answerSeconds,
                    double chrome, double bluffBonus, float power, double tributeChance) {
        this.key = key; this.minLevel = minLevel; this.size = size; this.chance = chance; this.percent = percent;
        this.minDemand = minDemand; this.maxDemand = maxDemand; this.answerSeconds = answerSeconds; this.chrome = chrome;
        this.bluffBonus = bluffBonus; this.power = power; this.tributeChance = tributeChance;
    }

    public static ShakedownStance of(int level) {
        ShakedownStance found = PREY;
        for (var stance : values()) if (level >= stance.minLevel) found = stance;
        return found;
    }

    public static ShakedownStance of(Player player) { return of(StreetCred.level(player)); }

    /** Lang key part for this stance. */
    public String key() { return key; }
    /** How many come, the boss included. */
    public int size() { return size; }
    /** How likely a street encounter is a shakedown at all, rather than a wandering ripperdoc. */
    public float chance() { return chance; }
    /** How much harder than a plain thug each of them hits and takes hits. */
    public float power() { return power; }
    /** How likely the gang comes to pay tribute instead of demanding a toll. */
    public double tributeChance() { return tributeChance; }
    public int answerTicks() { return answerSeconds * 20; }
    public int answerSeconds() { return answerSeconds; }
    public int percent() { return percent; }
    public int minDemand() { return minDemand; }
    public int maxDemand() { return maxDemand; }
    public double bluffBonus() { return bluffBonus; }

    /** The toll: a share of the player's money, kept within a range that suits who is being asked. */
    public int demand(int balance) { return Math.clamp(balance * (long) percent / 100, minDemand, maxDemand); }

    /** What a gang that has come to pay its respects hands over. */
    public static int tribute(int level) { return (int) Math.clamp(level * 4L, 100, 400); }

    /** Chance that a refusal is taken seriously: chrome on show counts (less to a gang that sees a nobody), and so does a name. */
    public double bluffChance(int installedCount) { return Math.clamp(installedCount * 0.12 * chrome + bluffBonus, 0, 0.9); }

    /** Chance that dropping a name makes the gang back down. Nobody has heard of a nobody; a legend barely has to say it. */
    public static double nameDropChance(int level) { return level < NAME_LEVEL ? 0 : Math.clamp((level - 10) / 100.0, 0, 0.9); }
}
