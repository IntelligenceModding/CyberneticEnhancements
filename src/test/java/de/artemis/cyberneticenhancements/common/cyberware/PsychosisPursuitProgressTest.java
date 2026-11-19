package de.artemis.cyberneticenhancements.common.cyberware;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class PsychosisPursuitProgressTest {
    @Test public void stationaryPursuitSkipsTargetTemporarily() {
        var progress = new PsychosisPursuitProgress();
        var target = UUID.randomUUID();
        assertFalse(progress.stalled(target, Vec3.ZERO, 10, false, 100));
        assertFalse(progress.stalled(target, Vec3.ZERO, 10, false, 159));
        assertTrue(progress.stalled(target, Vec3.ZERO, 10, false, 160));
        assertTrue(progress.skipped(target, 359));
        assertFalse(progress.skipped(target, 360));
    }
    @Test public void MovingInCirclesDoesNotCountAsApproaching() {
        var progress = new PsychosisPursuitProgress();
        var target = UUID.randomUUID();
        for(int tick = 0; tick < 160; tick++)
            assertFalse(progress.stalled(target, new Vec3(tick % 2, 0, 0), 10, false, tick));
        assertTrue(progress.stalled(target, Vec3.ZERO, 10, false, 160));
    }
    @Test public void realApproachAndMeleeDoNotTimeOut() {
        var progress = new PsychosisPursuitProgress();
        var target = UUID.randomUUID();
        for(int tick = 0; tick < 300; tick++)
            assertFalse(progress.stalled(target, new Vec3(tick*.05, 0, 0), 30-tick*.05, false, tick));
        for(int tick = 300; tick < 1000; tick++)
            assertFalse(progress.stalled(target, new Vec3(15, 0, 0), 2, true, tick));
    }
}
