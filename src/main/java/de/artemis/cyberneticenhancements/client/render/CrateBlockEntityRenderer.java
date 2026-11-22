package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.common.blockentity.CrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The floating "!" over a crate that holds a quest cargo. Blocks in front of it hide it, which is why such crates keep their top free.
 * Block entities are drawn before the water is, so a mark drawn right here would sit behind the water of a sunken container and be washed out;
 * the crates only report themselves here and the marks are drawn once the translucent blocks and particles are done.
 */
public final class CrateBlockEntityRenderer implements BlockEntityRenderer<CrateBlockEntity> {
    private static final List<BlockPos> MARKED = new ArrayList<>();

    public CrateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CrateBlockEntity crate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (crate.marked() && !ShaderPackCompatibility.isRenderingShadowPass() && crate.getLevel() != null) MARKED.add(crate.getBlockPos());
    }

    /** Draws the marks of the crates that were rendered this frame, in front of the water. */
    public static void drawMarks(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ShaderPackCompatibility.isRenderingShadowPass()) return;
        if (MARKED.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        var positions = new ArrayList<>(MARKED);
        MARKED.clear();
        if (minecraft.level == null) return;
        var font = minecraft.font;
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var mark = Component.literal("!").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (var pos : positions) {
            var pose = new PoseStack();
            pose.last().pose().set(event.getPoseStack().last().pose());
            pose.last().normal().set(event.getPoseStack().last().normal());
            pose.translate(pos.getX() + 0.5 - camera.x, pos.getY() + 1.15 + Math.sin(time * 0.12) * 0.05 - camera.y, pos.getZ() + 0.5 - camera.z);
            pose.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
            pose.scale(-0.07F, -0.07F, 0.07F);
            font.drawInBatch(mark, -font.width(mark) / 2F, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        buffers.endBatch();
    }

    @Override
    public AABB getRenderBoundingBox(CrateBlockEntity crate) {
        var pos = crate.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
