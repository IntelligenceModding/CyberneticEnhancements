package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

/** Continuous visual strength, independent of status-effect countdowns and network refreshes. */
public final class PsychosisBlindnessVisual {
    public static boolean active() {
        var mc=Minecraft.getInstance();
        return CyberpsychosisClientState.isControlLocked()&&mc.player!=null&&mc.player.isAlive()
                &&mc.getCameraEntity()==mc.player&&mc.player.hasEffect(MobEffects.BLINDNESS);
    }
    public static float strength(float vanilla) {return active()?vanilla*PsychosisPresentation.amount():vanilla;}
    private PsychosisBlindnessVisual(){}
}
