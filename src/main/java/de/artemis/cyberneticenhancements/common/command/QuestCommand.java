package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class QuestCommand {
    private QuestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cyberneticenhancements")
                        .requires(source -> source.hasPermission(Commands.LEVEL_ALL))
                        .then(Commands.literal("quests")
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> clearAll(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("questId", StringArgumentType.greedyString())
                                                        .executes(context -> removeById(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "questId")))))))
        );
    }

    private static int clearAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int affectedPlayers = 0;
        int removedEntries = 0;
        for (ServerPlayer target : targets) {
            int removed = PlayerQuestManager.clearAllQuestData(target);
            if (removed > 0) {
                affectedPlayers++;
                removedEntries += removed;
            }
        }
        sendResult(source, "command.cyberneticenhancements.quests.clear", targets, affectedPlayers, removedEntries, null);
        return removedEntries;
    }

    private static int removeById(CommandSourceStack source, Collection<ServerPlayer> targets, String questId) {
        int affectedPlayers = 0;
        int removedEntries = 0;
        for (ServerPlayer target : targets) {
            int removed = PlayerQuestManager.removeQuestEntryById(target, questId);
            if (removed > 0) {
                affectedPlayers++;
                removedEntries += removed;
            }
        }
        sendResult(source, "command.cyberneticenhancements.quests.remove", targets, affectedPlayers, removedEntries, questId);
        return removedEntries;
    }

    private static void sendResult(CommandSourceStack source, String keyBase, Collection<ServerPlayer> targets, int affectedPlayers, int removedEntries, String questId) {
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> questId == null
                    ? Component.translatable(keyBase + ".single", target.getDisplayName(), removedEntries)
                    : Component.translatable(keyBase + ".single", questId, target.getDisplayName(), removedEntries), true);
            return;
        }
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> questId == null
                ? Component.translatable(keyBase + ".multi", targets.size(), removedEntries)
                : Component.translatable(keyBase + ".multi", questId, targets.size(), removedEntries), true);
    }
}
