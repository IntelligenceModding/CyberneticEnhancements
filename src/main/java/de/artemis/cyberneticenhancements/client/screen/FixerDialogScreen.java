package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueChoice;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueNode;
import de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueTree;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.FixerEntity;
import de.artemis.cyberneticenhancements.common.entity.MercEntity;
import de.artemis.cyberneticenhancements.common.entity.NetrunnerEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.entity.RipperdocEntity;
import de.artemis.cyberneticenhancements.common.entity.TechieEntity;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.network.FixerDialogueChoicePayload;
import de.artemis.cyberneticenhancements.common.network.NpcIdentityAppearancePayload;
import de.artemis.cyberneticenhancements.common.network.NpcIdentityNameColorPayload;
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
    private static final int CHOICES_HEIGHT = 146;
    private static final int CHOICE_BUTTON_HEIGHT = 18;
    private static final int CHOICE_BUTTON_GAP = 4;
    private static final int CHOICE_ICON_Y_OFFSET = 1;
    private static final int TRANSCRIPT_TITLE_GAP = 8;
    private static final int TRANSCRIPT_BUBBLE_GAP = 8;
    private static final int TRUST_BAR_HEIGHT = 8;
    private static final int SHOP_ROW_GAP = 2;
    private static final int SHOP_ROW_MIN_HEIGHT = 24;
    private static final int SHOP_ICON_BOX_SIZE = 16;
    private static final int SHOP_DETAIL_GAP = 8;
    private static final int SHOP_MAX_VISIBLE_ROWS = 6;
    private static final int SHOP_VISIBLE_ACTIONS = 2;
    private static final int IDENTITY_LABEL_Y_OFFSET = 28;
    private static final int IDENTITY_FIELD_Y_OFFSET = 40;
    private static final int IDENTITY_GALLERY_Y_OFFSET = 68;
    private static final int IDENTITY_COLOR_BUTTON_SIZE = 18;
    private static final int IDENTITY_COLOR_BUTTON_GAP = 4;
    private static final int IDENTITY_TILE_MIN_HEIGHT = 70;
    private static final int IDENTITY_TILE_GAP = 8;
    private static final int IDENTITY_VISIBLE_SKINS = 4;
    private static final int IDENTITY_SCROLLBAR_HEIGHT = UiScrollbar.WIDTH;
    private static final int IDENTITY_SCROLLBAR_GAP = 10;
    private static final int IMAGE_W = 640;
    private static final int IMAGE_H = 484;

    private final List<ChoiceButton> choiceButtons = new ArrayList<>();
    private final List<ColorPresetButton> colorButtons = new ArrayList<>();
    private static final NameColorPreset[] NAME_COLOR_PRESETS = new NameColorPreset[] {
            new NameColorPreset("aqua", 0xFF4FE9E2),
            new NameColorPreset("blue", 0xFF5D86F1),
            new NameColorPreset("gold", 0xFFF0C04D),
            new NameColorPreset("green", 0xFF57C26E),
            new NameColorPreset("red", 0xFFD65D5D),
            new NameColorPreset("light_purple", 0xFFD88FF7),
            new NameColorPreset("white", 0xFFE8EDF2),
            new NameColorPreset("hidden", 0x00000000)
    };
    private EditBox nicknameField;
    private ConversationMode mode = ConversationMode.DIALOGUE;
    private String lastPlayerLineKey;
    private String localNpcLineKey;
    private String selectedAppearanceId = "";
    private String selectedNameColorId = "aqua";
    private final UiScrollbar shopScrollbar = new UiScrollbar(), identityScrollbar = new UiScrollbar();
    private int skinPage;
    private int selectedShopSlot;
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
    }

    @Override
    protected void init() {
        String nicknameDraft = nicknameField == null ? null : nicknameField.getValue();
        super.init();
        choiceButtons.clear();
        nicknameField = new UiEditBox(font, identityFieldX(), identityFieldY(), identityFieldWidth(), 18, Component.translatable("screen.cyberneticenhancements.npc.identity.nickname"));
        nicknameField.setMaxLength(32);
        nicknameField.setVisible(false);
        addRenderableWidget(nicknameField);
        colorButtons.clear();
        for (int index = 0; index < NAME_COLOR_PRESETS.length; index++) {
            NameColorPreset preset = NAME_COLOR_PRESETS[index];
            ColorPresetButton button = new ColorPresetButton(0, 0, IDENTITY_COLOR_BUTTON_SIZE, IDENTITY_COLOR_BUTTON_SIZE, preset);
            colorButtons.add(button);
            addRenderableWidget(button);
        }
        int buttonX = actionButtonsX();
        int buttonWidth = actionButtonsWidth();
        int startY = actionButtonsStartY();
        for (int index = 0; index < 5; index++) {
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
        for (int index = 0; index < colorButtons.size(); index++) {
            ColorPresetButton button = colorButtons.get(index);
            button.setX(identityColorButtonsX() + index * (IDENTITY_COLOR_BUTTON_SIZE + IDENTITY_COLOR_BUTTON_GAP));
            button.setY(identityFieldY());
            button.visible = mode == ConversationMode.IDENTITY;
            button.active = menu.identityUnlocked();
            button.selected = button.preset.colorId().equals(selectedNameColorId);
        }
        int buttonX = actionButtonsX();
        int buttonWidth = actionButtonsWidth();
        int startY = actionButtonsStartY();
        for (int index = 0; index < choiceButtons.size(); index++) {
            ChoiceButton button = choiceButtons.get(index);
            button.setX(buttonX);
            button.setY(startY + index * (CHOICE_BUTTON_HEIGHT + CHOICE_BUTTON_GAP));
            button.setWidth(buttonWidth);
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
        if (mode != ConversationMode.IDENTITY) {
            drawPanel(guiGraphics, statusX(), statusY(), statusWidth(), statusHeight(), PANEL_ALT);
        }
        drawPanel(guiGraphics, transcriptX(), transcriptY(), transcriptWidth(), transcriptHeight(), PANEL_ALT);
        if (mode != ConversationMode.IDENTITY && mode != ConversationMode.SHOP) {
            drawPanel(guiGraphics, choicesX(), choicesY(), choicesWidth(), choicesHeight(), PANEL_ALT);
        }

        renderHeader(guiGraphics);
        if (mode != ConversationMode.IDENTITY) {
            renderStatus(guiGraphics);
        }
        renderTranscript(guiGraphics);
        if (mode != ConversationMode.IDENTITY && mode != ConversationMode.SHOP) {
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
        renderShopTooltip(guiGraphics, mouseX, mouseY);
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
            int shopSlot = clickedShopSlot(mouseX, mouseY);
            if (shopSlot >= 0) {
                selectedShopSlot = shopSlot;
                alignShopScrollToSelection();
                return true;
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
        for (int index = 0; index < choiceButtons.size(); index++) {
            ChoiceButton button = choiceButtons.get(index);
            if (index >= specs.size()) {
                button.visible = false;
                button.active = false;
                button.spec = null;
                continue;
            }
            button.spec = specs.get(index);
            button.visible = true;
            button.active = button.spec.enabled();
            button.setMessage(button.spec.message());
        }
    }

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
            case CLOSE -> ChoiceIcon.BACK;
            case DIALOGUE -> ChoiceIcon.TALK;
        };
    }

    private List<ChoiceSpec> serviceChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(serviceChoice(FixerDialogMenu.OPTION_STABILIZE, "service_stabilize"));
        choices.add(serviceChoice(FixerDialogMenu.OPTION_PURGE, "service_purge"));
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
        return choices;
    }

    private List<ChoiceSpec> shopChoices() {
        normalizeSelectedShopSlot();
        ShopEntry entry = shopEntry(selectedShopSlot);
        int optionId = entry.optionId();
        List<ChoiceSpec> choices = new ArrayList<>();
        choices.add(customChoice(
                ChoiceIcon.TRADE,
                Component.translatable("screen.cyberneticenhancements.npc.choice.shop.buy", entry.label(), menu.priceForOption(optionId)),
                menu.isOptionAvailable(optionId),
                menu.isOptionAvailable(optionId) ? null : lockedTrustTooltip(optionId),
                () -> {
                    lastPlayerLineKey = playerKey("shop");
                    PacketDistributor.sendToServer(new FixerDialogueChoicePayload(optionId));
                }
        ));
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
        return choices;
    }

    private List<ChoiceSpec> contractChoices() {
        List<ChoiceSpec> choices = new ArrayList<>();
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
            for (int slot = 0; slot < 3; slot++) {
                if (!menu.offerPresent(slot)) {
                    continue;
                }
                final int offerSlot = slot;
                choices.add(customChoice(
                        ChoiceIcon.QUEST,
                        Component.translatable("screen.cyberneticenhancements.fixer.choice.contract.accept", menu.offerTitle(slot), menu.offerRewardMoney(slot)),
                        true,
                        () -> sendOption(FixerDialogMenu.OPTION_ACCEPT_CONTRACT_1 + offerSlot, playerKey("accept_contract"))
                ));
            }
        }
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
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
        choices.add(customChoice(
                ChoiceIcon.PREV,
                Component.translatable("screen.cyberneticenhancements.npc.identity.previous_page"),
                skinPage > 0,
                () -> scrollSkinGallery(-1)
        ));
        choices.add(customChoice(
                ChoiceIcon.NEXT,
                Component.translatable("screen.cyberneticenhancements.npc.identity.next_page"),
                hasNextSkinScroll(),
                () -> scrollSkinGallery(1)
        ));
        choices.add(localChoice(ChoiceIcon.BACK, "screen.cyberneticenhancements.fixer.choice.back", playerKey("back"), npcLineKey("back"), () -> switchMode(ConversationMode.DIALOGUE, playerKey("back"), npcLineKey("back"))));
        return choices;
    }

    private ChoiceSpec serviceChoice(int optionId, String labelSuffix) {
        return customChoice(
                ChoiceIcon.SERVICE,
                Component.translatable(choiceKey(labelSuffix), menu.priceForOption(optionId)),
                menu.isOptionAvailable(optionId),
                menu.isOptionAvailable(optionId) ? null : lockedTrustTooltip(optionId),
                () -> {
                    lastPlayerLineKey = playerKey(labelSuffix);
                    PacketDistributor.sendToServer(new FixerDialogueChoicePayload(optionId));
                }
        );
    }

    private void normalizeSelectedShopSlot() {
        int count = shopItemCount();
        selectedShopSlot = Math.max(0, Math.min(count - 1, selectedShopSlot));
        shopScrollOffset = Math.max(0, Math.min(maxShopScrollOffset(), shopScrollOffset));
        if (selectedShopSlot < shopScrollOffset) {
            shopScrollOffset = selectedShopSlot;
        }
        int lastVisible = shopScrollOffset + visibleShopRowCount() - 1;
        if (selectedShopSlot > lastVisible) {
            shopScrollOffset = Math.max(0, selectedShopSlot - visibleShopRowCount() + 1);
        }
    }

    private ShopEntry shopEntry(int slot) {
        return switch (menu.npcType()) {
            case FIXER -> switch (slot) {
                case 0 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_MAXDOC_MK2, "screen.cyberneticenhancements.npc.shop.fixer.maxdoc", "screen.cyberneticenhancements.npc.shopdesc.fixer.0", new ItemStack(ModItems.consumable("maxdoc_mk2").get()));
                case 1 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_RAM_JOLT, "screen.cyberneticenhancements.npc.shop.fixer.ram_jolt", "screen.cyberneticenhancements.npc.shopdesc.fixer.1", new ItemStack(ModItems.consumable("ram_jolt").get()));
                case 2 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT, "screen.cyberneticenhancements.npc.shop.fixer.memory_boost", "screen.cyberneticenhancements.npc.shopdesc.fixer.2", new ItemStack(ModItems.cyberware("memory_boost").get()));
                default -> new ShopEntry(FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS, "screen.cyberneticenhancements.npc.shop.fixer.immuno", "screen.cyberneticenhancements.npc.shopdesc.fixer.3", new ItemStack(ModItems.consumable("immunoblockers").get()));
            };
            case RIPPERDOC -> switch (slot) {
                case 0 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_MAXDOC_MK2, "screen.cyberneticenhancements.npc.shop.ripperdoc.maxdoc", "screen.cyberneticenhancements.npc.shopdesc.ripperdoc.0", new ItemStack(ModItems.consumable("maxdoc_mk2").get(), 2));
                case 1 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_RAM_JOLT, "screen.cyberneticenhancements.npc.shop.ripperdoc.bounce_back", "screen.cyberneticenhancements.npc.shopdesc.ripperdoc.1", new ItemStack(ModItems.consumable("bounce_back_mk2").get()));
                case 2 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT, "screen.cyberneticenhancements.npc.shop.ripperdoc.kiroshi", "screen.cyberneticenhancements.npc.shopdesc.ripperdoc.2", new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get()));
                default -> new ShopEntry(FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS, "screen.cyberneticenhancements.npc.shop.ripperdoc.bionic_joints", "screen.cyberneticenhancements.npc.shopdesc.ripperdoc.3", new ItemStack(ModItems.cyberware("bionic_joints").get()));
            };
            case TECHIE -> switch (slot) {
                case 0 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_MAXDOC_MK2, "screen.cyberneticenhancements.npc.shop.techie.components", "screen.cyberneticenhancements.npc.shopdesc.techie.0", new ItemStack(ModItems.COMMON_ITEM_COMPONENTS.get(), 2));
                case 1 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_RAM_JOLT, "screen.cyberneticenhancements.npc.shop.techie.precision_miner", "screen.cyberneticenhancements.npc.shopdesc.techie.1", new ItemStack(ModItems.cyberware("precision_miner").get()));
                case 2 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT, "screen.cyberneticenhancements.npc.shop.techie.masons_grip", "screen.cyberneticenhancements.npc.shopdesc.techie.2", new ItemStack(ModItems.cyberware("masons_grip").get()));
                default -> new ShopEntry(FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS, "screen.cyberneticenhancements.npc.shop.techie.cargo_spine", "screen.cyberneticenhancements.npc.shopdesc.techie.3", new ItemStack(ModItems.cyberware("cargo_spine").get()));
            };
            case NETRUNNER -> switch (slot) {
                case 0 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_MAXDOC_MK2, "screen.cyberneticenhancements.npc.shop.netrunner.ram_jolt", "screen.cyberneticenhancements.npc.shopdesc.netrunner.0", new ItemStack(ModItems.consumable("ram_jolt").get(), 2));
                case 1 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_RAM_JOLT, "screen.cyberneticenhancements.npc.shop.netrunner.memory_boost", "screen.cyberneticenhancements.npc.shopdesc.netrunner.1", new ItemStack(ModItems.cyberware("memory_boost").get()));
                case 2 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT, "screen.cyberneticenhancements.npc.shop.netrunner.ram_upgrade", "screen.cyberneticenhancements.npc.shopdesc.netrunner.2", new ItemStack(ModItems.cyberware("ram_upgrade").get()));
                default -> new ShopEntry(FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS, "screen.cyberneticenhancements.npc.shop.netrunner.chipware_socket", "screen.cyberneticenhancements.npc.shopdesc.netrunner.3", new ItemStack(ModItems.cyberware("chipware_socket").get()));
            };
            case MERC -> switch (slot) {
                case 0 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_MAXDOC_MK2, "screen.cyberneticenhancements.npc.shop.merc.maxdoc", "screen.cyberneticenhancements.npc.shopdesc.merc.0", new ItemStack(ModItems.consumable("maxdoc_mk2").get()));
                case 1 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_RAM_JOLT, "screen.cyberneticenhancements.npc.shop.merc.handle_wrap", "screen.cyberneticenhancements.npc.shopdesc.merc.1", new ItemStack(ModItems.cyberware("handle_wrap").get()));
                case 2 -> new ShopEntry(FixerDialogMenu.OPTION_BUY_CHROME_SUPPRESSANT, "screen.cyberneticenhancements.npc.shop.merc.fortified_ankles", "screen.cyberneticenhancements.npc.shopdesc.merc.2", new ItemStack(ModItems.cyberware("fortified_ankles").get()));
                default -> new ShopEntry(FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS, "screen.cyberneticenhancements.npc.shop.merc.subdermal_armor", "screen.cyberneticenhancements.npc.shopdesc.merc.3", new ItemStack(ModItems.cyberware("subdermal_armor").get()));
            };
        };
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

    private String choiceKey(String suffix) {
        return "screen.cyberneticenhancements.npc.choice." + menu.npcTypeId() + "." + suffix;
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
            normalizeSelectedShopSlot();
            alignShopScrollToSelection();
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
        selectedNameColorId = "aqua";
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
        PacketDistributor.sendToServer(new NpcIdentityNameColorPayload("aqua"));
        PacketDistributor.sendToServer(new NpcIdentityAppearancePayload(""));
        TextFieldFocusHelper.clearFocus(this, List.of(nicknameField));
    }

    private void syncIdentityEditorFromNpc() {
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (nicknameField != null && npc != null) {
            nicknameField.setValue(npc.getName().getString());
        }
        selectedAppearanceId = currentAppearanceId();
        selectedNameColorId = currentNameColorId();
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
            case DIALOGUE, CLOSE -> mode = ConversationMode.DIALOGUE;
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
        if (mode == ConversationMode.IDENTITY && selectedNameColorId != null && !selectedNameColorId.isBlank()) {
            return selectedNameColorId;
        }
        AbstractCityNpcEntity npc = currentNpcEntity();
        if (npc == null || npc.nameColorId().isBlank()) {
            return selectedNameColorId;
        }
        return npc.nameColorId();
    }

    private int currentNameColorRgb() {
        return switch (currentNameColorId()) {
            case "blue" -> 0x5D86F1;
            case "gold" -> 0xF0C04D;
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
            case "gold" -> net.minecraft.ChatFormatting.GOLD;
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

    private boolean hasNextSkinScroll() {
        return skinPage < maxSkinScrollOffset();
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
        guiGraphics.drawString(font, menu.npcTypeLabel(), x, y + 12, ACCENT, false);
        Component trust = Component.translatable("screen.cyberneticenhancements.fixer.trust", trustLabel());
        Component nextTrust = nextTrustLabel();
        int rightX = headerX() + headerWidth() - PANEL_TEXT_PADDING_X;
        guiGraphics.drawString(font, trust, rightX - font.width(trust), y, TEXT_SECONDARY, false);
        if (!nextTrust.getString().isBlank()) {
            guiGraphics.drawString(font, nextTrust, rightX - font.width(nextTrust), y + 12, TEXT_SECONDARY, false);
        }
        drawTrustBar(guiGraphics, x, headerY() + 32, headerWidth() - PANEL_TEXT_PADDING_X * 2, TRUST_BAR_HEIGHT);
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        int x = statusX() + PANEL_TEXT_PADDING_X;
        int headerY = statusY() + PANEL_TEXT_PADDING_Y;
        int rowY = headerY + font.lineHeight + 8;
        if (mode == ConversationMode.IDENTITY) {
            int totalSkins = Math.max(1, availableSkins().size());
            Component unlockState = menu.identityUnlocked()
                    ? Component.translatable("screen.cyberneticenhancements.npc.identity.unlocked")
                    : Component.translatable("screen.cyberneticenhancements.npc.identity.locked");
            Component appearanceState = Component.translatable("screen.cyberneticenhancements.npc.identity.appearance_state", selectedAppearanceIndex() + 1, totalSkins);
            guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.section.identity_status"), x, headerY, TEXT_PRIMARY, false);
            guiGraphics.drawString(font, trimStyled(unlockState, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY, menu.identityUnlocked() ? ACCENT : TEXT_SECONDARY, false);
            guiGraphics.drawString(font, trimStyled(appearanceState, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 14, TEXT_SECONDARY, false);
            return;
        }
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
                Component titleLine = menu.contractBoardTitle();
                Component rewardLine = menu.contractBoardNote();
                guiGraphics.drawString(font, titleLine, x, rowY, TEXT_SECONDARY, false);
                guiGraphics.drawString(font, trimStyled(rewardLine, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 12, ACCENT, false);
            }
            return;
        }
        guiGraphics.drawString(font, currentSummaryHeader(), x, headerY, TEXT_PRIMARY, false);
        Component primary = currentSummaryPrimary();
        Component secondary = currentSummarySecondary();
        guiGraphics.drawString(font, trimStyled(primary, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY, TEXT_SECONDARY, false);
        guiGraphics.drawString(font, trimStyled(secondary, statusWidth() - PANEL_TEXT_PADDING_X * 2), x, rowY + 14, ACCENT, false);
    }

    private void renderTranscript(GuiGraphics guiGraphics) {
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

    private void renderShopCatalog(GuiGraphics guiGraphics) {
        normalizeSelectedShopSlot();
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int y = transcriptY() + PANEL_TEXT_PADDING_Y;
        int width = transcriptWidth() - PANEL_TEXT_PADDING_X * 2 - (shopItemCount() > visibleShopRowCount() ? shopScrollbarWidth() + 6 : 0);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.npc.section.shop_catalog"), x, y, TEXT_PRIMARY, false);
        drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.npc.section.shop_catalog.subtitle"), x, y + 12, width, TEXT_SECONDARY, 1);

        int visibleRows = visibleShopRowCount();
        int rowHeight = shopRowHeight();
        guiGraphics.enableScissor(x, shopRowsTop(), x + width, shopRowsBottom());
        for (int visibleSlot = 0; visibleSlot < visibleRows; visibleSlot++) {
            int slot = shopScrollOffset + visibleSlot;
            if (slot >= shopItemCount()) {
                break;
            }
            int rowY = shopRowY(visibleSlot);
            renderShopRow(guiGraphics, x, rowY, width, rowHeight, slot, selectedShopSlot == slot);
        }
        guiGraphics.disableScissor();
        renderShopScrollbar(guiGraphics);
    }

    private void renderShopRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int slot, boolean selected) {
        ShopEntry entry = shopEntry(slot);
        boolean hovered = hoveredShopSlot(lastMouseX, lastMouseY) == slot;
        UiRenderer.control(guiGraphics, x, y, width, height, true, selected, hovered, 0xFFFFFFFF);

        int iconBoxX = x + 8;
        int iconBoxY = y + 4;
        guiGraphics.renderItem(entry.stack(), iconBoxX, iconBoxY);
        guiGraphics.renderItemDecorations(font, entry.stack(), iconBoxX, iconBoxY);

        int textX = iconBoxX + SHOP_ICON_BOX_SIZE + 8;
        Component priceLine = Component.translatable("screen.cyberneticenhancements.npc.shop.price", menu.priceForOption(entry.optionId()));
        Component stateLine = menu.isOptionAvailable(entry.optionId())
                ? Component.translatable("screen.cyberneticenhancements.npc.shop.state.available")
                : Component.translatable("screen.cyberneticenhancements.npc.shop.state.locked", menu.trustGateLabelForOption(entry.optionId()));
        int priceWidth = font.width(priceLine);
        int stateWidth = font.width(stateLine);
        int rightX = x + width - 8;
        int rightBlockWidth = Math.max(priceWidth, stateWidth);
        int rightBlockX = rightX - rightBlockWidth;
        int leftGap = 8;
        int textWidth = Math.max(32, rightBlockX - leftGap - textX);

        guiGraphics.drawString(font, trimStyled(entry.label(), textWidth), textX, y + 4, TEXT_PRIMARY, false);
        if (textWidth > 32) {
            guiGraphics.drawString(font, trimStyled(entry.description(), textWidth), textX, y + 14, TEXT_SECONDARY, false);
        }

        guiGraphics.drawString(font, priceLine, rightX - priceWidth, y + 4, ACCENT, false);
        guiGraphics.drawString(font, stateLine, rightX - stateWidth, y + 14, menu.isOptionAvailable(entry.optionId()) ? TEXT_SECONDARY : 0xFFD08A8A, false);
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
        previewNpc.applyClientPreviewIdentity(texture.toString(), category, currentNpcDisplayName(), selectedNameColorId);
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
            case "merc" -> new MercEntity(ModEntityTypes.MERC.get(), minecraft.level);
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
            if (button.spec.tooltip() != null) {
                guiGraphics.renderTooltip(font, button.spec.tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    private void renderShopTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mode != ConversationMode.SHOP) {
            return;
        }
        int slot = hoveredShopSlot(mouseX, mouseY);
        if (slot < 0) {
            return;
        }
        ShopEntry entry = shopEntry(slot);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(entry.label());
        tooltip.add(Component.translatable("screen.cyberneticenhancements.npc.shop.price", menu.priceForOption(entry.optionId())));
        if (menu.isOptionAvailable(entry.optionId())) {
            tooltip.add(Component.translatable("screen.cyberneticenhancements.npc.shop.state.available"));
        } else {
            tooltip.add(Component.translatable("screen.cyberneticenhancements.npc.shop.state.locked", menu.trustGateLabelForOption(entry.optionId())));
        }
        guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
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

    private Component nextTrustLabel() {
        return switch (menu.trustLevel()) {
            case 0 -> Component.translatable("screen.cyberneticenhancements.fixer.trust.next", Component.translatable("screen.cyberneticenhancements.fixer.trust.1"), 20);
            case 1 -> Component.translatable("screen.cyberneticenhancements.fixer.trust.next", Component.translatable("screen.cyberneticenhancements.fixer.trust.2"), 45);
            case 2 -> Component.translatable("screen.cyberneticenhancements.fixer.trust.next", Component.translatable("screen.cyberneticenhancements.fixer.trust.3"), 70);
            case 3 -> Component.translatable("screen.cyberneticenhancements.fixer.trust.next", Component.translatable("screen.cyberneticenhancements.fixer.trust.4"), 90);
            default -> Component.empty();
        };
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

    private Component currentSummaryHeader() {
        String type = menu.npcTypeId();
        return switch (mode) {
            case SERVICES -> Component.translatable("screen.cyberneticenhancements.npc.summary." + type + ".services.header");
            case SHOP -> Component.translatable("screen.cyberneticenhancements.npc.summary." + type + ".shop.header");
            default -> Component.translatable("screen.cyberneticenhancements.npc.summary." + type + ".dialogue.header");
        };
    }

    private Component currentSummaryPrimary() {
        return switch (mode) {
            case SERVICES -> Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".services.primary");
            case SHOP -> Component.translatable("screen.cyberneticenhancements.npc.summary.wallet", menu.balance());
            default -> Component.translatable("screen.cyberneticenhancements.npc.summary.wallet", menu.balance());
        };
    }

    private Component currentSummarySecondary() {
        if (mode == ConversationMode.SERVICES) {
            if (!menu.isOptionAvailable(FixerDialogMenu.OPTION_PURGE)) {
                return Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".services.secondary.locked", menu.trustGateLabelForOption(FixerDialogMenu.OPTION_PURGE));
            }
            if ((menu.npcType() == NpcType.FIXER || menu.npcType() == NpcType.RIPPERDOC) && !menu.purgeReadyClient()) {
                return Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".services.secondary.cooldown");
            }
            return Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".services.secondary.live");
        }
        if (mode == ConversationMode.SHOP) {
            return Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".shop.secondary", discountLabel());
        }
        return Component.translatable("screen.cyberneticenhancements.npc.summary." + menu.npcTypeId() + ".dialogue.secondary", discountLabel());
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

    private int headerHeight() {
        return HEADER_HEIGHT;
    }

    private int statusX() {
        return headerX();
    }

    private int statusY() {
        return headerY() + headerHeight() + PANEL_GAP;
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
        return mode == ConversationMode.IDENTITY
                ? statusY()
                : statusY() + statusHeight() + PANEL_GAP;
    }

    private int transcriptWidth() {
        return headerWidth();
    }

    private int transcriptHeight() {
        int baseHeight = imageHeight - CONTENT_PADDING * 2 - HEADER_HEIGHT - STATUS_HEIGHT - CHOICES_HEIGHT - PANEL_GAP * 3;
        return switch (mode) {
            case IDENTITY -> baseHeight + STATUS_HEIGHT + CHOICES_HEIGHT + PANEL_GAP * 2;
            case SHOP -> baseHeight + CHOICES_HEIGHT + PANEL_GAP;
            default -> baseHeight;
        };
    }

    private int identityFieldX() {
        return transcriptX() + PANEL_TEXT_PADDING_X;
    }

    private int identityFieldY() {
        return transcriptY() + PANEL_TEXT_PADDING_Y + IDENTITY_FIELD_Y_OFFSET;
    }

    private int identityColorButtonsX() {
        return identityGalleryRight() - identityColorButtonsWidth();
    }

    private int identityColorButtonsWidth() {
        return NAME_COLOR_PRESETS.length * IDENTITY_COLOR_BUTTON_SIZE + (NAME_COLOR_PRESETS.length - 1) * IDENTITY_COLOR_BUTTON_GAP;
    }

    private int identityFieldWidth() {
        return Math.max(120, identityColorButtonsX() - 8 - identityFieldX());
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
        return CHOICES_HEIGHT;
    }

    private int shopRowsTop() {
        return transcriptY() + PANEL_TEXT_PADDING_Y + 12 + font.lineHeight + SHOP_DETAIL_GAP;
    }

    private int shopRowsBottom() {
        return actionButtonsStartY() - SHOP_DETAIL_GAP;
    }

    private int shopRowHeight() {
        return SHOP_ROW_MIN_HEIGHT;
    }

    private int shopRowY(int visibleSlot) {
        return shopRowsTop() + visibleSlot * (shopRowHeight() + SHOP_ROW_GAP);
    }

    private int shopItemCount() {
        return 4;
    }

    private int visibleShopRowCount() {
        int availableHeight = shopRowsBottom() - shopRowsTop();
        int rows = Math.max(1, (availableHeight + SHOP_ROW_GAP) / (SHOP_ROW_MIN_HEIGHT + SHOP_ROW_GAP));
        return Math.max(1, Math.min(shopItemCount(), Math.min(rows, SHOP_MAX_VISIBLE_ROWS)));
    }

    private int maxShopScrollOffset() {
        return Math.max(0, shopItemCount() - visibleShopRowCount());
    }

    private void scrollShop(int delta) {
        shopScrollOffset = Math.max(0, Math.min(maxShopScrollOffset(), shopScrollOffset + delta));
        if (selectedShopSlot < shopScrollOffset) {
            selectedShopSlot = shopScrollOffset;
        }
        int lastVisible = shopScrollOffset + visibleShopRowCount() - 1;
        if (selectedShopSlot > lastVisible) {
            selectedShopSlot = lastVisible;
        }
    }

    private void alignShopScrollToSelection() {
        if (selectedShopSlot < shopScrollOffset) {
            shopScrollOffset = selectedShopSlot;
        }
        int lastVisible = shopScrollOffset + visibleShopRowCount() - 1;
        if (selectedShopSlot > lastVisible) {
            shopScrollOffset = Math.max(0, selectedShopSlot - visibleShopRowCount() + 1);
        }
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

    private int hoveredShopSlot(double mouseX, double mouseY) {
        if (mode != ConversationMode.SHOP) {
            return -1;
        }
        int x = transcriptX() + PANEL_TEXT_PADDING_X;
        int width = transcriptWidth() - PANEL_TEXT_PADDING_X * 2 - (shopItemCount() > visibleShopRowCount() ? shopScrollbarWidth() + 6 : 0);
        int height = shopRowHeight();
        for (int visibleSlot = 0; visibleSlot < visibleShopRowCount(); visibleSlot++) {
            int slot = shopScrollOffset + visibleSlot;
            if (slot >= shopItemCount()) {
                break;
            }
            int rowY = shopRowY(visibleSlot);
            if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + height) {
                return slot;
            }
        }
        return -1;
    }

    private int clickedShopSlot(double mouseX, double mouseY) {
        return hoveredShopSlot(mouseX, mouseY);
    }

    private boolean clickedShopScrollbar(double mouseX,double mouseY) {
        var track=shopTrack();if(!shopScrollbar.begin(track,mouseX,mouseY))return false;
        shopScrollOffset=(int)Math.round(shopScrollbar.value(track,mouseX,mouseY));return true;
    }
    private int actionButtonsX() {
        return (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP)
                ? transcriptX() + PANEL_TEXT_PADDING_X
                : choicesX() + PANEL_TEXT_PADDING_X;
    }

    private int actionButtonsWidth() {
        return (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP)
                ? transcriptWidth() - PANEL_TEXT_PADDING_X * 2
                : choicesWidth() - PANEL_TEXT_PADDING_X * 2;
    }

    private int actionButtonsStartY() {
        int buttonCount = visibleActionButtonCount();
        int totalHeight = buttonCount * CHOICE_BUTTON_HEIGHT + Math.max(0, buttonCount - 1) * CHOICE_BUTTON_GAP;
        if (mode == ConversationMode.IDENTITY || mode == ConversationMode.SHOP) {
            return transcriptY() + transcriptHeight() - PANEL_TEXT_PADDING_Y - totalHeight;
        }
        return choicesY() + choicesHeight() - PANEL_TEXT_PADDING_Y - totalHeight;
    }

    private int visibleActionButtonCount() {
        if (mode == ConversationMode.SHOP) {
            return SHOP_VISIBLE_ACTIONS;
        }
        return Math.max(1, currentChoices().size());
    }

    private record ShopEntry(int optionId, String labelKey, String descriptionKey, ItemStack stack) {
        private Component label() {
            return Component.translatable(labelKey);
        }

        private Component description() {
            return Component.translatable(descriptionKey);
        }
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
        QUEST(Icons.QUEST),
        SHOP(Icons.SHOP),
        TRADE(Icons.TRADE),
        SERVICE(Icons.CRAFTING),
        IDENTITY(Icons.SETTINGS),
        RESET(Icons.RESET),
        PREV(Icons.BACK),
        NEXT(Icons.NEXT),
        BACK(Icons.BACK);

        private final Icons icon;

        ChoiceIcon(Icons icon) {
            this.icon = icon;
        }
    }

    private record ChoiceSpec(ChoiceIcon icon, Component message, boolean enabled, Component tooltip, Runnable action) {
    }

    private record NameColorPreset(String colorId, int rgb) {
        private boolean hidden() {
            return "hidden".equals(colorId);
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
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            UiRenderer.action(guiGraphics, getX(), getY(), width, height, active, highlight.update(active && (hovered || isFocused())));
            int contentInset = Math.max(3, (height - font.lineHeight) / 2);
            int textY = getY() + contentInset;
            int iconY = getY() + Math.max(0, (height - font.lineHeight) / 2) + CHOICE_ICON_Y_OFFSET;
            int textX = getX() + contentInset;
            int availableWidth = width - contentInset * 2;
            if (spec != null && spec.icon() != null) {
                Component iconComponent = spec.icon().icon.component();
                guiGraphics.drawString(font, iconComponent, textX, iconY, active ? ACCENT : TEXT_SECONDARY, false);
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

    private final class ColorPresetButton extends Button {
        private final NameColorPreset preset;
        private boolean selected;

        private ColorPresetButton(int x, int y, int width, int height, NameColorPreset preset) {
            super(x, y, width, height, Component.empty(), button -> {
                selectedNameColorId = ((ColorPresetButton) button).preset.colorId();
                AbstractCityNpcEntity npc = currentNpcEntity();
                if (npc != null) {
                    npc.setCustomName(Component.literal(currentNpcDisplayName()).withStyle(resolveCurrentNameColor()));
                    npc.setCustomNameVisible(!((ColorPresetButton) button).preset.hidden());
                }
                PacketDistributor.sendToServer(new NpcIdentityNameColorPayload(((ColorPresetButton) button).preset.colorId()));
            }, DEFAULT_NARRATION);
            this.preset = preset;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            UiRenderer.control(guiGraphics,getX(),getY(),width,height,active,selected,isHoveredOrFocused(),0xFFFFFFFF);
            int fill=!active?UiRenderer.ROW_DISABLED:preset.hidden()?UiRenderer.FIELD:preset.rgb();
            guiGraphics.fill(getX()+3,getY()+3,getX()+width-3,getY()+height-3,fill);            if (preset.hidden()) {
                guiGraphics.renderItem(new ItemStack(Items.BARRIER), getX() + (width - 16) / 2, getY() + (height - 16) / 2);
            }
        }
    }
}
