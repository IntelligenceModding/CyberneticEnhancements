package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public final class NpcHealthBarRenderer {
    private NpcHealthBarRenderer() {}

    public static void render(LivingEntity entity, Component name, PoseStack pose, MultiBufferSource buffers,
                              EntityRenderDispatcher dispatcher, float partialTick) {
        if (de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState.isControlLocked()
                || !entity.isAlive() || entity.isInvisible() || ShaderPackCompatibility.isRenderingShadowPass()
                || !net.neoforged.neoforge.client.ClientHooks.isNameplateInRenderDistance(entity, dispatcher.distanceToSqr(entity))) return;
        var anchor = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (anchor == null) return;
        float health = entity.getHealth(), maximum = entity.getMaxHealth();
        if (!Float.isFinite(health) || !Float.isFinite(maximum) || maximum <= 0) return;
        float fraction = Math.clamp(health / maximum, 0, 1);
        float top = "deadmau5".equals(name.getString()) ? 2 : 12;
        pose.pushPose();
        pose.translate(anchor.x, anchor.y + 0.5, anchor.z);
        pose.mulPose(dispatcher.cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);
        var matrix = pose.last().pose();
        var vertices = buffers.getBuffer(RenderType.textBackground());
        var healing = de.artemis.cyberneticenhancements.common.entity.NpcCombatHealing.of(entity);
        float healingColor = NpcHealingColors.amount(entity, partialTick);
        bar(vertices, matrix, top, fraction, NpcHealingColors.blend(0xFFE66A6A, 0xFF63C99C, healingColor),
                NpcHealingColors.blend(0xFFF28A8A, 0xFFA0F0C6, healingColor));
        if (healing != null && healing.usingItem()) {
            float pending = Math.min(1, fraction + de.artemis.cyberneticenhancements.common.entity.NpcCombatHealing.HEAL_FRACTION);
            quad(vertices, matrix, -22 + 44 * fraction, top + 1, -22 + 44 * pending, top + 4, 0.04F, 0x805BCB98);
            bar(vertices, matrix, top + (entity instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity ? 14 : 7),
                    healing.progress(partialTick), 0xFF63C99C, 0xFFA0F0C6);
        }
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc)
            bar(vertices, matrix, top + 7, npc.aggressionRemaining(partialTick), UiRenderer.WARNING, 0xFFFFD29A);
        pose.popPose();
    }

    private static void bar(VertexConsumer vertices, Matrix4f matrix, float top, float fraction, int color, int highlight) {
        quad(vertices, matrix, -23, top, 23, top + 5, 0, UiRenderer.FIELD_EDGE);
        quad(vertices, matrix, -22, top + 1, 22, top + 4, 0.01F, UiRenderer.FIELD);
        if (fraction > 0) {
            float right = -22 + 44 * fraction;
            quad(vertices, matrix, -22, top + 1, right, top + 4, 0.02F, color);
            quad(vertices, matrix, -22, top + 1, right, top + 2, 0.03F, highlight);
        }
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, float left, float top, float right, float bottom, float z, int color) {
        vertices.addVertex(matrix, left, top, z).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, left, bottom, z).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, right, bottom, z).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, right, top, z).setColor(color).setLight(LightTexture.FULL_BRIGHT);
    }
}
