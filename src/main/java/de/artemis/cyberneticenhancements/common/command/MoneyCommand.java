package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class MoneyCommand {
    private MoneyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cyberneticenhancements")
                        .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.literal("money")
                                .then(Commands.literal("balance")
                                        .executes(context -> balanceSelf(context.getSource()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> balanceTarget(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(context -> setBalance(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                IntegerArgumentType.getInteger(context, "amount"))))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(context -> addBalance(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                IntegerArgumentType.getInteger(context, "amount"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(context -> removeBalance(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                IntegerArgumentType.getInteger(context, "amount"))))))
                                .then(Commands.literal("change")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(-999_999_999, 999_999_999))
                                                        .executes(context -> changeBalance(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                IntegerArgumentType.getInteger(context, "amount")))))))
        );
    }

    private static int balanceSelf(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return balanceTarget(source, source.getPlayerOrException());
    }

    private static int balanceTarget(CommandSourceStack source, ServerPlayer target) {
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(
                "command.cyberneticenhancements.money.balance",
                target.getDisplayName(),
                PlayerEurodollarManager.balance(target)
        ), false);
        return 1;
    }

    private static int setBalance(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer target : targets) {
            PlayerEurodollarManager.setBalance(target, amount, "admin_set", "Administrative balance set", null, null);
        }
        sendOperationFeedback(source, "command.cyberneticenhancements.money.set", targets, amount);
        return targets.size();
    }

    private static int addBalance(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer target : targets) {
            PlayerEurodollarManager.add(target, amount, "admin_add", "Administrative balance credit", null, null);
        }
        sendOperationFeedback(source, "command.cyberneticenhancements.money.add", targets, amount);
        return targets.size();
    }

    private static int removeBalance(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer target : targets) {
            PlayerEurodollarManager.remove(target, amount, "admin_remove", "Administrative balance debit", null, null);
        }
        sendOperationFeedback(source, "command.cyberneticenhancements.money.remove", targets, amount);
        return targets.size();
    }

    private static int changeBalance(CommandSourceStack source, Collection<ServerPlayer> targets, int delta) {
        for (ServerPlayer target : targets) {
            PlayerEurodollarManager.change(target, delta, "admin_change", "Administrative balance adjustment", null, null);
        }
        sendOperationFeedback(source, "command.cyberneticenhancements.money.change", targets, delta);
        return targets.size();
    }

    private static void sendOperationFeedback(CommandSourceStack source, String keyBase, Collection<ServerPlayer> targets, int amount) {
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(
                    keyBase + ".single",
                    amount,
                    target.getDisplayName(),
                    PlayerEurodollarManager.balance(target)
            ), true);
            return;
        }
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.translatable(
                keyBase + ".multi",
                amount,
                targets.size()
        ), true);
    }
}
