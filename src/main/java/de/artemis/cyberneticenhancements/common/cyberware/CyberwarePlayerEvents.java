package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CyberwarePlayerEvents {
    private CyberwarePlayerEvents() {
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        de.artemis.cyberneticenhancements.common.world.TraumaCoverage.copy(event.getOriginal(), event.getEntity()); SandevistanManager.copyState(event.getOriginal(), event.getEntity());
        PlayerCyberwareInventory.copyStoredInventory(event.getOriginal(), event.getEntity());
        CyberstrainManager.copyState(event.getOriginal(), event.getEntity());
        CyberConsumableManager.copyState(event.getOriginal(), event.getEntity());
        TemporaryCyberwareEffectManager.copyState(event.getOriginal(), event.getEntity());
        PlayerEurodollarManager.copyState(event.getOriginal(), event.getEntity());
        PlayerQuestManager.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity.copyRelations(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.block.RelicCacheBlock.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.world.BannerNavigation.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.world.NightCityNavigation.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.world.RelicCacheIntel.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.world.NpcDisposition.copyState(event.getOriginal(), event.getEntity());
        de.artemis.cyberneticenhancements.common.dialogue.NpcIncomingCalls.copyState(event.getOriginal(), event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (event.isWasDeath()) CyberstrainManager.clearPsychosis(serverPlayer);
            CyberwareEffects.refreshPlayerCyberware(serverPlayer);
            CyberstrainManager.syncControlLock(serverPlayer);
            PsychosisBossBarManager.onPlayerLogin(serverPlayer);
            PsychosisBossBarManager.update(serverPlayer);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NightCityNavigation.onLogin(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.BannerNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.BannerNavigation.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.quest.QuestNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.quest.QuestNavigation.sync(serverPlayer);
            CyberwareEffects.refreshPlayerCyberware(serverPlayer);
            CyberstrainManager.syncControlLock(serverPlayer);
            PsychosisBossBarManager.onPlayerLogin(serverPlayer);
            PsychosisBossBarManager.update(serverPlayer);
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PsychosisIntro.sync(serverPlayer);
            PsychosisRecovery.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NightCityNavigation.onLogin(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.BannerNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.BannerNavigation.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.quest.QuestNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.quest.QuestNavigation.sync(serverPlayer);
            de.artemis.cyberneticenhancements.common.economy.BankingPlayerDirectory.get(serverPlayer.server).update(serverPlayer);
            CyberwareEffects.refreshPlayerCyberware(serverPlayer);
            CyberstrainManager.syncControlLock(serverPlayer);
            PsychosisBossBarManager.onPlayerLogin(serverPlayer);
            PsychosisBossBarManager.update(serverPlayer);
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PsychosisPursuitController.clear(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.MaxTacResponse.onLogout(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.NightCityNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.world.BannerNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.quest.QuestNavigation.reset(serverPlayer);
            de.artemis.cyberneticenhancements.common.economy.BankingPlayerDirectory.get(serverPlayer.server).update(serverPlayer);
            CyberwareAbilities.onPlayerLogout(serverPlayer);
            PsychosisBossBarManager.onPlayerLogout(serverPlayer);
        }
    }
}
