package de.artemis.cyberneticenhancements.common.cyberware;

/** Shared timeline, in ticks. Angles for model parts are radians; view angles are degrees. */
public final class PsychosisIntroMotion {
    public static final int DURATION = 100;
    public static final int SETTLE_TICKS = 20;
    public static float buildup(float tick) { return ease(Math.clamp(tick / DURATION, 0, 1)); }
    public record Frame(float yaw, float pitch, float lean, float rightX, float rightY, float rightZ,
                        float leftX, float leftY, float leftZ) {}
    private static final float[] TIMES = {0, 12, 27, 43, 55, 75, 85, 100};
    private static final float[] LOOK_TIMES = {0, 20, 49, 80, 100};
    private static final float[] LOOK_YAW = {0, 27, -27, 0, 0};
    private static final float[] LOOK_PITCH = {0, 32, 36, 68, -5};
    public record Look(float yaw, float pitch) {}
    private static final Frame[] FRAMES = {
        new Frame(0,0,0, 0,0,0, 0,0,0),
        new Frame(27,32,0, -1.45F,.20F,-.22F, 0,0,0),
        new Frame(27,32,0, -1.45F,.20F,-.22F, 0,0,0),
        new Frame(-27,32,0, -.3F,0,0, -1.45F,-.20F,.22F),
        new Frame(-27,32,0, -.3F,0,0, -1.45F,-.20F,.22F),
        new Frame(0,68,.18F, -1.75F,-.42F,-.28F, -1.75F,.42F,.28F),
        new Frame(0,68,.18F, -1.75F,-.42F,-.28F, -1.75F,.42F,.28F),
        new Frame(0,-5,0, -.08F,0,0, -.08F,0,0)
    };
    public static Frame sample(float tick) {
        if (tick <= 0) return FRAMES[0];
        for (int i=1;i<TIMES.length;i++) if(tick<TIMES[i]) {
            float t=ease((tick-TIMES[i-1])/(TIMES[i]-TIMES[i-1]));
            Frame a=FRAMES[i-1],b=FRAMES[i];
            return new Frame(curve(LOOK_YAW,tick),curve(LOOK_PITCH,tick),m(a.lean,b.lean,t),
                    m(a.rightX,b.rightX,t),m(a.rightY,b.rightY,t),m(a.rightZ,b.rightZ,t),
                    m(a.leftX,b.leftX,t),m(a.leftY,b.leftY,t),m(a.leftZ,b.leftZ,t));
        }
        return FRAMES[FRAMES.length-1];
    }
    private static float m(float a,float b,float t){return a+(b-a)*t;}
    public static Look look(float tick, float baseYaw, float initialPitch) {
        Frame frame=sample(tick);
        float entry=ease(Math.clamp(tick/12,0,1));
        return new Look(baseYaw+frame.yaw(),m(initialPitch,frame.pitch(),entry));
    }
    private static float ease(float t){return t*t*t*(t*(t*6-15)+10);}

    /** Non-uniform monotone Hermite spline: continuous velocity without overshooting the gestures. */
    private static float curve(float[] values,float tick){
        for(int i=1;i<LOOK_TIMES.length;i++) if(tick<=LOOK_TIMES[i]){
            float span=LOOK_TIMES[i]-LOOK_TIMES[i-1],t=(tick-LOOK_TIMES[i-1])/span;
            float t2=t*t,t3=t2*t;
            return (2*t3-3*t2+1)*values[i-1]+(t3-2*t2+t)*span*tangent(values,i-1)
                    +(-2*t3+3*t2)*values[i]+(t3-t2)*span*tangent(values,i);
        }
        return values[values.length-1];
    }
    private static float tangent(float[] values,int i){
        if(i==0||i==values.length-1)return 0;
        float before=LOOK_TIMES[i]-LOOK_TIMES[i-1],after=LOOK_TIMES[i+1]-LOOK_TIMES[i];
        float a=(values[i]-values[i-1])/before,b=(values[i+1]-values[i])/after;
        if(a*b<=0)return 0;
        float w1=2*after+before,w2=after+2*before;
        return (w1+w2)/(w1/a+w2/b);
    }
    private PsychosisIntroMotion() {}
}
