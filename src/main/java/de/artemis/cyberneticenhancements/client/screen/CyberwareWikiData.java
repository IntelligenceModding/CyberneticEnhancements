package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCategory;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableDefinition;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableProfile;
import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusType;
import de.artemis.cyberneticenhancements.common.cyberware.CirculatoryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAcquisitionMethod;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareBalance;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleCategory;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecycleHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServicePlan;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlotType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper;
import de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.FrontalCortexManager;
import de.artemis.cyberneticenhancements.common.cyberware.HandsCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.IntegumentaryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.LegCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.NervousSystemCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.SkeletonCyberwareManager;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CyberwareWikiData {
    private static final String WIKI_KEY = "wiki.cyberneticenhancements.";
    private static final Map<String, List<RelicCacheLootSource>> RELIC_CACHE_SOURCES = createRelicCacheSources();

    private CyberwareWikiData() {
    }

    private static Component wiki(String suffix, Object... args) {
        return Component.translatable(WIKI_KEY + suffix, args);
    }

    static List<ArchiveTab> buildArchiveTabs() {
        return List.of(
                new ArchiveTab(
                        "wiki",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.wiki"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.wiki.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.wiki"),
                        buildWikiTopics()
                ),
                new ArchiveTab(
                        "banking",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.banking"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.banking.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.banking"),
                        List.of()
                ),
                new ArchiveTab(
                        "contacts",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.contacts"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.contacts.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.contacts"),
                        List.of()
                ),
                new ArchiveTab(
                        "lore",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.lore"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.lore.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.lore"),
                        buildLoreTopics()
                ),
                new ArchiveTab(
                        "quests",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.quests"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.quests.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.quests"),
                        buildQuestTopics()
                )
        );
    }

    private static List<WikiTopic> buildWikiTopics() {
        return List.of(
                new WikiTopic("cyberware", Component.translatable("wiki.cyberneticenhancements.topic.cyberware"), buildCyberwareTopics(), null),
                new WikiTopic("modules", Component.translatable("wiki.cyberneticenhancements.topic.modules"), buildModuleTopics(), null),
                new WikiTopic("chipware", Component.translatable("wiki.cyberneticenhancements.topic.chipware"), List.of(), new WikiSection("chipware", Component.translatable("wiki.cyberneticenhancements.topic.chipware"), buildChipwareEntries())),
                new WikiTopic("consumables", Component.translatable("wiki.cyberneticenhancements.topic.consumables"), buildConsumableTopics(), null),
                new WikiTopic("npcs", Component.translatable("wiki.cyberneticenhancements.topic.npcs"), buildNpcTopics(), null),
                new WikiTopic("stations", Component.translatable("wiki.cyberneticenhancements.topic.stations"), buildStationTopics(), null),
                new WikiTopic("systems", Component.translatable("wiki.cyberneticenhancements.topic.systems"), buildSystemTopics(), null)
        );
    }

    private static List<WikiTopic> buildLoreTopics() {
        return List.of(
                new WikiTopic(
                        "lore_world_notes",
                        Component.translatable("wiki.cyberneticenhancements.lore.world_notes"),
                        List.of(),
                        new WikiSection("lore_world_notes", Component.translatable("wiki.cyberneticenhancements.lore.world_notes"), buildLoreEntries())
                )
        );
    }

    private static List<WikiTopic> buildQuestTopics() {
        return List.of(
                new WikiTopic(
                        "quest_contract_board",
                        Component.translatable("wiki.cyberneticenhancements.quests.contract_board"),
                        List.of(),
                        new WikiSection("quest_contract_board", Component.translatable("wiki.cyberneticenhancements.quests.contract_board"), buildQuestEntries())
                )
        );
    }

    private static List<WikiTopic> buildCyberwareTopics() {
        List<WikiTopic> topics = new ArrayList<>();
        for (CyberwareSlotType slotType : CyberwareSlotType.values()) {
            List<WikiEntry> entries = CyberwareCatalog.definitions().stream()
                    .filter(definition -> definition.slotType() == slotType)
                    .sorted(Comparator.comparing(CyberwareDefinition::tier).thenComparing(CyberwareDefinition::displayName))
                    .map(CyberwareWikiData::buildCyberwareEntry)
                    .toList();
            topics.add(new WikiTopic(
                    "cyberware_" + slotType.name().toLowerCase(Locale.ROOT),
                    Component.translatable(slotType.translationKey()),
                    List.of(),
                    new WikiSection("cyberware_" + slotType.name().toLowerCase(Locale.ROOT), Component.translatable(slotType.translationKey()), entries)
            ));
        }
        return topics;
    }

    private static List<WikiTopic> buildModuleTopics() {
        List<WikiTopic> topics = new ArrayList<>();
        for (CyberwareModuleCategory category : CyberwareModuleCategory.values()) {
            List<WikiEntry> entries = CyberwareModuleCatalog.definitions().stream()
                    .filter(definition -> definition.category() == category)
                    .sorted(Comparator.comparing(CyberwareModuleDefinition::tier).thenComparing(CyberwareModuleDefinition::displayName))
                    .map(CyberwareWikiData::buildModuleEntry)
                    .toList();
            topics.add(new WikiTopic(
                    "modules_" + category.name().toLowerCase(Locale.ROOT),
                    Component.translatable(category.translationKey()),
                    List.of(),
                    new WikiSection("modules_" + category.name().toLowerCase(Locale.ROOT), Component.translatable(category.translationKey()), entries)
            ));
        }
        return topics;
    }

    private static List<WikiTopic> buildConsumableTopics() {
        List<WikiTopic> topics = new ArrayList<>();
        for (CyberConsumableCategory category : CyberConsumableCategory.values()) {
            List<WikiEntry> entries = CyberConsumableCatalog.definitions().stream()
                    .filter(definition -> definition.category() == category)
                    .sorted(Comparator.comparing(CyberConsumableDefinition::displayName))
                    .map(CyberwareWikiData::buildConsumableEntry)
                    .toList();
            topics.add(new WikiTopic(
                    "consumables_" + category.name().toLowerCase(Locale.ROOT),
                    Component.translatable(category.translationKey()),
                    List.of(),
                    new WikiSection("consumables_" + category.name().toLowerCase(Locale.ROOT), Component.translatable(category.translationKey()), entries)
            ));
        }
        return topics;
    }

    private static List<WikiTopic> buildStationTopics() {
        return List.of(
                new WikiTopic("station_ripper", Component.translatable("block.cyberneticenhancements.ripper_station"), List.of(), new WikiSection("station_ripper", Component.translatable("block.cyberneticenhancements.ripper_station"), List.of(buildRipperStationEntry()))),
                new WikiTopic("station_tech", Component.translatable("block.cyberneticenhancements.tech_station"), List.of(), new WikiSection("station_tech", Component.translatable("block.cyberneticenhancements.tech_station"), List.of(buildTechStationEntry()))),
                new WikiTopic("station_recycler", Component.translatable("block.cyberneticenhancements.recycler_station"), List.of(), new WikiSection("station_recycler", Component.translatable("block.cyberneticenhancements.recycler_station"), List.of(buildRecyclerStationEntry()))),
                new WikiTopic("station_relic_cache", wiki("station.relic_cache.title"), List.of(), new WikiSection("station_relic_cache", wiki("station.relic_cache.title"), List.of(buildRelicCacheEntry())))
        );
    }

    private static List<WikiTopic> buildNpcTopics() {
        return List.of(
                new WikiTopic(
                        "npc_general",
                        Component.translatable("wiki.cyberneticenhancements.npc.general"),
                        List.of(),
                        new WikiSection("npc_general", Component.translatable("wiki.cyberneticenhancements.npc.general"), List.of(buildNpcGeneralEntry()))
                ),
                new WikiTopic(
                        "npc_fixer",
                        Component.translatable("wiki.cyberneticenhancements.npc.fixer"),
                        List.of(),
                        new WikiSection("npc_fixer", Component.translatable("wiki.cyberneticenhancements.npc.fixer"), List.of(buildFixerEntry()))
                )
        );
    }

    private static List<WikiTopic> buildSystemTopics() {
        return List.of(
                new WikiTopic("system_cyberstrain", Component.translatable("wiki.cyberneticenhancements.system.cyberstrain"), List.of(), new WikiSection("system_cyberstrain", Component.translatable("wiki.cyberneticenhancements.system.cyberstrain"), List.of(buildCyberstrainEntry()))),
                new WikiTopic("system_psychosis", Component.translatable("wiki.cyberneticenhancements.system.cyberpsychosis"), List.of(), new WikiSection("system_psychosis", Component.translatable("wiki.cyberneticenhancements.system.cyberpsychosis"), List.of(buildCyberpsychosisEntry()))),
                new WikiTopic("system_statuses", Component.translatable("wiki.cyberneticenhancements.system.statuses"), List.of(), new WikiSection("system_statuses", Component.translatable("wiki.cyberneticenhancements.system.statuses"), buildStatusEntries())),
                new WikiTopic("system_progression", Component.translatable("wiki.cyberneticenhancements.system.progression"), List.of(), new WikiSection("system_progression", Component.translatable("wiki.cyberneticenhancements.system.progression"), List.of(buildProgressionEntry())))
        );
    }

    private static List<WikiEntry> buildStatusEntries() {
        List<WikiEntry> entries = new ArrayList<>();
        for (CombatStatusType type : CombatStatusType.values()) {
            entries.add(buildStatusEntry(type));
        }
        return entries;
    }

    private static List<WikiEntry> buildLoreEntries() {
        return List.of(
                buildLoreEntry(
                        "lore_world_notes",
                        Component.translatable("wiki.cyberneticenhancements.lore.world_notes"),
                        new ItemStack(Items.WRITABLE_BOOK),
                        wiki("lore.reserved"),
                        List.of(
                                sectionKey("current_state",
                                        wiki("lore.current_state.branch_ready"),
                                        wiki("lore.current_state.gameplay_authority"))
                        )
                )
        );
    }

    private static List<WikiEntry> buildQuestEntries() {
        return List.of(
                buildQuestEntry(
                        "quest_contract_board",
                        Component.translatable("wiki.cyberneticenhancements.quests.contract_board"),
                        new ItemStack(Items.MAP),
                        wiki("quests.reserved"),
                        List.of(
                                sectionKey("current_state",
                                        wiki("quests.current_state.placeholder"),
                                        wiki("quests.current_state.progression")),
                                sectionKey("planned_surface",
                                        wiki("quests.planned_surface.contracts"),
                                        wiki("quests.planned_surface.archive"))
                        )
                )
        );
    }

    private static WikiEntry buildCyberwareEntry(CyberwareDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.cyberware(definition.id()).get());
        List<Component> chips = new ArrayList<>();
        chips.add(Component.translatable(definition.slotType().translationKey()).withStyle(ChatFormatting.AQUA));
        chips.add(Component.translatable(definition.tier().translationKey()).withStyle(definition.tier().getColor()));
        chips.add(Component.translatable(definition.acquisitionMethod().translationKey()));
        chips.add(wiki("chip.chrome", definition.chromeCost()));
        chips.add(wiki("chip.integrity", CyberwareConditionHelper.getMaxIntegrity(definition)));

        List<ArticleSection> sections = new ArrayList<>();
        sections.add(sectionKey("overview",
                Component.translatable(definition.descriptionKey()),
                wiki("overview.slot", Component.translatable(definition.slotType().translationKey())),
                wiki("overview.tier", Component.translatable(definition.tier().translationKey())),
                wiki("overview.acquisition", Component.translatable(definition.acquisitionMethod().translationKey())),
                wiki("overview.chrome_cost", definition.chromeCost()),
                wiki("overview.integrity", CyberwareConditionHelper.getMaxIntegrity(definition))));
        addSection(sections, wiki("section.passive_effects"), buildCyberwarePassiveLines(definition));
        addSection(sections, wiki("section.special_behavior"), buildCyberwareBehaviorLines(definition));
        addSection(sections, wiki("section.acquisition"), buildAcquisitionLines(definition));
        addSection(sections, wiki("section.crafting"), buildCyberwareCraftingLines(definition), buildCyberwareCraftingVisual(definition));
        addSection(sections, wiki("section.repair_upgrade"), buildCyberwareServiceLines(definition), buildCyberwareServiceVisual(definition));
        addSection(sections, wiki("section.recycling"), buildCyberwareRecycleLines(definition), buildCyberwareRecycleVisual(definition));

        return new WikiEntry(
                definition.id(),
                stack.getHoverName(),
                stack,
                Component.translatable("wiki.cyberneticenhancements.badge.cyberware"),
                Component.translatable(definition.descriptionKey()),
                chips,
                sections
        );
    }

    private static WikiEntry buildModuleEntry(CyberwareModuleDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.module(definition.id()).get());
        return new WikiEntry(
                definition.id(),
                stack.getHoverName(),
                stack,
                Component.translatable("wiki.cyberneticenhancements.badge.module"),
                Component.translatable(definition.descriptionKey()),
                List.of(
                        Component.translatable(definition.category().translationKey()).withStyle(ChatFormatting.AQUA),
                        Component.translatable(definition.tier().translationKey()).withStyle(definition.tier().getColor()),
                        wiki("chip.chrome", definition.chromeCost())
                ),
                filterSections(List.of(
                        sectionKey("overview",
                                Component.translatable(definition.descriptionKey()),
                                wiki("overview.category", Component.translatable(definition.category().translationKey())),
                                wiki("overview.tier", Component.translatable(definition.tier().translationKey())),
                                wiki("overview.chrome_cost", definition.chromeCost())),
                        section(wiki("section.passive_effects"), describeEffects(definition.effects())),
                        section(wiki("section.installation"), buildModuleIntegrationLines(definition)),
                        section(wiki("section.acquisition"), buildCraftedItemAcquisitionLines(definition.id())),
                        section(wiki("section.crafting"), buildModuleCraftingLines(definition), buildModuleCraftingVisual(definition)),
                        section(wiki("section.recycling"), buildModuleRecycleLines(definition), buildModuleRecycleVisual(definition))
                ))
        );
    }

    private static WikiEntry buildChipwareEntry(ChipwareDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.chipware(definition.id()).get());
        return new WikiEntry(
                definition.id(),
                stack.getHoverName(),
                stack,
                Component.translatable("wiki.cyberneticenhancements.badge.chipware"),
                Component.translatable(definition.descriptionKey()),
                List.of(
                        Component.translatable(definition.tier().translationKey()).withStyle(definition.tier().getColor()),
                        wiki("chip.chrome", definition.chromeCost())
                ),
                filterSections(List.of(
                        sectionKey("overview",
                                Component.translatable(definition.descriptionKey()),
                                wiki("overview.tier", Component.translatable(definition.tier().translationKey())),
                                wiki("overview.chrome_cost", definition.chromeCost())),
                        section(wiki("section.passive_effects"), describeEffects(definition.effects())),
                        section(wiki("section.installation"), List.of(
                                wiki("chipware.installation.host_only"),
                                wiki("chipware.installation.slot_count")
                        )),
                        section(wiki("section.acquisition"), buildCraftedItemAcquisitionLines(definition.id())),
                        section(wiki("section.crafting"), buildChipwareCraftingLines(definition), buildChipwareCraftingVisual(definition)),
                        section(wiki("section.recycling"), buildChipwareRecycleLines(definition), buildChipwareRecycleVisual(definition))
                ))
        );
    }

    private static List<WikiEntry> buildChipwareEntries() {
        return ChipwareCatalog.definitions().stream()
                .sorted(Comparator.comparing(ChipwareDefinition::tier).thenComparing(ChipwareDefinition::displayName))
                .map(CyberwareWikiData::buildChipwareEntry)
                .toList();
    }

    private static WikiEntry buildConsumableEntry(CyberConsumableDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.consumable(definition.id()).get());
        return new WikiEntry(
                definition.id(),
                stack.getHoverName(),
                stack,
                Component.translatable("wiki.cyberneticenhancements.badge.consumable"),
                Component.translatable(definition.descriptionKey()),
                List.of(
                        Component.translatable(definition.category().translationKey()).withStyle(ChatFormatting.AQUA),
                        wiki("chip.cooldown_short", definition.cooldownTicks() / 20),
                        wiki("chip.overdose_short", definition.overdosePoints())
                ),
                filterSections(List.of(
                        sectionKey("overview",
                                Component.translatable(definition.descriptionKey()),
                                wiki("overview.category", Component.translatable(definition.category().translationKey())),
                                wiki("overview.profile", wiki("profile." + definition.profile().name().toLowerCase(Locale.ROOT))),
                                wiki("overview.cooldown_seconds", definition.cooldownTicks() / 20),
                                wiki("overview.overdose", definition.overdosePoints())),
                        section(wiki("section.effects"), buildConsumableEffectLines(definition)),
                        section(wiki("section.acquisition"), buildCraftedItemAcquisitionLines(definition.id())),
                        section(wiki("section.crafting"), buildConsumableCraftingLines(definition), buildConsumableCraftingVisual(definition)),
                        section(wiki("section.recycling"), buildConsumableRecycleLines(definition), buildConsumableRecycleVisual(definition))
                ))
        );
    }

    private static WikiEntry buildRipperStationEntry() {
        return buildStationEntry(
                "ripper_station",
                Component.translatable("block.cyberneticenhancements.ripper_station"),
                new ItemStack(ModBlocks.RIPPER_STATION.get()),
                wiki("station.ripper.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("station.ripper.overview.1"),
                                wiki("station.ripper.overview.2")),
                        sectionKey("functions",
                                wiki("station.ripper.functions.1"),
                                wiki("station.ripper.functions.2"),
                                wiki("station.ripper.functions.3"),
                                wiki("station.ripper.functions.4")),
                        section(wiki("section.crafting_recipe"), List.of(), buildRipperStationCraftingVisual())
                )
        );
    }

    private static WikiEntry buildTechStationEntry() {
        return buildStationEntry(
                "tech_station",
                Component.translatable("block.cyberneticenhancements.tech_station"),
                new ItemStack(ModBlocks.TECH_STATION.get()),
                wiki("station.tech.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("station.tech.overview.1"),
                                wiki("station.tech.overview.2"),
                                wiki("station.tech.overview.3")),
                        sectionKey("functions",
                                wiki("station.tech.functions.1"),
                                wiki("station.tech.functions.2"),
                                wiki("station.tech.functions.3")),
                        section(wiki("section.crafting_recipe"), List.of(), buildTechStationCraftingVisual())
                )
        );
    }

    private static WikiEntry buildRecyclerStationEntry() {
        return buildStationEntry(
                "recycler_station",
                Component.translatable("block.cyberneticenhancements.recycler_station"),
                new ItemStack(ModBlocks.RECYCLER_STATION.get()),
                wiki("station.recycler.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("station.recycler.overview.1"),
                                wiki("station.recycler.overview.2")),
                        sectionKey("functions",
                                wiki("station.recycler.functions.1"),
                                wiki("station.recycler.functions.2"),
                                wiki("station.recycler.functions.3"),
                                wiki("station.recycler.functions.4")),
                        section(wiki("section.crafting_recipe"), List.of(), buildRecyclerStationCraftingVisual())
                )
        );
    }

    private static WikiEntry buildRelicCacheEntry() {
        return buildStationEntry(
                "station_relic_cache",
                wiki("station.relic_cache.title"),
                new ItemStack(ModBlocks.UNCOMMON_RELIC_CACHE.get()),
                wiki("station.relic_cache.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("station.relic_cache.overview.1"),
                                wiki("station.relic_cache.overview.2"),
                                wiki("station.relic_cache.overview.3")),
                        sectionKey("spawn_sites",
                                wiki("station.relic_cache.spawn_sites.1"),
                                wiki("station.relic_cache.spawn_sites.2"),
                                wiki("station.relic_cache.spawn_sites.3"),
                                wiki("station.relic_cache.spawn_sites.4"),
                                wiki("station.relic_cache.spawn_sites.5")),
                        sectionKey("functions",
                                wiki("station.relic_cache.functions.1"),
                                wiki("station.relic_cache.functions.2"),
                                wiki("station.relic_cache.functions.3"),
                                wiki("station.relic_cache.functions.4")),
                        section(wiki("section.example_solution"),
                                List.of(
                                        wiki("station.relic_cache.example.1"),
                                        wiki("station.relic_cache.example.2"),
                                        wiki("station.relic_cache.example.3"),
                                        wiki("station.relic_cache.example.4")),
                                buildRelicCacheExampleVisual()),
                        sectionKey("how_to_manage",
                                wiki("station.relic_cache.how_to_manage.1"),
                                wiki("station.relic_cache.how_to_manage.2"),
                                wiki("station.relic_cache.how_to_manage.3"),
                                wiki("station.relic_cache.how_to_manage.4"),
                                wiki("station.relic_cache.how_to_manage.5"))
                )
        );
    }

    private static WikiEntry buildNpcGeneralEntry() {
        return buildNpcEntry(
                "npc_general",
                Component.translatable("wiki.cyberneticenhancements.npc.general"),
                new ItemStack(ModItems.FIXER_SPAWN_EGG.get()),
                wiki("npc.general.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("npc.general.overview.1"),
                                wiki("npc.general.overview.2"),
                                wiki("npc.general.overview.3")),
                        sectionKey("contacts_archive",
                                wiki("npc.general.contacts_archive.1"),
                                wiki("npc.general.contacts_archive.2"),
                                wiki("npc.general.contacts_archive.3"),
                                wiki("npc.general.contacts_archive.4")),
                        sectionKey("identity_config",
                                wiki("npc.general.identity_config.1"),
                                wiki("npc.general.identity_config.2"),
                                wiki("npc.general.identity_config.3")),
                        sectionKey("current_state",
                                wiki("npc.general.current_state.1"),
                                wiki("npc.general.current_state.2"))
                )
        );
    }

    private static WikiEntry buildFixerEntry() {
        return buildNpcEntry(
                "npc_fixer",
                Component.translatable("wiki.cyberneticenhancements.npc.fixer"),
                new ItemStack(ModItems.FIXER_SPAWN_EGG.get()),
                wiki("npc.fixer.subtitle"),
                List.of(
                        sectionKey("overview",
                                wiki("npc.fixer.overview.1"),
                                wiki("npc.fixer.overview.2"),
                                wiki("npc.fixer.overview.3")),
                        sectionKey("field_presence",
                                wiki("npc.fixer.field_presence.1"),
                                wiki("npc.fixer.field_presence.2"),
                                wiki("npc.fixer.field_presence.3")),
                        sectionKey("trust_model",
                                wiki("npc.fixer.trust_model.1"),
                                wiki("npc.fixer.trust_model.2"),
                                wiki("npc.fixer.trust_model.3"),
                                wiki("npc.fixer.trust_model.4"),
                                wiki("npc.fixer.trust_model.5"),
                                wiki("npc.fixer.trust_model.6")),
                        sectionKey("services",
                                wiki("npc.fixer.services.1"),
                                wiki("npc.fixer.services.2"),
                                wiki("npc.fixer.services.3"),
                                wiki("npc.fixer.services.4"),
                                wiki("npc.fixer.services.5")),
                        sectionKey("account_and_stock",
                                wiki("npc.fixer.account_and_stock.1"),
                                wiki("npc.fixer.account_and_stock.2"),
                                wiki("npc.fixer.account_and_stock.3")),
                        sectionKey("current_state",
                                wiki("npc.fixer.current_state.1"),
                                wiki("npc.fixer.current_state.2"))
                )
        );
    }

    private static WikiEntry buildCyberstrainEntry() {
        int strained = CyberwareBalance.intValue("cyberstrain.threshold.strained");
        int unstable = CyberwareBalance.intValue("cyberstrain.threshold.unstable");
        int critical = CyberwareBalance.intValue("cyberstrain.threshold.critical");
        int psychosis = CyberwareBalance.intValue("cyberstrain.threshold.psychosis");
        return buildSystemEntry(
                "cyberstrain",
                Component.translatable("wiki.cyberneticenhancements.system.cyberstrain"),
                new ItemStack(Items.REDSTONE),
                wiki("system.cyberstrain.subtitle"),
                List.of(
                        sectionKey("thresholds",
                                wiki("system.cyberstrain.threshold.strained", strained),
                                wiki("system.cyberstrain.threshold.unstable", unstable),
                                wiki("system.cyberstrain.threshold.critical", critical),
                                wiki("system.cyberstrain.threshold.psychosis", psychosis)),
                        sectionKey("what_it_changes",
                                wiki("system.cyberstrain.change.1"),
                                wiki("system.cyberstrain.change.2"),
                                wiki("system.cyberstrain.change.3")),
                        sectionKey("how_to_manage",
                                wiki("system.cyberstrain.manage.1"),
                                wiki("system.cyberstrain.manage.2"),
                                wiki("system.cyberstrain.manage.3"))
                )
        );
    }

    private static WikiEntry buildCyberpsychosisEntry() {
        return buildSystemEntry(
                "cyberpsychosis",
                Component.translatable("wiki.cyberneticenhancements.system.cyberpsychosis"),
                new ItemStack(Items.NETHER_STAR),
                wiki("system.psychosis.subtitle"),
                List.of(
                        sectionKey("trigger_model",
                                wiki("system.psychosis.trigger.1"),
                                wiki("system.psychosis.trigger.2")),
                        sectionKey("episode_tiers",
                                wiki("system.psychosis.tiers.1"),
                                wiki("system.psychosis.tiers.2")),
                        sectionKey("stabilization",
                                wiki("system.psychosis.stabilization.1"),
                                wiki("system.psychosis.stabilization.2")),
                        sectionKey("targeting_rules",
                                wiki("system.psychosis.targeting.1"),
                                wiki("system.psychosis.targeting.2"))
                )
        );
    }

    private static WikiEntry buildProgressionEntry() {
        return buildSystemEntry(
                "progression",
                Component.translatable("wiki.cyberneticenhancements.system.progression"),
                new ItemStack(Items.CHEST),
                wiki("system.progression.subtitle"),
                List.of(
                        sectionKey("craftable_starter_layer",
                                wiki("system.progression.craftable.1"),
                                wiki("system.progression.craftable.2"),
                                wiki("system.progression.craftable.3")),
                        sectionKey("loot_layer",
                                wiki("system.progression.loot.1"),
                                wiki("system.progression.loot.2"),
                                wiki("system.progression.loot.3")),
                        sectionKey("quest_layer",
                                wiki("system.progression.quest.1"),
                                wiki("system.progression.quest.2")),
                        sectionKey("current_state",
                                wiki("system.progression.state.1"),
                                wiki("system.progression.state.2"))
                )
        );
    }

    private static WikiEntry buildStatusEntry(CombatStatusType type) {
        return buildSystemEntry(
                "status_" + type.id(),
                Component.translatable(type.translationKey()),
                statusIcon(type),
                Component.translatable(type.descriptionKey()),
                List.of(
                        sectionKey("overview",
                                Component.translatable(type.descriptionKey()),
                                wiki("overview.max_stacks", type.maxStacks())),
                        new ArticleSection(wiki("section.mechanical_effects"), buildStatusMechanicLines(type)),
                        new ArticleSection(wiki("section.presentation"), buildStatusPresentationLines(type))
                )
        );
    }

    private static List<Component> buildCyberwarePassiveLines(CyberwareDefinition definition) {
        List<Component> lines = new ArrayList<>();
        double chromeHeadroom = definition.capacityBonus();
        for (CyberwareEffect effect : definition.effects()) {
            if (effect.type() == CyberwareEffectType.CHROME_CAPACITY) {
                chromeHeadroom += effect.amount();
                continue;
            }
            lines.add(effect.describe());
        }
        if (chromeHeadroom > 0.0001D) {
            lines.add(Component.translatable(CyberwareEffectType.CHROME_CAPACITY.translationKey(), formatNumber(chromeHeadroom)));
        }
        if (definition.supportsChipware()) {
            lines.add(Component.translatable("tooltip.cyberneticenhancements.chip_slots", definition.chipSlotCount()));
        }
        if (definition.supportsModules()) {
            lines.add(wiki("cyberware.module_slots", definition.moduleSlotCount()));
            lines.add(wiki("cyberware.module_category", Component.translatable(definition.moduleCategory().translationKey())));
        }
        return lines;
    }

    private static List<Component> buildCyberwareBehaviorLines(CyberwareDefinition definition) {
        if (hasSpecialBehavior(definition)) {
            return List.of(Component.translatable("tooltip.cyberneticenhancements.special." + definition.id()));
        }
        return List.of();
    }

    private static List<Component> buildAcquisitionLines(CyberwareDefinition definition) {
        return buildAcquisitionLines(definition.id(), definition.isCraftable(), definition.acquisitionMethod(), true);
    }

    private static List<Component> buildCyberwareCraftingLines(CyberwareDefinition definition) {
        return definition.isCraftable() ? List.of() : List.of();
    }

    private static SectionVisual buildCyberwareCraftingVisual(CyberwareDefinition definition) {
        if (!definition.isCraftable()) {
            return null;
        }

        ItemStack[] grid = emptyGrid();
        fill(grid, 0, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        fill(grid, 1, CyberwareServiceHelper.slotSupportItem(definition.slotType()), 1);
        fill(grid, 2, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        fill(grid, 3, auxiliarySupportItem(definition.slotType()), 1);
        fill(grid, 4, definition.upgradeFromId() != null ? ModItems.cyberware(definition.upgradeFromId()).get() : definition.motifItem(), 1);
        fill(grid, 5, auxiliarySupportItem(definition.slotType()), 1);
        fill(grid, 6, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        fill(grid, 7, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 8, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModItems.cyberware(definition.id()).get()));
    }

    private static List<Component> buildCyberwareServiceLines(CyberwareDefinition definition) {
        return List.of();
    }

    private static List<Component> buildCyberwareRecycleLines(CyberwareDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildCyberwareRecycleVisual(CyberwareDefinition definition) {
        return new CompactTransformVisual(
                new ItemStack(ModItems.cyberware(definition.id()).get()),
                List.of(
                        CyberwareRecycleHelper.createCyberwareResult(definition),
                        new ItemStack(CyberwareServiceHelper.slotSupportItem(definition.slotType()))
                )
        );
    }

    private static List<Component> buildModuleIntegrationLines(CyberwareModuleDefinition definition) {
        return List.of(
                wiki("module.integration.fits", Component.translatable(definition.category().translationKey())),
                wiki("module.integration.host"),
                wiki("module.integration.ripper")
        );
    }

    private static List<Component> buildModuleCraftingLines(CyberwareModuleDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildModuleCraftingVisual(CyberwareModuleDefinition definition) {
        ItemStack[] grid = emptyGrid();
        if (definition.category() == CyberwareModuleCategory.ARMS) {
            fill(grid, 0, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 1, ModItems.COPPER_WIRING.get(), 1);
            fill(grid, 2, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 3, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
            fill(grid, 4, definition.motifItem(), 1);
            fill(grid, 5, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
            fill(grid, 6, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 7, ModItems.REPLACEMENT_JOINT.get(), 1);
            fill(grid, 8, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        } else {
            fill(grid, 0, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 1, ModItems.REPLACEMENT_JOINT.get(), 1);
            fill(grid, 2, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 3, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
            fill(grid, 4, definition.motifItem(), 1);
            fill(grid, 5, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
            fill(grid, 6, CyberwareServiceHelper.componentItem(definition.tier()), 1);
            fill(grid, 7, ModItems.MICRO_BATTERY.get(), 1);
            fill(grid, 8, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        }
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModItems.module(definition.id()).get()));
    }

    private static List<Component> buildModuleRecycleLines(CyberwareModuleDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildModuleRecycleVisual(CyberwareModuleDefinition definition) {
        return new CompactTransformVisual(
                new ItemStack(ModItems.module(definition.id()).get()),
                List.of(CyberwareRecycleHelper.createModuleResult(definition))
        );
    }

    private static List<Component> buildChipwareCraftingLines(ChipwareDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildChipwareCraftingVisual(ChipwareDefinition definition) {
        ItemStack[] grid = emptyGrid();
        fill(grid, 1, CyberwareServiceHelper.componentItem(definition.tier()), 1);
        fill(grid, 3, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 4, definition.motifItem(), 1);
        fill(grid, 5, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 7, Items.REDSTONE, 1);
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModItems.chipware(definition.id()).get()));
    }

    private static List<Component> buildChipwareRecycleLines(ChipwareDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildChipwareRecycleVisual(ChipwareDefinition definition) {
        return new CompactTransformVisual(
                new ItemStack(ModItems.chipware(definition.id()).get()),
                List.of(CyberwareRecycleHelper.createChipwareResult(definition))
        );
    }

    private static List<Component> buildConsumableEffectLines(CyberConsumableDefinition definition) {
        String id = definition.id();
        CyberConsumableProfile profile = definition.profile();
        List<Component> lines = new ArrayList<>();
        switch (profile) {
            case MAXDOC_MK1, MAXDOC_MK2, MAXDOC_MK3 ->
                    lines.add(wiki("consumable.heal_health", formatNumber(CyberwareBalance.doubleValue("consumable." + id + ".heal"))));
            case BOUNCE_BACK_MK1, BOUNCE_BACK_MK2, BOUNCE_BACK_MK3 -> {
                lines.add(wiki("consumable.heal_health", formatNumber(CyberwareBalance.doubleValue("consumable." + id + ".heal"))));
                lines.add(wiki("consumable.regen_hearts", formatNumber(CyberwareBalance.doubleValue("consumable." + id + ".regen") / 2.0D)));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable." + id + ".duration_ticks"))));
            }
            case HEALTH_BOOSTER -> {
                lines.add(wiki("consumable.max_health_hearts", formatNumber(CyberwareBalance.doubleValue("consumable.health_booster.hearts") / 2.0D)));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.health_booster.duration_ticks"))));
            }
            case STAMINA_BOOSTER -> {
                lines.add(wiki("consumable.move_speed", formatPercent(CyberwareBalance.doubleValue("consumable.stamina_booster.move_speed"))));
                lines.add(wiki("consumable.break_speed", formatPercent(CyberwareBalance.doubleValue("consumable.stamina_booster.break_speed"))));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.stamina_booster.duration_ticks"))));
            }
            case OXY_BOOSTER -> lines.add(wiki("consumable.water_breathing", ticksToSeconds(CyberwareBalance.intValue("consumable.oxy_booster.duration_ticks"))));
            case CAPACITY_BOOSTER -> {
                lines.add(wiki("consumable.chrome_capacity", formatNumber(CyberwareBalance.doubleValue("consumable.capacity_booster.chrome_capacity"))));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.capacity_booster.duration_ticks"))));
            }
            case RAM_JOLT -> {
                lines.add(wiki("consumable.cooldown_multiplier", formatNumber(CyberwareBalance.doubleValue("consumable.ram_jolt.cooldown_factor"))));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.ram_jolt.duration_ticks"))));
            }
            case IMMUNOBLOCKERS -> {
                lines.add(wiki("consumable.suppression", CyberwareBalance.intValue("consumable.immunoblockers.suppression_amount")));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.immunoblockers.duration_ticks"))));
                lines.add(wiki("consumable.immunoblockers.note"));
            }
            case CHROME_SUPPRESSANT -> {
                lines.add(wiki("consumable.suppression", CyberwareBalance.intValue("consumable.chrome_suppressant.suppression_amount")));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.chrome_suppressant.duration_ticks"))));
                lines.add(wiki("consumable.chrome_suppressant.note"));
            }
            case BLACK_LACE -> {
                lines.add(wiki("consumable.health_cost", formatPercent(CyberwareBalance.doubleValue("consumable.black_lace.health_fraction_cost"))));
                lines.add(wiki("consumable.instability", formatNumber(CyberwareBalance.doubleValue("consumable.black_lace.instability"))));
                lines.add(wiki("consumable.move_speed", formatPercent(CyberwareBalance.doubleValue("consumable.black_lace.move_speed"))));
                lines.add(wiki("consumable.attack_damage", formatNumber(CyberwareBalance.doubleValue("consumable.black_lace.damage"))));
                lines.add(wiki("consumable.damage_reduction", formatPercent(CyberwareBalance.doubleValue("consumable.black_lace.damage_reduction"))));
                lines.add(wiki("consumable.break_speed", formatPercent(CyberwareBalance.doubleValue("consumable.black_lace.break_speed"))));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.black_lace.duration_ticks"))));
            }
            case ASSKICK -> {
                lines.add(wiki("consumable.max_health_hearts", formatNumber(CyberwareBalance.doubleValue("consumable.asskick.hearts") / 2.0D)));
                lines.add(wiki("consumable.instability", formatNumber(CyberwareBalance.doubleValue("consumable.asskick.instability"))));
                lines.add(wiki("consumable.asskick.note"));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.asskick.duration_ticks"))));
            }
            case JELLYTRICITY -> {
                lines.add(wiki("consumable.health_cost", formatPercent(CyberwareBalance.doubleValue("consumable.jellytricity.health_fraction_cost"))));
                lines.add(wiki("consumable.instability", formatNumber(CyberwareBalance.doubleValue("consumable.jellytricity.instability"))));
                lines.add(wiki("consumable.move_speed", formatPercent(CyberwareBalance.doubleValue("consumable.jellytricity.move_speed"))));
                lines.add(wiki("consumable.break_speed", formatPercent(CyberwareBalance.doubleValue("consumable.jellytricity.break_speed"))));
                lines.add(wiki("consumable.duration_seconds", ticksToSeconds(CyberwareBalance.intValue("consumable.jellytricity.duration_ticks"))));
            }
        }
        return lines;
    }

    private static List<Component> buildConsumableCraftingLines(CyberConsumableDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildConsumableCraftingVisual(CyberConsumableDefinition definition) {
        return new CraftingGridVisual(List.<ItemStack[]>of(consumableRecipeGrid(definition.id())), new ItemStack(ModItems.consumable(definition.id()).get()));
    }

    private static List<Component> buildConsumableRecycleLines(CyberConsumableDefinition definition) {
        return List.of();
    }

    private static List<Component> buildCraftedItemAcquisitionLines(String itemId) {
        return buildAcquisitionLines(itemId, true, null, false);
    }

    private static List<Component> buildAcquisitionLines(
            String itemId,
            boolean craftable,
            CyberwareAcquisitionMethod acquisitionMethod,
            boolean includeMethodLine
    ) {
        List<Component> lines = new ArrayList<>();
        if (includeMethodLine && acquisitionMethod != null) {
            lines.add(wiki("acquisition.method", Component.translatable(acquisitionMethod.translationKey())));
        }
        if (craftable) {
            lines.add(wiki("acquisition.craftable"));
        }

        List<RelicCacheLootSource> lootSources = RELIC_CACHE_SOURCES.getOrDefault(itemId, List.of());
        for (RelicCacheLootSource source : lootSources) {
            lines.add(wiki("acquisition.cache_tier", cacheTierName(source.tier())));
            lines.add(wiki("acquisition.cache_sites", joinComponents(cacheStructureNames(source.tier()))));
            String chance = formatPercent(source.weight() / (double) source.totalWeight());
            if (source.pool() == RelicCacheLootPool.PRIMARY) {
                lines.add(wiki("acquisition.cache_chance_primary", chance, source.weight(), source.totalWeight()));
            } else {
                lines.add(wiki("acquisition.cache_chance_support", chance, source.weight(), source.totalWeight()));
            }
        }

        if (!craftable && lootSources.isEmpty()) {
            if (acquisitionMethod == CyberwareAcquisitionMethod.LOOT) {
                lines.add(wiki("acquisition.live_source_missing"));
            } else if (acquisitionMethod == CyberwareAcquisitionMethod.QUEST) {
                lines.add(wiki("acquisition.quest"));
            }
        }
        return lines;
    }

    private static SectionVisual buildConsumableRecycleVisual(CyberConsumableDefinition definition) {
        return new CompactTransformVisual(
                new ItemStack(ModItems.consumable(definition.id()).get()),
                List.of(CyberwareRecycleHelper.createConsumableResult(definition))
        );
    }

    private static SectionVisual buildCyberwareServiceVisual(CyberwareDefinition definition) {
        ItemStack base = new ItemStack(ModItems.cyberware(definition.id()).get());
        ItemStack damaged = base.copy();
        CyberwareConditionHelper.damage(damaged, definition, Math.max(1, CyberwareConditionHelper.getMaxIntegrity(definition) / 2));
        CyberwareServicePlan repairPlan = CyberwareServiceHelper.getRepairPlan(damaged);
        CyberwareServicePlan upgradePlan = CyberwareServiceHelper.getUpgradePlan(base);
        List<ServiceOperationVisual> operations = new ArrayList<>(2);
        if (repairPlan.isAvailable()) {
            operations.add(new ServiceOperationVisual(
                    wiki("service.operation.repair"),
                    wiki("service.repair_note"),
                    damaged,
                    requirementsToStacks(repairPlan),
                    repairPlan.output().copy()
            ));
        }
        if (upgradePlan.isAvailable()) {
            operations.add(new ServiceOperationVisual(
                    wiki("service.operation.upgrade"),
                    wiki("service.upgrade_note"),
                    base,
                    requirementsToStacks(upgradePlan),
                    upgradePlan.output().copy()
            ));
        }
        return operations.isEmpty() ? null : new ServiceOperationsVisual(
                List.of(),
                operations,
                buildUpgradeStages(definition)
        );
    }

    private static List<UpgradeStageVisual> buildUpgradeStages(CyberwareDefinition definition) {
        List<UpgradeStageVisual> stages = new ArrayList<>();
        ItemStack current = new ItemStack(ModItems.cyberware(definition.id()).get());
        int maxUpgrades = CyberwareUpgradeHelper.getMaxUpgrades(definition);
        for (int level = 0; level < maxUpgrades; level++) {
            CyberwareServicePlan stagePlan = CyberwareServiceHelper.getUpgradePlan(current);
            List<Component> changes = new ArrayList<>(CyberwareUpgradeHelper.getUpgradePreviewLines(current, definition));
            if (changes.isEmpty()) {
                changes.add(wiki("service.stage.no_change"));
            }
            stages.add(new UpgradeStageVisual(Component.literal("U" + (level + 1)), requirementsToStacks(stagePlan), changes));
            current = CyberwareUpgradeHelper.createUpgradedCopy(current, definition);
        }
        return stages;
    }

    private static List<Component> buildStatusMechanicLines(CombatStatusType type) {
        return switch (type) {
            case SHOCK -> List.of(
                    wiki("status.shock.move_speed", signedPercent(CyberwareBalance.doubleValue("combat_status.shock.move_speed_per_stack"))),
                    wiki("status.shock.attack_speed", signedPercent(CyberwareBalance.doubleValue("combat_status.shock.attack_speed_per_stack"))),
                    wiki("status.shock.cooldown_multiplier", formatPercent(CyberwareBalance.doubleValue("combat_status.shock.ability_cooldown_per_stack"))),
                    wiki("status.shock.periodic")
            );
            case OVERHEAT -> List.of(
                    wiki("status.overheat.damage_taken", formatPercent(CyberwareBalance.doubleValue("combat_status.overheat.damage_taken_per_stack"))),
                    wiki("status.overheat.periodic"),
                    wiki("status.overheat.moving")
            );
            case CORROSION -> List.of(
                    wiki("status.corrosion.healing"),
                    wiki("status.corrosion.periodic")
            );
            case TRAUMA -> List.of(
                    wiki("status.trauma.damage_taken", formatPercent(CyberwareBalance.doubleValue("combat_status.trauma.damage_taken_per_stack"))),
                    wiki("status.trauma.knockback_taken", formatPercent(CyberwareBalance.doubleValue("combat_status.trauma.knockback_taken_per_stack"))),
                    wiki("status.trauma.periodic")
            );
            case BLEED -> List.of(
                    wiki("status.bleed.damage_taken", formatPercent(CyberwareBalance.doubleValue("combat_status.bleed.damage_taken_per_stack"))),
                    wiki("status.bleed.periodic"),
                    wiki("status.bleed.moving")
            );
            case MARK -> List.of(
                    wiki("status.mark.damage_taken", formatPercent(CyberwareBalance.doubleValue("combat_status.mark.damage_taken_per_stack"))),
                    wiki("status.mark.periodic")
            );
        };
    }

    private static List<Component> buildStatusPresentationLines(CombatStatusType type) {
        return switch (type) {
            case SHOCK -> List.of(
                    wiki("status.shock.visual"),
                    wiki("status.shock.intent")
            );
            case OVERHEAT -> List.of(
                    wiki("status.overheat.visual"),
                    wiki("status.overheat.intent")
            );
            case CORROSION -> List.of(
                    wiki("status.corrosion.visual"),
                    wiki("status.corrosion.intent")
            );
            case TRAUMA -> List.of(
                    wiki("status.trauma.visual"),
                    wiki("status.trauma.intent")
            );
            case BLEED -> List.of(
                    wiki("status.bleed.visual"),
                    wiki("status.bleed.intent")
            );
            case MARK -> List.of(
                    wiki("status.mark.visual"),
                    wiki("status.mark.intent")
            );
        };
    }

    private static List<Component> describeEffects(List<CyberwareEffect> effects) {
        return effects.stream().map(CyberwareEffect::describe).toList();
    }

    private static boolean hasSpecialBehavior(CyberwareDefinition definition) {
        String id = definition.id();
        return FrontalCortexManager.hasSpecialBehavior(id)
                || CyberwareAbilities.hasSpecialBehavior(id)
                || ArmCyberwareManager.hasSpecialBehavior(id)
                || FaceCyberwareManager.hasSpecialBehavior(id)
                || CirculatoryCyberwareManager.hasSpecialBehavior(id)
                || HandsCyberwareManager.hasSpecialBehavior(id)
                || SkeletonCyberwareManager.hasSpecialBehavior(id)
                || NervousSystemCyberwareManager.hasSpecialBehavior(id)
                || IntegumentaryCyberwareManager.hasSpecialBehavior(id)
                || LegCyberwareManager.hasSpecialBehavior(id);
    }

    private static ItemLike auxiliarySupportItem(CyberwareSlotType slotType) {
        return switch (slotType) {
            case FRONTAL_CORTEX, OPERATING_SYSTEM, NERVOUS_SYSTEM, FACE -> ModItems.MICRO_BATTERY.get();
            case ARMS, LEGS, SKELETON, HANDS -> ModItems.SERVO_SCREWS.get();
            case CIRCULATORY_SYSTEM, INTEGUMENTARY_SYSTEM -> ModItems.CONDUCTIVE_PASTE.get();
        };
    }

    private static SectionVisual buildRipperStationCraftingVisual() {
        ItemStack[] grid = emptyGrid();
        fill(grid, 0, Items.IRON_INGOT, 1);
        fill(grid, 1, ModItems.SENSOR_LENS.get(), 1);
        fill(grid, 2, Items.IRON_INGOT, 1);
        fill(grid, 3, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 4, Items.SMOOTH_STONE, 1);
        fill(grid, 5, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 6, Items.IRON_INGOT, 1);
        fill(grid, 7, ModItems.CONDUCTIVE_PASTE.get(), 1);
        fill(grid, 8, Items.IRON_INGOT, 1);
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModBlocks.RIPPER_STATION.get()));
    }

    private static SectionVisual buildTechStationCraftingVisual() {
        ItemStack[] grid = emptyGrid();
        fill(grid, 0, Items.IRON_INGOT, 1);
        fill(grid, 1, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 2, Items.IRON_INGOT, 1);
        fill(grid, 3, ModItems.REPLACEMENT_JOINT.get(), 1);
        fill(grid, 4, Items.SMOOTH_STONE, 1);
        fill(grid, 5, ModItems.REPLACEMENT_JOINT.get(), 1);
        fill(grid, 6, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
        fill(grid, 7, ModItems.CONDUCTIVE_PASTE.get(), 1);
        fill(grid, 8, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModBlocks.TECH_STATION.get()));
    }

    private static SectionVisual buildRecyclerStationCraftingVisual() {
        ItemStack[] grid = emptyGrid();
        fill(grid, 0, Items.IRON_INGOT, 1);
        fill(grid, 1, ModItems.SERVO_SCREWS.get(), 1);
        fill(grid, 2, Items.IRON_INGOT, 1);
        fill(grid, 3, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 4, Items.SMOOTH_STONE, 1);
        fill(grid, 5, ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
        fill(grid, 6, ModItems.REPLACEMENT_JOINT.get(), 1);
        fill(grid, 7, ModItems.CONDUCTIVE_PASTE.get(), 1);
        fill(grid, 8, ModItems.REPLACEMENT_JOINT.get(), 1);
        return new CraftingGridVisual(List.<ItemStack[]>of(grid), new ItemStack(ModBlocks.RECYCLER_STATION.get()));
    }

    private static SectionVisual buildRelicCacheExampleVisual() {
        int[] gridTokens = {
                2, 1, 4, 6, 0,
                7, 0, 5, 2, 4,
                6, 3, 0, 7, 1,
                4, 2, 5, 1, 6,
                1, 4, 0, 5, 6
        };
        int[] pathCells = {1, 11, 13, 23, 24};
        int[][] daemonTokens = {
                {1, 3, 7},
                {3, 7, 5},
                {7, 5, 6}
        };
        return new RelicCacheExampleVisual(5, gridTokens, pathCells, daemonTokens);
    }

    private static ItemStack[] consumableRecipeGrid(String id) {
        ItemStack[] grid = emptyGrid();
        switch (id) {
            case "maxdoc_mk1" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.REDSTONE, 1);
                fill(grid, 7, ModItems.CONDUCTIVE_PASTE.get(), 1);
            }
            case "maxdoc_mk2" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.GOLDEN_APPLE, 1);
                fill(grid, 7, ModItems.COMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "maxdoc_mk3" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.GHAST_TEAR, 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "bounce_back_mk1" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.SUGAR, 1);
                fill(grid, 7, ModItems.CONDUCTIVE_PASTE.get(), 1);
            }
            case "bounce_back_mk2" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.REDSTONE, 1);
                fill(grid, 6, Items.GHAST_TEAR, 1);
                fill(grid, 7, ModItems.COMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "bounce_back_mk3" -> {
                fill(grid, 1, Items.GLASS_BOTTLE, 1);
                fill(grid, 4, Items.GOLDEN_APPLE, 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "health_booster" -> {
                fill(grid, 1, Items.GOLDEN_APPLE, 1);
                fill(grid, 4, Items.GLASS_BOTTLE, 1);
                fill(grid, 7, ModItems.COMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "stamina_booster" -> {
                fill(grid, 1, Items.SUGAR, 1);
                fill(grid, 4, Items.GLASS_BOTTLE, 1);
                fill(grid, 7, ModItems.COMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "oxy_booster" -> {
                fill(grid, 1, Items.GHAST_TEAR, 1);
                fill(grid, 4, Items.GLASS_BOTTLE, 1);
                fill(grid, 7, ModItems.COMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "capacity_booster" -> {
                fill(grid, 1, Items.GOLDEN_APPLE, 1);
                fill(grid, 4, ModItems.MICRO_BATTERY.get(), 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "ram_jolt" -> {
                fill(grid, 1, Items.REDSTONE, 1);
                fill(grid, 4, ModItems.MICRO_BATTERY.get(), 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "immunoblockers" -> {
                fill(grid, 1, Items.FERMENTED_SPIDER_EYE, 1);
                fill(grid, 4, Items.GHAST_TEAR, 1);
                fill(grid, 7, ModItems.RARE_ITEM_COMPONENTS.get(), 1);
            }
            case "chrome_suppressant" -> {
                fill(grid, 1, Items.GHAST_TEAR, 1);
                fill(grid, 4, ModItems.CONDUCTIVE_PASTE.get(), 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "black_lace" -> {
                fill(grid, 1, Items.BLAZE_POWDER, 1);
                fill(grid, 4, Items.SUGAR, 1);
                fill(grid, 6, Items.FERMENTED_SPIDER_EYE, 1);
                fill(grid, 7, ModItems.RARE_ITEM_COMPONENTS.get(), 1);
            }
            case "asskick" -> {
                fill(grid, 1, Items.GOLDEN_APPLE, 1);
                fill(grid, 4, Items.SUGAR, 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            case "jellytricity" -> {
                fill(grid, 1, Items.SUGAR, 1);
                fill(grid, 4, Items.REDSTONE, 1);
                fill(grid, 7, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 1);
            }
            default -> {
            }
        }
        return grid;
    }

    private static void fill(ItemStack[] grid, int index, ItemLike itemLike, int count) {
        grid[index] = new ItemStack(itemLike, count);
    }

    private static ItemStack[] emptyGrid() {
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        return grid;
    }

    private static List<ItemStack> requirementsToStacks(CyberwareServicePlan plan) {
        List<ItemStack> stacks = new ArrayList<>();
        for (CyberwareServiceHelper.MaterialRequirement requirement : CyberwareServiceHelper.getRequiredMaterials(plan)) {
            stacks.add(requirement.stack().copyWithCount(requirement.count()));
        }
        return stacks;
    }

    private static Component stackLine(ItemStack stack) {
        return wiki("stack_line", stack.getCount(), stack.getHoverName());
    }

    private static void addSection(List<ArticleSection> sections, Component title, List<Component> lines) {
        addSection(sections, title, lines, null);
    }

    private static void addSection(List<ArticleSection> sections, Component title, List<Component> lines, SectionVisual visual) {
        ArticleSection section = section(title, lines, visual);
        if (section != null) {
            sections.add(section);
        }
    }

    private static ArticleSection section(Component title, List<Component> lines) {
        return section(title, lines, null);
    }

    private static ArticleSection section(Component title, List<Component> lines, SectionVisual visual) {
        boolean hasLines = lines != null && !lines.isEmpty();
        if (!hasLines && visual == null) {
            return null;
        }
        return new ArticleSection(title, hasLines ? lines : List.of(), visual);
    }

    private static List<ArticleSection> filterSections(List<ArticleSection> sections) {
        return sections.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static ArticleSection sectionKey(String titleKey, Component... lines) {
        return new ArticleSection(wiki("section." + titleKey), List.of(lines));
    }

    private static WikiEntry buildStationEntry(String id, Component title, ItemStack icon, Component subtitle, List<ArticleSection> sections) {
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.station"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.infrastructure")), sections);
    }

    private static WikiEntry buildNpcEntry(String id, Component title, ItemStack icon, Component subtitle, List<ArticleSection> sections) {
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.npc"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.contact")), sections);
    }

    private static WikiEntry buildSystemEntry(String id, Component title, ItemStack icon, Component subtitle, List<ArticleSection> sections) {
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.system"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.reference")), sections);
    }

    private static WikiEntry buildLoreEntry(String id, Component title, ItemStack icon, Component subtitle, List<ArticleSection> sections) {
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.lore"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.reference")), sections);
    }

    private static WikiEntry buildQuestEntry(String id, Component title, ItemStack icon, Component subtitle, List<ArticleSection> sections) {
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.quest"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.infrastructure")), sections);
    }

    private static ItemStack statusIcon(CombatStatusType type) {
        return switch (type) {
            case SHOCK -> new ItemStack(Items.LIGHTNING_ROD);
            case OVERHEAT -> new ItemStack(Items.BLAZE_POWDER);
            case CORROSION -> new ItemStack(Items.SPIDER_EYE);
            case TRAUMA -> new ItemStack(Items.IRON_AXE);
            case BLEED -> new ItemStack(Items.RED_DYE);
            case MARK -> new ItemStack(Items.ENDER_EYE);
        };
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String formatPercent(double value) {
        return formatNumber(value * 100.0D) + "%";
    }

    private static String signedPercent(double value) {
        return (value > 0 ? "+" : "") + formatPercent(value);
    }

    private static String ticksToSeconds(int ticks) {
        return formatNumber(ticks / 20.0D);
    }

    private static Component cacheTierName(RelicCacheTier tier) {
        return switch (tier) {
            case UNCOMMON -> Component.translatable("block.cyberneticenhancements.uncommon_relic_cache");
            case RARE -> Component.translatable("block.cyberneticenhancements.rare_relic_cache");
            case EPIC -> Component.translatable("block.cyberneticenhancements.epic_relic_cache");
            case LEGENDARY -> Component.translatable("block.cyberneticenhancements.legendary_relic_cache");
        };
    }

    private static List<Component> cacheStructureNames(RelicCacheTier tier) {
        return switch (tier) {
            case UNCOMMON -> List.of(
                    wiki("structure.igloo_basement"),
                    wiki("structure.jungle_temple"),
                    wiki("structure.pillager_outpost")
            );
            case RARE -> List.of(
                    wiki("structure.desert_pyramid"),
                    wiki("structure.woodland_mansion"),
                    wiki("structure.stronghold")
            );
            case EPIC -> List.of(
                    wiki("structure.trial_chambers"),
                    wiki("structure.ancient_city"),
                    wiki("structure.bastion_remnant")
            );
            case LEGENDARY -> List.of(wiki("structure.end_city"));
        };
    }

    private static Component joinComponents(List<Component> parts) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                joined.append(", ");
            }
            joined.append(parts.get(index));
        }
        return joined;
    }

    private static String friendly(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static Map<String, List<RelicCacheLootSource>> createRelicCacheSources() {
        Map<String, List<RelicCacheLootSource>> sources = new LinkedHashMap<>();
        registerLootSources(sources, RelicCacheTier.UNCOMMON, RelicCacheLootPool.PRIMARY, 91,
                "precision_miner", 9,
                "masons_grip", 9,
                "forager_lens", 8,
                "cargo_spine", 7,
                "fortified_ankles", 7,
                "basic_kiroshi_optics", 7,
                "ex_disk", 6,
                "memory_boost", 6,
                "newton_module", 5,
                "mining_accelerator", 6,
                "builder_grip", 6,
                "fall_damper", 5,
                "miner_skillchip", 5,
                "builder_skillchip", 5
        );
        registerLootSources(sources, RelicCacheTier.UNCOMMON, RelicCacheLootPool.SUPPORT, 44,
                "maxdoc_mk2", 4,
                "bounce_back_mk2", 4
        );
        registerLootSources(sources, RelicCacheTier.RARE, RelicCacheLootPool.PRIMARY, 89,
                "harvester_hands", 9,
                "hydrolung", 8,
                "kiroshi_retrieval_suite", 8,
                "vein_reader_optics", 7,
                "brushstep_legs", 7,
                "reach_extender", 7,
                "combat_actuator", 7,
                "dash_piston", 6,
                "sprint_motor", 6,
                "climbing_servo", 6,
                "scout_skillchip", 5,
                "combat_skillchip", 5,
                "runner_skillchip", 5,
                "relic_scanner", 3
        );
        registerLootSources(sources, RelicCacheTier.RARE, RelicCacheLootPool.SUPPORT, 44,
                "maxdoc_mk3", 4,
                "bounce_back_mk3", 4,
                "ram_jolt", 4,
                "capacity_booster", 3
        );
        registerLootSources(sources, RelicCacheTier.EPIC, RelicCacheLootPool.PRIMARY, 94,
                "self_ice", 8,
                "clairvoyant", 7,
                "biomonitor", 7,
                "synaptic_accelerator", 7,
                "tyrosine_injector", 6,
                "para_bellum", 6,
                "blood_pump", 6,
                "ram_reallocator", 5,
                "raven_microcyber", 4,
                "tetratronic_rippler", 4,
                "excavator_arms", 4,
                "the_oracle", 4,
                "cockatrice", 3,
                "epimorphic_skeleton", 3,
                "immovable_force", 3,
                "adreno_trigger", 3,
                "reflex_tuner", 3,
                "shock_palm", 4,
                "stealth_foot", 4,
                "trader_skillchip", 3
        );
        registerLootSources(sources, RelicCacheTier.EPIC, RelicCacheLootPool.SUPPORT, 22,
                "immunoblockers", 3,
                "black_lace", 2
        );
        registerLootSources(sources, RelicCacheTier.LEGENDARY, RelicCacheLootPool.PRIMARY, 50,
                "quantum_tuner", 6,
                "axolotl", 5,
                "militech_canto", 2,
                "militech_apogee", 3,
                "chipware_socket_mk3", 4,
                "behavioral_imprint_synced_faceplate", 3,
                "second_heart", 4,
                "chitin", 3,
                "maxtac_mantis_blades", 2,
                "pain_editor", 3,
                "painducer", 3,
                "cogito_lattice", 3,
                "revulsor", 3,
                "rara_avis", 3,
                "leeroy_ligament_system", 3
        );
        registerLootSources(sources, RelicCacheTier.LEGENDARY, RelicCacheLootPool.SUPPORT, 23,
                "immunoblockers", 4,
                "black_lace", 4
        );
        return sources;
    }

    private static void registerLootSources(
            Map<String, List<RelicCacheLootSource>> sources,
            RelicCacheTier tier,
            RelicCacheLootPool pool,
            int totalWeight,
            Object... idWeightPairs
    ) {
        if ((idWeightPairs.length & 1) != 0) {
            throw new IllegalArgumentException("Loot source pairs must be id/weight");
        }
        for (int index = 0; index < idWeightPairs.length; index += 2) {
            String id = (String) idWeightPairs[index];
            int weight = (Integer) idWeightPairs[index + 1];
            sources.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new RelicCacheLootSource(tier, pool, weight, totalWeight));
        }
    }

    record ArchiveTab(String id, Component title, Component subtitle, Component navigationTitle, List<WikiTopic> topics) {
    }

    record WikiTopic(String id, Component title, List<WikiTopic> children, WikiSection section) {
        boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    record WikiSection(String id, Component title, List<WikiEntry> entries) {
    }

    record WikiEntry(
            String id,
            Component title,
            ItemStack icon,
            Component badge,
            Component subtitle,
            List<Component> chips,
            List<ArticleSection> sections
    ) {
    }

    sealed interface SectionVisual permits CraftingGridVisual, CompactTransformVisual, ServiceOperationsVisual, RelicCacheExampleVisual {
    }

    record CraftingGridVisual(List<ItemStack[]> grids, ItemStack output) implements SectionVisual {
    }

    record CompactTransformVisual(ItemStack input, List<ItemStack> outputs) implements SectionVisual {
    }

    record ServiceOperationVisual(Component label, Component note, ItemStack input, List<ItemStack> materials, ItemStack output) {
    }

    record UpgradeStageVisual(Component label, List<ItemStack> materials, List<Component> changes) {
    }

    record ServiceOperationsVisual(List<Component> summaryChips, List<ServiceOperationVisual> operations, List<UpgradeStageVisual> upgradeStages) implements SectionVisual {
    }

    record RelicCacheExampleVisual(int gridSize, int[] gridTokens, int[] pathCells, int[][] daemonTokens) implements SectionVisual {
    }

    enum RelicCacheLootPool {
        PRIMARY,
        SUPPORT
    }

    record RelicCacheLootSource(RelicCacheTier tier, RelicCacheLootPool pool, int weight, int totalWeight) {
    }

    record ArticleSection(Component title, List<Component> lines, SectionVisual visual) {
        ArticleSection(Component title, List<Component> lines) {
            this(title, lines, null);
        }
    }
}
