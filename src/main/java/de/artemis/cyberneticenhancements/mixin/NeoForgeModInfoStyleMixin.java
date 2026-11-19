package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.Tesselator;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollPanel.class, remap = false)
abstract class NeoForgeModInfoStyleMixin {
    @Shadow @Final protected int left;
    @Shadow @Final protected int top;
    @Shadow @Final protected int width;
    @Shadow @Final protected int height;
    @Shadow @Final protected int border;
    @Shadow @Final private int barLeft;
    @Shadow @Final private int barWidth;
    @Shadow protected float scrollDistance;
    @Shadow protected abstract int getContentHeight();

    @Unique private boolean cyberneticenhancements$styleModInfo() {
        return VanillaMenuPresentation.enabled()
                && Minecraft.getInstance().screen.getClass() == ModListScreen.class
                && getClass().getName().equals("net.neoforged.neoforge.client.gui.ModListScreen$InfoPanel");
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$detailsPanel(GuiGraphics g, Tesselator tess, float delta, CallbackInfo ci) {
        if (!cyberneticenhancements$styleModInfo()) return;
        g.fill(left, top, left + width, top + height, 0xE01C0810);
        UiRenderer.outline(g, left, top, width, height, VanillaMenuPresentation.PANEL_EDGE);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cyberneticenhancements$detailsScrollbar(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        if (!cyberneticenhancements$styleModInfo()) return;
        // Match NeoForge's exact thumb geometry so scrolling and dragging retain
        // their native behavior. Only repaint the existing track and handle.
        int content = getContentHeight();
        int extra = content + border - height;
        if (extra <= 0) return;
        int thumb = Math.min(Math.max(32, height * height / content), height - border * 2);
        int y = Math.max(top, (int) scrollDistance * (height - thumb) / extra + top);
        VanillaMenuPresentation.scroll(g, barLeft, top, barWidth, height, false);
        VanillaMenuPresentation.scroll(g, barLeft, y, barWidth, thumb, true);
        g.flush();
    }
}
