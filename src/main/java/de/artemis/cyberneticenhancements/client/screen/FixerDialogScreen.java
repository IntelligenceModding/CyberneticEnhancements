package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestDimension;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer;
import de.artemis.cyberneticenhancements.common.dialogue.NpcService;
import de.artemis.cyberneticenhancements.common.dialogue.NpcShopCatalog;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.FixerEntity;
import de.artemis.cyberneticenhancements.common.entity.MercEntity;
import de.artemis.cyberneticenhancements.common.entity.NetrunnerEntity;
import de.artemis.cyberneticenhancements.common.entity.RipperdocEntity;
import de.artemis.cyberneticenhancements.common.entity.TechieEntity;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.network.FixerDialogueChoicePayload;
import de.artemis.cyberneticenhancements.common.network.NpcIdentityAppearancePayload;
import de.artemis.cyberneticenhancements.common.network.NpcIdentityNicknamePayload;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.ui.Icons;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import de.artemis.cyberneticenhancements.client.ui.UiMotion;
import de.artemis.cyberneticenhancements.client.ui.UiButton;
import de.artemis.cyberneticenhancements.client.ui.ShopGridLayout;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import de.artemis.cyberneticenhancements.client.ui.UiEditBox;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class FixerDialogScreen extends ResponsiveContainerScreen<FixerDialogMenu> {
    private static final int OUTER_BG = UiRenderer.PANEL;
    private static final int PANEL_BG = StationScreenStyle.PANEL_BG;
    private static final int PANEL_ALT = StationScreenStyle.PANEL_ALT;
    private static final int PANEL_EDGE = StationScreenStyle.PANEL_EDGE;
    private static final int PANEL_DEEP = StationScreenStyle.PANEL_DEEP;
    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;
    private static final int ACCENT = StationScreenStyle.ACCENT;
    private static final int CONTENT_PADDING = 12;
    private static final int PANEL_TEXT_PADDING_X = StationScreenStyle.PANEL_TEXT_PADDING_X;
    private static final int PANEL_TEXT_PADDING_Y = StationScreenStyle.PANEL_TEXT_PADDING_Y;
    private static final int PANEL_GAP = 8;
    private static final int HEADER_HEIGHT = 54;
    private static final int CHOICE_BUTTON_HEIGHT = 18;
    private static final int CHOICE_BUTTON_GAP = 4;
    private static final int CHOICE_ICON_Y_OFFSET = 1;
    private static final int TRUST_BAR_HEIGHT = 8;
    private static final int SHOP_TILE_GAP = 10;
    private static final int SHOP_TILE_WIDTH = 136;
    private static final int SHOP_TILE_HEIGHT = 144;
    private static final int SHOP_VISIBLE_ACTIONS = 1;
    private static final int CONTRACT_CARDS = 3;
    private static final int IDENTITY_LABEL_Y_OFFSET = 28;
    private static final int IDENTITY_FIELD_Y_OFFSET = 40;
    private static final int IDENTITY_GALLERY_Y_OFFSET = 68;
    private static final int IDENTITY_TILE_MIN_HEIGHT = 70;
    private static final int IDENTITY_TILE_GAP = 8;
    private static final int IDENTITY_VISIBLE_SKINS = 4;
    private static final int IDENTITY_SCROLLBAR_HEIGHT = UiScrollbar.WIDTH;
    private static final int IDENTITY_SCROLLBAR_GAP = 10;
    private static final int IMAGE_W = 640;
    private static final int IMAGE_H = 484;

    private final List<ChoiceButton> choiceButtons = new ArrayList<>();
    private final List<UiButton> shopBuyButtons = new ArrayList<>();
    private final List<UiButton> serviceBuyButtons = new ArrayList<>();
    private final List<UiButton> contractButtons = new ArrayList<>();
    private EditBox nicknameField;
    private ConversationMode mode = ConversationMode.SHOP;
    private String localNpcLineKey;
    private String selectedAppearanceId = "";
    private final UiScrollbar shopScrollbar = new UiScrollbar(), identityScrollbar = new UiScrollbar();
    private int skinPage;
    private int shopScrollOffset;
    private AbstractCityNpcEntity previewNpc;
    private int lastMouseX;
    private int lastMouseY;

    public FixerDialogScreen(FixerDialogMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelX = 10000;
        this.inventoryLabelY = 10000;
        this.titleLabelX = 10000;
        this.titleLabelY = 10000;
        this.localNpcLineKey = greetingKey();
        if (menu.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH) {
            // Their one request is all they have to offer, so it is laid out at once instead of behind a choice.
            this.mode = ConversationMode.CONTRACTS;
            this.localNpcLineKey = "screen.cyberneticenhancements.npc.dialogue.world_encounter.line.work";
        } else {
            // Opened by a key from the spoken dialogue: straight into the window that was asked for.
            switch (menu.initialView()) {
                case 1 -> { mode = ConversationMode.SHOP; localNpcLineKey = npcLineKey("shop"); }
                case 2 -> { mode = ConversationMode.SERVICES; localNpcLineKey = npcLineKey("services"); }
                case 3 -> { mode = ConversationMode.CONTRACTS; localNpcLineKey = npcLineKey("contracts"); }
                case 4 -> { mode = ConversationMode.IDENTITY; localNpcLineKey = responseKey("identity"); }
                default -> {
                    var type = menu.npcType();
                    if (type.supportsShop()) { mode = ConversationMode.SHOP; localNpcLineKey = npcLineKey("shop"); }
                    else if (!NpcService.forNpc(type).isEmpty()) { mode = ConversationMode.SERVICES; localNpcLineKey = npcLineKey("services"); }
                    else { mode = ConversationMode.CONTRACTS; localNpcLineKey = npcLineKey("contracts"); }
                }
            }
        }
    }

    /** Each window has its own Back button and names its speaker in the header, so nothing sits above it. */
    @Override
    protected boolean showsTitleBar() { return false; }

    @Override
    protected void init() {
        String nicknameDraft = nicknameField == null ? null : nicknameField.getValue();
        super.init();
        if (minecraft.player != null) minecraft.player.stopUsingItem();
        minecraft.options.keyUse.setDown(false);
        choiceButtons.clear();
        shopBuyButtons.clear();
        serviceBuyButtons.clear();
        contractButtons.clear();
        for (int slot = 0; slot < CONTRACT_CARDS * 2; slot++) { // Each card has up to two buttons: hand in above abandon, or just accept.
            final int cardSlot = slot;
            UiButton button = new UiButton(0, 0, 1, CHOICE_BUTTON_HEIGHT, Component.empty(), pressed -> contractButtonPressed(cardSlot));
            button.visible = false;
            contractButtons.add(button);
            addRenderableWidget(button);
        }
        for (NpcService service : NpcService.forNpc(menu.npcType())) {
            UiButton button = new UiButton(0, 0, 1, CHOICE_BUTTON_HEIGHT, Component.empty(), pressed ->
                    sendOption(service.optionId()));
            button.visible = false;
            serviceBuyButtons.add(button);
            addRenderableWidget(button);
        }
        for (int slot = 0; slot < shopItemCount(); slot++) {
            final int itemSlot = slot;
            UiButton button = new UiButton(0, 0, 1, CHOICE_BUTTON_HEIGHT, Component.empty(), pressed -> {
                sendOption(-itemSlot - 1);
            });
            button.visible = false;
            shopBuyButtons.add(button);
            addRenderableWidget(button);
        }
        nicknameField = new UiEditBox(font, identityFieldX(), identityFieldY(), identityFieldWidth(), 18, Component.translatable("screen.cyberneticenhancements.npc.identity.nickname"));
        nicknameField.setMaxLength(32);
        nicknameField.setVisible(false);
        addRenderableWidget(nicknameField);
        int buttonX = actionButtonsX();
        int buttonWidth = actionButtonsWidth();
        int startY = actionButtonsStartY();
        for (int index = 0; index < 6; index++) {
            ChoiceButton button = new ChoiceButton(buttonX, startY + index * (CHOICE_BUTTON_HEIGHT + CHOICE_BUTTON_GAP), buttonWidth, CHOICE_BUTTON_HEIGHT);
            choiceButtons.add(button);
            addRenderableWidget(button);
        }
        if (nicknameDraft == null) {
            syncIdentityEditorFromNpc();
        } else {
            nicknameField.setValue(nicknameDraft);
        }
        refreshChoices();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nicknameField != null) {
            nicknameField.setX(identityFieldX());
            nicknameField.setY(identityFieldY());
            nicknameField.setWidth(identityFieldWidth());
            nicknameField.setVisible(mode == ConversationMode.IDENTITY);
            nicknameField.setEditable(mode == ConversationMode.IDENTITY && menu.identityUnlocked());
        }
        refreshChoices();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        guiGraphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, OUTER_BG);
        drawPanel(guiGraphics, x0, y0, imageWidth, imageHeight, PANEL_BG);
        drawPanel(guiGraphics, headerX(), headerY(), headerWidth(), headerHeight(), PANEL_ALT);
        drawPanel(guiGraphics, transcriptX(), transcriptY(), transcriptWidth(), transcriptHeight(), PANEL_ALT);

        renderHeader(guiGraphics);
        renderTranscript(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderChoiceTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedContent(double mouseX, double mouseY, int button) {
        // As in the spoken dialogue, a right click anywhere steps back: out of the window and into the talk again.
        boolean typing = mode == ConversationMode.IDENTITY && nicknameField != null && nicknameField.isVisible() && nicknameField.isMouseOver(mouseX, mouseY);
        if (button == 1 && !typing) {
            onClose();
            return true;
        }
        if (button == 0
                && mode == ConversationMode.IDENTITY
                && nicknameField != null
                && nicknameField.isVisible()
                && nicknameField.isFocused()) {
            if (TextFieldFocusHelper.unfocusOnOutsideClick(this, mouseX, mouseY, List.of(nicknameField))) {
                sendNicknameUpdate();
            }
        }
        if (mode == ConversationMode.IDENTITY && button == 0 && menu.identityUnlocked()) {
            int skinIndex = clickedIdentitySkin(mouseX, mouseY);
            if (skinIndex >= 0) {
                List<ResourceLocation> skins = availableSkins();
                if (skinIndex < skins.size()) {
                    selectedAppearanceId = skins.get(skinIndex).toString();
                    AbstractCityNpcEntity npc = currentNpcEntity();
                    if (npc != null) {
                        npc.applyClientPreviewIdentity(
                                selectedAppearanceId,
                                npc.npcCategory(),
                                currentNpcDisplayName(),
                                currentNameColorId()
                        );
                    }
                    PacketDistributor.sendToServer(new NpcIdentityAppearancePayload(selectedAppearanceId));
                    alignSkinScrollToSelection();
                }
                return true;
            }
            if (clickedIdentityScrollbar(mouseX, mouseY)) {
                return true;
            }
        }
        if (mode == ConversationMode.SHOP && button == 0) {
            for (UiButton buyButton : shopBuyButtons) {
                if (buyButton.visible && buyButton.isMouseOver(mouseX, mouseY)) {
                    buyButton.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
            if (clickedShopScrollbar(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClickedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedContent(double x,double y,int button,double dx,double dy) {
        if(button==0&&mode==ConversationMode.SHOP&&shopScrollbar.dragging()){
            shopScrollOffset=(int)Math.round(shopScrollbar.value(shopTrack(),x,y));return true;
        }
        if(button==0&&mode==ConversationMode.IDENTITY&&identityScrollbar.dragging()){
            skinPage=(int)Math.round(identityScrollbar.value(identityTrack(),x,y));return true;
        }
        return super.mouseDraggedContent(x,y,button,dx,dy);
    }
    @Override
    protected boolean mouseReleasedContent(double x,double y,int button) {
        shopScrollbar.release();identityScrollbar.release();return super.mouseReleasedContent(x,y,button);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (mode == ConversationMode.IDENTITY
                && nicknameField != null
                && nicknameField.isVisible()
                && nicknameField.isFocused()) {
            if (keyCode == 256) {
                sendNicknameUpdate();
                TextFieldFocusHelper.clearFocus(this, List.of(nicknameField));
                return super.keyPressed(keyCode,scanCode,modifiers);
            }            if (keyCode == 257 || keyCode == 335) {
                sendNicknameUpdate();
                TextFieldFocusHelper.clearFocus(this, List.of(nicknameField));
                return true;
            }
            if (nicknameField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        // The key that opened this window from the spoken dialogue also steps back out of it.
        if (windowKeyHint() != null
                && de.artemis.cyberneticenhancements.client.ModKeyMappings.DIALOGUE_WINDOWS.get(windowKeyIndex()).matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (mode == ConversationMode.IDENTITY
                && nicknameField != null
                && nicknameField.isVisible()
                && nicknameField.isFocused()
                && nicknameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean mouseScrolledContent(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mode == ConversationMode.SHOP && scrollY != 0.0D) {
            scrollShop(scrollY < 0.0D ? 1 : -1);
            return true;
        }
        if (mode == ConversationMode.IDENTITY && menu.identityUnlocked() && scrollY != 0.0D) {
            scrollSkinGallery(scrollY < 0.0D ? 1 : -1);
            return true;
        }
        return super.mouseScrolledContent(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshChoices() {
        List<ChoiceSpec> specs = currentChoices();
        int buttonX = actionButtonsX();
        int buttonWidth = actionButtonsWidth();
        int startY = actionButtonsStartY();
        for (int index = 0; index < choiceButtons.size(); index++) {
            ChoiceButton button = choiceButtons.get(index);
            button.setX(buttonX);
            button.setY(startY + index * (CHOICE_BUTTON_HEIGHT + CHOICE_BUTTON_GAP));
            button.setWidth(buttonWidth);
            if (index >= specs.size()) {
                button.visible = false;
                button.active = false;
                clearChoiceHighlight(button);
                button.spec = null;
                continue;
            }
            ChoiceSpec replacement = specs.get(index);
            if (button.spec == null || button.spec.icon() != replacement.icon()
                    || !button.spec.message().equals(replacement.message())) {
                clearChoiceHighlight(button);
            }
            button.spec = replacement;
            button.visible = true;
            button.active = button.spec.enabled();
            button.setMessage(button.spec.message());
        }
        updateShopBuyButtons();
        updateServiceBuyButtons();
        updateContractButtons();
    }

    private void updateShopBuyButtons() {
        for (int slot = 0; slot < shopBuyButtons.size(); slot++) {
            UiButton button = shopBuyButtons.get(slot);
            if (mode != ConversationMode.SHOP) {
                button.visible = false;
                button.active = false;
                if (getFocused() == button) setFocused(null);
                button.setFocused(false);
                continue;
            }
            NpcShopCatalog.Offer entry = shopEntry(slot);
            int y = shopTileY(slot) + shopTileHeight() - CHOICE_BUTTON_HEIGHT - 8;
            var layout = shopLayout();
            boolean visible = mode == ConversationMode.SHOP && menu.shopOfferAvailable(slot)
                    && slot >= layout.firstSlot(shopScrollOffset) && slot < layout.lastSlot(shopScrollOffset) // Only the tiles that are drawn.
                    && shopTileY(slot) >= shopRowsTop() && y + CHOICE_BUTTON_HEIGHT <= shopRowsBottom();
            button.visible = visible;
            button.active = visible && menu.balance() >= menu.shopPrice(slot);
            if (!visible && getFocused() == button) setFocused(null);
            if (!visible) button.setFocused(false);
            button.setX(shopTileX(slot) + 8);
            button.setY(y);
            button.setWidth(shopTileWidth() - 16);
            button.setMessage(Component.translatable("screen.cyberneticenhancements.fixer.option.buy", menu.shopPrice(slot)));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                    "screen.cyberneticenhancements.npc.choice.shop.buy", entry.label(), menu.shopPrice(slot))));
        }
    }

    private void clearChoiceHighlight(ChoiceButton button) {
        if (getFocused() == button) setFocused(null);
        button.setFocused(false);
        button.highlight.reset();
    }

    private int devQuestPage;
    private QuestDimension devQuestDimension;

    private List<ChoiceSpec> currentChoices() {
        return switch (mode) {
            case SERVICES -> serviceChoices();
            case SHOP -> shopChoices();
            case CONTRACTS -> contractChoices();
            case IDENTITY -> identityChoices();
        };
    }

    private ChoiceSpec backChoice() {
        return new ChoiceSpec(ChoiceIcon.BACK, Component.translatable("screen.cyberneticenhancements.fixer.choice.back"), true, null, this::onClose, windowKeyHint());
    }

    /** The index of the key that opens the window being shown, as in the spoken dialogue; it also closes it again. */
    private int windowKeyIndex() {
        return switch (mode) { case SHOP -> 0; case SERVICES -> 1; case CONTRACTS -> 2; case IDENTITY -> 3; };
    }

    /** That key as a badge like the one beside the window's answer in the spoken dialogue, or nothing where no key opened the window. */
    private Component windowKeyHint() {
        if (menu.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH) return null;
        return Component.literal("[").append(de.artemis.cyberneticenhancements.client.ModKeyMappings.DIALOGUE_WINDOWS.get(windowKeyIndex()).getTranslatedKeyMessage()).append("]");
    }

    private List<ChoiceSpec> serviceChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(backChoice());
        return choices;
    }

    private List<ChoiceSpec> shopChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(backChoice());
        return choices;
    }

    /** The offers and the live contract are cards of their own; only the dev picker still lists its choices. */
    private List<ChoiceSpec> contractChoices() {
        if (menu.isDevTest() && menu.activeContractCount() == 0) return devQuestChoices();
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(backChoice());
        return choices;
    }

    private boolean devQuestPicker() {
        return mode == ConversationMode.CONTRACTS && menu.isDevTest() && menu.activeContractCount() == 0;
    }

    /** One card of the row: an open contract or an offer, by its slot in the menu. */
    private record ContractCard(boolean active, int slot) {}

    /** The open contracts first, then, while there is room for another, the offers on the board; at most one row of cards. */
    private List<ContractCard> contractCards() {
        List<ContractCard> cards = new ArrayList<>();
        for (int slot = 0; slot < PlayerQuestManager.MAX_ACTIVE_PER_ISSUER; slot++) if (menu.hasActiveContract(slot)) cards.add(new ContractCard(true, slot));
        if (cards.size() >= PlayerQuestManager.MAX_ACTIVE_PER_ISSUER) return cards;
        for (int slot = 0; slot < menu.offerSlots() && cards.size() < CONTRACT_CARDS; slot++) if (menu.offerPresent(slot)) cards.add(new ContractCard(false, slot));
        return cards;
    }

    private void contractButtonPressed(int buttonIndex) {
        var cards = contractCards();
        int card = buttonIndex / 2;
        if (card >= cards.size()) return;
        var entry = cards.get(card);
        if (!entry.active()) {
            sendOption(menu.acceptOfferOption(entry.slot()));
            return;
        }
        boolean turnIn = buttonIndex % 2 == 0;
        sendOption(switch (entry.slot()) {
            case 0 -> turnIn ? FixerDialogMenu.OPTION_TURN_IN_CONTRACT : FixerDialogMenu.OPTION_ABANDON_CONTRACT;
            case 1 -> turnIn ? FixerDialogMenu.OPTION_TURN_IN_CONTRACT_2 : FixerDialogMenu.OPTION_ABANDON_CONTRACT_2;
            default -> turnIn ? FixerDialogMenu.OPTION_TURN_IN_CONTRACT_3 : FixerDialogMenu.OPTION_ABANDON_CONTRACT_3;
        });
    }

    private List<ChoiceSpec> devQuestChoices() {
        if (menu.offersPreparing()) return List.of(customChoice(ChoiceIcon.QUEST,
                Component.translatable("screen.cyberneticenhancements.fixer.contracts_preparing"), false, () -> {}));
        var choices = new ArrayList<ChoiceSpec>();
        if (devQuestDimension == null) {
            for (var dimension : QuestDimension.values()) {
                choices.add(customChoice(ChoiceIcon.QUEST, dimension.displayName(), true, () -> {
                    devQuestDimension = dimension;
                    devQuestPage = 0;
                    refreshChoices();
                }));
            }
            choices.add(backChoice());
            return choices;
        }
        var dimension = devQuestDimension;
        int count = dimension == QuestDimension.STANDARD ? menu.offerSlots() : QuestType.values().length;
        int pages = Math.ceilDiv(count, 3);
        for (int slot = devQuestPage * 3; slot < Math.min(count, devQuestPage * 3 + 3); slot++) {
            int option = FixerDialogMenu.devQuestOption(dimension, slot);
            Component title = slot == QuestType.values().length
                    ? Component.translatable("quest.cyberneticenhancements.title.chromium_survey")
                    : QuestContract.titleForType(QuestType.values()[slot]);
            choices.add(customChoice(ChoiceIcon.QUEST, title, true, () -> sendOption(option)));
        }
        choices.add(customChoice(ChoiceIcon.QUEST, Component.translatable("screen.cyberneticenhancements.dev_test.quest_page", devQuestPage + 1, pages),
                true, () -> { devQuestPage = (devQuestPage + 1) % pages; refreshChoices(); }));
        choices.add(customChoice(ChoiceIcon.BACK, Component.translatable("screen.cyberneticenhancements.dev_test.quest_dimensions"),
                true, () -> { devQuestDimension = null; refreshChoices(); }));
        return choices;
    }

    private List<ChoiceSpec> identityChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(customChoice(
                ChoiceIcon.RESET,
                Component.translatable("screen.cyberneticenhancements.npc.identity.reset_name"),
                menu.identityUnlocked(),
                this::resetIdentityCustomization
        ));
        choices.add(backChoice());
        return choices;
    }

    private void clampShopScroll() {
        shopScrollOffset = Math.max(0, Math.min(maxShopScrollOffset(), shopScrollOffset));
    }

    private NpcShopCatalog.Offer shopEntry(int slot) {
        return menu.shopOffers().get(slot);
    }

    private String greetingKey() {
        return "screen.cyberneticenhancements.npc.line." + menu.npcTypeId() + ".greeting";
    }

    private String npcLineKey(String suffix) {
        return "screen.cyberneticenhancements.npc.line." + menu.npcTypeId() + "." + suffix;
    }

    private String responseKey(String suffix) {
        return "screen.cyberneticenhancements.npc.response." + menu.npcTypeId() + "." + suffix;
    }

    private ChoiceSpec customChoice(ChoiceIcon icon, Component message, boolean enabled, Runnable action) {
        return customChoice(icon, message, enabled, null, action);
    }

    private ChoiceSpec customChoice(ChoiceIcon icon, Component message, boolean enabled, Component tooltip, Runnable action) {
        return new ChoiceSpec(icon, message, enabled, tooltip, action, null);
    }

    private void sendOption(int optionId) {
        PacketDistributor.sendToServer(new FixerDialogueChoicePayload(optionId));
    }

    private void openIdentityMode() {
        mode = ConversationMode.IDENTITY;
        localNpcLineKey = responseKey("identity");
        syncIdentityEditorFromNpc();
        refreshChoices();
    }

    private void sendNicknameUpdate() {
        String nickname = nicknameField == null ? "" : nicknameField.getValue();
        PacketDistributor.sendToServer(new NpcIdentityNicknamePayload(nickname));
        AbstractCityNpcEntity npc = currentNpcEntity();
        String sanitized = nickname == null ? "" : nickname.trim();
        if (npc != null) {
            String displayName = sanitized.isBlank() ? currentNpcBaseName() : sanitized;
            npc.setCustomName(Component.literal(displayName).withStyle(resolveCurrentNameColor()));
            npc.setCustomNameVisible(!"hidden".equals(currentNameColorId()));
        }
        TextFieldFocusHelper.clearFocus(this, List.of(nicknameField));
    }

    private void resetIdentityCustomization() {
        if (nicknameField != null) {
            nicknameField.setValue("");
        }
        selectedAppearanceId = "";
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (npc != null) {
            npc.applyClientPreviewIdentity(
                    "",
                    npc.npcCategory(),
                    currentNpcBaseName(),
                    "aqua"
            );
        }
        PacketDistributor.sendToServer(new NpcIdentityNicknamePayload(""));
        PacketDistributor.sendToServer(new NpcIdentityAppearancePayload(""));
        TextFieldFocusHelper.clearFocus(this, List.of(nicknameField));
    }

    private void syncIdentityEditorFromNpc() {
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (nicknameField != null && npc != null) {
            nicknameField.setValue(npc.getName().getString());
        }
        selectedAppearanceId = currentAppearanceId();
        alignSkinScrollToSelection();
    }

    private AbstractCityNpcEntity currentNpcEntity() {
        if (minecraft == null || minecraft.level == null || menu.entityId() < 0) {
            return null;
        }
        return minecraft.level.getEntity(menu.entityId()) instanceof AbstractCityNpcEntity npc ? npc : null;
    }

    private String currentNpcDisplayName() {
        AbstractCityNpcEntity npc = currentNpcEntity();
        return npc == null ? title.getString() : npc.getName().getString();
    }

    private String currentNpcBaseName() {
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (npc == null || npc.baseName().isBlank()) {
            return title.getString();
        }
        return npc.baseName();
    }

    private String currentAppearanceId() {
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (npc == null) {
            return selectedAppearanceId;
        }
        if (!npc.appearanceOverride().isBlank()) {
            return npc.appearanceOverride();
        }
        return FixerEntityRenderer.resolveTexture(npc.getUUID(), npc.npcCategory()).toString();
    }

    private String currentNameColorId() {
        var npc = currentNpcEntity();
        return npc == null ? "aqua" : npc.nameColorId();
    }

    private int currentNameColorRgb() {
        return switch (currentNameColorId()) {
            case "blue" -> 0x5D86F1;
            case "yellow" -> 0xFFFF55;
            case "green" -> 0x57C26E;
            case "red" -> 0xD65D5D;
            case "white" -> 0xE8EDF2;
            case "light_purple" -> 0xD88FF7;
            case "hidden" -> 0xE8EDF2;
            default -> 0x4FE9E2;
        };
    }

    private net.minecraft.ChatFormatting resolveCurrentNameColor() {
        return switch (currentNameColorId()) {
            case "blue" -> net.minecraft.ChatFormatting.BLUE;
            case "yellow" -> net.minecraft.ChatFormatting.YELLOW;
            case "green" -> net.minecraft.ChatFormatting.GREEN;
            case "red" -> net.minecraft.ChatFormatting.RED;
            case "white" -> net.minecraft.ChatFormatting.WHITE;
            case "light_purple" -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            case "hidden" -> net.minecraft.ChatFormatting.WHITE;
            default -> net.minecraft.ChatFormatting.AQUA;
        };
    }

    private List<ResourceLocation> availableSkins() {
        return FixerEntityRenderer.availableTextures();
    }

    private int selectedAppearanceIndex() {
        List<ResourceLocation> skins = availableSkins();
        if (skins.isEmpty()) {
            return 0;
        }
        String current = selectedAppearanceId == null || selectedAppearanceId.isBlank() ? currentAppearanceId() : selectedAppearanceId;
        for (int index = 0; index < skins.size(); index++) {
            if (skins.get(index).toString().equals(current)) {
                return index;
            }
        }
        return 0;
    }

    private void scrollSkinGallery(int delta) {
        int maxOffset = maxSkinScrollOffset();
        skinPage = Math.max(0, Math.min(maxOffset, skinPage + delta));
    }

    private int maxSkinScrollOffset() {
        return Math.max(0, availableSkins().size() - IDENTITY_VISIBLE_SKINS);
    }

    private void alignSkinScrollToSelection() {
        int selectedIndex = selectedAppearanceIndex();
        if (selectedIndex < skinPage) {
            skinPage = selectedIndex;
            return;
        }
        int lastVisible = skinPage + IDENTITY_VISIBLE_SKINS - 1;
        if (selectedIndex > lastVisible) {
            skinPage = Math.max(0, selectedIndex - IDENTITY_VISIBLE_SKINS + 1);
        }
    }

    private int clickedIdentitySkin(double mouseX, double mouseY) {
        int startX = identityGalleryX();
        int startY = identityGalleryY();
        int tileWidth = identityTileWidth();
        int tileHeight = identityTileHeight();
        List<ResourceLocation> skins = availableSkins();
        int visible = Math.min(IDENTITY_VISIBLE_SKINS, Math.max(0, skins.size() - skinPage));
        for (int slot = 0; slot < visible; slot++) {
            int tileX = startX + slot * (tileWidth + IDENTITY_TILE_GAP);
            if (mouseX >= tileX && mouseX < tileX + tileWidth
                    && mouseY >= startY && mouseY < startY + tileHeight) {
                return skinPage + slot;
            }
        }
        return -1;
    }

    private UiScrollbar.Track identityTrack() {
        int total=availableSkins().size(), visible=Math.min(IDENTITY_VISIBLE_SKINS,total);
        return UiScrollbar.layout(identityScrollbarX(),identityScrollbarY(),identityScrollbarWidth(),IDENTITY_SCROLLBAR_HEIGHT,true,visible,maxSkinScrollOffset(),skinPage);
    }
    private boolean clickedIdentityScrollbar(double mouseX,double mouseY) {
        var track=identityTrack();if(!identityScrollbar.begin(track,mouseX,mouseY))return false;
        skinPage=(int)Math.round(identityScrollbar.value(track,mouseX,mouseY));return true;
    }
    private void renderHeader(GuiGraphics guiGraphics) {
        int x = headerX() + PANEL_TEXT_PADDING_X;
        int y = headerY() + PANEL_TEXT_PADDING_Y;
        guiGraphics.drawString(font, currentNpcDisplayName(), x, y, currentNameColorRgb(), false);
        // A passing street doctor has nothing to track: only the name, since no title bar carries it any more.
        if (menu.temporary()) return;
        guiGraphics.drawString(font, menu.npcTypeLabel(), x, y + 12, ACCENT, false);
        Component trust = Component.translatable("screen.cyberneticenhancements.fixer.trust", trustLabel());
        Component discount = Component.translatable("screen.cyberneticenhancements.fixer.summary.discount", discountLabel());
        int rightX = headerX() + headerWidth() - PANEL_TEXT_PADDING_X;
        guiGraphics.drawString(font, trust, rightX - font.width(trust), y, TEXT_SECONDARY, false);
        guiGraphics.drawString(font, discount, rightX - font.width(discount), y + 12, ACCENT, false);
        drawTrustBar(guiGraphics, x, headerY() + 32, headerWidth() - PANEL_TEXT_PADDING_X * 2, TRUST_BAR_HEIGHT);
    }

    private void renderTranscript(GuiGraphics guiGraphics) {
        if (mode == ConversationMode.SERVICES) {
            renderServices(guiGraphics);
            return;
        }
        if (mode == ConversationMode.IDENTITY) {
            renderIdentityEditor(guiGraphics);
            return;
        }
        if (mode == ConversationMode.SHOP) {
            renderShopCatalog(guiGraphics);
            return;
        }
        renderContracts(guiGraphics);
    }

    private int contractCardWidth() {
        return (transcriptWidth() - PANEL_TEXT_PADDING_X * 2 - SHOP_TILE_GAP * (CONTRACT_CARDS - 1)) / CONTRACT_CARDS;
    }

    /** Where the content of the shop, services and contracts windows starts: right below their title (the dev quest picker keeps a subtitle line). */
    private int contentTop() {
        return transcriptY() + (devQuestPicker() ? 48 : PANEL_TEXT_PADDING_Y + 20);
    }

    /** Where it ends, so all three windows keep the same distance to the Back button: down to it, but with room for the NPC's answer while that is shown above it. */
    private int contentBottom() {
        return actionButtonsStartY() - (menu.responseId() != FixerDialogMenu.RESPONSE_GREETING ? 32 : 10);
    }

    private int contractCardY() {
        return contentTop();
    }

    private int contractCardHeight() {
        return contentBottom() - contentTop();
    }

    /** Where the {@code count} cards start so that they sit in the middle of the panel; a live contract is one wide card. */
    private int contractCardsX(int count, int width) {
        return transcriptX() + (transcriptWidth() - (count * width + (count - 1) * SHOP_TILE_GAP)) / 2;
    }

    private void updateContractButtons() {
        boolean cardsShown = mode == ConversationMode.CONTRACTS && !devQuestPicker();
        var cards = contractCards();
        int width = contractCardWidth();
        for (int index = 0; index < contractButtons.size(); index++) {
            UiButton button = contractButtons.get(index);
            int card = index / 2;
            boolean hasCard = card < cards.size();
            boolean active = hasCard && cards.get(card).active();
            boolean visible = cardsShown && hasCard && (index % 2 == 0 || active) && !(menu.offersPreparing() && !active);
            button.visible = visible;
            if (!visible) {
                if (getFocused() == button) setFocused(null);
                button.setFocused(false);
                continue;
            }
            int slot = cards.get(card).slot();
            button.active = !active || index % 2 == 1 || menu.activeContractReady(slot);
            int y = contractCardY() + contractCardHeight() - CHOICE_BUTTON_HEIGHT - 10;
            button.setX(contractsOfferX(cards.size(), card) + 10);
            button.setWidth(width - 20);
            if (active) { // Hand in sits above abandon.
                if (index % 2 == 0) y -= CHOICE_BUTTON_HEIGHT + 4;
                button.setMessage(Component.translatable(index % 2 == 0 ? "screen.cyberneticenhancements.npc.contract.turn_in"
                        : "screen.cyberneticenhancements.npc.contract.abandon"));
            } else {
                button.setMessage(Component.translatable("screen.cyberneticenhancements.npc.contract.accept", menu.offerRewardMoney(slot)));
            }
            button.setY(y);
            button.setTooltip(null);
        }
    }

    private int contractsOfferX(int count, int index) {
        return contractCardsX(count, contractCardWidth()) + index * (contractCardWidth() + SHOP_TILE_GAP);
    }

    private static net.minecraft.world.item.Item contractIcon(QuestType type, int targetIndex) {
        return switch (type) {
            case DELIVERY -> Items.CHEST;
            case EXTRACTION -> Items.ENDER_EYE;
            case INVESTIGATION -> Items.SPYGLASS;
            case BREACH -> Items.TRIPWIRE_HOOK;
            case ELIMINATION -> Items.IRON_SWORD;
            case RECOVERY -> QuestContract.safeRecoveryItem(targetIndex);
            case CYBERPSYCHO -> Items.WITHER_SKELETON_SKULL;
            case LOCATE -> Items.COMPASS;
            case SALVAGE -> Items.BARREL;
        };
    }

    private void renderContracts(GuiGraphics graphics) {
        updateContractButtons();
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        int innerWidth = transcriptWidth() - PANEL_TEXT_PADDING_X * 2;
        graphics.drawString(font, modeLabel(), x, y, TEXT_PRIMARY, false);
        if (devQuestPicker() && devQuestDimension != null) {
            drawWrappedText(graphics, devQuestDimension.displayName(), x, y + 14, innerWidth, TEXT_SECONDARY, 2);
        }
        if (!devQuestPicker()) renderContractCards(graphics);
        if (menu.responseId() != FixerDialogMenu.RESPONSE_GREETING) {
            graphics.drawString(font, trimStyled(npcText(), innerWidth), x, actionButtonsStartY() - 20, ACCENT, false);
        }
    }

    private void renderContractCards(GuiGraphics graphics) {
        var cards = contractCards();
        if (cards.isEmpty()) {
            boolean withheld = menu.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH;
            Component none = Component.translatable(menu.offersPreparing() ? "screen.cyberneticenhancements.fixer.contracts_preparing"
                    : withheld ? "screen.cyberneticenhancements.fixer.contracts_withheld" : "screen.cyberneticenhancements.npc.contract.none");
            graphics.drawString(font, none, transcriptX() + (transcriptWidth() - font.width(none)) / 2,
                    contractCardY() + contractCardHeight() / 2 - font.lineHeight / 2, TEXT_SECONDARY, false);
            return;
        }
        for (int index = 0; index < cards.size(); index++) {
            var card = cards.get(index);
            int slot = card.slot(), x = contractsOfferX(cards.size(), index);
            if (card.active()) renderActiveContract(graphics, x, slot);
            else drawContractCard(graphics, x, contractCardWidth(),
                    new ItemStack(contractIcon(menu.offerType(slot), menu.offerTargetIndex(slot))), menu.offerTitle(slot),
                    Component.translatable("screen.cyberneticenhancements.npc.contract.tier", menu.offerTier(slot)),
                    menu.offerSummary(slot), Component.empty(), menu.offerRewards(slot), Component.empty(), true, 0);
        }
    }

    private void renderActiveContract(GuiGraphics graphics, int x, int slot) {
        boolean ready = menu.activeContractReady(slot);
        drawContractCard(graphics, x, contractCardWidth(),
                new ItemStack(contractIcon(menu.activeContractType(slot), menu.activeContractTargetIndex(slot))), menu.activeContractTitle(slot),
                Component.translatable("screen.cyberneticenhancements.npc.contract.tier", menu.activeContractTier(slot)),
                menu.activeContractObjective(slot), menu.activeContractLocationSummary(slot), menu.activeContractRewards(slot),
                ready ? Component.translatable("screen.cyberneticenhancements.npc.contract.ready") : Component.empty(), ready, CHOICE_BUTTON_HEIGHT + 4);
    }

    private void drawContractCard(GuiGraphics graphics, int cx, int width, ItemStack icon, Component title, Component tier, Component objective,
                                  Component extra, List<FixerDialogMenu.RewardLine> rewards, Component state, boolean highlight, int lift) {
        int cy = contractCardY(), height = contractCardHeight();
        UiRenderer.control(graphics, cx, cy, width, height, true, false, false, 0xFFFFFFFF);
        height -= lift; // A card with a second button keeps its text clear of it.
        graphics.pose().pushPose();
        graphics.pose().translate(cx + (width - 32) / 2, cy + 10, 0);
        graphics.pose().scale(2, 2, 1);
        SchematicIconRenderer.drawWithShadow(graphics, icon, 0, 0, true);
        graphics.pose().popPose();
        int textY = cy + 51;
        for (var line : font.split(title, width - 14)) {
            graphics.drawString(font, line, cx + (width - font.width(line)) / 2, textY, TEXT_PRIMARY, false);
            textY += 11;
        }
        graphics.drawString(font, tier, cx + (width - font.width(tier)) / 2, textY + 2, ACCENT, false);
        int bodyY = Math.max(cy + 90, textY + 18);
        drawCenteredWrapped(graphics, objective, cx, bodyY, width - 20, width, TEXT_SECONDARY, 5);
        if (!extra.getString().isBlank()) drawCenteredWrapped(graphics, extra, cx, bodyY + 62, width - 20, width, TEXT_PRIMARY, 2);
        // The rewards, one line each with the item that stands for it, as a block in the middle just above the buttons.
        int lineHeight = 14, rewardsBottom = cy + height - 40 - (state.getString().isBlank() ? 0 : 12);
        int blockWidth = 0;
        for (var line : rewards) blockWidth = Math.max(blockWidth, 16 + font.width(line.text()));
        int rewardX = cx + (width - Math.min(blockWidth, width - 20)) / 2, rewardY = rewardsBottom - rewards.size() * lineHeight;
        for (int index = 0; index < rewards.size(); index++) {
            var line = rewards.get(index);
            int rowY = rewardY + index * lineHeight;
            float bob = (float) Math.sin(net.minecraft.Util.getMillis() / 350.0 + index * 1.2) * 0.8F;
            graphics.pose().pushPose();
            graphics.pose().translate(rewardX, rowY + bob, 0);
            graphics.pose().scale(0.75F, 0.75F, 1);
            SchematicIconRenderer.drawWithShadow(graphics, line.icon(), 0, 0, true);
            graphics.pose().popPose();
            graphics.drawString(font, trimStyled(line.text(), width - 36), rewardX + 16, rowY + 2, ACCENT, false);
        }
        if (!state.getString().isBlank())
            graphics.drawString(font, state, cx + 10, cy + height - 50, highlight ? ACCENT : TEXT_SECONDARY, false);
    }

    private int serviceCardX(int slot) {
        return transcriptX() + PANEL_TEXT_PADDING_X + slot * (serviceCardWidth() + SHOP_TILE_GAP);
    }

    private int serviceCardWidth() {
        return (transcriptWidth() - PANEL_TEXT_PADDING_X * 2 - SHOP_TILE_GAP) / 2;
    }

    private int serviceCardY() {
        return contentTop();
    }

    private int serviceCardHeight() {
        return contentBottom() - contentTop();
    }

    private void updateServiceBuyButtons() {
        List<NpcService> services = NpcService.forNpc(menu.npcType());
        for (int slot = 0; slot < serviceBuyButtons.size(); slot++) {
            UiButton button = serviceBuyButtons.get(slot);
            NpcService service = services.get(slot);
            boolean visible = mode == ConversationMode.SERVICES;
            button.visible = visible;
            button.active = visible && menu.isOptionAvailable(service.optionId())
                    && (!service.daily() || menu.purgeReadyClient()) && menu.balance() >= menu.priceForOption(service.optionId());
            if (!visible && getFocused() == button) setFocused(null);
            if (!visible) button.setFocused(false);
            if (!visible) continue;
            button.setX(serviceCardX(slot) + 10);
            button.setY(serviceCardY() + serviceCardHeight() - CHOICE_BUTTON_HEIGHT - 10);
            button.setWidth(serviceCardWidth() - 20);
            button.setMessage(Component.translatable("screen.cyberneticenhancements.npc.service.purchase", menu.priceForOption(service.optionId())));
            button.setTooltip(null);
        }
    }

    private void renderServices(GuiGraphics graphics) {
        updateServiceBuyButtons();
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        graphics.drawString(font, modeLabel(), x, y, TEXT_PRIMARY, false);
        List<NpcService> services = NpcService.forNpc(menu.npcType());
        for (int slot = 0; slot < services.size(); slot++) {
            NpcService service = services.get(slot);
            int cx = serviceCardX(slot), cy = serviceCardY(), width = serviceCardWidth(), height = serviceCardHeight();
            UiRenderer.control(graphics, cx, cy, width, height, true, false, false, 0xFFFFFFFF);
            var icon = switch (service) {
                case STRAIN_BUFFER -> Items.HONEY_BOTTLE;
                case NEURAL_RESET -> Items.REDSTONE_TORCH;
                case TRAUMA_COVERAGE -> Items.TOTEM_OF_UNDYING; case MEDICAL_STIMS, CLINIC_CARE -> Items.GOLDEN_APPLE;
                case DETOX -> Items.MILK_BUCKET;
                case FIELD_TRIAGE -> Items.ENCHANTED_GOLDEN_APPLE;
                case NEURAL_FLUSH -> Items.DRAGON_BREATH;
                case MINING_CALIBRATION -> Items.IRON_PICKAXE;
                case HAZARD_PREPARATION -> Items.SHIELD;
                case RECON_SUPPORT -> Items.SPYGLASS;
                case STEALTH_PROTOCOL -> Items.ENDER_PEARL;
                case FIELD_RECOVERY -> Items.GLISTERING_MELON_SLICE;
                case COMBAT_PREPARATION -> Items.IRON_SWORD;
            };
            graphics.pose().pushPose();
            graphics.pose().translate(cx + (width - 32) / 2, cy + 10, 0);
            graphics.pose().scale(2, 2, 1);
            SchematicIconRenderer.drawWithShadow(graphics, new ItemStack(icon), 0, 0, true);
            graphics.pose().popPose();
            Component name = Component.translatable(service.nameKey());
            graphics.drawString(font, name, cx + (width - font.width(name)) / 2, cy + 51, TEXT_PRIMARY, false);
            drawCenteredWrapped(graphics, Component.translatable(service.descriptionKey()), cx, cy + 68, width - 20, width, TEXT_SECONDARY, 3);
            int lineY = cy + 110;
            for (Component detail : service.details()) {
                for (var line : font.split(detail, width - 20)) {
                    graphics.drawString(font, line, cx + (width - font.width(line)) / 2, lineY, TEXT_PRIMARY, false);
                    lineY += 12;
                }
            }
            drawCenteredWrapped(graphics, Component.translatable(service.daily()
                            ? "screen.cyberneticenhancements.npc.service.daily" : "screen.cyberneticenhancements.npc.service.refresh"),
                    cx, cy + height - 78, width - 20, width, TEXT_SECONDARY, 2);
            Component state;
            boolean ready = false;
            if (!menu.isOptionAvailable(service.optionId())) state = lockedTrustTooltip(service.optionId());
            else if (service.daily() && !menu.purgeReadyClient()) state = Component.translatable("screen.cyberneticenhancements.npc.service.used_today");
            else if (menu.balance() < menu.priceForOption(service.optionId())) state = Component.translatable("screen.cyberneticenhancements.npc.service.need_funds");
            else {
                state = Component.translatable("screen.cyberneticenhancements.npc.service.ready");
                ready = true;
            }
            drawCenteredWrapped(graphics, state, cx, cy + height - 50, width - 20, width, ready ? ACCENT : TEXT_SECONDARY, 2);
        }
        if (menu.responseId() != FixerDialogMenu.RESPONSE_GREETING) {
            graphics.drawString(font, trimStyled(npcText(), transcriptWidth() - PANEL_TEXT_PADDING_X * 2),
                    x, actionButtonsStartY() - 20, ACCENT, false);
        }
    }

    private void renderShopCatalog(GuiGraphics guiGraphics) {
        clampShopScroll();
        updateShopBuyButtons();
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        int width = shopGridWidth();
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.section.shop_catalog"), x, y, TEXT_PRIMARY, false);

        guiGraphics.enableScissor(x, shopRowsTop(), x + width, shopRowsBottom());
        ShopGridLayout layout = shopLayout();
        int firstSlot = layout.firstSlot(shopScrollOffset);
        int lastSlot = layout.lastSlot(shopScrollOffset);
        for (int slot = firstSlot; slot < lastSlot; slot++) {
            renderShopTile(guiGraphics, shopTileX(slot), shopTileY(slot), shopTileWidth(), slot);
        }
        guiGraphics.disableScissor();
        renderShopScrollbar(guiGraphics);
    }

    private void renderShopTile(GuiGraphics guiGraphics, int x, int y, int width, int slot) {
        NpcShopCatalog.Offer entry = shopEntry(slot);
        boolean available = menu.shopOfferAvailable(slot);
        UiRenderer.control(guiGraphics, x, y, width, shopTileHeight(), available, false, false, 0xFFFFFFFF);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + (width - 32) / 2, y + 10, 0);
        guiGraphics.pose().scale(2, 2, 1);
        SchematicIconRenderer.drawWithShadow(guiGraphics, entry.stack(), 0, 0, true);
        guiGraphics.renderItemDecorations(font, entry.stack(), 0, 0);
        guiGraphics.pose().popPose();
        var lines = font.split(entry.label(), width - 14);
        for (int line = 0; line < Math.min(2, lines.size()); line++) {
            var text = lines.get(line);
            guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y + 51 + line * 11, TEXT_PRIMARY, false);
        }
        var descriptionLines = font.split(entry.description(), width - 16);
        for (int line = 0; line < Math.min(3, descriptionLines.size()); line++) {
            var text = descriptionLines.get(line);
            guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y + 77 + line * 11, TEXT_SECONDARY, false);
        }
        if (!available) {
            Component priceLine = Component.translatable("screen.cyberneticenhancements.npc.shop.price", menu.shopPrice(slot));
            guiGraphics.drawString(font, priceLine, x + (width - font.width(priceLine)) / 2, y + shopTileHeight() - 28, ACCENT, false);
            var locked = trimStyled(Component.translatable("screen.cyberneticenhancements.npc.shop.state.locked", menu.shopTrustLabel(slot)), width - 14);
            guiGraphics.drawString(font, locked, x + (width - font.width(locked)) / 2, y + shopTileHeight() - 8, 0xFFD08A8A, false);
        }
    }

    private UiScrollbar.Track shopTrack() {
        return UiScrollbar.layout(shopScrollbarX(),shopScrollbarY(),shopScrollbarWidth(),shopScrollbarHeight(),false,visibleShopRowCount(),maxShopScrollOffset(),shopScrollOffset);
    }
    private void renderShopScrollbar(GuiGraphics graphics) {
        UiScrollbar.draw(graphics,shopTrack(),lastMouseX,lastMouseY,shopScrollbar.dragging());
    }
    private void renderIdentityEditor(GuiGraphics guiGraphics) {
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        int maxWidth = transcriptWidth() - PANEL_TEXT_PADDING_X * 2;
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.identity.title"), x, y, TEXT_PRIMARY, false);
        drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.npc.identity.subtitle"), x, y + 12, maxWidth, TEXT_SECONDARY, 1);

        int labelY = y + IDENTITY_LABEL_Y_OFFSET;
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.identity.nickname"), x, labelY, ACCENT, false);

        int galleryX = identityGalleryX();
        int galleryY = identityGalleryY();
        int tileWidth = identityTileWidth();
        int tileHeight = identityTileHeight();
        List<ResourceLocation> skins = availableSkins();
        String current = selectedAppearanceId == null || selectedAppearanceId.isBlank() ? currentAppearanceId() : selectedAppearanceId;
        int visible = Math.min(IDENTITY_VISIBLE_SKINS, Math.max(0, skins.size() - skinPage));
        for (int slot = 0; slot < visible; slot++) {
            int index = skinPage + slot;
            int tileX = galleryX + slot * (tileWidth + IDENTITY_TILE_GAP);
            boolean selected = skins.get(index).toString().equals(current);
            boolean hovered = lastMouseX >= tileX && lastMouseX < tileX + tileWidth
                    && lastMouseY >= galleryY && lastMouseY < galleryY + tileHeight;
            renderIdentityTileBackground(guiGraphics, tileX, galleryY, tileWidth, tileHeight, selected, hovered);
            guiGraphics.enableScissor(tileX + 1, galleryY + 1, tileX + tileWidth - 1, galleryY + tileHeight - 1);
            renderNpcModel(guiGraphics, skins.get(index), tileX + 4, galleryY + 4, tileWidth - 8, tileHeight - 10, false);
            guiGraphics.disableScissor();
            renderIdentityTileOverlay(guiGraphics, tileX, galleryY, tileWidth, tileHeight, selected, hovered);
        }
        renderIdentityScrollbar(guiGraphics);
    }

    private void renderIdentityTileBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        UiRenderer.control(guiGraphics, x, y, width, height, true, selected, hovered, 0xFFFFFFFF);
    }

    private void renderIdentityTileOverlay(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        int activeEdge = selected ? hovered ? TEXT_PRIMARY : ACCENT : hovered ? UiRenderer.PANEL_EDGE_ACTIVE : PANEL_EDGE;
        int passiveEdge = activeEdge;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 250.0F);
        guiGraphics.fill(x, y, x + width, y + 1, activeEdge);
        guiGraphics.fill(x, y, x + 1, y + height, activeEdge);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, passiveEdge);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, passiveEdge);

        guiGraphics.pose().popPose();
    }

    private void renderIdentityScrollbar(GuiGraphics graphics) {
        UiScrollbar.draw(graphics,identityTrack(),lastMouseX,lastMouseY,identityScrollbar.dragging());
    }
    private void renderNpcModel(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int renderWidth, int renderHeight, boolean preview) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        AbstractCityNpcEntity entity = previewNpc(texture);
        float centerX = x + renderWidth * 0.5F;
        float bottomY = y + renderHeight - (preview ? 10.0F : 34.0F);
        float scale = preview
                ? Math.max(42.0F, Math.min(renderWidth * 0.60F, renderHeight * 0.54F))
                : Math.max(36.0F, Math.min(renderWidth * 0.84F, renderHeight * 0.69F));
        Vector3f translation = new Vector3f(0.0F, preview ? entity.getBbHeight() * 0.24F : entity.getBbHeight() * 0.14F, 0.0F);
        Quaternionf bodyRotation = Axis.ZP.rotationDegrees(180.0F);
        bodyRotation.mul(Axis.YP.rotationDegrees(180.0F));
        bodyRotation.mul(Axis.XP.rotationDegrees(preview ? 5.0F : 3.0F));
        Quaternionf cameraRotation = new Quaternionf(bodyRotation).conjugate();
        InventoryScreen.renderEntityInInventory(guiGraphics, centerX, bottomY, scale, translation, cameraRotation, bodyRotation, entity);
    }

    private AbstractCityNpcEntity previewNpc(ResourceLocation texture) {
        if (previewNpc == null || previewNpc.level() != minecraft.level) {
            previewNpc = createPreviewNpc();
        }
        NpcCategory category = categoryForTexture(texture);
        previewNpc.applyClientPreviewIdentity(texture.toString(), category, currentNpcDisplayName(), currentNameColorId());
        previewNpc.setYRot(0.0F);
        previewNpc.setYBodyRot(0.0F);
        previewNpc.yBodyRotO = 0.0F;
        previewNpc.setYHeadRot(0.0F);
        previewNpc.yHeadRotO = 0.0F;
        previewNpc.setXRot(0.0F);
        previewNpc.xRotO = 0.0F;
        previewNpc.walkAnimation.setSpeed(0.0F);
        return previewNpc;
    }

    private AbstractCityNpcEntity createPreviewNpc() {
        return switch (menu.npcTypeId()) {
            case "ripperdoc" -> new RipperdocEntity(ModEntityTypes.RIPPERDOC.get(), minecraft.level);
            case "techie" -> new TechieEntity(ModEntityTypes.TECHIE.get(), minecraft.level);
            case "netrunner" -> new NetrunnerEntity(ModEntityTypes.NETRUNNER.get(), minecraft.level);
            case "med_tech" -> ModEntityTypes.MED_TECH.get().create(minecraft.level); case "merc" -> new MercEntity(ModEntityTypes.MERC.get(), minecraft.level);
            default -> new FixerEntity(ModEntityTypes.FIXER.get(), minecraft.level);
        };
    }

    private NpcCategory categoryForTexture(ResourceLocation texture) {
        String path = texture.getPath();
        if (path.contains("/female/")) {
            return NpcCategory.FEMALE;
        }
        if (path.contains("/male/")) {
            return NpcCategory.MALE;
        }
        return NpcCategory.GENERIC;
    }

    private Component npcText() {
        return switch (menu.responseId()) {
            case FixerDialogMenu.RESPONSE_INTRO -> Component.translatable(responseKey("intro"));
            case FixerDialogMenu.RESPONSE_STABILIZE_OK -> Component.translatable(responseKey("stabilize_ok"));
            case FixerDialogMenu.RESPONSE_NEED_FUNDS -> Component.translatable(responseKey("need_funds"));
            case FixerDialogMenu.RESPONSE_PURGE_LOCKED -> Component.translatable(responseKey("purge_locked"));
            case FixerDialogMenu.RESPONSE_PURGE_COOLDOWN -> Component.translatable(responseKey("purge_cooldown"));
            case FixerDialogMenu.RESPONSE_PURGE_OK -> Component.translatable(responseKey("purge_ok"));
            case FixerDialogMenu.RESPONSE_MAXDOC_OK -> Component.translatable(responseKey("stock_1_ok"));
            case FixerDialogMenu.RESPONSE_RAM_JOLT_OK -> Component.translatable(responseKey("stock_2_ok"));
            case FixerDialogMenu.RESPONSE_CHROME_SUPPRESSANT_OK -> Component.translatable(responseKey("stock_3_ok"));
            case FixerDialogMenu.RESPONSE_IMMUNOBLOCKERS_LOCKED -> Component.translatable(responseKey("stock_4_locked"));
            case FixerDialogMenu.RESPONSE_IMMUNOBLOCKERS_OK -> Component.translatable(responseKey("stock_4_ok"));
            case FixerDialogMenu.RESPONSE_COOL_OFF -> Component.translatable(responseKey("cool_off"));
            case FixerDialogMenu.RESPONSE_BUSY_STATE -> Component.translatable(responseKey("busy_state"));
            case FixerDialogMenu.RESPONSE_DEV_QUEST_UNAVAILABLE -> Component.translatable("screen.cyberneticenhancements.dev_test.quest_unavailable");
            case FixerDialogMenu.RESPONSE_CONTRACTS -> Component.translatable(responseKey("contracts"));
            case FixerDialogMenu.RESPONSE_CONTRACT_ACCEPTED -> Component.translatable(responseKey("contract_accepted"));
            case FixerDialogMenu.RESPONSE_CONTRACT_TURNED_IN -> Component.translatable(responseKey("contract_turned_in"));
            case FixerDialogMenu.RESPONSE_CONTRACT_NOT_READY -> Component.translatable(responseKey("contract_not_ready"));
            case FixerDialogMenu.RESPONSE_CONTRACT_MISSING_ITEMS -> Component.translatable(responseKey("contract_missing_items"));
            case FixerDialogMenu.RESPONSE_CONTRACT_ABANDONED -> Component.translatable(responseKey("contract_abandoned"));
            case FixerDialogMenu.RESPONSE_CONTRACT_MAX_ACTIVE -> Component.translatable(responseKey("contract_max_active"));
            case FixerDialogMenu.RESPONSE_CONTRACT_ALREADY_ACTIVE -> Component.translatable(responseKey("contract_already_active"));
            case FixerDialogMenu.RESPONSE_IDENTITY_LOCKED -> Component.translatable(responseKey("identity_locked"));
            case FixerDialogMenu.RESPONSE_IDENTITY_NICKNAME_OK -> Component.translatable(responseKey("identity_name_ok"));
            case FixerDialogMenu.RESPONSE_IDENTITY_APPEARANCE_OK -> Component.translatable(responseKey("identity_appearance_ok"));
            case FixerDialogMenu.RESPONSE_IDENTITY_COLOR_OK -> Component.translatable(responseKey("identity_color_ok"));
            case FixerDialogMenu.RESPONSE_IDENTITY_INVALID -> Component.translatable(responseKey("identity_invalid"));
            default -> Component.translatable(localNpcLineKey);
        };
    }

    private void drawWrappedText(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int lineCount = Math.min(maxLines, lines.size());
        for (int index = 0; index < lineCount; index++) {
            guiGraphics.drawString(font, lines.get(index), x, y + index * (font.lineHeight + 2), color, false);
        }
    }

    /** Wrapped text with every line in the middle of a card that starts at {@code cardX}. */
    private void drawCenteredWrapped(GuiGraphics guiGraphics, Component text, int cardX, int y, int wrapWidth, int cardWidth, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, wrapWidth);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            guiGraphics.drawString(font, lines.get(index), cardX + (cardWidth - font.width(lines.get(index))) / 2, y + index * (font.lineHeight + 2), color, false);
        }
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

    private void renderChoiceTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mode == ConversationMode.IDENTITY
                && nicknameField != null
                && nicknameField.isFocused()) {
            return;
        }
        for (ChoiceButton button : choiceButtons) {
            if (!button.visible || button.spec == null || !button.isHovered()) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(button.spec.categoryLabel().copy().withStyle(net.minecraft.ChatFormatting.AQUA));
            lines.add(button.spec.message());
            lines.add(Component.translatable("screen.cyberneticenhancements.npc.choice_hint." + button.spec.category())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            if (button.spec.tooltip() != null) lines.add(button.spec.tooltip());
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fill) {
        UiRenderer.panel(guiGraphics, x, y, width, height, fill);
    }

    private Component trustLabel() {
        return Component.translatable("screen.cyberneticenhancements.fixer.trust." + Math.max(0, Math.min(4, menu.trustLevel())));
    }

    private void drawTrustBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int progress = menu.trustProgressPercent();
        int fillWidth = Math.round((width - 2) * (progress / 100.0F));
        guiGraphics.fill(x, y, x + width, y + height, PANEL_DEEP);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_EDGE);
        guiGraphics.fill(x, y, x + 1, y + height, PANEL_EDGE);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, PANEL_EDGE);
        if (fillWidth > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, ACCENT);
        }
        for (int threshold : new int[]{20, 45, 70, 90}) {
            int markerX = x + 1 + Math.round((width - 2) * (threshold / 100.0F));
            int markerColor = progress >= threshold ? TEXT_PRIMARY : PANEL_EDGE;
            guiGraphics.fill(markerX, y + 1, markerX + 1, y + height - 1, markerColor);
        }
    }

    private Component discountLabel() {
        int discount = menu.trustDiscountPercent();
        return discount <= 0
                ? Component.translatable("screen.cyberneticenhancements.fixer.summary.discount.none")
                : Component.translatable("screen.cyberneticenhancements.fixer.summary.discount.value", discount);
    }

    private Component lockedTrustTooltip(int optionId) {
        return Component.translatable("screen.cyberneticenhancements.npc.locked.trust", menu.trustGateLabelForOption(optionId));
    }

    private Component modeLabel() {
        return switch (mode) {
            case SERVICES -> Component.translatable("screen.cyberneticenhancements.npc.mode." + menu.npcTypeId() + ".services");
            case SHOP -> Component.translatable("screen.cyberneticenhancements.npc.mode." + menu.npcTypeId() + ".shop");
            case CONTRACTS -> Component.translatable("screen.cyberneticenhancements.npc.mode." + menu.npcTypeId() + ".contracts");
            case IDENTITY -> Component.translatable("screen.cyberneticenhancements.npc.mode.identity");
        };
    }

    private int headerX() {
        return leftPos + CONTENT_PADDING;
    }

    private int headerY() {
        return topPos + CONTENT_PADDING;
    }

    private int headerWidth() {
        return imageWidth - CONTENT_PADDING * 2;
    }

    private int headerGap() {
        return PANEL_GAP;
    }

    private int headerHeight() {
        return HEADER_HEIGHT;
    }

    private int transcriptX() {
        return headerX();
    }

    private int transcriptY() {
        return headerY() + headerHeight() + headerGap();
    }

    private int transcriptWidth() {
        return headerWidth();
    }

    private int transcriptHeight() {
        return imageHeight - CONTENT_PADDING * 2 - headerHeight() - headerGap();
    }

    private int identityFieldX() {
        return transcriptX() + PANEL_TEXT_PADDING_X;
    }

    private int identityFieldY() {
        return transcriptY() + PANEL_TEXT_PADDING_Y + IDENTITY_FIELD_Y_OFFSET;
    }

    private int identityFieldWidth() {
        return Math.max(120, identityGalleryRight() - identityFieldX());
    }

    private int identityGalleryX() {
        return transcriptX() + PANEL_TEXT_PADDING_X;
    }

    private int identityGalleryRight() {
        return identityGalleryX() + identityGalleryContentWidth();
    }

    private int identityGalleryY() {
        return transcriptY() + PANEL_TEXT_PADDING_Y + IDENTITY_GALLERY_Y_OFFSET;
    }

    private int identityScrollbarX() {
        return identityGalleryX();
    }

    private int identityScrollbarY() {
        return actionButtonsStartY() - IDENTITY_SCROLLBAR_GAP - IDENTITY_SCROLLBAR_HEIGHT;
    }

    private int identityScrollbarWidth() {
        return identityGalleryContentWidth();
    }

    private int identityTileHeight() {
        int availableHeight = identityScrollbarY() - IDENTITY_SCROLLBAR_GAP - identityGalleryY();
        return Math.max(IDENTITY_TILE_MIN_HEIGHT, availableHeight);
    }

    private int identityTileWidth() {
        int availableWidth = transcriptWidth() - PANEL_TEXT_PADDING_X * 2;
        int totalGapWidth = (IDENTITY_VISIBLE_SKINS - 1) * IDENTITY_TILE_GAP;
        return Math.max(44, (availableWidth - totalGapWidth) / IDENTITY_VISIBLE_SKINS);
    }

    private int identityGalleryContentWidth() {
        return identityTileWidth() * IDENTITY_VISIBLE_SKINS + (IDENTITY_VISIBLE_SKINS - 1) * IDENTITY_TILE_GAP;
    }

    /** The tiles stretch so the visible rows fill the whole shop area instead of leaving an empty strip at the bottom. */
    private int shopTileHeight() {
        int available = shopRowsBottom() - shopRowsTop();
        int rows = Math.max(1, (available + SHOP_TILE_GAP) / (SHOP_TILE_HEIGHT + SHOP_TILE_GAP));
        return Math.max(SHOP_TILE_HEIGHT, (available - SHOP_TILE_GAP * (rows - 1)) / rows);
    }

    private int shopRowsTop() {
        return contentTop();
    }

    private int shopRowsBottom() {
        return contentBottom();
    }

    private ShopGridLayout shopLayout() {
        return new ShopGridLayout(shopItemCount(), transcriptWidth() - PANEL_TEXT_PADDING_X * 2,
                shopRowsBottom() - shopRowsTop(), SHOP_TILE_WIDTH, shopTileHeight(), SHOP_TILE_GAP, shopScrollbarWidth());
    }

    private int shopGridWidth() {
        return shopLayout().contentWidth();
    }

    private int shopColumns() {
        return shopLayout().columns();
    }

    private int shopTileWidth() {
        return shopLayout().tileWidth();
    }

    private int shopTileX(int slot) {
        return transcriptX() + PANEL_TEXT_PADDING_X + shopLayout().tileX(slot);
    }

    private int shopTileY(int slot) {
        return shopRowsTop() + shopLayout().tileY(slot, shopScrollOffset);
    }

    private int shopItemCount() {
        return menu.shopOffers().size();
    }

    private int visibleShopRowCount() {
        return shopLayout().visibleRows();
    }

    private int maxShopScrollOffset() {
        return shopLayout().maxScroll();
    }

    private void scrollShop(int delta) {
        shopScrollOffset = Math.max(0, Math.min(maxShopScrollOffset(), shopScrollOffset + delta));
    }

    private int shopScrollbarX() {
        return transcriptX() + transcriptWidth() - PANEL_TEXT_PADDING_X - shopScrollbarWidth();
    }

    private int shopScrollbarY() {
        return shopRowsTop();
    }

    private int shopScrollbarWidth() {
        return UiScrollbar.WIDTH;
    }

    private int shopScrollbarHeight() {
        return Math.max(18, shopRowsBottom() - shopRowsTop());
    }

    private boolean clickedShopScrollbar(double mouseX,double mouseY) {
        var track=shopTrack();if(!shopScrollbar.begin(track,mouseX,mouseY))return false;
        shopScrollOffset=(int)Math.round(shopScrollbar.value(track,mouseX,mouseY));return true;
    }
    private int actionButtonsX() {
        return transcriptX() + PANEL_TEXT_PADDING_X;
    }

    private int actionButtonsWidth() {
        return transcriptWidth() - PANEL_TEXT_PADDING_X * 2;
    }

    private int actionButtonsStartY() {
        return transcriptY() + transcriptHeight() - PANEL_TEXT_PADDING_Y - actionButtonsHeight();
    }

    private int actionButtonsHeight() {
        int buttonCount = visibleActionButtonCount();
        return buttonCount * CHOICE_BUTTON_HEIGHT + Math.max(0, buttonCount - 1) * CHOICE_BUTTON_GAP;
    }

    private int visibleActionButtonCount() {
        if (mode == ConversationMode.IDENTITY || devQuestPicker()) return Math.max(1, currentChoices().size());
        return SHOP_VISIBLE_ACTIONS;
    }

    private enum ConversationMode {
        SERVICES("screen.cyberneticenhancements.fixer.mode.services"),
        SHOP("screen.cyberneticenhancements.fixer.mode.shop"),
        CONTRACTS("screen.cyberneticenhancements.fixer.mode.contracts"),
        IDENTITY("screen.cyberneticenhancements.npc.mode.identity");

        private final String labelKey;

        ConversationMode(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private enum ChoiceIcon {
        TALK(Icons.DIALOGUE),
        INTEL(Icons.INFO),
        QUEST(Icons.QUEST),
        SHOP(Icons.SHOP),
        TRADE(Icons.TRADE),
        SERVICE(Icons.CRAFTING),
        IDENTITY(Icons.SETTINGS),
        RESET(Icons.RESET),
        BACK(Icons.BACK);

        private final Icons icon;

        ChoiceIcon(Icons icon) {
            this.icon = icon;
        }
    }

    private record ChoiceSpec(ChoiceIcon icon, Component message, boolean enabled, Component tooltip, Runnable action,
                              @org.jetbrains.annotations.Nullable Component key) {
        private String category() {
            return switch (icon) {
                case TALK -> "talk";
                case INTEL -> "intel";
                case QUEST -> "quests";
                case SHOP -> "shop";
                case TRADE -> "buy";
                case SERVICE -> "services";
                case IDENTITY, RESET -> "customize";
                case BACK -> "navigate";
            };
        }

        private boolean interactive() {
            return icon != ChoiceIcon.TALK && icon != ChoiceIcon.BACK;
        }

        private Component categoryLabel() {
            return Component.translatable("screen.cyberneticenhancements.npc.choice_type." + category());
        }
    }

    private final class ChoiceButton extends Button {
        private final UiMotion highlight = new UiMotion();
        private ChoiceSpec spec;

        private ChoiceButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), button -> {
                if (((ChoiceButton) button).spec != null) {
                    ((ChoiceButton) button).spec.action().run();
                }
            }, DEFAULT_NARRATION);
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
            if (spec == null) return super.createNarrationMessage();
            return Component.translatable("gui.narrate.button", spec.categoryLabel().copy().append(": ").append(getMessage()));
        }
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            UiRenderer.action(guiGraphics, getX(), getY(), width, height, active, highlight.update(active && UiRenderer.highlighted(this)));
            int contentInset = Math.max(3, (height - font.lineHeight) / 2);
            int textY = getY() + contentInset;
            int iconY = getY() + Math.max(0, (height - font.lineHeight) / 2) + CHOICE_ICON_Y_OFFSET;
            int textX = getX() + contentInset;
            int availableWidth = width - contentInset * 2;
            if (spec != null && spec.key() != null) {
                int keyWidth = font.width(spec.key()) + 6, keyHeight = Math.min(12, height - 4), keyX = getX() + width - contentInset - keyWidth, keyY = getY() + (height - keyHeight) / 2;
                UiRenderer.panel(guiGraphics, keyX, keyY, keyWidth, keyHeight, UiRenderer.ROW_DISABLED, UiRenderer.PANEL_EDGE_ACTIVE);
                guiGraphics.drawString(font, spec.key(), keyX + 3, keyY + (keyHeight - font.lineHeight) / 2 + 1, ACCENT, false);
                availableWidth -= keyWidth + contentInset;
            }
            if (spec != null) {
                textX += 2;
                availableWidth -= 2;
            }
            if (spec != null && spec.icon() != null) {
                Component iconComponent = spec.icon().icon.component();
                guiGraphics.drawString(font, iconComponent, textX, iconY, active && spec.interactive() ? ACCENT : TEXT_SECONDARY, false);
                int iconWidth = font.width(iconComponent);
                textX += iconWidth + contentInset;
                availableWidth -= iconWidth + contentInset;
            }
            Component message = getMessage();
            if (font.width(message) > availableWidth) {
                guiGraphics.drawString(font, trimStyled(message, availableWidth), textX, textY, active ? TEXT_PRIMARY : TEXT_SECONDARY, false);
                return;
            }
            guiGraphics.drawString(font, message, textX, textY, active ? TEXT_PRIMARY : TEXT_SECONDARY, false);
        }

    }

}
