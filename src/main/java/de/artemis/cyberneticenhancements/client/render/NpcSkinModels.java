package de.artemis.cyberneticenhancements.client.render;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class NpcSkinModels {
    private static final Map<ResourceLocation, Boolean> SLIM_MODELS = new HashMap<>();
    private static final MetadataSectionSerializer<String> MODEL_METADATA = new MetadataSectionSerializer<>() {
        @Override
        public String getMetadataSectionName() {
            return "npc_skin";
        }

        @Override
        public String fromJson(JsonObject json) {
            return json.has("model") ? json.get("model").getAsString() : "auto";
        }
    };

    private NpcSkinModels() {
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> SLIM_MODELS.clear());
    }

    public static boolean isSlim(ResourceLocation texture) {
        return SLIM_MODELS.computeIfAbsent(texture, key -> readModel(Minecraft.getInstance().getResourceManager(), key));
    }

    private static boolean readModel(ResourceManager manager, ResourceLocation texture) {
        var resource = manager.getResource(texture);
        if (resource.isEmpty()) {
            return false;
        }

        try {
            String model = resource.get().metadata().getSection(MODEL_METADATA).orElse("auto");
            if ("slim".equalsIgnoreCase(model)) return true;
            if ("wide".equalsIgnoreCase(model) || "classic".equalsIgnoreCase(model)) return false;
        } catch (IOException | RuntimeException ignored) {

        }
        try (InputStream stream = resource.get().open()) {
            return hasSlimArmLayout(ImageIO.read(stream));
        } catch (IOException ignored) {
            return false;
        }
    }

    static boolean hasSlimArmLayout(BufferedImage image) {

        if (image == null || image.getWidth() < 64 || image.getWidth() % 64 != 0
                || image.getHeight() != image.getWidth()) {
            return false;
        }
        int scale = image.getWidth() / 64;
        return transparent(image, 54, 20, 2, 12, scale)
                && transparent(image, 46, 52, 2, 12, scale);
    }

    private static boolean transparent(BufferedImage image, int x, int y, int width, int height, int scale) {
        for (int px = x * scale; px < (x + width) * scale; px++) {
            for (int py = y * scale; py < (y + height) * scale; py++) {
                if ((image.getRGB(px, py) >>> 24) != 0) return false;
            }
        }
        return true;
    }
}
