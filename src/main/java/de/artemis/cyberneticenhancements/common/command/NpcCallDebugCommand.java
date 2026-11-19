package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager;
import de.artemis.cyberneticenhancements.common.dialogue.NpcCallReason;
import de.artemis.cyberneticenhancements.common.dialogue.NpcIncomingCalls;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.BiFunction;
import java.util.concurrent.CompletableFuture;
import java.util.Locale;

public final class NpcCallDebugCommand {
    private static final String PREFIX = "command.cyberneticenhancements.calls.";

    private NpcCallDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var npc = Commands.argument("npc", EntityArgument.entity())
                .suggests(NpcCallDebugCommand::suggestContacts)
                .executes(context -> force(context.getSource(), EntityArgument.getEntity(context, "npc"), NpcCallReason.CHECK_IN, context.getSource().getPlayerOrException()));
        for (var reason : NpcCallReason.values()) {
            npc.then(Commands.literal(reason.id())
                    .executes(context -> force(context.getSource(), EntityArgument.getEntity(context, "npc"), reason, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> force(context.getSource(), EntityArgument.getEntity(context, "npc"), reason, EntityArgument.getPlayer(context, "player")))));
        }
        var calls = Commands.literal("calls").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("force").then(npc))
                .then(forPlayer(Commands.literal("check"), NpcCallDebugCommand::check))
                .then(forPlayer(Commands.literal("reset"), (source, player) -> {
                    NpcIncomingCalls.resetCooldowns(player);
                    de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(PREFIX + "reset", player.getName()), false);
                    return 1;
                }))
                .then(forPlayer(Commands.literal("status"), (source, player) -> {
                    de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(PREFIX + "status", player.getName(), NpcIncomingCalls.playTicks(player) / 1200,
                            (NpcIncomingCalls.cooldownRemaining(player) + 1199) / 1200, NpcCallManager.hasCall(player.getUUID())), false);
                    return 1;
                }))
                .then(Commands.literal("agequest")
                        .then(Commands.argument("npc", EntityArgument.entity())
                                .suggests(NpcCallDebugCommand::suggestContacts)
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(0, 100000))
                                        .executes(context -> age(context.getSource(), EntityArgument.getEntity(context, "npc"), context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "minutes")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> age(context.getSource(), EntityArgument.getEntity(context, "npc"), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "minutes")))))));
        dispatcher.register(Commands.literal("cyberneticenhancements").then(Commands.literal("dev").then(calls)));
    }

    private static CompletableFuture<Suggestions> suggestContacts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var player = context.getSource().getPlayer();
        if (player == null || builder.getRemaining().startsWith("@")) {
            return EntityArgument.entity().listSuggestions(context, builder);
        }
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (var contact : AbstractCityNpcEntity.archiveContacts(player)) {
            if (contact.deceased()) continue;
            String id = contact.npcId().toString();
            if (id.startsWith(prefix)) builder.suggest(id, Component.literal(contact.name()));
        }
        return builder.buildFuture();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> forPlayer(LiteralArgumentBuilder<CommandSourceStack> command,
                                                                       BiFunction<CommandSourceStack, ServerPlayer, Integer> action) {
        return command.executes(context -> action.apply(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> action.apply(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private static int force(CommandSourceStack source, Entity entity, NpcCallReason reason, ServerPlayer player) {
        if (!(entity instanceof AbstractCityNpcEntity npc)) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable(PREFIX + "not_npc"));
            return 0;
        }
        if (!NpcCallManager.startIncoming(player, npc, reason, true)) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable(PREFIX + "unavailable"));
            return 0;
        }
        NpcIncomingCalls.record(player, npc, reason);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(PREFIX + "forced", npc.getName(), player.getName(), reason.id()), false);
        return 1;
    }

    private static int check(CommandSourceStack source, ServerPlayer player) {
        boolean started = NpcIncomingCalls.evaluate(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(PREFIX + (started ? "started" : "no_call"), player.getName()), false);
        return started ? 1 : 0;
    }

    private static int age(CommandSourceStack source, Entity entity, ServerPlayer player, int minutes) {
        if (!(entity instanceof AbstractCityNpcEntity npc)) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable(PREFIX + "not_npc"));
            return 0;
        }
        if (!PlayerQuestManager.setAcceptedPlayTicks(player, npc, NpcIncomingCalls.playTicks(player) - minutes * 1200L)) {
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.translatable(PREFIX + "no_quest"));
            return 0;
        }
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(PREFIX + "aged", npc.getName(), minutes), false);
        return 1;
    }
}
