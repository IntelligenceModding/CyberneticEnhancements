package de.artemis.cyberneticenhancements.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class TitleSlideshowReloadTest {
    @Test
    public void repairedResourcesCanLoadAfterACorruptSlideAndFailedBatchesReleaseTheirImages() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "PNG", output);
        byte[] valid = output.toByteArray();
        var broken = new AtomicBoolean(true);
        ResourceManager resources = (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
                new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("open")) {
                        var location = (ResourceLocation) args[0];
                        return new ByteArrayInputStream(broken.get() && location.getPath().endsWith("/4.png")
                                ? new byte[]{0, 1, 2, 3} : valid);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        try (var failed = TitleSlideshow.readSlides(resources)) {
            assertNotNull("A corrupt slide must fail the whole batch for the panorama fallback", failed.error());
            for (var image : failed.images()) assertNull("Partially decoded slides must be released", image);
        }
        broken.set(false);
        var recovered = TitleSlideshow.readSlides(resources);
        try (recovered) {
            assertNull("The next resource generation must retry the repaired images", recovered.error());
            assertEquals(9, recovered.images().length);
            for (var image : recovered.images()) {
                assertNotNull(image);
                assertEquals(2, image.getWidth());
            }
        }
        for (var image : recovered.images()) assertNull(image);
    }
}
