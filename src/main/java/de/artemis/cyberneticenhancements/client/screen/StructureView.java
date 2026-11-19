package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A turnable 3D view of a structure template for the wiki, read straight from the mod's own data so it always shows
 * what actually generates. It spins slowly until dragged.
 */
final class StructureView {
    private record Placed(BlockPos pos, BlockState state) {}

    private record Model(int sizeX, int sizeY, int sizeZ, List<Placed> blocks) {
        static final Model EMPTY = new Model(1, 1, 1, List.of());
    }

    private static final Map<String, Model> MODELS = new ConcurrentHashMap<>();
    private static final long IDLE_BEFORE_SPIN_MILLIS = 2500;
    private static float yaw = 215, pitch = 24;
    private static long lastDragAt = Long.MIN_VALUE / 2, lastFrameAt;

    private StructureView() {}

    /** Turns the view by a mouse drag. */
    static void drag(double dragX, double dragY) {
        yaw += (float) dragX * 0.8F;
        pitch = Mth.clamp(pitch + (float) dragY * 0.6F, -10, 80);
        lastDragAt = Util.getMillis();
    }

    private static Model model(String template) {
        return MODELS.computeIfAbsent(template, StructureView::load);
    }

    private static Model load(String template) {
        try (var input = StructureView.class.getResourceAsStream("/data/cyberneticenhancements/structure/" + template + ".nbt")) {
            if (input == null) return Model.EMPTY;
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            var size = root.getList("size", Tag.TAG_INT);
            var palette = new ArrayList<BlockState>();
            for (var tag : root.getList("palette", Tag.TAG_COMPOUND))
                palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) tag));
            var blocks = new ArrayList<Placed>();
            for (var tag : root.getList("blocks", Tag.TAG_COMPOUND)) {
                var entry = (CompoundTag) tag;
                var at = entry.getList("pos", Tag.TAG_INT);
                BlockState state = palette.get(entry.getInt("state"));
                if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.JIGSAW) || state.is(Blocks.STRUCTURE_BLOCK)) continue;
                blocks.add(new Placed(new BlockPos(at.getInt(0), at.getInt(1), at.getInt(2)), state));
            }
            return new Model(size.getInt(0), size.getInt(1), size.getInt(2), List.copyOf(blocks));
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().warn("Could not load the wiki's structure view of {}", template, exception);
            return Model.EMPTY;
        }
    }

    static void render(GuiGraphics graphics, String template, int x, int y, int width, int height) {
        var model = model(template);
        if (model.blocks().isEmpty()) return;
        long now = Util.getMillis();
        if (now - lastDragAt > IDLE_BEFORE_SPIN_MILLIS && lastFrameAt > 0) yaw += Math.min(100, now - lastFrameAt) * 0.012F;
        lastFrameAt = now;

        double radius = Math.sqrt(model.sizeX() * model.sizeX() + model.sizeY() * model.sizeY() + model.sizeZ() * model.sizeZ()) / 2;
        float scale = (float) (Math.min(width, height) / (radius * 2.1));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + width / 2.0F, y + height / 2.0F, 300);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-model.sizeX() / 2.0F, -model.sizeY() / 2.0F, -model.sizeZ() / 2.0F);
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        // Stale depth from the panels behind this one would hide parts of the model as it turns; the clear is limited to the caller's scissor.
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        var renderer = Minecraft.getInstance().getBlockRenderer();
        var buffers = graphics.bufferSource();
        for (var placed : model.blocks()) {
            pose.pushPose();
            pose.translate(placed.pos().getX(), placed.pos().getY(), placed.pos().getZ());
            renderer.renderSingleBlock(placed.state(), pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        graphics.flush();
        Lighting.setupForFlatItems();
        pose.popPose();
    }
}
