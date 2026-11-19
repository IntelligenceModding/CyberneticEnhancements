package de.artemis.cyberneticenhancements.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Accepts real contracts through the normal board, including asynchronous terrain preparation. */
public final class QuestAcceptCommand {
    private static final Map<MinecraftServer, List<Pending>> PENDING = new WeakHashMap<>();

    private record Pending(UUID player, DevTestEntity issuer, QuestType type, boolean survey, long deadline) {}

    private QuestAcceptCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var accept = Commands.literal("accept").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (QuestType type : QuestType.values()) {
            accept.then(Commands.literal(type.id()).executes(context -> spawn(context.getSource(), type, false)));
        }
        accept.then(Commands.literal("chromium_survey").executes(context -> spawn(context.getSource(), QuestType.INVESTIGATION, true)));
        accept.then(Commands.literal("all").executes(context -> spawn(context.getSource(), null, false)));
        dispatcher.register(Commands.literal("cyberneticenhancements").then(Commands.literal("quests").then(accept)));
    }

    private static int spawn(CommandSourceStack source, QuestType selected, boolean survey) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var requests = PENDING.computeIfAbsent(source.getServer(), ignored -> new ArrayList<>());
        int spawned = 0;
        for (int index = 0; index < PlayerQuestManager.DEV_OFFER_COUNT; index++) {
            boolean chromium = index == QuestType.values().length;
            QuestType type = chromium ? QuestType.INVESTIGATION : QuestType.values()[index];
            if (selected != null && (type != selected || chromium != survey)) continue;
            var npc = ModEntityTypes.DEV_TEST.get().create(player.serverLevel());
            if (npc == null) continue;
            npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            npc.setPersistenceRequired();
            npc.setCustomName(Component.literal("Dev-Test · " + (chromium ? "chromium_survey" : type.id()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            if (!player.serverLevel().addFreshEntity(npc)) { npc.discard(); continue; }
            npc.setHome(player.blockPosition());
            npc.ensureIdentity();
            npc.markMet(player);
            var request = new Pending(player.getUUID(), npc, type, chromium, source.getServer().overworld().getGameTime() + 2400);
            requests.add(request);
            spawned++;
        }
        if (spawned == 0) GameplayMessages.failure(source, Component.literal("Could not spawn a Dev NPC."));
        else GameplayMessages.success(source, () -> Component.literal("Preparing quest terrain. Contracts will be accepted automatically."), false);
        return spawned;
    }

    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var requests = PENDING.get(server);
        if (requests == null || server.overworld().getGameTime() % 20 != 0) return;
        requests.removeIf(request -> process(server, request));
        if (requests.isEmpty()) PENDING.remove(server);
    }

    private static boolean process(MinecraftServer server, Pending request) {
        var player = server.getPlayerList().getPlayer(request.player());
        var npc = request.issuer();
        boolean expired = server.overworld().getGameTime() >= request.deadline();
        if (player == null || !npc.isAlive() || expired || player.serverLevel() != npc.level() || !npc.canInteract(player)) {
            if (player != null) GameplayMessages.failure(player.createCommandSourceStack(),
                    Component.literal("Quest preparation cancelled for " + (request.survey() ? "chromium_survey" : request.type().id()) + ". Run the command again."));
            npc.discard();
            return true;
        }
        var result = PlayerQuestManager.prepareAndAcceptDevQuest(player, npc, request.type(), request.survey());
        return result == PlayerQuestManager.Result.OK || result == PlayerQuestManager.Result.HAS_ACTIVE;
    }

    public static void stopped(ServerStoppedEvent event) {
        PENDING.remove(event.getServer());
    }
}
