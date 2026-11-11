package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import net.minecraft.Util;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;

public final class RipperStationHighlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ThreadLocal<Selection> SELECTION = new ThreadLocal<>();
    private static ShaderInstance shader;

    private RipperStationHighlightLayer(PlayerRenderer renderer) {
        super(renderer);
    }

    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new RipperStationHighlightLayer(renderer));
            }
        }
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "ripper_highlight"),
                            DefaultVertexFormat.NEW_ENTITY),
                    loaded -> shader = loaded);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load the ripper station body highlight shader", exception);
        }
    }

    public static void renderWithSelection(AbstractClientPlayer player, CyberwareSlot slot, Runnable render) {
        Selection previous = SELECTION.get();
        SELECTION.set(new Selection(player, slot));
        try {
            render.run();
        } finally {
            if (previous == null) {
                SELECTION.remove();
            } else {
                SELECTION.set(previous);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Selection selection = SELECTION.get();
        if (selection == null || selection.player() != player || selection.slot() == null || shader == null) {
            return;
        }
        var texture = player.getSkin().texture();
        var vertices = buffers.getBuffer(ShaderPackCompatibility.isShaderPackEnabled()
                ? RenderType.entityTranslucentEmissive(texture) : HighlightRenderType.FACTORY.apply(texture));
        for (ModelPart part : selectedParts(getParentModel(), selection.slot())) {
            part.render(poseStack, vertices, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0x708BDAF8);
        }
    }

    static List<ModelPart> selectedParts(PlayerModel<AbstractClientPlayer> model, CyberwareSlot slot) {
        if (slot == CyberwareSlot.HANDS_1) {
            return List.of(model.rightArm, model.rightSleeve);
        }
        if (slot == CyberwareSlot.HANDS_2) {
            return List.of(model.leftArm, model.leftSleeve);
        }
        return switch (slot.getType()) {
            case FRONTAL_CORTEX, FACE -> List.of(model.head, model.hat);
            case OPERATING_SYSTEM -> List.of(model.body, model.jacket);
            case ARMS, HANDS -> List.of(model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve);
            case LEGS -> List.of(model.rightLeg, model.leftLeg, model.rightPants, model.leftPants);
            case INTEGUMENTARY_SYSTEM -> List.of(model.hat, model.jacket, model.rightSleeve, model.leftSleeve,
                    model.rightPants, model.leftPants);
            case CIRCULATORY_SYSTEM -> List.of(model.body, model.jacket, model.rightArm, model.leftArm,
                    model.rightSleeve, model.leftSleeve, model.rightLeg, model.leftLeg, model.rightPants, model.leftPants);
            case SKELETON, NERVOUS_SYSTEM -> List.of(model.head, model.hat, model.body, model.jacket,
                    model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve,
                    model.rightLeg, model.leftLeg, model.rightPants, model.leftPants);
        };
    }

    private record Selection(AbstractClientPlayer player, CyberwareSlot slot) {
    }

    private static final class HighlightRenderType extends RenderType {
        private static final Function<ResourceLocation, RenderType> FACTORY = Util.memoize(texture -> create(
                "cyberneticenhancements_ripper_highlight", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> shader))
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .createCompositeState(false)));

        private HighlightRenderType() {
            super("cyberneticenhancements_ripper_highlight", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, true, () -> {}, () -> {});
        }
    }
}

