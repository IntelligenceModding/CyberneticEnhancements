package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import de.artemis.cyberneticenhancements.client.ui.StatusEffectHudLayout;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class StatusEffectHudLayoutMixin {
    @Inject(method = "renderEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;isAmbient()Z"))
    private void cyberneticenhancements$stackEffects(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci,
                                                     @Local(ordinal = 0) int beneficialCount, @Local(ordinal = 1) int otherCount,
                                                     @Local(ordinal = 2) LocalIntRef x, @Local(ordinal = 3) LocalIntRef y) {
        // Vanilla has counted this visible effect and will use these coordinates for both its background and icon.
        int index = beneficialCount + otherCount - 1;
        boolean demo = Minecraft.getInstance().isDemo();
        int rows = StatusEffectHudLayout.rows(graphics.guiHeight(), demo);
        x.set(StatusEffectHudLayout.x(graphics.guiWidth(), index, rows));
        y.set(StatusEffectHudLayout.y(index, rows, demo));
    }
}
