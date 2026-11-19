package de.artemis.cyberneticenhancements.common.cyberware;

import org.junit.Test;
import static org.junit.Assert.*;

public class PsychosisRecoveryMotionTest {
    @Test public void recoveryInspectsHandsHoldsHeadThenReturnsToStanding(){
        var hands=PsychosisRecoveryMotion.sample(30);
        assertTrue(hands.pitch()>30&&hands.rightX()<-1&&hands.leftX()<-1);
        var head=PsychosisRecoveryMotion.sample(80);
        assertTrue(head.pitch()>50&&head.rightX()<-1.7&&head.leftX()>-.1);
        var lowered=PsychosisRecoveryMotion.sample(110);
        assertTrue(lowered.rightX()>-.2&&lowered.leftX()>-.1&&lowered.pitch()>40);
        var end=PsychosisRecoveryMotion.sample(PsychosisRecoveryMotion.DURATION);
        assertEquals(0,end.pitch(),0);assertEquals(0,end.lean(),0);
        assertEquals(0,end.rightX(),0);assertEquals(0,end.leftX(),0);
    }
    @Test public void tremorAndEffectsFadeWithoutAnEndpointSnap(){
        float previous=1;
        for(int i=0;i<=PsychosisRecoveryMotion.DURATION;i++){
            float current=PsychosisRecoveryMotion.intensity(i);
            assertTrue(current<=previous+.00001F&&current>=0);
            previous=current;
        }
        assertEquals(0,previous,0);
        var before=PsychosisRecoveryMotion.look(139.99F,179,25);
        var end=PsychosisRecoveryMotion.look(140,179,25);
        assertEquals(before.yaw(),end.yaw(),.001);assertEquals(before.pitch(),end.pitch(),.001);
        assertEquals(179,PsychosisRecoveryMotion.look(0,179,25).yaw(),0);
        assertEquals(25,PsychosisRecoveryMotion.look(0,179,25).pitch(),0);
    }
}
