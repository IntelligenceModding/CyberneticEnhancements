package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

public final class HologramRenderTypes extends RenderType {
    private static ShaderInstance playerShader;
    private static final Function<ResourceLocation, RenderType> PLAYER = Util.memoize(texture -> create(
            "cyberneticenhancements_hologram_player", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            1536, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> playerShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)));
    public static final RenderType PROJECTION = create(
            "cyberneticenhancements_hologram_projection", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            1536, false, true, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false));

    private HologramRenderTypes() {
        super("cyberneticenhancements_hologram", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, true, () -> {}, () -> {});
    }

    public static RenderType player(ResourceLocation texture) {
        return PLAYER.apply(texture);
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new HologramShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "ripper_hologram"),
                            DefaultVertexFormat.NEW_ENTITY),
                    shader -> playerShader = shader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load the ripper station hologram shader", exception);
        }
    }
}
