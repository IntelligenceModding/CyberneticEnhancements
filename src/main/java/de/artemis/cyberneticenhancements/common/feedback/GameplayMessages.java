package de.artemis.cyberneticenhancements.common.feedback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import java.util.function.Supplier;

/** Mod-owned chat notifications only. Player chat, vanilla messages and welcome notices bypass this helper. */
public final class GameplayMessages {
    public static final int ACCENT = 0x82BACB, TEXT = 0xD7E6EA;
    private static final String FORMAT = "message.cyberneticenhancements.gameplay";
    private static final String LAST_SOUND = "cyberneticenhancements.last_chat_notice_sound";
    private GameplayMessages() {}

    public static Component format(Component message) {
        if (message.getContents() instanceof TranslatableContents translation && translation.getKey().equals(FORMAT)) return message;
        return Component.translatable(FORMAT, body(message)).withColor(ACCENT);
    }

    private static MutableComponent body(Component message) {
        MutableComponent copy;
        if (message.getContents() instanceof TranslatableContents translation) {
            Object[] args = translation.getArgs().clone();
            for (int i = 0; i < args.length; i++) if (args[i] instanceof Component component) args[i] = body(component);
            copy = Component.translatableWithFallback(translation.getKey(), translation.getFallback(), args);
        } else copy = MutableComponent.create(message.getContents());
        copy.setStyle(message.getStyle().withColor(TEXT));
        for (var sibling : message.getSiblings()) copy.append(body(sibling));
        return copy;
    }

    public static void chat(Player player, Component message) {
        player.displayClientMessage(format(message), false);
        sound(player);
    }

    public static void chat(Player player, Component message, PlayerActionFeedback action) {
        player.displayClientMessage(format(message), false);
        player.getPersistentData().putLong(LAST_SOUND, player.level().getGameTime());
        action.play(player);
    }

    public static void success(CommandSourceStack source, Supplier<Component> message, boolean broadcast) {
        source.sendSuccess(() -> format(message.get()), broadcast);
        if (source.getEntity() instanceof Player player && source.getServer().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_SENDCOMMANDFEEDBACK)) sound(player);
    }

    public static void failure(CommandSourceStack source, Component message) {
        source.sendFailure(format(message));
        if (source.getEntity() instanceof Player player) sound(player);
    }

    private static void sound(Player player) {
        if (player instanceof ServerPlayer server && server.connection == null) return;
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.contains(LAST_SOUND)) {
            long last = data.getLong(LAST_SOUND);
            if (now >= last && now - last < 20) return;
        }
        data.putLong(LAST_SOUND, now);
        if (player instanceof ServerPlayer server)
            server.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, .12F, 1.3F);
        else player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), .12F, 1.3F);
    }
}
