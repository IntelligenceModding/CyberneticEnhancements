package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Uses vanilla's self-lit eyes shader, also mapped to the emissive pass by Iris. */
final class EmissiveProjectionRenderTypes extends RenderType {
    private EmissiveProjectionRenderTypes() {
        super("cyberneticenhancements_emissive_projection", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, true, () -> {}, () -> {});
    }

    static RenderType translucent(ResourceLocation texture) {
        // Alpha blending preserves artwork colors. Unlike the translucent-emissive entity
        // shader, the eyes path has no 0.1 alpha cutoff, so faint echoes can fade to zero.
        // Keep normal depth testing and color-only writes: projections do not occlude
        // later transparent geometry or draw through solid walls.
        return EYES.apply(texture, TRANSLUCENT_TRANSPARENCY);
    }
}
