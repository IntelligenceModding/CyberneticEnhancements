package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.HashSet;
import java.util.Random;
import static org.junit.Assert.*;

public class SlideshowTest {
    @Test
    public void allNineBundledImagesAreRealPngFilesAtTheExpectedResolution() throws Exception {
        var directory = java.nio.file.Path.of("src/main/resources/assets/cyberneticenhancements/textures/title/slideshow");
        for (int index = 1; index <= 9; index++) {
            try (var stream = javax.imageio.ImageIO.createImageInputStream(directory.resolve(index + ".png").toFile())) {
                var readers = javax.imageio.ImageIO.getImageReaders(stream);
                assertTrue("Unreadable slide " + index, readers.hasNext());
                var reader = readers.next();
                try {
                    assertEquals("PNG", reader.getFormatName().toUpperCase(java.util.Locale.ROOT));
                    reader.setInput(stream);
                    assertEquals(2560, reader.getWidth(0));
                    assertEquals(1440, reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
    }

    @Test
    public void shuffledCyclesShowEveryImageAndNeverRepeatEitherOfTheLastTwo() {
        for (int count : new int[]{3, 4, 9}) for (int seed = 0; seed < 30; seed++) {
            var order = new SlideshowOrder(count, new Random(seed));
            int previous = -1, beforePrevious = -1;
            for (int cycle = 0; cycle < 200; cycle++) {
                var seen = new HashSet<Integer>();
                for (int i = 0; i < count; i++) {
                    int slide = order.next();
                    assertTrue(slide >= 0 && slide < count);
                    assertTrue("Repeated image within a cycle", seen.add(slide));
                    assertNotEquals("Immediate repeat across a reshuffle", previous, slide);
                    assertNotEquals("A-B-A repeat across a reshuffle", beforePrevious, slide);
                    beforePrevious = previous;
                    previous = slide;
                }
                assertEquals(count, seen.size());
            }
        }
    }

    @Test
    public void framesHoldCrossfadeAndContinueWithoutSkippingWhenReturningFromAnotherScreen() {
        var frame = new SlideshowFrame(9, new Random(1));
        frame.advance(0);
        int first = frame.current(), second = frame.next();
        for (int now = 100; now <= 11_000; now += 100) frame.advance(now);
        assertEquals(first, frame.current());
        assertEquals(0, frame.blend(), 0.001);
        frame.advance(11_750);
        assertEquals(0.5, frame.blend(), 0.001);
        frame.advance(12_500);
        assertEquals(second, frame.current());
        assertEquals(0, frame.blend(), 0.001);
        frame.advance(1_000_000);
        assertEquals("Returning from gameplay skipped an image", second, frame.current());
        assertEquals(0, frame.blend(), 0.001);
    }

    @Test
    public void croppingFillsWideTallAndStandardScreensWithoutStretching() {
        for (int[] screen : new int[][]{{1920,1080}, {2560,1080}, {3440,1440}, {5120,1440}, {1280,1024}, {1080,1920}}) {
            var crop = SlideshowFrame.cover(2560, 1440, screen[0], screen[1]);
            assertTrue(crop.x() >= 0 && crop.y() >= 0);
            assertTrue(crop.x() + crop.width() <= 2560 && crop.y() + crop.height() <= 1440);
            assertTrue(crop.width() == 2560 || crop.height() == 1440);
            assertEquals(screen[0] / (double) screen[1], crop.width() / (double) crop.height(), 0.004);
        }
        assertEquals(new SlideshowFrame.Crop(0, 0, 2560, 1440), SlideshowFrame.cover(2560, 1440, 1920, 1080));
    }
}
