package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ArticleSection;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiEntry;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ReferenceGroup;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ReferenceTableVisual;
import de.artemis.cyberneticenhancements.client.ui.WikiArticleLayout.Block;
import de.artemis.cyberneticenhancements.common.economy.EconomyBalance;
import de.artemis.cyberneticenhancements.common.fabrication.FabricationContent;
import de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog;
import de.artemis.cyberneticenhancements.common.progression.ExperienceRewards;
import de.artemis.cyberneticenhancements.common.quest.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QuestWikiEntries {
    private QuestWikiEntries() {}

    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("wiki.cyberneticenhancements.quest_guide." + key, args);
    }

    static List<WikiEntry> build() {
        List<WikiEntry> entries = new ArrayList<>();
        entries.add(new WikiEntry("quest_guide_overview", text("overview"), new ItemStack(Items.WRITABLE_BOOK),
                text("badge"), Component.empty(), List.of(text("rating_note", stars(1), stars(5))), List.of(
                section("basics", List.of(text("limits"), text("tiers"), text("urgent"), text("issuer_note"))),
                section("rewards", List.of(text("guaranteed"), text("discovery_rule"), text("delivery"))),
                section("risk", List.of(text("failure"), text("rating_explained"))))));
        for (QuestType type : QuestType.values()) if (type != QuestType.LOCATE) entries.add(entry(type, false)); // Encounter quests are covered under Encounters.
        entries.add(entry(QuestType.INVESTIGATION, true));
        return List.copyOf(entries);
    }

    private static WikiEntry entry(QuestType type, boolean survey) {
        String id = survey ? "chromium_survey" : type.id();
        var issuers = QuestGuide.issuers(type, survey);
        MutableComponent names = Component.empty();
        for (var npc : issuers) {
            if (!names.getString().isEmpty()) names.append(Component.literal(", "));
            names.append(npc.displayName());
        }
        List<ArticleSection> sections = new ArrayList<>();
        sections.add(section("objective", List.of(text("objective." + id),
                text(type.returnsItems() ? "turn_in_person" : type.requiresInPersonTurnIn() ? "turn_in_report" : "turn_in_remote"))));
        sections.add(section("contacts", List.of(text("issuers", names))));
        for (int tier = 1; tier <= 3; tier++) {
            List<Block> guaranteed = new ArrayList<>();
            List<Block> bonus = new ArrayList<>();
            List<Component> guaranteedNotes = new ArrayList<>();
            List<Component> bonusNotes = new ArrayList<>();
            int difficulty = QuestGuide.difficulty(type, tier);
            for (var npc : issuers) {
                guaranteed.add(new Block(npc.displayName(), text("amount", QuestGuide.money(type, tier, npc, false),
                        QuestGuide.money(type, tier, npc, true))));
            }
            guaranteed.add(new Block(text("trust_xp_label"), text("trust_xp_value",
                    EconomyBalance.tier("trust.quest." + type.id(), tier), ExperienceRewards.quest(type, tier))));
            if (type == QuestType.RECOVERY) guaranteedNotes.add(text("compensation"));
            if (type == QuestType.ELIMINATION) guaranteedNotes.add(text("targets", QuestContract.targetCountFor(type, tier, 0)));
            int kind = survey ? QuestContract.REWARD_CHROMIUM : QuestContract.rewardItemKindForTier(tier, type);
            ItemStack supply = QuestContract.rewardItemStack(kind, survey ? 4 : QuestContract.rewardItemCountFor(kind));
            guaranteed.add(new Block(text("supply_label"), supply.isEmpty() ? text("none") : text("item_amount", supply.getCount(), supply.getHoverName())));
            if (survey) guaranteedNotes.add(text("survey_marker"));
            var odds = QuestGuide.odds(type == QuestType.CYBERPSYCHO && tier == 3
                    ? EconomyBalance.value("discovery.psycho_core") : 0, EconomyBalance.value("discovery." + type.id()));
            if (type == QuestType.CYBERPSYCHO && tier == 3) {
                bonus.add(new Block(text("core_label"), Component.literal(percent(odds.core()))));
                for (String core : List.of("proprietary_neural_core", "military_actuator_core", "experimental_control_core"))
                    bonusNotes.add(text("core_item", percent(odds.core() / 3), new ItemStack(FabricationContent.part(core)).getHoverName()));
            }
            int designTier = type == QuestType.CYBERPSYCHO && tier == 3 ? 5 : Math.min(4, tier + 1);
            int eligible = (int) FabricationContent.SCHEMATICS.keySet().stream()
                    .filter(design -> CyberwareCatalog.get(design).tier().ordinal() + 1 <= designTier)
                    .filter(design -> !SchematicKnowledge.clientKnows(design)).count();
            if (eligible > 0) {
                bonus.add(new Block(text("schematic_label", designTier), Component.literal(percent(odds.discovery()))));
                bonusNotes.add(text("pool_summary", eligible, percent(odds.discovery() / eligible)));
            } else {
                ItemStack fallback = new ItemStack(FabricationContent.part(tier >= 3 ? "reinforced_alloy" : "neural_processor"));
                bonus.add(new Block(fallback.getHoverName(), Component.literal(percent(odds.discovery()))));
                bonusNotes.add(text("fallback_note"));
            }
            bonus.add(new Block(text("no_bonus_label"), Component.literal(percent(odds.none()))));
            sections.add(new ArticleSection(text("tier", tier, stars(difficulty)), List.of(),
                    new ReferenceTableVisual(List.of(
                            new ReferenceGroup(text("guaranteed_heading"), guaranteed, guaranteedNotes),
                            new ReferenceGroup(text("bonus_heading"), bonus, bonusNotes)))));
        }
        return new WikiEntry("quest_guide_" + id,
                Component.translatable("quest.cyberneticenhancements." + (survey ? "title.chromium_survey" : "type." + type.id())),
                new ItemStack(switch (type) {
                    case DELIVERY -> Items.CHEST;
                    case EXTRACTION -> Items.ENDER_PEARL;
                    case INVESTIGATION -> Items.COMPASS;
                    case BREACH -> Items.TRIPWIRE_HOOK;
                    case ELIMINATION -> Items.IRON_SWORD;
                    case RECOVERY -> Items.IRON_INGOT;
                    case CYBERPSYCHO -> Items.NETHERITE_SWORD;
                    case LOCATE -> Items.RECOVERY_COMPASS;
                }), text("badge"), Component.empty(),
                List.of(text("rating_range", stars(QuestGuide.difficulty(type, 1)), stars(QuestGuide.difficulty(type, 3)))),
                List.copyOf(sections));
    }

    private static ArticleSection section(String title, List<Component> lines) {
        return new ArticleSection(text(title), lines, null);
    }

    private static Component stars(int count) {
        return Component.literal("★".repeat(count)).withStyle(style -> style.withColor(
                        de.artemis.cyberneticenhancements.client.ui.UiRenderer.ACCENT & 0xFFFFFF))
                .append(Component.literal("☆".repeat(5 - count)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "") + "%";
    }
}
