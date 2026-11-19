package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestDimension;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueChoice;
import de.artemis.cyberneticenhancements.common.dialogue.NpcService;
import de.artemis.cyberneticenhancements.common.dialogue.NpcShopCatalog;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueNode;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueTree;
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
    private static final int STATUS_HEIGHT = 52;
    private static final int CHOICE_BUTTON_HEIGHT = 18;
    private static final int CHOICE_BUTTON_GAP = 4;
    private static final int CHOICE_ICON_Y_OFFSET = 1;
    private static final int TRANSCRIPT_TITLE_GAP = 8;
    private static final int TRANSCRIPT_BUBBLE_GAP = 8;
    private static final int TRUST_BAR_HEIGHT = 8;
    private static final int SHOP_TILE_GAP = 10;
    private static final int SHOP_TILE_WIDTH = 136;
    private static final int SHOP_TILE_HEIGHT = 156;
    private static final int SHOP_DETAIL_GAP = 8;
    private static final int SHOP_VISIBLE_ACTIONS = 1;
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
    private EditBox nicknameField;
    private ConversationMode mode = ConversationMode.DIALOGUE;
    private String lastPlayerLineKey;
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
        this.lastPlayerLineKey = playerKey("open");
        this.localNpcLineKey = greetingKey();
        if (menu.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH) {
            // Their one request is all they have to offer, so it is laid out at once instead of behind a choice.
            this.mode = ConversationMode.CONTRACTS;
            this.lastPlayerLineKey = "screen.cyberneticenhancements.npc.dialogue.world_encounter.player.work";
            this.localNpcLineKey = "screen.cyberneticenhancements.npc.dialogue.world_encounter.line.work";
        }
    }

    @Override
    protected void init() {
        String nicknameDraft = nicknameField == null ? null : nicknameField.getValue();
        super.init();
        if (minecraft.player != null) minecraft.player.stopUsingItem();
        minecraft.options.keyUse.setDown(false);
        choiceButtons.clear();
        shopBuyButtons.clear();
        serviceBuyButtons.clear();
        for (NpcService service : NpcService.forNpc(menu.npcType())) {
            UiButton button = new UiButton(0, 0, 1, CHOICE_BUTTON_HEIGHT, Component.empty(), pressed ->
                    sendOption(service.optionId(), playerKey(service.optionId() == FixerDialogMenu.OPTION_STABILIZE ? "service_stabilize" : "service_purge")));
            button.visible = false;
            serviceBuyButtons.add(button);
            addRenderableWidget(button);
        }
        for (int slot = 0; slot < shopItemCount(); slot++) {
            final int itemSlot = slot;
            UiButton button = new UiButton(0, 0, 1, CHOICE_BUTTON_HEIGHT, Component.empty(), pressed -> {
                sendOption(-itemSlot - 1, playerKey("shop"));
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
        if (!menu.temporary()) drawPanel(guiGraphics, headerX(), headerY(), headerWidth(), headerHeight(), PANEL_ALT);
        if (mode == ConversationMode.CONTRACTS) {
            drawPanel(guiGraphics, statusX(), statusY(), statusWidth(), statusHeight(), PANEL_ALT);
        }
        drawPanel(guiGraphics, transcriptX(), transcriptY(), transcriptWidth(), transcriptHeight(), PANEL_ALT);
        if (mode != ConversationMode.IDENTITY && mode != ConversationMode.SHOP && mode != ConversationMode.SERVICES) {
            drawPanel(guiGraphics, choicesX(), choicesY(), choicesWidth(), choicesHeight(), PANEL_ALT);
        }

        if (!menu.temporary()) renderHeader(guiGraphics);
        if (mode == ConversationMode.CONTRACTS) {
            renderStatus(guiGraphics);
        }
        renderTranscript(guiGraphics);
        if (mode != ConversationMode.IDENTITY && mode != ConversationMode.SHOP && mode != ConversationMode.SERVICES) {
            renderChoiceHeader(guiGraphics);
        }
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
            int y = shopTileY(slot) + SHOP_TILE_HEIGHT - CHOICE_BUTTON_HEIGHT - 8;
            boolean visible = mode == ConversationMode.SHOP && menu.shopOfferAvailable(slot)
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
            case DIALOGUE -> dialogueChoices();
            case SERVICES -> serviceChoices();
            case SHOP -> shopChoices();
            case CONTRACTS -> contractChoices();
            case IDENTITY -> identityChoices();
        };
    }

    private List<ChoiceSpec> dialogueChoices() {
        NpcDialogueNode node = currentDialogueNode();
        List<ChoiceSpec> choices = new ArrayList<>();
        for (NpcDialogueChoice choice : node.choices()) {
            choices.add(customChoice(
                    iconForDialogueChoice(choice),
                    Component.translatable(choice.translationKey()),
                    true,
                    () -> runDialogueChoice(choice)
            ));
        }
        if (menu.temporary()) return choices;
        boolean identityUnlocked = menu.identityUnlocked();
        choices.add(customChoice(
                ChoiceIcon.IDENTITY,
                Component.translatable("screen.cyberneticenhancements.npc.choice.identity"),
                identityUnlocked,
                identityUnlocked ? null : Component.translatable("screen.cyberneticenhancements.npc.identity.locked"),
                this::openIdentityMode
        ));
        return choices;
    }

    private ChoiceIcon iconForDialogueChoice(NpcDialogueChoice choice) {
        return switch (choice.view()) {
            case SHOP -> ChoiceIcon.SHOP;
            case SERVICES -> ChoiceIcon.SERVICE;
            case CONTRACTS -> ChoiceIcon.QUEST;
            case CACHE_INTEL -> ChoiceIcon.INTEL;
            case CLOSE -> ChoiceIcon.BACK;
            case DIALOGUE -> ChoiceIcon.TALK;
        };
    }

    private List<ChoiceSpec> serviceChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
        return choices;
    }

    private List<ChoiceSpec> shopChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
        return choices;
    }

    private List<ChoiceSpec> contractChoices() {
        if (menu.isDevTest() && !menu.hasActiveContract()) return devQuestChoices();
        List<ChoiceSpec> choices = new ArrayList<>();
        if (menu.offersPreparing() && !menu.hasActiveContract())
            choices.add(customChoice(ChoiceIcon.QUEST, Component.translatable("screen.cyberneticenhancements.fixer.contracts_preparing"), false, () -> {}));
        if (menu.hasActiveContract()) {
            choices.add(customChoice(
                    ChoiceIcon.QUEST,
                    Component.translatable("screen.cyberneticenhancements.fixer.choice.contract.turn_in", menu.activeContractTitle()),
                    menu.activeContractReady(),
                    () -> sendOption(FixerDialogMenu.OPTION_TURN_IN_CONTRACT, playerKey("turn_in_contract"))
            ));
            choices.add(customChoice(
                    ChoiceIcon.QUEST,
                    Component.translatable("screen.cyberneticenhancements.fixer.choice.contract.abandon", menu.activeContractTitle()),
                    true,
                    () -> sendOption(FixerDialogMenu.OPTION_ABANDON_CONTRACT, playerKey("abandon_contract"))
            ));
        } else {
            for (int slot = 0; slot < menu.offerSlots(); slot++) {
                if (!menu.offerPresent(slot)) {
                    continue;
                }
                final int offerSlot = slot;
                choices.add(customChoice(
                        ChoiceIcon.QUEST,
                        Component.translatable("screen.cyberneticenhancements.fixer.choice.contract.accept", menu.offerTitle(slot), menu.offerRewardMoney(slot)),
                        true,
                        () -> sendOption(menu.acceptOfferOption(offerSlot), playerKey("accept_contract"))
                ));
            }
        }
        boolean encounter = menu.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH;
        // With no contract to take or hand in, a leader who has withdrawn the offer says so rather than showing nothing.
        if (encounter && choices.isEmpty())
            choices.add(customChoice(ChoiceIcon.QUEST, Component.translatable("screen.cyberneticenhancements.fixer.contracts_withheld"), false, () -> {}));
        String backPlayer = encounter ? "screen.cyberneticenhancements.npc.dialogue.world_encounter.player.back" : playerKey("back");
        String backNpc = encounter ? currentNpcLineKey() : npcLineKey("back");
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", backPlayer, backNpc, () -> switchMode(ConversationMode.DIALOGUE, backPlayer, backNpc)));
        return choices;
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
            choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"),
                    npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
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
            choices.add(customChoice(ChoiceIcon.QUEST, title, true, () -> sendOption(option, playerKey("accept_contract"))));
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
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
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

    private String playerKey(String suffix) {
        return "screen.cyberneticenhancements.npc.player." + menu.npcTypeId() + "." + suffix;
    }

    private String npcLineKey(String suffix) {
        return "screen.cyberneticenhancements.npc.line." + menu.npcTypeId() + "." + suffix;
    }

    private String responseKey(String suffix) {
        return "screen.cyberneticenhancements.npc.response." + menu.npcTypeId() + "." + suffix;
    }

    private ChoiceSpec localChoice(ChoiceIcon icon, String labelKey, String playerLineKey, String npcLineKey, Runnable action) {
        return new ChoiceSpec(icon, Component.translatable(labelKey), true, null, () -> {
            lastPlayerLineKey = playerLineKey;
            localNpcLineKey = npcLineKey;
            action.run();
        });
    }

    private ChoiceSpec customChoice(ChoiceIcon icon, Component message, boolean enabled, Runnable action) {
        return customChoice(icon, message, enabled, null, action);
    }

    private ChoiceSpec customChoice(ChoiceIcon icon, Component message, boolean enabled, Component tooltip, Runnable action) {
        return new ChoiceSpec(icon, message, enabled, tooltip, action);
    }

    private void switchMode(ConversationMode newMode, String playerLineKey, String npcLineKey) {
        mode = newMode;
        lastPlayerLineKey = playerLineKey;
        localNpcLineKey = npcLineKey;
        if (newMode == ConversationMode.SHOP) {
            clampShopScroll();
        }
        refreshChoices();
    }

    private void sendOption(int optionId, String playerLineKey) {
        lastPlayerLineKey = playerLineKey;
        PacketDistributor.sendToServer(new FixerDialogueChoicePayload(optionId));
    }

    private void openIdentityMode() {
        mode = ConversationMode.IDENTITY;
        lastPlayerLineKey = playerKey("identity");
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

    private void runDialogueChoice(NpcDialogueChoice choice) {
        lastPlayerLineKey = choice.playerLineKey();
        localNpcLineKey = currentNpcLineKey();
        switch (choice.view()) {
            case SERVICES -> {
                mode = ConversationMode.SERVICES;
                localNpcLineKey = npcLineKey("services");
            }
            case SHOP -> {
                mode = ConversationMode.SHOP;
                localNpcLineKey = npcLineKey("shop");
            }
            case CONTRACTS -> {
                mode = ConversationMode.CONTRACTS;
                localNpcLineKey = npcLineKey("contracts");
            }
            case DIALOGUE, CLOSE, CACHE_INTEL -> mode = ConversationMode.DIALOGUE;
        }
        PacketDistributor.sendToServer(new FixerDialogueChoicePayload(choice.id()));
        refreshChoices();
    }

    private NpcDialogueTree dialogueTree() {
        return menu.dialogueTree();
    }

    private NpcDialogueNode currentDialogueNode() {
        return dialogueTree().node(menu.currentDialogueNodeId());
    }

    private String currentNpcLineKey() {
        return currentDialogueNode().npcLineKey();
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
        // A passing street doctor has nothing to track: no trust, discount or second title line repeating the name.
        if (menu.temporary()) return;
        guiGraphics.drawString(font, menu.npcTypeLabel(), x, y + 12, ACCENT, false);
        Component trust = Component.translatable("screen.cyberneticenhancements.fixer.trust", trustLabel());
        Component discount = Component.translatable("screen.cyberneticenhancements.fixer.summary.discount", discountLabel());
        int rightX = headerX() + headerWidth() - PANEL_TEXT_PADDING_X;
        guiGraphics.drawString(font, trust, rightX - font.width(trust), y, TEXT_SECONDARY, false);
        guiGraphics.drawString(font, discount, rightX - font.width(discount), y + 12, ACCENT, false);
        drawTrustBar(guiGraphics, x, headerY() + 32, headerWidth() - PANEL_TEXT_PADDING_X * 2, TRUST_BAR_HEIGHT);
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        int x = statusX() + PANEL_TEXT_PADDING_X;
        int headerY = statusY() + PANEL_TEXT_PADDING_Y;
        int rowY = headerY + font.lineHeight + 8;
        if (mode == ConversationMode.CONTRACTS) {
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.section.contract_status"), x, headerY, TEXT_PRIMARY, false);
            if (menu.hasActiveContract()) {
                Component titleLine = Component.translatable("screen.cyberneticenhancements.fixer.contract.active", menu.activeContractTitle());
                Component objectiveLine = menu.activeContractObjective();
                Component rewardLine = Component.translatable("screen.cyberneticenhancements.fixer.contract.reward", menu.activeContractRewardSummary());
                Component locationLine = menu.activeContractLocationSummary();
                guiGraphics.drawString(font, trimStyled(titleLine, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY, TEXT_SECONDARY, false);
                guiGraphics.drawString(font, trimStyled(objectiveLine, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 12, TEXT_SECONDARY, false);
                Component thirdLine = locationLine.getString().isBlank() ? rewardLine : locationLine;
                int thirdColor = locationLine.getString().isBlank() ? ACCENT : StationScreenStyle.TEXT_PRIMARY;
                guiGraphics.drawString(font, trimStyled(thirdLine, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 24, thirdColor, false);
            } else {
                Component titleLine = menu.isDevTest() && devQuestDimension != null ? devQuestDimension.displayName() : menu.contractBoardTitle();
                Component rewardLine = menu.contractBoardNote();
                guiGraphics.drawString(font, titleLine, x, rowY, TEXT_SECONDARY, false);
                guiGraphics.drawString(font, trimStyled(rewardLine, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 12, ACCENT, false);
            }
            return;
        }
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
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.fixer.section.transcript"), x, y, TEXT_PRIMARY, false);

        int bubbleWidth = transcriptWidth() - PANEL_TEXT_PADDING_X * 2;
        int contentTop = y + font.lineHeight + TRANSCRIPT_TITLE_GAP;
        int contentBottom = transcriptY() + transcriptHeight() - PANEL_TEXT_PADDING_Y;
        int availableBubbleHeight = Math.max(80, contentBottom - contentTop - TRANSCRIPT_BUBBLE_GAP);
        int bubbleHeight = availableBubbleHeight / 2;
        int secondBubbleY = contentTop + bubbleHeight + TRANSCRIPT_BUBBLE_GAP;

        drawSpeakerBlock(guiGraphics, Component.translatable("screen.cyberneticenhancements.fixer.speaker.player"), Component.translatable(lastPlayerLineKey), x, contentTop, bubbleWidth, bubbleHeight, 0xFF15252D, TEXT_PRIMARY);
        drawSpeakerBlock(guiGraphics, Component.literal(currentNpcDisplayName()), npcText(), x, secondBubbleY, bubbleWidth, bubbleHeight, 0xFF162E2A, currentNameColorRgb());
    }

    private int serviceCardX(int slot) {
        return transcriptX() + PANEL_TEXT_PADDING_X + slot * (serviceCardWidth() + SHOP_TILE_GAP);
    }

    private int serviceCardWidth() {
        return (transcriptWidth() - PANEL_TEXT_PADDING_X * 2 - SHOP_TILE_GAP) / 2;
    }

    private int serviceCardY() {
        return transcriptY() + 48;
    }

    private int serviceCardHeight() {
        return actionButtonsStartY() - 32 - serviceCardY();
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
        drawWrappedText(graphics, Component.translatable("screen.cyberneticenhancements.npc.service.subtitle"),
                x, y + 14, transcriptWidth() - PANEL_TEXT_PADDING_X * 2, TEXT_SECONDARY, 2);
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
            drawWrappedText(graphics, Component.translatable(service.descriptionKey()), cx + 10, cy + 68, width - 20, TEXT_SECONDARY, 3);
            int lineY = cy + 110;
            for (Component detail : service.details()) {
                for (var line : font.split(detail, width - 20)) {
                    graphics.drawString(font, line, cx + 10, lineY, TEXT_PRIMARY, false);
                    lineY += 12;
                }
            }
            drawWrappedText(graphics, Component.translatable(service.daily()
                            ? "screen.cyberneticenhancements.npc.service.daily" : "screen.cyberneticenhancements.npc.service.refresh"),
                    cx + 10, cy + height - 78, width - 20, TEXT_SECONDARY, 2);
            Component state;
            boolean ready = false;
            if (!menu.isOptionAvailable(service.optionId())) state = lockedTrustTooltip(service.optionId());
            else if (service.daily() && !menu.purgeReadyClient()) state = Component.translatable("screen.cyberneticenhancements.npc.service.used_today");
            else if (menu.balance() < menu.priceForOption(service.optionId())) state = Component.translatable("screen.cyberneticenhancements.npc.service.need_funds");
            else {
                state = Component.translatable("screen.cyberneticenhancements.npc.service.ready");
                ready = true;
            }
            drawWrappedText(graphics, state, cx + 10, cy + height - 50, width - 20, ready ? ACCENT : TEXT_SECONDARY, 2);
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
        drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.npc.section.shop_catalog.subtitle"), x, y + 12, width, TEXT_SECONDARY, 1);

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
        UiRenderer.control(guiGraphics, x, y, width, SHOP_TILE_HEIGHT, available, false, false, 0xFFFFFFFF);
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
            guiGraphics.drawString(font, priceLine, x + (width - font.width(priceLine)) / 2, y + 116, ACCENT, false);
            var locked = trimStyled(Component.translatable("screen.cyberneticenhancements.npc.shop.state.locked", menu.shopTrustLabel(slot)), width - 14);
            guiGraphics.drawString(font, locked, x + (width - font.width(locked)) / 2, y + 136, 0xFFD08A8A, false);
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

    private void drawSpeakerBlock(GuiGraphics guiGraphics, Component speaker, Component line, int x, int y, int width, int height, int fill, int speakerColor) {
        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_DEEP);
        guiGraphics.drawString(font, speaker, x + 8, y + 6, speakerColor, false);
        int availableTextHeight = Math.max(0, height - 22);
        int maxLines = Math.max(1, Math.min(3, availableTextHeight / (font.lineHeight + 2)));
        drawWrappedText(guiGraphics, line, x + 8, y + 18, width - 16, TEXT_SECONDARY, maxLines);
    }

    private void renderChoiceHeader(GuiGraphics guiGraphics) {
        int x = choicesX() + PANEL_TEXT_PADDING_X;
        int y = choicesY() + PANEL_TEXT_PADDING_Y;
        Component header = mode == ConversationMode.DIALOGUE
                ? Component.translatable("screen.cyberneticenhancements.npc.section.choices." + menu.npcTypeId())
                : modeLabel();
        guiGraphics.drawString(font, header, x, y, TEXT_PRIMARY, false);
    }

    private Component npcText() {
        if (mode == ConversationMode.DIALOGUE) {
            if (menu.responseId() == FixerDialogMenu.RESPONSE_CACHE_INTEL) return Component.translatable(responseKey("cache_intel"));
            if (menu.responseId() == FixerDialogMenu.RESPONSE_NO_CACHE_INTEL) return Component.translatable("message.cyberneticenhancements.cache_intel.none");
            return Component.translatable(currentNpcLineKey());
        }
        return switch (menu.responseId()) {
            case FixerDialogMenu.RESPONSE_INTRO -> Component.translatable(responseKey("intro"));
            case FixerDialogMenu.RESPONSE_CACHE_INTEL -> Component.translatable(responseKey("cache_intel"));
            case FixerDialogMenu.RESPONSE_NO_CACHE_INTEL -> Component.translatable("message.cyberneticenhancements.cache_intel.none");
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
            case DIALOGUE -> Component.translatable("screen.cyberneticenhancements.npc.section.choices." + menu.npcTypeId());
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

    /** A temporary NPC has no header panel (its name is already the window title), so no gap below it either. */
    private int headerGap() {
        return menu.temporary() ? 0 : PANEL_GAP;
    }

    private int headerHeight() {
        return menu.temporary() ? 0 : HEADER_HEIGHT;
    }

    private int statusX() {
        return headerX();
    }

    private int statusY() {
        return headerY() + headerHeight() + headerGap();
    }

    private int statusWidth() {
        return headerWidth();
    }

    private int statusHeight() {
        return STATUS_HEIGHT;
    }

    private int transcriptX() {
        return headerX();
    }

    private int transcriptY() {
        return mode == ConversationMode.CONTRACTS ? statusY() + statusHeight() + PANEL_GAP : statusY();
    }

    private int transcriptWidth() {
        return headerWidth();
    }

    private int transcriptHeight() {
        int baseHeight = imageHeight - CONTENT_PADDING * 2 - headerHeight() - headerGap();
        if (mode == ConversationMode.CONTRACTS) baseHeight -= STATUS_HEIGHT + PANEL_GAP;
        return switch (mode) {
            case IDENTITY -> baseHeight;
            case SHOP, SERVICES -> baseHeight;
            default -> baseHeight - choicesHeight() - PANEL_GAP;
        };
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

    private int choicesX() {
        return headerX();
    }

    private int choicesY() {
        return transcriptY() + transcriptHeight() + PANEL_GAP;
    }

    private int choicesWidth() {
        return headerWidth();
    }

    private int choicesHeight() {
        return PANEL_TEXT_PADDING_Y * 2 + font.lineHeight + PANEL_GAP + actionButtonsHeight();
    }

    private int shopRowsTop() {
        return transcriptY() + PANEL_TEXT_PADDING_Y + 12 + font.lineHeight + SHOP_DETAIL_GAP;
    }

    private int shopRowsBottom() {
        return actionButtonsStartY() - SHOP_DETAIL_GAP;
    }

    private ShopGridLayout shopLayout() {
        return new ShopGridLayout(shopItemCount(), transcriptWidth() - PANEL_TEXT_PADDING_X * 2,
                shopRowsBottom() - shopRowsTop(), SHOP_TILE_WIDTH, SHOP_TILE_HEIGHT, SHOP_TILE_GAP, shopScrollbarWidth());
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
        return (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP || mode == ConversationMode.SERVICES)
                ? transcriptX() + PANEL_TEXT_PADDING_X
                : choicesX() + PANEL_TEXT_PADDING_X;
    }

    private int actionButtonsWidth() {
        return (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP || mode == ConversationMode.SERVICES)
                ? transcriptWidth() - PANEL_TEXT_PADDING_X * 2
                : choicesWidth() - PANEL_TEXT_PADDING_X * 2;
    }

    private int actionButtonsStartY() {
        int totalHeight = actionButtonsHeight();
        if (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP || mode == ConversationMode.SERVICES) {
            return transcriptY() + transcriptHeight() - PANEL_TEXT_PADDING_Y - totalHeight;
        }
        return choicesY() + choicesHeight() - PANEL_TEXT_PADDING_Y - totalHeight;
    }

    private int actionButtonsHeight() {
        int buttonCount = visibleActionButtonCount();
        return buttonCount * CHOICE_BUTTON_HEIGHT + Math.max(0, buttonCount - 1) * CHOICE_BUTTON_GAP;
    }

    private int visibleActionButtonCount() {
        if (mode == ConversationMode.SHOP || mode == ConversationMode.SERVICES) {
            return SHOP_VISIBLE_ACTIONS;
        }
        return Math.max(1, currentChoices().size());
    }

    private enum ConversationMode {
        DIALOGUE("screen.cyberneticenhancements.fixer.mode.dialogue"),
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

    private record ChoiceSpec(ChoiceIcon icon, Component message, boolean enabled, Component tooltip, Runnable action) {
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
