package de.artemis.cyberneticenhancements.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenPanoramaAccess {
    @Accessor("PANORAMA")
    static PanoramaRenderer cyberneticenhancements$panorama() { throw new AssertionError(); }
}
