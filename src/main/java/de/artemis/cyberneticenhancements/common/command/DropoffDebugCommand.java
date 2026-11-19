package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload;
import de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.function.BiFunction;

public final class DropoffDebugCommand {
    private DropoffDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var command = Commands.literal("dropoff").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (String action : new String[]{"status", "give", "test", "deliver", "read", "reset"}) {
            command.then(forPlayer(Commands.literal(action), (source, player) -> run(source, player, action, 5)));
        }
        command.then(Commands.literal("queue").then(Commands.argument("seconds", IntegerArgumentType.integer(0, 1200))
                .executes(context -> run(context.getSource(), context.getSource().getPlayerOrException(), "test", IntegerArgumentType.getInteger(context, "seconds")))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> run(context.getSource(), EntityArgument.getPlayer(context, "player"), "test", IntegerArgumentType.getInteger(context, "seconds"))))));
        dispatcher.register(Commands.literal("cyberneticenhancements").then(Commands.literal("dev").then(command)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> forPlayer(LiteralArgumentBuilder<CommandSourceStack> command,
                                                                       BiFunction<CommandSourceStack, ServerPlayer, Integer> action) {
        return command.executes(context -> action.apply(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> action.apply(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private static int run(CommandSourceStack source, ServerPlayer player, String action, int seconds) {
        var deliveries = QuestRewardDeliveries.get(player.server);
        String result;
        switch (action) {
            case "give" -> {
                if (!deliveries.claimSetupItem(player)) {
                    de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.literal("Make room in " + player.getGameProfile().getName() + "'s inventory first."));
                    return 0;
                }
                result = "Starter claim processed. Already claimed or completed setup does not grant another barrel; use reset to test onboarding again.";
            }
            case "test" -> {
                deliveries.queueDebugDelivery(player, seconds);
                result = "Queued 3 emeralds for delivery in " + seconds + " seconds. Uses normal storage, hologram, sound and client-configured arrival chat.";
            }
            case "deliver" -> {
                deliveries.deliverNow(player);
                result = "Pending rewards made due now. Remaining parcels: " + deliveries.pendingParcels(player.getUUID()) + ". Missing, unloaded or full barrels retain their deliveries.";
            }
            case "reset" -> {
                deliveries.resetSetup(player.getUUID());
                result = "Setup, registration and starter claim reset. Queued rewards and existing barrel contents are preserved. Open Quests to test the claim popup; opening an owned barrel registers it again.";
            }
            case "read" -> {
                var point = deliveries.registeredPoint(player.getUUID());
                var level = point == null ? null : player.server.getLevel(point.dimension());
                if (level == null || !level.hasChunkAt(point.pos()) || !(level.getBlockEntity(point.pos()) instanceof DropoffPointBlockEntity barrel)
                        || !player.getUUID().equals(barrel.owner())) {
                    de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.failure(source, Component.literal("No loaded, registered Dropoff Point for this player."));
                    return 0;
                }
                barrel.acknowledgeDelivery();
                result = "Unread delivery marker cleared. Stored items are unchanged.";
            }
            default -> {
                var point = deliveries.registeredPoint(player.getUUID());
                var level = point == null ? null : player.server.getLevel(point.dimension());
                String state = point == null ? "unregistered" : level == null || !level.hasChunkAt(point.pos()) ? "unloaded"
                        : level.getBlockEntity(point.pos()) instanceof DropoffPointBlockEntity barrel && player.getUUID().equals(barrel.owner())
                        ? "loaded, unread=" + barrel.hasUnreadDelivery() : "missing or owner mismatch";
                long next = deliveries.nextDeliveryIn(player.getUUID(), player.server.overworld().getGameTime());
                result = "Setup placed=" + deliveries.hasPlacedDropoff(player.getUUID()) + ", starter claimed=" + deliveries.setupItemGiven(player.getUUID())
                        + ", parcels=" + deliveries.pendingParcels(player.getUUID()) + ", next=" + (next < 0 ? "none" : (next + 19) / 20 + "s")
                        + ", barrel=" + state + (point == null ? "" : " at " + point.dimension().location() + " " + point.pos().toShortString());
            }
        }
        PacketDistributor.sendToPlayer(player, ArchiveQuestsPayload.capture(player));
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.success(source, () -> Component.literal("Dropoff [" + player.getGameProfile().getName() + "]: " + result), false);
        return 1;
    }
}
