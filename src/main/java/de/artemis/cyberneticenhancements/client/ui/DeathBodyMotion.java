package de.artemis.cyberneticenhancements.client.ui;

/** One feet-pivoted collapse, shared by the body and its first-person eyes. */
public final class DeathBodyMotion {
    public static final int HOLD_TICKS = de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.HOLD_TICKS,
            DISSOLVE_TICKS = de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime.DISSOLVE_TICKS;
    public static final double IMPACT_SECONDS = 0.46, FALL_SECONDS = 0.82, RECOVERY_TICKS = 17;

    public static Pose sample(double seconds, boolean reducedMotion) {
        double time = reducedMotion ? FALL_SECONDS : Math.max(0, seconds);
        // Gravity-like acceleration: commit to the fall, hit quickly, then settle.
        double progress = Math.pow(Math.clamp(time / IMPACT_SECONDS, 0, 1), 2.2);
        double rebound = Math.abs(rebound(time));
        double settling = Math.clamp((time - IMPACT_SECONDS) / (FALL_SECONDS - IMPACT_SECONDS), 0, 1);
        settling = settling * settling * (3 - 2 * settling);
        return new Pose(progress * Math.PI / 2 - rebound * 0.055,
                progress * 0.14 + rebound * 0.045, progress, settling, 0);
    }

    public static double recoverySeconds(double elapsedTicks) {
        double t = Math.clamp(elapsedTicks / RECOVERY_TICKS, 0, 1);
        double remaining = 1 - t * t * (3 - 2 * t);
        return IMPACT_SECONDS * Math.pow(remaining, 1 / 2.2);
    }

    private static double rebound(double seconds) {
        double t = (seconds - IMPACT_SECONDS) / (FALL_SECONDS - IMPACT_SECONDS);
        return t <= 0 || t >= 1 ? 0 : Math.sin(t * Math.PI * 2) * Math.exp(-t * 4) * (1 - t);
    }

    /** Chosen once per death, so the limbs never jitter between frames. */
    public static RestPose restPose(long seed) {
        var random = new java.util.SplittableRandom(seed);
        return new RestPose(new Joint(random.nextDouble(-0.08, 0.08), random.nextDouble(-0.4, 0.4), random.nextDouble(-0.17, 0.17)),
                new Joint(random.nextDouble(-0.23, 0.15), random.nextDouble(-0.2, 0.2), random.nextDouble(0.15, 0.7)),
                new Joint(random.nextDouble(-0.23, 0.15), random.nextDouble(-0.2, 0.2), -random.nextDouble(0.15, 0.7)),
                new Joint(random.nextDouble(-0.13, 0.17), random.nextDouble(-0.12, 0.12), random.nextDouble(0.025, 0.22)),
                new Joint(random.nextDouble(-0.13, 0.17), random.nextDouble(-0.12, 0.12), -random.nextDouble(0.025, 0.22)));
    }

    public record Joint(double pitch, double yaw, double roll) {
        private Joint blend(double amount, double kick) {
            return new Joint(pitch * amount + kick, yaw * amount, roll * amount);
        }
    }

    public record RestPose(Joint head, Joint rightArm, Joint leftArm, Joint rightLeg, Joint leftLeg) {
        public RestPose sample(double seconds, boolean reducedMotion) {
            var motion = DeathBodyMotion.sample(seconds, reducedMotion);
            double p = motion.progress();
            double impact = reducedMotion ? 0 : rebound(seconds);
            double flail = p >= 1 ? 0 : Math.sin(p * Math.PI);
            return new RestPose(head.blend(p, impact * 0.07),
                    rightArm.blend(p, -flail * 0.65 + impact * 0.22),
                    leftArm.blend(p, -flail * 0.4 - impact * 0.16),
                    rightLeg.blend(p, flail * 0.14 - impact * 0.12),
                    leftLeg.blend(p, -flail * 0.08 + impact * 0.09));
        }
    }

    public static float visibility(double deadTicks) {
        return (float) Math.clamp(1 - (deadTicks - HOLD_TICKS) / DISSOLVE_TICKS, 0, 1);
    }

    public record Pose(double angle, double lift, double progress, double settling, double roll) {
        public Pose forDirection(float bodyYaw, float fallYaw) {
            double difference = Math.toRadians(fallYaw - bodyYaw);
            difference = Math.atan2(Math.sin(difference), Math.cos(difference));
            // Roll around the body's length after impact. Its head still points away from the hit.
            double target = Math.cos(difference) >= 0 ? -difference : Math.copySign(Math.PI, difference) - difference;
            double roll = settling == 0 ? 0 : target * settling;
            double sideways = Math.abs(Math.sin(difference + roll));
            return new Pose(angle, lift + sideways * Math.sin(angle) * 0.14, progress, settling, roll);
        }

        public double spread(float bodyYaw, float fallYaw) {
            return Math.abs(Math.cos(Math.toRadians(fallYaw - bodyYaw) + roll));
        }

        public double headOffset(float bodyYaw, float fallYaw) {
            return -1.76 * Math.cos(Math.toRadians(fallYaw - bodyYaw) + roll) * progress;
        }

        public double eyeHeight(double standingEyeHeight) { return lift + Math.cos(angle) * standingEyeHeight; }
        public double eyeBackward(double standingEyeHeight) { return Math.sin(angle) * standingEyeHeight; }

        public org.joml.Quaternionf rotation(float fallYaw) {
            double yaw = Math.toRadians(fallYaw);
            return new org.joml.Quaternionf().rotationAxis((float) angle, (float) -Math.cos(yaw), 0, (float) -Math.sin(yaw))
                    .rotateY((float) roll);
        }

        /** Rotate the original view with the torso, including sideways falls. */
        public View view(float fallYaw, float originalYaw, float originalPitch) {
            double yaw = Math.toRadians(originalYaw), pitch = Math.toRadians(originalPitch);
            var forward = new org.joml.Vector3f((float) (-Math.sin(yaw) * Math.cos(pitch)),
                    (float) -Math.sin(pitch), (float) (Math.cos(yaw) * Math.cos(pitch)));
            var up = new org.joml.Vector3f((float) (-Math.sin(yaw) * Math.sin(pitch)),
                    (float) Math.cos(pitch), (float) (Math.cos(yaw) * Math.sin(pitch)));
            var rotation = rotation(fallYaw);
            rotation.transform(forward); rotation.transform(up);
            float viewYaw = forward.x * forward.x + forward.z * forward.z < 0.000001 ? originalYaw
                    : (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
            float viewPitch = (float) -Math.toDegrees(Math.asin(Math.clamp(forward.y, -1, 1)));
            double viewRadians = Math.toRadians(viewYaw);
            var left = new org.joml.Vector3f((float) Math.cos(viewRadians), 0, (float) Math.sin(viewRadians));
            var expectedUp = new org.joml.Vector3f(forward).cross(left).normalize();
            float roll = (float) -Math.toDegrees(Math.atan2(up.dot(left), up.dot(expectedUp)));
            return new View(viewYaw, viewPitch, roll);
        }
    }
    public record View(float yaw, float pitch, float roll) { }
    private DeathBodyMotion() { }
}
