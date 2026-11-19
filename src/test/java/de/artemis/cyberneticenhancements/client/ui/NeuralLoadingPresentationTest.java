package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class NeuralLoadingPresentationTest {
    @Test
    public void completedReloadRemainsVisibleBeforeFadingIntoTheUnderlyingScreen() {
        assertEquals(1, NeuralLoadingPresentation.opacity(1000, 1000, -1, false), 0.001);
        assertEquals(1, NeuralLoadingPresentation.opacity(2000, 1000, -1, false), 0.001);
        assertEquals(0.5, NeuralLoadingPresentation.opacity(2500, 1000, -1, false), 0.001);
        assertEquals(0, NeuralLoadingPresentation.opacity(3000, 1000, -1, false), 0.001);
        assertEquals(0, NeuralLoadingPresentation.opacity(5000, 1000, -1, false), 0.001);
    }

    @Test
    public void resourceReloadFadesInWhileInitialStartupIsImmediatelyOpaque() {
        assertEquals(1, NeuralLoadingPresentation.opacity(1000, -1, -1, false), 0.001);
        assertEquals(0.15, NeuralLoadingPresentation.opacity(1000, -1, 1000, true), 0.001);
        assertEquals(0.5, NeuralLoadingPresentation.opacity(1250, -1, 1000, true), 0.001);
        assertEquals(1, NeuralLoadingPresentation.opacity(1500, -1, 1000, true), 0.001);
    }
}
