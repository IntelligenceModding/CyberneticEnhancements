package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TarotGameTests {
    @GameTest(template = "sandevistan_test", batch = "tarot_decoration", timeoutTicks = 110)
    public static void placedDecorationsPersistButCannotBeScannedOrLocatedAndCanBeRemoved(GameTestHelper h) {
        var player = player(h); player.setNoGravity(true);
        BlockPos anchor = h.absolutePos(new BlockPos(3, 1, 3)); wall(h, anchor, Direction.SOUTH);
        var origin = TarotCardEntity.attachmentPosition(anchor, Direction.SOUTH);
        player.setPos(origin.x, anchor.getY(), origin.z + 3); player.setYRot(180); player.setYHeadRot(180); player.setXRot(0);
        var at = TarotDecorations.preview(player);
        h.assertTrue(at != null && at.anchor().equals(anchor) && at.facing() == Direction.SOUTH, "Preview did not align with the targeted wall");
        h.assertTrue(!TarotDecorations.place(player, "the_fool", at), "An uncollected card could be placed");
        TarotKnowledge.learn(player, "the_fool");
        h.assertTrue(TarotDecorations.place(player, "the_fool", at), "Collected card could not be placed");
        var cards = h.getLevel().getEntitiesOfClass(TarotCardEntity.class, TarotCardEntity.attachmentBounds(anchor, Direction.SOUTH).inflate(.05));
        h.assertTrue(cards.size() == 1 && cards.getFirst().isDecoration(), "Placed card was not marked as a decoration");
        var card = cards.getFirst();
        h.assertTrue(!TarotDecorations.place(player, "the_fool", at), "Overlapping decorations were accepted");
        CompoundTag tag = new CompoundTag(); card.saveWithoutId(tag);
        var restored = ModEntityTypes.TAROT_CARD.get().create(h.getLevel()); restored.load(tag);
        h.assertTrue(restored.isDecoration() && restored.anchor().equals(anchor) && restored.cardId().equals("the_fool"), "Decoration flag or attachment was lost on reload");
        TarotPlacements.get(h.getLevel()).register(restored);
        h.assertTrue(TarotPlacements.get(h.getLevel()).nearest(origin).map(location -> !location.anchor().equals(anchor)).orElse(true), "Decoration leaked into the collectible locator");
        TarotKnowledge.change(player, java.util.List.of("the_fool"), false);
        h.assertTrue(TarotScanning.target(player) == null, "Decoration can be targeted for scanning by an unknowing player");
        h.onEachTick(() -> {
            TarotScanning.receive(player, new TarotScanPayload(card.getId(), true));
            TarotScanning.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        });
        h.runAfterDelay(75, () -> {
            try {
                h.assertTrue(!card.isRemoved() && TarotKnowledge.known(player).isEmpty(), "A forged scan consumed decoration or unlocked a reward");
                h.assertTrue(TarotPlacements.get(h.getLevel()).nearest(origin).map(location -> !location.anchor().equals(anchor)).orElse(true), "Ticking re-added decoration to the locator");
                player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                h.assertTrue(!card.hurt(player.damageSources().playerAttack(player), 1), "An adventure player could remove a decoration");
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                h.assertTrue(card.hurt(player.damageSources().playerAttack(player), 1) && card.isRemoved(), "Player could not remove a decorative copy by punching it");
                h.succeed();
            } finally { card.discard(); player.server.getPlayerList().remove(player); }
        });
    }

    @GameTest(template = "sandevistan_test", batch = "tarot_placement_validation", timeoutTicks = 30)
    public static void placementRejectsBlockedDistantLockedAndNonBuildingRequests(GameTestHelper h) {
        var player = player(h); player.setNoGravity(true);
        BlockPos anchor = h.absolutePos(new BlockPos(3, 1, 3)); wall(h, anchor, Direction.SOUTH);
        var origin = TarotCardEntity.attachmentPosition(anchor, Direction.SOUTH);
        player.setPos(origin.x, anchor.getY(), origin.z + 3); player.setYRot(180); player.setYHeadRot(180); player.setXRot(0);
        TarotKnowledge.learn(player, "the_fool");
        var at = new TarotDecorations.Placement(anchor, Direction.SOUTH);
        try {
            h.assertTrue(!TarotDecorations.place(player, "the_magician", at) && !TarotDecorations.place(player, "invalid", at), "Locked or unknown card was accepted");
            h.assertTrue(!TarotDecorations.place(player, "the_fool", new TarotDecorations.Placement(anchor, Direction.UP)), "A floor face was accepted");
            BlockPos distant = anchor.north(8); wall(h, distant, Direction.SOUTH);
            h.assertTrue(!TarotDecorations.place(player, "the_fool", new TarotDecorations.Placement(distant, Direction.SOUTH)), "A card was placed through another wall or beyond reach");
            h.getLevel().setBlockAndUpdate(anchor.above(), Blocks.STONE.defaultBlockState());
            h.assertTrue(!TarotDecorations.place(player, "the_fool", at), "Blocked placement was accepted");
            h.getLevel().setBlockAndUpdate(anchor.above(), Blocks.AIR.defaultBlockState());
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            h.assertTrue(!TarotDecorations.place(player, "the_fool", at), "Player without building permission could place a card");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(TarotDecorations.place(player, "the_fool", at), "Valid placement failed after permissions were restored");
            var card = h.getLevel().getEntitiesOfClass(TarotCardEntity.class, TarotCardEntity.attachmentBounds(anchor, Direction.SOUTH)).getFirst();
            h.getLevel().setBlockAndUpdate(anchor.relative(Direction.NORTH), Blocks.AIR.defaultBlockState());
            for (int i = 0; i < 40; i++) card.tick();
            h.assertTrue(card.isRemoved(), "Decoration remained floating after its support was broken");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                var payload = new TarotDecorationPayload("the_fool", at);
                TarotDecorationPayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(payload.equals(TarotDecorationPayload.STREAM_CODEC.decode(buffer)), "Placement packet lost its wall position");
            } finally { buffer.release(); }
        } finally { player.server.getPlayerList().remove(player); }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "tarot_commands", timeoutTicks = 30)
    public static void giveAndRemoveCommandsUpdateOnlyTheSelectedPlayersAndRevokeTheirBonuses(GameTestHelper h) throws Exception {
        var operator = player(h, "tarot-operator"); var recipient = player(h, "tarot-recipient");
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        de.artemis.cyberneticenhancements.common.command.CyberwareDebugCommand.register(dispatcher);
        var source = operator.createCommandSourceStack().withPermission(2);
        String command = "cyberneticenhancements dev tarot ";
        var health = recipient.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        var externalId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_test_external_health");
        health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(externalId, 4,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        float baseline = recipient.getMaxHealth();
        double fallBaseline = recipient.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE);
        try {
            var tarot = dispatcher.getRoot().getChild("cyberneticenhancements").getChild("dev").getChild("tarot");
            h.assertTrue(!tarot.getChild("give").canUse(source.withPermission(0)) && !tarot.getChild("remove").canUse(source.withPermission(0)), "Tarot changes do not require operator permission");
            h.assertTrue(dispatcher.execute(command + "give tarot-recipient the_fool", source) == 1, "Could not grant a card to another player");
            h.assertTrue(TarotKnowledge.known(operator).isEmpty() && TarotKnowledge.known(recipient).equals(java.util.Set.of("the_fool"))
                    && recipient.getMaxHealth() == baseline + 1
                    && recipient.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE) == fallBaseline + .5,
                    "Giving a card changed the wrong player or missed one of its bonuses");
            h.assertTrue(dispatcher.execute(command + "give tarot-recipient the_fool", source) == 0 && recipient.getMaxHealth() == baseline + 1, "Repeated give stacked a bonus");
            h.assertTrue(dispatcher.execute(command + "give tarot-recipient all", source) == 37 && TarotKnowledge.known(recipient).size() == 38, "Bulk give did not complete the collection");
            recipient.setHealth(recipient.getMaxHealth());
            h.assertTrue(dispatcher.execute(command + "remove tarot-recipient the_fool", source) == 1 && TarotKnowledge.known(recipient).size() == 37
                    && recipient.getMaxHealth() == baseline + 9 && recipient.getHealth() <= recipient.getMaxHealth()
                    && recipient.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE) == fallBaseline + 2,
                    "Single-card removal left a bonus or excess health");
            h.assertTrue(dispatcher.execute(command + "remove tarot-recipient all", source) == 37 && TarotKnowledge.known(recipient).isEmpty()
                    && recipient.getMaxHealth() == baseline && health.getModifier(externalId) != null && TarotRewards.chromeHeadroom(recipient) == 0
                    && recipient.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE) == fallBaseline,
                    "Bulk removal left tarot bonuses or removed another modifier");
            h.assertTrue(dispatcher.execute(command + "remove tarot-recipient all", source) == 0, "Repeated removal reported a change");
            h.assertTrue(dispatcher.execute(command + "give @s the_magician", source) == 1 && TarotKnowledge.known(operator).contains("the_magician")
                    && TarotKnowledge.known(recipient).isEmpty(), "Player selector selected the wrong target");
            h.assertTrue(dispatcher.execute(command + "remove @s the_magician", source) == 1, "Player selector could not remove a card");
            h.assertTrue(dispatcher.execute(command + "give tarot-recipient invalid", source) == 0 && TarotKnowledge.known(recipient).isEmpty(), "Unknown card changed a collection");
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(command + "give tarot-recipient ", source)).get().getList();
            h.assertTrue(suggestions.stream().anyMatch(s -> s.getText().equals("all")) && suggestions.stream().anyMatch(s -> s.getText().equals("the_magician")), "Missing card autocomplete");
            recipient.setHealth(0);
            h.assertTrue(dispatcher.execute(command + "give tarot-recipient the_fool", source) == 1 && recipient.getHealth() == 0, "Giving a card revived a dead player");
            dispatcher.execute(command + "remove tarot-recipient all", source);
            CompoundTag saved = new CompoundTag(); recipient.saveWithoutId(saved);
            var reloaded = new ServerPlayer(recipient.server, h.getLevel(), recipient.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            reloaded.load(saved); TarotRewards.apply(reloaded);
            h.assertTrue(TarotKnowledge.known(reloaded).isEmpty() && reloaded.getMaxHealth() == baseline, "Removed cards or bonuses returned on reload");
        } finally { operator.server.getPlayerList().remove(operator); recipient.server.getPlayerList().remove(recipient); }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "tarot_full_collection", timeoutTicks = 30)
    public static void allThirtyEightRewardsStackOncePersistAndSurviveDeathWithoutChromeCost(GameTestHelper h) {
        var level = h.getLevel(); var player = player(h);
        var inventory = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
        var attributes = java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, net.minecraft.world.entity.ai.attributes.Attributes.LUCK,
                net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED, net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,
                net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE);
        double[] baseline = attributes.stream().mapToDouble(player::getAttributeValue).toArray();
        double[] additions = {10, 3, 2.5, 1.25, baseline[4] * .06, baseline[5] * .025, 1.5, baseline[7] * .06, .5, .05, 2.5};
        int chrome = inventory.getInstalledChromeCost(), capacity = inventory.getChromeCapacity();
        try {
            for (var card : TarotCatalog.CARDS) h.assertTrue(TarotKnowledge.learn(player, card.id()), "New card could not be learned: " + card.id());
            h.assertTrue(TarotKnowledge.known(player).size() == 38, "Full collection is incomplete");
            for (var card : TarotCatalog.CARDS) h.assertTrue(!TarotKnowledge.learn(player, card.id()), "Duplicate card granted another reward");
            TarotKnowledge.sync(player); TarotRewards.apply(player);
            h.assertTrue(inventory.getInstalledChromeCost() == chrome && inventory.getChromeCapacity() == capacity + 60, "Headroom added chrome cost or failed to apply");
            CompoundTag saved = new CompoundTag(); player.saveWithoutId(saved);
            var reloaded = new ServerPlayer(player.server, level, player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            reloaded.load(saved); TarotRewards.apply(reloaded);
            var clone = new ServerPlayer(player.server, level, player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            TarotKnowledge.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
            for (var subject : java.util.List.of(player, reloaded, clone)) {
                h.assertTrue(TarotKnowledge.known(subject).size() == 38, "Collection did not persist through reload or death");
                for (int i = 0; i < attributes.size(); i++)
                    h.assertTrue(Math.abs(subject.getAttributeValue(attributes.get(i)) - baseline[i] - additions[i]) < 1e-6,
                            "Wrong or duplicated reward for " + attributes.get(i));
                h.assertTrue(TarotRewards.chromeHeadroom(subject) == 60, "Headroom was lost on reload or death");
            }
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
            try {
                var payload = new TarotKnowledgePayload(TarotCatalog.CARDS.stream().map(TarotCatalog.Card::id).toList());
                TarotKnowledgePayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(TarotKnowledgePayload.STREAM_CODEC.decode(buffer).ids().equals(payload.ids()), "Collection packet lost card identities");
            } finally { buffer.release(); }
        } finally { player.server.getPlayerList().remove(player); }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "tarot_locate", timeoutTicks = 30)
    public static void locatorKeepsUnloadedCardsButRemovesCollectedOnesAndRequiresPermission(GameTestHelper h) throws Exception {
        var level = h.getLevel(); var player = player(h);
        var near = ModEntityTypes.TAROT_CARD.get().create(level);
        var far = ModEntityTypes.TAROT_CARD.get().create(level);
        near.attach(h.absolutePos(new BlockPos(3, 1, 3)), Direction.SOUTH, "the_fool");
        far.attach(near.anchor().east(20), Direction.NORTH, "the_fool");
        var index = TarotPlacements.get(level);
        try {
            index.register(far); index.register(near);
            var origin = net.minecraft.world.phys.Vec3.atCenterOf(near.anchor());
            player.setPos(origin);
            h.assertTrue(index.nearest(origin).orElseThrow().anchor().equals(near.anchor()), "Locator did not choose the nearest card");
            var saved = TarotPlacements.load(index.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            h.assertTrue(saved.nearest(origin).orElseThrow().anchor().equals(near.anchor()), "Locator index did not survive save/reload");
            near.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            h.assertTrue(index.nearest(origin).orElseThrow().anchor().equals(near.anchor()), "Unloading hid the card from the locator");
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            de.artemis.cyberneticenhancements.common.command.CyberwareDebugCommand.register(dispatcher);
            var source = player.createCommandSourceStack().withPermission(2);
            h.assertTrue(!dispatcher.getRoot().getChild("cyberneticenhancements").getChild("dev").getChild("tarot").canUse(source.withPermission(0)), "Non-operator can locate tarot cards");
            h.assertTrue(dispatcher.execute("cyberneticenhancements dev tarot locate", source) == 1, "Locate command did not find an unloaded card");
            h.assertTrue(dispatcher.execute("cyberneticenhancements dev tarot locate all", source) == 1, "All-card search failed");
            h.assertTrue(dispatcher.execute("cyberneticenhancements dev tarot locate the_fool", source) == 1, "Specific-card search failed");
            h.assertTrue(dispatcher.execute("cyberneticenhancements dev tarot locate invalid", source) == 0, "Unknown card filter was accepted");
            h.assertTrue(index.nearest(origin, "the_fool").orElseThrow().anchor().equals(near.anchor()) && index.nearest(origin, "invalid").isEmpty(), "Locator ignored its card filter");
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("cyberneticenhancements dev tarot locate ", source)).get().getList();
            h.assertTrue(suggestions.stream().anyMatch(s -> s.getText().equals("all")) && suggestions.stream().anyMatch(s -> s.getText().equals("the_fool")), "Card selector autocomplete is incomplete");
            var copy = ModEntityTypes.TAROT_CARD.get().create(level); copy.setUUID(near.getUUID()); copy.attach(near.anchor(), near.facing(), near.cardId());
            copy.discard();
            h.assertTrue(!index.nearest(origin).orElseThrow().anchor().equals(near.anchor()), "Collected card remained in the index");
            var empty = new TarotPlacements(); h.assertTrue(empty.nearest(origin).isEmpty(), "Empty index returned a card");
        } finally { index.remove(near.getUUID()); index.remove(far.getUUID()); player.server.getPlayerList().remove(player); }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "tarot_structure", timeoutTicks = 30)
    public static void structureProcessingCannotPlaceASecondCardEvenAfterCollection(GameTestHelper h) {
        var level = h.getLevel(); BlockPos anchor = h.absolutePos(new BlockPos(3, 1, 3));
        wall(h, anchor, Direction.SOUTH);
        var chunk = level.getChunkAt(anchor); var pos = chunk.getPos();
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structure = registry.stream().filter(s -> TarotWorldgen.selected(level.getSeed(), registry.getKey(s) + "@" + pos.toLong())).findFirst().orElseThrow();
        var piece = new net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece(net.minecraft.util.RandomSource.create(1), 0, 0);
        var b = piece.getBoundingBox(); piece.move(anchor.getX() - 2 - b.minX(), anchor.getY() - b.minY(), anchor.getZ() - 2 - b.minZ());
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, pos, 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        var previous = new java.util.HashMap<>(chunk.getAllStarts());
        String key = TarotWorldgen.structureKey(level, start);
        var area = net.minecraft.world.phys.AABB.of(start.getBoundingBox()).inflate(2);
        try {
            chunk.setStartForStructure(structure, start); TarotWorldgen.enqueue(level, chunk);
            for (int i = 0; i < 150; i++) TarotWorldgen.onLevelTick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(() -> true, level));
            var cards = level.getEntitiesOfClass(TarotCardEntity.class, area);
            h.assertTrue(cards.size() == 1 && TarotPlacements.get(level).contains(key), "Structure did not receive exactly one persisted card");
            h.assertTrue(cards.getFirst().cardId().equals(TarotCatalog.forStructure(level.getSeed(), key).id()), "Structure did not use its assigned card identity");
            TarotWorldgen.enqueue(level, chunk);
            for (int i = 0; i < 150; i++) TarotWorldgen.onLevelTick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(() -> true, level));
            h.assertTrue(level.getEntitiesOfClass(TarotCardEntity.class, area).size() == 1, "Structure received duplicate cards");
            cards.forEach(net.minecraft.world.entity.Entity::discard); TarotWorldgen.enqueue(level, chunk);
            for (int i = 0; i < 150; i++) TarotWorldgen.onLevelTick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(() -> true, level));
            h.assertTrue(level.getEntitiesOfClass(TarotCardEntity.class, area).isEmpty(), "Collected card respawned in the same structure");
        } finally { chunk.setAllStarts(previous); level.getEntitiesOfClass(TarotCardEntity.class, area).forEach(net.minecraft.world.entity.Entity::discard); }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "tarot_wall", timeoutTicks = 20)
    public static void allWallDirectionsRequireFullSupportAndKeepTheirAttachmentOnReload(GameTestHelper h) {
        for (Direction face : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos anchor = h.absolutePos(new BlockPos(3, 1, 3));
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = 0; y < 4; y++)
                h.getLevel().setBlockAndUpdate(anchor.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
            wall(h, anchor, face);
            for (int col = 0; col < 2; col++)
                h.getLevel().setBlockAndUpdate(anchor.relative(face.getCounterClockWise(), col).above(3), Blocks.STONE.defaultBlockState());
            h.assertTrue(TarotCardEntity.canAttach(h.getLevel(), anchor, face), "Flat wall rejected: " + face);
            var card = ModEntityTypes.TAROT_CARD.get().create(h.getLevel()); card.attach(anchor, face, "the_fool");
            h.assertTrue(card.getBoundingBox().getYsize() == 3 && card.getBoundingBox().maxY == anchor.getY() + 3
                    && (face.getAxis() == Direction.Axis.X ? card.getBoundingBox().getZsize() : card.getBoundingBox().getXsize()) == 2,
                    "Projection does not exactly fit a two-block-wide, three-block-high wall below a ceiling");
            CompoundTag tag = new CompoundTag(); card.saveWithoutId(tag);
            var restored = ModEntityTypes.TAROT_CARD.get().create(h.getLevel()); restored.load(tag);
            h.assertTrue(restored.anchor().equals(anchor) && restored.facing() == face && restored.cardId().equals("the_fool")
                    && restored.getBoundingBox().equals(card.getBoundingBox()), "Card attachment changed after reload");
            BlockPos support = anchor.relative(face.getOpposite()).above(2);
            h.getLevel().setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
            h.assertTrue(!TarotCardEntity.canAttach(h.getLevel(), anchor, face), "Incomplete wall accepted");
            h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(anchor.above(), Blocks.STONE.defaultBlockState());
            h.assertTrue(!TarotCardEntity.canAttach(h.getLevel(), anchor, face), "Projection clipped through a block");
        }
        TarotPlacements data = new TarotPlacements(); data.mark("minecraft:desert_pyramid@42");
        var restored = TarotPlacements.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(restored.contains("minecraft:desert_pyramid@42"), "Collected structure placement was forgotten on reload");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "tarot_scan", timeoutTicks = 160)
    public static void heldScanResetsOnReleaseRejectsWallsAndPersistsTheCollection(GameTestHelper h) {
        BlockPos anchor = h.absolutePos(new BlockPos(3, 1, 3)); wall(h, anchor, Direction.SOUTH);
        var card = ModEntityTypes.TAROT_CARD.get().create(h.getLevel()); card.attach(anchor, Direction.SOUTH, "the_fool"); h.getLevel().addFreshEntity(card);
        var player = player(h); player.setNoGravity(true);
        float originalMaxHealth = player.getMaxHealth();
        var implants = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
        int originalChromeCost = implants.getInstalledChromeCost();
        player.setPos(card.getX(), anchor.getY(), card.getZ() + 3); player.setYRot(180); player.setYHeadRot(180); player.setXRot(0);
        h.assertTrue(TarotScanning.target(player) == card, "Card cannot be targeted from its front");
        BlockPos obstruction = BlockPos.containing(card.getX(), player.getEyeY(), card.getZ() + 1);
        h.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
        h.assertTrue(TarotScanning.target(player) == null, "Scan passed through a wall");
        TarotScanning.receive(player, new TarotScanPayload(card.getId(), true));
        h.getLevel().setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
        long start = h.getLevel().getGameTime();
        h.onEachTick(() -> {
            long tick = h.getLevel().getGameTime() - start;
            if (tick < 20 || tick >= 40 && tick <= 103) TarotScanning.receive(player, new TarotScanPayload(card.getId(), true));
            else TarotScanning.receive(player, new TarotScanPayload(card.getId(), false));
            // Embedded mock connections do not run the normal network player's tick.
            TarotScanning.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            if (tick == 80) h.assertTrue(!TarotKnowledge.known(player).contains("the_fool") && !card.isRemoved(), "Interrupted scan kept its progress");
        });
        h.runAfterDelay(108, () -> {
            h.assertTrue(card.isCollecting() && !card.isRemoved(), "Collected card vanished before its dissolve animation");
            h.assertTrue(TarotScanning.target(player) == null, "Dissolving card can still be scanned");
            TarotPlacements.get(h.getLevel()).register(card);
            h.assertTrue(TarotPlacements.get(h.getLevel()).nearest(card.position(), "the_fool").isEmpty(), "Dissolving card was registered as collectible again");
            var savedCard = new CompoundTag(); card.saveWithoutId(savedCard);
            var restoredCard = ModEntityTypes.TAROT_CARD.get().create(h.getLevel()); restoredCard.load(savedCard);
            h.assertTrue(restoredCard.isCollecting(), "Reloading a dissolving card made it collectible again");
        });
        h.runAfterDelay(120, () -> {
            try {
                h.assertTrue(card.isRemoved() && TarotKnowledge.known(player).contains("the_fool"), "Complete scan did not archive and remove the card");
                h.assertTrue(!TarotKnowledge.learn(player, "the_fool") && !TarotKnowledge.learn(player, "invalid"), "Duplicate/unknown card was accepted");
                TarotRewards.apply(player); TarotKnowledge.sync(player);
                h.assertTrue(player.getMaxHealth() == originalMaxHealth + 1 && implants.getInstalledChromeCost() == originalChromeCost,
                        "Reward did not add exactly half a heart for free, or stacked on duplicate collection/sync");
                var clone = new ServerPlayer(player.server, h.getLevel(), player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
                TarotKnowledge.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
                h.assertTrue(TarotKnowledge.known(clone).contains("the_fool"), "Collection was lost on death");
                h.assertTrue(clone.getMaxHealth() == originalMaxHealth + 1, "Permanent reward was lost on death");
                CompoundTag savedPlayer = new CompoundTag(); player.saveWithoutId(savedPlayer);
                var reloaded = new ServerPlayer(player.server, h.getLevel(), player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
                reloaded.load(savedPlayer); TarotRewards.apply(reloaded);
                h.assertTrue(TarotKnowledge.known(reloaded).contains("the_fool") && reloaded.getMaxHealth() == originalMaxHealth + 1,
                        "Collection or reward was lost or doubled after save/reload");
                h.succeed();
            } finally { player.server.getPlayerList().remove(player); }
        });
    }
    private static void wall(GameTestHelper h, BlockPos anchor, Direction face) {
        for (int col = 0; col < 2; col++) for (int y = 0; y < 3; y++) {
            BlockPos air = anchor.relative(face.getCounterClockWise(), col).above(y);
            h.getLevel().setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(air.relative(face.getOpposite()), Blocks.STONE.defaultBlockState());
        }
    }
    private static ServerPlayer player(GameTestHelper h) {
        return player(h, "tarot-test");
    }
    private static ServerPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
