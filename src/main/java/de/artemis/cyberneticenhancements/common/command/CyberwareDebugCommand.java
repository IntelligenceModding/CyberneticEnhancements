package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager;
import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CirculatoryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusType;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffects;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.FrontalCortexManager;
import de.artemis.cyberneticenhancements.common.cyberware.HandsCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.IntegumentaryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.LegCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.NervousSystemCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.cyberware.SkeletonCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.TemporaryCyberwareEffectManager;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CyberwareDebugCommand {
    private CyberwareDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal("dev")
                .then(Commands.literal("tarot")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("locate").executes(context -> locateTarot(context.getSource(), "all"))
                                .then(Commands.argument("card", StringArgumentType.word())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
                                                        de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.CARDS.stream().map(de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.Card::id)), builder))
                                        .executes(context -> locateTarot(context.getSource(), StringArgumentType.getString(context, "card")))))
                        .then(tarotCollectionCommand("give", true))
                        .then(tarotCollectionCommand("remove", false)))
                .then(Commands.literal("cyberpsycho")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> spawnCyberpsycho(context.getSource(), 2, null))
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 3))
                                .executes(context -> spawnCyberpsycho(context.getSource(), IntegerArgumentType.getInteger(context, "tier"), null))
                                .then(Commands.argument("loadout", StringArgumentType.word())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(CyberpsychoLoadout.values()).map(value -> value.name().toLowerCase(java.util.Locale.ROOT)), builder))
                                        .executes(context -> spawnCyberpsycho(context.getSource(), IntegerArgumentType.getInteger(context, "tier"),
                                                StringArgumentType.getString(context, "loadout"))))))
                .then(Commands.literal("medtec")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("coverage").executes(context -> grantMedtecCoverage(context.getSource()))))
                .then(Commands.literal("cyberware")
                        .executes(context -> execute(context.getSource())))
                .then(Commands.literal("applystatus")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(context -> applyCombatStatus(context.getSource(),
                                        StringArgumentType.getString(context, "type"), 1, 8, true))
                                .then(Commands.argument("stacks", IntegerArgumentType.integer(1, 3))
                                        .executes(context -> applyCombatStatus(context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "stacks"),
                                                8,
                                                true))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                                .executes(context -> applyCombatStatus(context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        IntegerArgumentType.getInteger(context, "stacks"),
                                                        IntegerArgumentType.getInteger(context, "seconds"),
                                                        true))
                                                .then(Commands.argument("source", StringArgumentType.word())
                                                        .executes(context -> applyCombatStatus(context.getSource(),
                                                                StringArgumentType.getString(context, "type"),
                                                                IntegerArgumentType.getInteger(context, "stacks"),
                                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                                !"ambient".equalsIgnoreCase(StringArgumentType.getString(context, "source")))))))))
                .then(Commands.literal("reset")
                        .then(Commands.literal("cooldowns")
                                .executes(context -> resetCooldowns(context.getSource())))
                        .then(Commands.literal("buffs")
                                .executes(context -> resetBuffs(context.getSource())))
                        .then(Commands.literal("statuses")
                                .executes(context -> resetBuffs(context.getSource())))
                        .then(Commands.literal("all")
                                .executes(context -> resetAll(context.getSource())))
                        .then(Commands.literal("both")
                                .executes(context -> resetAll(context.getSource()))));

        dispatcher.register(
                Commands.literal("cyberneticenhancements")
                        .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                        .then(debugCommand)
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tarotCollectionCommand(String action, boolean give) {
        return Commands.literal(action).requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("players", net.minecraft.commands.arguments.EntityArgument.players())
                        .then(Commands.argument("card", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
                                                de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.CARDS.stream().map(de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.Card::id)), builder))
                                .executes(context -> changeTarot(context.getSource(),
                                        net.minecraft.commands.arguments.EntityArgument.getPlayers(context, "players"),
                                        StringArgumentType.getString(context, "card"), give))));
    }

    private static int changeTarot(CommandSourceStack source, java.util.Collection<ServerPlayer> players, String filter, boolean give) {
        if (!filter.equals("all") && de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.find(filter) == null) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.tarot_unknown", filter)); return 0;
        }
        var ids = filter.equals("all") ? de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.CARDS.stream()
                .map(de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.Card::id).toList() : java.util.List.of(filter);
        int changedTotal = 0;
        for (ServerPlayer player : players) {
            int changed = de.artemis.cyberneticenhancements.common.tarot.TarotKnowledge.change(player, ids, give);
            changedTotal += changed;
            if (changed == 0) de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.tarot_unchanged", player.getDisplayName()), false);
            else de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(give ? "command.cyberneticenhancements.debug.tarot_given" : "command.cyberneticenhancements.debug.tarot_removed",
                    changed, player.getDisplayName()), false);
        }
        return changedTotal;
    }

    private static int locateTarot(CommandSourceStack source, String filter) {
        if (!filter.equals("all") && de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.find(filter) == null) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.tarot_unknown", filter)); return 0;
        }
        var level = source.getLevel();
        var placements = de.artemis.cyberneticenhancements.common.tarot.TarotPlacements.get(level);
        // Includes older cards that predate the persisted locator index, without loading chunks.
        for (var entity : level.getAllEntities())
            if (entity instanceof de.artemis.cyberneticenhancements.common.tarot.TarotCardEntity card && !card.isRemoved()) placements.register(card);
        var nearest = placements.nearest(source.getPosition(), filter);
        if (nearest.isEmpty()) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.tarot_none", filter)); return 0;
        }
        var card = nearest.get(); BlockPos at = card.anchor();
        BlockPos approach = at.relative(card.facing(), 2);
        String teleport = "/tp @s " + (approach.getX() + .5) + " " + approach.getY() + " " + (approach.getZ() + .5) + " " + card.facing().getOpposite().toYRot() + " 0";
        Component coordinates = Component.literal("[" + at.getX() + ", " + at.getY() + ", " + at.getZ() + "]")
                .withStyle(style -> style.withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, teleport))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        int distance = (int) Math.sqrt(net.minecraft.world.phys.Vec3.atCenterOf(at).distanceToSqr(source.getPosition()));
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.tarot_found",
                de.artemis.cyberneticenhancements.common.tarot.TarotCatalog.find(card.cardId()).name(), coordinates, distance), false);
        return 1;
    }

    private static int spawnCyberpsycho(CommandSourceStack source, int tier, String loadoutName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var level = player.serverLevel();
        CyberpsychoLoadout loadout;
        try {
            loadout = loadoutName == null ? CyberpsychoLoadout.fromIndex(level.random.nextInt(CyberpsychoLoadout.values().length))
                    : CyberpsychoLoadout.valueOf(loadoutName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.cyberpsycho_loadout"));
            return 0;
        }
        var psycho = ModEntityTypes.CYBERPSYCHO.get().create(level);
        if (psycho == null) return 0;
        double facing = Math.toRadians(player.getYRot());
        for (int radius : new int[]{8, 12, 16}) for (int offset = 0; offset < 16; offset++) {
            double angle = facing + offset * Math.PI / 8;
            var column = BlockPos.containing(player.getX() - Math.sin(angle) * radius, player.getY(), player.getZ() + Math.cos(angle) * radius);
            if (!level.hasChunkAt(column)) continue;
            for (int dy : new int[]{0, -1, 1, -2, 2, -3, 3, -4, -5, -6}) {
                var pos = column.above(dy);
                if (pos.getY() < level.getMinBuildHeight() + 1 || pos.getY() + 2 >= level.getMaxBuildHeight()
                        || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
                psycho.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180, 0);
                if (!level.noCollision(psycho) || !level.getEntities(psycho, psycho.getBoundingBox(),
                        entity -> entity instanceof net.minecraft.world.entity.LivingEntity && entity.isAlive()).isEmpty()) continue;
                psycho.configureDebugEncounter(tier, loadout, pos);
                if (!player.isCreative() && !player.isSpectator()) psycho.setTarget(player);
                if (!level.addFreshEntity(psycho)) { psycho.discard(); return 0; }
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.cyberpsycho_spawned",
                        psycho.getName(), tier, loadout.label(), pos.getX(), pos.getY(), pos.getZ()), false);
                return 1;
            }
        }
        psycho.discard();
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.cyberpsycho_no_space"));
        return 0;
    }

    private static int grantMedtecCoverage(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        player.getPersistentData().putBoolean(de.artemis.cyberneticenhancements.common.world.TraumaCoverage.COVERAGE, true);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.medtec_coverage"), false);
        return 1;
    }

    private static int execute(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CompoundTag storedTag = PlayerCyberwareInventory.getStoredInventoryTag(player);

        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.header"), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.installed_count", inventory.getInstalledCount()), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.chrome", inventory.getInstalledChromeCost(), inventory.getChromeCapacity()), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.cyberstrain", inventory.getCyberstrain()), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.effective_cyberstrain", CyberstrainManager.getEffectiveCyberstrain(player, inventory)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.psychosis_state", CyberstrainManager.getPsychosisStateLabel(player, inventory)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.psychosis_active", CyberstrainManager.isPsychosisActive(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.psychosis_remaining", CyberstrainManager.getPsychosisSecondsRemaining(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.suppression", CyberstrainManager.getSuppressionAmount(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.overdose", CyberConsumableManager.getOverdosePoints(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.live_slot_count", inventory.getSlots()), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.stored_tag_empty", storedTag.isEmpty()), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.stored_tag", storedTag), false);

        boolean anyInstalled = false;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            if (slot.ordinal() >= inventory.getSlots()) {
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.slot_missing", slot.name()), false);
                continue;
            }

            ItemStack stack = inventory.getStackInSlot(slot.ordinal());
            if (stack.getItem() instanceof CyberwareItem cyberwareItem) {
                anyInstalled = true;
                de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(
                        "command.cyberneticenhancements.debug.slot_installed",
                        slot.name(),
                        stack.getHoverName(),
                        stack.getCount()
                ), false);
            }
        }

        if (!anyInstalled) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.no_installed"), false);
        }

        return 1;
    }

    private static int resetCooldowns(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CyberConsumableManager.clearCooldowns(player);
        CyberwareAbilities.clearCooldowns(player);
        ArmCyberwareManager.clearCooldowns(player);
        CirculatoryCyberwareManager.clearCooldowns(player);
        FaceCyberwareManager.clearCooldowns(player);
        FrontalCortexManager.clearCooldowns(player);
        HandsCyberwareManager.clearCooldowns(player);
        IntegumentaryCyberwareManager.clearCooldowns(player);
        SkeletonCyberwareManager.clearCooldowns(player);
        NervousSystemCyberwareManager.clearCooldowns(player);
        LegCyberwareManager.clearCooldowns(player);
        CyberwareEffects.refreshPlayerCyberware(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.reset_cooldowns"), false);
        return 1;
    }

    private static int resetBuffs(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TemporaryCyberwareEffectManager.clearEffects(player);
        CyberstrainManager.clearTemporaryStatuses(player);
        CombatStatusManager.clearStatuses(player);
        CyberwareEffects.refreshPlayerCyberware(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.reset_buffs"), false);
        return 1;
    }

    private static int resetAll(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CyberConsumableManager.clearCooldowns(player);
        CyberwareAbilities.clearCooldowns(player);
        ArmCyberwareManager.clearCooldowns(player);
        CirculatoryCyberwareManager.clearCooldowns(player);
        FaceCyberwareManager.clearCooldowns(player);
        FrontalCortexManager.clearCooldowns(player);
        HandsCyberwareManager.clearCooldowns(player);
        IntegumentaryCyberwareManager.clearCooldowns(player);
        SkeletonCyberwareManager.clearCooldowns(player);
        NervousSystemCyberwareManager.clearCooldowns(player);
        LegCyberwareManager.clearCooldowns(player);
        TemporaryCyberwareEffectManager.clearEffects(player);
        CyberstrainManager.clearTemporaryStatuses(player);
        CombatStatusManager.clearStatuses(player);
        CyberwareEffects.refreshPlayerCyberware(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.reset_all"), false);
        return 1;
    }

    private static int applyCombatStatus(
            CommandSourceStack source,
            String rawType,
            int stacks,
            int seconds,
            boolean playerApplied
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CombatStatusType type = parseCombatStatusType(rawType);
        if (type == null) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable("command.cyberneticenhancements.debug.unknown_status", rawType));
            return 0;
        }

        CombatStatusManager.applyStatus(player, type, seconds * 20L, stacks, playerApplied);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.debug.applied_status", type.id(), stacks, seconds, playerApplied ? "player" : "ambient"), false);
        return 1;
    }

    private static CombatStatusType parseCombatStatusType(String rawType) {
        String normalized = rawType.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT);
        try {
            return CombatStatusType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
