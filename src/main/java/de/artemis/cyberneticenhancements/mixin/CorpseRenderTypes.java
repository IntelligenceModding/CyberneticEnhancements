package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Preserve each skin/armor texture while enabling the shared hologram alpha. */
public abstract class CorpseRenderTypes extends RenderType {
    private CorpseRenderTypes() {
        super("corpse_texture_access", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, () -> {}, () -> {});
    }

    @Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
    public interface CompositeAccess {
        @Accessor("state") RenderType.CompositeState cyberneticenhancements$state();
    }

    @Mixin(RenderType.CompositeState.class)
    public interface StateAccess {
        @Accessor("textureState") RenderStateShard.EmptyTextureStateShard cyberneticenhancements$texture();
    }

    @Mixin(RenderStateShard.EmptyTextureStateShard.class)
    public interface TextureAccess {
        @Invoker("cutoutTexture") Optional<ResourceLocation> cyberneticenhancements$location();
    }
}
