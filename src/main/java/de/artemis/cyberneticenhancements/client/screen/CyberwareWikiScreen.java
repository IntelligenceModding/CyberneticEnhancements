package de.artemis.cyberneticenhancements.client.screen;

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
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.UpgradeStageVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.SectionVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ServiceOperationVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.ServiceOperationsVisual;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiEntry;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiSection;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.WikiTopic;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactActionPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingActionPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestActionPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsRequestPayload;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.screens.Screen;
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

public final class CyberwareWikiScreen extends Screen {
    private static final int OUTER_MARGIN = 14;
    private static final int PANEL_GAP = 8;
    private static final int TOP_BAR_HEIGHT = 54;
    private static final int FOOTER_HEIGHT = 18;
    private static final int NAV_WIDTH = 230;
    private static final int LIST_WIDTH = 230;
    private static final int ROW_HEIGHT = 22;
    private static final int ENTRY_ROW_HEIGHT = 26;
    private static final int SECTION_GAP = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int PANEL_HEADER_HEIGHT = 22;
    private static final int CHIP_HEIGHT = 16;
    private static final int VISUAL_SLOT_STRIDE = 20;
    private static final int VISUAL_CONTENT_PADDING = 4;
    private static final int TRANSFORM_OUTPUT_START = 58;
    private static final int SERVICE_MATERIALS_START = 34;
    private static final int SERVICE_OUTPUT_GAP = 24;
    private static final int RELIC_EXAMPLE_CELL_SIZE = 18;
    private static final int RELIC_EXAMPLE_CELL_GAP = 3;
    private static final int RELIC_EXAMPLE_ROUTE_THICKNESS = 2;
    private static final int PIN_BUTTON_WIDTH = 56;
    private static final int CLOSE_BUTTON_WIDTH = 48;
    private static final int QUEST_ACTION_BUTTON_WIDTH = 88;
    private static final int TOP_TAB_HEIGHT = 18;
    private static final int TOP_TAB_GAP = 6;
    private static final int CONTACT_FILTER_TAB_HEIGHT = 18;
    private static final int CONTACT_FILTER_TAB_GAP = 4;
    private static final int BANK_FILTER_TAB_HEIGHT = 18;
    private static final int BANK_FILTER_TAB_GAP = 4;
    private static final long OPEN_ANIMATION_MS = 220L;
    private static final long CONTENT_ANIMATION_MS = 180L;
    private static final long BOOT_SEQUENCE_MS = 2400L;
    private static final long BOOT_FADE_MS = 260L;
    private static final String BANKING_TAB_ID = "banking";
    private static final String CONTACTS_TAB_ID = "contacts";
    private static final String QUESTS_TAB_ID = "quests";
    private static final String BANK_FILTER_ALL = "all";
    private static final String BANK_FILTER_INCOME = "income";
    private static final String BANK_FILTER_EXPENSE = "expense";
    private static final String CONTACT_FILTER_ALL = "all";
    private static final String CONTACT_FILTER_PINNED = "pinned";
    private static final String CONTACT_FILTER_HIDDEN = "hidden";
    private static final String PINNED_TOPIC_ID = "pinned_entries_root";
    private static final String PINNED_SECTION_ID = "pinned_entries";
    private static final Set<String> PINNED_ENTRY_IDS = new LinkedHashSet<>();
    private static final Map<Item, String> ENTRY_IDS_BY_ITEM = createEntryIdsByItem();
    private static final Map<Block, String> ENTRY_IDS_BY_BLOCK = createEntryIdsByBlock();

    private final Screen parent;
    private final List<ArchiveTab> archiveTabs = CyberwareWikiData.buildArchiveTabs();
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
    private final long openedAt = System.currentTimeMillis();
    private long contentChangedAt = openedAt + BOOT_SEQUENCE_MS;
    private long sectionChangedAt = openedAt + BOOT_SEQUENCE_MS;

    private ArchiveTab activeTab;
    private WikiSection selectedSection;
    private WikiEntry selectedEntry;
    private DragTarget dragTarget = DragTarget.NONE;
    private int articleContentHeight;
    private ItemStack hoveredVisualStack = ItemStack.EMPTY;
    private int currentMouseX;
    private int currentMouseY;
    private final String initialEntryId;
    private int contactsRefreshCooldown;
    private String activeBankFilterId = BANK_FILTER_ALL;
    private String activeContactsFilterId = CONTACT_FILTER_ALL;
    private EditBox bankTargetField;
    private EditBox bankAmountField;
    private boolean bankPlayerDropdownOpen;

    public CyberwareWikiScreen(Screen parent) {
        this(parent, null);
    }

    public CyberwareWikiScreen(Screen parent, String initialEntryId) {
        super(Component.translatable("screen.cyberneticenhancements.archive.title"));
        this.parent = parent;
        this.initialEntryId = initialEntryId;
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
        initBankingFields();
        activateTab(activeTab);
        if (initialEntryId != null) {
            selectEntryById(initialEntryId);
        }
    }

