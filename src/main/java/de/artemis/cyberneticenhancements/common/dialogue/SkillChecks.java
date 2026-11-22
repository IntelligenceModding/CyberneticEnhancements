package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.config.SkillsConfig;
import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Node;
import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Option;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.skills.Skill;
import de.artemis.cyberneticenhancements.common.skills.Skills;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Answers that need a skill. There are no dice: a check passes if the rank is high enough, so what an answer needs is always
 * on show. The rank is looked at again on the server when the answer is chosen, because the client only decides how it is
 * drawn. A check never stands between the player and finishing a contract; it only opens an extra line and a little trust.
 */
public final class SkillChecks {
    /** Trust points an answer is worth. They count against the same daily limit as trading, so they cannot be farmed. */
    private static final int TRUST = 2;

    private record Check(Skill skill, int rank) {}

    private SkillChecks() {}

    private static List<Check> checksFor(NpcType type) {
        return switch (type) {
            case FIXER -> List.of(new Check(Skill.CHARISMA, 4), new Check(Skill.INTIMIDATION, 4), new Check(Skill.STREETWISE, 3), new Check(Skill.DECEPTION, 4));
            case RIPPERDOC -> List.of(new Check(Skill.RIPPERDOC_RAPPORT, 4), new Check(Skill.MAINTENANCE, 4), new Check(Skill.NEURAL_RESISTANCE, 4),
                    new Check(Skill.CHROME_TOLERANCE, 4), new Check(Skill.STREET_MEDICINE, 4));
            default -> List.of();
        };
    }

    /** The rank this kind of contact asks of a skill for an extra answer, or 0 when it asks nothing of it. */
    public static int rankNeeded(NpcType type, Skill skill) {
        return checksFor(type).stream().filter(check -> check.skill == skill).mapToInt(Check::rank).findFirst().orElse(0);
    }

    public static List<Skill> unlockedSkills(NpcType type, Player player) {
        if (!SkillsConfig.enabled()) return List.of();
        return checksFor(type).stream().filter(check -> Skills.meets(player, check.skill, check.rank))
                .map(Check::skill).toList();
    }

    /** Whether this kind of contact has anything to ask of a skill. */
    public static boolean offeredBy(AbstractCityNpcEntity npc) {
        return SkillsConfig.enabled() && !checksFor(npc.npcType()).isEmpty();
    }

    /** The entry that leads to the skill answers, to be put on the contact's first question. */
    public static Option entry(AbstractCityNpcEntity npc, Supplier<Node> back) {
        return Option.of(Component.translatable("screen.cyberneticenhancements.npc.dialogue.choice.skills"),
                player -> offeredBy(npc) && npc.canTakeCall(player)
                        ? menu(player, npc, back, Component.translatable("screen.cyberneticenhancements.npc.dialogue.skills.intro")) : null);
    }

    private static Node menu(ServerPlayer player, AbstractCityNpcEntity npc, Supplier<Node> back, Component line) {
        var options = new ArrayList<Option>();
        for (Check check : checksFor(npc.npcType())) options.add(option(player, npc, check, back));
        options.add(Option.of(Component.translatable("screen.cyberneticenhancements.npc.dialogue.skills.back"), p -> back.get()));
        return new Node(npc.getName(), line, options, p -> back.get());
    }

    private static Option option(ServerPlayer player, AbstractCityNpcEntity npc, Check check, Supplier<Node> back) {
        Component label = Component.translatable("screen.cyberneticenhancements.npc.dialogue.skill_check", check.skill.title(), check.rank,
                Component.translatable("screen.cyberneticenhancements.npc.dialogue.check." + check.skill.key() + ".label"));
        if (!Skills.meets(player, check.skill, check.rank))
            return Option.locked(label, Component.translatable("screen.cyberneticenhancements.npc.dialogue.skill_needed", check.skill.title(), check.rank));
        // Drawn open here, and looked at again when it is chosen: the client's idea of the rank is never trusted.
        return Option.of(label, chooser -> {
            if (!SkillsConfig.enabled() || !Skills.meets(chooser, check.skill, check.rank) || !npc.canTakeCall(chooser)) return null;
            npc.rewardCommerceTrust(chooser, TRUST);
            return menu(chooser, npc, back, Component.translatable("screen.cyberneticenhancements.npc.dialogue.check." + check.skill.key() + ".pass"));
        });
    }
}
