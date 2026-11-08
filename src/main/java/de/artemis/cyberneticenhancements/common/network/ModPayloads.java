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
        event.registrar("1")
                .playToClient(CyberpsychosisControlPayload.TYPE, CyberpsychosisControlPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> CyberpsychosisClientState.apply(
                                payload.locked(),
                                payload.yaw(),
                                payload.pitch(),
                                payload.forward(),
                                payload.strafe(),
                                payload.jump(),
                                payload.sprint())))
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
                .playToServer(UpgradeStationInputSlotPayload.TYPE, UpgradeStationInputSlotPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (payload.slotIndex() < 0) {
                                return;
                            }
                            if (context.player().containerMenu instanceof TechStationMenu techStationMenu) {
                                switch (payload.slotIndex()) {
                                    case 0 -> techStationMenu.tryUpgradeRepairSupportedTier();
                                    case 1 -> techStationMenu.tryUpgradeUpgradeSupportedTier();
                                    default -> {
                                    }
                                }
                                return;
                            }
                            if (context.player().containerMenu instanceof RecyclerStationMenu recyclerStationMenu && payload.slotIndex() == 0) {
                                recyclerStationMenu.tryUpgradeSupportedTier();
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
                                case ArchiveBankingActionPayload.ACTION_SEND -> handleSendAction(serverPlayer, payload.targetName(), payload.amount());
                                case ArchiveBankingActionPayload.ACTION_REQUEST -> handleRequestAction(serverPlayer, payload.targetName(), payload.amount());
                                case ArchiveBankingActionPayload.ACTION_APPROVE -> handleApproveAction(serverPlayer, payload.requestId());
                                case ArchiveBankingActionPayload.ACTION_DECLINE -> handleDeclineAction(serverPlayer, payload.requestId());
                                default -> "message.cyberneticenhancements.archive.banking.action_unavailable";
                            };
                            if (failureKey != null) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(failureKey), true);
                            }
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveBankingPayload.capture(serverPlayer));
                        }))
                .playToServer(ArchiveQuestActionPayload.TYPE, ArchiveQuestActionPayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                                return;
                            }
                            boolean success = switch (payload.actionId()) {
                                case ArchiveQuestActionPayload.ACTION_DISCARD -> PlayerQuestManager.abandonQuestById(serverPlayer, payload.questId()) == PlayerQuestManager.Result.OK;
                                default -> false;
                            };
                            if (!success) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.cyberneticenhancements.archive.quest_discard_unavailable"), true);
                                return;
                            }
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveQuestsPayload.capture(serverPlayer));
                            PacketDistributor.sendToPlayer(serverPlayer, ArchiveContactsPayload.capture(serverPlayer));
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

                            boolean success = switch (payload.actionId()) {
                                case ArchiveContactActionPayload.ACTION_LOCATE -> AbstractCityNpcEntity.locateFromArchive(serverPlayer, npcId);
                                case ArchiveContactActionPayload.ACTION_MEETUP -> AbstractCityNpcEntity.summonFromArchive(serverPlayer, npcId);
                                case ArchiveContactActionPayload.ACTION_TOGGLE_PIN -> AbstractCityNpcEntity.togglePinnedFromArchive(serverPlayer, npcId);
                                case ArchiveContactActionPayload.ACTION_TOGGLE_HIDE -> AbstractCityNpcEntity.toggleHiddenFromArchive(serverPlayer, npcId);
                                default -> false;
                            };
                            if (!success) {
                                String messageKey = switch (payload.actionId()) {
                                    case ArchiveContactActionPayload.ACTION_MEETUP -> "message.cyberneticenhancements.archive.contact_meetup_unavailable";
                                    case ArchiveContactActionPayload.ACTION_TOGGLE_PIN, ArchiveContactActionPayload.ACTION_TOGGLE_HIDE -> "message.cyberneticenhancements.archive.contact_update_unavailable";
                                    default -> "message.cyberneticenhancements.archive.contact_locate_unavailable";
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
            case REQUEST_NOT_FOUND, REQUEST_NOT_INCOMING -> "message.cyberneticenhancements.archive.banking.request_missing";
            case OK -> null;
        };
    }
}
