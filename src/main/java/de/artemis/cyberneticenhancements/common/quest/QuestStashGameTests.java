package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
import java.util.List;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestStashGameTests {
    @GameTest(template = "sandevistan_test", batch = "dimensional_quest_sites", timeoutTicks = 8000)
    public static void unvisitedDimensionsPrepareDrySupportedExtractionSites(GameTestHelper h) {
        var origin = h.getLevel();
        var server = origin.getServer();
        var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        h.assertTrue(nether != null && end != null && nether.players().isEmpty() && end.players().isEmpty(), "Test requires empty Nether and End dimensions");
        String netherId = UUID.randomUUID().toString(), endId = UUID.randomUUID().toString();
        var sites = new java.util.HashMap<net.minecraft.server.level.ServerLevel, BlockPos>();
        long seed = 41813L;
        h.startSequence().thenWaitUntil(() -> {
            for (var level : List.of(nether, end)) {
                var saved = sites.get(level);
                if (saved != null) {
                    QuestTerrainPreparation.ready(level, saved.getX(), saved.getZ());
                    continue;
                }
                var result = DimensionalQuestSites.find(level, origin, BlockPos.ZERO, level == nether ? netherId : endId, seed, 48);
                h.assertTrue(result.preparing() || result.pos() != null, "No safe site found in " + level.dimension().location());
                if (result.pos() != null) sites.put(level, result.pos());
            }
            h.assertTrue(sites.size() == 2 && sites.entrySet().stream().allMatch(entry -> entry.getKey().hasChunkAt(entry.getValue())),
                    "Waiting for both dimension sites, found " + sites.keySet().stream().map(level -> level.dimension().location().toString()).toList());
        }).thenExecute(() -> {
            for (var level : List.of(nether, end)) {
                var pos = sites.get(level);
                h.assertTrue(DimensionalQuestSites.usable(level, pos), "Generated dimension site was unsafe");
                var floor = level.getBlockState(pos.below());
                level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
                h.assertTrue(!DimensionalQuestSites.usable(level, pos), "Site over air was accepted");
                level.setBlockAndUpdate(pos.below(), floor);
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                h.assertTrue(!DimensionalQuestSites.usable(level, pos), "A lava-filled objective was accepted");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                var contract = new QuestContract(level == nether ? netherId : endId, UUID.randomUUID(), "Test issuer",
                        "fixer", QuestType.EXTRACTION, 1, QuestStatus.ACTIVE, 0, 1, 0, false,
                        level.dimension().location().toString(), pos, 1, 100, 1, 0, 0, 0, 0);
                h.assertTrue(PlayerQuestManager.ensureExtractionDrop(level, contract), "Extraction could not place its crate");
                h.assertTrue(level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate
                        && crate.questId().equals(contract.id()) && !crate.getItem(0).isEmpty(), "Crate was not bound or stocked");
                h.assertTrue(contract.rewardMoney() == (level == nether ? 125 : 135), "Wrong dimensional cash bonus");
                QuestRecovery.cleanupContainer(level, pos, contract.id());
            }
        }).thenSucceed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_obsolete_world_cleanup", timeoutTicks = 100)
    public static void issuingANewCopyDeletesOldGroundClosedStorageAndPortableInventoryCopies(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-world-cleanup");
        var other = feedbackPlayer(h, "quest-other-holder");
        var level = h.getLevel();
        var versions = QuestItemVersions.get(level);
        var original = trackedItem();
        versions.issue(original);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.BARREL.defaultBlockState());
        var barrel = (net.minecraft.world.Container) level.getBlockEntity(pos);
        barrel.setItem(0, original.copy());
        barrel.setItem(1, new ItemStack(Items.DIAMOND, 3));
        var portable = new ItemStack(Items.SHULKER_BOX);
        var slots = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(2, original.copy());
        slots.set(8, new ItemStack(Items.GOLD_INGOT, 4));
        portable.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(slots));
        barrel.setItem(2, portable);
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new net.minecraft.world.item.component.BundleContents(
                List.of(original.copy(), new ItemStack(Items.EMERALD, 2))));
        barrel.setItem(3, bundle);
        owner.player().getEnderChestInventory().setItem(0, original.copy());
        other.player().getInventory().setItem(0, original.copy());
        other.player().getInventory().setItem(40, original.copy());
        var ground = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 4, pos.getY(), pos.getZ(), original.copy());
        level.addFreshEntity(ground);
        var cart = net.minecraft.world.entity.EntityType.CHEST_MINECART.create(level);
        cart.setPos(pos.getX() + 3, pos.getY(), pos.getZ());
        cart.setItem(0, original.copy());
        level.addFreshEntity(cart);
        try {
            var replacement = original.copy();
            versions.issue(replacement);
            h.assertTrue(ground.isRemoved(), "Old ground copy was left until someone tried to pick it up");
            h.assertTrue(barrel.getItem(0).isEmpty() && cart.getItem(0).isEmpty(), "Old copy survived in closed block/entity storage");
            h.assertTrue(owner.player().getEnderChestInventory().getItem(0).isEmpty()
                    && other.player().getInventory().getItem(0).isEmpty() && other.player().getOffhandItem().isEmpty(),
                    "Old copy survived in another player's inventory or an Ender Chest");
            var nested = barrel.getItem(2).get(DataComponents.CONTAINER).stream().toList();
            h.assertTrue(nested.get(2).isEmpty() && nested.get(8).getCount() == 4 && barrel.getItem(2).is(Items.SHULKER_BOX),
                    "Nested old item survived, or shulker slot positions/unrelated contents were damaged");
            var bundled = barrel.getItem(3).get(DataComponents.BUNDLE_CONTENTS).itemCopyStream().toList();
            h.assertTrue(bundled.size() == 1 && bundled.getFirst().is(Items.EMERALD) && bundled.getFirst().getCount() == 2,
                    "Bundle cleanup kept the old item or removed unrelated contents");
            h.assertTrue(barrel.getItem(1).getCount() == 3 && versions.current(replacement), "Cleanup damaged normal loot or the replacement");
            var late = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 4, pos.getY(), pos.getZ(), original.copy());
            level.addFreshEntity(late);
            h.assertTrue(late.isRemoved(), "A stale ground copy loaded after replacement was not removed");
            owner.player().getInventory().setItem(0, replacement);
            versions.retire(QuestItemVersions.key(replacement).split("/")[0]);
            h.assertTrue(owner.player().getInventory().getItem(0).isEmpty(), "Retired quest item survived in inventory");
        } finally {
            cart.discard();
            ground.discard();
            level.removeBlock(pos, false);
            owner.player().server.getPlayerList().remove(owner.player());
            other.player().server.getPlayerList().remove(other.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_obsolete_chunk_load", timeoutTicks = 100)
    public static void storedOldCopiesAreRemovedWhenTheirChunkLoads(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.BARREL.defaultBlockState());
        var barrel = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(pos);
        var original = trackedItem();
        var versions = QuestItemVersions.get(level);
        versions.issue(original);
        barrel.setItem(0, original.copy());
        barrel.setItem(1, new ItemStack(Items.DIAMOND, 2));
        var saved = barrel.saveWithFullMetadata(level.registryAccess());
        versions.issue(original.copy());
        var restored = (net.minecraft.world.level.block.entity.BarrelBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                pos, barrel.getBlockState(), saved, level.registryAccess());
        level.setBlockEntity(restored);
        QuestItemCleanup.loaded(new net.neoforged.neoforge.event.level.ChunkEvent.Load(level.getChunkAt(pos), false));
        h.startSequence().thenWaitUntil(() -> h.assertTrue(restored.getItem(0).isEmpty(), "Old item remained after storage loaded"))
                .thenExecute(() -> {
                    h.assertTrue(restored.getItem(1).getCount() == 2, "Loading cleanup damaged unrelated storage contents");
                    level.removeBlock(pos, false);
                    h.succeed();
                });
    }

    private static ItemStack trackedItem() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("QuestContractId", UUID.randomUUID().toString());
        tag.putString("QuestItemType", "exfil_shard");
        var stack = new ItemStack(Items.AMETHYST_SHARD);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return stack;
    }

    @GameTest(template = "sandevistan_test", batch = "iron_quest_crate", timeoutTicks = 80)
    public static void metalQuestContainersPreserveOldStashesOpenNormallyAndSaveTheirContents(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-crate");
        var player = owner.player();
        var level = h.getLevel();
        try {
            h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
            h.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST);
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            var legacy = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos);
            legacy.applyComponents(net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Old stash")).build(), net.minecraft.core.component.DataComponentPatch.EMPTY);
            legacy.setItem(0, new ItemStack(Items.DIAMOND, 3));
            h.assertTrue(PlayerQuestManager.ensureCourierStash(level, contract(h, QuestType.DELIVERY, pos)), "Legacy delivery chest was not migrated");
            var crate = (de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(crate.getContainerSize() == 27 && crate.getItem(0).is(Items.DIAMOND) && crate.getItem(0).getCount() == 3
                    && crate.getName().getString().equals("Old stash"), "Migrating lost stash items or its name");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "Migrating spilled the old stash contents");
            var saved = crate.saveWithFullMetadata(level.registryAccess());
            var restored = (de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, crate.getBlockState(), saved, level.registryAccess());
            h.assertTrue(restored != null && restored.getItem(0).getCount() == 3 && restored.getName().getString().equals("Old stash"), "Metal crate contents or type did not survive save/load");
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
            player.openMenu(crate);
            h.assertTrue(player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu && menu.getContainer() == crate
                    && level.getBlockState(pos).getValue(net.minecraft.world.level.block.BarrelBlock.OPEN), "Metal crate did not open as a normal 27-slot container");
            player.containerMenu.quickMoveStack(player, 0);
            h.assertTrue(crate.isEmpty() && player.getInventory().countItem(Items.DIAMOND) == 3, "Shift-clicking the crate lost items");
            player.closeContainer();
            h.assertTrue(!level.getBlockState(pos).getValue(net.minecraft.world.level.block.BarrelBlock.OPEN), "Metal crate did not close");
            h.setBlock(new BlockPos(5, 1, 2), Blocks.STONE);
            h.setBlock(new BlockPos(5, 2, 2), Blocks.BARREL);
            var dropPos = h.absolutePos(new BlockPos(5, 2, 2));
            ((net.minecraft.world.Container) level.getBlockEntity(dropPos)).setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
            h.assertTrue(PlayerQuestManager.ensureExtractionDrop(level, contract(h, QuestType.EXTRACTION, dropPos)), "Legacy extraction barrel was not migrated");
            var drop = (net.minecraft.world.Container) level.getBlockEntity(dropPos);
            h.assertTrue(level.getBlockState(dropPos).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.IRON_QUEST_CRATE.get())
                    && drop.getItem(0).getCount() == 2 && drop.getItem(0).is(Items.GOLD_INGOT) && drop.getItem(1).is(Items.AMETHYST_SHARD), "Migrating the extraction barrel lost contents or failed to provide the quest shard");
        } finally {
            player.closeContainer();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_item_uniqueness", timeoutTicks = 80)
    public static void replacementInvalidatesOldPickupsCursorsAndContainerCopiesAndSurvivesSaving(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-unique");
        var observer = feedbackPlayer(h, "quest-picker");
        var player = owner.player();
        try {
            String contract = UUID.randomUUID().toString();
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("QuestContractId", contract);
            tag.putString("QuestItemType", "exfil_shard");
            var original = new ItemStack(Items.AMETHYST_SHARD);
            original.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            var versions = QuestItemVersions.get(h.getLevel());
            h.assertTrue(versions.current(original), "Existing unversioned quest item was invalidated before replacement");
            versions.issue(original);
            var replacement = original.copy();
            versions.issue(replacement);
            var restored = QuestItemVersions.load(versions.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(!restored.current(original) && restored.current(replacement), "Saving forgot which quest item was current");
            player.getInventory().setItem(0, replacement.copy());
            var dropped = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), original.copy());
            dropped.setNoPickUpDelay();
            h.getLevel().addFreshEntity(dropped);
            dropped.playerTouch(observer.player());
            h.assertTrue(dropped.isRemoved() && observer.player().getInventory().isEmpty(), "Another player could pick up a superseded quest item");
            var chest = new net.minecraft.world.SimpleContainer(27);
            chest.setItem(0, original.copy());
            chest.setItem(1, replacement.copy());
            var menu = net.minecraft.world.inventory.ChestMenu.threeRows(3, player.getInventory(), chest);
            player.containerMenu = menu;
            menu.setCarried(original.copy());
            PlayerQuestManager.sanitizeQuestItems(player);
            h.assertTrue(menu.getCarried().isEmpty() && chest.isEmpty() && player.getInventory().countItem(Items.AMETHYST_SHARD) == 1,
                    "Old cursor/container copies or same-version duplicates survived cleanup");
            var otherTag = tag.copy();
            otherTag.putString("QuestItemType", "courier_package");
            var other = new ItemStack(Items.PAPER);
            other.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(otherTag));
            player.getInventory().setItem(2, other);
            var cloned = replacement.copyWithCount(5);
            player.getInventory().setItem(1, cloned);
            PlayerQuestManager.sanitizeQuestItems(player);
            h.assertTrue(player.getInventory().countItem(Items.AMETHYST_SHARD) == 1 && player.getInventory().countItem(Items.PAPER) == 1,
                    "Duplicate stacks were not removed independently per quest item type");
        } finally {
            player.closeContainer();
            player.server.getPlayerList().remove(player);
            observer.player().server.getPlayerList().remove(observer.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_item_recovery", timeoutTicks = 80)
    public static void cursorItemsAreProtectedAndMissingCargosRequireExplicitReplacement(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-cursor");
        var player = owner.player();
        try {
            for (boolean creative : new boolean[]{false, true}) {
                PlayerQuestManager.clearAllQuestData(player);
                player.getInventory().clearContent();
                player.setGameMode(creative ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SURVIVAL);
                var quest = contract(h, QuestType.DELIVERY, h.absolutePos(new BlockPos(2, 2, 2)));
                var board = new net.minecraft.nbt.CompoundTag();
                board.put("Active", quest.toTag());
                var boards = new net.minecraft.nbt.CompoundTag();
                boards.put(quest.issuerId().toString(), board);
                var root = new net.minecraft.nbt.CompoundTag();
                root.put("IssuerBoards", boards);
                player.getPersistentData().put("cyberneticenhancements.quest_state", root);
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.putString("QuestContractId", quest.id());
                tag.putString("QuestItemType", "courier_package");
                var cargo = new ItemStack(de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get());
                cargo.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                player.containerMenu.setCarried(creative ? ItemStack.EMPTY : cargo);
                for (int tick = 0; tick < 200; tick++) {
                    player.tickCount++;
                    if (creative && tick % 10 == 0) PlayerQuestManager.updateCreativeCursor(player, quest.id(), "courier_package", 0);
                    PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                    h.assertTrue(player.getInventory().isEmpty(), "A cursor-held quest item was replaced in " + (creative ? "creative" : "survival"));
                }
                player.containerMenu.setCarried(ItemStack.EMPTY);
                PlayerQuestManager.updateCreativeCursor(player, "", "", 0);
                for (int tick = 0; tick <= 200; tick++) {
                    player.tickCount++;
                    PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                    h.assertTrue(player.getInventory().isEmpty(), "Missing cargo was restored automatically");
                    h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().deliveryItemMissing() == (tick == 200),
                            "Missing cargo did not get a replacement-call instruction after ten seconds");
                }
            }
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_site_spacing", timeoutTicks = 40)
    public static void reservationsSeparateSitesAndSurviveReload(GameTestHelper h) {
        var sites = new QuestSiteReservations();
        var pos = new BlockPos(1000, 64, 1000);
        var active = contract(h, QuestType.EXTRACTION, pos);
        sites.reserve(active, true, 0);
        h.assertTrue(!sites.available(pos.above(100), 32, "", 100000), "Vertical separation allowed an overlapping site");
        h.assertTrue(!sites.available(pos.offset(63, 0, 0), 32, "", 100000), "Nearby site was allowed");
        h.assertTrue(sites.available(pos.offset(64, 0, 0), 32, "", 100000), "Site clearance was larger than specified");
        sites.reserve(active, false, 0);
        h.assertTrue(!sites.available(pos, 32, "", 100000), "Offer refresh expired an active reservation");
        h.assertTrue(sites.available(pos, 32, active.id(), 100000), "Accepting the reserved offer conflicted with itself");
        sites.finish(active.id());
        var restored = QuestSiteReservations.load(sites.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(!restored.available(pos, 32, "", 100000), "Recent completed site was forgotten on reload");
        var pending = contract(h, QuestType.DELIVERY, pos.offset(200, 0, 0));
        restored.reserve(pending, false, 0);
        h.assertTrue(!restored.available(pending.targetPos(), 32, "", 47999), "Pending offer did not reserve its site");
        h.assertTrue(restored.available(pending.targetPos(), 32, "", 48000), "Unused expired offer blocked its site forever");
        active.setTargetLocation(active.dimensionId(), pos.offset(400, 0, 0), 1);
        restored.reserve(active, true, 100000);
        h.assertTrue(!restored.available(pos, 32, "", 100000), "Moving a quest forgot its previous site");
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "quest_feedback", timeoutTicks = 80)
    public static void everyQuestTypeNotifiesOnlyItsOwnerAndOnlyOnTransition(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-owner");
        var observer = feedbackPlayer(h, "quest-observer");
        try {
            drain(owner.channel()); drain(observer.channel());
            for (QuestType type : QuestType.values()) {
                var quest = contract(h, type, h.absolutePos(new BlockPos(2, 1, 2)));
                PlayerQuestManager.markQuestReady(owner.player(), quest);
                var packets = drain(owner.channel());
                long chat = packets.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket message && !message.overlay()).count();
                long actionBar = packets.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket message && message.overlay()).count();
                long sound = packets.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket).count();
                h.assertTrue(quest.isReady() && chat == 1 && actionBar == 0 && sound == 1, "Quest type did not send consistent local feedback: " + type);
                h.assertTrue(drain(observer.channel()).stream().noneMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket
                        || packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket), "Another player received quest feedback: " + type);
                PlayerQuestManager.markQuestReady(owner.player(), quest);
                h.assertTrue(drain(owner.channel()).isEmpty(), "Ready quest repeated its feedback: " + type);
            }
        } finally {
            owner.player().server.getPlayerList().remove(owner.player());
            observer.player().server.getPlayerList().remove(observer.player());
        }
        h.succeed();
    }

    private static FeedbackPlayer feedbackPlayer(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return new FeedbackPlayer(player, channel);
    }

    private static java.util.List<Object> drain(io.netty.channel.embedded.EmbeddedChannel channel) {
        channel.runPendingTasks();
        java.util.List<Object> packets = new java.util.ArrayList<>();
        Object packet;
        while ((packet = channel.readOutbound()) != null) packets.add(packet);
        return packets;
    }

    private record FeedbackPlayer(net.minecraft.server.level.ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {
    }
    private static QuestContract contract(GameTestHelper h, QuestType type, BlockPos pos) {
        var legacy = new QuestContract(UUID.randomUUID().toString(), UUID.randomUUID(), "Test", "netrunner", type,
                1, QuestStatus.ACTIVE, 0, 1, 0, false, h.getLevel().dimension().location().toString(), pos,
                8, 100, 1, 0, 0, 0, 0);
        var tag = legacy.toTag();
        tag.remove("SiteBindingVersion");
        return QuestContract.fromTag(tag);
    }

    @GameTest(template = "sandevistan_test", batch = "quest_stash_ground", timeoutTicks = 40)
    public static void underwaterStashesRestOnSeabed(GameTestHelper h) {
        var level = h.getLevel();
        for (int x : new int[]{2, 6}) {
            h.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
            for (int y = 2; y <= 9; y++) h.setBlock(new BlockPos(x, y, 2), Blocks.AIR);
            for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(x, y, 2), Blocks.WATER);
        }
        BlockPos target = PlayerQuestManager.courierGroundPosition(level, h.absolutePos(new BlockPos(2, 2, 2)).getX(), h.absolutePos(new BlockPos(2, 2, 2)).getZ());
        h.assertTrue(target.equals(h.absolutePos(new BlockPos(2, 2, 2))), "Incorrect stash target: " + target + "; expected " + h.absolutePos(new BlockPos(2, 2, 2)) + "; support=" + level.getBlockState(target.below()));
        var delivery = contract(h, QuestType.DELIVERY, target);
        h.assertTrue(PlayerQuestManager.ensureCourierStash(level, delivery), "Seabed chest did not spawn");
        h.assertTrue(level.getBlockState(target).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.IRON_QUEST_CRATE.get()) && level.getBlockState(target).getValue(BlockStateProperties.WATERLOGGED), "Underwater quest crate was not waterlogged");
        h.assertTrue(level.getBlockState(target.below()).is(Blocks.STONE), "Chest replaced seabed with a lodestone");
        h.assertTrue(!PlayerQuestManager.ensureCourierStash(level, delivery), "Existing chest was replaced");
        BlockPos floating = h.absolutePos(new BlockPos(2, 6, 2));
        h.assertTrue(!PlayerQuestManager.ensureCourierStash(level, contract(h, QuestType.DELIVERY, floating)), "Unsupported floating chest was created");
        BlockPos drop = h.absolutePos(new BlockPos(6, 2, 2));
        var extraction = contract(h, QuestType.EXTRACTION, drop);
        h.assertTrue(PlayerQuestManager.ensureExtractionDrop(level, extraction), "Seabed dead drop did not spawn");
        h.assertTrue(level.getBlockState(drop).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.IRON_QUEST_CRATE.get()) && level.getBlockState(drop).getValue(BlockStateProperties.WATERLOGGED) && level.getBlockState(drop.below()).is(Blocks.STONE), "Dead drop was not a waterlogged metal crate on the seabed");
        h.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        BlockPos dry = h.absolutePos(new BlockPos(4, 2, 2));
        h.assertTrue(PlayerQuestManager.ensureCourierStash(level, contract(h, QuestType.DELIVERY, dry)), "Land chest did not spawn");
        h.assertTrue(!level.getBlockState(dry).getValue(BlockStateProperties.WATERLOGGED), "Land chest was waterlogged");
        h.succeed();
    }
}
