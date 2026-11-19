package de.artemis.cyberneticenhancements.client.render;

import de.artemis.cyberneticenhancements.common.entity.NpcCombatHealing;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/** Smooth, reversible healing colors for overhead health bars, plus shared color blending. */
public final class NpcHealingColors {
    private static final double FADE_TICKS = 7;
    private static final Map<LivingEntity, Transition> TRANSITIONS = new WeakHashMap<>();

    private NpcHealingColors() {}

    public static float amount(LivingEntity entity, float partialTick) {
        var healing = NpcCombatHealing.of(entity);
        if (healing == null) return 0;
        return TRANSITIONS.computeIfAbsent(entity, ignored -> new Transition())
                .sample(healing.highlighted(), entity.tickCount + (double) partialTick);
    }

    public static int blend(int red, int green, float amount) {
        return FastColor.ARGB32.lerp(amount, red, green);
    }

    private static final class Transition {
        private boolean green;
        private float from;
        private double started;

        float sample(boolean target, double now) {
            float progress = (float) Math.clamp((now - started) / FADE_TICKS, 0, 1);
            float eased = progress * progress * (3 - 2 * progress);
            float value = from + ((green ? 1 : 0) - from) * eased;
            if (green != target) {
                from = value;
                green = target;
                started = now;
            }
            return value;
        }
    }
}
