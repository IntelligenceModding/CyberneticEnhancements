package de.artemis.cyberneticenhancements.client.screen;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContentLayout;
import de.artemis.cyberneticenhancements.client.ui.WikiArticleLayout;
import de.artemis.cyberneticenhancements.client.ui.WikiSearch;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ReferenceTableVisual;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.ArchiveBankingClientState;
import de.artemis.cyberneticenhancements.client.ArchiveContactsClientState;
import de.artemis.cyberneticenhancements.client.ArchiveQuestsClientState;
import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ArchiveTab;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ArticleSection;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.CompactTransformVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.CraftingGridVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.RelicCacheExampleVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.StructureViewVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.UpgradeStageVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.SectionVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ServiceOperationVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ServiceOperationsVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiEntry;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiSection;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiTopic;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactActionPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsRequestPayload;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.ArchiveContactLayout;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import de.artemis.cyberneticenhancements.client.ui.UiOpeningAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.screens.Screen;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CyberwareWikiScreen extends ResponsiveScreen {
    private static final int OUTER_MARGIN = 14;
    private static final int PANEL_GAP = 10;
    private static final int TOP_BAR_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 0;
    private static final int NAV_WIDTH = 230;
    private static final int LIST_WIDTH = 230;
    private static final int ROW_HEIGHT = 22;
    private static final int NAV_CHILD_INDENT = 4;
    private static final int ENTRY_ROW_HEIGHT = 27;
    private static final int SECTION_GAP = 10;
    private static final int SCROLLBAR_WIDTH = de.artemis.cyberneticenhancements.client.ui.UiScrollbar.WIDTH;
    private static final int DECEASED_COLOR = 0xFFF17883;
    private final java.util.Map<DragTarget, de.artemis.cyberneticenhancements.client.ui.UiScrollbar> scrollbars = new java.util.EnumMap<>(DragTarget.class);
    private static final int PANEL_HEADER_HEIGHT = 32;
    private static final int CHIP_HEIGHT = 16;
    private static final int VISUAL_SLOT_STRIDE = 20;
    private static final int VISUAL_CONTENT_PADDING = 4;
    private static final int TRANSFORM_OUTPUT_START = 58;
    private static final int SERVICE_MATERIALS_START = 34;
    private static final int SERVICE_OUTPUT_GAP = 24;
    private static final int RELIC_EXAMPLE_CELL_SIZE = 18;
    private static final int RELIC_EXAMPLE_CELL_GAP = 3;
    private static final int RELIC_EXAMPLE_ROUTE_THICKNESS = 2;
    private static final int PIN_BUTTON_WIDTH = 20;
    private static final int CLOSE_BUTTON_WIDTH = de.artemis.cyberneticenhancements.client.ui.UiCloseControl.WIDTH;
    private static final int TOP_TAB_HEIGHT = de.artemis.cyberneticenhancements.client.ui.UiCloseControl.HEIGHT;
    private static final int TOP_TAB_GAP = 10;
    private static final int CONTACT_FILTER_TAB_HEIGHT = 20;
    private static final int CONTACT_FILTER_TAB_GAP = 10;
    private static final long CONTENT_ANIMATION_MS = 180L;
    private static final long BOOT_SEQUENCE_MS = 1200L;
    private static final long BOOT_FADE_MS = 300L;
    private final SchematicBrowser schematicBrowser = new SchematicBrowser();
    private final TarotBrowser tarotBrowser = new TarotBrowser();
    private final ContractBrowser contractBrowser = new ContractBrowser(this::closeForPlacement);
    private final BankingBrowser bankingBrowser = new BankingBrowser();
    private final CompassBrowser compassBrowser = new CompassBrowser();
    private final HudBrowser hudBrowser = new HudBrowser();
    private final JournalBrowser journalBrowser = new JournalBrowser();
    private boolean initialized;
    private static final String BANKING_TAB_ID = "banking";
    private static final String CONTACTS_TAB_ID = "contacts";
    private static final String QUESTS_TAB_ID = "quests";
    private static final String CONTACT_FILTER_ALL = "all";
    private static final String CONTACT_FILTER_PINNED = "pinned";
    private static final String CONTACT_FILTER_HIDDEN = "hidden";
    private static final String CONTACT_FILTER_DECEASED = "deceased";
    private static final String PINNED_TOPIC_ID = "pinned_entries_root";
    private static final String PINNED_SECTION_ID = "pinned_entries";
    private static final Set<String> PINNED_ENTRY_IDS = new LinkedHashSet<>();
    private static final Map<Item, String> ENTRY_IDS_BY_ITEM = createEntryIdsByItem();
    private static final Map<Block, String> ENTRY_IDS_BY_BLOCK = createEntryIdsByBlock();

    private final Screen parent;
    private final List<ArchiveTab> archiveTabs = new ArrayList<>(CyberwareWikiData.buildArchiveTabs());
    private int schematicRevision = de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.revision();
    private int economyRevision = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.clientRevision();
    private int referenceRevision = de.artemis.cyberneticenhancements.client.ArchiveReferenceData.revision();
    private int upgradeRevision = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.clientRevision();
    private final Map<String, WikiEntry> entryIndex = new HashMap<>();
    private final Map<String, String> tabByEntryId = new HashMap<>();
    private final Map<String, WikiSection> baseSectionsByEntryId = new HashMap<>();
    private final Set<String> expandedTopics = new LinkedHashSet<>();
    private final Map<String, Float> topicExpansion = new HashMap<>();
    private final Map<String, Float> navEmphasis = new HashMap<>();
    private final Map<String, Float> entryEmphasis = new HashMap<>();
    private final ScrollState navScroll = new ScrollState();
    private final ScrollState listScroll = new ScrollState();
    private final ScrollState articleScroll = new ScrollState();
    private final boolean showOpeningAnimation = de.artemis.cyberneticenhancements.client.ClientConfig.SHOW_ARCHIVE_BOOT_ANIMATION.get();
    private final long openedAt = System.currentTimeMillis();
    private long contentChangedAt = openedAt + BOOT_SEQUENCE_MS;
    private long sectionChangedAt = openedAt + BOOT_SEQUENCE_MS;

    private ArchiveTab activeTab;
    private WikiSection selectedSection;
    private WikiEntry selectedEntry;
    private de.artemis.cyberneticenhancements.client.ui.UiEditBox wikiSearch;
    private String wikiQuery = "", wikiBeforeSearchEntry = "";
    private WikiSection wikiSearchResults;
    private List<WikiSearch.Document> wikiSearchDocuments = List.of();
    private final Map<String, String> wikiSearchContexts = new HashMap<>();
    private Component hoveredSearchContext;
    private DragTarget dragTarget = DragTarget.NONE;
    private int articleContentHeight;
    private ItemStack hoveredVisualStack = ItemStack.EMPTY;
    private Component hoveredPinTooltip;
    private final UiFadeLayer openingFade = new UiFadeLayer();
    private final UiOpeningAnimation openingAnimation = new UiOpeningAnimation(System.nanoTime(), BOOT_SEQUENCE_MS, BOOT_FADE_MS, showOpeningAnimation);
    private UiOpeningAnimation.Frame openingFrame = openingAnimation.sample(System.nanoTime());
    private int currentMouseX;
    private int currentMouseY;
    private final String initialEntryId;
    private int contactsRefreshCooldown;
    private final Set<String> seenDeceasedContacts = new LinkedHashSet<>();
    private String activeContactsFilterId = CONTACT_FILTER_ALL;
    private de.artemis.cyberneticenhancements.client.ui.UiEditBox contactsSearch;
    private String contactsQuery = "", contactsType = "";

    public CyberwareWikiScreen(Screen parent) {
        this(parent, null);
    }

    public CyberwareWikiScreen(Screen parent, String initialEntryId) {
        super(Component.translatable("screen.cyberneticenhancements.archive.title"));
        this.parent = parent;
        this.initialEntryId = initialEntryId;
        applySavedTabOrder();
        this.activeTab = archiveTabs.isEmpty() ? null : archiveTabs.getFirst();
        indexTabs(archiveTabs);
        if (initialEntryId != null) {
            ArchiveTab targetTab = archiveTabById(tabByEntryId.get(initialEntryId));
            if (targetTab != null) {
                this.activeTab = targetTab;
            }
        }
    }

    @Override
    protected void init() {
        PINNED_ENTRY_IDS.clear();
        PINNED_ENTRY_IDS.addAll(de.artemis.cyberneticenhancements.client.ClientConfig.WIKI_PINNED_ENTRIES.get());
        boolean firstInitialization = !initialized;
        super.init();
        openingFade.prepare();
        initialized = true;
        if (firstInitialization) {
            activateTab(activeTab);
            if (initialEntryId != null) {
                selectEntryById(initialEntryId);
            }
            if (!showOpeningAnimation) {
                contentChangedAt = sectionChangedAt = System.currentTimeMillis() - CONTENT_ANIMATION_MS;
            }
        }
    }

    @Override
    public void onClose() {
        cancelArchiveTabDrag();
        ArchiveBankingClientState.clear();
        ArchiveContactsClientState.clear();
        ArchiveQuestsClientState.clear();
        Minecraft.getInstance().setScreen(parent);
    }

    private void closeForPlacement() {
        onClose();
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.closeContainer();
        else minecraft.setScreen(null);
    }

    public void onDropoffClaimResult(boolean granted) {
        if (contractBrowser.dropoffClaimResult(granted) && isQuestsTab()) closeForPlacement();
    }

    @Override
    public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) {
            for (String npcId : seenDeceasedContacts) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(
                        ArchiveContactActionPayload.ACTION_ACKNOWLEDGE_DECEASED, npcId));
            }
        }
        seenDeceasedContacts.clear();
        cancelArchiveTabDrag();
        compassBrowser.hide();
        openingFade.close();
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.flush();
        if (minecraft.level == null) {
            renderPanorama(graphics, partialTick);
            graphics.flush();
        }
        renderBlurredBackground(partialTick);
        if (!de.artemis.cyberneticenhancements.client.TitleSlideshow.renderedForCurrentScreen())
            graphics.fill(0, 0, width, height, 0x28081018);
    }

    @Override
    public void tick() {
        super.tick();
        int revision = de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.revision();
        int references = de.artemis.cyberneticenhancements.client.ArchiveReferenceData.revision();
        int upgrades = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.clientRevision();
        int economy = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.clientRevision();
        if (revision != schematicRevision || references != referenceRevision || upgrades != upgradeRevision || economy != economyRevision) {
            economyRevision = economy;
            schematicRevision = revision;
            referenceRevision = references;
            upgradeRevision = upgrades;
            refreshReferenceEntries();
        }
        if ((!isContactsTab() && !isQuestsTab() && !isBankingTab()) || minecraft == null || minecraft.player == null) {
            return;
        }
        if (contactsRefreshCooldown > 0) {
            contactsRefreshCooldown--;
            return;
        }
        if (isContactsTab()) {
            requestContactsRefresh();
        } else if (isBankingTab()) {
            requestBankingRefresh();
        } else {
            requestQuestsRefresh();
        }
        contactsRefreshCooldown = 20;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        openingFrame = openingAnimation.sample(System.nanoTime());
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        hoveredVisualStack = ItemStack.EMPTY;
        hoveredPinTooltip = null;
        hoveredSearchContext = null;

        navScroll.tick();
        listScroll.tick();
        articleScroll.tick();
        tickTopicExpansion(displayTopics());
        int rootX = OUTER_MARGIN;
        int rootY = OUTER_MARGIN;
        int rootWidth = width - OUTER_MARGIN * 2;
        int rootHeight = height - OUTER_MARGIN * 2;

        if (openingFrame.wikiOpacity() <= 0.0F) {
            renderBootSequence(guiGraphics, rootX, rootY, rootWidth, rootHeight);
            return;
        }

        openingFade.render(guiGraphics, openingFrame.wikiOpacity(), openingFrame.wikiScale(), openingFrame.wikiOffsetY(),
                () -> renderArchiveContent(guiGraphics, mouseX, mouseY, partialTick));
        renderBootSequence(guiGraphics, rootX, rootY, rootWidth, rootHeight);
    }

    private void renderArchiveContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RootLayout layout = rootLayout();
        WikiFrameRenderer.drawPanel(guiGraphics, layout.rootX(), layout.rootY(), layout.rootWidth(), layout.rootHeight(), StationScreenStyle.PANEL_BG);
        drawTopBar(guiGraphics, layout.rootX(), layout.rootY(), layout.rootWidth());
        if (isWikiTab()) renderWikiSearch(guiGraphics, layout, mouseX, mouseY, partialTick);

        Pane navPane = layout.navPane();
        Pane listPane = layout.listPane();
        Pane articlePane = layout.articlePane();
        if (isHudTab()) {
            hudBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
            return;
        }
        if (isJournalTab()) {
            journalBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
            return;
        }
        journalBrowser.hide();
        if (isCompassTab()) {
            bankingBrowser.hide();
            compassBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
            return;
        }
        compassBrowser.hide();
        boolean bankingTab = isBankingTab();
        boolean contactsTab = isContactsTab();
        boolean questsTab = isQuestsTab();
        if (!bankingTab) {
            bankingBrowser.hide();
        }

        if (isLoreTab()) {
            tarotBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
        } else if (isSchematicsTab()) {
            schematicBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
        } else if (questsTab) {
            contractBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
        } else if (bankingTab) {
            bankingBrowser.render(guiGraphics, font, articlePane.x, articlePane.y, articlePane.width, articlePane.height, mouseX, mouseY);
        } else if (!bankingTab && !contactsTab) {
            if (!wikiSearching()) drawPanelShell(guiGraphics, navPane, activeTab == null ? Component.translatable("screen.cyberneticenhancements.archive.topics") : activeTab.navigationTitle(), 0);
            if (!layout.listCollapsed()) {
                drawPanelShell(guiGraphics, listPane, selectedSection == null ? Component.translatable("screen.cyberneticenhancements.archive.entries") : selectedSection.title());
            }
            drawPanelShell(guiGraphics, articlePane, selectedEntry == null ? Component.translatable("screen.cyberneticenhancements.archive.entry") : selectedEntry.title(), selectedEntry == null ? 0 : PIN_BUTTON_WIDTH + 12);
            if (!wikiSearching()) renderNavigation(guiGraphics, navPane, mouseX, mouseY);
            if (!layout.listCollapsed()) {
                renderEntryList(guiGraphics, listPane, mouseX, mouseY);
            }
            renderArticle(guiGraphics, articlePane);
            renderPinButton(guiGraphics, articlePane);
        } else {
            UiRenderer.panel(guiGraphics, articlePane.x, articlePane.y, articlePane.width, articlePane.height, UiRenderer.PANEL);
            renderContactsRoster(guiGraphics, articlePane);
        }

        if (!bootSequenceBlocksInput() && !hoveredVisualStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredVisualStack, mouseX, mouseY);
        }
        if (!bootSequenceBlocksInput() && hoveredPinTooltip != null) {
            guiGraphics.renderTooltip(font, hoveredPinTooltip, mouseX, mouseY);
        } else if (!bootSequenceBlocksInput() && hoveredSearchContext != null) {
            guiGraphics.renderTooltip(font, font.split(hoveredSearchContext, 300), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClickedContent(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClickedContent(mouseX, mouseY, button);
        }

        if (bootSequenceBlocksInput()) {
            return true;
        }

        RootLayout layout = rootLayout();
        boolean bankingTab = isBankingTab();
        boolean contactsTab = isContactsTab();
        boolean questsTab = isQuestsTab();
        if (button == 0 && handleArchiveTabClick(mouseX, mouseY, layout.rootX(), layout.rootY(), layout.rootWidth())) {
            return true;
        }

        if (isInside(mouseX, mouseY, closeButtonBounds(layout.rootX(), layout.rootY(), layout.rootWidth()))) {
            playClick();
            onClose();
            return true;
        }

        if (isHudTab()) return button == 0 && hudBrowser.click(mouseX, mouseY);
        if (isCompassTab()) return compassBrowser.click(mouseX, mouseY);
        if (isJournalTab()) return journalBrowser.click(mouseX, mouseY);
        if (isWikiTab() && wikiSearch != null) {
            var clear = wikiSearchClearBounds(layout);
            if (isInside(mouseX, mouseY, clear)) {
                if (!wikiQuery.isEmpty()) { wikiSearch.setValue(""); playClick(); }
                wikiSearch.setFocused(true);
                return true;
            }
            boolean searchClicked = wikiSearch.mouseClicked(mouseX, mouseY, button);
            wikiSearch.setFocused(searchClicked);
            if (searchClicked) return true;
        }
        if (isLoreTab()) {
            boolean handled = tarotBrowser.click(mouseX, mouseY);
            if (handled) playClick();
            String placement = tarotBrowser.takePlacementRequest();
            if (placement != null) {
                closeForPlacement();
                de.artemis.cyberneticenhancements.client.TarotDecorationClient.start(placement);
            }
            return handled;
        }
        if (isSchematicsTab()) return schematicBrowser.click(mouseX, mouseY);
        if (isQuestsTab()) return contractBrowser.click(mouseX, mouseY);
        if (bankingTab) return bankingBrowser.click(mouseX, mouseY);
        if (contactsTab && handleContactsSearchClick(mouseX, mouseY, layout.articlePane())) return true;

        if (!bankingTab && !contactsTab && !questsTab) {
            if (!wikiSearching() && tryStartScrollbarDrag(mouseX, mouseY, layout.navPane(), navScroll, maxNavScroll(), DragTarget.NAV)) {
                return true;
            }
            if (!layout.listCollapsed() && tryStartScrollbarDrag(mouseX, mouseY, layout.listPane(), listScroll, maxListScroll(), DragTarget.LIST)) {
                return true;
            }
            if (tryStartScrollbarDrag(mouseX, mouseY, layout.articlePane(), articleScroll, maxArticleScroll(layout.articlePane()), DragTarget.ARTICLE)) {
                return true;
            }
        } else if (tryStartScrollbarDrag(mouseX, mouseY, layout.articlePane(), articleScroll,
                maxContactsScroll(layout.articlePane()), DragTarget.ARTICLE)) {
            return true;
        }

        if (!bankingTab && !contactsTab && !questsTab && handlePinButtonClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }

        if (!bankingTab && !contactsTab && !questsTab && !wikiSearching() && handleNavigationClick(mouseX, mouseY, layout.navPane())) {
            return true;
        }
        if (!bankingTab && !contactsTab && !questsTab && !layout.listCollapsed() && handleEntryClick(mouseX, mouseY, layout.listPane())) {
            return true;
        }
        if (contactsTab && handleContactsClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }

        return super.mouseClickedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseReleasedContent(double mouseX, double mouseY, int button) {
        tarotBrowser.release();
        if (button == 0 && draggedArchiveTab != null) return finishArchiveTabDrag(mouseX, mouseY);
        schematicBrowser.release();
        contractBrowser.release();
        bankingBrowser.release();
        compassBrowser.release();
        hudBrowser.release();
        journalBrowser.release();
        scrollbars.values().forEach(de.artemis.cyberneticenhancements.client.ui.UiScrollbar::release);
        if (bootSequenceBlocksInput()) {
            dragTarget = DragTarget.NONE;
            return true;
        }
        if (button == 0) {
            dragTarget = DragTarget.NONE;
        }
        return super.mouseReleasedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedContent(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggedArchiveTab != null) {
            updateArchiveTabDrag(mouseX, mouseY);
            return true;
        }
        if (isLoreTab()) return tarotBrowser.drag(mouseX, mouseY);
        if (isSchematicsTab()) return schematicBrowser.drag(mouseX, mouseY);
        if (isQuestsTab()) return contractBrowser.drag(mouseX, mouseY);
        if (isBankingTab()) return bankingBrowser.drag(mouseX, mouseY);
        if (isHudTab()) return hudBrowser.drag(mouseX, mouseY);
        if (isCompassTab()) return compassBrowser.drag(mouseX, mouseY);
        if (isJournalTab()) return journalBrowser.drag(mouseX, mouseY);
        if (bootSequenceBlocksInput()) {
            return true;
        }
        if (button == 0 && dragTarget == DragTarget.NONE && structureViewArea.length == 4 && mouseX >= structureViewArea[0] && mouseX < structureViewArea[2]
                && mouseY >= structureViewArea[1] && mouseY < structureViewArea[3]) {
            StructureView.drag(dragX, dragY);
            return true;
        }
        if (button != 0 || dragTarget == DragTarget.NONE) {
            return super.mouseDraggedContent(mouseX, mouseY, button, dragX, dragY);
        }

        RootLayout layout = rootLayout();
        switch (dragTarget) {
            case NAV -> {
                if (!isContactsTab() && !isQuestsTab()) {
                    dragScrollbar(navScroll, layout.navPane(), mouseY, maxNavScroll());
                }
            }
            case LIST -> {
                if (!isContactsTab() && !isQuestsTab() && !layout.listCollapsed()) {
                    dragScrollbar(listScroll, layout.listPane(), mouseY, maxListScroll());
                }
            }
            case ARTICLE ->
                    dragScrollbar(articleScroll, layout.articlePane(), mouseY, isContactsTab() ? maxContactsScroll(layout.articlePane()) : maxArticleScroll(layout.articlePane()));
            default -> {
            }
        }
        return true;
    }

    @Override
    protected boolean mouseScrolledContent(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (bootSequenceBlocksInput()) {
            return true;
        }
        RootLayout layout = rootLayout();
        double amount = -scrollY * de.artemis.cyberneticenhancements.client.ui.UiScrollState.WHEEL_STEP;
        if (isLoreTab()) return tarotBrowser.scroll(mouseX, mouseY, scrollY);
        if (isSchematicsTab()) return schematicBrowser.scroll(mouseX, mouseY, scrollY);
        if (isQuestsTab()) return contractBrowser.scroll(mouseX, mouseY, scrollY);
        if (isBankingTab()) return bankingBrowser.scroll(mouseX, mouseY, scrollY);
        if (isHudTab()) return hudBrowser.scroll(mouseX, mouseY, scrollY);
        if (isCompassTab()) return compassBrowser.scroll(mouseX, mouseY, scrollY);
        if (isJournalTab()) return journalBrowser.scroll(mouseX, mouseY, scrollY);
        if (isLoreTab() || isSchematicsTab() || isBankingTab() || isContactsTab() || isQuestsTab()) {
            if (isInsidePane(mouseX, mouseY, layout.articlePane())) {
                articleScroll.add(amount, maxContactsScroll(layout.articlePane()));
                return true;
            }
            return super.mouseScrolledContent(mouseX, mouseY, scrollX, scrollY);
        }
        if (isInsidePane(mouseX, mouseY, layout.navPane())) {
            navScroll.add(amount, maxNavScroll());
            return true;
        }
        if (!layout.listCollapsed() && isInsidePane(mouseX, mouseY, layout.listPane())) {
            listScroll.add(amount, maxListScroll());
            return true;
        }
        if (isInsidePane(mouseX, mouseY, layout.articlePane())) {
            articleScroll.add(amount, maxArticleScroll(layout.articlePane()));
            return true;
        }
        return super.mouseScrolledContent(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && draggedArchiveTab != null) { cancelArchiveTabDrag(); return true; }
        if (isHudTab() && !bootSequenceBlocksInput() && hudBrowser.key(keyCode, scanCode, modifiers)) return true;
        if (isCompassTab() && !bootSequenceBlocksInput() && compassBrowser.key(keyCode, scanCode, modifiers)) return true;
        if (isJournalTab() && !bootSequenceBlocksInput() && journalBrowser.key(keyCode, scanCode, modifiers)) return true;
        if (isContactsTab() && contactsSearch != null && contactsSearch.isFocused() && !bootSequenceBlocksInput()) {
            if (keyCode == 256) contactsSearch.setFocused(false);
            else { contactsSearch.keyPressed(keyCode, scanCode, modifiers); return true; }
        }
        if (isWikiTab() && wikiSearch != null && wikiSearch.isFocused() && !bootSequenceBlocksInput()) {
            if (keyCode == 256) wikiSearch.setFocused(false);
            else { wikiSearch.keyPressed(keyCode, scanCode, modifiers); return true; }
        }
        if (isSchematicsTab() && schematicBrowser.key(keyCode, scanCode, modifiers)) return true;
        if (isQuestsTab() && contractBrowser.key(keyCode, scanCode, modifiers)) return true;
        if (isBankingTab() && bankingBrowser.key(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256 || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (bootSequenceBlocksInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isHudTab() && !bootSequenceBlocksInput()) return hudBrowser.character(codePoint, modifiers);
        if (isCompassTab() && !bootSequenceBlocksInput()) return compassBrowser.character(codePoint, modifiers);
        if (isJournalTab() && !bootSequenceBlocksInput()) return journalBrowser.character(codePoint, modifiers);
        if (isContactsTab() && contactsSearch != null && contactsSearch.isFocused() && !bootSequenceBlocksInput())
            return contactsSearch.charTyped(codePoint, modifiers);
        if (isWikiTab() && wikiSearch != null && wikiSearch.isFocused() && !bootSequenceBlocksInput()) return wikiSearch.charTyped(codePoint, modifiers);
        if (isSchematicsTab() && schematicBrowser.character(codePoint, modifiers)) return true;
        if (isQuestsTab() && contractBrowser.character(codePoint, modifiers)) return true;
        if (isBankingTab() && bankingBrowser.character(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isWikiTab() { return activeTab != null && activeTab.id().equals("wiki"); }

    private boolean wikiSearching() { return isWikiTab() && !wikiQuery.isEmpty(); }

    private Rect wikiSearchClearBounds(RootLayout layout) {
        return new Rect("wiki_search_clear", layout.rootX() + layout.rootWidth() - PANEL_GAP - 80,
                layout.rootY() + TOP_BAR_HEIGHT + PANEL_GAP + 10, 70, 22);
    }

    private void renderWikiSearch(GuiGraphics graphics, RootLayout layout, int mouseX, int mouseY, float partialTick) {
        int x = layout.rootX() + PANEL_GAP, y = layout.rootY() + TOP_BAR_HEIGHT + PANEL_GAP;
        int width = layout.rootWidth() - PANEL_GAP * 2;
        UiRenderer.panel(graphics, x, y, width, 42, UiRenderer.PANEL);
        var hint = Component.translatable("screen.cyberneticenhancements.archive.wiki.search");
        if (wikiSearch == null) {
            wikiSearch = new de.artemis.cyberneticenhancements.client.ui.UiEditBox(font, x + 10, y + 10, width - 100, 22, hint);
            wikiSearch.setMaxLength(80);
            wikiSearch.setHint(hint);
            wikiSearch.setResponder(this::applyWikiSearch);
        }
        wikiSearch.setX(x + 10);
        wikiSearch.setY(y + 10);
        wikiSearch.setWidth(width - 100);
        wikiSearch.render(graphics, mouseX, mouseY, partialTick);
        var clear = wikiSearchClearBounds(layout);
        UiRenderer.button(graphics, font, clear.x(), clear.y(), clear.width(), clear.height(),
                Component.translatable("screen.cyberneticenhancements.archive.wiki.clear"), !wikiQuery.isEmpty(), false,
                !bootSequenceBlocksInput() && isInside(mouseX, mouseY, clear), -1);
    }

    private void applyWikiSearch(String value) {
        String query = value.strip().toLowerCase(Locale.ROOT);
        boolean changed = !query.equals(wikiQuery);
        if (wikiQuery.isEmpty() && !query.isEmpty()) wikiBeforeSearchEntry = selectedEntry == null ? "" : selectedEntry.id();
        wikiQuery = query;
        navScroll.reset();
        listScroll.reset();
        dragTarget = DragTarget.NONE;
        scrollbars.values().forEach(de.artemis.cyberneticenhancements.client.ui.UiScrollbar::release);
        if (query.isEmpty()) {
            wikiSearchResults = null;
            wikiSearchContexts.clear();
            String entry = selectedEntry == null ? wikiBeforeSearchEntry : selectedEntry.id();
            if (entryIndex.containsKey(entry)) selectEntryById(entry);
            else selectFirstSection();
            return;
        }
        var hits = WikiSearch.search(wikiSearchDocuments, query);
        wikiSearchContexts.clear();
        hits.forEach(hit -> wikiSearchContexts.put(hit.id(), hit.context()));
        var results = hits.stream().map(hit -> entryIndex.get(hit.id())).filter(java.util.Objects::nonNull).toList();
        wikiSearchResults = new WikiSection("wiki_search_results", Component.translatable("screen.cyberneticenhancements.archive.wiki.results", results.size()), results);
        selectedSection = wikiSearchResults;
        WikiEntry next = changed || selectedEntry == null ? null : results.stream().filter(entry -> entry.id().equals(selectedEntry.id())).findFirst().orElse(null);
        if (next == null && !results.isEmpty()) next = results.getFirst();
        setSelectedEntry(next);
        sectionChangedAt = animationAnchorTime();
    }

    private void renderNavigation(GuiGraphics guiGraphics, Pane pane, int mouseX, int mouseY) {
        NavLayout layout = buildNavLayout();
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = Math.max(0.0D, layout.totalHeight() - viewportHeight);
        int rowAreaWidth = pane.width - 12 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        navScroll.clamp(maxScroll);

        guiGraphics.enableScissor(pane.x + 4, contentY, pane.x + pane.width - 8, contentY + viewportHeight);
        for (NavRow row : layout.rows()) {
            int rowY = contentY + Math.round(row.y()) - (int) Math.round(navScroll.current);
            if (rowY + ROW_HEIGHT < contentY || rowY > contentY + viewportHeight || row.alpha() < 0.04F) {
                continue;
            }

            int rowX = pane.x + 6 + row.depth() * NAV_CHILD_INDENT;
            int rowWidth = rowAreaWidth - 4 - row.depth() * NAV_CHILD_INDENT;
            boolean hovered = mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            boolean selected = row.topic().section() != null && selectedSection != null && selectedSection.id().equals(row.topic().section().id());
            float emphasis = animateToward(navEmphasis, row.topic().id(), selected ? hovered ? 1.0F : 0.82F : hovered ? 0.55F : 0.0F, 0.22F);
            drawListRow(guiGraphics, rowX, rowY, rowWidth, ROW_HEIGHT - 2, selected, emphasis, row.alpha());

            int textX = pane.x + 28 + row.depth() * 12;
            if (row.topic().hasChildren()) {
                UiRenderer.disclosureChevron(guiGraphics, textX - 11, rowY + ROW_HEIGHT / 2.0F,
                        topicExpansion.getOrDefault(row.topic().id(), expandedTopics.contains(row.topic().id()) ? 1.0F : 0.0F),
                        withAlpha(hovered || selected ? StationScreenStyle.ACCENT : StationScreenStyle.TEXT_SECONDARY, row.alpha()));
            }
            int entryCount = topicEntryCount(row.topic());
            String count = entryCount == 1 ? "" : Integer.toString(entryCount);
            int countX = pane.x + rowAreaWidth - 11 - font.width(count);
            guiGraphics.drawString(font, trimStyled(row.topic().title(), Math.max(1, countX - textX - 8)), textX, rowY + 6,
                    withAlpha(UiRenderer.TEXT_SECONDARY, row.alpha()), false);
            guiGraphics.drawString(font, count, countX, rowY + 6, withAlpha(UiRenderer.TEXT_SECONDARY, row.alpha()), false);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, navScroll, maxScroll, viewportHeight, contentY);
    }

    private void renderEntryList(GuiGraphics guiGraphics, Pane pane, int mouseX, int mouseY) {
        List<WikiEntry> entries = selectedSection == null ? List.of() : selectedSection.entries();
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = Math.max(0.0D, entries.size() * (double) ENTRY_ROW_HEIGHT - viewportHeight);
        int rowAreaWidth = pane.width - 12 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        listScroll.clamp(maxScroll);

        float listAnim = easeOutCubic(animationProgress(sectionChangedAt, CONTENT_ANIMATION_MS));
        int rowOffsetX = 0;
        float rowAlpha = Math.max(0.15F, listAnim);

        guiGraphics.enableScissor(pane.x + 4, contentY, pane.x + pane.width - 8, contentY + viewportHeight);
        for (int index = 0; index < entries.size(); index++) {
            WikiEntry entry = entries.get(index);
            int rowY = contentY + index * ENTRY_ROW_HEIGHT - (int) Math.round(listScroll.current);
            if (rowY + ENTRY_ROW_HEIGHT < contentY || rowY > contentY + viewportHeight) {
                continue;
            }

            boolean hovered = mouseX >= pane.x + 6 && mouseX <= pane.x + rowAreaWidth && mouseY >= rowY && mouseY <= rowY + ENTRY_ROW_HEIGHT - 2;
            boolean selected = selectedEntry != null && selectedEntry.id().equals(entry.id());
            float emphasis = animateToward(entryEmphasis, entry.id(), selected ? hovered ? 1.0F : 0.82F : hovered ? 0.55F : 0.0F, 0.22F);

            int rowX = pane.x + 6 + rowOffsetX;
            int rowWidth = rowAreaWidth - 4;
            int rowHeight = ENTRY_ROW_HEIGHT - 2;
            int iconX = rowX + 4, iconY = rowY + 4;
            int textX = rowX + 26, textWidth = rowWidth - 50;
            drawListRow(guiGraphics, rowX, rowY, rowWidth, rowHeight, selected, emphasis, rowAlpha);
            renderEntryIcon(guiGraphics, entry, iconX, iconY);
            int titleY = rowY + 3;
            guiGraphics.drawString(font, trimStyled(entry.title(), textWidth), textX, titleY, withAlpha(StationScreenStyle.TEXT_PRIMARY, rowAlpha), false);
            Component detail = wikiSearching() ? Component.literal(wikiSearchContexts.getOrDefault(entry.id(), entry.badge().getString())) : entry.badge();
            guiGraphics.drawString(font, trimStyled(detail, textWidth), textX, rowY + 14, withAlpha(UiRenderer.ACCENT, rowAlpha), false);
            int pinX = rowX + rowWidth - 20;
            boolean pinHovered = hovered && mouseX >= pinX && mouseY >= contentY && mouseY < contentY + viewportHeight;
            UiRenderer.listPin(guiGraphics, font, pinX, rowY + 2, isPinned(entry.id()), pinHovered);
            if (pinHovered) hoveredPinTooltip = pinLabel(entry.id());
            else if (hovered && wikiSearching() && mouseY >= contentY && mouseY < contentY + viewportHeight)
                hoveredSearchContext = entry.title().copy().append("\n").append(detail);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, listScroll, maxScroll, viewportHeight, contentY);
    }

    private void renderEntryIcon(GuiGraphics graphics, WikiEntry entry, int x, int y) {
        boolean known = !entry.id().startsWith("schematic:") || de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.clientKnows(entry.id().substring(entry.id().lastIndexOf(':') + 1));
        de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer.drawWithShadow(graphics, entry.icon(), x, y, known);
    }

    private void renderArticle(GuiGraphics guiGraphics, Pane pane) {
        int headerX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;
        int contentX = pane.x + 10;
        int viewportY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        ArticleMetrics metrics = articleMetrics(pane);
        int contentWidth = metrics.contentWidth();
        articleContentHeight = metrics.contentHeight();

        if (selectedEntry == null) {
            guiGraphics.drawString(font, Component.translatable(wikiSearching() ? "screen.cyberneticenhancements.archive.wiki.no_results" : "screen.cyberneticenhancements.archive.no_entry"), headerX, headerY, StationScreenStyle.TEXT_SECONDARY, false);
            return;
        }

        float contentAnim = easeOutCubic(animationProgress(contentChangedAt, CONTENT_ANIMATION_MS));
        int contentOffsetX = 0;
        int alphaMask = ((int) (contentAnim * 255.0F) << 24) | 0x00FFFFFF;

        double maxScroll = metrics.maxScroll();
        articleScroll.clamp(maxScroll);

        guiGraphics.enableScissor(pane.x + 4, viewportY, pane.x + pane.width - 8, viewportY + viewportHeight);
        int y = headerY - (int) Math.round(articleScroll.current);

        StationSlotRenderer.drawStandardSlot(guiGraphics, 0, 0, headerX + contentOffsetX, y + 2, StationScreenStyle.SLOT_ACTIVE);
        renderEntryIcon(guiGraphics, selectedEntry, headerX + contentOffsetX, y + 2);
        guiGraphics.drawString(font, trimStyled(selectedEntry.title(), Math.max(1, contentWidth - 26)), headerX + 22 + contentOffsetX, y + 1, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, selectedEntry.badge(), headerX + 22 + contentOffsetX, y + 12, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        y += 26;

        if (!selectedEntry.subtitle().getString().isBlank()) {
            for (var line : font.split(selectedEntry.subtitle(), Math.min(WikiArticleLayout.MAX_PROSE_WIDTH, contentWidth - 8))) {
                guiGraphics.drawString(font, line, contentX + contentOffsetX, y, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
                y += WikiArticleLayout.LINE_HEIGHT;
            }
            y += 6;
        }

        if (!selectedEntry.chips().isEmpty()) {
            y = renderChips(guiGraphics, selectedEntry.chips(), contentX + contentOffsetX, y, contentWidth, alphaMask);
        }
        y += 6;

        for (ArticleSection section : selectedEntry.sections()) {
            drawArticleSection(guiGraphics, section, contentX + contentOffsetX, y, contentWidth, alphaMask);
            y += sectionHeight(font, section, contentWidth) + SECTION_GAP;
        }

        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
    }

    private int contactsContentWidth(Pane pane) {
        int fullWidth = ArchiveContentLayout.contentWidth(pane.width);
        int visible = Math.max(1, pane.y + pane.height - 10 - contactsContentTop(pane));
        int count = filteredContacts(ArchiveContactsClientState.get().contacts()).size();
        boolean overflowing = ArchiveContactLayout.contentHeight(count, fullWidth) > visible;
        return ArchiveContentLayout.contentWidth(pane.width, overflowing);
    }
    private int contactsContentTop(Pane pane) {
        return pane.y + 42 + contactFilterTabsHeight(contactFilters(), ArchiveContentLayout.contentWidth(pane.width)) + SECTION_GAP;
    }

    private void renderContactsRoster(GuiGraphics g, Pane pane) {
        renderContactsSearch(g, pane);
        int contentX = ArchiveContentLayout.contentX(pane.x), contentWidth = contactsContentWidth(pane);
        var allContacts = ArchiveContactsClientState.get().contacts();
        renderContactFilterTabs(g, contactFilters(), contentX, pane.y + 42, ArchiveContentLayout.contentWidth(pane.width), -1);
        int top = contactsContentTop(pane), visible = Math.max(1, pane.y + pane.height - 10 - top);
        double maxScroll = maxContactsScroll(pane);
        articleScroll.clamp(maxScroll);
        int cardTop = top - (int) Math.round(articleScroll.current);
        g.enableScissor(contentX, top, contentX + contentWidth + 3, top + visible);
        var contacts = filteredContacts(allContacts);
        if (!ArchiveContactsClientState.isLoaded() || contacts.isEmpty()) {
            String key = !ArchiveContactsClientState.isLoaded() ? "syncing" : allContacts.isEmpty() ? "empty" : "empty_filtered";
            UiRenderer.panel(g, contentX, top, contentWidth, 74, UiRenderer.ROW);
            g.drawCenteredString(font, Component.translatable("screen.cyberneticenhancements.archive.contacts." + key), contentX + contentWidth / 2, top + 32, UiRenderer.TEXT_SECONDARY);
        } else for (int index = 0; index < contacts.size(); index++) {
            var card = ArchiveContactLayout.card(index, contentWidth);
            int cardY = cardTop + card.y();
            if (cardY + card.height() > top && cardY < top + visible) {
                drawContactCard(g, contacts.get(index), contentX + card.x(), cardY, card.width(), card.height(), -1);
                var contact = contacts.get(index);
                if (contact.deceased() && !contact.hidden() && !contact.isPlayer()) seenDeceasedContacts.add(contact.npcId());
            }
        }
        g.disableScissor();
        drawScrollbar(g, pane, articleScroll, maxScroll, visible, top);
    }

    private List<String> contactTypes() {
        return ArchiveContactsClientState.get().contacts().stream().map(ArchiveContactsPayload.ContactEntry::npcTypeId)
                .distinct().sorted().toList();
    }

    private Rect contactsTypeBounds(Pane pane) {
        int labelWidth = font.width(Component.translatable("screen.cyberneticenhancements.archive.quests.all_types"));
        for (String type : contactTypes()) labelWidth = Math.max(labelWidth, font.width(contactTypeFilterLabel(type)));
        int width = labelWidth + 54;
        return new Rect("contacts_type", pane.x + pane.width - 10 - width, pane.y + 10, width, 22);
    }

    private void renderContactsSearch(GuiGraphics graphics, Pane pane) {
        if (!contactsType.isEmpty() && !contactTypes().contains(contactsType)) {
            contactsType = "";
            articleScroll.reset();
        }
        var type = contactsTypeBounds(pane);
        int x = ArchiveContentLayout.contentX(pane.x);
        int width = Math.max(80, type.x() - x - 10);
        Component hint = Component.translatable("screen.cyberneticenhancements.archive.contacts.search");
        if (contactsSearch == null) {
            contactsSearch = new de.artemis.cyberneticenhancements.client.ui.UiEditBox(font, x, pane.y + 10, width, 22, hint);
            contactsSearch.setHint(hint);
            contactsSearch.setMaxLength(80);
            contactsSearch.setResponder(value -> {
                contactsQuery = value.strip().toLowerCase(Locale.ROOT);
                articleScroll.reset();
            });
        }
        contactsSearch.setX(x);
        contactsSearch.setY(pane.y + 10);
        contactsSearch.setWidth(width);
        contactsSearch.render(graphics, currentMouseX, currentMouseY, 0);
        Component selected = contactsType.isEmpty() ? Component.translatable("screen.cyberneticenhancements.archive.quests.all_types")
                : contactTypeFilterLabel(contactsType);
        UiRenderer.button(graphics, font, type.x(), type.y(), type.width(), type.height(),
                Component.translatable("screen.cyberneticenhancements.archive.quests.type_filter", selected), true,
                !contactsType.isEmpty(), !bootSequenceBlocksInput() && isInside(currentMouseX, currentMouseY, type), -1);
    }

    private boolean handleContactsSearchClick(double mouseX, double mouseY, Pane pane) {
        if (contactsSearch != null) {
            boolean clicked = contactsSearch.mouseClicked(mouseX, mouseY, 0);
            contactsSearch.setFocused(clicked);
            if (clicked) return true;
        }
        if (!isInside(mouseX, mouseY, contactsTypeBounds(pane))) return false;
        var types = contactTypes();
        int index = contactsType.isEmpty() ? -1 : types.indexOf(contactsType);
        contactsType = index + 1 < types.size() ? types.get(index + 1) : "";
        articleScroll.reset();
        playClick();
        return true;
    }
    private void drawContactCard(GuiGraphics g, ArchiveContactsPayload.ContactEntry contact, int x, int y, int width, int height, int alphaMask) {
        UiRenderer.panel(g, x, y, width, height, withAlpha(UiRenderer.ROW, alphaMask), withAlpha(contact.deceased() ? 0xFFAA4F59 : UiRenderer.FIELD_EDGE, alphaMask));
        int padding = ArchiveContactLayout.PADDING;
        Component status = contactStateLabel(contact);
        int statusWidth = Math.min(width / 2, font.width(status));
        g.drawString(font, trimStyled(contact.displayName(), width - padding * 2 - statusWidth - ArchiveContactLayout.GAP),
                x + padding, y + padding, withAlpha(contact.deceased() ? DECEASED_COLOR : UiRenderer.TEXT, alphaMask), false);
        g.drawString(font, trimStyled(status, statusWidth), x + width - padding - statusWidth, y + padding,
                withAlpha(contactStateColor(contact), alphaMask), false);
        int headerDivider = ArchiveContactLayout.BODY_TOP - ArchiveContactLayout.GAP - 1;
        g.fill(x + padding, y + headerDivider, x + width - padding, y + headerDivider + 1, withAlpha(UiRenderer.FIELD_EDGE, alphaMask));
        int bodyY = y + ArchiveContactLayout.BODY_TOP;
        drawNpcHeadPreview(g, contact, x + padding, bodyY, ArchiveContactLayout.PORTRAIT_SIZE - 14, alphaMask);
        int textX = x + padding + ArchiveContactLayout.PORTRAIT_SIZE + ArchiveContactLayout.GAP;
        int textWidth = x + width - padding - textX;
        Component[] details = {contactTypeLabel(contact), trustLabel(contact), locationLabel(contact), contact.deceased()
                ? Component.translatable("screen.cyberneticenhancements.archive.contacts.connection_closed") : positionLabel(contact)};
        if (contact.isPlayer()) details = new Component[]{contactTypeLabel(contact),
                Component.translatable("screen.cyberneticenhancements.call.player." + contact.callStatus()),
                Component.translatable("screen.cyberneticenhancements.call.player.voice_required"),
                Component.translatable("screen.cyberneticenhancements.call.player.private_call")};
        for (int index = 0; index < details.length; index++) {
            g.drawString(font, trimStyled(details[index], textWidth), textX, bodyY + index * 16, withAlpha(UiRenderer.TEXT_SECONDARY, alphaMask), false);
        }
        g.fill(x + padding, y + ArchiveContactLayout.FOOTER_Y, x + width - padding, y + ArchiveContactLayout.FOOTER_Y + 1, withAlpha(UiRenderer.FIELD_EDGE, alphaMask));
        drawContactActionButton(g, pinButtonBounds(contact, x, y, width), Component.translatable(contact.pinned() ? "screen.cyberneticenhancements.archive.contacts.button.unpin" : "screen.cyberneticenhancements.archive.contacts.button.pin"), alphaMask, contact.pinned());
        drawContactActionButton(g, hideButtonBounds(contact, x, y, width), Component.translatable(contact.isPlayer()
                ? "screen.cyberneticenhancements.call.player." + (contact.callStatus().equals("blocked") ? "unblock" : "block_contact")
                : contact.hidden() ? "screen.cyberneticenhancements.archive.contacts.button.unhide" : "screen.cyberneticenhancements.archive.contacts.button.hide"), alphaMask, contact.isPlayer() ? contact.callStatus().equals("blocked") : contact.hidden());
        var call = callButtonBounds(contact, x, y, width);
        UiRenderer.button(g, font, call.x(), call.y(), call.width(), call.height(),
                Component.translatable(contact.deceased() ? "screen.cyberneticenhancements.archive.contacts.state.deceased" : "screen.cyberneticenhancements.archive.contacts.button.call"),
                !contact.deceased() && (!contact.isPlayer() || contact.callStatus().equals("available")), false, !bootSequenceBlocksInput() && isInside(currentMouseX, currentMouseY, call)
                        && g.containsPointInScissor(currentMouseX, currentMouseY), alphaMask);
    }
    private void drawNpcHeadPreview(GuiGraphics guiGraphics, ArchiveContactsPayload.ContactEntry contact, int x, int y, int size, int alphaMask) {
        UiRenderer.panel(guiGraphics, x, y, size + 14, size + 14, withAlpha(UiRenderer.FIELD, alphaMask), withAlpha(contact.deceased() ? 0xFFAA4F59 : UiRenderer.FIELD_EDGE, alphaMask));
        ResourceLocation texture = FixerEntityRenderer.resolveTexture(parseUuid(contact.npcId()), NpcCategory.fromId(contact.categoryId()));
        if (contact.isPlayer()) texture = de.artemis.cyberneticenhancements.client.PlayerCallClient.skin(parseUuid(contact.npcId()));
        renderNpcHeadModel(guiGraphics, texture, x + 7, y + 7, size, alphaMask, contact.deceased() ? 0xFFB08080 : 0xFFFFFFFF);
    }

    private void renderNpcHeadModel(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int renderSize, int alphaMask, int tint) {
        PlayerModel<?> model = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;
        model.head.xRot = -0.12F;
        model.head.yRot = -0.42F;
        model.head.zRot = 0.0F;
        model.hat.xRot = model.head.xRot;
        model.hat.yRot = model.head.yRot;
        model.hat.zRot = 0.0F;

        float centerX = x + renderSize * 0.5F;
        float baseY = y + renderSize - 7.0F;
        float scale = renderSize * 1.26F;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, baseY, 180.0F);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(-8.0F));
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(0.0F));
        Lighting.setupForEntityInInventory();

        VertexConsumer buffer = guiGraphics.bufferSource().getBuffer(RenderType.entityTranslucent(texture));
        model.head.render(guiGraphics.pose(), buffer, 15728880, OverlayTexture.NO_OVERLAY, withAlpha(tint, alphaMask));
        model.hat.render(guiGraphics.pose(), buffer, 15728880, OverlayTexture.NO_OVERLAY, withAlpha(tint, alphaMask));
        guiGraphics.flush();
        Lighting.setupFor3DItems();
        guiGraphics.pose().popPose();
    }

    private record ChipBox(Component text, int x, int y, int width, int height) {}
    private record ChipLayout(List<ChipBox> boxes, int height) {}

    private static ChipLayout layoutChips(Font font, List<Component> chips, int availableWidth) {
        int width = Math.max(1, availableWidth), x = 0, y = 0, rowHeight = 0;
        List<ChipBox> boxes = new ArrayList<>();
        for (Component chip : chips) {
            int chipWidth = Math.min(width, font.width(chip) + 12);
            int lines = font.split(chip, Math.max(1, chipWidth - 12)).size();
            int chipHeight = Math.max(CHIP_HEIGHT, lines * font.lineHeight + 7);
            if (x > 0 && x + chipWidth > width) {
                x = 0;
                y += rowHeight + 4;
                rowHeight = 0;
            }
            boxes.add(new ChipBox(chip, x, y, chipWidth, chipHeight));
            x += chipWidth + 4;
            rowHeight = Math.max(rowHeight, chipHeight);
        }
        return new ChipLayout(boxes, boxes.isEmpty() ? 0 : y + rowHeight);
    }

    private int renderChips(GuiGraphics guiGraphics, List<Component> chips, int startX, int startY, int width, int alphaMask) {
        ChipLayout layout = layoutChips(font, chips, width);
        for (ChipBox box : layout.boxes())
            drawMiniChip(guiGraphics, startX + box.x(), startY + box.y(), box.width(), box.height(), box.text(), alphaMask);
        return startY + layout.height();
    }

    private int measureContactsHeight(Pane pane) {
        if (!ArchiveContactsClientState.isLoaded()) return 74;
        return Math.max(74, ArchiveContactLayout.contentHeight(filteredContacts(ArchiveContactsClientState.get().contacts()).size(), contactsContentWidth(pane)));
    }






    private boolean insideRosterViewport(double mouseX, double mouseY, Pane pane) {
        return mouseX >= ArchiveContentLayout.contentX(pane.x) && mouseX < ArchiveContentLayout.contentX(pane.x) + ArchiveContentLayout.contentWidth(pane.width)
                && mouseY >= pane.y + 42 && mouseY < pane.y + pane.height - 10;
    }
    private boolean handleContactsClick(double mouseX, double mouseY, Pane pane) {
        if (!insideRosterViewport(mouseX, mouseY, pane)) return false;
        if (!ArchiveContactsClientState.isLoaded()) {
            return false;
        }

        double maxScroll = maxContactsScroll(pane);
        int contentWidth = contactsContentWidth(pane);
        int contentX = ArchiveContentLayout.contentX(pane.x);
        int y = pane.y + 42;
        List<ArchiveContactsPayload.ContactEntry> allContacts = ArchiveContactsClientState.get().contacts();
        List<ContactFilter> filters = contactFilters();

        for (Rect bounds : contactFilterTabBounds(filters, contentX, y, ArchiveContentLayout.contentWidth(pane.width))) {
            if (isInside(mouseX, mouseY, bounds)) {
                setContactsFilter(bounds.id());
                playClick();
                return true;
            }
        }

        if (mouseY < contactsContentTop(pane)) return false;
        y = contactsContentTop(pane) - (int) Math.round(articleScroll.current);
        var contacts = filteredContacts(allContacts);
        for (int index = 0; index < contacts.size(); index++) {
            var contact = contacts.get(index);
            var card = ArchiveContactLayout.card(index, contentWidth);
            int cardX = contentX + card.x(), cardY = y + card.y();
            Rect pin = pinButtonBounds(contact, cardX, cardY, card.width());
            if (isInside(mouseX, mouseY, pin)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(ArchiveContactActionPayload.ACTION_TOGGLE_PIN, contact.npcId()));
                playClick();
                return true;
            }
            Rect hide = hideButtonBounds(contact, cardX, cardY, card.width());
            if (isInside(mouseX, mouseY, hide)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(contact.isPlayer() ? ArchiveContactActionPayload.ACTION_TOGGLE_BLOCK : ArchiveContactActionPayload.ACTION_TOGGLE_HIDE, contact.npcId()));
                playClick();
                return true;
            }
            Rect call = callButtonBounds(contact, cardX, cardY, card.width());
            if (isInside(mouseX, mouseY, call)) {
                if (contact.deceased() || contact.isPlayer() && !contact.callStatus().equals("available")) return true;
                playClick();
                onClose();
                de.artemis.cyberneticenhancements.client.NpcCallOverlay.start(contact);
                return true;
            }
        }
        return false;
    }


private int renderContactFilterTabs(GuiGraphics g, List<ContactFilter> filters, int startX, int startY, int width, int alphaMask) {
        for (Rect bounds : contactFilterTabBounds(filters, startX, startY, width)) {
            drawActionButton(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), contactFilterLabel(bounds.id(), filters), alphaMask, bounds.id().equals(activeContactsFilterId));
        }
        return startY + contactFilterTabsHeight(filters, width) + SECTION_GAP;
    }

    private void drawContactActionButton(GuiGraphics guiGraphics, Rect bounds, Component label, int alphaMask) {
        drawContactActionButton(guiGraphics, bounds, label, alphaMask, false);
    }

    private void drawContactActionButton(GuiGraphics guiGraphics, Rect bounds, Component label, int alphaMask, boolean selected) {
        drawActionButton(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), label, alphaMask, selected);
    }

    private Rect contactActionBounds(ArchiveContactsPayload.ContactEntry contact, int cardX, int cardY, int cardWidth, int index, String action) {
        var bounds = ArchiveContactLayout.action(index, cardWidth);
        return new Rect(contact.npcId() + ":" + action, cardX + bounds.x(), cardY + bounds.y(), bounds.width(), bounds.height());
    }
    private Rect pinButtonBounds(ArchiveContactsPayload.ContactEntry contact, int x, int y, int width) {
        return contactActionBounds(contact, x, y, width, 0, "pin");
    }

    private Rect hideButtonBounds(ArchiveContactsPayload.ContactEntry contact, int x, int y, int width) {
        return contactActionBounds(contact, x, y, width, 1, "hide");
    }

    private Rect callButtonBounds(ArchiveContactsPayload.ContactEntry contact, int x, int y, int width) {
        return contactActionBounds(contact, x, y, width, 2, "call");
    }

    private List<ContactFilter> contactFilters() {
        return List.of(
                new ContactFilter(CONTACT_FILTER_ALL, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.all")),
                new ContactFilter(CONTACT_FILTER_PINNED, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.pinned")),
                new ContactFilter(CONTACT_FILTER_DECEASED, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.deceased")),
                new ContactFilter(CONTACT_FILTER_HIDDEN, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.hidden")));
    }

    private List<ArchiveContactsPayload.ContactEntry> filteredContacts(List<ArchiveContactsPayload.ContactEntry> contacts) {
        List<ArchiveContactsPayload.ContactEntry> filtered = new ArrayList<>();
        for (ArchiveContactsPayload.ContactEntry contact : contacts) {
            if (!matchesContactsFilter(contact) || !contactsType.isEmpty() && !contactsType.equals(contact.npcTypeId())
                    || !(contact.name() + " " + contact.npcTypeId() + " " + contactTypeLabel(contact).getString())
                            .toLowerCase(Locale.ROOT).contains(contactsQuery)) {
                continue;
            }
            filtered.add(contact);
        }
        return filtered;
    }

    private boolean matchesContactsFilter(ArchiveContactsPayload.ContactEntry contact) {
        if (CONTACT_FILTER_HIDDEN.equals(activeContactsFilterId)) {
            return contact.hidden();
        }
        if (contact.hidden()) {
            return false;
        }
        if (CONTACT_FILTER_ALL.equals(activeContactsFilterId)) {
            return true;
        }
        if (CONTACT_FILTER_PINNED.equals(activeContactsFilterId)) {
            return contact.pinned();
        }
        if (CONTACT_FILTER_DECEASED.equals(activeContactsFilterId)) return contact.deceased();
        return true;
    }

    private void setContactsFilter(String filterId) {
        if (filterId == null || filterId.equals(activeContactsFilterId)) {
            return;
        }
        activeContactsFilterId = filterId;
        articleScroll.reset();
        contentChangedAt = animationAnchorTime();
    }

    private List<Rect> contactFilterTabBounds(List<ContactFilter> filters, int startX, int startY, int availableWidth) {
        List<Rect> bounds = new ArrayList<>(filters.size());
        int x = startX;
        int y = startY;
        for (ContactFilter filter : filters) {
            int tabWidth = Math.max(84, font.width(filter.label()) + 20);
            if (x > startX && x + tabWidth > startX + availableWidth) {
                x = startX;
                y += CONTACT_FILTER_TAB_HEIGHT + CONTACT_FILTER_TAB_GAP;
            }
            bounds.add(new Rect(filter.id(), x, y, tabWidth, CONTACT_FILTER_TAB_HEIGHT));
            x += tabWidth + CONTACT_FILTER_TAB_GAP;
        }
        return bounds;
    }

    private int contactFilterTabsHeight(List<ContactFilter> filters, int availableWidth) {
        if (filters.isEmpty()) {
            return 0;
        }
        int rows = 1;
        int rowWidth = 0;
        for (ContactFilter filter : filters) {
            int tabWidth = Math.max(84, font.width(filter.label()) + 20);
            if (rowWidth > 0 && rowWidth + CONTACT_FILTER_TAB_GAP + tabWidth > availableWidth) {
                rows++;
                rowWidth = tabWidth;
            } else {
                rowWidth = rowWidth == 0 ? tabWidth : rowWidth + CONTACT_FILTER_TAB_GAP + tabWidth;
            }
        }
        return rows * CONTACT_FILTER_TAB_HEIGHT + Math.max(0, rows - 1) * CONTACT_FILTER_TAB_GAP;
    }

    private Component contactFilterLabel(String id, List<ContactFilter> filters) {
        for (ContactFilter filter : filters) {
            if (filter.id().equals(id)) {
                return filter.label();
            }
        }
        return Component.literal(id);
    }

    private Component contactTypeLabel(ArchiveContactsPayload.ContactEntry contact) {
        return Component.translatable("entity.cyberneticenhancements." + contact.npcTypeId());
    }

    private Component contactTypeFilterLabel(String npcTypeId) {
        return Component.translatable("entity.cyberneticenhancements." + npcTypeId);
    }


    private Component trustLabel(ArchiveContactsPayload.ContactEntry contact) {
        return Component.translatable(
                "screen.cyberneticenhancements.archive.contacts.trust",
                Component.translatable("screen.cyberneticenhancements.fixer.trust." + Mth.clamp(contact.trustLevel(), 0, 4))
        );
    }

    private Component locationLabel(ArchiveContactsPayload.ContactEntry contact) {
        return Component.translatable(
                "screen.cyberneticenhancements.archive.contacts.location",
                contact.hasLocation() ? formatDimension(contact.dimensionId()) : Component.translatable("screen.cyberneticenhancements.archive.contacts.location_unknown")
        );
    }

    private Component positionLabel(ArchiveContactsPayload.ContactEntry contact) {
        if (!contact.hasLocation()) {
            return Component.translatable("screen.cyberneticenhancements.archive.contacts.position", Component.translatable("screen.cyberneticenhancements.archive.contacts.position_unknown"));
        }
        return Component.translatable("screen.cyberneticenhancements.archive.contacts.position", contact.x() + ", " + contact.y() + ", " + contact.z());
    }

    private Component contactStateLabel(ArchiveContactsPayload.ContactEntry contact) {
        if (contact.isPlayer()) return Component.translatable("screen.cyberneticenhancements.call.player." + contact.callStatus());
        String stateKey = contact.deceased() ? "screen.cyberneticenhancements.archive.contacts.state.deceased" : contact.refusing()
                ? "screen.cyberneticenhancements.archive.contacts.state.refusing"
                : contact.surchargeActive()
                ? "screen.cyberneticenhancements.archive.contacts.state.hiking"
                : "screen.cyberneticenhancements.archive.contacts.state.available";
        return Component.translatable(
                "screen.cyberneticenhancements.archive.contacts.state",
                Component.translatable(stateKey)
        );
    }

    private int contactStateColor(ArchiveContactsPayload.ContactEntry contact) {
        if (contact.isPlayer()) return contact.callStatus().equals("available") ? StationScreenStyle.ACCENT : StationScreenStyle.WARNING;
        if (contact.deceased()) return DECEASED_COLOR;
        if (contact.refusing()) {
            return StationScreenStyle.WARNING;
        }
        if (contact.surchargeActive()) {
            return StationScreenStyle.WARNING;
        }
        return StationScreenStyle.ACCENT;
    }

    private String accessSummary(ArchiveContactsPayload.ContactEntry contact) {
        if (!"fixer".equals(contact.npcTypeId())) {
            return humanizeId(contact.npcTypeId());
        }
        if (contact.trustLevel() >= 3) {
            return Component.translatable("screen.cyberneticenhancements.archive.contacts.access.fixer.3").getString();
        }
        if (contact.trustLevel() >= 2) {
            return Component.translatable("screen.cyberneticenhancements.archive.contacts.access.fixer.2").getString();
        }
        if (contact.trustLevel() >= 1) {
            return Component.translatable("screen.cyberneticenhancements.archive.contacts.access.fixer.1").getString();
        }
        return Component.translatable("screen.cyberneticenhancements.archive.contacts.access.fixer.0").getString();
    }

    private Component formatDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) {
            return Component.translatable("screen.cyberneticenhancements.archive.contacts.location_unknown");
        }
        return switch (dimensionId) {
            case "minecraft:overworld" ->
                    Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.overworld");
            case "minecraft:the_nether" ->
                    Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.nether");
            case "minecraft:the_end" ->
                    Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.end");
            default ->
                    Component.literal(humanizeId(dimensionId.contains(":") ? dimensionId.substring(dimensionId.indexOf(':') + 1) : dimensionId));
        };
    }

    private String humanizeId(String raw) {
        String normalized = raw.replace(':', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return raw;
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    private void drawArticleSection(GuiGraphics guiGraphics, ArticleSection section, int x, int y, int width, int alphaMask) {
        int height = sectionHeight(font, section, width);
        drawArchiveCard(guiGraphics, x, y, width, height, alphaMask);
        int headingHeight = sectionHeadingHeight(section, width);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + headingHeight - 5, withAlpha(UiRenderer.FIELD, alphaMask));
        guiGraphics.fill(x + 1, y + 8, x + 3, y + headingHeight - 10, withAlpha(UiRenderer.ACCENT, alphaMask));
        int titleY = y + 9;
        for (var line : font.split(section.title(), Math.max(1, width - 24))) {
            guiGraphics.drawString(font, line, x + 12, titleY, withAlpha(UiRenderer.TEXT, alphaMask), false);
            titleY += WikiArticleLayout.LINE_HEIGHT;
        }
        int lineY = y + headingHeight;
        if (section.visual() != null) {
            renderSectionVisual(guiGraphics, section.visual(), x + 10, lineY, width - 20, alphaMask);
            lineY += visualHeight(section.visual(), width - 20);
            if (!section.lines().isEmpty()) {
                lineY += 6;
            }
        }
        drawArticleFlow(guiGraphics, articleTextFlow(section.lines(), width - 20), x + 10, lineY, width - 20, alphaMask);
    }

    private int sectionHeadingHeight(ArticleSection section, int width) {
        return font.split(section.title(), Math.max(1, width - 24)).size() * WikiArticleLayout.LINE_HEIGHT + 16;
    }

    private WikiArticleLayout.Flow articleTextFlow(List<Component> paragraphs, int width) {
        return articleFlow(paragraphs.stream().map(WikiArticleLayout::describe).toList(), width);
    }

    private WikiArticleLayout.Flow articleFlow(List<WikiArticleLayout.Block> blocks, int width) {
        return WikiArticleLayout.layout(blocks, width, font::width, (text, wrapWidth) -> font.split(text, wrapWidth).size());
    }

    private void drawArticleFlow(GuiGraphics graphics, WikiArticleLayout.Flow flow, int x, int y, int width, int alphaMask) {
        for (var row : flow.rows()) {
            var block = row.block();
            int textY = y + row.y();
            int valueX = x;
            if (block.fact()) {
                graphics.fill(x, textY, x + width, textY + row.height(), withAlpha(0x70111820, alphaMask));
                drawArticleLines(graphics, block.label(), x + 6, textY + 4, row.labelWidth(), UiRenderer.TEXT_SECONDARY, alphaMask);
                valueX += 6 + row.labelWidth() + WikiArticleLayout.COLUMN_GAP;
                textY += 4;
            }
            drawArticleLines(graphics, block.value(), valueX, textY, row.valueWidth(),
                    block.fact() ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, alphaMask);
        }
    }

    private void drawArticleLines(GuiGraphics graphics, Component text, int x, int y, int width, int color, int alphaMask) {
        for (var line : font.split(text, Math.max(1, width))) {
            graphics.drawString(font, line, x, y, withAlpha(color, alphaMask), false);
            y += WikiArticleLayout.LINE_HEIGHT;
        }
    }

    private int referenceTableHeight(ReferenceTableVisual visual, int width) {
        if (referenceColumns(visual, width)) {
            int columnWidth = (width - 14) / 2;
            return visual.groups().stream().mapToInt(group -> referenceGroupHeight(group, columnWidth)).max().orElse(0);
        }
        int height = 0;
        for (var group : visual.groups()) {
            height += referenceGroupHeight(group, width) + 10;
        }
        return Math.max(0, height - 10);
    }

    private boolean referenceColumns(ReferenceTableVisual visual, int width) {
        return visual.groups().size() == 2 && width >= 380;
    }

    private int referenceGroupHeight(CyberwareWikiData.ReferenceGroup group, int width) {
        int height = font.split(group.title(), Math.max(1, width)).size() * WikiArticleLayout.LINE_HEIGHT + 6
                + articleFlow(group.rows(), width).height();
        if (!group.notes().isEmpty()) height += 6 + articleTextFlow(group.notes(), width).height();
        return height;
    }

    private void renderReferenceTable(GuiGraphics graphics, ReferenceTableVisual visual, int x, int y, int width, int alphaMask) {
        boolean columns = referenceColumns(visual, width);
        int groupWidth = columns ? (width - 14) / 2 : width;
        int groupX = x, groupY = y;
        for (var group : visual.groups()) {
            int contentY = groupY;
            drawArticleLines(graphics, group.title(), groupX, contentY, groupWidth, UiRenderer.ACCENT, alphaMask);
            contentY += font.split(group.title(), Math.max(1, groupWidth)).size() * WikiArticleLayout.LINE_HEIGHT + 6;
            var flow = articleFlow(group.rows(), groupWidth);
            drawArticleFlow(graphics, flow, groupX, contentY, groupWidth, alphaMask);
            contentY += flow.height();
            if (!group.notes().isEmpty()) {
                contentY += 6;
                var notes = articleTextFlow(group.notes(), groupWidth);
                drawArticleFlow(graphics, notes, groupX, contentY, groupWidth, alphaMask);
            }
            if (columns) groupX += groupWidth + 14;
            else groupY += referenceGroupHeight(group, groupWidth) + 10;
        }
    }

    private int measureArticleHeight(Font font, WikiEntry entry, int width) {
        int height = 26;
        if (!entry.subtitle().getString().isBlank()) {
            height += font.split(entry.subtitle(), Math.min(WikiArticleLayout.MAX_PROSE_WIDTH, width - 8)).size() * WikiArticleLayout.LINE_HEIGHT + 6;
        }
        if (!entry.chips().isEmpty()) height += layoutChips(font, entry.chips(), width).height();
        height += 6;
        for (ArticleSection section : entry.sections()) {
            height += sectionHeight(font, section, width) + SECTION_GAP;
        }
        return height;
    }

    private int sectionHeight(Font font, ArticleSection section, int width) {
        int height = sectionHeadingHeight(section, width);
        if (section.visual() != null) {
            height += visualHeight(section.visual(), width - 20);
            if (!section.lines().isEmpty()) {
                height += 6;
            }
        }
        height += articleTextFlow(section.lines(), width - 20).height();
        return height + 10;
    }

    private void renderSectionVisual(GuiGraphics guiGraphics, SectionVisual visual, int x, int y, int width, int alphaMask) {
        switch (visual) {
            case ReferenceTableVisual table -> renderReferenceTable(guiGraphics, table, x, y, width, alphaMask);
            case CraftingGridVisual craftingGridVisual ->
                    renderCraftingVisual(guiGraphics, craftingGridVisual, x, y, width);
            case CompactTransformVisual compactTransformVisual ->
                    renderCompactTransformVisual(guiGraphics, compactTransformVisual, x, y, width, alphaMask);
            case ServiceOperationsVisual serviceOperationsVisual ->
                    renderServiceVisual(guiGraphics, serviceOperationsVisual, x, y, width, alphaMask);
            case RelicCacheExampleVisual relicCacheExampleVisual ->
                    renderRelicCacheExampleVisual(guiGraphics, relicCacheExampleVisual, x, y, width, alphaMask);
            case StructureViewVisual structure -> renderStructureView(guiGraphics, structure, x, y, width, alphaMask);
        }
    }

    private int visualHeight(SectionVisual visual, int width) {
        return switch (visual) {
            case ReferenceTableVisual table -> referenceTableHeight(table, width);
            case CraftingGridVisual grid -> grid.unlocked()
                    ? slotSpan(grid.grids().getFirst().length == 25 ? 5 : 3) + VISUAL_CONTENT_PADDING * 2
                    : font.split(lockedRecipeMessage(), Math.max(1, width - VISUAL_CONTENT_PADDING * 2)).size() * 11 + VISUAL_CONTENT_PADDING * 2;
            case CompactTransformVisual compactTransformVisual ->
                    compactTransformVisualHeight(Math.max(1, (compactTransformVisual.outputs().size() + 2) / 3));
            case ServiceOperationsVisual serviceOperationsVisual -> serviceVisualHeight(serviceOperationsVisual, width);
            case RelicCacheExampleVisual ignored -> relicCacheExampleVisualHeight();
            case StructureViewVisual ignored -> STRUCTURE_VIEW_HEIGHT;
        };
    }

    private void renderCraftingVisual(GuiGraphics guiGraphics, CraftingGridVisual visual, int x, int y, int width) {
        if (!visual.unlocked()) {
            int lineY = y + VISUAL_CONTENT_PADDING;
            for (var line : font.split(lockedRecipeMessage(), Math.max(1, width - VISUAL_CONTENT_PADDING * 2))) {
                guiGraphics.drawString(font, line, x + VISUAL_CONTENT_PADDING, lineY, StationScreenStyle.TEXT_SECONDARY, false);
                lineY += 11;
            }
            return;
        }
        ItemStack[] grid = visual.grids().getFirst();
        int columns = grid.length == 25 ? 5 : 3;
        int contentSize = slotSpan(columns);
        int resultOffset = contentSize + 40;
        int diagramWidth = resultOffset + StationSlotRenderer.SLOT_FRAME_SIZE;
        int startX = x + Math.max(VISUAL_CONTENT_PADDING, (width - diagramWidth) / 2);
        int startY = y + VISUAL_CONTENT_PADDING;
        drawRecipeSurface(guiGraphics, startX - 3, startY - 3, diagramWidth + 6, contentSize + 6);
        for (int index = 0; index < grid.length; index++) {
            int slotX = startX + (index % columns) * VISUAL_SLOT_STRIDE;
            int slotY = startY + (index / columns) * VISUAL_SLOT_STRIDE;
            drawVisualSlot(guiGraphics, grid[index], slotX, slotY, StationScreenStyle.SLOT_BG);
        }
        int resultY = startY + (contentSize - StationSlotRenderer.SLOT_FRAME_SIZE) / 2;
        drawArrow(guiGraphics, startX + contentSize + 8, resultY + 8, startX + resultOffset - 8);
        drawVisualSlot(guiGraphics, visual.output(), startX + resultOffset, resultY, StationScreenStyle.SLOT_ACTIVE);
    }

    private Component lockedRecipeMessage() {
        return Component.translatable("screen.cyberneticenhancements.schematic.recipe_locked");
    }

    private void renderCompactTransformVisual(GuiGraphics guiGraphics, CompactTransformVisual visual, int x, int y, int width, int alphaMask) {
        int outputColumns = Math.min(3, Math.max(1, visual.outputs().size()));
        int outputRows = Math.max(1, (visual.outputs().size() + 2) / 3);
        int contentWidth = TRANSFORM_OUTPUT_START + slotSpan(outputColumns);
        int contentHeight = slotSpan(outputRows);
        int visualHeight = compactTransformVisualHeight(outputRows);
        int startX = x + Math.max(VISUAL_CONTENT_PADDING, (width - contentWidth) / 2);
        int startY = y + Math.max(VISUAL_CONTENT_PADDING, (visualHeight - contentHeight) / 2);
        drawRecipeSurface(guiGraphics, startX - 3, startY - 3, contentWidth + 6, contentHeight + 6);
        int inputY = startY + Math.max(0, (contentHeight - StationSlotRenderer.SLOT_FRAME_SIZE) / 2);
        drawVisualSlot(guiGraphics, visual.input(), startX, inputY, StationScreenStyle.SLOT_BG);
        int outputStartX = startX + TRANSFORM_OUTPUT_START;
        drawArrow(guiGraphics, startX + 22, inputY + 8, outputStartX - 8);
        for (int index = 0; index < visual.outputs().size(); index++) {
            int cursorX = outputStartX + index % outputColumns * VISUAL_SLOT_STRIDE;
            int cursorY = startY + index / outputColumns * VISUAL_SLOT_STRIDE;
            drawVisualSlot(guiGraphics, visual.outputs().get(index), cursorX, cursorY, StationScreenStyle.SLOT_ACTIVE);
        }
    }

    private void renderServiceVisual(GuiGraphics guiGraphics, ServiceOperationsVisual visual, int x, int y, int width, int alphaMask) {
        int rowY = y;
        if (!visual.summaryChips().isEmpty()) {
            rowY = renderServiceSummaryChips(guiGraphics, visual.summaryChips(), x, rowY, width, alphaMask) + 8;
        }

        for (ServiceOperationVisual operation : visual.operations()) {
            int operationHeight = serviceOperationCardHeight(operation, width);
            renderServiceOperationCard(guiGraphics, operation, x, rowY, width, operationHeight, alphaMask);
            rowY += operationHeight;
            if (operation != visual.operations().getLast() || !visual.upgradeStages().isEmpty()) {
                rowY += 8;
            }
        }

        if (!visual.upgradeStages().isEmpty()) {
            renderUpgradeStages(guiGraphics, visual.upgradeStages(), x, rowY, width, alphaMask);
        }
    }

    private static final int STRUCTURE_VIEW_HEIGHT = 280;
    private int[] structureViewArea = new int[0];

    private void renderStructureView(GuiGraphics guiGraphics, StructureViewVisual visual, int x, int y, int width, int alphaMask) {
        drawRecipeSurface(guiGraphics, x, y, width, STRUCTURE_VIEW_HEIGHT);
        structureViewArea = new int[] {x, y, x + width, y + STRUCTURE_VIEW_HEIGHT};
        guiGraphics.enableScissor(x + 1, y + 1, x + width - 1, y + STRUCTURE_VIEW_HEIGHT - 1);
        StructureView.render(guiGraphics, visual.template(), x, y, width, STRUCTURE_VIEW_HEIGHT);
        guiGraphics.disableScissor();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 600); // in front of the model's depth
        guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.view.drag_to_rotate"), x + 8, y + STRUCTURE_VIEW_HEIGHT - 14,
                StationScreenStyle.TEXT_SECONDARY, false);
        guiGraphics.pose().popPose();
    }

    private void renderRelicCacheExampleVisual(GuiGraphics guiGraphics, RelicCacheExampleVisual visual, int x, int y, int width, int alphaMask) {
        int visualHeight = relicCacheExampleVisualHeight();
        int innerX = x + VISUAL_CONTENT_PADDING;
        int innerY = y + VISUAL_CONTENT_PADDING;
        int gridSpan = RELIC_EXAMPLE_CELL_SIZE * visual.gridSize() + RELIC_EXAMPLE_CELL_GAP * Math.max(0, visual.gridSize() - 1);
        int gridX = innerX + 4;
        int gridY = innerY + 18;
        int sideX = gridX + gridSpan + 16;
        int sideWidth = Math.max(120, width - (sideX - x) - VISUAL_CONTENT_PADDING);

        guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.station.relic_cache.example.grid_label"), gridX, innerY + 2, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.station.relic_cache.example.daemon_label"), sideX, innerY + 2, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);

        for (int step = 0; step < visual.pathCells().length - 1; step++) {
            int fromCell = visual.pathCells()[step];
            int toCell = visual.pathCells()[step + 1];
            int fromCenterX = gridX + cellColumn(fromCell, visual.gridSize()) * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP) + RELIC_EXAMPLE_CELL_SIZE / 2;
            int fromCenterY = gridY + cellRow(fromCell, visual.gridSize()) * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP) + RELIC_EXAMPLE_CELL_SIZE / 2;
            int toCenterX = gridX + cellColumn(toCell, visual.gridSize()) * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP) + RELIC_EXAMPLE_CELL_SIZE / 2;
            int toCenterY = gridY + cellRow(toCell, visual.gridSize()) * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP) + RELIC_EXAMPLE_CELL_SIZE / 2;
            if (fromCenterX == toCenterX) {
                int minY = Math.min(fromCenterY, toCenterY);
                int maxY = Math.max(fromCenterY, toCenterY);
                guiGraphics.fill(fromCenterX - 1, minY, fromCenterX - 1 + RELIC_EXAMPLE_ROUTE_THICKNESS, maxY + 1, withAlpha(StationScreenStyle.ACCENT, alphaMask));
            } else {
                int minX = Math.min(fromCenterX, toCenterX);
                int maxX = Math.max(fromCenterX, toCenterX);
                guiGraphics.fill(minX, fromCenterY - 1, maxX + 1, fromCenterY - 1 + RELIC_EXAMPLE_ROUTE_THICKNESS, withAlpha(StationScreenStyle.ACCENT, alphaMask));
            }
        }

        for (int row = 0; row < visual.gridSize(); row++) {
            for (int column = 0; column < visual.gridSize(); column++) {
                int cellIndex = row * visual.gridSize() + column;
                int cellX = gridX + column * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP);
                int cellY = gridY + row * (RELIC_EXAMPLE_CELL_SIZE + RELIC_EXAMPLE_CELL_GAP);
                boolean onPath = pathIndex(visual.pathCells(), cellIndex) >= 0;
                int fill = onPath ? withAlpha(StationScreenStyle.SLOT_ACTIVE, alphaMask) : withAlpha(StationScreenStyle.SLOT_BG, alphaMask);
                guiGraphics.fill(cellX, cellY, cellX + RELIC_EXAMPLE_CELL_SIZE, cellY + RELIC_EXAMPLE_CELL_SIZE, fill);
                guiGraphics.fill(cellX, cellY, cellX + RELIC_EXAMPLE_CELL_SIZE, cellY + 1, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
                guiGraphics.fill(cellX, cellY, cellX + 1, cellY + RELIC_EXAMPLE_CELL_SIZE, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
                guiGraphics.fill(cellX, cellY + RELIC_EXAMPLE_CELL_SIZE - 1, cellX + RELIC_EXAMPLE_CELL_SIZE, cellY + RELIC_EXAMPLE_CELL_SIZE, withAlpha(StationScreenStyle.FRAME_SHADOW_SOFT, alphaMask));
                guiGraphics.fill(cellX + RELIC_EXAMPLE_CELL_SIZE - 1, cellY, cellX + RELIC_EXAMPLE_CELL_SIZE, cellY + RELIC_EXAMPLE_CELL_SIZE, withAlpha(StationScreenStyle.FRAME_SHADOW_SOFT, alphaMask));

                String token = tokenLabel(visual.gridTokens()[cellIndex]);
                guiGraphics.drawString(font, token, cellX + (RELIC_EXAMPLE_CELL_SIZE - font.width(token)) / 2, cellY + 5, withAlpha(onPath ? StationScreenStyle.TEXT_PRIMARY : StationScreenStyle.TEXT_SECONDARY, alphaMask), false);

                int order = pathIndex(visual.pathCells(), cellIndex);
                if (order >= 0) {
                    String stepLabel = Integer.toString(order + 1);
                    guiGraphics.drawString(font, stepLabel, cellX + 2, cellY + 1, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
                }
            }
        }

        int daemonY = gridY;
        for (int daemonIndex = 0; daemonIndex < visual.daemonTokens().length; daemonIndex++) {
            int[] daemon = visual.daemonTokens()[daemonIndex];
            int cardHeight = 24;
            guiGraphics.fill(sideX, daemonY, sideX + sideWidth, daemonY + cardHeight, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
            guiGraphics.fill(sideX, daemonY, sideX + sideWidth, daemonY + 1, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.relic_cache_hack.daemon", daemonIndex + 1), sideX + 4, daemonY + 4, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            int tokenBox = Math.max(14, Math.min(18, (sideWidth - 12 - (daemon.length - 1) * 2) / daemon.length));
            int rowWidth = daemon.length * tokenBox + Math.max(0, daemon.length - 1) * 2;
            int tokenX = sideX + 4 + Math.max(0, (sideWidth - 8 - rowWidth) / 2);
            for (int tokenIndex = 0; tokenIndex < daemon.length; tokenIndex++) {
                int boxX = tokenX + tokenIndex * (tokenBox + 2);
                guiGraphics.fill(boxX, daemonY + 12, boxX + tokenBox, daemonY + 22, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
                String token = tokenLabel(daemon[tokenIndex]);
                guiGraphics.drawString(font, token, boxX + (tokenBox - font.width(token)) / 2, daemonY + 13, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
            }
            daemonY += cardHeight + 6;
        }
    }

    private int serviceVisualHeight(ServiceOperationsVisual visual, int width) {
        int height = 0;
        if (!visual.summaryChips().isEmpty()) {
            height += measureServiceSummaryChipHeight(visual.summaryChips(), width) + 8;
        }
        for (int index = 0; index < visual.operations().size(); index++) {
            height += serviceOperationCardHeight(visual.operations().get(index), width);
            if (index < visual.operations().size() - 1 || !visual.upgradeStages().isEmpty()) {
                height += 8;
            }
        }
        if (!visual.upgradeStages().isEmpty()) {
            height += measureUpgradeStagesHeight(visual.upgradeStages(), width);
        }
        return height;
    }

    private int renderServiceSummaryChips(GuiGraphics guiGraphics, List<Component> chips, int startX, int startY, int width, int alphaMask) {
        return renderChips(guiGraphics, chips, startX, startY, width, alphaMask);
    }

    private int measureServiceSummaryChipHeight(List<Component> chips, int width) {
        return layoutChips(font, chips, width).height();
    }

    private void renderServiceOperationCard(GuiGraphics guiGraphics, ServiceOperationVisual operation, int x, int y, int width, int height, int alphaMask) {
        guiGraphics.drawString(font, operation.label(), x, y, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);

        int slotsY = y + 16;
        int inputX = x + Math.max(0, (width - serviceSlotRowWidth(operation.materials().size())) / 2);
        int materialsX = inputX + SERVICE_MATERIALS_START;
        drawRecipeSurface(guiGraphics, inputX - 3, slotsY - 3,
                serviceSlotRowWidth(operation.materials().size()) + 6, StationSlotRenderer.SLOT_FRAME_SIZE + 6);
        drawVisualSlot(guiGraphics, operation.input(), inputX, slotsY, StationScreenStyle.SLOT_BG);
        for (int index = 0; index < operation.materials().size(); index++) {
            drawVisualSlot(guiGraphics, operation.materials().get(index), materialsX + index * VISUAL_SLOT_STRIDE, slotsY, StationScreenStyle.SLOT_BG);
        }

        int outputX = inputX + SERVICE_MATERIALS_START + Math.max(1, operation.materials().size()) * VISUAL_SLOT_STRIDE + SERVICE_OUTPUT_GAP;
        guiGraphics.drawString(font, "+", inputX + 23, slotsY + 4,
                withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        drawArrow(guiGraphics, materialsX + Math.max(1, operation.materials().size()) * VISUAL_SLOT_STRIDE, slotsY + 8, outputX - 6);
        drawVisualSlot(guiGraphics, operation.output(), outputX, slotsY, StationScreenStyle.SLOT_ACTIVE);
        int lineY = slotsY + StationSlotRenderer.SLOT_FRAME_SIZE + 6;
        drawArticleFlow(guiGraphics, articleTextFlow(List.of(operation.note()), width), x, lineY, width, alphaMask);
    }

    private int serviceOperationCardHeight(ServiceOperationVisual operation, int width) {
        return 16 + StationSlotRenderer.SLOT_FRAME_SIZE + 6
                + articleTextFlow(List.of(operation.note()), width).height() + 4;
    }

    private int renderUpgradeStages(GuiGraphics guiGraphics, List<UpgradeStageVisual> stages, int x, int y, int width, int alphaMask) {
        guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.service.upgrade_stages"), x, y, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.fill(x, y + 12, x + width, y + 13, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        int stageY = y + 18;
        for (UpgradeStageVisual stage : stages) {
            int stageHeight = stageHeight(stage, width);
            guiGraphics.fill(x, stageY + stageHeight - 1, x + width, stageY + stageHeight,
                    withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
            int chipWidth = Math.max(34, font.width(stage.label()) + 12);
            drawMiniChip(guiGraphics, x, stageY, chipWidth, CHIP_HEIGHT, stage.label(), alphaMask);
            if (!stage.materials().isEmpty()) {
                int materialX = x + chipWidth + 10;
                for (int index = 0; index < stage.materials().size(); index++) {
                    drawVisualSlot(guiGraphics, stage.materials().get(index), materialX + index * VISUAL_SLOT_STRIDE, stageY, StationScreenStyle.SLOT_BG);
                }
            }
            int lineY = stageY + Math.max(CHIP_HEIGHT, StationSlotRenderer.SLOT_FRAME_SIZE) + 6;
            drawArticleFlow(guiGraphics, articleTextFlow(stage.changes(), width), x, lineY, width, alphaMask);
            stageY += stageHeight;
            if (stage != stages.getLast()) {
                stageY += 4;
            }
        }
        return stageY;
    }

    private int measureUpgradeStagesHeight(List<UpgradeStageVisual> stages, int width) {
        int total = 18;
        for (int index = 0; index < stages.size(); index++) {
            total += stageHeight(stages.get(index), width);
            if (index < stages.size() - 1) {
                total += 4;
            }
        }
        return total;
    }

    private int stageHeight(UpgradeStageVisual stage, int width) {
        int height = Math.max(CHIP_HEIGHT, StationSlotRenderer.SLOT_FRAME_SIZE) + 6;
        height += articleTextFlow(stage.changes(), width).height();
        return height + 4;
    }

    private int compactTransformVisualHeight(int outputRows) {
        return slotSpan(outputRows) + VISUAL_CONTENT_PADDING * 2;
    }

    private int relicCacheExampleVisualHeight() {
        return 144;
    }

    private int slotSpan(int count) {
        return StationSlotRenderer.SLOT_FRAME_SIZE + VISUAL_SLOT_STRIDE * Math.max(0, count - 1);
    }

    private int serviceSlotRowWidth(int materialCount) {
        return SERVICE_MATERIALS_START + Math.max(1, materialCount) * VISUAL_SLOT_STRIDE + SERVICE_OUTPUT_GAP + StationSlotRenderer.SLOT_FRAME_SIZE;
    }

    private void drawRecipeSurface(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, UiRenderer.FIELD);
        UiRenderer.outline(graphics, x, y, width, height, UiRenderer.PANEL_EDGE);
    }

    private void drawVisualSlot(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int fill) {
        StationSlotRenderer.drawStandardSlot(guiGraphics, 0, 0, x, y, fill);
        ItemStack safeStack = stack == null ? ItemStack.EMPTY : stack;
        if (!safeStack.isEmpty()) {
            guiGraphics.renderItem(safeStack, x, y);
            guiGraphics.renderItemDecorations(font, safeStack, x, y);
            if (currentMouseX >= x && currentMouseX < x + 16 && currentMouseY >= y && currentMouseY < y + 16) {
                hoveredVisualStack = safeStack;
            }
        }
    }

    private void drawArrow(GuiGraphics guiGraphics, int startX, int centerY, int endX) {
        if (endX <= startX) {
            return;
        }
        String arrow = "→";
        guiGraphics.drawString(font, arrow, (startX + endX - font.width(arrow)) / 2,
                centerY - font.lineHeight / 2, UiRenderer.TEXT_SECONDARY, false);
    }

    private int pathIndex(int[] pathCells, int cellIndex) {
        for (int index = 0; index < pathCells.length; index++) {
            if (pathCells[index] == cellIndex) {
                return index;
            }
        }
        return -1;
    }

    private int cellRow(int cellIndex, int gridSize) {
        return cellIndex / gridSize;
    }

    private int cellColumn(int cellIndex, int gridSize) {
        return cellIndex % gridSize;
    }

    private String tokenLabel(int tokenId) {
        String[] labels = de.artemis.cyberneticenhancements.common.reliccache.RelicCacheHackPuzzle.TOKEN_LABELS;
        if (tokenId < 0 || tokenId >= labels.length) {
            return "--";
        }
        return labels[tokenId];
    }

    private boolean handleNavigationClick(double mouseX, double mouseY, Pane pane) {
        NavLayout layout = buildNavLayout();
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        if (mouseY < contentY || mouseY >= contentY + viewportHeight) {
            return false;
        }
        int rowAreaWidth = pane.width - 12 - (Math.max(0.0D, layout.totalHeight() - viewportHeight) > 0.0D ? SCROLLBAR_WIDTH : 0);
        for (int index = layout.rows().size() - 1; index >= 0; index--) {
            NavRow row = layout.rows().get(index);
            if (row.alpha() < 0.20F) {
                continue;
            }
            int rowY = contentY + Math.round(row.y()) - (int) Math.round(navScroll.current);
            if (rowY + ROW_HEIGHT < contentY || rowY > contentY + viewportHeight) {
                continue;
            }
            int rowX = pane.x + 6 + row.depth() * NAV_CHILD_INDENT;
            if (mouseX >= rowX && mouseX < pane.x + 6 + rowAreaWidth - 4 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2) {
                playClick();
                if (row.topic().hasChildren()) {
                    if (!expandedTopics.add(row.topic().id())) {
                        expandedTopics.remove(row.topic().id());
                    }
                    navScroll.clamp(maxNavScroll());
                } else if (row.topic().section() != null) {
                    setSelectedSection(row.topic().section());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleEntryClick(double mouseX, double mouseY, Pane pane) {
        if (selectedSection == null) {
            return false;
        }
        List<WikiEntry> entries = selectedSection.entries();
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        if (mouseY < contentY || mouseY >= contentY + viewportHeight) return false;
        int rowAreaWidth = pane.width - 12 - (Math.max(0.0D, entries.size() * (double) ENTRY_ROW_HEIGHT - viewportHeight) > 0.0D ? SCROLLBAR_WIDTH : 0);
        for (int index = 0; index < entries.size(); index++) {
            int rowY = contentY + index * ENTRY_ROW_HEIGHT - (int) Math.round(listScroll.current);
            if (rowY + ENTRY_ROW_HEIGHT < contentY || rowY > contentY + viewportHeight) {
                continue;
            }
            if (mouseX >= pane.x + 6 && mouseX <= pane.x + 6 + rowAreaWidth - 4 && mouseY >= rowY && mouseY <= rowY + ENTRY_ROW_HEIGHT - 2) {
                if (mouseX >= pane.x + 6 + rowAreaWidth - 4 - 20) {
                    togglePinned(entries.get(index).id());
                    playClick();
                    return true;
                }
                setSelectedEntry(entries.get(index));
                playClick();
                return true;
            }
        }
        return false;
    }

    private de.artemis.cyberneticenhancements.client.ui.UiScrollbar.Track scrollTrack(Pane pane, double value, double max, int viewport, int top) {
        return de.artemis.cyberneticenhancements.client.ui.UiScrollbar.layout(isContactsTab() ? ArchiveContentLayout.scrollbarX(pane.x, pane.width) : pane.x + pane.width - SCROLLBAR_WIDTH - 6, top, SCROLLBAR_WIDTH, viewport, false, viewport, max, value);
    }

    private DragTarget scrollTarget(ScrollState state) {
        return state == navScroll ? DragTarget.NAV : state == listScroll ? DragTarget.LIST : DragTarget.ARTICLE;
    }

    private de.artemis.cyberneticenhancements.client.ui.UiScrollbar scrollbar(DragTarget target) {
        return scrollbars.computeIfAbsent(target, ignored -> new de.artemis.cyberneticenhancements.client.ui.UiScrollbar());
    }

    private boolean tryStartScrollbarDrag(double mouseX, double mouseY, Pane pane, ScrollState scroll, double maxScroll, DragTarget target) {
        int top = isContactsTab() && scroll == articleScroll ? contactsContentTop(pane) : pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewport = isContactsTab() && scroll == articleScroll ? Math.max(1, pane.y + pane.height - 10 - top) : pane.height - PANEL_HEADER_HEIGHT - 12;
        var track = scrollTrack(pane, scroll.current, maxScroll, viewport, top);
        var bar = scrollbar(target);
        if (!bar.begin(track, mouseX, mouseY)) return false;
        dragTarget = target;
        scroll.set(bar.value(track, mouseX, mouseY), maxScroll);
        return true;
    }

    private void dragScrollbar(ScrollState scroll, Pane pane, double mouseY, double maxScroll) {
        int top = isContactsTab() && scroll == articleScroll ? contactsContentTop(pane) : pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewport = isContactsTab() && scroll == articleScroll ? Math.max(1, pane.y + pane.height - 10 - top) : pane.height - PANEL_HEADER_HEIGHT - 12;
        var track = scrollTrack(pane, scroll.current, maxScroll, viewport, top);
        scroll.set(scrollbar(scrollTarget(scroll)).value(track, currentMouseX, mouseY), maxScroll);
    }

    private void drawScrollbar(GuiGraphics graphics, Pane pane, ScrollState scroll, double maxScroll, int viewportHeight, int contentY) {
        var target = scrollTarget(scroll);
        de.artemis.cyberneticenhancements.client.ui.UiScrollbar.draw(graphics, scrollTrack(pane, scroll.current, maxScroll, viewportHeight, contentY), currentMouseX, currentMouseY, dragTarget == target);
    }

    private NavLayout buildNavLayout() {
        List<NavRow> rows = new ArrayList<>();
        float y = 0.0F;
        for (WikiTopic topic : displayTopics()) {
            y += appendTopic(rows, topic, 0, y, 1.0F);
        }
        return new NavLayout(rows, y);
    }

    private float appendTopic(List<NavRow> rows, WikiTopic topic, int depth, float y, float alpha) {
        rows.add(new NavRow(topic, depth, y, alpha));
        float consumed = ROW_HEIGHT;
        if (!topic.hasChildren()) {
            return consumed;
        }

        float expansion = topicExpansion.getOrDefault(topic.id(), expandedTopics.contains(topic.id()) ? 1.0F : 0.0F);
        if (expansion <= 0.001F) {
            return consumed;
        }

        float childBaseY = y + ROW_HEIGHT;
        float childConsumed = 0.0F;
        for (WikiTopic child : topic.children()) {
            float childHeight = appendTopic(rows, child, depth + 1, childBaseY + childConsumed * expansion, alpha * expansion);
            childConsumed += childHeight;
        }
        return consumed + childConsumed * expansion;
    }

    private double maxNavScroll() {
        int viewportHeight = rootLayout().navPane().height - PANEL_HEADER_HEIGHT - 12;
        return Math.max(0.0D, buildNavLayout().totalHeight() - viewportHeight);
    }

    private double maxListScroll() {
        if (shouldCollapseListPane()) {
            return 0.0D;
        }
        int viewportHeight = rootLayout().listPane().height - PANEL_HEADER_HEIGHT - 12;
        int entryCount = selectedSection == null ? 0 : selectedSection.entries().size();
        return Math.max(0.0D, entryCount * (double) ENTRY_ROW_HEIGHT - viewportHeight);
    }

    private double maxArticleScroll(Pane pane) {
        return articleMetrics(pane).maxScroll();
    }

    private double maxContactsScroll(Pane pane) {
        int viewportHeight = Math.max(1, pane.y + pane.height - 10 - contactsContentTop(pane));
        return Math.max(0.0D, measureContactsHeight(pane) - viewportHeight);
    }



    private ArticleMetrics articleMetrics(Pane pane) {
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        int contentWidthWithoutScrollbar = pane.width - 20;
        if (selectedEntry == null) {
            return new ArticleMetrics(contentWidthWithoutScrollbar, 0, 0.0D);
        }

        int contentHeightWithoutScrollbar = measureArticleHeight(font, selectedEntry, contentWidthWithoutScrollbar);
        double maxScrollWithoutScrollbar = Math.max(0.0D, contentHeightWithoutScrollbar - viewportHeight + 4.0D);
        if (maxScrollWithoutScrollbar <= 0.0D) {
            return new ArticleMetrics(contentWidthWithoutScrollbar, contentHeightWithoutScrollbar, 0.0D);
        }

        int contentWidthWithScrollbar = pane.width - 20 - SCROLLBAR_WIDTH;
        int contentHeightWithScrollbar = measureArticleHeight(font, selectedEntry, contentWidthWithScrollbar);
        double maxScrollWithScrollbar = Math.max(0.0D, contentHeightWithScrollbar - viewportHeight + 4.0D);
        return new ArticleMetrics(contentWidthWithScrollbar, contentHeightWithScrollbar, maxScrollWithScrollbar);
    }

    private void refreshReferenceEntries() {
        cancelArchiveTabDrag();
        String tabId = activeTab == null ? "wiki" : activeTab.id();
        String entryId = selectedEntry == null ? "" : selectedEntry.id();
        String sectionId = selectedSection == null ? "" : selectedSection.id();
        boolean pinned = PINNED_SECTION_ID.equals(sectionId);
        archiveTabs.clear();
        archiveTabs.addAll(CyberwareWikiData.buildArchiveTabs());
        applySavedTabOrder();
        entryIndex.clear();
        tabByEntryId.clear();
        baseSectionsByEntryId.clear();
        indexTabs(archiveTabs);
        activeTab = archiveTabs.stream().filter(tab -> tab.id().equals(tabId)).findFirst().orElse(archiveTabs.getFirst());
        if (!entryId.isEmpty() && entryIndex.containsKey(entryId)) {
            selectedEntry = entryIndex.get(entryId);
            selectedSection = baseSectionsByEntryId.get(entryId);
            if (pinned) {
                var topic = buildPinnedTopic();
                if (topic != null) selectedSection = topic.section();
            }
        } else if (!isLoreTab() && !isSchematicsTab() && !isContactsTab() && !isBankingTab() && !isQuestsTab()) {
            selectFirstSection();
        }
        articleContentHeight = 0;
        hoveredVisualStack = ItemStack.EMPTY;
        if (wikiSearching()) applyWikiSearch(wikiQuery);
    }
    private void activateTab(ArchiveTab tab) {
        hudBrowser.release();
        hudBrowser.unfocus();
        tarotBrowser.release();
        journalBrowser.hide();
        compassBrowser.hide();
        if (wikiSearch != null) wikiSearch.setFocused(false);
        if (contactsSearch != null) contactsSearch.setFocused(false);
        dragTarget = DragTarget.NONE;
        scrollbars.values().forEach(de.artemis.cyberneticenhancements.client.ui.UiScrollbar::release);
        schematicBrowser.release();
        contractBrowser.release();
        schematicBrowser.unfocus();
        contractBrowser.unfocus();
        bankingBrowser.hide();
        activeTab = tab == null && !archiveTabs.isEmpty() ? archiveTabs.getFirst() : tab;
        if (wikiSearching()) applyWikiSearch(wikiQuery);
        expandedTopics.clear();
        topicExpansion.clear();
        navScroll.reset();
        listScroll.reset();
        articleScroll.reset();

        List<WikiTopic> topics = displayTopics();
        for (WikiTopic topic : topics) {
            expandedTopics.add(topic.id());
        }
        seedTopicExpansion(topics, 1.0F);
        sectionChangedAt = animationAnchorTime();
        contentChangedAt = animationAnchorTime();
        if (isBankingTab()) {
            selectedSection = null;
            selectedEntry = null;
            requestBankingRefresh();
        } else if (isContactsTab()) {
            selectedSection = null;
            selectedEntry = null;
            requestContactsRefresh();
        } else if (isQuestsTab()) {
            contractBrowser.reset();
            selectedSection = null;
            selectedEntry = null;
            requestQuestsRefresh();
        } else {
            selectFirstSection();
        }
    }

    private void requestContactsRefresh() {
        if (minecraft != null && minecraft.player != null) {
            PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
        }
    }

    private void requestBankingRefresh() {
        if (minecraft != null && minecraft.player != null) {
            PacketDistributor.sendToServer(new ArchiveBankingRequestPayload());
        }
    }

    private void requestQuestsRefresh() {
        if (minecraft != null && minecraft.player != null) {
            PacketDistributor.sendToServer(new ArchiveQuestsRequestPayload());
        }
    }

    private void selectEntryById(String entryId) {
        WikiEntry entry = entryIndex.get(entryId);
        WikiSection section = baseSectionsByEntryId.get(entryId);
        if (entry == null || section == null) {
            return;
        }
        setSelectedSection(section);
        setSelectedEntry(entry);
    }

    private void selectFirstSection() {
        for (WikiTopic topic : displayTopics()) {
            WikiSection section = firstSection(topic);
            if (section != null) {
                setSelectedSection(section);
                return;
            }
        }
        selectedSection = null;
        selectedEntry = null;
    }

    private WikiSection firstSection(WikiTopic topic) {
        if (topic.section() != null) {
            return topic.section();
        }
        for (WikiTopic child : topic.children()) {
            WikiSection section = firstSection(child);
            if (section != null) {
                return section;
            }
        }
        return null;
    }

    private void setSelectedSection(WikiSection section) {
        if (section == null) {
            return;
        }
        if (selectedSection != null && selectedSection.id().equals(section.id())) {
            selectedSection = section;
            if (selectedEntry != null && section.entries().stream().noneMatch(entry -> entry.id().equals(selectedEntry.id()))) {
                setSelectedEntry(section.entries().isEmpty() ? null : section.entries().getFirst());
            }
            return;
        }
        selectedSection = section;
        listScroll.reset();
        sectionChangedAt = animationAnchorTime();
        setSelectedEntry(section.entries().isEmpty() ? null : section.entries().getFirst());
        contentChangedAt = animationAnchorTime();
    }

    private void setSelectedEntry(WikiEntry entry) {
        selectedEntry = entry;
        articleScroll.reset();
        contentChangedAt = animationAnchorTime();
    }

    private RootLayout rootLayout() {
        int rootX = OUTER_MARGIN;
        int rootY = OUTER_MARGIN;
        int rootWidth = width - OUTER_MARGIN * 2;
        int rootHeight = height - OUTER_MARGIN * 2;
        int contentY = rootY + TOP_BAR_HEIGHT + PANEL_GAP;
        int contentHeight = rootHeight - TOP_BAR_HEIGHT - FOOTER_HEIGHT - PANEL_GAP * 2;
        if (isLoreTab() || isSchematicsTab() || isBankingTab() || isContactsTab() || isQuestsTab()
                || isCompassTab() || isJournalTab() || isHudTab()) {
            Pane fullPane = new Pane(rootX + PANEL_GAP, contentY, rootWidth - PANEL_GAP * 2, contentHeight);
            Pane hiddenPane = new Pane(0, 0, 0, 0);
            return new RootLayout(rootX, rootY, rootWidth, rootHeight, hiddenPane, hiddenPane, fullPane, true);
        }

        if (isWikiTab()) {
            contentY += 42 + PANEL_GAP;
            contentHeight -= 42 + PANEL_GAP;
        }
        if (wikiSearching()) {
            Pane listPane = new Pane(rootX + PANEL_GAP, contentY, LIST_WIDTH, contentHeight);
            int articleX = listPane.x + listPane.width + PANEL_GAP;
            Pane articlePane = new Pane(articleX, contentY, rootX + rootWidth - PANEL_GAP - articleX, contentHeight);
            return new RootLayout(rootX, rootY, rootWidth, rootHeight, new Pane(0, 0, 0, 0), listPane, articlePane, false);
        }

        Pane navPane = new Pane(rootX + PANEL_GAP, contentY, NAV_WIDTH, contentHeight);
        boolean listCollapsed = shouldCollapseListPane();
        int listX = navPane.x + navPane.width + PANEL_GAP;
        Pane listPane = new Pane(listX, contentY, listCollapsed ? 0 : LIST_WIDTH, contentHeight);
        int contentRight = rootX + rootWidth - PANEL_GAP;
        int articleX = listCollapsed ? listX : listX + LIST_WIDTH + PANEL_GAP;
        Pane articlePane = new Pane(articleX, contentY, contentRight - articleX, contentHeight);
        return new RootLayout(rootX, rootY, rootWidth, rootHeight, navPane, listPane, articlePane, listCollapsed);
    }

    private boolean shouldCollapseListPane() {
        if (wikiSearching()) return false;
        return selectedSection != null && selectedSection.entries().size() <= 1;
    }

    private boolean isSchematicsTab() {
        return activeTab != null && activeTab.id().equals("schematics");
    }

    private boolean isLoreTab() { return activeTab != null && activeTab.id().equals("lore"); }

    private boolean isBankingTab() {
        return activeTab != null && BANKING_TAB_ID.equals(activeTab.id());
    }

    private boolean isCompassTab() {
        return activeTab != null && activeTab.id().equals("compass");
    }

    private boolean isJournalTab() { return activeTab != null && "journal".equals(activeTab.id()); }
    public boolean isHudTab() { return activeTab != null && "hud".equals(activeTab.id()); }

    private boolean isContactsTab() {
        return activeTab != null && CONTACTS_TAB_ID.equals(activeTab.id());
    }

    private boolean isQuestsTab() {
        return activeTab != null && QUESTS_TAB_ID.equals(activeTab.id());
    }

    private void drawTopBar(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + TOP_BAR_HEIGHT, StationScreenStyle.PANEL_ALT);
        guiGraphics.fill(x + 2, y + TOP_BAR_HEIGHT - 1, x + width - 2, y + TOP_BAR_HEIGHT, StationScreenStyle.FRAME_HIGHLIGHT);
        Rect closeBounds = closeButtonBounds(x, y, width);
        renderArchiveTabs(guiGraphics, x, y, width);
        drawActionButton(guiGraphics, closeBounds.x(), closeBounds.y(), closeBounds.width(), closeBounds.height(), de.artemis.cyberneticenhancements.client.ui.UiCloseControl.label(), 0xFFFFFFFF, false);
    }

    private void renderBootSequence(GuiGraphics guiGraphics, int rootX, int rootY, int rootWidth, int rootHeight) {
        if (openingFrame.bootOpacity() <= 0.0F) {
            return;
        }
        openingFade.render(guiGraphics, openingFrame.bootOpacity(), openingFrame.bootScale(), openingFrame.bootOffsetY(),
                () -> renderBootPanel(guiGraphics, rootX, rootY, rootWidth, rootHeight));
    }

    private void renderBootPanel(GuiGraphics guiGraphics, int rootX, int rootY, int rootWidth, int rootHeight) {
        float progress = bootSequenceProgress();
        int panelWidth = Math.min(rootWidth - 80, 440), panelHeight = 162;
        int panelX = rootX + (rootWidth - panelWidth) / 2;
        int panelY = rootY + (rootHeight - panelHeight) / 2 - 10;
        String percent = String.format(Locale.ROOT, "%d%%", Mth.floor(progress * 100.0F));
        de.artemis.cyberneticenhancements.client.ui.UiConnectionPanel.render(guiGraphics, font, panelX, panelY, panelWidth, panelHeight, progress, openingFrame.bootLinearProgress(),
                Component.translatable("screen.cyberneticenhancements.archive.boot.header"),
                Component.translatable("screen.cyberneticenhancements.archive.boot.title"),
                Component.translatable("screen.cyberneticenhancements.archive.boot.subtitle"),
                List.of(Component.translatable("screen.cyberneticenhancements.archive.boot.phase.1"),
                        Component.translatable("screen.cyberneticenhancements.archive.boot.phase.2"),
                        Component.translatable("screen.cyberneticenhancements.archive.boot.phase.3"),
                        Component.translatable("screen.cyberneticenhancements.archive.boot.phase.4")),
                Component.translatable("screen.cyberneticenhancements.archive.boot.status", percent),
                Component.translatable("screen.cyberneticenhancements.archive.boot.ready"));
    }

    private void drawPanelShell(GuiGraphics guiGraphics, Pane pane, Component title) {
        drawPanelShell(guiGraphics, pane, title, 0);
    }

    private void drawPanelShell(GuiGraphics guiGraphics, Pane pane, Component title, int rightInset) {
        WikiFrameRenderer.drawPanel(guiGraphics, pane.x, pane.y, pane.width, pane.height, UiRenderer.PANEL);
        guiGraphics.fill(pane.x + 2, pane.y + PANEL_HEADER_HEIGHT - 1, pane.x + pane.width - 2, pane.y + PANEL_HEADER_HEIGHT, StationScreenStyle.FRAME_HIGHLIGHT_SOFT);
        guiGraphics.drawString(font, trimStyled(title, Math.max(24, pane.width - 20 - rightInset)), pane.x + 10, pane.y + (PANEL_HEADER_HEIGHT - font.lineHeight) / 2, StationScreenStyle.TEXT_PRIMARY, false);
    }

    private void drawArchiveCard(GuiGraphics g, int x, int y, int width, int height, int alphaMask) {

        UiRenderer.panel(g, x, y, width, height, withAlpha(UiRenderer.ROW, alphaMask), withAlpha(UiRenderer.FIELD_EDGE, alphaMask));
    }

    private void drawListRow(GuiGraphics g, int x, int y, int width, int height, boolean selected, float emphasis, float alpha) {
        UiRenderer.control(g, x, y, width, height, true, selected, emphasis, ((int) (Math.clamp(alpha, 0, 1) * 255) << 24) | 0xFFFFFF);
    }

    private void drawMiniChip(GuiGraphics g, int x, int y, int width, int height, Component text, int alphaMask) {
        UiRenderer.panel(g, x, y, width, height, withAlpha(UiRenderer.FIELD, alphaMask), withAlpha(UiRenderer.FIELD_EDGE, alphaMask));
        var lines = font.split(text, Math.max(1, width - 12));
        int lineY = y + (height - lines.size() * font.lineHeight) / 2;
        for (var line : lines) {
            g.drawString(font, line, x + (width - font.width(line)) / 2, lineY, withAlpha(UiRenderer.TEXT_SECONDARY, alphaMask), false);
            lineY += font.lineHeight;
        }
    }

    private void drawInteractiveFrame(GuiGraphics graphics, int x, int y, int width, int height, boolean selected, int alphaMask) {
        boolean hovered = !bootSequenceBlocksInput() && currentMouseX >= x && currentMouseX < x + width
                && currentMouseY >= y && currentMouseY < y + height
                && graphics.containsPointInScissor(currentMouseX, currentMouseY);
        UiRenderer.control(graphics, x, y, width, height, true, selected, hovered, alphaMask);
    }

    private void drawActionButton(GuiGraphics graphics, int x, int y, int width, int height, Component label,
                                  int alphaMask, boolean selected) {
        boolean hovered = !bootSequenceBlocksInput() && currentMouseX >= x && currentMouseX < x + width
                && currentMouseY >= y && currentMouseY < y + height && graphics.containsPointInScissor(currentMouseX, currentMouseY);
        UiRenderer.button(graphics, font, x, y, width, height, label, true, selected, hovered, alphaMask);
    }

    private String draggedArchiveTab;
    private double tabPressX, tabGrabX;
    private boolean tabDragActive, tabDropValid;
    private List<ArchiveTab> tabDragOriginal = List.of();
    private final Map<String, Float> tabVisualX = new HashMap<>();
    private long tabAnimationAt = System.nanoTime();

    private void cancelArchiveTabDrag() {
        if (draggedArchiveTab != null) {
            archiveTabs.clear(); archiveTabs.addAll(tabDragOriginal);
        }
        draggedArchiveTab = null;
        tabDragActive = false;
        tabDragOriginal = List.of();
    }

    private Rect archiveTabVisualBounds(Rect bounds) {
        return new Rect(bounds.id(), Math.round(tabVisualX.getOrDefault(bounds.id(), (float) bounds.x())),
                bounds.y(), bounds.width(), bounds.height());
    }
    private void applySavedTabOrder() {
        var ids = de.artemis.cyberneticenhancements.client.ui.ArchiveTabOrder.normalize(
                archiveTabs.stream().map(ArchiveTab::id).toList(),
                de.artemis.cyberneticenhancements.client.ClientConfig.ARCHIVE_TAB_ORDER.get());
        archiveTabs.sort(java.util.Comparator.comparingInt(tab -> ids.indexOf(tab.id())));
    }

    private void renderArchiveTabs(GuiGraphics graphics, int rootX, int rootY, int rootWidth) {
        long now = System.nanoTime();
        float blend = (float) (1 - Math.exp(-Math.min(0.1, (now - tabAnimationAt) / 1_000_000_000.0) * 20));
        tabAnimationAt = now;
        Rect grabbed = null;
        for (Rect bounds : archiveTabBounds(rootX, rootY, rootWidth)) {
            ArchiveTab tab = archiveTabById(bounds.id());
            if (tab == null) continue;
            float visualX = tabVisualX.getOrDefault(tab.id(), (float) bounds.x());
            visualX += (bounds.x() - visualX) * blend;
            if (Math.abs(bounds.x() - visualX) < 0.5F) visualX = bounds.x();
            tabVisualX.put(tab.id(), visualX);
            if (tabDragActive && bounds.id().equals(draggedArchiveTab)) {
                grabbed = bounds;
                UiRenderer.outline(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), UiRenderer.FIELD_EDGE);
                continue;
            }
            drawActionButton(graphics, Math.round(visualX), bounds.y(), bounds.width(), bounds.height(), tab.title(),
                    0xFFFFFFFF, activeTab != null && activeTab.id().equals(tab.id()));
        }
        if (grabbed != null) {
            int gx = Math.clamp((int) (currentMouseX - tabGrabX), rootX + 12,
                    closeButtonBounds(rootX, rootY, rootWidth).x() - 12 - grabbed.width());
            int gy = grabbed.y() - 3;
            UiRenderer.panel(graphics, gx, gy, grabbed.width(), grabbed.height(), UiRenderer.ROW_SELECTED,
                    tabDropValid ? UiRenderer.ACCENT : UiRenderer.WARNING);
            ArchiveTab tab = archiveTabById(draggedArchiveTab);
            graphics.drawCenteredString(font, tab.title(), gx + grabbed.width() / 2, gy + (grabbed.height() - font.lineHeight) / 2, UiRenderer.TEXT);
        }
    }

    private boolean handleArchiveTabClick(double mouseX, double mouseY, int rootX, int rootY, int rootWidth) {
        for (Rect bounds : archiveTabBounds(rootX, rootY, rootWidth)) {
            bounds = archiveTabVisualBounds(bounds);
            if (!isInside(mouseX, mouseY, bounds)) continue;
            draggedArchiveTab = bounds.id();
            tabPressX = mouseX; tabGrabX = mouseX - bounds.x();
            tabDragActive = false;
            tabDragOriginal = List.copyOf(archiveTabs);
            return true;
        }
        return false;
    }

    private void updateArchiveTabDrag(double mouseX, double mouseY) {
        if (!tabDragActive && Math.abs(mouseX - tabPressX) < 6) return;
        tabDragActive = true;
        var layout = rootLayout();
        Rect close = closeButtonBounds(layout.rootX(), layout.rootY(), layout.rootWidth());
        tabDropValid = mouseX >= layout.rootX() + 12 && mouseX < close.x() - 12;
        if (!tabDropValid) return;
        var bounds = archiveTabBounds(layout.rootX(), layout.rootY(), layout.rootWidth());
        int target = de.artemis.cyberneticenhancements.client.ui.ArchiveTabOrder.dropIndex(
                bounds.stream().map(Rect::id).toList(), bounds.stream().map(rect -> rect.x() + rect.width() / 2).toList(),
                draggedArchiveTab, mouseX);
        var ids = de.artemis.cyberneticenhancements.client.ui.ArchiveTabOrder.move(
                archiveTabs.stream().map(ArchiveTab::id).toList(), draggedArchiveTab, target);
        archiveTabs.sort(java.util.Comparator.comparingInt(tab -> ids.indexOf(tab.id())));
    }

    private boolean finishArchiveTabDrag(double mouseX, double mouseY) {
        updateArchiveTabDrag(mouseX, mouseY);
        String id = draggedArchiveTab;
        if (tabDragActive) {
            if (tabDropValid) {
                var order = de.artemis.cyberneticenhancements.client.ClientConfig.ARCHIVE_TAB_ORDER;
                order.set(archiveTabs.stream().map(ArchiveTab::id).toList());
                order.save();
                playClick();
            } else {
                archiveTabs.clear(); archiveTabs.addAll(tabDragOriginal);
            }
        } else {
            var layout = rootLayout();
            for (Rect bounds : archiveTabBounds(layout.rootX(), layout.rootY(), layout.rootWidth())) {
                if (bounds.id().equals(id) && isInside(mouseX, mouseY, archiveTabVisualBounds(bounds))) {
                    ArchiveTab tab = archiveTabById(id);
                    if (activeTab == null || !activeTab.id().equals(id)) activateTab(tab);
                    playClick();
                    break;
                }
            }
        }
        draggedArchiveTab = null;
        tabDragActive = false;
        tabDragOriginal = List.of();
        return true;
    }
    private List<Rect> archiveTabBounds(int rootX, int rootY, int rootWidth) {
        Rect closeBounds = closeButtonBounds(rootX, rootY, rootWidth);
        int startX = rootX + 12;
        int maxX = closeBounds.x() - 12;
        int y = closeBounds.y();
        List<Rect> bounds = new ArrayList<>(archiveTabs.size());
        int tabSpace = maxX - startX - Math.max(0, archiveTabs.size() - 1) * TOP_TAB_GAP;
        int naturalWidth = archiveTabs.stream().mapToInt(tab -> Math.max(64, font.width(tab.title()) + 18)).sum();
        double widthScale = naturalWidth == 0 ? 1 : Math.min(1, tabSpace / (double) naturalWidth);
        int x = startX;
        for (ArchiveTab tab : archiveTabs) {
            int tabWidth = Math.max(1, (int) Math.floor(Math.max(64, font.width(tab.title()) + 18) * widthScale));
            if (x + tabWidth > maxX) {
                break;
            }
            bounds.add(new Rect(tab.id(), x, y, tabWidth, TOP_TAB_HEIGHT));
            x += tabWidth + TOP_TAB_GAP;
        }
        return bounds;
    }

    private ArchiveTab archiveTabById(String id) {
        for (ArchiveTab tab : archiveTabs) {
            if (tab.id().equals(id)) {
                return tab;
            }
        }
        return null;
    }

    private Rect closeButtonBounds(int rootX, int rootY, int rootWidth) {
        return new Rect("close", rootX + rootWidth - CLOSE_BUTTON_WIDTH - 16, rootY + 8, CLOSE_BUTTON_WIDTH, de.artemis.cyberneticenhancements.client.ui.UiCloseControl.HEIGHT);
    }

    private FormattedCharSequence trimStyled(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return Language.getInstance().getVisualOrder(text);
        }

        int ellipsisWidth = font.width("...");
        FormattedText base = font.substrByWidth(text, Math.max(0, maxWidth - ellipsisWidth));
        FormattedText combined = FormattedText.composite(base, Component.literal("...").setStyle(text.getStyle()));
        return Language.getInstance().getVisualOrder(combined);
    }

    private boolean isInsidePane(double mouseX, double mouseY, Pane pane) {
        return mouseX >= pane.x && mouseX <= pane.x + pane.width && mouseY >= pane.y && mouseY <= pane.y + pane.height;
    }

    private boolean isInside(double mouseX, double mouseY, Rect bounds) {
        return mouseX >= bounds.x() && mouseX <= bounds.x() + bounds.width() && mouseY >= bounds.y() && mouseY <= bounds.y() + bounds.height();
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void tickTopicExpansion(List<WikiTopic> topics) {
        for (WikiTopic topic : topics) {
            float current = topicExpansion.getOrDefault(topic.id(), expandedTopics.contains(topic.id()) ? 1.0F : 0.0F);
            float target = expandedTopics.contains(topic.id()) ? 1.0F : 0.0F;
            float next = Mth.lerp(0.22F, current, target);
            if (Math.abs(next - target) < 0.02F) {
                next = target;
            }
            topicExpansion.put(topic.id(), next);
            tickTopicExpansion(topic.children());
        }
    }

    private void seedTopicExpansion(List<WikiTopic> topics, float value) {
        for (WikiTopic topic : topics) {
            topicExpansion.put(topic.id(), value);
            seedTopicExpansion(topic.children(), value);
        }
    }

    private void indexTabs(List<ArchiveTab> tabs) {
        for (ArchiveTab tab : tabs) {
            indexTopics(tab.id(), tab.topics());
        }
        wikiSearchDocuments = WikiSearchIndex.build(tabs);
    }

    private void indexTopics(String tabId, List<WikiTopic> topics) {
        for (WikiTopic topic : topics) {
            if (topic.section() != null) {
                for (WikiEntry entry : topic.section().entries()) {
                    entryIndex.put(entry.id(), entry);
                    tabByEntryId.put(entry.id(), tabId);
                    baseSectionsByEntryId.put(entry.id(), topic.section());
                }
            }
            indexTopics(tabId, topic.children());
        }
    }

    private List<WikiTopic> currentTabTopics() {
        return activeTab == null ? List.of() : activeTab.topics();
    }

    private List<WikiTopic> displayTopics() {
        if (wikiSearching() && wikiSearchResults != null) return List.of(new WikiTopic("wiki_search_results", wikiSearchResults.title(), List.of(), wikiSearchResults));
        List<WikiTopic> currentTopics = currentTabTopics();
        WikiTopic pinnedTopic = buildPinnedTopic();
        if (pinnedTopic == null) {
            return currentTopics;
        }

        if (!topicExpansion.containsKey(PINNED_TOPIC_ID)) {
            expandedTopics.add(PINNED_TOPIC_ID);
            seedTopicExpansion(List.of(pinnedTopic), 1.0F);
        }

        List<WikiTopic> topics = new ArrayList<>(currentTopics.size() + 1);
        topics.add(pinnedTopic);
        topics.addAll(currentTopics);
        return topics;
    }

    private WikiTopic buildPinnedTopic() {
        if (activeTab == null || PINNED_ENTRY_IDS.isEmpty()) {
            return null;
        }

        List<WikiEntry> pinnedEntries = new ArrayList<>();
        for (String entryId : PINNED_ENTRY_IDS) {
            if (!activeTab.id().equals(tabByEntryId.get(entryId))) {
                continue;
            }
            WikiEntry entry = entryIndex.get(entryId);
            if (entry == null) {
                continue;
            }
            pinnedEntries.add(entry);
        }

        if (pinnedEntries.isEmpty()) {
            return null;
        }

        return new WikiTopic(
                PINNED_TOPIC_ID,
                Component.translatable("screen.cyberneticenhancements.archive.pinned"),
                List.of(),
                new WikiSection(PINNED_SECTION_ID, Component.translatable("screen.cyberneticenhancements.archive.pinned"), pinnedEntries)
        );
    }

    private void renderPinButton(GuiGraphics guiGraphics, Pane articlePane) {
        if (selectedEntry == null) {
            return;
        }

        PinButtonBounds bounds = pinButtonBounds(articlePane);
        boolean pinned = isPinned(selectedEntry.id());
        boolean hovered = currentMouseX >= bounds.x() && currentMouseX < bounds.x() + bounds.width()
                && currentMouseY >= bounds.y() && currentMouseY < bounds.y() + bounds.height();
        UiRenderer.listPin(guiGraphics, font, bounds.x(), bounds.y(), pinned, hovered);
        if (hovered) hoveredPinTooltip = pinLabel(selectedEntry.id());
    }

    private boolean handlePinButtonClick(double mouseX, double mouseY, Pane articlePane) {
        if (selectedEntry == null) {
            return false;
        }

        PinButtonBounds bounds = pinButtonBounds(articlePane);
        if (mouseX < bounds.x() || mouseX > bounds.x() + bounds.width() || mouseY < bounds.y() || mouseY > bounds.y() + bounds.height()) {
            return false;
        }

        togglePinned(selectedEntry.id());
        playClick();
        return true;
    }

    private PinButtonBounds pinButtonBounds(Pane articlePane) {
        return new PinButtonBounds(articlePane.x + articlePane.width - PIN_BUTTON_WIDTH - 10, articlePane.y + (PANEL_HEADER_HEIGHT - 20) / 2, PIN_BUTTON_WIDTH, 20);
    }

    private boolean isPinned(String entryId) {
        return PINNED_ENTRY_IDS.contains(entryId);
    }

    private Component pinLabel(String entryId) {
        return Component.translatable(isPinned(entryId) ? "screen.cyberneticenhancements.archive.unpin" : "screen.cyberneticenhancements.archive.pin");
    }

    private int topicEntryCount(WikiTopic topic) {
        if (topic.section() != null) return topic.section().entries().size();
        return topic.children().stream().mapToInt(this::topicEntryCount).sum();
    }

    private void togglePinned(String entryId) {
        if (!PINNED_ENTRY_IDS.add(entryId)) {
            PINNED_ENTRY_IDS.remove(entryId);
        }
        var setting = de.artemis.cyberneticenhancements.client.ClientConfig.WIKI_PINNED_ENTRIES;
        setting.set(List.copyOf(PINNED_ENTRY_IDS));
        setting.save();
        refreshPinnedSelectionState(entryId);
    }

    private void refreshPinnedSelectionState(String changedEntryId) {
        if (selectedEntry == null) {
            return;
        }

        if (selectedSection != null && selectedSection.id().equals(PINNED_SECTION_ID)) {
            WikiTopic pinnedTopic = buildPinnedTopic();
            if (pinnedTopic == null || pinnedTopic.section() == null) {
                WikiSection fallbackSection = baseSectionsByEntryId.get(selectedEntry.id());
                if (fallbackSection != null) {
                    selectedSection = fallbackSection;
                    listScroll.reset();
                    sectionChangedAt = animationAnchorTime();
                }
            } else {
                selectedSection = pinnedTopic.section();
                if (!isPinned(selectedEntry.id())) {
                    selectedEntry = selectedSection.entries().isEmpty() ? null : selectedSection.entries().getFirst();
                    articleScroll.reset();
                    contentChangedAt = animationAnchorTime();
                }
            }
        }
    }

    private boolean bootSequenceBlocksInput() {
        return !openingFrame.complete();
    }

    private float bootSequenceProgress() {
        return openingFrame.bootProgress();
    }

    private float animateToward(Map<String, Float> state, String key, float target, float speed) {
        float current = state.getOrDefault(key, target);
        float next = Mth.lerp(speed, current, target);
        if (Math.abs(next - target) < 0.02F) {
            next = target;
        }
        state.put(key, next);
        return next;
    }

    private long archiveUiStartAt() {
        return openedAt + (showOpeningAnimation ? BOOT_SEQUENCE_MS : 0);
    }

    private long animationAnchorTime() {
        return Math.max(System.currentTimeMillis(), archiveUiStartAt());
    }

    private static float animationProgress(long startedAt, long durationMs) {
        return Mth.clamp((System.currentTimeMillis() - startedAt) / (float) durationMs, 0.0F, 1.0F);
    }

    private static float easeOutCubic(float progress) {
        float inverted = 1.0F - progress;
        return 1.0F - inverted * inverted * inverted;
    }

    private static int withAlpha(int color, int alphaMask) {
        return (alphaMask & 0xFF000000) | (color & 0x00FFFFFF);
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (color & 0x00FFFFFF);
    }

    private static int blendColors(int from, int to, float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        int fromR = from >> 16 & 0xFF;
        int fromG = from >> 8 & 0xFF;
        int fromB = from & 0xFF;
        int toR = to >> 16 & 0xFF;
        int toG = to >> 8 & 0xFF;
        int toB = to & 0xFF;
        int r = Mth.floor(Mth.lerp(clamped, fromR, toR));
        int g = Mth.floor(Mth.lerp(clamped, fromG, toG));
        int b = Mth.floor(Mth.lerp(clamped, fromB, toB));
        return r << 16 | g << 8 | b;
    }

    private record Pane(int x, int y, int width, int height) {
    }

    private record RootLayout(int rootX, int rootY, int rootWidth, int rootHeight, Pane navPane, Pane listPane,
                              Pane articlePane, boolean listCollapsed) {
    }

    private record NavRow(WikiTopic topic, int depth, float y, float alpha) {
    }

    private record NavLayout(List<NavRow> rows, float totalHeight) {
    }

    private record ArticleMetrics(int contentWidth, int contentHeight, double maxScroll) {
    }

    private record ContactFilter(String id, Component label) {
    }


    private record Rect(String id, int x, int y, int width, int height) {
    }

    private record PinButtonBounds(int x, int y, int width, int height) {
    }

    private enum DragTarget {
        NONE,
        NAV,
        LIST,
        ARTICLE
    }

    private static final class ScrollState extends de.artemis.cyberneticenhancements.client.ui.UiScrollState {
    }

    public static String findEntryId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return ENTRY_IDS_BY_ITEM.get(stack.getItem());
    }

    public static String findEntryId(Block block) {
        if (block == null) {
            return null;
        }
        return ENTRY_IDS_BY_BLOCK.get(block);
    }

    private static Map<Item, String> createEntryIdsByItem() {
        Map<Item, String> entries = new LinkedHashMap<>();
        for (var item : ModItems.cyberwareItems()) {
            entries.put(item.get(), item.getId().getPath());
        }
        for (var item : ModItems.moduleItems()) {
            entries.put(item.get(), item.getId().getPath());
        }
        for (var item : ModItems.chipwareItems()) {
            entries.put(item.get(), item.getId().getPath());
        }
        for (var item : ModItems.consumableItems()) {
            entries.put(item.get(), item.getId().getPath());
        }
        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.PARTS.forEach((id, item) -> entries.put(item.get(), id));
        for (var item : List.of(ModItems.COPPER_WIRING, ModItems.CONDUCTIVE_PASTE, ModItems.SERVO_SCREWS,
                ModItems.SENSOR_LENS, ModItems.MICRO_BATTERY, ModItems.REPLACEMENT_JOINT, ModItems.BASIC_CIRCUIT_PLATE,
                ModItems.COMMON_ITEM_COMPONENTS, ModItems.UNCOMMON_ITEM_COMPONENTS, ModItems.RARE_ITEM_COMPONENTS,
                ModItems.EPIC_ITEM_COMPONENTS, ModItems.LEGENDARY_ITEM_COMPONENTS))
            entries.put(item.get(), item.getId().getPath());
        entries.put(ModBlocks.UPGRADE_STATION.asItem(), "station_upgrade");
        entries.put(ModItems.FIXER_SPAWN_EGG.get(), "npc_fixer");
        entries.put(ModItems.RIPPERDOC_SPAWN_EGG.get(), "npc_ripperdoc");
        entries.put(ModItems.TECHIE_SPAWN_EGG.get(), "npc_techie");
        entries.put(ModItems.NETRUNNER_SPAWN_EGG.get(), "npc_netrunner");
        entries.put(ModItems.MERC_SPAWN_EGG.get(), "npc_merc");
        entries.put(ModBlocks.RIPPER_STATION.asItem(), "station_ripper");
        entries.put(ModBlocks.TECH_STATION.asItem(), "station_tech");
        entries.put(ModBlocks.RECYCLER_STATION.asItem(), "station_recycler");
        entries.put(ModBlocks.CYBERWARE_FABRICATOR.asItem(), "station_fabricator");
        entries.put(ModBlocks.DROPOFF_POINT.asItem(), "station_dropoff");
        entries.put(ModBlocks.RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.UNCOMMON_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.RARE_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.EPIC_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.LEGENDARY_RELIC_CACHE.asItem(), "station_relic_cache");
        return Map.copyOf(entries);
    }

    private static Map<Block, String> createEntryIdsByBlock() {
        Map<Block, String> entries = new LinkedHashMap<>();
        entries.put(ModBlocks.UPGRADE_STATION.get(), "station_upgrade");
        entries.put(ModBlocks.RIPPER_STATION.get(), "station_ripper");
        entries.put(ModBlocks.TECH_STATION.get(), "station_tech");
        entries.put(ModBlocks.RECYCLER_STATION.get(), "station_recycler");
        entries.put(ModBlocks.CYBERWARE_FABRICATOR.get(), "station_fabricator");
        entries.put(ModBlocks.DROPOFF_POINT.get(), "station_dropoff");
        entries.put(ModBlocks.RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.UNCOMMON_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.RARE_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.EPIC_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.LEGENDARY_RELIC_CACHE.get(), "station_relic_cache");
        return Map.copyOf(entries);
    }
}
