package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.CompositeAccess;
import de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.StateAccess;
import de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes.TextureAccess;
import net.minecraft.client.renderer.RenderType;

/** Keep skin and armor textures while permitting holographic transparency. */
final class CorpseRenderTypes extends RenderType {
    private CorpseRenderTypes() {
        super("corpse_texture_access", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, () -> {}, () -> {});
    }

    static RenderType translucent(RenderType original) {
        if (!(original instanceof CompositeAccess composite)
                || original.format() != DefaultVertexFormat.NEW_ENTITY && original.format() != DefaultVertexFormat.BLOCK) return original;
        var state = (StateAccess) (Object) composite.cyberneticenhancements$state();
        var texture = (TextureAccess) (Object) state.cyberneticenhancements$texture();
        return texture.cyberneticenhancements$location().map(RenderType::entityTranslucent).orElse(original);
    }
}
