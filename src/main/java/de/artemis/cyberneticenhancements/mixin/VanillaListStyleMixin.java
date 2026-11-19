package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
abstract class VanillaListStyleMixin extends AbstractWidget {
    @Shadow protected abstract boolean isSelectedItem(int index);

    protected VanillaListStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$listPanel(GuiGraphics g, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this)) return;
        g.fill(getX(), getY(), getRight(), getBottom(), 0xB016060E);
        ci.cancel();
    }

    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$listEdges(GuiGraphics g, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this)) return;
        g.fill(getX(), getY() - 1, getRight(), getY(), VanillaMenuPresentation.PANEL_EDGE);
        g.fill(getX(), getBottom(), getRight(), getBottom() + 1, VanillaMenuPresentation.PANEL_EDGE);
        ci.cancel();
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void cyberneticenhancements$entryCard(GuiGraphics g, int mouseX, int mouseY, float delta,
            int index, int x, int y, int width, int height, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this) || !((Object) this instanceof ObjectSelectionList<?>)) return;
        if (isSelectedItem(index)) return; // The native selection pass draws this card.
        boolean highlighted = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        g.fill(x - 2, y - 1, x + width - 2, y + height + 1, highlighted ? 0xE04A1220 : 0xD01C0810);
        UiRenderer.outline(g, x - 2, y - 1, width, height + 2,
                highlighted ? VanillaMenuPresentation.ENTRY_HOVER : VanillaMenuPresentation.ENTRY_EDGE);
    }

    @Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$selection(GuiGraphics g, int y, int width, int height, int outer, int inner, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this)) return;
        int x = getX() + (getWidth() - width) / 2;
        g.fill(x, y - 1, x + width, y + height + 1, 0xFF4A1220);
        UiRenderer.outline(g, x, y - 1, width, height + 2, VanillaMenuPresentation.ENTRY_SELECTED);
        ci.cancel();
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void cyberneticenhancements$scrollbar(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        if (VanillaMenuPresentation.enabled(this)) {
            VanillaMenuPresentation.scroll(g, x, y, width, height, sprite.getPath().equals("widget/scroller"));
        } else g.blitSprite(sprite, x, y, width, height);
    }
}
