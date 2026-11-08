package de.artemis.cyberneticenhancements;

import de.artemis.cyberneticenhancements.common.command.CyberwareDebugCommand;
import de.artemis.cyberneticenhancements.common.command.CyberpsychosisCommand;
import de.artemis.cyberneticenhancements.common.command.MoneyCommand;
import de.artemis.cyberneticenhancements.common.command.QuestCommand;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CirculatoryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareBalance;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffects;
import de.artemis.cyberneticenhancements.common.cyberware.HandsCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.IntegumentaryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.NervousSystemCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwarePlayerEvents;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.datagen.DataGenerators;
import de.artemis.cyberneticenhancements.common.network.ModPayloads;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModCreativeModeTabs;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeSerializers;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import de.artemis.cyberneticenhancements.common.world.FixerVillageSpawner;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig;
import de.artemis.cyberneticenhancements.common.world.RelicCacheWorldgen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(CyberneticEnhancements.MOD_ID)
public class CyberneticEnhancements {
    public static final String MOD_ID = "cyberneticenhancements";

    public CyberneticEnhancements(IEventBus modEventBus) {
        CyberwareBalance.initialize();
        NpcIdentityConfig.initialize();
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        modEventBus.addListener(DataGenerators::gatherData);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(ModEntityTypes::registerAttributes);
        NeoForge.EVENT_BUS.addListener(CyberwareEffects::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CyberwarePlayerEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(CyberwarePlayerEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(CyberwarePlayerEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(CyberwarePlayerEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(CyberstrainManager::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(CyberwareEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatStatusManager::onKnockback);
        NeoForge.EVENT_BUS.addListener(CirculatoryCyberwareManager::onKnockback);
        NeoForge.EVENT_BUS.addListener(ArmCyberwareManager::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(HandsCyberwareManager::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(HandsCyberwareManager::onKnockback);
        NeoForge.EVENT_BUS.addListener(HandsCyberwareManager::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(IntegumentaryCyberwareManager::onKnockback);
        NeoForge.EVENT_BUS.addListener(NervousSystemCyberwareManager::onKnockback);
        NeoForge.EVENT_BUS.addListener(CombatStatusManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(CyberwareEffects::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(PlayerQuestManager::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(PlayerQuestManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerQuestManager::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CyberneticEnhancements::registerCommands);
        NeoForge.EVENT_BUS.addListener(FixerVillageSpawner::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(FixerVillageSpawner::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(FixerVillageSpawner::onLevelTick);
        NeoForge.EVENT_BUS.addListener(FixerVillageSpawner::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(RelicCacheWorldgen::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(RelicCacheWorldgen::onLevelTick);
        NeoForge.EVENT_BUS.addListener(RelicCacheWorldgen::onLevelUnload);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        CyberwareDebugCommand.register(event.getDispatcher());
        CyberpsychosisCommand.register(event.getDispatcher());
        MoneyCommand.register(event.getDispatcher());
        QuestCommand.register(event.getDispatcher());
    }
}
