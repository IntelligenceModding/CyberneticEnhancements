package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.world.StreetEncounters;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Answers a street shakedown, and lets operators trigger either street encounter on demand. */
public final class EncounterCommand {
    private EncounterCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var encounter = Commands.literal("encounter");
        encounter.then(Commands.literal("pay").requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                .executes(context -> answer(context.getSource(), true)));
        encounter.then(Commands.literal("refuse").requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                .executes(context -> answer(context.getSource(), false)));
        encounter.then(Commands.literal("trigger").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ripperdoc").executes(context -> trigger(context.getSource(), true)))
                .then(Commands.literal("shakedown").executes(context -> trigger(context.getSource(), false))));
        dispatcher.register(Commands.literal("cyberneticenhancements").then(encounter));
    }

    private static int answer(CommandSourceStack source, boolean pay) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (pay ? StreetEncounters.pay(player) : StreetEncounters.refuse(player)) return 1;
        GameplayMessages.failure(source, Component.translatable("message.cyberneticenhancements.encounter.none"));
        return 0;
    }

    private static int trigger(CommandSourceStack source, boolean ripperdoc) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (ripperdoc ? StreetEncounters.startRipperdoc(player) : StreetEncounters.startShakedown(player)) return 1;
        GameplayMessages.failure(source, Component.translatable("message.cyberneticenhancements.encounter.no_room"));
        return 0;
    }
}
