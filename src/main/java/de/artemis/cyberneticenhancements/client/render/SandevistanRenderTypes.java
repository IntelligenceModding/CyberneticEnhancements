package de.artemis.cyberneticenhancements.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class SandevistanRenderTypes {
    private static final ResourceLocation PROJECTION = ResourceLocation.fromNamespaceAndPath(
            "cyberneticenhancements", "textures/effect/sandevistan_echo.png");
    private SandevistanRenderTypes() {}

    public static RenderType echo() {

        return RenderType.entityTranslucent(PROJECTION, false);
    }
}
