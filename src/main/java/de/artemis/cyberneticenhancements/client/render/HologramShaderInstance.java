package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

final class HologramShaderInstance extends ShaderInstance {
    HologramShaderInstance(ResourceProvider resources, ResourceLocation name, VertexFormat format) throws IOException {
        super(resources, name, format);
    }

    @SuppressWarnings("unused")
    public boolean iris$shouldSkipThis() {

        return ShaderPackCompatibility.isRenderingShadowPass();
    }
}
