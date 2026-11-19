package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import de.artemis.cyberneticenhancements.client.ui.NeuralLoadingPresentation;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Styles only the Minecraft-stage handoff. The early loader window is untouched. */
@Mixin(NeoForgeLoadingOverlay.class)
abstract class NeuralStartupOverlayMixin {
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Unique private Matrix4f cyberneticenhancements$projection;
    @Unique private long cyberneticenhancements$openedAt;
    @Unique private boolean cyberneticenhancements$themed;

    @Inject(method = "render", at = @At("HEAD"))
    private void cyberneticenhancements$begin(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        cyberneticenhancements$themed = ((Object) this).getClass() == NeoForgeLoadingOverlay.class && NeuralLoadingPresentation.enabled();
        if (cyberneticenhancements$themed) {
            cyberneticenhancements$projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            if (cyberneticenhancements$openedAt == 0) cyberneticenhancements$openedAt = Util.getMillis();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/earlydisplay/DisplayWindow;render(I)V"))
    private void cyberneticenhancements$oldWindow(DisplayWindow window, int alpha) {
        if (!cyberneticenhancements$themed) window.render(alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V"))
    private void cyberneticenhancements$clear(float red, float green, float blue, float alpha) {
        if (cyberneticenhancements$themed) GlStateManager._clearColor(9 / 255.0F, 4 / 255.0F, 8 / 255.0F, 1);
        else GlStateManager._clearColor(red, green, blue, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void cyberneticenhancements$oldFrame(MeshData mesh) {
        if (cyberneticenhancements$themed) mesh.close();
        else BufferUploader.drawWithShader(mesh);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cyberneticenhancements$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!cyberneticenhancements$themed) return;
        RenderSystem.setProjectionMatrix(cyberneticenhancements$projection, VertexSorting.ORTHOGRAPHIC_Z);
        // The loader resets the cached model-view matrix to identity. GUI vertices
        // need GameRenderer's Z translation as well as its projection to be visible.
        RenderSystem.applyModelViewMatrix();
        long now = Util.getMillis();
        NeuralLoadingPresentation.render(graphics, fadeOutStart >= 0 ? 1 : currentProgress,
                NeuralLoadingPresentation.opacity(now, fadeOutStart, -1, false), (now - cyberneticenhancements$openedAt) / 1000.0);
        graphics.flush();
    }
}
