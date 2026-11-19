package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CyberpsychosisCommand {
    private CyberpsychosisCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cyberneticenhancements")
                        .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.literal("cyberpsychosis")
                                .then(Commands.literal("trigger")
                                        .executes(context -> trigger(context.getSource(), 15))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120))
                                                .executes(context -> trigger(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))))
                                .then(Commands.literal("clear")
                                        .executes(context -> clear(context.getSource())))
                                .then(Commands.literal("status")
                                        .executes(context -> status(context.getSource()))))
        );
    }

    private static int trigger(CommandSourceStack source, int durationSeconds) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CyberstrainManager.forcePsychosisEpisode(player, durationSeconds);
        return 1;
    }

    private static int clear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean notified = CyberstrainManager.endPsychosisEpisode(player);
        if (!notified) de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.cyberpsychosis.cleared"), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.cyberpsychosis.state", CyberstrainManager.getPsychosisStateLabel(player, inventory)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.cyberpsychosis.active", CyberstrainManager.isPsychosisActive(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.cyberpsychosis.remaining", CyberstrainManager.getPsychosisSecondsRemaining(player)), false);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable("command.cyberneticenhancements.cyberpsychosis.effective", CyberstrainManager.getEffectiveCyberstrain(player, inventory)), false);
        return 1;
    }
}
