package de.artemis.cyberneticenhancements.common.world;

import org.junit.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;

public class DeferredWorkQueueTest {
    @Test
    public void boundedBatchesKeepRemainingWorkAndNewArrivals() {
        var queue = new DeferredWorkQueue<String, Integer>();
        for (int i = 0; i < 12; i++) queue.add("world", i);
        assertEquals(List.of(0, 1, 2), queue.takeBatch("world", 3));
        queue.add("world", 12);
        queue.defer("world", List.of(1));
        assertEquals(List.of(3, 4, 5, 6, 7, 8, 9, 10), queue.takeBatch("world", 8));
        assertEquals(List.of(11, 12, 1), queue.takeBatch("world", 8));
        assertTrue(queue.takeBatch("world", 8).isEmpty());
    }

    @Test
    public void chunkLoadsDuringPlacementLeaveTheCurrentBatchStable() {
        var queue = new DeferredWorkQueue<String, Integer>();
        queue.add("world", 1);
        queue.add("world", 2);
        var batch = queue.takeBatch("world");
        var processed = new ArrayList<Integer>();
        for (int chunk : batch) {
            processed.add(chunk);
            queue.add("world", chunk + 10);
        }
        assertEquals(List.of(1, 2), processed);
        assertEquals(List.of(11, 12), queue.takeBatch("world"));
        assertTrue(queue.takeBatch("world").isEmpty());
    }

    @Test
    public void deferredChunksDoNotOverwriteChunksLoadedWhileProcessing() {
        var queue = new DeferredWorkQueue<String, Integer>();
        queue.add("world", 1);
        queue.add("world", 2);
        var current = queue.takeBatch("world");
        queue.add("world", 3);
        queue.defer("world", List.of(current.get(1)));
        assertEquals(List.of(3, 2), queue.takeBatch("world"));
        queue.add("world", 4);
        queue.defer("world", List.of());
        assertEquals(List.of(4), queue.takeBatch("world"));
    }

    @Test
    public void worldUnloadClearsOnlyThatWorldsPendingWork() {
        var queue = new DeferredWorkQueue<Object, Integer>();
        var firstWorld = new Object();
        var secondWorld = new Object();
        queue.add(firstWorld, 1);
        queue.add(secondWorld, 2);
        queue.clear(firstWorld);
        assertTrue(queue.takeBatch(firstWorld).isEmpty());
        assertEquals(List.of(2), queue.takeBatch(secondWorld));
    }

    @Test(timeout = 5000)
    public void concurrentLoadsAndBatchDrainingDoNotLoseOrDuplicateWork() throws Exception {
        var queue = new DeferredWorkQueue<String, Integer>();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var producers = new ArrayList<CompletableFuture<Void>>();
            for (int producer = 0; producer < 2; producer++) {
                int offset = producer * 1000;
                producers.add(CompletableFuture.runAsync(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(exception);
                    }
                    for (int index = 0; index < 1000; index++) queue.add("world", offset + index);
                }, executor));
            }
            start.countDown();
            var finished = CompletableFuture.allOf(producers.toArray(CompletableFuture[]::new));
            var processed = new ArrayList<Integer>();
            while (!finished.isDone()) {
                processed.addAll(queue.takeBatch("world"));
                Thread.yield();
            }
            finished.get();
            processed.addAll(queue.takeBatch("world"));
            assertEquals(2000, processed.size());
            assertEquals(2000, new HashSet<>(processed).size());
            assertTrue(queue.takeBatch("world").isEmpty());
        } finally {
            executor.shutdownNow();
        }
    }
}
