package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.FlatlinedDeathCounter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class FlatlinedDeathStatsMixin {
    @Inject(method = "handleAwardStats", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$deathRecord(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        var deaths = Stats.CUSTOM.get(Stats.DEATHS);
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) FlatlinedDeathCounter.update(player.getStats().getValue(deaths));
    }
}
