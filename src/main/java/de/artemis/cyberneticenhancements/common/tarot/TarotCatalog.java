package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/** Stable identifiers are also the player's saved collection keys. */
public final class TarotCatalog {
    public static final int TEXTURE_WIDTH = 160, TEXTURE_HEIGHT = 240;
    public static final float ASPECT_RATIO = (float) TEXTURE_WIDTH / TEXTURE_HEIGHT;
    public enum BonusType {
        HEALTH("health", .5, false), HEADROOM("headroom", 1, false),
        ARMOR("armor", 1, false), ARMOR_TOUGHNESS("armor_toughness", 1, false),
        ATTACK_DAMAGE("attack_damage", 1, false), ATTACK_SPEED("attack_speed", 100, true),
        MOVEMENT_SPEED("movement_speed", 100, true), LUCK("luck", 1, false),
        MINING_SPEED("mining_speed", 100, true), BLOCK_REACH("block_reach", 1, false),
        KNOCKBACK_RESISTANCE("knockback_resistance", 100, true), SAFE_FALL_DISTANCE("safe_fall_distance", 1, false);

        private final String id;
        private final double displayScale;
        private final boolean percent;
        BonusType(String id, double displayScale, boolean percent) {
            this.id = id; this.displayScale = displayScale; this.percent = percent;
        }
        public String id() { return id; }
        public boolean multipliesBase() { return this == ATTACK_SPEED || this == MOVEMENT_SPEED || this == MINING_SPEED; }
        public Component label() { return Component.translatable("tarot.cyberneticenhancements.bonus." + id); }
        public String format(double amount) {
            return BigDecimal.valueOf(amount * displayScale).setScale(6, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString() + (percent ? "%" : "");
        }
    }

    public record Reward(BonusType type, double amount) {
        public Reward {
            java.util.Objects.requireNonNull(type);
            if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Tarot rewards must be positive and finite");
        }
        public Component text() { return Component.translatable("tarot.cyberneticenhancements.reward." + type.id(), type.format(amount)); }
    }
    public record Card(String id, int number, String displayName, String descriptionText, List<Reward> rewards) {
        public Card {
            rewards = List.copyOf(rewards);
            if (rewards.size() != 2 || rewards.get(0).type() == rewards.get(1).type())
                throw new IllegalArgumentException("Every tarot card needs two different reward types");
        }
        public Card(String id, int number, String displayName, String descriptionText, BonusType firstType, double firstAmount, BonusType secondType, double secondAmount) {
            this(id, number, displayName, descriptionText, List.of(new Reward(firstType, firstAmount), new Reward(secondType, secondAmount)));
        }
        public Component name() { return Component.translatable("tarot.cyberneticenhancements." + id); }
        public Component description() { return Component.translatable("tarot.cyberneticenhancements." + id + ".description"); }
        public Component reward() { return Component.empty().append(rewards.get(0).text()).append("  |  ").append(rewards.get(1).text()); }
        public ResourceLocation texture() { return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/tarot/" + id + ".png"); }
    }

