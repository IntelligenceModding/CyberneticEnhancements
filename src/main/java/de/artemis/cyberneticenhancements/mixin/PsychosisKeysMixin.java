package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class PsychosisKeysMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void psychosis$blockScreens(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        var mc=Minecraft.getInstance();
        if (!CyberpsychosisClientState.isControlLocked() || mc.player==null || !mc.player.isAlive() || screen==null) return;
        if (screen instanceof de.artemis.cyberneticenhancements.client.ui.PsychosisClosingScreen) return;
        if (screen instanceof net.minecraft.client.gui.screens.PauseScreen || screen instanceof net.minecraft.client.gui.screens.DeathScreen
                || screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen || screen instanceof net.minecraft.client.gui.screens.TitleScreen) return;
        ci.cancel();
        if (mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
    }
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void psychosis$lockActions(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (!CyberpsychosisClientState.isControlLocked() || mc.player == null || mc.screen != null) return;
        for (var key : mc.options.keyMappings) while (key.consumeClick()) { }
        ci.cancel();
    }
}
