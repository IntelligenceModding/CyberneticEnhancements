package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.world.TraumaCoverage;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class TraumaRespawnMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleClientCommand", at = @At("HEAD"))
    private void cyberneticenhancements$abortRescue(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        if (!player.server.isSameThread()) return;
        if (packet.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) TraumaCoverage.abandon(player);
    }
}
