package de.artemis.cyberneticenhancements.common.cyberware;

/** Slow recovery gestures with deterministic, fading tremor, shared by camera and model. */
public final class PsychosisRecoveryMotion {
    public static final int DURATION=140;
    private static final float[] TIMES={0,18,42,65,90,108,140};
    private static final PsychosisIntroMotion.Frame[] FRAMES={
        new PsychosisIntroMotion.Frame(0,0,0, 0,0,0, 0,0,0),
        new PsychosisIntroMotion.Frame(-2,38,.10F, -1.4F,.15F,-.15F, -1.4F,-.15F,.15F),
        new PsychosisIntroMotion.Frame(2,43,.13F, -1.4F,.15F,-.15F, -1.4F,-.15F,.15F),
        new PsychosisIntroMotion.Frame(-4,58,.18F, -1.9F,-.15F,-.1F, -.3F,0,.1F),
        new PsychosisIntroMotion.Frame(-3,62,.20F, -1.9F,-.15F,-.1F, 0,0,0),
        new PsychosisIntroMotion.Frame(0,58,.12F, -.1F,0,0, 0,0,0),
        new PsychosisIntroMotion.Frame(0,0,0, 0,0,0, 0,0,0)
    };
    public static float intensity(float tick){return 1-ease(Math.clamp(tick/DURATION,0,1));}
    public static PsychosisIntroMotion.Frame sample(float tick){
        if(tick<=0)return FRAMES[0];
        if(tick>=DURATION)return FRAMES[FRAMES.length-1];
        for(int i=1;i<TIMES.length;i++)if(tick<TIMES[i]){
            float t=ease((tick-TIMES[i-1])/(TIMES[i]-TIMES[i-1]));
            var a=FRAMES[i-1];var b=FRAMES[i];
            float envelope=(float)Math.pow(Math.sin(Math.PI*tick/DURATION),2);
            float shake=(float)(Math.sin(tick*1.7)+.4*Math.sin(tick*.77))*envelope;
            return new PsychosisIntroMotion.Frame(m(a.yaw(),b.yaw(),t)+shake*.6F,m(a.pitch(),b.pitch(),t)+shake*.8F,
                    m(a.lean(),b.lean(),t),m(a.rightX(),b.rightX(),t)+shake*.018F,m(a.rightY(),b.rightY(),t),m(a.rightZ(),b.rightZ(),t),
                    m(a.leftX(),b.leftX(),t)-shake*.012F,m(a.leftY(),b.leftY(),t),m(a.leftZ(),b.leftZ(),t));
        }
        return FRAMES[FRAMES.length-1];
    }
    public static PsychosisIntroMotion.Look look(float tick,float yaw,float pitch){
        var pose=sample(tick);return new PsychosisIntroMotion.Look(yaw+pose.yaw(),m(pitch,pose.pitch(),ease(Math.clamp(tick/18,0,1))));
    }
    private static float ease(float t){return t*t*t*(t*(t*6-15)+10);}
    private static float m(float a,float b,float t){return a+(b-a)*t;}
    private PsychosisRecoveryMotion(){}
}
