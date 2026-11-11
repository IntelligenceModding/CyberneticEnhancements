package de.artemis.cyberneticenhancements;

import de.artemis.cyberneticenhancements.client.ClientModEvents;
import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.ModKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CyberneticEnhancements.MOD_ID, dist = Dist.CLIENT)
public class CyberneticEnhancementsClient {
    public CyberneticEnhancementsClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) ConfigurationScreen::new);
        modEventBus.addListener(ClientModEvents::onClientSetup);
        modEventBus.addListener(ClientModEvents::registerRenderers);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.SandevistanEchoRenderer::registerModels);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.RipperStationHighlightLayer::registerLayers);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.RipperStationHighlightLayer::registerShaders);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.HologramRenderTypes::registerShaders);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.NpcSkinModels::registerReloadListeners);
        modEventBus.addListener(ClientModEvents::registerScreens);
        modEventBus.addListener(ClientModEvents::registerTooltipComponents);
        modEventBus.addListener(ModKeyMappings::register);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.ArchiveReferenceData::onRecipesUpdated);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.BetaWelcomeMessage::onLogin);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.BetaWelcomeMessage::onLogout);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.BetaWelcomeMessage::onTick);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.render.SandevistanEchoRenderer::onPlayerRendered);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.SandevistanClientEffects::onFov);
        NeoForge.EVENT_BUS.addListener(de.artemis.cyberneticenhancements.client.SandevistanClientEffects::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onMovementInput);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onInteractionInput);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onTooltipColor);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onRenderGuiLayer);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onScreenRender);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientLogout);
    }
}
