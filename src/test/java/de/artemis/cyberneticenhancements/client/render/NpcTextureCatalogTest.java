package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class NpcTextureCatalogTest {
    @Test
    public void missingTextureDuringEnumerationDoesNotCrashOrRepeatEachFrame() {
        var catalog = new NpcTextureCatalog<String>();
        var calls = new AtomicInteger();
        for (int frame = 0; frame < 60; frame++) {
            assertTrue(catalog.get("female", () -> {
                calls.incrementAndGet();
                throw new UncheckedIOException(new NoSuchFileException("textures/entity/npc/female/v_1.png"));
            }).isEmpty());
        }
        assertEquals(1, calls.get());
        assertEquals(List.of("generic.png"), catalog.get("generic", () -> List.of("generic.png")));
    }

    @Test
    public void resourceReloadRetriesUnavailableSkinsAndRefreshesSuccessfulResults() {
        var catalog = new NpcTextureCatalog<String>();
        catalog.get("female", () -> { throw new UncheckedIOException(new NoSuchFileException("skin.png")); });
        catalog.get("male", () -> List.of("old.png"));
        catalog.clear();
        assertEquals(List.of("restored.png"), catalog.get("female", () -> List.of("restored.png")));
        assertEquals(List.of("new.png"), catalog.get("male", () -> List.of("new.png")));
    }

    @Test
    public void discoveredSkinsAreAnImmutableSnapshotAcrossRenderCalls() {
        var catalog = new NpcTextureCatalog<String>();
        var source = new java.util.ArrayList<>(List.of("one.png"));
        var first = catalog.get("male", () -> source);
        source.add("two.png");
        assertSame(first, catalog.get("male", () -> { throw new AssertionError("Repeated resource scan"); }));
        assertEquals(List.of("one.png"), first);
    }
}