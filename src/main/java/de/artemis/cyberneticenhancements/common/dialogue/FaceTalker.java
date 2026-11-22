package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.network.FaceDialogueStatePayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Someone who starts talking as soon as you look at them, with no screen to open. */
public interface FaceTalker {
    /** False while they are in no mood to talk to this player, such as when they are hostile; an open conversation ends then too. */
    default boolean willTalk(ServerPlayer player) { return true; }

    /** Called every tick while this player is in conversation with them, so they can stand still and listen. */
    default void listening(ServerPlayer player) {}

    /** The type and trust shown in the corner of their box, or {@code null} for someone with neither. */
    default FaceDialogueStatePayload.@Nullable Header header(ServerPlayer player) { return null; }

    /** What they say to this player right now, or {@code null} when they have nothing to say. */
    @Nullable FaceDialogue.Node greet(ServerPlayer player);
}
