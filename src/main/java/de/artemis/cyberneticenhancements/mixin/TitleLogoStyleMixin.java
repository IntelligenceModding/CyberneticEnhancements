package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.ThreadLocalRandom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
abstract class TitleLogoStyleMixin {
    @Shadow @Final private boolean showEasterEgg;
    @Unique private long cyberneticenhancements$nextGlitch, cyberneticenhancements$glitchEnd;
    @Unique private int cyberneticenhancements$glitchSeed;

    @ModifyArgs(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setColor(FFFF)V", ordinal = 0))
    private void cyberneticenhancements$logoTint(Args args) {
        if (!TitleScreenPresentation.enabled(Minecraft.getInstance().screen)) return;
        args.set(1, 0.40F);
        args.set(2, 0.48F);
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("RETURN"))
    private void cyberneticenhancements$occasionalGlitch(GuiGraphics graphics, int width, float alpha, int top, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        double motion = mc.options.screenEffectScale().get();
        if (!TitleScreenPresentation.enabled(mc.screen) || motion <= 0 || !mc.isWindowActive()) {
            cyberneticenhancements$nextGlitch = cyberneticenhancements$glitchEnd = 0;
            return;
        }
        long now = Util.getMillis();
        var random = ThreadLocalRandom.current();
        if (cyberneticenhancements$nextGlitch == 0) {
            cyberneticenhancements$nextGlitch = now + random.nextLong(3_000, 8_001);
        }
        if (now >= cyberneticenhancements$nextGlitch) {
            cyberneticenhancements$glitchEnd = now + random.nextLong(100, 181);
            cyberneticenhancements$nextGlitch = now + random.nextLong(3_000, 9_001);
            cyberneticenhancements$glitchSeed = random.nextInt(10_000);
        }
        if (now >= cyberneticenhancements$glitchEnd || alpha <= 0.02F) return;
        // Two faint, displaced texture strips, confined to the Minecraft lettering.
        var logo = showEasterEgg ? LogoRenderer.EASTER_EGG_LOGO : LogoRenderer.MINECRAFT_LOGO;
        int frame = (int) (now / 40);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.setColor(1, 0.55F, 0.63F, (float) (0.45 * motion * alpha));
            for (int band = 0; band < 2; band++) {
                int row = Math.floorMod(cyberneticenhancements$glitchSeed + frame * 7 + band * 19, 41);
                int shift = (band == 0 ? -1 : 1) * Math.max(1, (int) Math.round(3 * motion));
                graphics.blit(logo, width / 2 - 128 + shift, top + row, 0, row, 256, 2, 256, 64);
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }
}
