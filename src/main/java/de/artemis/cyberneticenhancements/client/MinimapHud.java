package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.screen.MapSettings;
import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.client.ui.NeuralLinkStyle;
import de.artemis.cyberneticenhancements.client.ui.UiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * A small square of the explored map around the player, north up, in the corner opposite the call window and quest tracker.
 * It reads the same pictures the Map tab does, so it shows nothing the map does not already know.
 */
public final class MinimapHud {
    private static final int INSET = 4;

    private MinimapHud() {}

    public static void render(GuiGraphics original, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        var viewport = NeuralLinkStyle.viewport();
        int size = Math.min(MapSettings.MINIMAP_SIZE.value(), viewport.height() / 3);
        // Automatically opposite the call window, and clear of the status effect icons when it is on the right.
        int corner = MapSettings.MINIMAP_CORNER.value();
        boolean left = corner == 0 ? !ClientConfig.CALL_WINDOW_ON_LEFT.get() : corner == 1;
        int x = left ? NeuralLinkStyle.MARGIN : NeuralLinkStyle.x(viewport, false) + NeuralLinkStyle.WIDTH - size, y = NeuralLinkStyle.MARGIN;
        int inner = size - 2 * INSET, mapX = x + INSET, mapY = y + INSET;
        double perBlock = inner / (double) MapSettings.MINIMAP_RANGE.value(), centerX = mapX + inner / 2.0, centerY = mapY + inner / 2.0;
        Vec3 position = player.getPosition(partialTick);

        float[] tint = RenderSystem.getShaderColor().clone();
        float opacity = MapSettings.MINIMAP_OPACITY.scale();
        original.flush();
        GuiGraphics graphics = new UiGraphics(original, viewport);
        try {
            if (MapSettings.MINIMAP_FRAME.on()) GameplayHudStyle.panel(graphics, x, y, size, size, UiRenderer.ACCENT, opacity);
            graphics.fill(mapX, mapY, mapX + inner, mapY + inner, GameplayHudStyle.color(GameplayHudStyle.SURFACE, .85F * opacity));
            graphics.enableScissor(mapX, mapY, mapX + inner, mapY + inner);
            terrain(graphics, position, centerX, centerY, inner, perBlock, tint, opacity);
            if (MapSettings.MINIMAP_MARKERS.on()) landmarks(graphics, position, centerX, centerY, inner, perBlock, opacity);
            graphics.disableScissor();
            arrow(graphics, centerX, centerY, player.getViewYRot(partialTick));
            if (MapSettings.MINIMAP_NORTH.on()) {
                graphics.pose().pushPose();
                graphics.pose().translate((float) centerX, mapY + 2, 0);
                graphics.pose().scale(.65F, .65F, 1);
                graphics.drawString(minecraft.font, "N", -minecraft.font.width("N") / 2, 0, GameplayHudStyle.color(UiRenderer.TEXT_SECONDARY, .9F * opacity), true);
                graphics.pose().popPose();
            }
            if (MapSettings.MINIMAP_FRAME.on()) graphics.renderOutline(x, y, size, size, GameplayHudStyle.color(UiRenderer.FIELD_EDGE, .65F * opacity));
        } finally {
            graphics.flush();
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
        }
    }

    private static void terrain(GuiGraphics graphics, Vec3 position, double centerX, double centerY, int inner, double perBlock, float[] tint, float opacity) {
        int region = WorldMapCache.REGION_SIZE;
        double reach = inner / 2.0 / perBlock;
        int minX = Math.floorDiv((int) Math.floor(position.x - reach), region), maxX = Math.floorDiv((int) Math.floor(position.x + reach), region);
        int minZ = Math.floorDiv((int) Math.floor(position.z - reach), region), maxZ = Math.floorDiv((int) Math.floor(position.z + reach), region);
        boolean smooth = perBlock < 1 && MapSettings.MINIMAP_SMOOTH.on();
        // Down in a cave the map keeps showing the surface, unless it was asked to follow the player down.
        boolean underground = !MapSettings.MINIMAP_CAVES.on() && WorldMapCache.layer() != WorldMapCache.SURFACE && !Minecraft.getInstance().level.dimensionType().hasCeiling();
        if (!underground) WorldMapCache.refreshSurface();
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3] * .92F * opacity);
        for (int rx = minX; rx <= maxX; rx++) {
            for (int rz = minZ; rz <= maxZ; rz++) {
                var picture = underground ? WorldMapCache.surface(rx, rz) : WorldMapCache.peek(rx, rz);
                if (picture == null) continue;
                var texture = picture.texture(smooth);
                // Each picture runs from its own whole-pixel edge to the next one's, so no hairline of background shows between them.
                int left = (int) Math.round(centerX + (rx * (double) region - position.x) * perBlock), right = (int) Math.round(centerX + ((rx + 1) * (double) region - position.x) * perBlock);
                int top = (int) Math.round(centerY + (rz * (double) region - position.z) * perBlock), bottom = (int) Math.round(centerY + ((rz + 1) * (double) region - position.z) * perBlock);
                graphics.pose().pushPose();
                graphics.pose().translate((float) left, (float) top, 0);
                graphics.pose().scale((right - left) / (float) region, (bottom - top) / (float) region, 1);
                graphics.blit(texture, 0, 0, 0F, 0F, region, region, region, region);
                graphics.pose().popPose();
            }
        }
        graphics.flush();
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
    }

    /** The compass's landmarks as dots; the ones out of range sit on the edge, pointing the way. */
    private static void landmarks(GuiGraphics graphics, Vec3 position, double centerX, double centerY, int inner, double perBlock, float opacity) {
        String dimension = Minecraft.getInstance().level.dimension().location().toString();
        double limit = inner / 2.0 - 3;
        for (var point : CompassPreferences.visible()) {
            if (!point.dimension().equals(dimension)) continue;
            double dx = (point.pos().getX() + .5 - position.x) * perBlock, dz = (point.pos().getZ() + .5 - position.z) * perBlock;
            boolean outside = Math.abs(dx) > limit || Math.abs(dz) > limit;
            int px = (int) Math.round(centerX + Math.clamp(dx, -limit, limit)), py = (int) Math.round(centerY + Math.clamp(dz, -limit, limit));
            int radius = outside ? 1 : 2;
            graphics.fill(px - radius, py - radius, px + radius + 1, py + radius + 1, 0xAA000000);
            graphics.fill(px - radius + 1, py - radius + 1, px + radius, py + radius, GameplayHudStyle.color(point.color(), (outside ? .7F : 1) * opacity));
        }
    }

    /** The player, pointing the way they face. */
    private static void arrow(GuiGraphics graphics, double centerX, double centerY, float yaw) {
        graphics.pose().pushPose();
        graphics.pose().translate((float) centerX, (float) centerY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(yaw - 180));
        for (int row = 0; row < 5; row++) {
            int half = (row + 1) / 2;
            graphics.fill(-half - 1, row - 4, half + 2, row - 3, 0x99000000);
        }
        for (int row = 0; row < 5; row++) {
            int half = (row + 1) / 2;
            graphics.fill(-half, row - 4, half + 1, row - 3, GameplayHudStyle.color(UiRenderer.ACCENT, 1));
        }
        graphics.pose().popPose();
    }
}
