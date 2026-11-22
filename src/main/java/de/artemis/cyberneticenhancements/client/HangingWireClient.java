package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import de.artemis.cyberneticenhancements.common.entity.WireSigns;
import de.artemis.cyberneticenhancements.common.network.HangingWireDecoratePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lets the player work on a hanging wire they look at: use a banner or lantern on it to hang it, use an empty hand on a hung item
 * to take it off, and attack the wire to break it (or a hung item to take just that off).
 */
public final class HangingWireClient {
    private HangingWireClient() {}

    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;
        HangingWireEntity.Action action;
        HangingWireAim.Aim aim;
        if (event.isAttack()) {
            aim = HangingWireAim.find(player, HangingWireAim.partialTick());
            if (aim == null) return;
            action = aim.item() ? HangingWireEntity.Action.TAKE : HangingWireEntity.Action.BREAK;
        } else if (event.isUseItem()) {
            boolean hang = HangingWireEntity.canHang(player.getMainHandItem()) || HangingWireEntity.canHang(player.getOffhandItem());
            if (!hang && !player.getMainHandItem().isEmpty() && !WireSigns.isTool(player.getMainHandItem())) return;
            aim = HangingWireAim.find(player, HangingWireAim.partialTick());
            if (aim == null) return;
            var hung = aim.item() ? aim.wire().decorationAt(aim.t(), 0.6) : null;
            // On a hung sign, dye, ink, honeycomb and an axe work it, and an empty hand opens it for writing; on anything else an empty hand takes it off.
            boolean tool = hung != null && WireSigns.isSign(hung.stack()) && WireSigns.isTool(player.getMainHandItem());
            if (hang) action = HangingWireEntity.Action.HANG;
            else if (aim.item() && (player.getMainHandItem().isEmpty() || tool)) action = HangingWireEntity.Action.USE;
            else return;
        } else return;
        event.setSwingHand(false);
        event.setCanceled(true);
        PacketDistributor.sendToServer(new HangingWireDecoratePayload(aim.wire().getId(), aim.t(), action));
    }
}
