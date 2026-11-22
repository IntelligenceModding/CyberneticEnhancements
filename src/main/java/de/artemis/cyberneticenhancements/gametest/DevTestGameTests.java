package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DevTestGameTests {
    @GameTest(template = "sandevistan_test", batch = "dev_contacts_access", timeoutTicks = 220)
    public static void onlyOperatorsCanInteractWithAndCallDeveloperContacts(GameTestHelper h) {
        var operator = player(h);
        var regular = player(h, false);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(operator.position());
        npc.setHome(npc.blockPosition());
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(operator);
        npc.markMet(regular);
        h.assertTrue(!npc.isNoAi(), "Developer NPC AI is disabled");
        var saved = new net.minecraft.nbt.CompoundTag();
        npc.saveWithoutId(saved);
        saved.putBoolean("NoAI", true);
        var restored = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        restored.load(saved);
        h.assertTrue(!restored.isNoAi(), "Legacy developer NPC did not regain AI");
        restored.discard();
        h.assertTrue(AbstractCityNpcEntity.archiveContacts(regular).isEmpty(), "Non-operator gained a developer contact");
        h.assertTrue(npc.mobInteract(regular, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.FAIL
                && regular.containerMenu == regular.inventoryMenu, "Non-operator opened developer dialogue");
        var forgedMenu = new FixerDialogMenu(9, regular.getInventory(), npc);
        h.assertTrue(!forgedMenu.stillValid(regular), "Developer menu accepts non-operators");
        h.assertTrue(PlayerQuestManager.snapshotForIssuer(regular, npc).offers().isEmpty()
                && PlayerQuestManager.acceptIssuerOffer(regular, npc, 0) == PlayerQuestManager.Result.INVALID,
                "Non-operator can access developer contracts");
        h.assertTrue(!npc.canTakeCall(regular) && npc.canTakeCall(operator), "Developer call permissions are incorrect");
        var callId = UUID.randomUUID();
        de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(regular,
                new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), -1));
        h.assertTrue(!de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.hasCall(regular.getUUID()), "Non-operator started a developer call");
        de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(operator,
                new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(callId, npc.getUUID(), -1));
        h.assertTrue(de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.hasCall(operator.getUUID()), "Operator could not start a developer call");
        h.startSequence().thenWaitUntil(() -> h.assertTrue(npc.isOnCall(), "Developer contact has not answered"))
                .thenExecute(() -> {
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(operator,
                            new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(callId, npc.getUUID(), 0));
                    npc.discard();
                    var contact = de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(operator).contacts().stream()
                            .filter(entry -> entry.npcId().equals(npc.getStringUUID())).findFirst().orElseThrow();
                    h.assertTrue(contact.nameColor().equals("light_purple"), "Unloaded developer contact lost its purple color");
                    operator.server.getPlayerList().remove(operator);
                    regular.server.getPlayerList().remove(regular);
                    h.succeed();
                });
    }

    @GameTest(template = "sandevistan_test", batch = "dev_repeat_quests", timeoutTicks = 1800)
    public static void developerContactsReplaceUsedQuestOffersWithoutWaitingForANewDay(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        var previousId = new java.util.concurrent.atomic.AtomicReference<String>();
        awaitBoard(h, player, npc, () -> {
            var offers = PlayerQuestManager.snapshotForIssuer(player, npc).offers();
            int index = java.util.stream.IntStream.range(0, offers.size()).filter(i -> offers.get(i).type() == QuestType.RECOVERY).findFirst().orElseThrow();
            previousId.set(offers.get(index).id());
            h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, npc, index) == PlayerQuestManager.Result.OK, "First recovery quest not accepted");
            h.assertTrue(PlayerQuestManager.abandonIssuerActive(player, npc) == PlayerQuestManager.Result.OK, "First quest not abandoned");
            awaitBoard(h, player, npc, () -> {
                try {
                    var refreshed = PlayerQuestManager.snapshotForIssuer(player, npc).offers();
                    int next = java.util.stream.IntStream.range(0, refreshed.size()).filter(i -> refreshed.get(i).type() == QuestType.RECOVERY).findFirst().orElseThrow();
                    h.assertTrue(!refreshed.get(next).id().equals(previousId.get()), "Used developer offer was not replaced");
                    h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, npc, next) == PlayerQuestManager.Result.OK, "Repeated recovery quest not accepted");
                } finally {
                    PlayerQuestManager.clearAllQuestData(player);
                    npc.discard();
                    player.server.getPlayerList().remove(player);
                }
                h.succeed();
            });
        });
    }

    @GameTest(template = "sandevistan_test", batch = "deceased_contacts", timeoutTicks = 100)
    public static void deceasedContactsAreHiddenOnceAfterArchiveAcknowledgement(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(player.position());
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        try {
            var id = npc.getUUID();
            AbstractCityNpcEntity.togglePinnedFromArchive(player, id);
            h.assertTrue(!AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(player, id), "Living contact was auto-hidden");
            var identities = de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(h.getLevel());
            identities.markDeceased(id);
            var before = AbstractCityNpcEntity.archiveContacts(player).stream().filter(contact -> contact.npcId().equals(id)).findFirst().orElseThrow();
            h.assertTrue(before.deceased() && !before.hidden() && before.pinned(), "Deceased contact disappeared before being viewed");
            var unknown = UUID.randomUUID();
            identities.markDeceased(unknown);
            h.assertTrue(!AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(player, unknown), "Unknown contact could be acknowledged");
            h.assertTrue(AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(player, id), "Viewed deceased contact was not acknowledged");
            var hidden = AbstractCityNpcEntity.archiveContacts(player).stream().filter(contact -> contact.npcId().equals(id)).findFirst().orElseThrow();
            h.assertTrue(hidden.hidden() && !hidden.pinned(), "Auto-hide did not match manual hiding");
            h.assertTrue(!AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(player, id), "Repeated acknowledgement changed state");
            AbstractCityNpcEntity.toggleHiddenFromArchive(player, id);
            h.assertTrue(!AbstractCityNpcEntity.acknowledgeDeceasedFromArchive(player, id), "Manual unhide was overridden on a subsequent visit");
            h.assertTrue(AbstractCityNpcEntity.archiveContacts(player).stream().filter(contact -> contact.npcId().equals(id)).noneMatch(contact -> contact.hidden()),
                    "Manually restored deceased contact was hidden again");
        } finally {
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "dev_quest_command", timeoutTicks = 2600)
    public static void questAcceptCommandStartsEveryQuestWithSeparateIssuers(GameTestHelper h) throws Exception {
        var player = player(h);
        var origin = player.position();
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        de.artemis.cyberneticenhancements.common.command.QuestCommand.register(dispatcher);
        de.artemis.cyberneticenhancements.common.command.QuestAcceptCommand.register(dispatcher);
        var source = player.createCommandSourceStack().withPermission(2);
        var node = dispatcher.getRoot().getChild("cyberneticenhancements").getChild("quests").getChild("accept");
        h.assertTrue(!node.canUse(source.withPermission(0)), "Quest acceptance development command allows non-operators");
        for (var type : QuestType.values()) h.assertTrue(node.getChild(type.id()) != null, "Missing quest option " + type.id());
        h.assertTrue(node.getChild("chromium_survey") != null, "Missing chromium survey command");
        h.assertTrue(dispatcher.execute("cyberneticenhancements quests accept all", source) == PlayerQuestManager.DEV_OFFER_COUNT,
                "All option did not spawn one issuer per quest variant");
        var issuers = h.getLevel().getEntitiesOfClass(de.artemis.cyberneticenhancements.common.entity.DevTestEntity.class,
                player.getBoundingBox().inflate(0.1), npc -> npc.position().equals(origin));
        h.assertTrue(issuers.size() == PlayerQuestManager.DEV_OFFER_COUNT, "Quest issuers did not spawn exactly where the player stood");
        // Moving away while terrain is prepared must leave the issuers at the command's original position.
        player.setPos(origin.add(8, 0, 0));
        h.startSequence().thenWaitUntil(() -> h.assertTrue(PlayerQuestManager.activeContracts(player).size() == PlayerQuestManager.DEV_OFFER_COUNT,
                "Development contracts are still preparing")).thenExecute(() -> {
            try {
                var active = PlayerQuestManager.activeContracts(player);
                h.assertTrue(active.stream().map(q -> q.type()).distinct().count() == QuestType.values().length, "Missing accepted quest types");
                h.assertTrue(active.stream().filter(q -> q.chromiumSurvey()).count() == 1, "Survey variant was not accepted exactly once");
                h.assertTrue(active.stream().map(q -> q.issuerId()).distinct().count() == active.size(), "All option broke per-contact capacity");
                h.assertTrue(issuers.stream().noneMatch(npc -> npc.isNoAi()), "Command issuers cannot walk");
                h.assertTrue(AbstractCityNpcEntity.archiveContacts(player).size() == issuers.size(), "Command issuers were not added to contacts");
                h.assertTrue(player.getInventory().countItem(de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get()) == 1,
                        "Command acceptance did not issue the delivery cargo");
                var issuer = issuers.getFirst();
                h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, issuer, 0) != PlayerQuestManager.Result.OK,
                        "Command contracts left a spare offer to accept");
            } finally {
                PlayerQuestManager.clearAllQuestData(player);
                issuers.forEach(npc -> npc.discard());
                player.server.getPlayerList().remove(player);
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "dev_quest_single_command", timeoutTicks = 100)
    public static void questAcceptCommandCanStartOneQuest(GameTestHelper h) throws Exception {
        var player = player(h);
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        de.artemis.cyberneticenhancements.common.command.QuestAcceptCommand.register(dispatcher);
        h.assertTrue(dispatcher.execute("cyberneticenhancements quests accept recovery", player.createCommandSourceStack().withPermission(2)) == 1,
                "Individual option did not spawn exactly one NPC");
        h.startSequence().thenWaitUntil(() -> h.assertTrue(PlayerQuestManager.activeContracts(player).size() == 1, "Recovery contract not accepted"))
                .thenExecute(() -> {
                    var quest = PlayerQuestManager.activeContracts(player).getFirst();
                    h.assertTrue(quest.type() == QuestType.RECOVERY && !quest.chromiumSurvey(), "Individual option accepted a different quest");
                    var npc = h.getLevel().getEntity(quest.issuerId());
                    PlayerQuestManager.clearAllQuestData(player);
                    if (npc != null) npc.discard();
                    player.server.getPlayerList().remove(player);
                    h.succeed();
                });
    }

    @GameTest(template = "sandevistan_test", batch = "npc_extended_stock", timeoutTicks = 200)
    public static void largeShopsPurchaseBeyondFourEntriesAndRejectInvalidIndices(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        var regular = ModEntityTypes.FIXER.get().create(h.getLevel());
        regular.setPos(player.position());
        h.getLevel().addFreshEntity(regular);
        try {
            var menu = new FixerDialogMenu(1, player.getInventory(), npc);
            player.containerMenu = menu;
            int count = menu.shopOffers().size();
            h.assertTrue(count > 4, "Developer shop did not contain extended stock");
            for (int index : new int[]{4, count / 2, count - 1}) {
                player.getInventory().clearContent();
                var expected = menu.shopOffers().get(index).stack();
                menu.handleChoice(player, -index - 1);
                h.assertTrue(player.getInventory().countItem(expected.getItem()) == expected.getCount(), "Wrong item purchased from extended slot " + index);
                h.assertTrue(player.containerMenu == menu, "Extended purchase closed dialogue");
            }
            player.getInventory().clearContent();
            menu.handleChoice(player, -count - 1);
            menu.handleChoice(player, Integer.MIN_VALUE);
            h.assertTrue(player.getInventory().isEmpty(), "Invalid index purchased an item");
            var regularMenu = new FixerDialogMenu(2, player.getInventory(), regular);
            player.containerMenu = regularMenu;
            PlayerEurodollarManager.setBalance(player, 100);
            regularMenu.handleChoice(player, -3);
            h.assertTrue(PlayerEurodollarManager.balance(player) == 100 && player.getInventory().isEmpty(), "Generic purchase bypassed trust");
            regularMenu.handleChoice(player, -1);
            var first = regularMenu.shopOffers().getFirst().stack();
            h.assertTrue(PlayerEurodollarManager.balance(player) == 55 && player.getInventory().countItem(first.getItem()) == first.getCount(),
                    "Generic purchase used the wrong price or stock");
            int balance = PlayerEurodollarManager.balance(player);
            regularMenu.handleChoice(player, -count);
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance, "Index from another catalog was charged");
        } finally {
            player.closeContainer();
            npc.discard();
            regular.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_consumable_interaction", timeoutTicks = 200)
    public static void openingDialogueStopsConsumptionWithoutUsingTheHeldItem(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        try {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                var item = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.item.Items.APPLE
                        : de.artemis.cyberneticenhancements.common.registry.ModItems.consumable("maxdoc_mk2").get();
                player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(item, 3));
                player.startUsingItem(hand);
                h.assertTrue(player.isUsingItem(), "Test item use did not start: " + hand);
                var result = npc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                h.assertTrue(result.consumesAction() && player.containerMenu instanceof FixerDialogMenu, "NPC interaction did not open dialogue");
                h.assertTrue(!player.isUsingItem(), "Item use continued behind the NPC GUI: " + hand);
                h.assertTrue(player.getItemInHand(hand).is(item) && player.getItemInHand(hand).getCount() == 3, "Opening dialogue consumed the item");
                player.closeContainer();
            }
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_workshop_services", timeoutTicks = 200)
    public static void workshopServicesChargeRealPricesAndEnforceTrust(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        try {
            PlayerEurodollarManager.setBalance(player, 1000);
            var menu = new FixerDialogMenu(1, player.getInventory(), npc);
            player.containerMenu = menu;
            menu.handleChoice(player, FixerDialogMenu.OPTION_PURGE);
            h.assertTrue(PlayerEurodollarManager.balance(player) == 1000 && !player.hasEffect(MobEffects.FIRE_RESISTANCE), "Locked service charged or applied effects");
            menu.handleChoice(player, FixerDialogMenu.OPTION_STABILIZE);
            h.assertTrue(PlayerEurodollarManager.balance(player) == 920, "Mining service used the old fallback price");
            var haste = player.getEffect(MobEffects.DIG_SPEED);
            h.assertTrue(haste != null && haste.getAmplifier() == 1 && haste.getDuration() == 6000, "Mining calibration effect or duration was wrong");
            npc.raiseTrust(player, 300);
            menu.handleChoice(player, FixerDialogMenu.OPTION_PURGE);
            h.assertTrue(PlayerEurodollarManager.balance(player) == 830, "Advanced service did not apply the trusted price");
            h.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Hazard preparation failed");
            player.removeAllEffects();
            PlayerEurodollarManager.setBalance(player, 0);
            menu.handleChoice(player, FixerDialogMenu.OPTION_STABILIZE);
            h.assertTrue(!player.hasEffect(MobEffects.DIG_SPEED) && PlayerEurodollarManager.balance(player) == 0, "Unpaid service applied effects");
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_clinic_services", timeoutTicks = 200)
    public static void clinicDetoxCleansHarmfulEffectsAndHonorsDailyLimit(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.RIPPERDOC.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        try {
            npc.raiseTrust(player, 300);
            PlayerEurodollarManager.setBalance(player, 1000);
            player.setHealth(4);
            var menu = new FixerDialogMenu(1, player.getInventory(), npc);
            player.containerMenu = menu;
            h.assertTrue(npc.lastPurgeDay(player) == -1, "Unused daily treatment was marked used on the first day");
            menu.handleChoice(player, FixerDialogMenu.OPTION_STABILIZE);
            h.assertTrue(player.getHealth() == 12 && CyberstrainManager.getSuppressionAmount(player) == 24
                    && CyberstrainManager.getSuppressionSecondsRemaining(player) == 180, "Clinic care did not match its description");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400));
            CyberstrainManager.applyRamJolt(player, 1200, 0.8D);
            menu.handleChoice(player, FixerDialogMenu.OPTION_PURGE);
            h.assertTrue(!player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.BLINDNESS)
                    && player.hasEffect(MobEffects.MOVEMENT_SPEED), "Detox removed beneficial effects or left harmful effects");
            h.assertTrue(CyberstrainManager.getSuppressionAmount(player) == 0 && CyberstrainManager.getRamJoltSecondsRemaining(player) == 0
                    && player.getHealth() == 16, "Detox did not clear doses and heal");
            int balance = PlayerEurodollarManager.balance(player);
            h.assertTrue(balance == 770, "Clinic unexpectedly applied a trust discount or wrong service price");
            menu.handleChoice(player, FixerDialogMenu.OPTION_PURGE);
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance && player.getHealth() == 16, "Daily treatment was sold twice");
            npc.setLastPurgeDay(player, player.level().getDayTime() / 24000L - 1);
            menu.handleChoice(player, FixerDialogMenu.OPTION_PURGE);
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance - 160 && player.getHealth() == 20, "Daily treatment did not become available again");
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_purchase_sync", timeoutTicks = 200)
    public static void purchasesSyncMainInventoryWhileDialogueRemainsOpen(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "shop-sync-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation()) {
            @Override
            public boolean hasPermissions(int permissionLevel) { return permissionLevel <= 2; }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        try {
            player.getInventory().clearContent();
            for (int slot = 0; slot < 9; slot++) {
                player.getInventory().setItem(slot, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
            }
            var menu = new FixerDialogMenu(1, player.getInventory(), npc);
            player.containerMenu = menu;
            var item = FixerDialogMenu.shopItem(npc.npcType(), FixerDialogMenu.OPTION_BUY_MAXDOC_MK2).getItem();
            for (int purchase = 1; purchase <= 2; purchase++) {
                channel.runPendingTasks();
                while (channel.readOutbound() != null) {}
                menu.handleChoice(player, FixerDialogMenu.OPTION_BUY_MAXDOC_MK2);
                channel.runPendingTasks();
                int expected = purchase * 2;
                boolean synced = false;
                Object packet;
                while ((packet = channel.readOutbound()) != null) {
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket content
                            && content.getContainerId() == 0 && content.getItems().stream().anyMatch(stack -> stack.is(item) && stack.getCount() == expected)) synced = true;
                }
                h.assertTrue(synced, "Purchase did not immediately sync the main inventory: " + purchase);
                h.assertTrue(player.getInventory().getItem(9).is(item) && player.getInventory().getItem(9).getCount() == expected,
                        "Purchase did not fill or merge into the main inventory");
                h.assertTrue(player.containerMenu == menu, "Purchase closed the dialogue");
            }
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "dev_quest_dimensions", timeoutTicks = 8000)
    public static void developerQuestChoicesAcceptTheSelectedTypeAndDimension(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        var sequence = h.startSequence();
        var routes = de.artemis.cyberneticenhancements.common.quest.QuestDimension.values();
        var types = new QuestType[]{QuestType.RECOVERY, QuestType.INVESTIGATION, QuestType.CYBERPSYCHO};
        for (int index = 0; index < routes.length; index++) {
            var route = routes[index];
            var type = types[index];
            sequence.thenExecute(() -> {
                var menu = new FixerDialogMenu(1, player.getInventory(), npc);
                player.containerMenu = menu;
                menu.handleChoice(player, FixerDialogMenu.devQuestOption(route, type.ordinal()));
            }).thenWaitUntil(() -> {
                var contracts = PlayerQuestManager.activeContracts(player);
                h.assertTrue(contracts.size() == 1, "Waiting for " + route + " " + type);
                var contract = contracts.getFirst();
                h.assertTrue(contract.type() == type
                        && de.artemis.cyberneticenhancements.common.quest.QuestDimension.of(contract.dimensionId()) == route,
                        "Developer menu accepted a different quest than the requested type and dimension");
            }).thenExecute(() -> {
                var contract = PlayerQuestManager.activeContracts(player).getFirst();
                if (route != de.artemis.cyberneticenhancements.common.quest.QuestDimension.STANDARD) {
                    var targets = de.artemis.cyberneticenhancements.common.quest.QuestNavigation.capture(player).targets();
                    h.assertTrue(targets.stream().anyMatch(target -> target.questId().equals(contract.id())
                            && target.dimension().equals(contract.dimensionId()) && target.pos().equals(contract.targetPos())),
                            "Developer quest did not publish its dimensional navigation target");
                }
                if (type == QuestType.INVESTIGATION) h.assertTrue(contract.investigation() != null, "Nether survey did not initialize");
                h.assertTrue(PlayerQuestManager.abandonIssuerActive(player, npc) == PlayerQuestManager.Result.OK, "Could not clear test quest");
            });
        }
        sequence.thenExecute(() -> {
            npc.discard();
            player.server.getPlayerList().remove(player);
        }).thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "dev_test", timeoutTicks = 1600)
    public static void developerNpcHasAllQuestTypesAndBecomesAPurpleArchiveContact(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        awaitBoard(h, player, npc, () -> {
        try {
            h.assertTrue(npc.trustPoints(player) == 300 && npc.trustLevel(player) == 4, "Test NPC did not start at maximum trust");
            npc.ensureIdentity();
            h.assertTrue(npc.getName().getStyle().getColor().getValue() == net.minecraft.ChatFormatting.LIGHT_PURPLE.getColor()
                    && npc.nameColorId(player).equals("light_purple"), "Dev NPC did not retain the spawn egg's purple name");
            var saved = new net.minecraft.nbt.CompoundTag();
            npc.saveWithoutId(saved);
            var restored = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
            restored.load(saved);
            h.assertTrue(restored.getName().getStyle().getColor().getValue() == net.minecraft.ChatFormatting.LIGHT_PURPLE.getColor()
                    && restored.nameColorId().equals("light_purple"), "Dev NPC lost its purple name after reloading");
            restored.discard();
            npc.lowerTrust(player, 300);
            npc.applyDialoguePenalty(player, 300, 100, true);
            h.assertTrue(npc.hasMaxTrust(player) && !npc.isRefusing(player), "Test NPC became locked");
            npc.markMet(player);
            h.assertTrue(AbstractCityNpcEntity.archiveContacts(player).stream().anyMatch(contact -> contact.npcId().equals(npc.getUUID())), "Test NPC did not appear in archive contacts");
            h.assertTrue(de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player).contacts().stream()
                    .anyMatch(contact -> contact.npcId().equals(npc.getStringUUID()) && contact.nameColor().equals("light_purple")), "Dev contact was not purple");
            var board = PlayerQuestManager.snapshotForIssuer(player, npc);
            var types = java.util.EnumSet.noneOf(QuestType.class);
            board.offers().forEach(offer -> types.add(offer.type()));
            h.assertTrue(types.size() == QuestType.values().length && board.offers().size() == PlayerQuestManager.DEV_OFFER_COUNT, "Test board missing quest types: " + types);
            h.assertTrue(board.offers().stream().anyMatch(q -> q.chromiumSurvey()) && board.offers().stream().anyMatch(q -> q.type() == QuestType.INVESTIGATION && !q.chromiumSurvey()), "Developer board did not include both investigation variants");
            var sighting = board.offers().stream().filter(offer -> offer.type() == QuestType.CYBERPSYCHO).findFirst().orElseThrow();
            h.assertTrue(board.offers().stream().filter(offer -> offer.type() != QuestType.CYBERPSYCHO).allMatch(offer -> offer.rewardMoney() < sighting.rewardMoney()),
                    "Cyberpsycho sighting was not the most lucrative contract");
            for (int i = 0; i < board.offers().size(); i++) {
                for (int j = i + 1; j < board.offers().size(); j++) {
                    var first = board.offers().get(i).targetPos();
                    var second = board.offers().get(j).targetPos();
                    if (first == null || second == null || board.offers().get(i).chromiumSurvey() || board.offers().get(j).chromiumSurvey()) continue;
                    long dx = (long) first.getX() - second.getX();
                    long dz = (long) first.getZ() - second.getZ();
                    h.assertTrue(dx * dx + dz * dz >= 64 * 64, "Quest offers overlapped across types");
                }
            }
            var menu = new FixerDialogMenu(1, player.getInventory(), npc);
            h.assertTrue(menu.offerSlots() == PlayerQuestManager.DEV_OFFER_COUNT, "Developer menu did not expose its actions");
            player.containerMenu = menu;
            menu.handleChoice(player, menu.acceptOfferOption(board.offers().indexOf(sighting)));
            h.assertTrue(player.containerMenu == player.inventoryMenu, "Accepting a quest did not close the NPC menu");
            var accepted = PlayerQuestManager.snapshotForIssuer(player, npc).active();
            h.assertTrue(accepted != null && accepted.type() == QuestType.CYBERPSYCHO, "Cyberpsycho sighting could not be accepted through the menu");
            var busyMenu = new FixerDialogMenu(2, player.getInventory(), npc);
            player.containerMenu = busyMenu;
            busyMenu.handleChoice(player, busyMenu.acceptOfferOption(0));
            h.assertTrue(player.containerMenu == busyMenu, "Rejected quest acceptance closed the NPC menu");
            player.closeContainer();
            h.assertTrue(AbstractCityNpcEntity.resolveCallContact(player, npc.getUUID()) == npc, "Dev contact cannot be called");
        } finally {
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "courier_handoff", timeoutTicks = 1600)
    public static void deliveryRecognizesDepositedPackageAndAllowsPayout(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        awaitBoard(h, player, npc, () -> {
        try {
            var board = PlayerQuestManager.snapshotForIssuer(player, npc);
            int slot = -1;
            for (int i = 0; i < board.offers().size(); i++) if (board.offers().get(i).type() == QuestType.DELIVERY) slot = i;
            h.assertTrue(slot >= 0 && PlayerQuestManager.acceptIssuerOffer(player, npc, slot) == PlayerQuestManager.Result.OK, "Delivery could not be accepted");
            var active = PlayerQuestManager.snapshotForIssuer(player, npc).active();
            var target = active.targetPos();
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(target.above()));
            var chest = (net.minecraft.world.Container) h.getLevel().getBlockEntity(target);
            h.assertTrue(chest != null, "Delivery chest was not created");
            chest.setItem(0, new net.minecraft.world.item.ItemStack(de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get()));
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            h.assertTrue(!PlayerQuestManager.snapshotForIssuer(player, npc).active().isReady(), "An unrelated package completed delivery");
            net.minecraft.world.item.ItemStack cargo = net.minecraft.world.item.ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.is(de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get())) {
                    cargo = stack.copy();
                    player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                    break;
                }
            }
            h.assertTrue(!cargo.isEmpty(), "Provided cargo disappeared before deposit");
            chest.setItem(1, cargo);
            chest.setChanged();
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            var ready = PlayerQuestManager.snapshotForIssuer(player, npc).active();
            h.assertTrue(ready.isReady() && ready.destinationReached() && ready.progress() == ready.targetCount(), "Deposited cargo did not finish the delivery objective");
            h.assertTrue(!h.getLevel().getBlockState(target).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.IRON_QUEST_CRATE.get()), "Delivered stash remained active");
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.OK, "Delivered contract could not be paid out");
        } finally {
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
        });
    }
    @GameTest(template = "sandevistan_test", batch = "quest_cursor", timeoutTicks = 1600)
    public static void questItemsOnCursorAreNotReplacedAndAreCleanedUpOnAbandon(GameTestHelper h) {
        var player = player(h);
        var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel());
        npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        awaitBoard(h, player, npc, () -> {
        try {
            var board = PlayerQuestManager.snapshotForIssuer(player, npc);
            int offer = -1;
            for (int i = 0; i < board.offers().size(); i++) if (board.offers().get(i).type() == QuestType.DELIVERY) offer = i;
            h.assertTrue(offer >= 0 && PlayerQuestManager.acceptIssuerOffer(player, npc, offer) == PlayerQuestManager.Result.OK, "Delivery could not be accepted");
            for (int context = 0; context < 2; context++) {
                if (context == 1) player.containerMenu = net.minecraft.world.inventory.ChestMenu.threeRows(3, player.getInventory(), new net.minecraft.world.SimpleContainer(27));
                int checked = 0;
                for (int slot = 0; slot < 36; slot++) {
                    var original = player.getInventory().getItem(slot).copy();
                    if (!original.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) continue;
                    player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
                    player.containerMenu.setCarried(original.copy());
                    for (int tick = 0; tick < 100; tick++) {
                        player.tickCount++;
                        PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                    }
                    h.assertTrue(countMatching(player, original) == 0, "Quest item was replaced while on the cursor");
                    h.assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(original, player.containerMenu.getCarried()), "Cursor stack was changed");
                    player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                    player.getInventory().setItem(slot, original);
                    checked++;
                }
                h.assertTrue(checked == 1 && player.getInventory().countItem(net.minecraft.world.item.Items.COMPASS) == 0,
                        "Fixture did not check the cargo or a retired quest compass was issued");
            }
            var cargoItem = de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get();
            for (int slot = 0; slot < 36; slot++) {
                var stack = player.getInventory().getItem(slot);
                if (!stack.is(cargoItem)) continue;
                var lost = stack.copy();
                player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
                for (int tick = 0; tick <= 220; tick++) {
                    player.tickCount++;
                    PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                    h.assertTrue(countMatching(player, lost) == 0, "Missing cargo was automatically replaced");
                }
                h.assertTrue(player.getInventory().countItem(cargoItem) == 0, "Missing cargo was restored for free");
                h.assertTrue(PlayerQuestManager.requestDeliveryReplacement(player, npc), "Explicit replacement request failed");
                h.assertTrue(player.getInventory().countItem(cargoItem) == 1 && countMatching(player, lost) == 0, "Explicit replacement did not invalidate the old cargo");
                break;
            }
            for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(cargoItem)) {
                player.containerMenu.setCarried(player.getInventory().getItem(slot).copy());
                player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
                break;
            }
            h.assertTrue(PlayerQuestManager.abandonIssuerActive(player, npc) == PlayerQuestManager.Result.OK, "Quest could not be abandoned");
            h.assertTrue(player.containerMenu.getCarried().isEmpty(), "Abandoning a quest left its cursor item behind");
        } finally {
            player.closeContainer();
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
        });
    }

    private static void awaitBoard(GameTestHelper h, ServerPlayer player, AbstractCityNpcEntity npc, Runnable work) {
        int count = npc instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity ? PlayerQuestManager.DEV_OFFER_COUNT : 3;
        h.startSequence().thenWaitUntil(() -> h.assertTrue(PlayerQuestManager.snapshotForIssuer(player, npc).offers().size() == count,
                "Contract terrain is still preparing")).thenExecute(work);
    }

    @GameTest(template = "sandevistan_test", batch = "normal_board_preparation", timeoutTicks = 1600)
    public static void ordinaryBoardsStillPrepareThreeContracts(GameTestHelper h) {
        var player = player(h); var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel()); npc.setPos(player.position());
        awaitBoard(h, player, npc, () -> {
            try { h.assertTrue(PlayerQuestManager.snapshotForIssuer(player, npc).offers().size() == 3, "Normal board size changed"); }
            finally { npc.discard(); player.server.getPlayerList().remove(player); }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "menu_nonblocking", timeoutTicks = 1600)
    public static void openingMenuDoesNotSynchronouslyLoadRemoteChunksAndPreparedOffersKeepTheirIds(GameTestHelper h) {
        var player = player(h); var npc = ModEntityTypes.DEV_TEST.get().create(h.getLevel()); npc.setPos(player.position());
        h.getLevel().addFreshEntity(npc);
        int loaded = h.getLevel().getChunkSource().getLoadedChunksCount();
        var initial = PlayerQuestManager.snapshotForIssuer(player, npc);
        var menu = new FixerDialogMenu(1, player.getInventory(), npc); menu.broadcastChanges();
        h.assertTrue(h.getLevel().getChunkSource().getLoadedChunksCount() == loaded, "Opening the menu synchronously loaded remote terrain");
        var ids = initial.offers().stream().map(offer -> offer.id()).toList();
        awaitBoard(h, player, npc, () -> {
            try {
                var finished = PlayerQuestManager.snapshotForIssuer(player, npc);
                h.assertTrue(finished.offers().stream().map(offer -> offer.id()).toList().containsAll(ids), "Background preparation replaced already displayed offers");
            } finally { npc.discard(); player.server.getPlayerList().remove(player); }
            h.succeed();
        });
    }
    private static int countMatching(ServerPlayer player, net.minecraft.world.item.ItemStack original) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(original, stack)) count += stack.getCount();
        }
        return count;
    }
    private static ServerPlayer player(GameTestHelper h) {
        return player(h, true);
    }

    private static ServerPlayer player(GameTestHelper h, boolean operator) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "cache-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation()) {
            @Override
            public boolean hasPermissions(int permissionLevel) {
                return permissionLevel <= (operator ? 2 : 0);
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }


}
