package de.artemis.cyberneticenhancements.common.cyberware;
import org.junit.Test;
import static org.junit.Assert.*;

public class PsychosisIntroMotionTest {
    @Test public void effectsBuildThroughoutTheIntroInsteadOfSwitchingOnAtTheEnd(){
        assertEquals(0,PsychosisIntroMotion.buildup(0),0);
        assertEquals(1,PsychosisIntroMotion.buildup(100),0);
        float previous=0;
        for(int tick=0;tick<=100;tick++){
            float current=PsychosisIntroMotion.buildup(tick);
            assertTrue(current>=previous&&current-previous<.025F);
            previous=current;
        }
        assertTrue(PsychosisIntroMotion.buildup(25)>0);
        assertEquals(.5F,PsychosisIntroMotion.buildup(50),.001);
    }
    @Test public void cameraVelocityIsContinuousAtGestureBoundaries(){
        float step=.02F;
        for(float time:new float[]{20,49,80}){
            var before=PsychosisIntroMotion.look(time-step,179,17);
            var at=PsychosisIntroMotion.look(time,179,17);
            var after=PsychosisIntroMotion.look(time+step,179,17);
            assertEquals((at.yaw()-before.yaw())/step,(after.yaw()-at.yaw())/step,.07);
            assertEquals((at.pitch()-before.pitch())/step,(after.pitch()-at.pitch())/step,.07);
        }
    }
    @Test public void cameraStartsAtExistingViewAndArrivesAtRestWithoutAWrapSnap(){
        var start=PsychosisIntroMotion.look(0,179,42);
        assertEquals(179,start.yaw(),0);assertEquals(42,start.pitch(),0);
        var before=PsychosisIntroMotion.look(99.98F,179,42);
        var end=PsychosisIntroMotion.look(100,179,42);
        assertEquals(before.yaw(),end.yaw(),.001);assertEquals(before.pitch(),end.pitch(),.001);
        CyberpsychosisClientState.apply(true,end.yaw(),end.pitch(),0,0,false,false);
        CyberpsychosisClientState.alignView(end.yaw(),end.pitch());
        CyberpsychosisClientState.apply(true,-179,10,1,0,false,true);
        // The next pursuit packet updates the destination, not the camera's displayed angle.
        assertEquals(end.yaw(),CyberpsychosisClientState.yaw(),0);
        assertEquals(end.pitch(),CyberpsychosisClientState.pitch(),0);
        CyberpsychosisClientState.setControlLocked(false);
    }
    @Test public void gesturesPlayInOrderAndEndLookingUpWithHandsLowered(){
        var right=PsychosisIntroMotion.sample(20);
        assertTrue(right.yaw()>0 && right.pitch()>30 && right.rightX()<right.leftX());
        var left=PsychosisIntroMotion.sample(48);
        assertTrue(left.yaw()<0 && left.pitch()>30 && left.leftX()<left.rightX());
        var forehead=PsychosisIntroMotion.sample(80);
        assertTrue(forehead.pitch()>60 && forehead.rightX()<-1.7 && forehead.leftX()<-1.7);
        var end=PsychosisIntroMotion.sample(PsychosisIntroMotion.DURATION);
        assertTrue(end.pitch()<0 && Math.abs(end.yaw())<.001 && end.rightX()>-.2 && end.leftX()>-.2);
    }
    @Test public void keyframesJoinContinuouslyAndClampOutsideTimeline(){
        assertEquals(PsychosisIntroMotion.sample(0),PsychosisIntroMotion.sample(-20));
        assertEquals(PsychosisIntroMotion.sample(100),PsychosisIntroMotion.sample(200));
        for(float tick=1;tick<=100;tick+=.1F){
            var a=PsychosisIntroMotion.sample(tick-.1F);var b=PsychosisIntroMotion.sample(tick);
            assertTrue(Math.abs(a.yaw()-b.yaw())<1 && Math.abs(a.pitch()-b.pitch())<1);
            assertTrue(Math.abs(a.rightX()-b.rightX())<.1 && Math.abs(a.leftX()-b.leftX())<.1);
        }
    }
}
