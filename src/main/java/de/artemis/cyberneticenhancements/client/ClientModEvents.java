package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ArchiveBankingClientState;
import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.RelicCacheBlockEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen;
import de.artemis.cyberneticenhancements.client.screen.FixerDialogScreen;
import de.artemis.cyberneticenhancements.client.screen.RelicCacheHackScreen;
import de.artemis.cyberneticenhancements.client.screen.RecyclerStationScreen;
import de.artemis.cyberneticenhancements.client.screen.RipperStationScreen;
import de.artemis.cyberneticenhancements.client.screen.TechStationScreen;
import de.artemis.cyberneticenhancements.client.screen.WikiLaunchButton;
import de.artemis.cyberneticenhancements.client.tooltip.ModTooltipStyle;
import de.artemis.cyberneticenhancements.client.tooltip.UpgradeProgressClientTooltip;
import de.artemis.cyberneticenhancements.client.tooltip.UpgradeProgressTooltip;
import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import de.artemis.cyberneticenhancements.common.network.ActivateArmCyberwarePayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveContactsRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsRequestPayload;
import de.artemis.cyberneticenhancements.common.network.ActivateAuxiliaryCyberwarePayload;
import de.artemis.cyberneticenhancements.common.network.ActivateCyberwarePayload;
import de.artemis.cyberneticenhancements.common.network.ActivateFaceCyberwarePayload;
import de.artemis.cyberneticenhancements.common.network.ActivateLegCyberwarePayload;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiViewport;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ClientModEvents {
    private static final int HAZARD_OUTLINE_R = 0x1E;
    private static final int HAZARD_OUTLINE_G = 0xE2;
    private static final int HAZARD_OUTLINE_B = 0xB5;
    private static boolean jumpKeyWasDown;
    private static boolean airborneLegJumpArmed;
    private static boolean psychosisMouseSuppressed;
    private static Field mouseAccumulatedDxField;
    private static Field mouseAccumulatedDyField;
    private static Field mouseXField;
    private static Field mouseYField;
    private static Field mouseGrabbedField;
    private static Field mouseIgnoreFirstMoveField;
    private static Method mouseSetIgnoreFirstMoveMethod;
    private static boolean mouseReflectionInitialized;
    private static Field containerHoveredSlotField;
    private static boolean containerReflectionInitialized;

    private ClientModEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SandevistanClientOptions::initialize);
        event.enqueueWork(() -> ModBlocks.allRelicCaches().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.translucent())));
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RIPPER_STATION.get(), RipperStationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RELIC_CACHE.get(), RelicCacheBlockEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FIXER.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RIPPERDOC.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TECHIE.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.NETRUNNER.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MERC.get(), FixerEntityRenderer::new);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FABRICATOR.get(), de.artemis.cyberneticenhancements.client.screen.FabricatorScreen::new);
        event.register(ModMenuTypes.RIPPER_STATION.get(), RipperStationScreen::new);
        event.register(ModMenuTypes.TECH_STATION.get(), TechStationScreen::new);
        event.register(ModMenuTypes.UPGRADE_STATION.get(), TechStationScreen::new);
        event.register(ModMenuTypes.RECYCLER_STATION.get(), RecyclerStationScreen::new);
        event.register(ModMenuTypes.FIXER_DIALOG.get(), FixerDialogScreen::new);
        event.register(ModMenuTypes.RELIC_CACHE_HACK.get(), RelicCacheHackScreen::new);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(UpgradeProgressTooltip.class, UpgradeProgressClientTooltip::new);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SandevistanClientEffects.tick();
        if (minecraft.player == null) {
            return;
        }

        FaceHazardHighlightClientState.prune();

        if (CyberpsychosisClientState.isControlLocked()) {
            if (minecraft.screen != null) {
                disablePsychosisMouseSuppression(minecraft);
                jumpKeyWasDown = false;
                airborneLegJumpArmed = false;
                return;
            }

            enablePsychosisMouseSuppression(minecraft);
            suppressPsychosisMouseLook(minecraft);
            releaseControlKeys(minecraft.options);
            jumpKeyWasDown = false;
            airborneLegJumpArmed = false;
            applyLockedPsychosisControl(minecraft);
            minecraft.options.keyJump.setDown(CyberpsychosisClientState.jump());
            minecraft.options.keySprint.setDown(CyberpsychosisClientState.sprint());
            minecraft.player.setSprinting(CyberpsychosisClientState.sprint());
            if (minecraft.player.input != null) {
                minecraft.player.input.forwardImpulse = CyberpsychosisClientState.forward();
                minecraft.player.input.leftImpulse = CyberpsychosisClientState.strafe();
                minecraft.player.input.up = CyberpsychosisClientState.forward() > 0.01F;
                minecraft.player.input.down = false;
                minecraft.player.input.left = CyberpsychosisClientState.strafe() > 0.01F;
                minecraft.player.input.right = CyberpsychosisClientState.strafe() < -0.01F;
                minecraft.player.input.jumping = CyberpsychosisClientState.jump();
                minecraft.player.input.shiftKeyDown = false;
            }
            performLockedPsychosisJump(minecraft);
            return;
        }

        disablePsychosisMouseSuppression(minecraft);

        while (ModKeyMappings.ACTIVATE_CYBERWARE.consumeClick()) {
            PacketDistributor.sendToServer(new ActivateCyberwarePayload());
        }
        while (ModKeyMappings.ACTIVATE_AUXILIARY_CYBERWARE.consumeClick()) {
            PacketDistributor.sendToServer(new ActivateAuxiliaryCyberwarePayload());
        }
        while (ModKeyMappings.ACTIVATE_ARM_CYBERWARE.consumeClick()) {
            PacketDistributor.sendToServer(new ActivateArmCyberwarePayload());
        }
        while (ModKeyMappings.ACTIVATE_FACE_CYBERWARE.consumeClick()) {
            PacketDistributor.sendToServer(new ActivateFaceCyberwarePayload());
        }
        boolean jumpKeyDown = minecraft.options.keyJump.isDown()
                || minecraft.player.input != null && minecraft.player.input.jumping;
        boolean airborne = !minecraft.player.onGround()
                && !minecraft.player.onClimbable()
                && !minecraft.player.isInWaterOrBubble();
        if (!airborne) {
            airborneLegJumpArmed = false;
        } else {
            if (!jumpKeyDown) {
                airborneLegJumpArmed = true;
            } else if (!jumpKeyWasDown && airborneLegJumpArmed) {
                PacketDistributor.sendToServer(new ActivateLegCyberwarePayload());
                airborneLegJumpArmed = false;
            }
        }
        jumpKeyWasDown = jumpKeyDown;

        while (ModKeyMappings.TOGGLE_HUD.consumeClick()) {
            boolean enabled = HudVisibilityController.toggleHud();
            minecraft.player.displayClientMessage(
                    Icons.INFO.withText(Component.translatable(enabled
                            ? "message.cyberneticenhancements.hud.enabled"
                            : "message.cyberneticenhancements.hud.disabled")),
                    true
            );
        }
        while (ModKeyMappings.OPEN_ARCHIVE.consumeClick()) {
            openArchiveScreen(minecraft, minecraft.screen, resolveArchiveEntryId(minecraft));
        }
    }

    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!shouldBlockWorldInput()) {
            return;
        }

        event.getInput().forwardImpulse = CyberpsychosisClientState.forward();
        event.getInput().leftImpulse = CyberpsychosisClientState.strafe();
        event.getInput().up = CyberpsychosisClientState.forward() > 0.01F;
        event.getInput().down = false;
        event.getInput().left = CyberpsychosisClientState.strafe() > 0.01F;
        event.getInput().right = CyberpsychosisClientState.strafe() < -0.01F;
        event.getInput().jumping = CyberpsychosisClientState.jump();
        event.getInput().shiftKeyDown = false;
    }

    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!shouldBlockWorldInput()) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (shouldBlockWorldInput()) {
            event.setCanceled(true);
        }
    }

    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (ModTooltipStyle.shouldStyle(event.getItemStack())) {
            ModTooltipStyle.apply(event);
        }
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            SandevistanClientEffects.renderOverlay(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()) && HudVisibilityController.isHudEnabled()) {
            CyberwareHudOverlay.render(event.getGuiGraphics());
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                && !de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.isShadowPass()) {

            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.flushProjections();
            de.artemis.cyberneticenhancements.client.render.SandevistanEchoRenderer.render(event);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (CyberpsychosisClientState.isControlLocked() && minecraft.player != null && minecraft.screen == null) {
            CyberpsychosisClientState.tickSmoothing();
            suppressPsychosisMouseLook(minecraft);
            applyLockedPsychosisControl(minecraft);
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        if (minecraft.level == null
                || (FaceHazardHighlightClientState.activeBlockHighlights().isEmpty()
                && FaceHazardHighlightClientState.activeEntityHighlights().isEmpty())) {
            return;
        }

        OutlineBufferSource outlineBufferSource = minecraft.renderBuffers().outlineBufferSource();
        outlineBufferSource.setColor(HAZARD_OUTLINE_R, HAZARD_OUTLINE_G, HAZARD_OUTLINE_B, 255);
        minecraft.levelRenderer.requestOutlineEffect();
        var camera = event.getCamera().getPosition();

        for (var entry : FaceHazardHighlightClientState.activeBlockHighlights().entrySet()) {
            var state = minecraft.level.getBlockState(entry.getKey());
            if (state.isAir()) {
                continue;
            }
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(entry.getKey().getX() - camera.x, entry.getKey().getY() - camera.y, entry.getKey().getZ() - camera.z);
            minecraft.getBlockRenderer().renderSingleBlock(state, event.getPoseStack(), outlineBufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            event.getPoseStack().popPose();
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
        for (var entry : FaceHazardHighlightClientState.activeEntityHighlights().entrySet()) {
            Entity entity = resolveHighlightedEntity(minecraft, entry.getKey());
            if (entity == null) {
                continue;
            }
            if (entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                continue;
            }

            double renderX = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camera.x;
            double renderY = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camera.y;
            double renderZ = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camera.z;
            float renderYRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

            entityRenderDispatcher.render(
                    entity,
                    renderX,
                    renderY,
                    renderZ,
                    renderYRot,
                    partialTick,
                    event.getPoseStack(),
                    outlineBufferSource,
                    entityRenderDispatcher.getPackedLightCoords(entity, partialTick)
            );
        }
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.clearPending();
        de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.clearClient();
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.clearClient();
        SandevistanClientEffects.clear();
        CyberpsychosisClientState.setControlLocked(false);
        ArchiveBankingClientState.clear();
        ArchiveContactsClientState.clear();
        ArchiveQuestsClientState.clear();
        CyberwareHudClientState.clear();
        PsychosisOverlayClientState.clear();
        FaceHazardHighlightClientState.clear();
        jumpKeyWasDown = false;
        airborneLegJumpArmed = false;
        psychosisMouseSuppressed = false;
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen
                || screen instanceof RipperStationScreen
                || screen instanceof TechStationScreen
                || screen instanceof RecyclerStationScreen)) {
            return;
        }

        double scale = screen instanceof ResponsiveUi ? 1 : ResponsiveUi.referenceViewport().renderScale();
        int width = Math.max(1, (int) Math.ceil(78 * scale));
        int height = Math.max(1, (int) Math.ceil(18 * scale));
        int x = screen.width - width - (int) Math.ceil(12 * scale);
        int y = (int) Math.ceil(10 * scale);
        event.addListener(new WikiLaunchButton(
                x,
                y,
                width,
                height,
                Component.translatable("screen.cyberneticenhancements.archive.button"),
                () -> openArchiveScreen(Minecraft.getInstance(), screen, null)
        ));
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)
                || event.getScreen() instanceof CyberwareWikiScreen) {
            return;
        }

        ItemStack hoveredStack = hoveredContainerStack(containerScreen);
        String entryId = CyberwareWikiScreen.findEntryId(hoveredStack);
        if (entryId == null) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component hint = Component.translatable("screen.cyberneticenhancements.archive.hover_hint", ModKeyMappings.OPEN_ARCHIVE.getTranslatedKeyMessage());
        int width = font.width(hint) + 10;
        UiViewport viewport = event.getScreen() instanceof ResponsiveUi responsive
                ? responsive.viewport() : ResponsiveUi.referenceViewport();
        event.getGuiGraphics().flush();
        GuiGraphics graphics = new UiGraphics(event.getGuiGraphics(), viewport);
        int x = viewport.width() - width - 12;
        int y = 34;
        UiRenderer.panel(graphics, x, y, width, 16, UiRenderer.PANEL, UiRenderer.FIELD_EDGE);
        graphics.drawString(font, hint, x + 5, y + 4, UiRenderer.ACCENT, false);
        graphics.flush();
    }

    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)
                || event.getScreen() instanceof CyberwareWikiScreen) {
            return;
        }

        if (event.getScreen().getFocused() instanceof EditBox editBox
                && editBox.isVisible()
                && editBox.isFocused()) {
            return;
        }

        if (!ModKeyMappings.OPEN_ARCHIVE.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String entryId = CyberwareWikiScreen.findEntryId(hoveredContainerStack(containerScreen));
        openArchiveScreen(minecraft, event.getScreen(), entryId);
        event.setCanceled(true);
    }

    private static Entity resolveHighlightedEntity(Minecraft minecraft, UUID entityId) {
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        for (Entity entity : minecraft.level.getEntitiesOfClass(Entity.class, minecraft.player.getBoundingBox().inflate(64.0D), candidate -> candidate.getUUID().equals(entityId))) {
            return entity;
        }
        return null;
    }

    private static void releaseControlKeys(Options options) {
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
        options.keyShift.setDown(false);
        options.keySprint.setDown(false);
        options.keyAttack.setDown(false);
        options.keyUse.setDown(false);
    }

    private static boolean shouldBlockWorldInput() {
        Minecraft minecraft = Minecraft.getInstance();
        return CyberpsychosisClientState.isControlLocked() && minecraft.screen == null;
    }

    private static void applyLockedPsychosisControl(Minecraft minecraft) {
        minecraft.player.setYRot(CyberpsychosisClientState.yaw());
        minecraft.player.setYHeadRot(CyberpsychosisClientState.yaw());
        minecraft.player.setYBodyRot(CyberpsychosisClientState.yaw());
        minecraft.player.setXRot(CyberpsychosisClientState.pitch());
        minecraft.player.yRotO = CyberpsychosisClientState.yaw();
        minecraft.player.xRotO = CyberpsychosisClientState.pitch();
    }

    private static void performLockedPsychosisJump(Minecraft minecraft) {
        if (!CyberpsychosisClientState.jump()
                || !minecraft.player.onGround()
                || minecraft.player.onClimbable()
                || minecraft.player.isInWaterOrBubble()) {
            return;
        }

        minecraft.player.jumpFromGround();
        minecraft.player.hasImpulse = true;
    }

    private static void suppressPsychosisMouseLook(Minecraft minecraft) {
        MouseHandler mouseHandler = minecraft.mouseHandler;
        initializeMouseReflection();
        double mouseX = mouseHandler.xpos();
        double mouseY = mouseHandler.ypos();

        setMouseDouble(mouseAccumulatedDxField, mouseHandler, 0.0D);
        setMouseDouble(mouseAccumulatedDyField, mouseHandler, 0.0D);
        setMouseDouble(mouseXField, mouseHandler, mouseX);
        setMouseDouble(mouseYField, mouseHandler, mouseY);
        setMouseBoolean(mouseIgnoreFirstMoveField, mouseHandler, true);
        invokeMouseIgnoreFirstMove(mouseHandler);
    }

    private static void enablePsychosisMouseSuppression(Minecraft minecraft) {
        if (psychosisMouseSuppressed || minecraft.screen != null) {
            return;
        }

        initializeMouseReflection();
        setMouseBoolean(mouseGrabbedField, minecraft.mouseHandler, false);
        psychosisMouseSuppressed = true;
    }

    private static void disablePsychosisMouseSuppression(Minecraft minecraft) {
        if (!psychosisMouseSuppressed) {
            return;
        }

        initializeMouseReflection();
        setMouseBoolean(mouseGrabbedField, minecraft.mouseHandler, true);
        if (minecraft.screen == null) {
            suppressPsychosisMouseLook(minecraft);
        }
        psychosisMouseSuppressed = false;
    }

    private static void initializeMouseReflection() {
        if (mouseReflectionInitialized) {
            return;
        }
        mouseReflectionInitialized = true;

        mouseAccumulatedDxField = resolveMouseField("accumulatedDX", "f_91516_");
        mouseAccumulatedDyField = resolveMouseField("accumulatedDY", "f_91517_");
        mouseXField = resolveMouseField("xpos", "f_91507_");
        mouseYField = resolveMouseField("ypos", "f_91508_");
        mouseGrabbedField = resolveMouseField("mouseGrabbed", "f_91520_");
        mouseIgnoreFirstMoveField = resolveMouseField("ignoreFirstMove", "f_91511_");
        mouseSetIgnoreFirstMoveMethod = resolveMouseMethod("setIgnoreFirstMove", "m_91599_", "m_91603_");
    }

    private static Field resolveMouseField(String... candidateNames) {
        for (String candidateName : candidateNames) {
            try {
                Field field = MouseHandler.class.getDeclaredField(candidateName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Method resolveMouseMethod(String... candidateNames) {
        for (String candidateName : candidateNames) {
            try {
                Method method = MouseHandler.class.getDeclaredMethod(candidateName);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void setMouseDouble(Field field, MouseHandler mouseHandler, double value) {
        if (field == null) {
            return;
        }
        try {
            field.setDouble(mouseHandler, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void setMouseBoolean(Field field, MouseHandler mouseHandler, boolean value) {
        if (field == null) {
            return;
        }
        try {
            field.setBoolean(mouseHandler, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void invokeMouseIgnoreFirstMove(MouseHandler mouseHandler) {
        if (mouseSetIgnoreFirstMoveMethod == null) {
            return;
        }
        try {
            mouseSetIgnoreFirstMoveMethod.invoke(mouseHandler);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void openArchiveScreen(Minecraft minecraft, Screen parent, String entryId) {
        if (minecraft == null || minecraft.level == null || minecraft.screen instanceof CyberwareWikiScreen) {
            return;
        }
        if (minecraft.player != null) {
            PacketDistributor.sendToServer(new ArchiveBankingRequestPayload());
            PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
            PacketDistributor.sendToServer(new ArchiveQuestsRequestPayload());
        }
        minecraft.setScreen(entryId == null ? new CyberwareWikiScreen(parent) : new CyberwareWikiScreen(parent, entryId));
    }

    private static String resolveArchiveEntryId(Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }

        if (minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String hoveredEntry = CyberwareWikiScreen.findEntryId(hoveredContainerStack(containerScreen));
            if (hoveredEntry != null) {
                return hoveredEntry;
            }
        }

        if (minecraft.level != null && minecraft.hitResult instanceof BlockHitResult blockHitResult) {
            return CyberwareWikiScreen.findEntryId(minecraft.level.getBlockState(blockHitResult.getBlockPos()).getBlock());
        }
        return null;
    }

    private static ItemStack hoveredContainerStack(AbstractContainerScreen<?> containerScreen) {
        Slot hoveredSlot = hoveredSlot(containerScreen);
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        return hoveredSlot.getItem();
    }

    private static Slot hoveredSlot(AbstractContainerScreen<?> containerScreen) {
        initializeContainerScreenReflection();
        if (containerHoveredSlotField == null) {
            return null;
        }
        try {
            return (Slot) containerHoveredSlotField.get(containerScreen);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static void initializeContainerScreenReflection() {
        if (containerReflectionInitialized) {
            return;
        }
        containerReflectionInitialized = true;
        containerHoveredSlotField = resolveContainerScreenField("hoveredSlot", "f_97716_");
    }

    private static Field resolveContainerScreenField(String... candidateNames) {
        for (String candidateName : candidateNames) {
            try {
                Field field = AbstractContainerScreen.class.getDeclaredField(candidateName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