    public static final List<Card> CARDS = List.of(
            new Card("the_fool", 0, "The Fool",
                    "The Fool is everyone - including you and me.\n\nEach step he takes on his journey feels like stepping into a brave new world.\n\nUltimately, the journey will change him. But as the card shows, he's a trustworthy lad whose tireless hope drives him toward his goal.", BonusType.HEALTH, 1, BonusType.SAFE_FALL_DISTANCE, 0.5),
            new Card("the_magician", 1, "The Magician",
                    "The Magician is the card of self-confidence and adapting to situations through intellect and sheer will.\n\nThe Magician is a schemer who always has one last trick up his sleeve, who despite everything manages to stay afloat and remain in control of his own destiny.", BonusType.HEADROOM, 5, BonusType.BLOCK_REACH, 0.1),
            new Card("the_high_priestess", 2, "The High Priestess",
                    "The High Priestess is a card of mystery.\n\nIt shows how all our secrets hang by a delicate thread and the struggle between common sense and intuition.\n\nThe High Priestess symbolizes the cold, calm waters as well as the mysteries hidden in their depths.", BonusType.HEADROOM, 5, BonusType.LUCK, 0.25),
            new Card("the_empress", 3, "The Empress",
                    "The empress is a card of femininity and motherhood.\n\nShe is authoritative, knows what she wants, and exhibits refinement and sensuousness.\n\nThe Empress symbolizes creativity and growth, and instructs us not to dismiss our unconscious impulses, but to trust our intuition.", BonusType.HEALTH, 1, BonusType.ARMOR, 0.5),
            new Card("the_emperor", 4, "The Emperor",
                    "The Emperor represents patriarchal control and is pleased with the authority and power he possesses to shape the future.\n\nThe Emperor makes the rules and enforces them for the common good.\n\nBut prestige has its dark sides – the Emperor is dominating and ruthless and will climb over a mountain of bodies to achieve his aim.", BonusType.KNOCKBACK_RESISTANCE, 0.01, BonusType.HEALTH, 1),
            new Card("the_hierophant", 5, "The Hierophant",
                    "The Hierophant symbolizes a respect for tradition.\n\nIt represents one who tries to maintain the established order, even though their very character is shaped by it.\n\nThe Hierophant places his faith in institutions – for the alternative is pure chaos. Only by placing his faith in order can he draw strength.", BonusType.HEADROOM, 5, BonusType.MINING_SPEED, 0.01),
            new Card("the_lovers", 6, "The Lovers",
                    "The Lovers is the card of dichotomies.\n\nIt points to the contradictions that clash within each of us and of the challenge of striking a balance between extremes.\n\nThe Lovers is also the card of dilemmas, like The Fool who stands at the crossroads, unable to make his choice.", BonusType.HEALTH, 1, BonusType.HEADROOM, 5),
            new Card("the_chariot", 7, "The Chariot",
                    "The Chariot is always charging ahead despite being pulled by its steeds in opposite directions.\n\nThe rider who steers it constantly reins in the light and dark sides of the soul with the help of Reason.\n\nTo ride in The Chariot is to experience highs and lows – ups and downs.", BonusType.MOVEMENT_SPEED, 0.005, BonusType.KNOCKBACK_RESISTANCE, 0.01),
            new Card("strength", 8, "Strength",
                    "Strength is the card of resilience.\n\nIt is associated with determination, bravery and internal struggle. One must have dedication in order to overcome obstacles and reach one's goal.\n\nStrength is about physical prowess and spiritual fortitude – the power that must be unleashed to achieve the impossible.", BonusType.ATTACK_DAMAGE, 0.25, BonusType.ARMOR_TOUGHNESS, 0.5),
            new Card("the_hermit", 9, "The Hermit",
                    "The Hermit is a card of self-imposed isolation.\n\nIt represents an escape from the hustle and bustle of the city, a turning away from constant newness toward Ye Olde ways.\n\nFor the hermit, solitude is the road to the sublime – a road that is taken not with great bounds and strides, but with small, concentrated steps.", BonusType.HEADROOM, 5, BonusType.MINING_SPEED, 0.01),
            new Card("wheel_of_fortune", 10, "Wheel of Fortune",
                    "The Wheel of Fortune means that change is coming.\n\nOne's destiny could turn out for the better or the worse, yet it also bears the promise of new possibilities.\n\nThe Wheel reminds us that nobody remains at the top forever, but also that not every situation is hopeless.", BonusType.LUCK, 0.25, BonusType.HEADROOM, 5),
            new Card("justice", 11, "Justice",
                    "Justice is the card of conflict resolution.\n\nIt proclaims the need for order, to see through lies and deceit, and a return to the natural state of affairs.\n\nJustice implies a just sentence, but also due process.", BonusType.ARMOR, 0.5, BonusType.ATTACK_DAMAGE, 0.25),
            new Card("the_hanged_man", 12, "The Hanged Man",
                    "The Hanged Man is the card of sacrifice.\n\nIt says that a price must be paid in order to achieve enlightenment.\n\nThe Hanged Man's forsakenness opens the path for rebirth into a new life, though this path is wrought with pain suspended in time and ultimately ends in death.", BonusType.SAFE_FALL_DISTANCE, 0.5, BonusType.MOVEMENT_SPEED, 0.005),
            new Card("death", 13, "Death",
                    "Death is the card of becoming.\n\nIt signifies an imminent and difficult transition – the conclusion of one phase of life and the beginning of another.\n\nInevitably, something gets lost during the transformation, but something else will rise and take its place.", BonusType.ATTACK_DAMAGE, 0.25, BonusType.ATTACK_SPEED, 0.01),
            new Card("temperance", 14, "Temperance",
                    "Temperance is the card of balance.\n\nIt may symbolize self-restraint or the gradual shift toward a more mature state of equilibrium.\n\nTemperance is associated with being in control of oneself as well as the desire to achieve inner peace.", BonusType.HEALTH, 1, BonusType.KNOCKBACK_RESISTANCE, 0.01),
            new Card("the_devil", 15, "The Devil",
                    "The Devil is addiction, craving and passion.\n\nHe brings fame and fortune, but at the price of losing oneself to a world of material distractions.\n\nThe Devil lures unsuspecting souls into traps, but always grants them a choice. One can try their luck and take him up on his offer, but one should always know when to call quits.", BonusType.ATTACK_SPEED, 0.01, BonusType.HEADROOM, 5),
            new Card("the_tower", 16, "The Tower",
                    "The Tower is an omen of radical change, chaos and destruction.\n\nThe lightning striking The Tower signifies a return to the old order that lies buried under the ruins, and a new order that will rise from it.\n\nIt is a symbol of tragedy, apocalypse, and self-destruction.", BonusType.ARMOR_TOUGHNESS, 0.5, BonusType.HEALTH, 1),
            new Card("the_star", 17, "The Star",
                    "The Star is the card of hope.\n\nIn the darkest of nights there is a light that shines the path to home.\n\nThe Star is inspiration, motivation and gives us strength to move forward.", BonusType.LUCK, 0.25, BonusType.SAFE_FALL_DISTANCE, 0.5),
            new Card("the_moon", 18, "The Moon",
                    "The Moon reminds us that reality is not always what it seems at first glance.\n\nIn a world of appearances and illusions, the best course is often charted by one's own intuition.\n\nThe Moon is also the card of dreams, desires, and of course, sleep – Death's nightly ritual.", BonusType.MOVEMENT_SPEED, 0.005, BonusType.BLOCK_REACH, 0.1),
            new Card("the_sun", 19, "The Sun",
                    "The Sun symbolizes success. It is a card of freedom, renewal and a bright future that lies ahead.\n\nThe Sun also represents truth, for its light will always pull back the curtain of shadow that hides the world's secrets.\n\nIt is also represents greatness and splendor.", BonusType.HEALTH, 1, BonusType.LUCK, 0.25),
            new Card("judgement", 20, "Judgement",
                    "Judgement is the card of renewal.\n\nThe angel blowing into the horn heralds resurrection and liberation.\n\nThis card foretells an important change that will result in healing or fulfillment. It is also a symbol of self-worth.", BonusType.ARMOR, 0.5, BonusType.ARMOR_TOUGHNESS, 0.5),
            new Card("the_world", 21, "The World",
                    "The World lies at the end of The Fool's long and winding journey.\n\nWiser and more world-weary than he started out, the Fool faces a moment of reckoning. Some of us accept where our journey has led us to, while others embark on a new challenge.\n\nOne thing in this World is certain – you can't have it all.", BonusType.HEADROOM, 5, BonusType.HEALTH, 1),
            new Card("page_of_wands", 22, "Page of Wands",
                    "The Page of Wands lives for sparks of inspiration and the promise of new opportunities.\n\nGrowth is the only constant; change - the only rule.\n\nTheir journey is messy, imperfect, but always high-spirited. In the craters of ruin they sow all they can, knowing whatever sprouts may be the beginning of something revolutionary.", BonusType.SAFE_FALL_DISTANCE, 0.5, BonusType.MOVEMENT_SPEED, 0.005),
            new Card("knight_of_wands", 23, "Knight of Wands",
                    "The Knight of Wands does not wait for permission. The presence of obstacles only proves they're on the right path.\n\nBeneath an outward swagger lies a genuine hunger for greatness, and fear takes a back seat when adventure calls.\n\nThey leap before looking, but even when they fall they never stop moving forward.", BonusType.ATTACK_DAMAGE, 0.25, BonusType.ATTACK_SPEED, 0.01),
            new Card("queen_of_wands", 24, "Queen of Wands",
                    "The Queen of Wands stands firm when others waver and flee. She is both bulwark and spearhead.\n\nHer courage isn't loud, but it echoes with a spirit of independence that endures long after the noise fades.\n\nDrawing this card marks a moment when one must protect what matters most.", BonusType.BLOCK_REACH, 0.1, BonusType.MINING_SPEED, 0.01),
            new Card("king_of_wands", 25, "King of Wands",
                    "The master of planning and unorthodox ideas.\n\nAn individual who brings people together and radiates inspiration to those they consider friends.\n\nHowever, their enemies will soon find they are an impulsive, devastating force of nature, capable of breaking oaths and crossing any line, just to see their goals achieved.", BonusType.BLOCK_REACH, 0.1, BonusType.ATTACK_DAMAGE, 0.25),
            new Card("page_of_cups", 26, "Page of Cups",
                    "The Page of Cups drifts through the world with open eyes and an open heart.\n\nTo some, they appear naive; to others, virtuous. This card represents the youthfulness of one's spirit, their hopes, instincts and ideals.\n\nTheir dreams today will reshape their tomorrow, assuming they survive the arrows of fate.", BonusType.LUCK, 0.25, BonusType.HEALTH, 1),
            new Card("knight_of_cups", 27, "Knight of Cups",
                    "The Knight of Cups is a seeker with stars in their eyes and a burning fire in their chest.\n\nThey thrive on taking action, driven not by logic but by longing. This is a card of newfound vision and ideas that stir the soul.\n\nWhen the world shatters their heart, the reckoning may echo violently.", BonusType.HEALTH, 1, BonusType.LUCK, 0.25),
            new Card("queen_of_cups", 28, "Queen of Cups",
                    "The Queen of Cups is a wellspring of calm.\n\nShe listens more than she speaks, understands more than she reveals, and sees what others bury deep.\n\nIn her presence, broken spirits can learn to hope again.", BonusType.HEADROOM, 5, BonusType.ARMOR, 0.5),
            new Card("king_of_cups", 29, "King of Cups",
                    "A King of Cups is a creative and emotionally-driven person.\n\nThey are strong individuals, who realize the importance of feelings. Intuition helps them navigate their lives.\n\nWhen fate is in their favor, compassion guides their actions. When they're at odds with fate, it'd be unwise to trust them.", BonusType.ARMOR_TOUGHNESS, 0.5, BonusType.HEADROOM, 5),
            new Card("page_of_swords", 30, "Page of Swords",
                    "The Page of Swords never drops their guard.\n\nCurious and calculating, their inquisitive and innovative nature defines their trajectory and the fortunes of those around them.\n\nThis card signifies an approaching beginning that requires careful consideration of all pathways and outcomes. When preparing for anything, one's awareness becomes their armor.", BonusType.MOVEMENT_SPEED, 0.005, BonusType.BLOCK_REACH, 0.1),
            new Card("knight_of_swords", 31, "Knight of Swords",
                    "The Knight of Swords is a harbinger of dramatic change.\n\nIt signifies leaving the comfort of the known world for new ventures without looking back, no matter the risk. It represents one who values honesty and clarity above all else.\n\nTheir forthright nature can sometimes sting; yet, this aversion to deceit should be considered a gift for friend and foe alike.", BonusType.ATTACK_SPEED, 0.01, BonusType.KNOCKBACK_RESISTANCE, 0.01),
            new Card("queen_of_swords", 32, "Queen of Swords",
                    "The Queen of Swords rules with her razor wit and a tongue that cuts deeper than folded steel.\n\nShe seeks truth in all things and will not tolerate deceit. This is a card of clear-eyed independence, wisdom, intellect, and an ability to see through illusions.\n\nShe walks alone, not because she must, but because few can keep her pace and embrace life without the protection of a mask.", BonusType.ATTACK_SPEED, 0.01, BonusType.ARMOR_TOUGHNESS, 0.5),
            new Card("king_of_swords", 33, "King of Swords",
                    "Two things matter above all else for a King of Swords: logic and conviction.\n\nThey represent a person with a precise moral compass, who is known for their caring demeanor.\n\nHowever, enemies beware - the King of Swords is a ruthless opponent, who will not rest until you are punished and left to be torn apart by hounds of fate.", BonusType.ARMOR, 0.5, BonusType.ATTACK_SPEED, 0.01),
            new Card("page_of_pentacles", 34, "Page of Pentacles",
                    "For the Page of Pentacles, every horizon is the gateway to a new beginning.\n\nThey inevitably make mistakes and burn bridges, but never lose faith that they are working toward something better.\n\nDrawing this card signals a moment when fear of the future dissipates, revealing a promising plan.", BonusType.MINING_SPEED, 0.01, BonusType.HEADROOM, 5),
            new Card("knight_of_pentacles", 35, "Knight of Pentacles",
                    "When a line is crossed, the Knight of Pentacles is who you want standing at your side.\n\nThis card represents those who protect what is theirs. The Knight's stubborn nature does not serve the pursuit of new glory or fame, but the safeguarding of legacy.\n\nIt also symbolizes the preservation of songs written in one's own blood.", BonusType.KNOCKBACK_RESISTANCE, 0.01, BonusType.SAFE_FALL_DISTANCE, 0.5),
            new Card("queen_of_pentacles", 36, "Queen of Pentacles",
                    "The Queen of Pentacles lives boldly and wears luxury like a suit of armor.\n\nShe opens her doors without fear, offering comfort, generosity and connection.\n\nShe knows what it means to lose oneself and find another standing in their place. This card represents a return to something real.", BonusType.HEADROOM, 5, BonusType.MINING_SPEED, 0.01),
            new Card("king_of_pentacles", 37, "King of Pentacles",
                    "The King of Pentacles is imperious.\n\nThey represent enterprise, pragmatism, but also attachment.\n\nFrom such a person one can expect coldness or attachment to material possessions; they are likely to turn your world upside down sooner or later.", BonusType.MINING_SPEED, 0.01, BonusType.ARMOR, 0.5)
    );

    public static Card find(String id) { return CARDS.stream().filter(card -> card.id().equals(id)).findFirst().orElse(null); }
    /** Native attribute units; BonusType.format converts health to hearts and speed to percentages. */
    public static double total(BonusType type, Predicate<String> included) {
        return CARDS.stream().filter(card -> included.test(card.id())).flatMap(card -> card.rewards().stream())
                .filter(reward -> reward.type() == type).mapToDouble(Reward::amount).sum();
    }
    public static Card forStructure(long seed, String key) {
        RandomSource random = RandomSource.create(seed ^ (long) key.hashCode() * 0xD1B54A32D192ED03L);
        return CARDS.get(random.nextInt(CARDS.size()));
    }
    private TarotCatalog() {}
}
