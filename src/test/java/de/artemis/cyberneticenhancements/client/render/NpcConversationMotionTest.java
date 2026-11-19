package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class NpcConversationMotionTest {
    @Test
    public void gesturesStayBelowTheFaceAndHeadMovementStaysSubtle() {
        for (int identity : new int[]{Integer.MIN_VALUE, -81, 0, 13, Integer.MAX_VALUE}) {
            for (int frame = 0; frame < 1800; frame++) {
                var pose = NpcConversationMotion.sample(frame / 60.0, identity, frame / 60.0);
                assertTrue(Math.abs(pose.headYaw()) < 0.12F);
                assertTrue(Math.abs(pose.headPitch()) < 0.12F);
                assertTrue(Math.abs(pose.headRoll()) < 0.04F);
                assertTrue(Math.abs(pose.bodyRoll()) < 0.025F);
                assertTrue(pose.leftPitch() >= -0.5F && pose.leftPitch() <= 0);
                assertTrue(pose.rightPitch() >= -0.25F && pose.rightPitch() <= 0);
            }
        }
    }

    @Test
    public void repliesTriggerAShortGestureAndSettleBackToListening() {
        var listening = NpcConversationMotion.sample(10, 42, -1);
        var speaking = NpcConversationMotion.sample(10, 42, 1);
        assertTrue(speaking.leftPitch() < listening.leftPitch() - 0.2F);
        assertTrue(speaking.headPitch() != listening.headPitch());
        assertEquals(listening, NpcConversationMotion.sample(10, 42, 4));
        assertNotEquals(listening, NpcConversationMotion.sample(11, 42, -1));
        assertNotEquals(listening, NpcConversationMotion.sample(10, 84, -1));
    }

    @Test
    public void animationIsDeterministicAndContinuousAtReplyBoundaries() {
        var start = NpcConversationMotion.sample(10, 42, 0);
        var next = NpcConversationMotion.sample(10, 42, 0.001);
        assertEquals(start.headPitch(), next.headPitch(), 0.0001F);
        assertEquals(NpcConversationMotion.sample(10, 42, -1), NpcConversationMotion.sample(10, 42, 3.2));
        assertEquals(next, NpcConversationMotion.sample(10, 42, 0.001));
    }
}
