package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.artemis.cyberneticenhancements.client.ClientModEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
abstract class PauseSavingIndicatorMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void cyberneticenhancements$skipCornerSavingIndicator(Gui gui, GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
        if (!ClientModEvents.hasCenteredSavingIndicator(Minecraft.getInstance().screen)) {
            original.call(gui, graphics, delta);
        }
    }
}
