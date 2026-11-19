package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealingHealthBarTest {
    private static HealingHealthBar.Frame sample(HealingHealthBar bar, float health, boolean using, boolean highlighted, double tick) {
        return bar.sample(health, using, highlighted, tick, 0.25F);
    }

    @Test
    public void previewBecomesASmoothTwoAndAHalfSecondFillWithoutExtendingTheHealingTarget() {
        var bar = new HealingHealthBar();
        sample(bar, 0.4F, false, false, 0);
        var preview = sample(bar, 0.4F, true, true, 10);
        assertEquals(0.4F, preview.health(), 0.0001F);
        assertEquals(0.65F, preview.pendingHealth(), 0.0001F);
        assertEquals(1, sample(bar, 0.4F, true, true, 17).green(), 0.0001F);
        var consumed = sample(bar, 0.65F, false, true, 50);
        assertEquals(0.4F, consumed.health(), 0.0001F);
        assertEquals(0.65F, consumed.pendingHealth(), 0.0001F);
        var halfway = sample(bar, 0.65F, false, false, 75);
        assertEquals(0.525F, halfway.health(), 0.0001F);
        assertTrue(halfway.filling());
        assertEquals(1, halfway.green(), 0.0001F);
        var finished = sample(bar, 0.65F, false, false, 100);
        assertEquals(0.65F, finished.health(), 0.0001F);
        assertFalse(finished.filling());
        assertEquals(0, sample(bar, 0.65F, false, false, 107).green(), 0.0001F);
    }

    @Test
    public void interruptingItemUseRemovesThePreviewWithoutInventingAHeal() {
        var bar = new HealingHealthBar();
        sample(bar, 0.4F, false, false, 0);
        sample(bar, 0.4F, true, true, 10);
        var interrupted = sample(bar, 0.3F, false, false, 20);
        assertEquals(0.3F, interrupted.health(), 0.0001F);
        assertEquals(interrupted.health(), interrupted.pendingHealth(), 0);
        assertFalse(interrupted.filling());
        assertEquals(0.3F, sample(bar, 0.3F, false, false, 100).health(), 0.0001F);
    }

    @Test
    public void takingDamageDuringTheFillDropsImmediatelyAndCorrectsItsEndpoint() {
        var bar = new HealingHealthBar();
        sample(bar, 0.4F, false, false, 0);
        sample(bar, 0.65F, false, true, 10);
        var beforeHit = sample(bar, 0.65F, false, false, 35);
        var afterHit = sample(bar, 0.55F, false, false, 35);
        assertEquals(beforeHit.health() - 0.1F, afterHit.health(), 0.0001F);
        assertEquals(0.55F, afterHit.pendingHealth(), 0.0001F);
        assertEquals(0.55F, sample(bar, 0.55F, false, false, 60).health(), 0.0001F);
    }
}
