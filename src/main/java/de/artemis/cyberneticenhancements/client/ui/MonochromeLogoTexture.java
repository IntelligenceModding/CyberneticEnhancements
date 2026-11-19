package de.artemis.cyberneticenhancements.client.ui;

import java.io.IOException;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Removes source hue before the intro applies its shared red tint. */
public final class MonochromeLogoTexture extends SimpleTexture {
    public MonochromeLogoTexture(ResourceLocation location) {
        super(location);
    }

    @Override
    protected TextureImage getTextureImage(ResourceManager resources) {
        TextureImage texture = super.getTextureImage(resources);
        try {
            var image = texture.getImage();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    // NativeImage uses ABGR. Weighted luminance keeps orange fur
                    // darker than white lettering instead of flattening both to
                    // the strongest channel's brightness.
                    int red = pixel & 255;
                    int green = pixel >>> 8 & 255;
                    int blue = pixel >>> 16 & 255;
                    int brightness = (54 * red + 183 * green + 19 * blue + 128) >>> 8;
                    image.setPixelRGBA(x, y, pixel & 0xFF000000 | brightness * 0x010101);
                }
            }
        } catch (IOException ignored) {
            // Preserve the original error for Minecraft's normal missing-texture handling.
        }
        return texture;
    }
}
