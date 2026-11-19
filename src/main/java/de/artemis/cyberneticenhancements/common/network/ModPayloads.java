package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleHandler;
import de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.FrontalCortexManager;
import de.artemis.cyberneticenhancements.common.cyberware.LegCyberwareManager;
import de.artemis.cyberneticenhancements.client.ArchiveBankingClientState;
import de.artemis.cyberneticenhancements.client.CyberwareHudClientState;
import de.artemis.cyberneticenhancements.client.ArchiveContactsClientState;
import de.artemis.cyberneticenhancements.client.ArchiveQuestsClientState;
import de.artemis.cyberneticenhancements.client.FaceHazardHighlightClientState;
import de.artemis.cyberneticenhancements.client.PsychosisOverlayClientState;
import de.artemis.cyberneticenhancements.client.PlayerMotionSyncClient;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu;
import de.artemis.cyberneticenhancements.common.menu.RipperStationMenu;
import de.artemis.cyberneticenhancements.common.menu.TechStationMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransferPayload.TYPE,
                de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TechStationMenu menu
                            && menu.containerId == payload.containerId()) menu.transferUpgrade(player, payload.recipe(), payload.level(), payload.maximum());
                }));
        event.registrar("1").playToServer(de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransferPayload.TYPE,
                de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TechStationMenu menu
                            && menu.containerId == payload.containerId()) menu.transferRepair(player, payload.recipe(), payload.kind(), payload.maximum());
                }));
        event.registrar("1").playToServer(de.artemis.cyberneticenhancements.common.menu.RecyclerRecipeTransferPayload.TYPE,
                de.artemis.cyberneticenhancements.common.menu.RecyclerRecipeTransferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof RecyclerStationMenu menu
                            && menu.containerId == payload.containerId()) menu.transferRecipe(player, payload.recipe(), payload.maximum());
                }));
        event.registrar("1").playToServer(de.artemis.cyberneticenhancements.common.fabrication.FabricatorRecipeTransferPayload.TYPE,
                de.artemis.cyberneticenhancements.common.fabrication.FabricatorRecipeTransferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof de.artemis.cyberneticenhancements.common.fabrication.FabricatorMenu menu
                            && menu.containerId == payload.containerId()) menu.transferRecipe(player, payload.recipe(), payload.maximum());
                }));
        event.registrar("1").playToServer(de.artemis.cyberneticenhancements.common.tarot.TarotDecorationPayload.TYPE,
                de.artemis.cyberneticenhancements.common.tarot.TarotDecorationPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer player) {
                        boolean placed = de.artemis.cyberneticenhancements.common.tarot.TarotDecorations.place(player, p.cardId(), p.placement());
                        PacketDistributor.sendToPlayer(player, new de.artemis.cyberneticenhancements.common.tarot.TarotDecorationResultPayload(p.cardId(), placed));
                    }
                }))
                .playToClient(de.artemis.cyberneticenhancements.common.tarot.TarotDecorationResultPayload.TYPE,
                        de.artemis.cyberneticenhancements.common.tarot.TarotDecorationResultPayload.STREAM_CODEC,
                        (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.client.TarotDecorationClient.receive(p)));
        event.registrar("1").playToClient(de.artemis.cyberneticenhancements.common.tutorial.TutorialStatePayload.TYPE,
                de.artemis.cyberneticenhancements.common.tutorial.TutorialStatePayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.client.TutorialClient.receive(p)))
                .playToServer(de.artemis.cyberneticenhancements.common.tutorial.TutorialSignalPayload.TYPE,
                        de.artemis.cyberneticenhancements.common.tutorial.TutorialSignalPayload.STREAM_CODEC,
                        (p, c) -> c.enqueueWork(() -> { if (c.player() instanceof ServerPlayer player)
                            de.artemis.cyberneticenhancements.common.tutorial.TutorialDirector.signal(player, p.signal()); }));
        event.registrar("1").playToClient(de.artemis.cyberneticenhancements.common.tarot.TarotKnowledgePayload.TYPE,
                de.artemis.cyberneticenhancements.common.tarot.TarotKnowledgePayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.common.tarot.TarotKnowledge.receive(p.ids())))
                .playToServer(de.artemis.cyberneticenhancements.common.tarot.TarotScanPayload.TYPE,
                        de.artemis.cyberneticenhancements.common.tarot.TarotScanPayload.STREAM_CODEC,
                        (p, c) -> c.enqueueWork(() -> { if (c.player() instanceof ServerPlayer player) de.artemis.cyberneticenhancements.common.tarot.TarotScanning.receive(player, p); }));
        event.registrar("1").playToServer(DeleteCompassMarkerPayload.TYPE, DeleteCompassMarkerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    if (payload.id().startsWith("chromium:"))
                        de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.deleteMarker(player, payload.id());
                    else if (payload.id().startsWith("cache:"))
                        de.artemis.cyberneticenhancements.common.world.RelicCacheIntel.deleteMarker(player, payload.id());
                    else de.artemis.cyberneticenhancements.common.world.BannerNavigation.deleteMarker(player, payload.id());
                }));
        event.registrar("2").playToClient(QuestNavigationPayload.TYPE, QuestNavigationPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.QuestNavigationClientState.update(payload)))
                .playToServer(NpcCallRequestPayload.TYPE, NpcCallRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player, payload);
                        }));
        event.registrar("1").playToClient(TraumaRescuePayload.TYPE, TraumaRescuePayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.client.TraumaRescueClient.receive(p)))
                .playToClient(DeathBodyPayload.TYPE, DeathBodyPayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.client.render.DeathBodies.receive(p)))
                .playToClient(PlayerDeploymentPayload.TYPE, PlayerDeploymentPayload.STREAM_CODEC,
                        (p, c) -> c.enqueueWork(() -> de.artemis.cyberneticenhancements.client.render.PlayerDeploymentRender.receive(p)))
                .playToServer(CallMedtecPayload.TYPE, CallMedtecPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.world.TraumaCoverage.callMedtec(player);
                        }))
                .playToServer(JournalRequestPayload.TYPE, JournalRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.journal.JournalEvents.request(player, payload);
                        }))
                .playToClient(JournalPayload.TYPE, JournalPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.JournalClientState.receive(payload)))
                .playToServer(PlayerCallPreferencesPayload.TYPE, PlayerCallPreferencesPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.dialogue.PlayerCallManager.preferences(player, payload.enabled());
                        }))
                .playToServer(PlayerCallRequestPayload.TYPE, PlayerCallRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.dialogue.PlayerCallManager.handle(player, payload);
                        }))
                .playToClient(PlayerCallStatePayload.TYPE, PlayerCallStatePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.NpcCallOverlay.receivePlayer(payload)))
                .playToClient(EconomyBalancePayload.TYPE, EconomyBalancePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.common.economy.EconomyBalance.receive(payload.json())))
                .playToClient(NpcDispositionPayload.TYPE, NpcDispositionPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.NpcDispositionClientState.update(payload)))
                .playToClient(CityNavigationPayload.TYPE, CityNavigationPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.CityNavigationClientState.update(payload)))
                .playToServer(CityNavigationRequestPayload.TYPE, CityNavigationRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                de.artemis.cyberneticenhancements.common.world.NightCityNavigation.setEnabled(player, payload.enabled());
                        }))
                .playToClient(BannerWaypointsPayload.TYPE, BannerWaypointsPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.BannerWaypointsClientState.update(payload)))
                .playToClient(QuestProgressPayload.TYPE, QuestProgressPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.QuestProgressOverlay.receive(payload)))
                .playToClient(BarredSitesPayload.TYPE, BarredSitesPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.BarredSitesClientState.update(payload)))
                .playToClient(DropoffDeliveryNoticePayload.TYPE, DropoffDeliveryNoticePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(de.artemis.cyberneticenhancements.client.ClientModEvents::receiveDropoffDelivery))
                .playToClient(QuestDropoffClaimResultPayload.TYPE, QuestDropoffClaimResultPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (net.minecraft.client.Minecraft.getInstance().screen instanceof de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen screen)
                                screen.onDropoffClaimResult(payload.granted());
                        }))
                .playBidirectional(QuestCursorPayload.TYPE, QuestCursorPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player)
                                PlayerQuestManager.updateCreativeCursor(player, payload.contract(), payload.itemType(), payload.revision());
                            else de.artemis.cyberneticenhancements.client.QuestCursorTracker.invalidate(payload);
                        }))
                .playToClient(NpcCallStatePayload.TYPE, NpcCallStatePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.NpcCallOverlay.receive(payload)))
                .playToClient(de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledgePayload.TYPE,
                        de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledgePayload.STREAM_CODEC, (payload, context) ->
                                context.enqueueWork(() -> de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.receiveDetails(payload.records())))
                .playToServer(de.artemis.cyberneticenhancements.common.fabrication.SchematicExtractionPayload.TYPE,
                        de.artemis.cyberneticenhancements.common.fabrication.SchematicExtractionPayload.STREAM_CODEC, (payload, context) ->
                                context.enqueueWork(() -> {
                                    if (context.player() instanceof ServerPlayer player)
                                        de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge.extract(player, payload.design());
                                }))
                .playToClient(CyberwareUpgradesPayload.TYPE, CyberwareUpgradesPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry.receive(payload.json())))
                .playToClient(SandevistanEchoPayload.TYPE, SandevistanEchoPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.render.SandevistanEchoRenderer.receive(payload)))
                .playToClient(SandevistanSnapshotPayload.TYPE, SandevistanSnapshotPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.SandevistanClientEffects.receive(payload)))
                .playToClient(PsychosisIntroPayload.TYPE, PsychosisIntroPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.PsychosisIntroClient.receive(payload)))
                .playToClient(CyberpsychosisControlPayload.TYPE, CyberpsychosisControlPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> de.artemis.cyberneticenhancements.client.ClientModEvents.receivePsychosisControl(payload)))
                .playToClient(CyberwareHudPayload.TYPE, CyberwareHudPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> CyberwareHudClientState.update(payload)))
                .playToClient(ArchiveBankingPayload.TYPE, ArchiveBankingPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> ArchiveBankingClientState.update(payload)))
                .playToClient(ArchiveContactsPayload.TYPE, ArchiveContactsPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> ArchiveContactsClientState.update(payload)))
                .playToClient(ArchiveQuestsPayload.TYPE, ArchiveQuestsPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> ArchiveQuestsClientState.update(payload)))
                .playToClient(PsychosisOverlayPayload.TYPE, PsychosisOverlayPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> PsychosisOverlayClientState.update(payload)))
                .playToClient(FaceHazardHighlightPayload.TYPE, FaceHazardHighlightPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> FaceHazardHighlightClientState.update(payload)))
                .playToClient(PlayerMotionSyncPayload.TYPE, PlayerMotionSyncPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> PlayerMotionSyncClient.apply(payload)))
                .playToServer(ActivateCyberwarePayload.TYPE, ActivateCyberwarePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> CyberwareAbilities.activate(context.player())))
                .playToServer(ActivateAuxiliaryCyberwarePayload.TYPE, ActivateAuxiliaryCyberwarePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> FrontalCortexManager.activateAuxiliary(context.player())))
                .playToServer(ActivateArmCyberwarePayload.TYPE, ActivateArmCyberwarePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> ArmCyberwareManager.activate(context.player())))
                .playToServer(ActivateFaceCyberwarePayload.TYPE, ActivateFaceCyberwarePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> FaceCyberwareManager.activate(context.player())))
                .playToServer(ActivateLegCyberwarePayload.TYPE, ActivateLegCyberwarePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> LegCyberwareManager.activateMidairJump(context.player())))
                .playToServer(UpgradeCyberwareSlotPayload.TYPE, UpgradeCyberwareSlotPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (payload.slotOrdinal() < 0 || payload.slotOrdinal() >= CyberwareSlot.values().length) {
                                return;
                            }
                            if (context.player().containerMenu instanceof RipperStationMenu menu) {
                                menu.tryUpgradeSupportedTier(CyberwareSlot.values()[payload.slotOrdinal()]);
                            }
                        }))
                .playToServer(UpgradeRipperSubSlotPayload.TYPE, UpgradeRipperSubSlotPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (!(context.player().containerMenu instanceof RipperStationMenu menu) || payload.slotIndex() < 0) {
                                return;
                            }

                            switch (payload.kind()) {
                                case UpgradeRipperSubSlotPayload.KIND_CHIPWARE -> {
                                    if (payload.handlerIndex() >= 0 && payload.handlerIndex() < 3 && payload.slotIndex() < 3) {
                                        menu.tryUpgradeChipSupportedTier(payload.handlerIndex(), payload.slotIndex());
                                    }
                                }
                                case UpgradeRipperSubSlotPayload.KIND_ARM_MODULE -> {
                                    if (payload.slotIndex() < CyberwareModuleHandler.MAX_MODULE_SLOTS) {
                                        menu.tryUpgradeArmModuleSupportedTier(payload.slotIndex());
                                    }
                                }
                                case UpgradeRipperSubSlotPayload.KIND_LEG_MODULE -> {
                                    if (payload.slotIndex() < CyberwareModuleHandler.MAX_MODULE_SLOTS) {
                                        menu.tryUpgradeLegModuleSupportedTier(payload.slotIndex());
                                    }
                                }
                                default -> {
                                }
                            }
                        }))
                .playToServer(RelicCacheHackSelectPayload.TYPE, RelicCacheHackSelectPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (payload.cellIndex() < 0) {
                                return;
                            }
                            if (context.player().containerMenu instanceof RelicCacheHackMenu menu
                                    && context.player() instanceof ServerPlayer) {
                                menu.trySelectCell(payload.cellIndex());
                            }
                        }))
                .playToServer(FixerDialogueChoicePayload.TYPE, FixerDialogueChoicePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player().containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                                    && context.player() instanceof ServerPlayer serverPlayer) {
                                menu.handleChoice(serverPlayer, payload.optionId());
                            }
                        }))
                .playToServer(NpcIdentityNicknamePayload.TYPE, NpcIdentityNicknamePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player().containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                                    && context.player() instanceof ServerPlayer serverPlayer) {
                                menu.handleNicknameUpdate(serverPlayer, payload.nickname());
                            }
                        }))
                .playToServer(NpcIdentityAppearancePayload.TYPE, NpcIdentityAppearancePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player().containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                                    && context.player() instanceof ServerPlayer serverPlayer) {
                                menu.handleAppearanceUpdate(serverPlayer, payload.appearanceId());
                            }
                        }))
                .playToServer(NpcIdentityNameColorPayload.TYPE, NpcIdentityNameColorPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player().containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                                    && context.player() instanceof ServerPlayer serverPlayer) {
                                menu.handleNameColorUpdate(serverPlayer, payload.colorId());
                            }
                        }))
                .playToServer(ArchiveContactsRequestPayload.TYPE, ArchiveContactsRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, ArchiveContactsPayload.capture(serverPlayer));
                            }
                        }))
                .playToServer(ArchiveBankingRequestPayload.TYPE, ArchiveBankingRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, ArchiveBankingPayload.capture(serverPlayer));
                            }
                        }))
                .playToServer(ArchiveQuestsRequestPayload.TYPE, ArchiveQuestsRequestPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, ArchiveQuestsPayload.capture(serverPlayer));
                            }
                        }))
                .playToServer(ArchiveBankingActionPayload.TYPE, ArchiveBankingActionPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                                return;
                            }
                            String failureKey = switch (payload.actionId()) {
                                case ArchiveBankingActionPayload.ACTION_SEND ->
                                        handleSendAction(serverPlayer, payload.targetName(), payload.amount());
                                case ArchiveBankingActionPayload.ACTION_REQUEST ->
                                        handleRequestAction(serverPlayer, payload.targetName(), payload.amount());
                                case ArchiveBankingActionPayload.ACTION_APPROVE ->
                                        handleApproveAction(serverPlayer, payload.requestId());
                                case ArchiveBankingActionPayload.ACTION_DECLINE ->
                                        handleDeclineAction(serverPlayer, payload.requestId());
                                default -> "message.cyberneticenhancements.archive.banking.action_unavailable";
                            };
                            if (failureKey != null) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(failureKey), true);
                                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(serverPlayer);
                            } else if (payload.actionId() == ArchiveBankingActionPayload.ACTION_DECLINE) {
                                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.CANCELLED.play(serverPlayer);
                            } else if (payload.actionId() == ArchiveBankingActionPayload.ACTION_REQUEST) {
                                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.TRANSFER.play(serverPlayer);
                            }
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveBankingPayload.capture(serverPlayer).withActionResult(payload.actionId(), failureKey));
                        }))
                .playToServer(ArchiveQuestActionPayload.TYPE, ArchiveQuestActionPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                                return;
                            }
                            if (payload.actionId() == ArchiveQuestActionPayload.ACTION_CLAIM_DROPOFF) {
                                boolean granted = de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries.get(serverPlayer.server).claimSetupItem(serverPlayer);
                                PacketDistributor.sendToPlayer(serverPlayer, ArchiveQuestsPayload.capture(serverPlayer));
                                PacketDistributor.sendToPlayer(serverPlayer, new QuestDropoffClaimResultPayload(granted));
                                return;
                            }
                            if (!de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries.get(serverPlayer.server).hasPlacedDropoff(serverPlayer.getUUID())) {
                                PacketDistributor.sendToPlayer(serverPlayer, ArchiveQuestsPayload.capture(serverPlayer));
                                return;
                            }
                            if (payload.actionId() == ArchiveQuestActionPayload.ACTION_TURN_IN
                                    && PlayerQuestManager.activeContracts(serverPlayer).stream().anyMatch(quest -> quest.id().equals(payload.questId()) && quest.type().requiresInPersonTurnIn())) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.cyberneticenhancements.archive.quest_turn_in_in_person"), true);
                                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(serverPlayer);
                                return;
                            }
                            boolean success = switch (payload.actionId()) {
                                case ArchiveQuestActionPayload.ACTION_DISCARD ->
                                        PlayerQuestManager.abandonQuestById(serverPlayer, payload.questId()) == PlayerQuestManager.Result.OK;
                                case ArchiveQuestActionPayload.ACTION_TURN_IN ->
                                        PlayerQuestManager.turnInRemotely(serverPlayer, payload.questId()) == PlayerQuestManager.TurnInResult.OK;
                                default -> false;
                            };
                            if (!success) {
                                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(serverPlayer);
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(payload.actionId() == ArchiveQuestActionPayload.ACTION_TURN_IN
                                        ? "message.cyberneticenhancements.archive.quest_turn_in_unavailable" : "message.cyberneticenhancements.archive.quest_discard_unavailable"), true);
                                return;
                            }
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveQuestsPayload.capture(serverPlayer));
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveContactsPayload.capture(serverPlayer));
                            serverPlayer.inventoryMenu.broadcastChanges();
                        }))
                .playToServer(ArchiveContactActionPayload.TYPE, ArchiveContactActionPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                                return;
                            }
                            java.util.UUID npcId;
                            try {
                                npcId = java.util.UUID.fromString(payload.npcId());
                            } catch (IllegalArgumentException ignored) {
                                return;
                            }

                            if (payload.actionId() == ArchiveContactActionPayload.ACTION_ACKNOWLEDGE_DECEASED) {
                                AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(serverPlayer, npcId);
                                return;
                            }
                            if (de.artemis.cyberneticenhancements.common.dialogue.PlayerCallManager.contactAction(serverPlayer, npcId, payload.actionId()))
                                return;
                            boolean success = switch (payload.actionId()) {
                                case ArchiveContactActionPayload.ACTION_TOGGLE_PIN ->
                                        AbstractCityNpcEntity.togglePinnedFromArchive(serverPlayer, npcId);
                                case ArchiveContactActionPayload.ACTION_TOGGLE_HIDE ->
                                        AbstractCityNpcEntity.toggleHiddenFromArchive(serverPlayer, npcId);
                                default -> false;
                            };
                            if (!success) {
                                String messageKey = switch (payload.actionId()) {
                                    case ArchiveContactActionPayload.ACTION_TOGGLE_PIN,
                                         ArchiveContactActionPayload.ACTION_TOGGLE_HIDE ->
                                            "message.cyberneticenhancements.archive.contact_update_unavailable";
                                    default -> "message.cyberneticenhancements.archive.contact_update_unavailable";
                                };
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(messageKey), true);
                                return;
                            }
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveContactsPayload.capture(serverPlayer));
                        }));
    }

    private static String handleSendAction(ServerPlayer source, String targetName, int amount) {
        ServerPlayer target = findOnlinePlayer(source, targetName);
        if (target == null) {
            return "message.cyberneticenhancements.archive.banking.target_missing";
        }
        PlayerEurodollarManager.TransferResult result = PlayerEurodollarManager.sendToPlayer(source, target, amount);
        if (result == PlayerEurodollarManager.TransferResult.OK) {
            PacketDistributor.sendToPlayer(target, ArchiveBankingPayload.capture(target));
        }
        return bankingFailureKey(result);
    }

    private static String handleRequestAction(ServerPlayer source, String targetName, int amount) {
        ServerPlayer target = findOnlinePlayer(source, targetName);
        PlayerEurodollarManager.TransferResult result;
        if (target != null) {
            result = PlayerEurodollarManager.createRequest(source, target, amount);
            if (result == PlayerEurodollarManager.TransferResult.OK) {
                PacketDistributor.sendToPlayer(target, ArchiveBankingPayload.capture(target));
            }
            return bankingFailureKey(result);
        }
        var known = de.artemis.cyberneticenhancements.common.economy.BankingPlayerDirectory.get(source.server).find(targetName);
        if (known != null)
            return bankingFailureKey(PlayerEurodollarManager.createRequest(source, known.id(), known.name(), amount));
        java.util.Optional<com.mojang.authlib.GameProfile> profile = source.server.getProfileCache().get(targetName.trim());
        if (profile.isEmpty() || profile.get().getId() == null || profile.get().getName() == null || profile.get().getName().isBlank()) {
            return "message.cyberneticenhancements.archive.banking.target_missing";
        }
        result = PlayerEurodollarManager.createRequest(source, profile.get().getId(), profile.get().getName(), amount);
        return bankingFailureKey(result);
    }

    private static String handleApproveAction(ServerPlayer source, String requestIdRaw) {
        java.util.UUID requestId = parseUuid(requestIdRaw);
        if (requestId == null) {
            return "message.cyberneticenhancements.archive.banking.request_missing";
        }
        PlayerEurodollarManager.TransferResult result = PlayerEurodollarManager.approveRequest(source, requestId);
        refreshAllBanking(source);
        return bankingFailureKey(result);
    }

    private static String handleDeclineAction(ServerPlayer source, String requestIdRaw) {
        java.util.UUID requestId = parseUuid(requestIdRaw);
        if (requestId == null) {
            return "message.cyberneticenhancements.archive.banking.request_missing";
        }
        PlayerEurodollarManager.TransferResult result = PlayerEurodollarManager.declineRequest(source, requestId);
        refreshAllBanking(source);
        return bankingFailureKey(result);
    }

    private static void refreshAllBanking(ServerPlayer source) {
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, ArchiveBankingPayload.capture(player));
        }
    }

    private static ServerPlayer findOnlinePlayer(ServerPlayer source, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(rawName.trim())) {
                return player;
            }
        }
        return null;
    }

    private static java.util.UUID parseUuid(String raw) {
        try {
            return java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String bankingFailureKey(PlayerEurodollarManager.TransferResult result) {
        if (result == null || result == PlayerEurodollarManager.TransferResult.OK) {
            return null;
        }
        return switch (result) {
            case INVALID_AMOUNT -> "message.cyberneticenhancements.archive.banking.invalid_amount";
            case SELF_TARGET -> "message.cyberneticenhancements.archive.banking.self_target";
            case INSUFFICIENT_FUNDS -> "message.cyberneticenhancements.archive.banking.insufficient_funds";
            case TARGET_NOT_FOUND, TARGET_OFFLINE -> "message.cyberneticenhancements.archive.banking.target_missing";
            case REQUEST_NOT_FOUND, REQUEST_NOT_INCOMING ->
                    "message.cyberneticenhancements.archive.banking.request_missing";
            case OK -> null;
        };
    }
}
