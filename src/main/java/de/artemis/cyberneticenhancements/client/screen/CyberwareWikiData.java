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
                buildSchematicsTab(),
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
                        "skill_tree",
                        Component.translatable("screen.cyberneticenhancements.archive.tab.skill_tree"),
                        Component.translatable("screen.cyberneticenhancements.archive.mode.skill_tree.subtitle"),
                        Component.translatable("screen.cyberneticenhancements.archive.navigation.skill_tree"),
                        buildSkillTreeTopics()
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

    static ArchiveTab buildSchematicsTab() {
        return new ArchiveTab("schematics", Component.literal("Schematics"), Component.literal("Collected and undiscovered fabrication designs"), Component.literal("Schematic collection"), List.of());
    }

    private static SectionVisual fabricationVisual(String id) {
        return recipeVisual(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", id))));
    }

    private static SectionVisual recipeVisual(ItemStack item) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return null;
        var fabrication = level.getRecipeManager().getAllRecipesFor(de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes.FABRICATION.get()).stream()
                .filter(holder -> holder.value().result().is(item.getItem()))
                .sorted(Comparator.<net.minecraft.world.item.crafting.RecipeHolder<de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe>, Boolean>
                        comparing(holder -> !holder.value().clientUnlocked()).thenComparing(holder -> holder.id().toString()))
                .findFirst();
        if (fabrication.isPresent()) {
            var holder = fabrication.get();
            var recipe = holder.value();
            ItemStack[] grid = WikiRecipeLayouts.grid(recipe);
            return new CraftingGridVisual(List.<ItemStack[]>of(grid), recipe.result().copy(), recipe.schematic(), holder.id());
        }
        return level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .filter(holder -> holder.value().getResultItem(level.registryAccess()).is(item.getItem()))
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .filter(holder -> holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe
                        || holder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe)
                .map(holder -> {
                    var recipe = holder.value();
                    ItemStack[] grid = WikiRecipeLayouts.grid(recipe);
                    return (SectionVisual) new CraftingGridVisual(List.<ItemStack[]>of(grid),
                            recipe.getResultItem(level.registryAccess()).copy(), "", holder.id());
                }).findFirst().orElse(null);
    }
    private static List<WikiTopic> buildWikiTopics() {
        return List.of(
                new WikiTopic("cyberware", Component.translatable("wiki.cyberneticenhancements.topic.cyberware"), buildCyberwareTopics(), null),
                new WikiTopic("components", wiki("topic.components"), List.of(), new WikiSection("components", wiki("topic.components"), buildComponentEntries())),
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

    private static List<WikiTopic> buildSkillTreeTopics() {
        Component title = wiki("skill_tree.title");
        WikiEntry preview = new WikiEntry(
                "skill_tree_preview",
                title,
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                wiki("skill_tree.coming_soon").copy().withStyle(ChatFormatting.AQUA),
                wiki("skill_tree.subtitle"),
                List.of(),
                Arrays.asList(
                        sectionKey("overview", wiki("skill_tree.planned")),
                        sectionKey("current_state", wiki("skill_tree.unavailable"), wiki("skill_tree.subject_to_change"))
                )
        );
        return List.of(new WikiTopic("skill_tree_preview", title, List.of(),
                new WikiSection("skill_tree_preview", title, List.of(preview))));
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
                new WikiTopic("station_fabricator", Component.literal("Cyberware Fabricator"), List.of(), new WikiSection("station_fabricator", Component.literal("Cyberware Fabricator"), List.of(buildFabricatorEntry()))),
                new WikiTopic("station_ripper", Component.translatable("block.cyberneticenhancements.ripper_station"), List.of(), new WikiSection("station_ripper", Component.translatable("block.cyberneticenhancements.ripper_station"), List.of(buildRipperStationEntry()))),
                new WikiTopic("station_tech", Component.translatable("block.cyberneticenhancements.tech_station"), List.of(), new WikiSection("station_tech", Component.translatable("block.cyberneticenhancements.tech_station"), List.of(buildTechStationEntry()))),
                new WikiTopic("station_upgrade", Component.translatable("block.cyberneticenhancements.upgrade_station"), List.of(), new WikiSection("station_upgrade", Component.translatable("block.cyberneticenhancements.upgrade_station"), List.of(buildUpgradeStationEntry()))),
                new WikiTopic("station_recycler", Component.translatable("block.cyberneticenhancements.recycler_station"), List.of(), new WikiSection("station_recycler", Component.translatable("block.cyberneticenhancements.recycler_station"), List.of(buildRecyclerStationEntry()))),
                new WikiTopic("station_relic_cache", wiki("station.relic_cache.title"), List.of(), new WikiSection("station_relic_cache", wiki("station.relic_cache.title"), List.of(buildRelicCacheEntry())))
        );
    }

    private static WikiEntry buildFabricatorEntry() {
        return buildStationEntry("station_fabricator", Component.translatable("block.cyberneticenhancements.cyberware_fabricator"),
                new ItemStack(ModBlocks.CYBERWARE_FABRICATOR.get()), wiki("station.fabricator.subtitle"),
                filterSections(Arrays.asList(
                        sectionKey("overview", wiki("station.fabricator.operation"), wiki("station.fabricator.output")),
                        sectionKey("functions", wiki("station.fabricator.tiers")),
                        sectionKey("schematics", wiki("station.fabricator.schematics"), wiki("station.fabricator.collection")),
                        section(wiki("section.crafting_recipe"), List.of(), recipeVisual(new ItemStack(ModBlocks.CYBERWARE_FABRICATOR.get()))))));
    }
    private static final int[] SHOP_OPTIONS = {
            de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_MAXDOC_MK2,
            de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_RAM_JOLT,
            de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT,
            de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS
    };

    private static List<WikiTopic> buildNpcTopics() {
        var topics = new ArrayList<WikiTopic>();
        topics.add(new WikiTopic("npc_general", wiki("npc.general"), List.of(),
                new WikiSection("npc_general", wiki("npc.general"), List.of(buildNpcGeneralEntry()))));
        for (var type : de.artemis.cyberneticenhancements.common.entity.NpcType.values()) {
            String id = "npc_" + type.id();
            topics.add(new WikiTopic(id, type.displayName(), List.of(),
                    new WikiSection(id, type.displayName(), List.of(buildNpcTypeEntry(type)))));
        }
        return topics;
    }

    private static WikiEntry buildNpcTypeEntry(de.artemis.cyberneticenhancements.common.entity.NpcType type) {
        var egg = switch (type) {
            case FIXER -> ModItems.FIXER_SPAWN_EGG.get();
            case RIPPERDOC -> ModItems.RIPPERDOC_SPAWN_EGG.get();
            case TECHIE -> ModItems.TECHIE_SPAWN_EGG.get();
            case NETRUNNER -> ModItems.NETRUNNER_SPAWN_EGG.get();
            case MERC -> ModItems.MERC_SPAWN_EGG.get();
        };
        List<Component> stock = new ArrayList<>();
        if (type.supportsShop()) for (int option : SHOP_OPTIONS) {
            var stack = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.shopItem(type, option);
            if (!stack.isEmpty()) stock.add(wiki("npc.stock_entry", stack.getCount(), stack.getHoverName(),
                    de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.requiredTrust(type, option)));
        }
        var sections = new ArrayList<ArticleSection>();
        sections.add(sectionKey("overview", wiki("npc.role." + type.id()), wiki("npc.shared_relationship")));
        sections.add(sectionKey("services", wiki("npc.services." + type.id()),
                wiki("npc.secondary_service", de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.requiredTrust(type,
                        de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_PURGE))));
        sections.add(sectionKey("account_and_stock", wiki("npc.currency"), wiki(type.supportsTrustDiscount() ? "npc.discounts" : "npc.fixed_prices")));
        addSection(sections, wiki("section.stock"), stock);
        sections.add(sectionKey("functions", wiki(type.supportsContracts() ? "npc.contracts_available" : "npc.no_contracts"),
                wiki(type.supportsIntel() ? "npc.intel_available" : "npc.no_intel")));
        sections.add(sectionKey("trust_model", wiki("npc.fixer.trust_model.1"), wiki("npc.fixer.trust_model.2"),
                wiki("npc.fixer.trust_model.3"), wiki("npc.fixer.trust_model.4"), wiki("npc.fixer.trust_model.5"),
                wiki("npc.fixer.trust_model.6"), wiki("npc.identity_unlock")));
        return buildNpcEntry("npc_" + type.id(), type.displayName(), new ItemStack(egg), wiki("npc.role." + type.id()), sections);
    }
    private static List<WikiTopic> buildSystemTopics() {
        return List.of(
                new WikiTopic("system_controls", wiki("controls.title"), List.of(),
                        new WikiSection("system_controls", wiki("controls.title"), List.of(buildControlsEntry()))),
                new WikiTopic("system_banking", wiki("banking.title"), List.of(),
                        new WikiSection("system_banking", wiki("banking.title"), List.of(buildBankingEntry()))),
                new WikiTopic("system_sandevistan", wiki("sandevistan.title"), List.of(),
                        new WikiSection("system_sandevistan", wiki("sandevistan.title"), List.of(buildSandevistanEntry()))),
                new WikiTopic("system_cyberstrain", Component.translatable("wiki.cyberneticenhancements.system.cyberstrain"), List.of(), new WikiSection("system_cyberstrain", Component.translatable("wiki.cyberneticenhancements.system.cyberstrain"), List.of(buildCyberstrainEntry()))),
                new WikiTopic("system_psychosis", Component.translatable("wiki.cyberneticenhancements.system.cyberpsychosis"), List.of(), new WikiSection("system_psychosis", Component.translatable("wiki.cyberneticenhancements.system.cyberpsychosis"), List.of(buildCyberpsychosisEntry()))),
                new WikiTopic("system_statuses", Component.translatable("wiki.cyberneticenhancements.system.statuses"), List.of(), new WikiSection("system_statuses", Component.translatable("wiki.cyberneticenhancements.system.statuses"), buildStatusEntries())),
                new WikiTopic("system_progression", Component.translatable("wiki.cyberneticenhancements.system.progression"), List.of(), new WikiSection("system_progression", Component.translatable("wiki.cyberneticenhancements.system.progression"), List.of(buildProgressionEntry())))
        );
    }

    private static WikiEntry buildControlsEntry() {
        List<Component> bindings = new ArrayList<>();
        for (var mapping : List.of(de.artemis.cyberneticenhancements.client.ModKeyMappings.ACTIVATE_CYBERWARE,
                de.artemis.cyberneticenhancements.client.ModKeyMappings.ACTIVATE_AUXILIARY_CYBERWARE,
                de.artemis.cyberneticenhancements.client.ModKeyMappings.ACTIVATE_ARM_CYBERWARE,
                de.artemis.cyberneticenhancements.client.ModKeyMappings.ACTIVATE_FACE_CYBERWARE,
                de.artemis.cyberneticenhancements.client.ModKeyMappings.OPEN_ARCHIVE,
                de.artemis.cyberneticenhancements.client.ModKeyMappings.TOGGLE_HUD))
            bindings.add(wiki("controls.binding", Component.translatable(mapping.getName()), mapping.getTranslatedKeyMessage()));
        return buildSystemEntry("system_controls", wiki("controls.title"), new ItemStack(Items.COMPASS), wiki("controls.subtitle"),
                List.of(section(wiki("section.functions"), bindings), sectionKey("overview", wiki("controls.archive"))));
    }

    private static WikiEntry buildBankingEntry() {
        return buildSystemEntry("system_banking", wiki("banking.title"), new ItemStack(Items.GOLD_NUGGET), wiki("banking.subtitle"),
                List.of(sectionKey("overview", wiki("banking.operations"), wiki("banking.authority"), wiki("banking.sources"))));
    }

    private static WikiEntry buildSandevistanEntry() {
        return new WikiEntry("system_sandevistan", wiki("sandevistan.title"), new ItemStack(Items.CLOCK),
                wiki("sandevistan.badge"), wiki("sandevistan.subtitle"), List.of(), Arrays.asList(
                sectionKey("overview", wiki("sandevistan.controls"), wiki("sandevistan.dilation")),
                sectionKey("special_behavior", wiki("sandevistan.models"), wiki("sandevistan.experimental")),
                sectionKey("maintenance", wiki("sandevistan.maintenance"), wiki("sandevistan.overload"), wiki("sandevistan.recovery")),
                sectionKey("notes", wiki("sandevistan.multiplayer"), wiki("sandevistan.options"))));
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
                        Arrays.asList(
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
                        Arrays.asList(
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

        chips.add(wiki("chip.chrome", definition.chromeCost()));
        chips.add(wiki("chip.integrity", CyberwareConditionHelper.getMaxIntegrity(definition)));

        List<ArticleSection> sections = new ArrayList<>();
        sections.add(sectionKey("overview",
                Component.translatable(definition.descriptionKey()),
                wiki("overview.slot", Component.translatable(definition.slotType().translationKey())),
                wiki("overview.tier", Component.translatable(definition.tier().translationKey())),
                wiki("overview.acquisition", acquisitionRoute(definition.id())),
                wiki("overview.chrome_cost", definition.chromeCost()),
                wiki("overview.integrity", CyberwareConditionHelper.getMaxIntegrity(definition))));
        addSection(sections, wiki("section.passive_effects"), buildCyberwarePassiveLines(definition));
        addSection(sections, wiki("section.special_behavior"), buildCyberwareBehaviorLines(definition));
        if (de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanProfile.supports(definition.id())) {
            sections.add(section(wiki("sandevistan.title"), List.of(wiki("sandevistan.controls"),
                    wiki("sandevistan.dilation"), wiki("sandevistan.maintenance"), wiki("sandevistan.recovery"), wiki("sandevistan.multiplayer"))));
        }
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
                filterSections(Arrays.asList(
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
                filterSections(Arrays.asList(
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

    private static List<WikiEntry> buildComponentEntries() {
        var materials = new java.util.TreeMap<String, net.minecraft.world.item.Item>();
        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.PARTS.forEach((id, item) -> materials.put(id, item.get()));
        for (var item : List.of(ModItems.COPPER_WIRING, ModItems.CONDUCTIVE_PASTE, ModItems.SERVO_SCREWS,
                ModItems.SENSOR_LENS, ModItems.MICRO_BATTERY, ModItems.REPLACEMENT_JOINT, ModItems.BASIC_CIRCUIT_PLATE,
                ModItems.COMMON_ITEM_COMPONENTS, ModItems.UNCOMMON_ITEM_COMPONENTS, ModItems.RARE_ITEM_COMPONENTS,
                ModItems.EPIC_ITEM_COMPONENTS, ModItems.LEGENDARY_ITEM_COMPONENTS))
            materials.put(item.getId().getPath(), item.get());
        return materials.entrySet().stream().map(entry -> {
            ItemStack stack = new ItemStack(entry.getValue());
            var sections = new ArrayList<ArticleSection>();
            sections.add(sectionKey("overview", wiki("component.uses")));
            addSection(sections, wiki("section.acquisition"), buildAcquisitionLines(entry.getKey()));
            addSection(sections, wiki("section.crafting"), List.of(), recipeVisual(stack));
            if (!recovery(stack).isEmpty())
                addSection(sections, wiki("section.recycling"), List.of(wiki("recycle.no_refund")), new CompactTransformVisual(stack, recovery(stack)));
            return new WikiEntry(entry.getKey(), stack.getHoverName(), stack, wiki("badge.component"),
                    wiki("component.uses"), List.of(), sections);
        }).toList();
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
                filterSections(Arrays.asList(
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
                "station_ripper",
                Component.translatable("block.cyberneticenhancements.ripper_station"),
                new ItemStack(ModBlocks.RIPPER_STATION.get()),
                wiki("station.ripper.subtitle"),
                Arrays.asList(
                        sectionKey("overview",
                                wiki("station.ripper.overview.1"),
                                wiki("station.ripper.overview.2")),
                        sectionKey("functions",
                                wiki("station.ripper.functions.1"),
                                wiki("station.ripper.functions.2"),
                                wiki("station.ripper.functions.3"),
                                wiki("station.ripper.functions.4")),
                        section(wiki("section.crafting_recipe"), List.of(), recipeVisual(new ItemStack(ModBlocks.RIPPER_STATION.get())))
                )
        );
    }

    private static WikiEntry buildTechStationEntry() {
        return buildStationEntry(
                "station_tech",
                Component.translatable("block.cyberneticenhancements.tech_station"),
                new ItemStack(ModBlocks.TECH_STATION.get()),
                wiki("station.tech.subtitle"),
                Arrays.asList(
                        sectionKey("overview",
                                wiki("station.tech.overview.1"),
                                wiki("station.tech.overview.2"),
                                wiki("station.tech.overview.3")),
                        sectionKey("functions",
                                wiki("station.tech.functions.1"),
                                wiki("station.tech.functions.2"),
                                wiki("station.tech.functions.3")),
                        section(wiki("section.crafting_recipe"), List.of(), recipeVisual(new ItemStack(ModBlocks.TECH_STATION.get())))
                )
        );
    }

    private static WikiEntry buildUpgradeStationEntry() {
        return buildStationEntry("station_upgrade", Component.translatable("block.cyberneticenhancements.upgrade_station"),
                new ItemStack(ModBlocks.UPGRADE_STATION.get()), wiki("station.upgrade.subtitle"), Arrays.asList(
                        sectionKey("overview", wiki("station.upgrade.overview"), wiki("station.upgrade.condition")),
                        sectionKey("functions", wiki("station.upgrade.cost"), wiki("station.upgrade.models"), wiki("station.upgrade.capacity")),
                        section(wiki("section.crafting_recipe"), List.of(), recipeVisual(new ItemStack(ModBlocks.UPGRADE_STATION.get())))));
    }
    private static WikiEntry buildRecyclerStationEntry() {
        return buildStationEntry(
                "station_recycler",
                Component.translatable("block.cyberneticenhancements.recycler_station"),
                new ItemStack(ModBlocks.RECYCLER_STATION.get()),
                wiki("station.recycler.subtitle"),
                Arrays.asList(
                        sectionKey("overview",
                                wiki("station.recycler.overview.1"),
                                wiki("station.recycler.overview.2")),
                        sectionKey("functions",
                                wiki("station.recycler.functions.1"),
                                wiki("station.recycler.functions.2"),
                                wiki("station.recycler.functions.3"),
                                wiki("station.recycler.functions.4")),
                        section(wiki("section.crafting_recipe"), List.of(), recipeVisual(new ItemStack(ModBlocks.RECYCLER_STATION.get())))
                )
        );
    }

    private static WikiEntry buildRelicCacheEntry() {
        return buildStationEntry(
                "station_relic_cache",
                wiki("station.relic_cache.title"),
                new ItemStack(ModBlocks.UNCOMMON_RELIC_CACHE.get()),
                wiki("station.relic_cache.subtitle"),
                Arrays.asList(
                        sectionKey("overview",
                                wiki("station.relic_cache.overview.1"),
                                wiki("station.relic_cache.overview.2"),
                                wiki("station.relic_cache.overview.3")),
                        sectionKey("spawn_sites",
                                wiki("station.relic_cache.spawn_sites.1"),
                                wiki("station.relic_cache.spawn_sites.2"),
                                wiki("station.relic_cache.spawn_sites.3"),
                                wiki("station.relic_cache.spawn_sites.4"),
                                wiki("station.relic_cache.spawn_sites.5"),
                                wiki("station.relic_cache.spawn_sites.6"),
                                wiki("station.relic_cache.spawn_sites.7")),
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
                Arrays.asList(
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
                Arrays.asList(
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
                Arrays.asList(
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
                Arrays.asList(
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
                Arrays.asList(
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
        return buildAcquisitionLines(definition.id());
    }

    private static List<Component> buildCyberwareCraftingLines(CyberwareDefinition definition) {
        return List.of(Component.literal("Assemble at the Cyberware Fabricator. Inspect machine tier and schematic requirements in its recipe browser."));
    }

    private static SectionVisual buildCyberwareCraftingVisual(CyberwareDefinition definition) {
        return fabricationVisual(definition.id());
    }

    private static List<Component> buildCyberwareServiceLines(CyberwareDefinition definition) {
        var lines = new ArrayList<Component>();
        lines.add(wiki("service.separate_stations"));
        lines.add(wiki("service.preserve_data"));
        lines.add(wiki("service.no_schematic"));
        var service = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.get(definition.id());
        if (service.repairMaterials().isEmpty()) lines.add(wiki("service.repair_disabled"));
        if (service.levels().isEmpty()) lines.add(wiki("service.upgrade_disabled"));
        return lines;
    }

    private static List<Component> buildCyberwareRecycleLines(CyberwareDefinition definition) {
        if (recovery(new ItemStack(ModItems.cyberware(definition.id()).get())).isEmpty())
            return List.of(wiki("recycle.unavailable"));
        return List.of(wiki("recycle.condition",
                formatNumber(de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe.GOOD_CONDITION_MINIMUM * 100),
                formatNumber(de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe.WORN_CONDITION_MINIMUM * 100),
                formatNumber(de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe.WORN_CONDITION_MINIMUM * 100)),
                wiki("recycle.example_good"), wiki("recycle.no_refund"));
    }

    private static List<ItemStack> recovery(ItemStack input) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level == null ? List.of() : CyberwareRecycleHelper.resolve(level, input).outputs();
    }

    private static SectionVisual buildCyberwareRecycleVisual(CyberwareDefinition definition) {
        if (recovery(new ItemStack(ModItems.cyberware(definition.id()).get())).isEmpty()) return null;
        return new CompactTransformVisual(
                new ItemStack(ModItems.cyberware(definition.id()).get()),
                recovery(new ItemStack(ModItems.cyberware(definition.id()).get()))
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
        return List.of(Component.literal("Assemble at the Cyberware Fabricator. Inspect machine tier and schematic requirements in its recipe browser."));
    }

    private static SectionVisual buildModuleCraftingVisual(CyberwareModuleDefinition definition) {
        return fabricationVisual(definition.id());
    }

    private static List<Component> buildModuleRecycleLines(CyberwareModuleDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildModuleRecycleVisual(CyberwareModuleDefinition definition) {
        if (recovery(new ItemStack(ModItems.module(definition.id()).get())).isEmpty()) return null;
        return new CompactTransformVisual(
                new ItemStack(ModItems.module(definition.id()).get()),
                recovery(new ItemStack(ModItems.module(definition.id()).get()))
        );
    }

    private static List<Component> buildChipwareCraftingLines(ChipwareDefinition definition) {
        return List.of(Component.literal("Assemble at the Cyberware Fabricator. Inspect machine tier and schematic requirements in its recipe browser."));
    }

    private static SectionVisual buildChipwareCraftingVisual(ChipwareDefinition definition) {
        return fabricationVisual(definition.id());
    }

    private static List<Component> buildChipwareRecycleLines(ChipwareDefinition definition) {
        return List.of();
    }

    private static SectionVisual buildChipwareRecycleVisual(ChipwareDefinition definition) {
        if (recovery(new ItemStack(ModItems.chipware(definition.id()).get())).isEmpty()) return null;
        return new CompactTransformVisual(
                new ItemStack(ModItems.chipware(definition.id()).get()),
                recovery(new ItemStack(ModItems.chipware(definition.id()).get()))
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
            case OXY_BOOSTER ->
                    lines.add(wiki("consumable.water_breathing", ticksToSeconds(CyberwareBalance.intValue("consumable.oxy_booster.duration_ticks"))));
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
        return recipeVisual(new ItemStack(ModItems.consumable(definition.id()).get()));
    }

    private static List<Component> buildConsumableRecycleLines(CyberConsumableDefinition definition) {
        return List.of();
    }

    private static List<Component> buildCraftedItemAcquisitionLines(String itemId) {
        return buildAcquisitionLines(itemId);
    }

    private static Component acquisitionRoute(String itemId) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return wiki("acquisition.server_recipes");
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", itemId));
        var designs = level.getRecipeManager().getAllRecipesFor(de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes.FABRICATION.get()).stream()
                .filter(holder -> holder.value().result().is(item)).toList();
        if (!designs.isEmpty()) return wiki(designs.stream().anyMatch(holder -> holder.value().schematic().isEmpty())
                ? "acquisition.route_public" : "acquisition.route_schematic");
        return wiki(level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .anyMatch(holder -> holder.value().getResultItem(level.registryAccess()).is(item))
                ? "acquisition.route_crafting" : "acquisition.route_exploration");
    }

    private static List<Component> buildAcquisitionLines(
            String itemId
    ) {
        var lines = new ArrayList<Component>();
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return List.of(wiki("acquisition.server_recipes"));
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", itemId));
        var fabrication = level.getRecipeManager().getAllRecipesFor(de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes.FABRICATION.get()).stream()
                .filter(holder -> holder.value().result().is(item)).toList();
        if (!fabrication.isEmpty()) {
            lines.add(wiki("acquisition.fabricator"));
            if (fabrication.stream().anyMatch(holder -> holder.value().schematic().isEmpty()))
                lines.add(wiki("acquisition.public_design"));
            else lines.add(wiki("acquisition.schematic_design"));
        } else if (level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .anyMatch(holder -> holder.value().getResultItem(level.registryAccess()).is(item))) {
            lines.add(wiki("acquisition.craftable"));
        } else lines.add(wiki("acquisition.no_recipe"));
        if (de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.SCHEMATICS.containsKey(itemId)) {
            var definition = CyberwareCatalog.get(itemId);
            lines.add(wiki(definition.acquisitionMethod() == CyberwareAcquisitionMethod.QUEST
                    ? "acquisition.quest" : "acquisition.discovery"));
        }
        for (var type : de.artemis.cyberneticenhancements.common.entity.NpcType.values())
            for (int option : SHOP_OPTIONS) {
                var stock = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.shopItem(type, option);
                if (stock.is(item))
                    lines.add(wiki("acquisition.shop", type.displayName(),
                            de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.requiredTrust(type, option)));
            }
        lines.add(wiki("acquisition.loot_configured"));
        return lines;
    }
    private static SectionVisual buildConsumableRecycleVisual(CyberConsumableDefinition definition) {
        if (recovery(new ItemStack(ModItems.consumable(definition.id()).get())).isEmpty()) return null;
        return new CompactTransformVisual(
                new ItemStack(ModItems.consumable(definition.id()).get()),
                recovery(new ItemStack(ModItems.consumable(definition.id()).get()))
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
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.station"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.infrastructure")), filterSections(sections));
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
        return new WikiEntry(id, title, icon, Component.translatable("wiki.cyberneticenhancements.badge.quest"), subtitle, List.of(Component.translatable("wiki.cyberneticenhancements.chip.infrastructure")), filterSections(sections));
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

    record ArchiveTab(String id, Component title, Component subtitle, Component navigationTitle,
                      List<WikiTopic> topics) {
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

    record CraftingGridVisual(List<ItemStack[]> grids, ItemStack output, String schematic,
                              net.minecraft.resources.ResourceLocation recipeId) implements SectionVisual {
        CraftingGridVisual(List<ItemStack[]> grids, ItemStack output) {
            this(grids, output, "", null);
        }

        CraftingGridVisual(List<ItemStack[]> grids, ItemStack output, String schematic) {
            this(grids, output, schematic, null);
        }

        boolean unlocked() {
            if (recipeId != null) {
                var level = net.minecraft.client.Minecraft.getInstance().level;
                if (level == null) return false;
                var current = level.getRecipeManager().byKey(recipeId).orElse(null);
                if (current == null) return false;
                if (current.value() instanceof de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe recipe) {
                    if (!recipe.clientUnlocked()) return false;
                } else if (!(current.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe)) return false;
            }
            return de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.clientKnows(schematic);
        }
    }

    record CompactTransformVisual(ItemStack input, List<ItemStack> outputs) implements SectionVisual {
    }

    record ServiceOperationVisual(Component label, Component note, ItemStack input, List<ItemStack> materials,
                                  ItemStack output) {
    }

    record UpgradeStageVisual(Component label, List<ItemStack> materials, List<Component> changes) {
    }

    record ServiceOperationsVisual(List<Component> summaryChips, List<ServiceOperationVisual> operations,
                                   List<UpgradeStageVisual> upgradeStages) implements SectionVisual {
    }

    record RelicCacheExampleVisual(int gridSize, int[] gridTokens, int[] pathCells,
                                   int[][] daemonTokens) implements SectionVisual {
    }

    record ArticleSection(Component title, List<Component> lines, SectionVisual visual) {
        ArticleSection(Component title, List<Component> lines) {
            this(title, lines, null);
        }
    }
}
