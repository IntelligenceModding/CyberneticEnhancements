package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.PsychosisPresentation;
import de.artemis.cyberneticenhancements.client.PsychosisTimerOverlay;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility;
import de.artemis.cyberneticenhancements.client.ui.PsychosisHudFade;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Gui.class)
abstract class PsychosisHudTransitionMixin {
    @WrapMethod(method = "renderCrosshair")
    private void psychosis$fadeCrosshair(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
        float visibility=PsychosisHudFade.alpha(1);
        if(visibility>=.99999F) { original.call(graphics,delta); return; }
        if(visibility<=.001F)return;
        graphics.flush();
        float[] color=RenderSystem.getShaderColor().clone();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0,-(1-visibility)*8,0);
            // Inversion blending ignores source alpha; scaling RGB fades its contribution to zero.
            PsychosisHudFade.withoutFade(() -> RenderSystem.setShaderColor(
                    color[0]*visibility,color[1]*visibility,color[2]*visibility,color[3]));
            original.call(graphics,delta);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            PsychosisHudFade.withoutFade(() -> RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]));
        }
    }

    @WrapMethod(method = "render")
    private void psychosis$fadeHud(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
        float amount = PsychosisPresentation.amount();
        graphics.flush();
        float[] color = RenderSystem.getShaderColor().clone();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, amount * 8, 0);
            PsychosisHudFade.begin(amount);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            // Keep world-dependent blends (vignette, portal, crosshair) on the live scene.
            if (amount < .995F) original.call(graphics, delta);
            graphics.flush();
        } finally {
            PsychosisHudFade.end();
            graphics.pose().popPose();
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        }
        if (GameplayHudVisibility.status()) {
            PsychosisPresentation.renderFrame(graphics);
            graphics.flush();
            PsychosisTimerOverlay.render(graphics);
        }
    }
}
