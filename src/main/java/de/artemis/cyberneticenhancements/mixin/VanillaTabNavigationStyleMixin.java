package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TabNavigationBar.class)
abstract class VanillaTabNavigationStyleMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), require = 2)
    private void cyberneticenhancements$tabDividers(GuiGraphics g, ResourceLocation texture, int x, int y,
            float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
        if (VanillaMenuPresentation.enabled()) VanillaMenuPresentation.menuDivider(g, x, y, width);
        else original.call(g, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}
