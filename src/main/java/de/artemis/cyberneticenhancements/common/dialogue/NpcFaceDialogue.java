package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Node;
import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Option;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload.Choice;
import de.artemis.cyberneticenhancements.common.world.RelicCacheIntel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

/**
 * What any city contact says when you look at them. It is the same conversation as on the phone, with the same wording and
 * rules, shown as spoken answers; only what needs a phone (finding or summoning them) is left out, and the shop and services
 * windows stay one answer away.
 */
public final class NpcFaceDialogue {
    private final AbstractCityNpcEntity npc;
    private final NpcCallConversation conversation;

    public NpcFaceDialogue(AbstractCityNpcEntity npc) {
        this.npc = npc;
        this.conversation = new NpcCallConversation(npc);
    }

    public Node greet(ServerPlayer player) {
        conversation.open(player);
        return current();
    }

    private Node current() {
        var options = new ArrayList<Option>();
        boolean root = false, hasBack = false;
        boolean first = conversation.choices().stream().anyMatch(choice -> choice.action() == LOCATE);
        for (Choice choice : conversation.choices()) {
            int action = choice.action();
            if (action == WORK) root = true;
            if (action == WORK && first && npc.npcType().supportsContracts()) continue; // The board has a window of its own, one key away.
            if (action == LOCATE || action == MEET || action == KEEPALIVE || action == TALK_IN_PERSON) continue;
            if (action == END) continue;
            if (action == BACK) hasBack = true;
            options.add(Option.of(choice.label(), player -> answer(player, action)));
        }
        if (root && npc.npcType().supportsIntel()) options.add(Option.of(Component.translatable("screen.cyberneticenhancements.npc.dialogue.choice.cache"), player -> {
            boolean found = RelicCacheIntel.provide(player);
            return Node.say(npc.getName(), Component.translatable(found
                    ? "screen.cyberneticenhancements.npc.response." + npc.npcType().id() + ".cache_intel"
                    : "message.cyberneticenhancements.cache_intel.none"));
        }));
        if (root && SkillChecks.offeredBy(npc)) options.add(SkillChecks.entry(npc, this::current));
        options.add(Option.of(Component.translatable("dialogue.cyberneticenhancements.leave"), player -> null));
        // Right click: a step back, or from the first question, away.
        return new Node(npc.getName(), conversation.line(), options, hasBack ? player -> answer(player, BACK) : player -> null);
    }

    private Node answer(ServerPlayer player, int action) {
        if (!npc.canTakeCall(player)) return null;
        conversation.respond(player, action);
        return conversation.ended() ? Node.say(npc.getName(), conversation.line()) : current();
    }
}
