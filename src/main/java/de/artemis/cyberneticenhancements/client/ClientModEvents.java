package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Element;
import de.artemis.cyberneticenhancements.client.ArchiveBankingClientState;
import de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.RelicCacheBlockEntityRenderer;
import de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer;
import de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen;
import de.artemis.cyberneticenhancements.client.screen.FixerDialogScreen;
import de.artemis.cyberneticenhancements.client.screen.MapSettings;
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
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.TextInputFocus;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
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
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.UUID;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Field;

public final class ClientModEvents {
    private static final Map<ResourceLocation, Element> GAMEPLAY_HUD_LAYERS = Map.ofEntries(
            Map.entry(VanillaGuiLayers.CROSSHAIR, Element.HOTBAR), Map.entry(VanillaGuiLayers.HOTBAR, Element.HOTBAR),
            Map.entry(VanillaGuiLayers.JUMP_METER, Element.HOTBAR), Map.entry(VanillaGuiLayers.EXPERIENCE_BAR, Element.HOTBAR),
            Map.entry(VanillaGuiLayers.EXPERIENCE_LEVEL, Element.HOTBAR), Map.entry(VanillaGuiLayers.SELECTED_ITEM_NAME, Element.HOTBAR),
            Map.entry(VanillaGuiLayers.SPECTATOR_TOOLTIP, Element.HOTBAR), Map.entry(VanillaGuiLayers.PLAYER_HEALTH, Element.VITALS),
            Map.entry(VanillaGuiLayers.ARMOR_LEVEL, Element.VITALS), Map.entry(VanillaGuiLayers.FOOD_LEVEL, Element.VITALS),
            Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, Element.VITALS), Map.entry(VanillaGuiLayers.AIR_LEVEL, Element.VITALS),
            Map.entry(VanillaGuiLayers.EFFECTS, Element.EFFECTS), Map.entry(VanillaGuiLayers.BOSS_OVERLAY, Element.BOSS_BARS),
            Map.entry(VanillaGuiLayers.CHAT, Element.CHAT), Map.entry(VanillaGuiLayers.SCOREBOARD_SIDEBAR, Element.NOTIFICATIONS),
            Map.entry(VanillaGuiLayers.OVERLAY_MESSAGE, Element.NOTIFICATIONS), Map.entry(VanillaGuiLayers.TITLE, Element.NOTIFICATIONS),
            Map.entry(VanillaGuiLayers.TAB_LIST, Element.NOTIFICATIONS));
    private static final int HAZARD_OUTLINE_R = 0x1E;
    private static final int HAZARD_OUTLINE_G = 0xE2;
    private static final int HAZARD_OUTLINE_B = 0xB5;
    private static boolean jumpKeyWasDown;
    private static boolean airborneLegJumpArmed;
    private static Field containerHoveredSlotField;
    private static boolean containerReflectionInitialized;

    private ClientModEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SandevistanClientOptions::initialize);
        event.enqueueWork(() -> ModBlocks.allRelicCaches().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.translucent())));
    }

    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(de.artemis.cyberneticenhancements.common.registry.ModParticles.RISING_BUBBLE.get(),
                de.artemis.cyberneticenhancements.client.particle.RisingBubbleParticle.Provider::new);
        event.registerSpriteSet(de.artemis.cyberneticenhancements.common.registry.ModParticles.VENT_SMOKE.get(),
                de.artemis.cyberneticenhancements.client.particle.VentSmokeParticle.Provider::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HOLO_BARRIER.get(), de.artemis.cyberneticenhancements.client.render.HoloBarrierRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DROPOFF_POINT.get(), de.artemis.cyberneticenhancements.client.render.DropoffPointBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FAST_TRAVEL.get(), de.artemis.cyberneticenhancements.client.render.FastTravelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CRATE.get(), de.artemis.cyberneticenhancements.client.render.CrateBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RIPPER_STATION.get(), RipperStationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RELIC_CACHE.get(), RelicCacheBlockEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.HANGING_WIRE.get(), de.artemis.cyberneticenhancements.client.render.HangingWireRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FIXER.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RIPPERDOC.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.STREET_RIPPERDOC.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.WORLD_ENCOUNTER_NPC.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TECHIE.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.NETRUNNER.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEV_TEST.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MERC.get(), FixerEntityRenderer::new); event.registerEntityRenderer(ModEntityTypes.MED_TECH.get(), FixerEntityRenderer::new); event.registerEntityRenderer(ModEntityTypes.TRAUMA_TEAM.get(), FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CYBERPSYCHO.get(), de.artemis.cyberneticenhancements.client.render.CyberpsychoRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ENCOUNTER_BODY.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TAROT_CARD.get(), de.artemis.cyberneticenhancements.client.render.TarotCardRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MAXTEC_GUARD.get(), de.artemis.cyberneticenhancements.client.render.FixerEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MAX_TAC.get(), de.artemis.cyberneticenhancements.client.render.CyberpsychoRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GANG_MEMBER.get(), de.artemis.cyberneticenhancements.client.render.CyberpsychoRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CYBERPSYCHO_MISSILE.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 0.65F, true));
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FABRICATOR.get(), de.artemis.cyberneticenhancements.client.screen.FabricatorScreen::new);
        event.register(ModMenuTypes.RIPPER_STATION.get(), RipperStationScreen::new);
        event.register(ModMenuTypes.TECH_STATION.get(), TechStationScreen::new);
        event.register(ModMenuTypes.UPGRADE_STATION.get(), TechStationScreen::new);
        event.register(ModMenuTypes.RECYCLER_STATION.get(), RecyclerStationScreen::new);
        event.register(ModMenuTypes.CRATE.get(), net.minecraft.client.gui.screens.inventory.ContainerScreen::new);
        event.register(ModMenuTypes.FIXER_DIALOG.get(), FixerDialogScreen::new);
        event.register(ModMenuTypes.RELIC_CACHE_HACK.get(), RelicCacheHackScreen::new);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(UpgradeProgressTooltip.class, UpgradeProgressClientTooltip::new);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        NpcDispositionClientState.tick();
        PlayerCallClient.tick();
        Minecraft minecraft = Minecraft.getInstance();
        SandevistanClientEffects.tick();
        NpcContactsOverlay.tick();
        NpcCallOverlay.tick();
        FaceDialogueOverlay.tick();
        QuestProgressOverlay.tick();
        QuestCursorTracker.tick();
        if (minecraft.player == null) {
            return;
        }

        WorldMapCache.tick();
        FaceHazardHighlightClientState.prune();

        if (CyberpsychosisClientState.isControlLocked()) {
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> || minecraft.screen instanceof ResponsiveUi || minecraft.screen instanceof net.minecraft.client.gui.screens.ChatScreen) {
                var closing = new de.artemis.cyberneticenhancements.client.ui.PsychosisClosingScreen();
                minecraft.player.closeContainer(); minecraft.setScreen(closing);
            }
            if (minecraft.screen != null) {
                jumpKeyWasDown = false;
                airborneLegJumpArmed = false;
                return;
            }



            releaseControlKeys(minecraft.options);
            jumpKeyWasDown = false;
            airborneLegJumpArmed = false;
            applyLockedPsychosisControl(minecraft);
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

        while (ModKeyMappings.ADD_COMPASS_MARKER.consumeClick()) if (TutorialClient.shows("markers")) CustomCompassMarkers.add();
        while (ModKeyMappings.TOGGLE_HUD.consumeClick()) {
            if (HudVisibilityController.enforced()) {
                minecraft.player.displayClientMessage(Icons.INFO.withText(Component.translatable("message.cyberneticenhancements.hud.enforced")), true);
                continue;
            }
            boolean enabled = HudVisibilityController.toggleHud();
            minecraft.player.displayClientMessage(
                    Icons.INFO.withText(Component.translatable(enabled
                            ? "message.cyberneticenhancements.hud.enabled"
                            : "message.cyberneticenhancements.hud.disabled")),
                    true
            );
        }
        while (ModKeyMappings.TOGGLE_MINIMAP.consumeClick()) {
            boolean shown = de.artemis.cyberneticenhancements.client.screen.MapSettings.toggleMinimap();
            minecraft.player.displayClientMessage(Icons.INFO.withText(Component.translatable(shown
                    ? "message.cyberneticenhancements.minimap.enabled" : "message.cyberneticenhancements.minimap.disabled")), true);
        }
        while (ModKeyMappings.OPEN_ARCHIVE.consumeClick()) {
            if (!TutorialClient.shows("archive")) continue;
            openArchiveScreen(minecraft, minecraft.screen, resolveArchiveEntryId(minecraft));
        }
        while (ModKeyMappings.OPEN_MAP.consumeClick()) {
            if (!TutorialClient.shows("archive") || minecraft.screen != null) continue;
            openMapScreen(minecraft);
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

    public static void onRenderGui(RenderGuiEvent.Pre event) {
        CyberwareHudOverlay.clearChatOffset();
        if (de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation.hidesGameplayUi()) { event.setCanceled(true); return; }

    }

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        var element = GAMEPLAY_HUD_LAYERS.get(event.getName());
        boolean hiddenHud = element != null && !GameplayHudVisibility.shows(element, true);
        boolean savingInPauseMenu = VanillaGuiLayers.SAVING_INDICATOR.equals(event.getName())
                && hasCenteredSavingIndicator(Minecraft.getInstance().screen);
        if (hiddenHud || savingInPauseMenu) {
            event.setCanceled(true);
            if (VanillaGuiLayers.HOTBAR.equals(event.getName()) || VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName())) {
                onRenderGuiLayer(new RenderGuiLayerEvent.Post(event.getGuiGraphics(), event.getPartialTick(), event.getName(), event.getLayer()));
            }
        }
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!GameplayHudVisibility.status()) return;
        var graphics = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName())) {
            if (TutorialClient.shows("compass")) CompassHudOverlay.render(graphics, partial);
        }
        if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            if (GameplayHudVisibility.gameplay()) {
                TraumaRescueClient.render(graphics);
                SandevistanClientEffects.renderOverlay(graphics, partial);
                if (TutorialClient.shows("compass") && GameplayHudVisibility.shows(Element.HOTBAR, true))
                    CompassHudOverlay.renderHotbarFocus(graphics, partial);
            }
            renderArchiveControlledHud(graphics);
        }
    }

    private static void renderArchiveControlledHud(GuiGraphics graphics) {
        if (GameplayHudVisibility.shows(Element.TAROT, true)) {
            TarotDecorationClient.renderHud(graphics);
            TarotScanClient.render(graphics);
        }
        if (TutorialClient.shows("contacts") && GameplayHudVisibility.shows(Element.CONTACTS, true)) NpcContactsOverlay.render(graphics);
        if (GameplayHudVisibility.shows(Element.CALLS, true)) NpcCallOverlay.render(graphics);
        GameplayHudVisibility.renderToggleable(Element.MINIMAP, MapSettings.MINIMAP.on(), graphics,
                () -> MinimapHud.render(graphics, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
        if (TutorialClient.shows("quests")) GameplayHudVisibility.renderToggleable(Element.QUESTS, true, graphics, () -> QuestProgressOverlay.render(graphics));
        if (!OriginClient.hidesHud()) {
            FaceDialogueOverlay.render(graphics);
            StreetCredHud.render(graphics);
        }
        if (GameplayHudVisibility.shows(Element.TUTORIAL, true)) TutorialClient.render(graphics);
        if (TutorialClient.shows("cyberware")) GameplayHudVisibility.renderToggleable(Element.CYBERWARE, true, graphics, () -> CyberwareHudOverlay.render(graphics));
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        de.artemis.cyberneticenhancements.client.render.CrateBlockEntityRenderer.drawMarks(event);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            de.artemis.cyberneticenhancements.client.render.DropoffPointBlockEntityRenderer.flushProjections();
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                && !de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.isShadowPass()) {

            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.flushProjections();
            de.artemis.cyberneticenhancements.client.render.DropoffPointBlockEntityRenderer.queueQuestTurnIns(
                    event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            de.artemis.cyberneticenhancements.client.render.DropoffPointBlockEntityRenderer.captureOpaqueDepth();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (CyberpsychosisClientState.isControlLocked() && minecraft.player != null && minecraft.screen == null) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) CyberpsychosisClientState.tickSmoothing();

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
        WorldMapCache.flush();
        PlayerCallClient.clear();
        de.artemis.cyberneticenhancements.client.BarredSitesClientState.clear();
        JournalClientState.clear();
        NpcContactsOverlay.reset();
        NpcCallOverlay.close();
        NpcCallOverlay.reset();
        FaceDialogueOverlay.clear();
        de.artemis.cyberneticenhancements.client.render.RipperStationBlockEntityRenderer.clearPending();
        de.artemis.cyberneticenhancements.client.render.DropoffPointBlockEntityRenderer.clearPending();
        de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.clearClient();
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.clearClient();
        SandevistanClientEffects.clear();
        CyberpsychosisClientState.setControlLocked(false);
        PsychosisIntroClient.clear();
        ArchiveBankingClientState.clear();
        ArchiveContactsClientState.clear();
        ArchiveQuestsClientState.clear();
        QuestNavigationClientState.clear();
        QuestProgressOverlay.clear();
        HudVisibilityController.clear();
        BannerWaypointsClientState.clear();
        CityNavigationClientState.clear();
        NpcDispositionClientState.clear();
        de.artemis.cyberneticenhancements.common.economy.EconomyBalance.clearClient();
        CompassPreferences.clearCache();
        CyberwareHudClientState.clear();
        PsychosisOverlayClientState.clear();
        FaceHazardHighlightClientState.clear();
        jumpKeyWasDown = false;
        airborneLegJumpArmed = false;
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?>) || !TutorialClient.shows("archive")) return;

        double scale = screen instanceof ResponsiveUi ? 1 : ResponsiveUi.referenceViewport().renderScale();
        int width = Math.max(1, (int) Math.ceil(78 * scale));
        int height = Math.max(1, (int) Math.ceil(18 * scale));
        int x = 8;
        int y = screen.height - height - 8;
        event.addListener(new WikiLaunchButton(
                screen,
                x,
                y,
                width,
                height,
                Component.translatable("screen.cyberneticenhancements.archive.button"),
                entry -> openArchiveScreen(Minecraft.getInstance(), screen, entry)
        ));
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        WikiLaunchButton.updateForScreen(event.getScreen());
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.DeathScreen
                || event.getScreen() instanceof net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen) {
            CustomCompassMarkers.died();
            TraumaRescueClient.render(event.getGuiGraphics());
        }
        FlatlinedScreenTransition.render(event.getGuiGraphics(), event.getScreen());
        if (de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation.hidesGameplayUi()) return;
        if (hasCenteredSavingIndicator(event.getScreen())) {
            renderPauseSavingIndicator(event.getGuiGraphics());
        }
        if (Minecraft.getInstance().level != null && !GameplayHudVisibility.status()
                && !(event.getScreen() instanceof CyberwareWikiScreen archive && archive.isHudTab())) {
            var graphics = event.getGuiGraphics();
            if (TutorialClient.shows("compass")) {
                if (event.getScreen() instanceof AbstractContainerScreen<?> container)
                    CompassHudOverlay.renderInventory(graphics, container, event.getMouseX(), event.getMouseY());
                else CompassHudOverlay.render(graphics, event.getPartialTick());
            }
            renderArchiveControlledHud(graphics);
            PsychosisTimerOverlay.render(graphics);
        }
    }

    public static boolean hasCenteredSavingIndicator(Screen screen) {
        return screen instanceof PauseScreen pause && pause.showsPauseMenu();
    }

    private static void renderPauseSavingIndicator(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        int textWidth = minecraft.font.width(Component.translatable("menu.savingLevel"));
        int vanillaX = graphics.guiWidth() - textWidth - 2;
        int vanillaY = graphics.guiHeight() - 35;
        int centeredX = (graphics.guiWidth() - textWidth) / 2;
        int belowTitleY = 53;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(centeredX - vanillaX, belowTitleY - vanillaY, 0);
            minecraft.gui.renderSavingIndicator(graphics, minecraft.getTimer());
        } finally {
            graphics.pose().popPose();
        }
    }

    public static void receiveDropoffDelivery() {
        var player = Minecraft.getInstance().player;
        if (player != null && ClientConfig.SHOW_DROPOFF_DELIVERY_MESSAGES.get())
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player,
                    Component.translatable("message.cyberneticenhancements.dropoff.arrived"));
    }

    public static String hoveredArchiveEntry(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container
                ? CyberwareWikiScreen.findEntryId(hoveredContainerStack(container)) : null;
    }

    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)
                || event.getScreen() instanceof CyberwareWikiScreen) {
            return;
        }

        if (TextInputFocus.isTyping(event.getScreen())) {
            return;
        }

        if (!ModKeyMappings.OPEN_ARCHIVE.matches(event.getKeyCode(), event.getScanCode()) || !TutorialClient.shows("archive")) {
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

    public static void receivePsychosisControl(de.artemis.cyberneticenhancements.common.network.CyberpsychosisControlPayload payload) {
        boolean wasLocked = CyberpsychosisClientState.isControlLocked();
        CyberpsychosisClientState.apply(payload.locked(), payload.yaw(), payload.pitch(), payload.forward(),
                payload.strafe(), payload.jump(), payload.sprint());
        if (!wasLocked || payload.locked()) return;
        Minecraft minecraft = Minecraft.getInstance();
        releaseControlKeys(minecraft.options);
        // Restore physically held keys rather than retaining the last synthetic jump/sprint.
        net.minecraft.client.KeyMapping.setAll();
        jumpKeyWasDown = false;
        airborneLegJumpArmed = false;
        if (minecraft.player == null) return;
        minecraft.player.setJumping(false);
        minecraft.player.setSprinting(false);
        var input = minecraft.player.input;
        if (input != null) {
            input.forwardImpulse = input.leftImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = input.shiftKeyDown = false;
        }
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
        var intro = PsychosisIntroClient.view(minecraft.player, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        if (intro != null) {
            CyberpsychosisClientState.alignView(intro.yaw(), intro.pitch());
            minecraft.player.setYRot(intro.yaw()); minecraft.player.setYHeadRot(intro.yaw());
            minecraft.player.setYBodyRot(intro.baseYaw()); minecraft.player.setXRot(intro.pitch());
            minecraft.player.yRotO=intro.yaw(); minecraft.player.xRotO=intro.pitch(); minecraft.player.yBodyRotO=intro.baseYaw(); return;
        }
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

    public static void openArchive(Minecraft minecraft, Screen parent, String entryId, String anchor) {
        openArchiveScreen(minecraft, parent, entryId, anchor);
    }

    private static void openArchiveScreen(Minecraft minecraft, Screen parent, String entryId) {
        openArchiveScreen(minecraft, parent, entryId, null);
    }

    private static void openArchiveScreen(Minecraft minecraft, Screen parent, String entryId, String anchor) {
        openArchiveScreen(minecraft, parent, entryId, anchor, null);
    }

    private static void openArchiveScreen(Minecraft minecraft, Screen parent, String entryId, String anchor, String tab) {
        if (minecraft == null || minecraft.level == null || minecraft.screen instanceof CyberwareWikiScreen) {
            return;
        }
        if (minecraft.player != null) {
            PacketDistributor.sendToServer(new ArchiveBankingRequestPayload());
            PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
            PacketDistributor.sendToServer(new ArchiveQuestsRequestPayload());
        }
        var screen = entryId == null ? new CyberwareWikiScreen(parent) : new CyberwareWikiScreen(parent, entryId);
        if (anchor != null) screen.jumpTo(anchor);
        if (tab != null) screen.selectTab(tab);
        minecraft.setScreen(screen);
    }

    private static void openMapScreen(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        PacketDistributor.sendToServer(new ArchiveBankingRequestPayload());
        PacketDistributor.sendToServer(new ArchiveContactsRequestPayload());
        PacketDistributor.sendToServer(new ArchiveQuestsRequestPayload());
        minecraft.setScreen(CyberwareWikiScreen.forMap(null));
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
