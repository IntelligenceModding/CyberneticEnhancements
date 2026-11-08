package de.artemis.cyberneticenhancements;

import de.artemis.cyberneticenhancements.client.ClientModEvents;
import de.artemis.cyberneticenhancements.client.ModKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CyberneticEnhancements.MOD_ID, dist = Dist.CLIENT)
public class CyberneticEnhancementsClient {
    public CyberneticEnhancementsClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::onClientSetup);
        modEventBus.addListener(ClientModEvents::registerRenderers);
        modEventBus.addListener(de.artemis.cyberneticenhancements.client.render.NpcSkinModels::registerReloadListeners);
        modEventBus.addListener(ClientModEvents::registerScreens);
        modEventBus.addListener(ClientModEvents::registerTooltipComponents);
        modEventBus.addListener(ModKeyMappings::register);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
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