    @Override
    public void onClose() {
        ArchiveBankingClientState.clear();
        ArchiveContactsClientState.clear();
        ArchiveQuestsClientState.clear();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
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

    private void initBankingFields() {
        bankTargetField = new EditBox(font, 0, 0, 120, 18, Component.translatable("screen.cyberneticenhancements.archive.banking.target"));
        bankTargetField.setMaxLength(32);
        bankTargetField.setHint(Component.translatable("screen.cyberneticenhancements.archive.banking.target"));
        bankTargetField.setVisible(false);
        addRenderableWidget(bankTargetField);

        bankAmountField = new EditBox(font, 0, 0, 72, 18, Component.translatable("screen.cyberneticenhancements.archive.banking.amount"));
        bankAmountField.setMaxLength(9);
        bankAmountField.setHint(Component.translatable("screen.cyberneticenhancements.archive.banking.amount"));
        bankAmountField.setVisible(false);
        addRenderableWidget(bankAmountField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        hoveredVisualStack = ItemStack.EMPTY;

        navScroll.tick();
        listScroll.tick();
        articleScroll.tick();
        tickTopicExpansion(displayTopics());
        int rootX = OUTER_MARGIN;
        int rootY = OUTER_MARGIN;
        int rootWidth = width - OUTER_MARGIN * 2;
        int rootHeight = height - OUTER_MARGIN * 2;

        if (bootOverlayProgress() > 0.0F) {
            renderBootSequence(guiGraphics, rootX, rootY, rootWidth, rootHeight);
            return;
        }

        RootLayout layout = rootLayout();
        WikiFrameRenderer.drawPanel(guiGraphics, layout.rootX(), layout.rootY(), layout.rootWidth(), layout.rootHeight(), StationScreenStyle.PANEL_BG);
        drawTopBar(guiGraphics, layout.rootX(), layout.rootY(), layout.rootWidth());

        Pane navPane = layout.navPane();
        Pane listPane = layout.listPane();
        Pane articlePane = layout.articlePane();
        boolean bankingTab = isBankingTab();
        boolean contactsTab = isContactsTab();
        boolean questsTab = isQuestsTab();
        if (!bankingTab) {
            setBankingFieldsVisible(false);
        }

        if (!bankingTab && !contactsTab && !questsTab) {
            drawPanelShell(guiGraphics, navPane, activeTab == null ? Component.translatable("screen.cyberneticenhancements.archive.topics") : activeTab.navigationTitle(), 0);
            if (!layout.listCollapsed()) {
                drawPanelShell(guiGraphics, listPane, selectedSection == null ? Component.translatable("screen.cyberneticenhancements.archive.entries") : selectedSection.title());
            }
            drawPanelShell(guiGraphics, articlePane, selectedEntry == null ? Component.translatable("screen.cyberneticenhancements.archive.entry") : selectedEntry.title(), selectedEntry == null ? 0 : PIN_BUTTON_WIDTH + 12);
            renderNavigation(guiGraphics, navPane, mouseX, mouseY);
            if (!layout.listCollapsed()) {
                renderEntryList(guiGraphics, listPane, mouseX, mouseY);
            }
            renderArticle(guiGraphics, articlePane);
            renderPinButton(guiGraphics, articlePane);
        } else {
            drawPanelShell(guiGraphics, articlePane,
                    bankingTab
                            ? Component.translatable("screen.cyberneticenhancements.archive.banking.title")
                            : questsTab
                            ? Component.translatable("screen.cyberneticenhancements.archive.quests.title")
                            : Component.translatable("screen.cyberneticenhancements.archive.contacts.title"),
                    0);
            if (bankingTab) {
                renderBankingRoster(guiGraphics, articlePane, mouseX, mouseY, partialTick);
            } else if (contactsTab) {
                renderContactsRoster(guiGraphics, articlePane);
            } else {
                renderQuestsRoster(guiGraphics, articlePane);
            }
        }

        if (!hoveredVisualStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredVisualStack, mouseX, mouseY);
        }

        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.close_hint"), layout.rootX() + layout.rootWidth() - 82, layout.rootY() + layout.rootHeight() - 14, StationScreenStyle.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (bootSequenceBlocksInput()) {
            return true;
        }

        RootLayout layout = rootLayout();
        boolean bankingTab = isBankingTab();
        boolean contactsTab = isContactsTab();
        boolean questsTab = isQuestsTab();
        if (bankingTab) {
            TextFieldFocusHelper.unfocusOnOutsideClick(this, mouseX, mouseY, List.of(bankTargetField, bankAmountField));
        }
        if (handleArchiveTabClick(mouseX, mouseY, layout.rootX(), layout.rootY(), layout.rootWidth())) {
            return true;
        }

        if (isInside(mouseX, mouseY, closeButtonBounds(layout.rootX(), layout.rootY(), layout.rootWidth()))) {
            playClick();
            onClose();
            return true;
        }

        if (!bankingTab && !contactsTab && !questsTab) {
            if (tryStartScrollbarDrag(mouseX, mouseY, layout.navPane(), navScroll, maxNavScroll(), DragTarget.NAV)) {
                return true;
            }
            if (!layout.listCollapsed() && tryStartScrollbarDrag(mouseX, mouseY, layout.listPane(), listScroll, maxListScroll(), DragTarget.LIST)) {
                return true;
            }
            if (tryStartScrollbarDrag(mouseX, mouseY, layout.articlePane(), articleScroll, maxArticleScroll(layout.articlePane()), DragTarget.ARTICLE)) {
                return true;
            }
        } else if (tryStartScrollbarDrag(mouseX, mouseY, layout.articlePane(), articleScroll,
                bankingTab ? maxBankingScroll(layout.articlePane()) : contactsTab ? maxContactsScroll(layout.articlePane()) : maxQuestsScroll(layout.articlePane()), DragTarget.ARTICLE)) {
            return true;
        }

        if (!bankingTab && !contactsTab && !questsTab && handlePinButtonClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }

        if (!bankingTab && !contactsTab && !questsTab && handleNavigationClick(mouseX, mouseY, layout.navPane())) {
            return true;
        }
        if (!bankingTab && !contactsTab && !questsTab && !layout.listCollapsed() && handleEntryClick(mouseX, mouseY, layout.listPane())) {
            return true;
        }
        if (bankingTab && handleBankingClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }
        if (contactsTab && handleContactsClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }
        if (questsTab && handleQuestsClick(mouseX, mouseY, layout.articlePane())) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (bootSequenceBlocksInput()) {
            dragTarget = DragTarget.NONE;
            return true;
        }
        if (button == 0) {
            dragTarget = DragTarget.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (bootSequenceBlocksInput()) {
            return true;
        }
        if (button != 0 || dragTarget == DragTarget.NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
            case ARTICLE -> dragScrollbar(articleScroll, layout.articlePane(), mouseY, isBankingTab() ? maxBankingScroll(layout.articlePane()) : isContactsTab() ? maxContactsScroll(layout.articlePane()) : isQuestsTab() ? maxQuestsScroll(layout.articlePane()) : maxArticleScroll(layout.articlePane()));
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (bootSequenceBlocksInput()) {
            return true;
        }
        RootLayout layout = rootLayout();
        double amount = -scrollY * 24.0D;
        if (isBankingTab() || isContactsTab() || isQuestsTab()) {
            if (isInsidePane(mouseX, mouseY, layout.articlePane())) {
                articleScroll.add(amount, isBankingTab() ? maxBankingScroll(layout.articlePane()) : isContactsTab() ? maxContactsScroll(layout.articlePane()) : maxQuestsScroll(layout.articlePane()));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isBankingTab() && handleBankingFieldKeyPressed(keyCode, scanCode, modifiers)) {
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
        if (isBankingTab() && bankingFieldFocused()) {
            if (bankTargetField.charTyped(codePoint, modifiers)) {
                return true;
            }
            if (bankAmountField.charTyped(codePoint, modifiers)) {
                return true;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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

            boolean hovered = mouseX >= pane.x + 6 && mouseX <= pane.x + rowAreaWidth && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            boolean selected = row.topic().section() != null && selectedSection != null && selectedSection.id().equals(row.topic().section().id());
            float emphasis = animateToward(navEmphasis, row.topic().id(), selected ? 1.0F : hovered ? 0.55F : 0.0F, 0.22F);
            drawListRow(guiGraphics, pane.x + 6, rowY, rowAreaWidth - 4, ROW_HEIGHT - 2, emphasis, row.alpha());

            int textX = pane.x + 14 + row.depth() * 12;
            if (row.topic().hasChildren()) {
                guiGraphics.drawString(font, expandedTopics.contains(row.topic().id()) ? "v" : ">", textX, rowY + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, row.alpha()), false);
                textX += 12;
            }
            guiGraphics.drawString(font, row.topic().title(), textX, rowY + 6, withAlpha(selected ? StationScreenStyle.TEXT_PRIMARY : StationScreenStyle.TEXT_SECONDARY, row.alpha()), false);
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
        int rowOffsetX = Math.round((1.0F - listAnim) * 12.0F);
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
            float emphasis = animateToward(entryEmphasis, entry.id(), selected ? 1.0F : hovered ? 0.55F : 0.0F, 0.22F);

            drawListRow(guiGraphics, pane.x + 6 + rowOffsetX, rowY, rowAreaWidth - 4, ENTRY_ROW_HEIGHT - 2, emphasis, rowAlpha);
            StationSlotRenderer.drawStandardSlot(guiGraphics, 0, 0, pane.x + 12 + rowOffsetX, rowY + 5, StationScreenStyle.SLOT_BG);
            guiGraphics.renderItem(entry.icon(), pane.x + 12 + rowOffsetX, rowY + 5);
            guiGraphics.drawString(font, trimStyled(entry.title(), 118), pane.x + 34 + rowOffsetX, rowY + 5, withAlpha(StationScreenStyle.TEXT_PRIMARY, rowAlpha), false);
            guiGraphics.drawString(font, font.plainSubstrByWidth(entry.badge().getString(), 118), pane.x + 34 + rowOffsetX, rowY + 15, withAlpha(selected ? StationScreenStyle.ACCENT : StationScreenStyle.TEXT_SECONDARY, rowAlpha), false);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, listScroll, maxScroll, viewportHeight, contentY);
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
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.no_entry"), headerX, headerY, StationScreenStyle.TEXT_SECONDARY, false);
            return;
        }

        float contentAnim = easeOutCubic(animationProgress(contentChangedAt, CONTENT_ANIMATION_MS));
        int contentOffsetX = Math.round((1.0F - contentAnim) * 14.0F);
        int alphaMask = ((int) (contentAnim * 255.0F) << 24) | 0x00FFFFFF;

        double maxScroll = metrics.maxScroll();
        articleScroll.clamp(maxScroll);

        guiGraphics.enableScissor(pane.x + 4, viewportY, pane.x + pane.width - 8, viewportY + viewportHeight);
        int y = headerY - (int) Math.round(articleScroll.current);

        StationSlotRenderer.drawStandardSlot(guiGraphics, 0, 0, headerX + contentOffsetX, y + 2, StationScreenStyle.SLOT_ACTIVE);
        guiGraphics.renderItem(selectedEntry.icon(), headerX + contentOffsetX, y + 2);
        guiGraphics.drawString(font, selectedEntry.title(), headerX + 22 + contentOffsetX, y + 1, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, selectedEntry.badge(), headerX + 22 + contentOffsetX, y + 12, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        y += 26;

        for (var line : font.split(selectedEntry.subtitle(), contentWidth - 8)) {
            guiGraphics.drawString(font, line, contentX + contentOffsetX, y, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            y += 11;
        }

        y += 6;
        y = renderChips(guiGraphics, selectedEntry.chips(), contentX + contentOffsetX, y, contentWidth, alphaMask);
        y += 6;

        for (ArticleSection section : selectedEntry.sections()) {
            drawArticleSection(guiGraphics, section, contentX + contentOffsetX, y, contentWidth, alphaMask);
            y += sectionHeight(font, section, contentWidth) + SECTION_GAP;
        }

        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
    }

    private void renderContactsRoster(GuiGraphics guiGraphics, Pane pane) {
        int viewportY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = maxContactsScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int headerX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;

        float contentAnim = easeOutCubic(animationProgress(contentChangedAt, CONTENT_ANIMATION_MS));
        int contentOffsetX = Math.round((1.0F - contentAnim) * 14.0F);
        int alphaMask = ((int) (contentAnim * 255.0F) << 24) | 0x00FFFFFF;

        articleScroll.clamp(maxScroll);

        guiGraphics.enableScissor(pane.x + 4, viewportY, contentX + contentWidth + 4, viewportY + viewportHeight);
        int y = headerY - (int) Math.round(articleScroll.current);

        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.contacts.subtitle"), headerX + contentOffsetX, y, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        y += 18;

        if (!ArchiveContactsClientState.isLoaded()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.contacts.syncing"), headerX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        List<ArchiveContactsPayload.ContactEntry> allContacts = ArchiveContactsClientState.get().contacts();
        if (allContacts.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.contacts.empty"), headerX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        List<ContactFilter> filters = contactFilters(allContacts);
        y = renderContactFilterTabs(guiGraphics, filters, contentX + contentOffsetX, y, contentWidth, alphaMask);

        List<ArchiveContactsPayload.ContactEntry> contacts = filteredContacts(allContacts);
        if (contacts.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.contacts.empty_filtered"), headerX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        for (ArchiveContactsPayload.ContactEntry contact : contacts) {
            int cardHeight = contactCardHeight();
            drawContactCard(guiGraphics, contact, contentX + contentOffsetX, y, contentWidth, cardHeight, alphaMask);
            y += cardHeight + SECTION_GAP;
        }

        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
    }

    private void renderQuestsRoster(GuiGraphics guiGraphics, Pane pane) {
        int viewportY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = maxQuestsScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int headerX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;

        float contentAnim = easeOutCubic(animationProgress(contentChangedAt, CONTENT_ANIMATION_MS));
        int contentOffsetX = Math.round((1.0F - contentAnim) * 14.0F);
        int alphaMask = ((int) (contentAnim * 255.0F) << 24) | 0x00FFFFFF;

        articleScroll.clamp(maxScroll);
        guiGraphics.enableScissor(pane.x + 4, viewportY, contentX + contentWidth + 4, viewportY + viewportHeight);
        int y = headerY - (int) Math.round(articleScroll.current);

        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.quests.subtitle"), headerX + contentOffsetX, y, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        y += 18;

        if (!ArchiveQuestsClientState.isLoaded()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.quests.syncing"), headerX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        List<ArchiveQuestsPayload.QuestEntry> quests = ArchiveQuestsClientState.get().quests();
        if (quests.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.quests.empty"), headerX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        String currentGroup = "";
        for (ArchiveQuestsPayload.QuestEntry quest : quests) {
            String nextGroup = questGroupLabelKey(quest.sortGroup());
            if (!nextGroup.equals(currentGroup)) {
                currentGroup = nextGroup;
                guiGraphics.drawString(font, Component.translatable(nextGroup), contentX + contentOffsetX, y, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
                y += 14;
            }
            int cardHeight = questCardHeight(quest, contentWidth);
            drawQuestCard(guiGraphics, quest, contentX + contentOffsetX, y, contentWidth, cardHeight, alphaMask);
            y += cardHeight + SECTION_GAP;
        }

        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
    }

    private void renderBankingRoster(GuiGraphics guiGraphics, Pane pane, int mouseX, int mouseY, float partialTick) {
        int viewportY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = maxBankingScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;

        float contentAnim = easeOutCubic(animationProgress(contentChangedAt, CONTENT_ANIMATION_MS));
        int contentOffsetX = Math.round((1.0F - contentAnim) * 14.0F);
        int alphaMask = ((int) (contentAnim * 255.0F) << 24) | 0x00FFFFFF;

        articleScroll.clamp(maxScroll);
        guiGraphics.enableScissor(pane.x + 4, viewportY, contentX + contentWidth + 4, viewportY + viewportHeight);
        int y = headerY - (int) Math.round(articleScroll.current);

        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.subtitle"), contentX + contentOffsetX, y, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        y += 18;

        if (!ArchiveBankingClientState.isLoaded()) {
            setBankingFieldsVisible(false);
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.syncing"), contentX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            guiGraphics.disableScissor();
            drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
            return;
        }

        ArchiveBankingPayload banking = ArchiveBankingClientState.get();
        drawBankingSummary(guiGraphics, banking, contentX + contentOffsetX, y, contentWidth, alphaMask);
        y += 50;

        drawBankingGraph(guiGraphics, banking.graphNet(), contentX + contentOffsetX, y, contentWidth, alphaMask);
        y += 70;

        updateBankingFieldLayout(contentX + contentOffsetX, y, contentWidth);
        drawBankingActionPanel(guiGraphics, banking, contentX + contentOffsetX, y, contentWidth, alphaMask, mouseX, mouseY, partialTick);
        y += bankingActionPanelHeight() + 6;

        y = drawBankingRequestsPanel(guiGraphics, banking.requests(), contentX + contentOffsetX, y, contentWidth, alphaMask);

        List<BankFilter> filters = bankFilters();
        y = renderBankFilterTabs(guiGraphics, filters, contentX + contentOffsetX, y, contentWidth, alphaMask);

        List<ArchiveBankingPayload.LedgerEntry> entries = filteredBankEntries(banking.entries());
        if (entries.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.entries.empty"), contentX + contentOffsetX, y + 6, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        } else {
            for (ArchiveBankingPayload.LedgerEntry entry : entries) {
                int rowHeight = bankEntryRowHeight(entry, contentWidth);
                drawBankEntryRow(guiGraphics, entry, contentX + contentOffsetX, y, contentWidth, rowHeight, alphaMask);
                y += rowHeight + 4;
            }
        }

        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics, pane, articleScroll, maxScroll, viewportHeight, viewportY);
    }

    private void drawBankingSummary(GuiGraphics guiGraphics, ArchiveBankingPayload banking, int x, int y, int width, int alphaMask) {
        int chipWidth = Math.max(96, (width - 12) / 4);
        drawMiniChip(guiGraphics, x, y, chipWidth, CHIP_HEIGHT, Component.translatable("screen.cyberneticenhancements.archive.banking.balance", banking.balance()), alphaMask);
        drawMiniChip(guiGraphics, x + chipWidth + 4, y, chipWidth, CHIP_HEIGHT, Component.translatable("screen.cyberneticenhancements.archive.banking.income", banking.totalIncome()), alphaMask);
        drawMiniChip(guiGraphics, x + (chipWidth + 4) * 2, y, chipWidth, CHIP_HEIGHT, Component.translatable("screen.cyberneticenhancements.archive.banking.expense", banking.totalExpense()), alphaMask);
        drawMiniChip(guiGraphics, x + (chipWidth + 4) * 3, y, chipWidth, CHIP_HEIGHT, Component.translatable("screen.cyberneticenhancements.archive.banking.net", banking.net()), alphaMask);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.summary"), x, y + 26, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
    }

    private void drawBankingGraph(GuiGraphics guiGraphics, List<Integer> points, int x, int y, int width, int alphaMask) {
        int height = 60;
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.graph"), x + 6, y + 6, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);

        int graphX = x + 8;
        int graphY = y + 24;
        int graphWidth = width - 16;
        int graphHeight = 28;
        guiGraphics.fill(graphX, graphY + graphHeight / 2, graphX + graphWidth, graphY + graphHeight / 2 + 1, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));

        int maxAbs = 1;
        for (int point : points) {
            maxAbs = Math.max(maxAbs, Math.abs(point));
        }
        int barGap = 3;
        int barWidth = Math.max(6, (graphWidth - Math.max(0, points.size() - 1) * barGap) / Math.max(1, points.size()));
        for (int index = 0; index < points.size(); index++) {
            int value = points.get(index);
            int barX = graphX + index * (barWidth + barGap);
            int magnitude = Math.max(1, Math.round((Math.abs(value) / (float) maxAbs) * (graphHeight / 2f - 2)));
            if (value >= 0) {
                guiGraphics.fill(barX, graphY + graphHeight / 2 - magnitude, barX + barWidth, graphY + graphHeight / 2, withAlpha(StationScreenStyle.ACCENT, alphaMask));
            } else {
                guiGraphics.fill(barX, graphY + graphHeight / 2 + 1, barX + barWidth, graphY + graphHeight / 2 + 1 + magnitude, withAlpha(0xFFCF5C5C, alphaMask));
            }
        }
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.graph.caption"), x + 6, y + 48, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
    }

    private void drawBankingActionPanel(GuiGraphics guiGraphics, ArchiveBankingPayload banking, int x, int y, int width, int alphaMask, int mouseX, int mouseY, float partialTick) {
        int panelHeight = bankingActionPanelHeight();
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, panelHeight, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.actions"), x + 6, y + 6, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.target"), bankTargetField.getX(), y + 24, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.amount"), bankAmountField.getX(), y + 24, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        setBankingFieldsVisible(true);
        bankTargetField.render(guiGraphics, mouseX, mouseY, partialTick);
        bankAmountField.render(guiGraphics, mouseX, mouseY, partialTick);
        drawBankPlayerDropdownToggle(guiGraphics, x, y, width, alphaMask);
        drawContactActionButton(guiGraphics, bankSendButtonBounds(x, y, width), Component.translatable("screen.cyberneticenhancements.archive.banking.button.send"), alphaMask);
        drawContactActionButton(guiGraphics, bankRequestButtonBounds(x, y, width), Component.translatable("screen.cyberneticenhancements.archive.banking.button.request"), alphaMask);
        if (bankPlayerDropdownOpen) {
            drawBankPlayerDropdown(guiGraphics, x, y, width, alphaMask);
        }
    }

    private int drawBankingRequestsPanel(GuiGraphics guiGraphics, List<ArchiveBankingPayload.RequestEntry> requests, int x, int y, int width, int alphaMask) {
        int panelHeight = bankingRequestsPanelHeight(requests, width);
        int innerX = x + 6;
        int innerY = y + 24;
        int innerWidth = width - 12;
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, panelHeight, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.requests"), x + 6, y + 6, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        if (requests.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.requests.empty"), innerX, innerY + 2, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        } else {
            for (ArchiveBankingPayload.RequestEntry request : requests) {
                int cardHeight = bankRequestCardHeight(request);
                drawBankRequestCard(guiGraphics, request, innerX, innerY, innerWidth, cardHeight, alphaMask);
                innerY += cardHeight + SECTION_GAP;
            }
        }
        return y + panelHeight + 8;
    }

    private void drawBankPlayerDropdownToggle(GuiGraphics guiGraphics, int x, int y, int width, int alphaMask) {
        Rect bounds = bankPlayerDropdownToggleBounds(x, y, width);
        int fill = bankPlayerDropdownOpen ? blendColors(StationScreenStyle.PANEL_ALT, StationScreenStyle.SLOT_ACTIVE, 0.78F) : StationScreenStyle.SECTION_SHADOW;
        int accent = bankPlayerDropdownOpen ? StationScreenStyle.ACCENT : StationScreenStyle.FRAME_HIGHLIGHT_SOFT;
        WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(fill, alphaMask), withAlpha(accent, alphaMask));
        String arrow = bankPlayerDropdownOpen ? "^" : "v";
        guiGraphics.drawString(font, arrow, bounds.x() + (bounds.width() - font.width(arrow)) / 2, bounds.y() + 5, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
    }

    private void drawBankPlayerDropdown(GuiGraphics guiGraphics, int x, int y, int width, int alphaMask) {
        List<PlayerInfo> players = onlineBankPlayers();
        Rect bounds = bankPlayerDropdownBounds(x, y, width, players.size());
        WikiFrameRenderer.drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        if (players.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.banking.players.none"), bounds.x() + 8, bounds.y() + 8, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            return;
        }
        int rowY = bounds.y() + 4;
        for (int index = 0; index < players.size(); index++) {
            PlayerInfo player = players.get(index);
            Rect row = bankPlayerDropdownRowBounds(x, y, width, index);
            int rowFill = index % 2 == 0 ? StationScreenStyle.PANEL_ALT : StationScreenStyle.SECTION_SHADOW;
            guiGraphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + row.height(), withAlpha(rowFill, alphaMask));
            PlayerFaceRenderer.draw(guiGraphics, player.getSkin().texture(), row.x() + 4, row.y() + 3, 12);
            guiGraphics.drawString(font, trimStyled(Component.literal(player.getProfile().getName()), row.width() - 24), row.x() + 20, row.y() + 5, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
            rowY += row.height();
        }
    }

    private int renderBankFilterTabs(GuiGraphics guiGraphics, List<BankFilter> filters, int startX, int startY, int width, int alphaMask) {
        for (Rect bounds : bankFilterTabBounds(filters, startX, startY, width)) {
            boolean active = bounds.id().equals(activeBankFilterId);
            int fill = active ? blendColors(StationScreenStyle.PANEL_ALT, StationScreenStyle.SLOT_ACTIVE, 0.78F) : StationScreenStyle.SECTION_SHADOW;
            int accent = active ? StationScreenStyle.ACCENT : StationScreenStyle.FRAME_HIGHLIGHT_SOFT;
            WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(fill, alphaMask), withAlpha(accent, alphaMask));
            FormattedCharSequence label = trimStyled(bankFilterLabel(bounds.id(), filters), bounds.width() - 10);
            guiGraphics.drawString(font, label, bounds.x() + (bounds.width() - font.width(label)) / 2, bounds.y() + 5, withAlpha(active ? StationScreenStyle.TEXT_PRIMARY : StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        }
        return startY + bankFilterTabsHeight(filters, width) + 4;
    }

    private void drawBankEntryRow(GuiGraphics guiGraphics, ArchiveBankingPayload.LedgerEntry entry, int x, int y, int width, int height, int alphaMask) {
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));

        String amount = (entry.amount() >= 0 ? "+" : "") + entry.amount() + " €$";
        int amountColor = entry.amount() >= 0 ? StationScreenStyle.ACCENT : 0xFFCF5C5C;
        guiGraphics.drawString(font, trimStyled(Component.literal(entry.note()), width - 130), x + 8, y + 6, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, Component.literal(amount), x + width - font.width(amount) - 8, y + 6, withAlpha(amountColor, alphaMask), false);

        int lineY = y + 26;
        Component counterpart = entry.counterpartyName().isBlank()
                ? Component.translatable("screen.cyberneticenhancements.archive.banking.counterparty.system")
                : Component.translatable("screen.cyberneticenhancements.archive.banking.counterparty.named", entry.counterpartyName());
        drawWrappedQuestLine(guiGraphics, counterpart, x + 8, lineY, width - 16, alphaMask);
        lineY += wrappedQuestLineHeight(counterpart, width - 16);
        Component type = Component.translatable("screen.cyberneticenhancements.archive.banking.entry.type", humanizeId(entry.typeId()));
        drawWrappedQuestLine(guiGraphics, type, x + 8, lineY, width - 16, alphaMask);
        lineY += wrappedQuestLineHeight(type, width - 16);
        Component day = Component.translatable("screen.cyberneticenhancements.archive.banking.entry.day", entry.day());
        drawWrappedQuestLine(guiGraphics, day, x + 8, lineY, width - 16, alphaMask);
    }

    private void drawBankRequestCard(GuiGraphics guiGraphics, ArchiveBankingPayload.RequestEntry request, int x, int y, int width, int height, int alphaMask) {
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 20, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 20, x + width - 2, y + 21, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));

        Component title = request.incoming()
                ? Component.translatable("screen.cyberneticenhancements.archive.banking.request.incoming", request.fromName())
                : Component.translatable("screen.cyberneticenhancements.archive.banking.request.outgoing", request.toName());
        guiGraphics.drawString(font, trimStyled(title, width - 160), x + 8, y + 6, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, Component.literal(request.amount() + " €$"), x + width - 72, y + 6, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);

        int lineY = y + 28;
        drawWrappedQuestLine(guiGraphics, Component.translatable("screen.cyberneticenhancements.archive.banking.entry.day", request.createdDay()), x + 8, lineY, width - 16, alphaMask);
        if (request.incoming()) {
            drawContactActionButton(guiGraphics, bankApproveRequestBounds(request, x, y, width), Component.translatable("screen.cyberneticenhancements.archive.banking.button.approve"), alphaMask);
            drawContactActionButton(guiGraphics, bankDeclineRequestBounds(request, x, y, width), Component.translatable("screen.cyberneticenhancements.archive.banking.button.decline"), alphaMask);
        } else {
            drawContactActionButton(guiGraphics, bankDeclineRequestBounds(request, x, y, width), Component.translatable("screen.cyberneticenhancements.archive.banking.button.cancel"), alphaMask);
        }
    }

    private boolean handleBankingClick(double mouseX, double mouseY, Pane pane) {
        if (!ArchiveBankingClientState.isLoaded()) {
            return false;
        }
        double maxScroll = maxBankingScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int y = pane.y + PANEL_HEADER_HEIGHT + 8 - (int) Math.round(articleScroll.current);
        y += 18 + 50 + 70;

        Rect dropdownToggle = bankPlayerDropdownToggleBounds(contentX, y, contentWidth);
        if (isInside(mouseX, mouseY, dropdownToggle)) {
            bankPlayerDropdownOpen = !bankPlayerDropdownOpen;
            playClick();
            return true;
        }
        if (bankPlayerDropdownOpen) {
            List<PlayerInfo> players = onlineBankPlayers();
            for (int index = 0; index < players.size(); index++) {
                Rect row = bankPlayerDropdownRowBounds(contentX, y, contentWidth, index);
                if (isInside(mouseX, mouseY, row)) {
                    bankTargetField.setValue(players.get(index).getProfile().getName());
                    bankPlayerDropdownOpen = false;
                    clearBankingFieldFocus();
                    playClick();
                    return true;
                }
            }
        }

        Rect send = bankSendButtonBounds(contentX, y, contentWidth);
        if (isInside(mouseX, mouseY, send)) {
            PacketDistributor.sendToServer(new ArchiveBankingActionPayload(
                    ArchiveBankingActionPayload.ACTION_SEND,
                    bankTargetField.getValue(),
                    parseBankAmountField(),
                    ""
            ));
            bankPlayerDropdownOpen = false;
            playClick();
            return true;
        }
        Rect request = bankRequestButtonBounds(contentX, y, contentWidth);
        if (isInside(mouseX, mouseY, request)) {
            PacketDistributor.sendToServer(new ArchiveBankingActionPayload(
                    ArchiveBankingActionPayload.ACTION_REQUEST,
                    bankTargetField.getValue(),
                    parseBankAmountField(),
                    ""
            ));
            bankPlayerDropdownOpen = false;
            playClick();
            return true;
        }

        List<BankFilter> filters = bankFilters();
        y += bankingActionPanelHeight() + 6;

        ArchiveBankingPayload banking = ArchiveBankingClientState.get();
        int requestsPanelHeight = bankingRequestsPanelHeight(banking.requests(), contentWidth);
        int requestsInnerX = contentX + 6;
        int requestsInnerY = y + 24;
        int requestsInnerWidth = contentWidth - 12;
        for (ArchiveBankingPayload.RequestEntry requestEntry : banking.requests()) {
            if (requestEntry.incoming()) {
                Rect approve = bankApproveRequestBounds(requestEntry, requestsInnerX, requestsInnerY, requestsInnerWidth);
                if (isInside(mouseX, mouseY, approve)) {
                    PacketDistributor.sendToServer(new ArchiveBankingActionPayload(ArchiveBankingActionPayload.ACTION_APPROVE, "", 0, requestEntry.requestId()));
                    playClick();
                    return true;
                }
            }
            Rect decline = bankDeclineRequestBounds(requestEntry, requestsInnerX, requestsInnerY, requestsInnerWidth);
            if (isInside(mouseX, mouseY, decline)) {
                PacketDistributor.sendToServer(new ArchiveBankingActionPayload(ArchiveBankingActionPayload.ACTION_DECLINE, "", 0, requestEntry.requestId()));
                playClick();
                return true;
            }
            requestsInnerY += bankRequestCardHeight(requestEntry) + SECTION_GAP;
        }

        y += requestsPanelHeight + 8;

        for (Rect bounds : bankFilterTabBounds(filters, contentX, y, contentWidth)) {
            if (isInside(mouseX, mouseY, bounds)) {
                setBankFilter(bounds.id());
                playClick();
                return true;
            }
        }

        y += bankFilterTabsHeight(filters, contentWidth) + 4;
        bankPlayerDropdownOpen = false;
        return false;
    }

    private void updateBankingFieldLayout(int x, int y, int width) {
        int fieldY = y + 24;
        int buttonsWidth = 144;
        int dropdownWidth = 18;
        int amountWidth = Math.max(96, font.width(Component.translatable("screen.cyberneticenhancements.archive.banking.amount")) + 18);
        int gap = 8;
        int targetWidth = Math.max(92, width - amountWidth - dropdownWidth - buttonsWidth - gap * 4 - 8);
        bankTargetField.setX(x + 6);
        bankTargetField.setY(fieldY);
        bankTargetField.setWidth(targetWidth);
        bankTargetField.setHeight(18);
        bankAmountField.setX(bankTargetField.getX() + targetWidth + gap + dropdownWidth + gap);
        bankAmountField.setY(fieldY);
        bankAmountField.setWidth(amountWidth);
        bankAmountField.setHeight(18);
    }

    private void setBankingFieldsVisible(boolean visible) {
        if (bankTargetField != null) {
            bankTargetField.setVisible(visible);
        }
        if (bankAmountField != null) {
            bankAmountField.setVisible(visible);
        }
    }

    private boolean handleBankingFieldKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!bankingFieldFocused()) {
            return false;
        }
        if (keyCode == 257 || keyCode == 335 || keyCode == 256) {
            clearBankingFieldFocus();
            return true;
        }
        if (bankTargetField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (bankAmountField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return true;
    }

    private boolean bankingFieldFocused() {
        return (bankTargetField != null && bankTargetField.isVisible() && bankTargetField.isFocused())
                || (bankAmountField != null && bankAmountField.isVisible() && bankAmountField.isFocused());
    }

    private void clearBankingFieldFocus() {
        TextFieldFocusHelper.clearFocus(this, List.of(bankTargetField, bankAmountField));
    }

    private int parseBankAmountField() {
        if (bankAmountField == null) {
            return 0;
        }
        try {
            return Integer.parseInt(bankAmountField.getValue().trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private List<BankFilter> bankFilters() {
        return List.of(
                new BankFilter(BANK_FILTER_ALL, Component.translatable("screen.cyberneticenhancements.archive.banking.filter.all")),
                new BankFilter(BANK_FILTER_INCOME, Component.translatable("screen.cyberneticenhancements.archive.banking.filter.income")),
                new BankFilter(BANK_FILTER_EXPENSE, Component.translatable("screen.cyberneticenhancements.archive.banking.filter.expense"))
        );
    }

    private List<ArchiveBankingPayload.LedgerEntry> filteredBankEntries(List<ArchiveBankingPayload.LedgerEntry> entries) {
        List<ArchiveBankingPayload.LedgerEntry> filtered = new ArrayList<>();
        for (ArchiveBankingPayload.LedgerEntry entry : entries) {
            if (BANK_FILTER_INCOME.equals(activeBankFilterId) && entry.amount() <= 0) {
                continue;
            }
            if (BANK_FILTER_EXPENSE.equals(activeBankFilterId) && entry.amount() >= 0) {
                continue;
            }
            filtered.add(entry);
        }
        return filtered;
    }

    private List<Rect> bankFilterTabBounds(List<BankFilter> filters, int startX, int startY, int availableWidth) {
        List<Rect> bounds = new ArrayList<>(filters.size());
        int x = startX;
        for (BankFilter filter : filters) {
            int tabWidth = Math.max(68, font.width(filter.label()) + 18);
            if (x > startX && x + tabWidth > startX + availableWidth) {
                x = startX;
                startY += BANK_FILTER_TAB_HEIGHT + BANK_FILTER_TAB_GAP;
            }
            bounds.add(new Rect(filter.id(), x, startY, tabWidth, BANK_FILTER_TAB_HEIGHT));
            x += tabWidth + BANK_FILTER_TAB_GAP;
        }
        return bounds;
    }

    private int bankFilterTabsHeight(List<BankFilter> filters, int availableWidth) {
        if (filters.isEmpty()) {
            return 0;
        }
        int rows = 1;
        int rowWidth = 0;
        for (BankFilter filter : filters) {
            int tabWidth = Math.max(68, font.width(filter.label()) + 18);
            if (rowWidth > 0 && rowWidth + BANK_FILTER_TAB_GAP + tabWidth > availableWidth) {
                rows++;
                rowWidth = tabWidth;
            } else {
                rowWidth = rowWidth == 0 ? tabWidth : rowWidth + BANK_FILTER_TAB_GAP + tabWidth;
            }
        }
        return rows * BANK_FILTER_TAB_HEIGHT + Math.max(0, rows - 1) * BANK_FILTER_TAB_GAP;
    }

    private Component bankFilterLabel(String id, List<BankFilter> filters) {
        for (BankFilter filter : filters) {
            if (filter.id().equals(id)) {
                return filter.label();
            }
        }
        return Component.literal(id);
    }

    private void setBankFilter(String filterId) {
        if (filterId == null || filterId.equals(activeBankFilterId)) {
            return;
        }
        activeBankFilterId = filterId;
        articleScroll.reset();
        contentChangedAt = animationAnchorTime();
    }

    private int bankEntryRowHeight(ArchiveBankingPayload.LedgerEntry entry, int width) {
        int bodyWidth = width - 16;
        int height = 24;
        height += wrappedQuestLineHeight(
                entry.counterpartyName().isBlank()
                        ? Component.translatable("screen.cyberneticenhancements.archive.banking.counterparty.system")
                        : Component.translatable("screen.cyberneticenhancements.archive.banking.counterparty.named", entry.counterpartyName()),
                bodyWidth);
        height += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.banking.entry.type", humanizeId(entry.typeId())), bodyWidth);
        height += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.banking.entry.day", entry.day()), bodyWidth);
        return height + 8;
    }

    private int bankRequestCardHeight(ArchiveBankingPayload.RequestEntry request) {
        return request.incoming() ? 58 : 50;
    }

    private int bankingActionPanelHeight() {
        return 52 + bankPlayerDropdownHeight();
    }

    private int bankPlayerDropdownHeight() {
        if (!bankPlayerDropdownOpen) {
            return 0;
        }
        int rows = Math.max(1, onlineBankPlayers().size());
        return 4 + rows * 18 + 4;
    }

    private int bankingRequestsPanelHeight(List<ArchiveBankingPayload.RequestEntry> requests, int width) {
        int height = 34;
        if (requests.isEmpty()) {
            return height + 18;
        }
        for (ArchiveBankingPayload.RequestEntry request : requests) {
            height += bankRequestCardHeight(request) + SECTION_GAP;
        }
        return height + 6;
    }

    private List<PlayerInfo> onlineBankPlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return List.of();
        }
        List<PlayerInfo> players = new ArrayList<>();
        String selfName = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            if (info.getProfile().getName().equalsIgnoreCase(selfName)) {
                continue;
            }
            players.add(info);
        }
        players.sort((left, right) -> left.getProfile().getName().compareToIgnoreCase(right.getProfile().getName()));
        return players;
    }

    private Rect bankSendButtonBounds(int x, int y, int width) {
        int rightX = x + width - 142;
        return new Rect("bank_send", rightX, y + 24, 64, 18);
    }

    private Rect bankRequestButtonBounds(int x, int y, int width) {
        int rightX = x + width - 74;
        return new Rect("bank_request", rightX, y + 24, 64, 18);
    }

    private Rect bankPlayerDropdownToggleBounds(int x, int y, int width) {
        return new Rect("bank_dropdown", bankTargetField.getX() + bankTargetField.getWidth() + 8, y + 24, 18, 18);
    }

    private Rect bankPlayerDropdownBounds(int x, int y, int width, int playerCount) {
        int rows = Math.max(1, playerCount);
        return new Rect("bank_dropdown_rows", bankTargetField.getX(), y + 44, bankTargetField.getWidth() + 26, 8 + rows * 18);
    }

    private Rect bankPlayerDropdownRowBounds(int x, int y, int width, int index) {
        Rect dropdown = bankPlayerDropdownBounds(x, y, width, onlineBankPlayers().size());
        return new Rect("bank_dropdown_row_" + index, dropdown.x() + 4, dropdown.y() + 4 + index * 18, dropdown.width() - 8, 18);
    }

    private Rect bankApproveRequestBounds(ArchiveBankingPayload.RequestEntry request, int x, int y, int width) {
        return new Rect(request.requestId() + ":approve", x + width - 142, y + 28, 64, 18);
    }

    private Rect bankDeclineRequestBounds(ArchiveBankingPayload.RequestEntry request, int x, int y, int width) {
        return new Rect(request.requestId() + ":decline", x + width - 74, y + 28, 64, 18);
    }

    private void drawQuestCard(GuiGraphics guiGraphics, ArchiveQuestsPayload.QuestEntry quest, int x, int y, int width, int height, int alphaMask) {
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 20, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 20, x + width - 2, y + 21, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));

        guiGraphics.drawString(font, trimStyled(Component.literal(quest.title()), width - 20), x + 8, y + 6, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        Component issuer = Component.translatable("screen.cyberneticenhancements.archive.quests.issuer", quest.issuerName());
        int bodyWidth = width - 16;
        Rect discardBounds = questCanBeDiscarded(quest) ? discardQuestButtonBounds(quest, x, y, width) : null;
        int issuerWidth = discardBounds == null ? bodyWidth : discardBounds.x() - (x + 8) - 8;
        guiGraphics.drawString(font, trimStyled(issuer, issuerWidth), x + 8, y + 28, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        if (discardBounds != null) {
            drawQuestActionButton(guiGraphics, discardBounds, Component.translatable("screen.cyberneticenhancements.archive.quests.button.discard"), alphaMask);
        }

        int lineY = y + 42;
        Component objective = Component.translatable("screen.cyberneticenhancements.archive.quests.objective", quest.objective());
        drawWrappedQuestLine(guiGraphics, objective, x + 8, lineY, bodyWidth, alphaMask);
        lineY += wrappedQuestLineHeight(objective, bodyWidth);
        Component reward = Component.translatable("screen.cyberneticenhancements.archive.quests.reward", quest.reward());
        drawWrappedQuestLine(guiGraphics, reward, x + 8, lineY, bodyWidth, alphaMask);
        lineY += wrappedQuestLineHeight(reward, bodyWidth);
        if (quest.hasLocation()) {
            Component location = Component.translatable("screen.cyberneticenhancements.archive.quests.location", formatDimension(quest.dimensionId()), quest.x(), quest.y(), quest.z());
            drawWrappedQuestLine(guiGraphics, location, x + 8, lineY, bodyWidth, alphaMask);
            lineY += wrappedQuestLineHeight(location, bodyWidth);
        }
        Component status = Component.translatable("screen.cyberneticenhancements.archive.quests.status", quest.statusLabel());
        drawWrappedQuestLine(guiGraphics, status, x + 8, lineY, bodyWidth, alphaMask);
    }

    private void drawWrappedQuestLine(GuiGraphics guiGraphics, Component line, int x, int y, int width, int alphaMask) {
        int lineOffset = 0;
        for (FormattedCharSequence wrapped : font.split(line, width)) {
            guiGraphics.drawString(font, wrapped, x, y + lineOffset, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            lineOffset += 11;
        }
    }

    private int wrappedQuestLineHeight(Component line, int width) {
        return font.split(line, width).size() * 11 + 2;
    }

    private void drawQuestActionButton(GuiGraphics guiGraphics, Rect bounds, Component label, int alphaMask) {
        WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask), withAlpha(StationScreenStyle.ACCENT, alphaMask));
        FormattedCharSequence trimmed = trimStyled(label, bounds.width() - 8);
        guiGraphics.drawString(font, trimmed, bounds.x() + (bounds.width() - font.width(trimmed)) / 2, bounds.y() + 5, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
    }

    private void drawContactCard(GuiGraphics guiGraphics, ArchiveContactsPayload.ContactEntry contact, int x, int y, int width, int height, int alphaMask) {
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 20, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 20, x + width - 2, y + 21, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));

        int rightColumnX = x + width - 154;
        int headSize = 44;
        int headX = x + 10;
        int headY = y + 30;
        drawNpcHeadPreview(guiGraphics, contact, headX, headY, headSize, alphaMask);

        int headerTextX = x + 10;
        guiGraphics.drawString(font, trimStyled(Component.literal(contact.name()), rightColumnX - headerTextX - 10), headerTextX, y + 6, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        int textX = headX + headSize + 18;
        guiGraphics.drawString(font, contactTypeLabel(contact), textX, y + 32, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        guiGraphics.drawString(font, trustLabel(contact), textX, y + 43, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        guiGraphics.drawString(font, contactStateLabel(contact), textX, y + 54, withAlpha(contactStateColor(contact), alphaMask), false);
        guiGraphics.drawString(font, locationLabel(contact), textX, y + 65, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        guiGraphics.drawString(font, positionLabel(contact), textX, y + 76, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);

        drawContactActionButton(guiGraphics, pinButtonBounds(contact, x, y, width), Component.translatable(contact.pinned() ? "screen.cyberneticenhancements.archive.contacts.button.unpin" : "screen.cyberneticenhancements.archive.contacts.button.pin"), alphaMask);
        drawContactActionButton(guiGraphics, hideButtonBounds(contact, x, y, width), Component.translatable(contact.hidden() ? "screen.cyberneticenhancements.archive.contacts.button.unhide" : "screen.cyberneticenhancements.archive.contacts.button.hide"), alphaMask);
        drawContactActionButton(guiGraphics, locateButtonBounds(contact, x, y, width), Component.translatable("screen.cyberneticenhancements.archive.contacts.button.locate"), alphaMask);
        drawContactActionButton(guiGraphics, meetupButtonBounds(contact, x, y, width), Component.translatable("screen.cyberneticenhancements.archive.contacts.button.meetup"), alphaMask);
    }

    private void drawNpcHeadPreview(GuiGraphics guiGraphics, ArchiveContactsPayload.ContactEntry contact, int x, int y, int size, int alphaMask) {
        WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, x, y, size + 14, size + 14, withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask), withAlpha(StationScreenStyle.ACCENT, alphaMask));
        ResourceLocation texture = FixerEntityRenderer.resolveTexture(parseUuid(contact.npcId()), NpcCategory.fromId(contact.categoryId()));
        renderNpcHeadModel(guiGraphics, texture, x + 7, y + 7, size, alphaMask);
    }

    private void renderNpcHeadModel(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int renderSize, int alphaMask) {
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
        model.head.render(guiGraphics.pose(), buffer, 15728880, OverlayTexture.NO_OVERLAY, withAlpha(0xFFFFFFFF, alphaMask));
        model.hat.render(guiGraphics.pose(), buffer, 15728880, OverlayTexture.NO_OVERLAY, withAlpha(0xFFFFFFFF, alphaMask));
        guiGraphics.flush();
        Lighting.setupFor3DItems();
        guiGraphics.pose().popPose();
    }

    private int renderChips(GuiGraphics guiGraphics, List<Component> chips, int startX, int startY, int width, int alphaMask) {
        int x = startX;
        int y = startY;
        for (Component chip : chips) {
            int chipWidth = Math.min(width, font.width(chip) + 10);
            if (x + chipWidth > startX + width) {
                x = startX;
                y += CHIP_HEIGHT + 4;
            }
            drawMiniChip(guiGraphics, x, y, chipWidth, CHIP_HEIGHT, chip, alphaMask);
            x += chipWidth + 4;
        }
        return y + CHIP_HEIGHT;
    }

    private int measureContactsHeight(Pane pane) {
        int height = 18;
        if (!ArchiveContactsClientState.isLoaded()) {
            return height + 22;
        }
        List<ArchiveContactsPayload.ContactEntry> allContacts = ArchiveContactsClientState.get().contacts();
        if (allContacts.isEmpty()) {
            return height + 22;
        }
        height += contactFilterTabsHeight(contactFilters(allContacts), pane.width - 20 - SCROLLBAR_WIDTH) + 2;
        List<ArchiveContactsPayload.ContactEntry> contacts = filteredContacts(allContacts);
        if (contacts.isEmpty()) {
            return height + 22;
        }
        return height + contacts.size() * contactCardHeight() + Math.max(0, contacts.size() - 1) * SECTION_GAP;
    }

    private int measureBankingHeight(Pane pane) {
        int contentWidth = pane.width - 20 - SCROLLBAR_WIDTH;
        int height = 18 + 50 + 70 + bankingActionPanelHeight() + 6;
        height += ArchiveBankingClientState.isLoaded()
                ? bankingRequestsPanelHeight(ArchiveBankingClientState.get().requests(), contentWidth) + 8
                : bankingRequestsPanelHeight(List.of(), contentWidth) + 8;
        List<BankFilter> filters = bankFilters();
        height += bankFilterTabsHeight(filters, contentWidth) + 4;
        if (!ArchiveBankingClientState.isLoaded()) {
            return height + 22;
        }
        ArchiveBankingPayload banking = ArchiveBankingClientState.get();
        List<ArchiveBankingPayload.LedgerEntry> entries = filteredBankEntries(banking.entries());
        if (entries.isEmpty()) {
            return height + 22;
        }
        for (ArchiveBankingPayload.LedgerEntry entry : entries) {
            height += bankEntryRowHeight(entry, contentWidth) + 4;
        }
        return height;
    }

    private int measureQuestsHeight(Pane pane) {
        int height = 18;
        if (!ArchiveQuestsClientState.isLoaded()) {
            return height + 22;
        }
        List<ArchiveQuestsPayload.QuestEntry> quests = ArchiveQuestsClientState.get().quests();
        if (quests.isEmpty()) {
            return height + 22;
        }
        String currentGroup = "";
        int contentWidth = pane.width - 20 - SCROLLBAR_WIDTH;
        for (ArchiveQuestsPayload.QuestEntry quest : quests) {
            String nextGroup = questGroupLabelKey(quest.sortGroup());
            if (!nextGroup.equals(currentGroup)) {
                currentGroup = nextGroup;
                height += 14;
            }
            height += questCardHeight(quest, contentWidth) + SECTION_GAP;
        }
        return height;
    }

    private int questCardHeight(ArchiveQuestsPayload.QuestEntry quest, int width) {
        int innerWidth = width - 16;
        int contentHeight = 0;
        contentHeight += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.quests.objective", quest.objective()), innerWidth);
        contentHeight += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.quests.reward", quest.reward()), innerWidth);
        if (quest.hasLocation()) {
            contentHeight += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.quests.location", formatDimension(quest.dimensionId()), quest.x(), quest.y(), quest.z()), innerWidth);
        }
        contentHeight += wrappedQuestLineHeight(Component.translatable("screen.cyberneticenhancements.archive.quests.status", quest.statusLabel()), innerWidth);
        return 46 + contentHeight;
    }

    private boolean questCanBeDiscarded(ArchiveQuestsPayload.QuestEntry quest) {
        return "active".equals(quest.statusId()) || "ready".equals(quest.statusId());
    }

    private Rect discardQuestButtonBounds(ArchiveQuestsPayload.QuestEntry quest, int cardX, int cardY, int cardWidth) {
        return new Rect(
                quest.questId() + ":discard",
                cardX + cardWidth - QUEST_ACTION_BUTTON_WIDTH - 10,
                cardY + 24,
                QUEST_ACTION_BUTTON_WIDTH,
                18
        );
    }

    private boolean handleContactsClick(double mouseX, double mouseY, Pane pane) {
        if (!ArchiveContactsClientState.isLoaded()) {
            return false;
        }

        double maxScroll = maxContactsScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;
        int y = headerY - (int) Math.round(articleScroll.current) + 18;
        List<ArchiveContactsPayload.ContactEntry> allContacts = ArchiveContactsClientState.get().contacts();
        List<ContactFilter> filters = contactFilters(allContacts);

        for (Rect bounds : contactFilterTabBounds(filters, contentX, y, contentWidth)) {
            if (isInside(mouseX, mouseY, bounds)) {
                setContactsFilter(bounds.id());
                playClick();
                return true;
            }
        }

        y += contactFilterTabsHeight(filters, contentWidth) + 2;
        for (ArchiveContactsPayload.ContactEntry contact : filteredContacts(allContacts)) {
            Rect pin = pinButtonBounds(contact, contentX, y, contentWidth);
            if (isInside(mouseX, mouseY, pin)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(ArchiveContactActionPayload.ACTION_TOGGLE_PIN, contact.npcId()));
                playClick();
                return true;
            }
            Rect hide = hideButtonBounds(contact, contentX, y, contentWidth);
            if (isInside(mouseX, mouseY, hide)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(ArchiveContactActionPayload.ACTION_TOGGLE_HIDE, contact.npcId()));
                playClick();
                return true;
            }
            Rect locate = locateButtonBounds(contact, contentX, y, contentWidth);
            if (isInside(mouseX, mouseY, locate)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(ArchiveContactActionPayload.ACTION_LOCATE, contact.npcId()));
                playClick();
                onClose();
                return true;
            }
            Rect meetup = meetupButtonBounds(contact, contentX, y, contentWidth);
            if (isInside(mouseX, mouseY, meetup)) {
                PacketDistributor.sendToServer(new ArchiveContactActionPayload(ArchiveContactActionPayload.ACTION_MEETUP, contact.npcId()));
                playClick();
                onClose();
                return true;
            }
            y += contactCardHeight() + SECTION_GAP;
        }
        return false;
    }

    private boolean handleQuestsClick(double mouseX, double mouseY, Pane pane) {
        if (!ArchiveQuestsClientState.isLoaded()) {
            return false;
        }
        int viewportY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        double maxScroll = maxQuestsScroll(pane);
        int contentWidth = pane.width - 20 - (maxScroll > 0.0D ? SCROLLBAR_WIDTH : 0);
        int contentX = pane.x + 10;
        int headerY = pane.y + PANEL_HEADER_HEIGHT + 8;
        int y = headerY - (int) Math.round(articleScroll.current);

        y += 18;
        List<ArchiveQuestsPayload.QuestEntry> quests = ArchiveQuestsClientState.get().quests();
        String currentGroup = "";
        for (ArchiveQuestsPayload.QuestEntry quest : quests) {
            String nextGroup = questGroupLabelKey(quest.sortGroup());
            if (!nextGroup.equals(currentGroup)) {
                currentGroup = nextGroup;
                y += 14;
            }
            int cardHeight = questCardHeight(quest, contentWidth);
            if (questCanBeDiscarded(quest)) {
                Rect discard = discardQuestButtonBounds(quest, contentX, y, contentWidth);
                if (isInside(mouseX, mouseY, discard)) {
                    PacketDistributor.sendToServer(new ArchiveQuestActionPayload(ArchiveQuestActionPayload.ACTION_DISCARD, quest.questId()));
                    playClick();
                    return true;
                }
            }
            y += cardHeight + SECTION_GAP;
        }
        return false;
    }

    private int contactCardHeight() {
        return 113;
    }

    private int renderContactFilterTabs(GuiGraphics guiGraphics, List<ContactFilter> filters, int startX, int startY, int width, int alphaMask) {
        for (Rect bounds : contactFilterTabBounds(filters, startX, startY, width)) {
            boolean active = bounds.id().equals(activeContactsFilterId);
            int fill = active ? blendColors(StationScreenStyle.PANEL_ALT, StationScreenStyle.SLOT_ACTIVE, 0.78F) : StationScreenStyle.SECTION_SHADOW;
            int accent = active ? StationScreenStyle.ACCENT : StationScreenStyle.FRAME_HIGHLIGHT_SOFT;
            WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(fill, alphaMask), withAlpha(accent, alphaMask));
            FormattedCharSequence label = trimStyled(contactFilterLabel(bounds.id(), filters), bounds.width() - 10);
            guiGraphics.drawString(font, label, bounds.x() + (bounds.width() - font.width(label)) / 2, bounds.y() + 5, withAlpha(active ? StationScreenStyle.TEXT_PRIMARY : StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        }
        return startY + contactFilterTabsHeight(filters, width);
    }

    private void drawContactActionButton(GuiGraphics guiGraphics, Rect bounds, Component label, int alphaMask) {
        WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask), withAlpha(StationScreenStyle.ACCENT, alphaMask));
        FormattedCharSequence trimmed = trimStyled(label, bounds.width() - 8);
        guiGraphics.drawString(font, trimmed, bounds.x() + (bounds.width() - font.width(trimmed)) / 2, bounds.y() + 5, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
    }

    private Rect pinButtonBounds(ArchiveContactsPayload.ContactEntry contact, int cardX, int cardY, int cardWidth) {
        int rightX = cardX + cardWidth - 154;
        return new Rect(contact.npcId() + ":pin", rightX, cardY + 30, 64, 18);
    }

    private Rect hideButtonBounds(ArchiveContactsPayload.ContactEntry contact, int cardX, int cardY, int cardWidth) {
        int rightX = cardX + cardWidth - 86;
        return new Rect(contact.npcId() + ":hide", rightX, cardY + 30, 64, 18);
    }

    private Rect locateButtonBounds(ArchiveContactsPayload.ContactEntry contact, int cardX, int cardY, int cardWidth) {
        int buttonY = cardY + 54;
        int rightX = cardX + cardWidth - 154;
        return new Rect(contact.npcId() + ":locate", rightX, buttonY, 132, 18);
    }

    private Rect meetupButtonBounds(ArchiveContactsPayload.ContactEntry contact, int cardX, int cardY, int cardWidth) {
        int buttonY = cardY + 76;
        int rightX = cardX + cardWidth - 154;
        return new Rect(contact.npcId() + ":meetup", rightX, buttonY, 132, 18);
    }

    private List<ContactFilter> contactFilters(List<ArchiveContactsPayload.ContactEntry> contacts) {
        List<ContactFilter> filters = new ArrayList<>();
        filters.add(new ContactFilter(CONTACT_FILTER_ALL, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.all"), null));
        LinkedHashSet<String> seenTypes = new LinkedHashSet<>();
        for (ArchiveContactsPayload.ContactEntry contact : contacts) {
            if (seenTypes.add(contact.npcTypeId())) {
                filters.add(new ContactFilter("type:" + contact.npcTypeId(), contactTypeFilterLabel(contact.npcTypeId()), contact.npcTypeId()));
            }
        }
        filters.add(new ContactFilter(CONTACT_FILTER_PINNED, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.pinned"), null));
        filters.add(new ContactFilter(CONTACT_FILTER_HIDDEN, Component.translatable("screen.cyberneticenhancements.archive.contacts.filter.hidden"), null));
        boolean filterFound = false;
        for (ContactFilter filter : filters) {
            if (filter.id().equals(activeContactsFilterId)) {
                filterFound = true;
                break;
            }
        }
        if (!filterFound) {
            activeContactsFilterId = CONTACT_FILTER_ALL;
        }
        return filters;
    }

    private List<ArchiveContactsPayload.ContactEntry> filteredContacts(List<ArchiveContactsPayload.ContactEntry> contacts) {
        List<ArchiveContactsPayload.ContactEntry> filtered = new ArrayList<>();
        for (ArchiveContactsPayload.ContactEntry contact : contacts) {
            if (!matchesContactsFilter(contact)) {
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
        if (activeContactsFilterId.startsWith("type:")) {
            return activeContactsFilterId.substring(5).equals(contact.npcTypeId());
        }
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
            int tabWidth = Math.max(64, font.width(filter.label()) + 18);
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
            int tabWidth = Math.max(64, font.width(filter.label()) + 18);
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

    private String questGroupLabelKey(int sortGroup) {
        return switch (sortGroup) {
            case 0 -> "screen.cyberneticenhancements.archive.quests.group.ready";
            case 1 -> "screen.cyberneticenhancements.archive.quests.group.active";
            case 3 -> "screen.cyberneticenhancements.archive.quests.group.completed";
            case 4 -> "screen.cyberneticenhancements.archive.quests.group.failed";
            default -> "screen.cyberneticenhancements.archive.quests.group.other";
        };
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
        String stateKey = contact.refusing()
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
        if (contact.refusing()) {
            return 0xFFCF5C5C;
        }
        if (contact.surchargeActive()) {
            return 0xFFE0B35A;
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
            case "minecraft:overworld" -> Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.nether");
            case "minecraft:the_end" -> Component.translatable("screen.cyberneticenhancements.archive.contacts.dimension.end");
            default -> Component.literal(humanizeId(dimensionId.contains(":") ? dimensionId.substring(dimensionId.indexOf(':') + 1) : dimensionId));
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
        WikiFrameRenderer.drawPanel(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, withAlpha(StationScreenStyle.PANEL_ALT, alphaMask));
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        guiGraphics.drawString(font, section.title(), x + 6, y + 6, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        int lineY = y + 24;
        if (section.visual() != null) {
            renderSectionVisual(guiGraphics, section.visual(), x + 4, lineY, width - 8, alphaMask);
            lineY += visualHeight(section.visual(), width - 8);
            if (!section.lines().isEmpty()) {
                lineY += 6;
            }
        }
        for (Component line : section.lines()) {
            for (var wrapped : font.split(line, width - 8)) {
                guiGraphics.drawString(font, wrapped, x + 4, lineY, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
                lineY += 11;
            }
            lineY += 2;
        }
    }

    private int measureArticleHeight(Font font, WikiEntry entry, int width) {
        int height = 26;
        height += font.split(entry.subtitle(), width - 8).size() * 11;
        int chipLines = 1;
        int lineWidth = 0;
        for (Component chip : entry.chips()) {
            int chipWidth = Math.min(width, font.width(chip) + 10) + 4;
            if (lineWidth + chipWidth > width) {
                chipLines++;
                lineWidth = 0;
            }
            lineWidth += chipWidth;
        }
        height += 12 + chipLines * (CHIP_HEIGHT + 4);
        for (ArticleSection section : entry.sections()) {
            height += sectionHeight(font, section, width) + SECTION_GAP;
        }
        return height;
    }

    private int sectionHeight(Font font, ArticleSection section, int width) {
        int height = 24;
        if (section.visual() != null) {
            height += visualHeight(section.visual(), width - 8);
            if (!section.lines().isEmpty()) {
                height += 6;
            }
        }
        for (Component line : section.lines()) {
            height += font.split(line, width - 8).size() * 11 + 2;
        }
        return height + (section.lines().isEmpty() ? 0 : 4);
    }

    private void renderSectionVisual(GuiGraphics guiGraphics, SectionVisual visual, int x, int y, int width, int alphaMask) {
        switch (visual) {
            case CraftingGridVisual craftingGridVisual -> renderCraftingVisual(guiGraphics, craftingGridVisual, x, y, width);
            case CompactTransformVisual compactTransformVisual -> renderCompactTransformVisual(guiGraphics, compactTransformVisual, x, y, width, alphaMask);
            case ServiceOperationsVisual serviceOperationsVisual -> renderServiceVisual(guiGraphics, serviceOperationsVisual, x, y, width, alphaMask);
            case RelicCacheExampleVisual relicCacheExampleVisual -> renderRelicCacheExampleVisual(guiGraphics, relicCacheExampleVisual, x, y, width, alphaMask);
        }
    }

    private int visualHeight(SectionVisual visual, int width) {
        return switch (visual) {
            case CraftingGridVisual ignored -> craftingVisualHeight();
            case CompactTransformVisual compactTransformVisual -> compactTransformVisual.outputs().size() > 2 ? compactTransformVisualHeight(2) : compactTransformVisualHeight(1);
            case ServiceOperationsVisual serviceOperationsVisual -> serviceVisualHeight(serviceOperationsVisual, width);
            case RelicCacheExampleVisual ignored -> relicCacheExampleVisualHeight();
        };
    }

    private void renderCraftingVisual(GuiGraphics guiGraphics, CraftingGridVisual visual, int x, int y, int width) {
        ItemStack[] grid = visual.grids().getFirst();
        int contentSize = slotSpan(3);
        int startX = x + Math.max(VISUAL_CONTENT_PADDING, (width - contentSize) / 2);
        int startY = y + VISUAL_CONTENT_PADDING;
        for (int index = 0; index < grid.length; index++) {
            int slotX = startX + (index % 3) * VISUAL_SLOT_STRIDE;
            int slotY = startY + (index / 3) * VISUAL_SLOT_STRIDE;
            drawVisualSlot(guiGraphics, grid[index], slotX, slotY, StationScreenStyle.SLOT_BG);
        }
    }

    private int craftingVisualHeight() {
        return slotSpan(3) + VISUAL_CONTENT_PADDING * 2;
    }

    private void renderCompactTransformVisual(GuiGraphics guiGraphics, CompactTransformVisual visual, int x, int y, int width, int alphaMask) {
        int outputColumns = Math.min(2, Math.max(1, visual.outputs().size()));
        int outputRows = visual.outputs().size() > 2 ? 2 : 1;
        int contentWidth = TRANSFORM_OUTPUT_START + slotSpan(outputColumns);
        int contentHeight = slotSpan(outputRows);
        int visualHeight = compactTransformVisualHeight(outputRows);
        int startX = x + Math.max(VISUAL_CONTENT_PADDING, (width - contentWidth) / 2);
        int startY = y + Math.max(VISUAL_CONTENT_PADDING, (visualHeight - contentHeight) / 2);
        int inputY = startY + Math.max(0, (contentHeight - StationSlotRenderer.SLOT_FRAME_SIZE) / 2);
        drawVisualSlot(guiGraphics, visual.input(), startX, inputY, StationScreenStyle.SLOT_BG);
        int outputStartX = startX + TRANSFORM_OUTPUT_START;
        drawArrow(guiGraphics, startX + 22, inputY + 8, outputStartX - 8);
        int cursorX = outputStartX;
        int cursorY = startY;
        for (int index = 0; index < visual.outputs().size(); index++) {
            if (index == 2) {
                cursorX = outputStartX;
                cursorY += VISUAL_SLOT_STRIDE;
            }
            drawVisualSlot(guiGraphics, visual.outputs().get(index), cursorX, cursorY, StationScreenStyle.SLOT_ACTIVE);
            cursorX += VISUAL_SLOT_STRIDE;
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
        int x = startX;
        int y = startY;
        for (Component chip : chips) {
            int chipWidth = Math.min(width, font.width(chip) + 12) + 4;
            if (x + chipWidth > startX + width) {
                x = startX;
                y += CHIP_HEIGHT + 4;
            }
            drawMiniChip(guiGraphics, x, y, chipWidth, CHIP_HEIGHT, chip, alphaMask);
            x += chipWidth + 4;
        }
        return y + CHIP_HEIGHT;
    }

    private int measureServiceSummaryChipHeight(List<Component> chips, int width) {
        int lineCount = 1;
        int lineWidth = 0;
        for (Component chip : chips) {
            int chipWidth = Math.min(width, font.width(chip) + 12) + 4;
            int spacing = lineWidth == 0 ? 0 : 4;
            if (lineWidth > 0 && lineWidth + spacing + chipWidth > width) {
                lineCount++;
                lineWidth = chipWidth;
            } else {
                lineWidth += spacing + chipWidth;
            }
        }
        return lineCount * CHIP_HEIGHT + Math.max(0, lineCount - 1) * 4;
    }

    private void renderServiceOperationCard(GuiGraphics guiGraphics, ServiceOperationVisual operation, int x, int y, int width, int height, int alphaMask) {
        guiGraphics.drawString(font, operation.label(), x, y, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);

        int lineY = y + 12;
        for (FormattedCharSequence wrapped : font.split(operation.note(), width - 12)) {
            guiGraphics.drawString(font, wrapped, x, lineY, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
            lineY += 11;
        }

        int slotsY = lineY + 4;
        int inputX = x + Math.max(0, (width - serviceSlotRowWidth(operation.materials().size())) / 2);
        int materialsX = inputX + SERVICE_MATERIALS_START;
        drawVisualSlot(guiGraphics, operation.input(), inputX, slotsY, StationScreenStyle.SLOT_BG);
        for (int index = 0; index < operation.materials().size(); index++) {
            drawVisualSlot(guiGraphics, operation.materials().get(index), materialsX + index * VISUAL_SLOT_STRIDE, slotsY, StationScreenStyle.SLOT_BG);
        }

        int outputX = inputX + SERVICE_MATERIALS_START + Math.max(1, operation.materials().size()) * VISUAL_SLOT_STRIDE + SERVICE_OUTPUT_GAP;
        drawArrow(guiGraphics, inputX + 18, slotsY + 8, materialsX - 6);
        drawArrow(guiGraphics, materialsX + Math.max(1, operation.materials().size()) * VISUAL_SLOT_STRIDE, slotsY + 8, outputX - 6);
        drawVisualSlot(guiGraphics, operation.output(), outputX, slotsY, StationScreenStyle.SLOT_ACTIVE);
    }

    private int serviceOperationCardHeight(ServiceOperationVisual operation, int width) {
        return 16 + font.split(operation.note(), width - 12).size() * 11 + StationSlotRenderer.SLOT_FRAME_SIZE + 11;
    }

    private int renderUpgradeStages(GuiGraphics guiGraphics, List<UpgradeStageVisual> stages, int x, int y, int width, int alphaMask) {
        guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.service.upgrade_stages"), x, y, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.fill(x, y + 12, x + width, y + 13, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, alphaMask));
        int stageY = y + 18;
        for (UpgradeStageVisual stage : stages) {
            int stageHeight = stageHeight(stage, width);
            int chipWidth = Math.max(34, font.width(stage.label()) + 12);
            drawMiniChip(guiGraphics, x, stageY, chipWidth, CHIP_HEIGHT, stage.label(), alphaMask);
            int lineY = stageY + 22;
            if (!stage.materials().isEmpty()) {
                guiGraphics.drawString(font, Component.translatable("wiki.cyberneticenhancements.service.stage.cost"), x, lineY, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
                int materialX = x + 32;
                for (int index = 0; index < stage.materials().size(); index++) {
                    drawVisualSlot(guiGraphics, stage.materials().get(index), materialX + index * VISUAL_SLOT_STRIDE, lineY - 4, StationScreenStyle.SLOT_BG);
                }
                lineY += 22;
            }
            for (Component change : stage.changes()) {
                for (var wrapped : font.split(change, width - 8)) {
                    guiGraphics.drawString(font, wrapped, x, lineY, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
                    lineY += 11;
                }
                lineY += 2;
            }
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
        int height = 20;
        if (!stage.materials().isEmpty()) {
            height += 22;
        }
        for (Component change : stage.changes()) {
            height += font.split(change, width - 12).size() * 11 + 2;
        }
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
        int color = StationScreenStyle.ACCENT;
        guiGraphics.fill(startX, centerY, endX, centerY + 1, color);
        guiGraphics.fill(endX - 4, centerY - 2, endX, centerY - 1, color);
        guiGraphics.fill(endX - 4, centerY + 2, endX, centerY + 3, color);
        guiGraphics.fill(endX - 2, centerY - 1, endX + 2, centerY + 2, color);
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
        int rowAreaWidth = pane.width - 12 - (Math.max(0.0D, layout.totalHeight() - viewportHeight) > 0.0D ? SCROLLBAR_WIDTH : 0);
        for (NavRow row : layout.rows()) {
            if (row.alpha() < 0.20F) {
                continue;
            }
            int rowY = contentY + Math.round(row.y()) - (int) Math.round(navScroll.current);
            if (rowY + ROW_HEIGHT < contentY || rowY > contentY + viewportHeight) {
                continue;
            }
            if (mouseX >= pane.x + 6 && mouseX <= pane.x + 6 + rowAreaWidth - 4 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2) {
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
        int rowAreaWidth = pane.width - 12 - (Math.max(0.0D, entries.size() * (double) ENTRY_ROW_HEIGHT - viewportHeight) > 0.0D ? SCROLLBAR_WIDTH : 0);
        for (int index = 0; index < entries.size(); index++) {
            int rowY = contentY + index * ENTRY_ROW_HEIGHT - (int) Math.round(listScroll.current);
            if (rowY + ENTRY_ROW_HEIGHT < contentY || rowY > contentY + viewportHeight) {
                continue;
            }
            if (mouseX >= pane.x + 6 && mouseX <= pane.x + 6 + rowAreaWidth - 4 && mouseY >= rowY && mouseY <= rowY + ENTRY_ROW_HEIGHT - 2) {
                setSelectedEntry(entries.get(index));
                playClick();
                return true;
            }
        }
        return false;
    }

    private boolean tryStartScrollbarDrag(double mouseX, double mouseY, Pane pane, ScrollState scroll, double maxScroll, DragTarget target) {
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        if (maxScroll <= 0.0D) {
            return false;
        }
        ScrollbarThumb thumb = buildThumb(pane, scroll.current, maxScroll, viewportHeight, contentY);
        if (mouseX >= thumb.x && mouseX <= thumb.x + thumb.width && mouseY >= thumb.y && mouseY <= thumb.y + thumb.height) {
            dragTarget = target;
            scroll.startDrag(mouseY, thumb.y);
            return true;
        }
        return false;
    }

    private void dragScrollbar(ScrollState scroll, Pane pane, double mouseY, double maxScroll) {
        int contentY = pane.y + PANEL_HEADER_HEIGHT + 6;
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        int thumbHeight = Math.max(20, (int) Math.round(viewportHeight * (viewportHeight / (double) (viewportHeight + maxScroll))));
        int availableTrack = Math.max(1, viewportHeight - thumbHeight);
        double thumbY = Mth.clamp(scroll.dragThumbStart + (mouseY - scroll.dragMouseStart), contentY, contentY + availableTrack);
        double ratio = (thumbY - contentY) / availableTrack;
        scroll.target = ratio * maxScroll;
        scroll.current = scroll.target;
    }

    private ScrollbarThumb buildThumb(Pane pane, double scroll, double maxScroll, int viewportHeight, int contentY) {
        int thumbHeight = Math.max(20, (int) Math.round(viewportHeight * (viewportHeight / (double) (viewportHeight + maxScroll))));
        int availableTrack = Math.max(1, viewportHeight - thumbHeight);
        int thumbY = contentY + (maxScroll <= 0.0D ? 0 : (int) Math.round((scroll / maxScroll) * availableTrack));
        return new ScrollbarThumb(pane.x + pane.width - SCROLLBAR_WIDTH - 6, thumbY, SCROLLBAR_WIDTH, thumbHeight);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, Pane pane, ScrollState scroll, double maxScroll, int viewportHeight, int contentY) {
        if (maxScroll <= 0.0D) {
            return;
        }
        int trackX = pane.x + pane.width - SCROLLBAR_WIDTH - 6;
        guiGraphics.fill(trackX, contentY, trackX + SCROLLBAR_WIDTH, contentY + viewportHeight, StationScreenStyle.PANEL_DEEP);
        ScrollbarThumb thumb = buildThumb(pane, scroll.current, maxScroll, viewportHeight, contentY);
        guiGraphics.fill(thumb.x, thumb.y, thumb.x + thumb.width, thumb.y + thumb.height, StationScreenStyle.SECTION_HIGHLIGHT);
        guiGraphics.fill(thumb.x, thumb.y, thumb.x + thumb.width, thumb.y + 1, StationScreenStyle.ACCENT);
        guiGraphics.fill(thumb.x, thumb.y + thumb.height - 1, thumb.x + thumb.width, thumb.y + thumb.height, StationScreenStyle.FRAME_SHADOW);
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
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        return Math.max(0.0D, measureContactsHeight(pane) - viewportHeight + 4.0D);
    }

    private double maxBankingScroll(Pane pane) {
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        return Math.max(0.0D, measureBankingHeight(pane) - viewportHeight + 4.0D);
    }

    private double maxQuestsScroll(Pane pane) {
        int viewportHeight = pane.height - PANEL_HEADER_HEIGHT - 12;
        return Math.max(0.0D, measureQuestsHeight(pane) - viewportHeight + 4.0D);
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

    private void activateTab(ArchiveTab tab) {
        activeTab = tab == null && !archiveTabs.isEmpty() ? archiveTabs.getFirst() : tab;
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
        float openProgress = easeOutCubic(animationProgress(archiveUiStartAt(), OPEN_ANIMATION_MS));
        int slideOffset = Math.round((1.0F - openProgress) * 18.0F);
        int rootX = OUTER_MARGIN;
        int rootY = OUTER_MARGIN - slideOffset;
        int rootWidth = width - OUTER_MARGIN * 2;
        int rootHeight = height - OUTER_MARGIN * 2;
        int contentY = rootY + TOP_BAR_HEIGHT + PANEL_GAP;
        int contentHeight = rootHeight - TOP_BAR_HEIGHT - FOOTER_HEIGHT - PANEL_GAP * 2;
        if (isBankingTab() || isContactsTab() || isQuestsTab()) {
            Pane fullPane = new Pane(rootX + PANEL_GAP, contentY, rootWidth - PANEL_GAP * 2, contentHeight);
            Pane hiddenPane = new Pane(0, 0, 0, 0);
            return new RootLayout(rootX, rootY, rootWidth, rootHeight, hiddenPane, hiddenPane, fullPane, true);
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
        return selectedSection != null && selectedSection.entries().size() <= 1;
    }

    private boolean isBankingTab() {
        return activeTab != null && BANKING_TAB_ID.equals(activeTab.id());
    }

    private boolean isContactsTab() {
        return activeTab != null && CONTACTS_TAB_ID.equals(activeTab.id());
    }

    private boolean isQuestsTab() {
        return activeTab != null && QUESTS_TAB_ID.equals(activeTab.id());
    }

    private void drawTopBar(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + TOP_BAR_HEIGHT, StationScreenStyle.PANEL_ALT);
        guiGraphics.fill(x + 2, y + TOP_BAR_HEIGHT - 1, x + width - 2, y + TOP_BAR_HEIGHT, StationScreenStyle.FRAME_HIGHLIGHT);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.title"), x + 14, y + 10, StationScreenStyle.TEXT_PRIMARY, false);
        Component subtitle = activeTab == null ? Component.translatable("screen.cyberneticenhancements.archive.subtitle") : activeTab.subtitle();
        Rect closeBounds = closeButtonBounds(x, y, width);
        int subtitleX = x + 138;
        int subtitleWidth = Math.max(48, closeBounds.x() - subtitleX - 10);
        guiGraphics.drawString(font, trimStyled(subtitle, subtitleWidth), subtitleX, y + 10, StationScreenStyle.TEXT_SECONDARY, false);
        renderArchiveTabs(guiGraphics, x + 12, y + 29, closeBounds.x() - (x + 12) - 12);
        drawMiniChip(guiGraphics, closeBounds.x(), closeBounds.y(), closeBounds.width(), closeBounds.height(), Component.translatable("screen.cyberneticenhancements.archive.close"), 0xFFFFFFFF);
    }

    private void renderBootSequence(GuiGraphics guiGraphics, int rootX, int rootY, int rootWidth, int rootHeight) {
        float overlayProgress = bootOverlayProgress();
        if (overlayProgress <= 0.0F) {
            return;
        }

        float bootProgress = bootSequenceProgress();
        int alphaMask = ((int) (overlayProgress * 255.0F) << 24) | 0x00FFFFFF;
        int overlayFill = withAlpha(0xFF05080C, overlayProgress);
        guiGraphics.fill(0, 0, width, height, overlayFill);

        int panelWidth = Math.min(rootWidth - 80, 440);
        int panelHeight = 162;
        int panelX = rootX + (rootWidth - panelWidth) / 2;
        int panelY = rootY + (rootHeight - panelHeight) / 2 - 10;

        WikiFrameRenderer.drawPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, withAlpha(StationScreenStyle.PANEL_DEEP, alphaMask));
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 20, withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask));
        guiGraphics.fill(panelX + 2, panelY + 20, panelX + panelWidth - 2, panelY + 21, withAlpha(StationScreenStyle.ACCENT, overlayProgress * 0.8F));

        long now = System.currentTimeMillis();
        int sweepY = panelY + 24 + (int) ((panelHeight - 44) * bootProgress);
        guiGraphics.fill(panelX + 2, Math.max(panelY + 22, sweepY - 8), panelX + panelWidth - 2, Math.min(panelY + panelHeight - 2, sweepY + 2), withAlpha(StationScreenStyle.SLOT_ACTIVE, 0.12F * overlayProgress));
        for (int y = panelY + 24; y < panelY + panelHeight - 2; y += 3) {
            float stripeAlpha = (((y + now / 24L) % 9) == 0) ? 0.08F : 0.04F;
            guiGraphics.fill(panelX + 2, y, panelX + panelWidth - 2, y + 1, withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, stripeAlpha * overlayProgress));
        }

        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.boot.header"), panelX + 10, panelY + 8, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.boot.title"), panelX + 10, panelY + 30, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
        guiGraphics.drawString(font, trimStyled(Component.translatable("screen.cyberneticenhancements.archive.boot.subtitle"), panelWidth - 20), panelX + 10, panelY + 44, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);

        List<Component> statusLines = List.of(
                Component.translatable("screen.cyberneticenhancements.archive.boot.phase.1"),
                Component.translatable("screen.cyberneticenhancements.archive.boot.phase.2"),
                Component.translatable("screen.cyberneticenhancements.archive.boot.phase.3"),
                Component.translatable("screen.cyberneticenhancements.archive.boot.phase.4")
        );
        int completedLines = Math.min(statusLines.size(), Math.max(1, Mth.ceil(bootProgress * statusLines.size())));
        int lineY = panelY + 66;
        for (int index = 0; index < statusLines.size(); index++) {
            boolean active = index < completedLines;
            int lineColor = active ? withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask) : withAlpha(StationScreenStyle.TEXT_SECONDARY, 0.45F * overlayProgress);
            String prefix = active ? "[OK]" : "[  ]";
            guiGraphics.drawString(font, prefix, panelX + 10, lineY, active ? withAlpha(StationScreenStyle.ACCENT, alphaMask) : withAlpha(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, 0.55F * overlayProgress), false);
            guiGraphics.drawString(font, trimStyled(statusLines.get(index), panelWidth - 72), panelX + 44, lineY, lineColor, false);
            lineY += 14;
        }

        int barX = panelX + 10;
        int barY = panelY + panelHeight - 30;
        int barWidth = panelWidth - 20;
        guiGraphics.fill(barX, barY, barX + barWidth, barY + 8, withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask));
        guiGraphics.fill(barX + 1, barY + 1, barX + 1 + Math.max(0, Math.round((barWidth - 2) * bootProgress)), barY + 7, withAlpha(StationScreenStyle.ACCENT, alphaMask));
        guiGraphics.fill(barX + 1, barY + 1, barX + 1 + Math.max(0, Math.round((barWidth - 2) * bootProgress * 0.45F)), barY + 3, withAlpha(StationScreenStyle.TEXT_PRIMARY, 0.35F * overlayProgress));

        String percent = String.format(Locale.ROOT, "%d%%", Mth.floor(bootProgress * 100.0F));
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.boot.status", percent), barX, barY - 11, withAlpha(StationScreenStyle.TEXT_SECONDARY, alphaMask), false);
        if (bootProgress >= 0.995F) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.archive.boot.ready"), panelX + panelWidth - 150, barY - 11, withAlpha(StationScreenStyle.ACCENT, alphaMask), false);
        }
    }

    private void drawPanelShell(GuiGraphics guiGraphics, Pane pane, Component title) {
        drawPanelShell(guiGraphics, pane, title, 0);
    }

    private void drawPanelShell(GuiGraphics guiGraphics, Pane pane, Component title, int rightInset) {
        WikiFrameRenderer.drawPanel(guiGraphics, pane.x, pane.y, pane.width, pane.height, StationScreenStyle.PANEL_ALT);
        guiGraphics.fill(pane.x + 2, pane.y + 2, pane.x + pane.width - 2, pane.y + PANEL_HEADER_HEIGHT, StationScreenStyle.PANEL_DEEP);
        guiGraphics.fill(pane.x + 2, pane.y + PANEL_HEADER_HEIGHT - 1, pane.x + pane.width - 2, pane.y + PANEL_HEADER_HEIGHT, StationScreenStyle.FRAME_HIGHLIGHT_SOFT);
        guiGraphics.drawString(font, trimStyled(title, Math.max(24, pane.width - 16 - rightInset)), pane.x + 8, pane.y + 7, StationScreenStyle.TEXT_PRIMARY, false);
    }

    private void drawListRow(GuiGraphics guiGraphics, int x, int y, int width, int height, float emphasis, float alpha) {
        int fill = blendColors(StationScreenStyle.PANEL_DEEP, StationScreenStyle.SLOT_ACTIVE, emphasis * 0.75F);
        guiGraphics.fill(x, y, x + width, y + height, withAlpha(fill, alpha));
        if (emphasis > 0.03F) {
            int accentWidth = Math.max(1, Math.round(2.0F * emphasis));
            guiGraphics.fill(x, y, x + accentWidth, y + height, withAlpha(StationScreenStyle.ACCENT, alpha));
            guiGraphics.fill(x + accentWidth, y, x + width, y + 1, withAlpha(blendColors(StationScreenStyle.FRAME_HIGHLIGHT_SOFT, StationScreenStyle.ACCENT, emphasis * 0.5F), alpha));
        }
    }

    private void drawMiniChip(GuiGraphics guiGraphics, int x, int y, int width, int height, Component text, int alphaMask) {
        WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, x, y, width, height, withAlpha(StationScreenStyle.SECTION_SHADOW, alphaMask), withAlpha(StationScreenStyle.ACCENT, alphaMask));
        FormattedCharSequence trimmed = trimStyled(text, width - 8);
        guiGraphics.drawString(font, trimmed, x + (width - font.width(trimmed)) / 2, y + 4, withAlpha(StationScreenStyle.TEXT_PRIMARY, alphaMask), false);
    }

    private void renderArchiveTabs(GuiGraphics guiGraphics, int startX, int startY, int availableWidth) {
        int x = startX;
        for (ArchiveTab tab : archiveTabs) {
            int tabWidth = Math.max(64, font.width(tab.title()) + 18);
            if (x + tabWidth > startX + availableWidth) {
                break;
            }
            boolean active = activeTab != null && activeTab.id().equals(tab.id());
            int fill = active ? blendColors(StationScreenStyle.PANEL_ALT, StationScreenStyle.SLOT_ACTIVE, 0.78F) : StationScreenStyle.SECTION_SHADOW;
            int accent = active ? StationScreenStyle.ACCENT : StationScreenStyle.FRAME_HIGHLIGHT_SOFT;
            WikiFrameRenderer.drawPanelWithAccentTop(guiGraphics, x, startY, tabWidth, TOP_TAB_HEIGHT, fill, accent);
            if (active) {
                guiGraphics.fill(x + 1, startY + TOP_TAB_HEIGHT - 1, x + tabWidth - 1, startY + TOP_TAB_HEIGHT, fill);
            }
            FormattedCharSequence label = trimStyled(tab.title(), tabWidth - 10);
            guiGraphics.drawString(font, label, x + (tabWidth - font.width(label)) / 2, startY + 5, active ? StationScreenStyle.TEXT_PRIMARY : StationScreenStyle.TEXT_SECONDARY, false);
            x += tabWidth + TOP_TAB_GAP;
        }
    }

    private boolean handleArchiveTabClick(double mouseX, double mouseY, int rootX, int rootY, int rootWidth) {
        for (Rect bounds : archiveTabBounds(rootX, rootY, rootWidth)) {
            if (!isInside(mouseX, mouseY, bounds)) {
                continue;
            }
            ArchiveTab tab = archiveTabById(bounds.id());
            if (tab == null) {
                return false;
            }
            if (activeTab == null || !activeTab.id().equals(tab.id())) {
                activateTab(tab);
            }
            playClick();
            return true;
        }
        return false;
    }

    private List<Rect> archiveTabBounds(int rootX, int rootY, int rootWidth) {
        Rect closeBounds = closeButtonBounds(rootX, rootY, rootWidth);
        int startX = rootX + 12;
        int maxX = closeBounds.x() - 12;
        int y = rootY + 29;
        List<Rect> bounds = new ArrayList<>(archiveTabs.size());
        int x = startX;
        for (ArchiveTab tab : archiveTabs) {
            int tabWidth = Math.max(64, font.width(tab.title()) + 18);
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
        return new Rect("close", rootX + rootWidth - CLOSE_BUTTON_WIDTH - 16, rootY + 8, CLOSE_BUTTON_WIDTH, CHIP_HEIGHT);
    }

    private FormattedCharSequence trimStyled(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return Language.getInstance().getVisualOrder(text);
        }

        int ellipsisWidth = font.width("...");
        FormattedText base = font.substrByWidth(text, Math.max(0, maxWidth - ellipsisWidth));
        FormattedText combined = FormattedText.composite(base, Component.literal("..."));
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
        drawMiniChip(
                guiGraphics,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                Component.translatable(pinned ? "screen.cyberneticenhancements.archive.unpin" : "screen.cyberneticenhancements.archive.pin"),
                0xFFFFFFFF
        );
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
        return new PinButtonBounds(articlePane.x + articlePane.width - PIN_BUTTON_WIDTH - 8, articlePane.y + 3, PIN_BUTTON_WIDTH, CHIP_HEIGHT);
    }

    private boolean isPinned(String entryId) {
        return PINNED_ENTRY_IDS.contains(entryId);
    }

    private void togglePinned(String entryId) {
        if (!PINNED_ENTRY_IDS.add(entryId)) {
            PINNED_ENTRY_IDS.remove(entryId);
        }
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
        return System.currentTimeMillis() - openedAt < BOOT_SEQUENCE_MS;
    }

    private float bootSequenceProgress() {
        return animationProgress(openedAt, BOOT_SEQUENCE_MS);
    }

    private float bootOverlayProgress() {
        long elapsed = System.currentTimeMillis() - openedAt;
        if (elapsed <= BOOT_SEQUENCE_MS) {
            return 1.0F;
        }
        if (elapsed >= BOOT_SEQUENCE_MS + BOOT_FADE_MS) {
            return 0.0F;
        }
        float fade = 1.0F - ((elapsed - BOOT_SEQUENCE_MS) / (float) BOOT_FADE_MS);
        return easeOutCubic(Mth.clamp(fade, 0.0F, 1.0F));
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
        return openedAt + BOOT_SEQUENCE_MS;
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

    private record RootLayout(int rootX, int rootY, int rootWidth, int rootHeight, Pane navPane, Pane listPane, Pane articlePane, boolean listCollapsed) {
    }

    private record NavRow(WikiTopic topic, int depth, float y, float alpha) {
    }

    private record NavLayout(List<NavRow> rows, float totalHeight) {
    }

    private record ScrollbarThumb(int x, int y, int width, int height) {
    }

    private record ArticleMetrics(int contentWidth, int contentHeight, double maxScroll) {
    }

    private record ContactFilter(String id, Component label, String npcTypeId) {
    }

    private record BankFilter(String id, Component label) {
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

    private static final class ScrollState {
        private double target;
        private double current;
        private double dragMouseStart;
        private double dragThumbStart;

        private void tick() {
            current = Mth.lerp(0.35D, current, target);
            if (Math.abs(current - target) < 0.5D) {
                current = target;
            }
        }

        private void add(double amount, double maxScroll) {
            target = Mth.clamp(target + amount, 0.0D, maxScroll);
        }

        private void clamp(double maxScroll) {
            target = Mth.clamp(target, 0.0D, maxScroll);
            current = Mth.clamp(current, 0.0D, maxScroll);
        }

        private void reset() {
            target = 0.0D;
            current = 0.0D;
        }

        private void startDrag(double mouseY, double thumbY) {
            dragMouseStart = mouseY;
            dragThumbStart = thumbY;
        }
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
        entries.put(ModBlocks.RIPPER_STATION.asItem(), "station_ripper");
        entries.put(ModBlocks.TECH_STATION.asItem(), "station_tech");
        entries.put(ModBlocks.RECYCLER_STATION.asItem(), "station_recycler");
        entries.put(ModBlocks.RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.UNCOMMON_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.RARE_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.EPIC_RELIC_CACHE.asItem(), "station_relic_cache");
        entries.put(ModBlocks.LEGENDARY_RELIC_CACHE.asItem(), "station_relic_cache");
        return Map.copyOf(entries);
    }

    private static Map<Block, String> createEntryIdsByBlock() {
        Map<Block, String> entries = new LinkedHashMap<>();
        entries.put(ModBlocks.RIPPER_STATION.get(), "station_ripper");
        entries.put(ModBlocks.TECH_STATION.get(), "station_tech");
        entries.put(ModBlocks.RECYCLER_STATION.get(), "station_recycler");
        entries.put(ModBlocks.RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.UNCOMMON_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.RARE_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.EPIC_RELIC_CACHE.get(), "station_relic_cache");
        entries.put(ModBlocks.LEGENDARY_RELIC_CACHE.get(), "station_relic_cache");
        return Map.copyOf(entries);
    }
}
