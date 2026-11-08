package de.artemis.cyberneticenhancements.client.render;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

public final class FixerEntityRenderer extends HumanoidMobRenderer<AbstractCityNpcEntity, PlayerModel<AbstractCityNpcEntity>> {
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final PlayerModel<AbstractCityNpcEntity> wideModel;
    private final PlayerModel<AbstractCityNpcEntity> slimModel;

    public FixerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.model.setAllVisible(true);
    }

    @Override
    public void render(AbstractCityNpcEntity entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        if (!entity.hasResolvedIdentity()) {
            return;
        }
        model = NpcSkinModels.isSlim(getTextureLocation(entity)) ? slimModel : wideModel;
        model.setAllVisible(true);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractCityNpcEntity entity) {
        if (!entity.appearanceOverride().isBlank()) {
            ResourceLocation override = ResourceLocation.tryParse(entity.appearanceOverride());
            if (override != null && Minecraft.getInstance().getResourceManager().getResource(override).isPresent()) {
                return override;
            }
        }
        return resolveTexture(entity.getUUID(), entity.npcCategory());
    }

    public static ResourceLocation resolveTexture(java.util.UUID entityId, NpcCategory category) {
        List<ResourceLocation> textures = texturesFor(category);
        if (textures.isEmpty()) {
            return FALLBACK_TEXTURE;
        }
        int index = Math.floorMod(entityId.hashCode(), textures.size());
        return textures.get(index);
    }

    private static List<ResourceLocation> texturesFor(NpcCategory category) {
        List<ResourceLocation> categoryTextures = listTextures(NpcIdentityConfig.textureFolder(category));
        if (!categoryTextures.isEmpty()) {
            return categoryTextures;
        }

        List<ResourceLocation> genericTextures = listTextures(NpcIdentityConfig.textureFolder(NpcCategory.GENERIC));
        if (!genericTextures.isEmpty()) {
            return genericTextures;
        }

        return listTextures("textures/entity/npc");
    }

    public static List<ResourceLocation> availableTextures() {
        return listTextures("textures/entity/npc");
    }

    public static List<ResourceLocation> availableTextures(NpcCategory category) {
        return texturesFor(category);
    }

    private static List<ResourceLocation> listTextures(String prefix) {
        return Minecraft.getInstance().getResourceManager()
                .listResources(prefix, location ->
                        location.getNamespace().equals(CyberneticEnhancements.MOD_ID)
                                && location.getPath().endsWith(".png"))
                .keySet()
                .stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }
}
