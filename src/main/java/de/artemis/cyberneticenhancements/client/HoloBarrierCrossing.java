package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.block.HoloBarrierBlock;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** The scan that sounds when the player steps through a holographic barrier. */
public final class HoloBarrierCrossing {
    private static boolean inside;

    private HoloBarrierCrossing() {}

    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) { inside = false; return; }
        boolean crossing = HoloBarrierBlock.isCrossing(player.level(), player.getBoundingBox());
        if (crossing && !inside) player.level().playLocalSound(player.getX(), player.getY() + 1, player.getZ(),
                ModSounds.BARRIER_SCAN.get(), SoundSource.BLOCKS, .6F, 1.0F, false);
        inside = crossing;
    }
}
