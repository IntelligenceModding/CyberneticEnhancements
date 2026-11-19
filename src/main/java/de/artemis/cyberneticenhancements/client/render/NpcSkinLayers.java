package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class NpcSkinLayers {
    private static final Map<ResourceLocation, Layers> CACHE = new HashMap<>();

    private NpcSkinLayers() {
    }

    static Layers get(ResourceLocation skin) {
        return CACHE.computeIfAbsent(skin, NpcSkinLayers::load);
    }

    private static Layers load(ResourceLocation skin) {
        var minecraft = Minecraft.getInstance();
        var resource = minecraft.getResourceManager().getResource(skin);
        if (resource.isEmpty()) return new Layers(skin, null);
        try (var stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int width = image.getWidth(), height = image.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) pixels[y * width + x] = image.getPixelRGBA(x, y);
            var split = NpcSkinAlpha.split(pixels);
            if (!split.hasTranslucency()) return new Layers(skin, null);
            String id = UUID.nameUUIDFromBytes(skin.toString().getBytes(StandardCharsets.UTF_8)).toString();
            ResourceLocation opaque = ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", "dynamic/npc/opaque/" + id);
            ResourceLocation translucent = ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", "dynamic/npc/translucent/" + id);
            register(opaque, width, height, split.opaque());
            register(translucent, width, height, split.translucent());
            return new Layers(opaque, translucent);
        } catch (IOException | UncheckedIOException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not prepare translucent NPC skin {}", skin, exception);
            return new Layers(skin, null);
        }
    }

    private static void register(ResourceLocation id, int width, int height, int[] pixels) {
        NativeImage image = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setPixelRGBA(x, y, pixels[y * width + x]);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
    }

    static void clear() {
        var textures = Minecraft.getInstance().getTextureManager();
        CACHE.forEach((original, layers) -> {
            if (layers.translucent != null) {
                textures.release(layers.opaque);
                textures.release(layers.translucent);
            }
        });
        CACHE.clear();
    }

    record Layers(ResourceLocation opaque, ResourceLocation translucent) {
    }
}