package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

final class HologramPixelGeometry {
    private HologramPixelGeometry() {}

    static void ring(PoseStack pose, VertexConsumer vertices, float y, int outer, int inner, float pixel, int color) {
        var matrix = pose.last().pose();
        for (int x = -outer; x < outer; x++) for (int z = -outer; z < outer; z++) {
            if (!HologramPixels.ringCell(x, z, outer, inner)) continue;
            vertices.addVertex(matrix, x * pixel, y, z * pixel).setColor(color);
            vertices.addVertex(matrix, (x + 1) * pixel, y, z * pixel).setColor(color);
            vertices.addVertex(matrix, (x + 1) * pixel, y, (z + 1) * pixel).setColor(color);
            vertices.addVertex(matrix, x * pixel, y, (z + 1) * pixel).setColor(color);
        }
    }
}
