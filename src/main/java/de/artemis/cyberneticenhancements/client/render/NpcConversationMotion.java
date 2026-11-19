package de.artemis.cyberneticenhancements.client.render;

public final class NpcConversationMotion {
    private NpcConversationMotion() {}

    public record Pose(float headYaw, float headPitch, float headRoll, float bodyPitch, float bodyRoll,
                       float leftPitch, float leftRoll, float rightPitch, float rightRoll) {}

    public static Pose sample(double seconds, int identity, double replyAge) {
        double phase = Math.floorMod(identity, 997) / 997.0 * Math.PI * 2;
        float breath = (float) Math.sin(seconds * 1.6 + phase);
        float weight = (float) Math.sin(seconds * 0.43 + phase);
        double glanceTime = (seconds + phase * 2) % 14;
        float glance = pulse(glanceTime, 7, 2.4) * (identity % 2 == 0 ? 1 : -1);
        float speech = pulse(replyAge, 0, 3.2);
        float gesture = pulse(replyAge, 0.3, 1.7);
        float nod = pulse(replyAge, 0.15, 0.85) * 0.075F + (float) Math.sin(replyAge * 7) * speech * 0.025F;
        float emphasis = 0.75F + Math.floorMod(identity, 3) * 0.125F;
        return new Pose(weight * 0.018F + glance * 0.085F, breath * 0.007F + nod,
                weight * 0.012F + speech * 0.02F, breath * 0.008F + speech * 0.012F, weight * 0.018F,
                -gesture * 0.48F * emphasis, -gesture * 0.10F - breath * 0.009F,
                -gesture * 0.22F * emphasis, gesture * 0.055F + breath * 0.009F);
    }

    private static float pulse(double time, double start, double duration) {
        if (time <= start || time >= start + duration) return 0;
        double wave = Math.sin(Math.PI * (time - start) / duration);
        return (float) (wave * wave);
    }
}
