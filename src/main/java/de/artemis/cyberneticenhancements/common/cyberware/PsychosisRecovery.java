package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.PsychosisIntroPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PsychosisRecovery {
    public static final String START="cyberneticenhancements.psychosis_recovery_start",YAW=START+"_yaw",PITCH=START+"_pitch";
    public static boolean pending(Player p){return p.getPersistentData().contains(START);}
    public static boolean active(Player p){return pending(p)&&p.level().getGameTime()-p.getPersistentData().getLong(START)<PsychosisRecoveryMotion.DURATION;}
    private static PsychosisIntroPayload payload(Player p){var t=p.getPersistentData();return new PsychosisIntroPayload(p.getUUID(),t.getLong(START),t.getFloat(YAW),t.getFloat(PITCH),true);}
    public static void begin(ServerPlayer p){
        if(!p.isAlive())return;
        p.stopUsingItem();p.setSprinting(false);p.removeEffect(MobEffects.CONFUSION);
        CyberstrainManager.maintainEpisodeBlindness(p);
        var t=p.getPersistentData();t.putLong(START,p.level().getGameTime());t.putFloat(YAW,p.getYRot());t.putFloat(PITCH,p.getXRot());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,payload(p));
    }
    public static boolean tick(ServerPlayer p){
        if(!pending(p))return false;
        if(de.artemis.cyberneticenhancements.common.world.TraumaCoverage.downed(p)){
            cancel(p);CyberstrainManager.syncControlLock(p);return false;
        }
        var t=p.getPersistentData();
        if(!active(p)){
            var look=PsychosisRecoveryMotion.look(PsychosisRecoveryMotion.DURATION,t.getFloat(YAW),t.getFloat(PITCH));
            p.setYRot(look.yaw());p.setXRot(look.pitch());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new PsychosisIntroPayload(p.getUUID(),-1,0,0,true));
            clearData(p);p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.CONFUSION);
            CyberstrainManager.syncControlLock(p);return false;
        }
        CyberstrainManager.maintainEpisodeBlindness(p);
        var look=PsychosisRecoveryMotion.look(p.level().getGameTime()-t.getLong(START),t.getFloat(YAW),t.getFloat(PITCH));
        p.setYRot(look.yaw());p.setXRot(look.pitch());p.setYHeadRot(look.yaw());p.setYBodyRot(t.getFloat(YAW));
        PsychosisIntro.hold(p);return true;
    }
    public static void cancel(ServerPlayer p){
        if(pending(p)){
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new PsychosisIntroPayload(p.getUUID(),-1,0,0,true));
            p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.CONFUSION);
        }
        clearData(p);
    }
    private static void clearData(Player p){var t=p.getPersistentData();t.remove(START);t.remove(YAW);t.remove(PITCH);}
    public static void sync(ServerPlayer p){if(active(p))PacketDistributor.sendToPlayer(p,payload(p));}
    public static void tracking(PlayerEvent.StartTracking event){if(event.getEntity() instanceof ServerPlayer viewer&&event.getTarget() instanceof ServerPlayer p&&active(p))PacketDistributor.sendToPlayer(viewer,payload(p));}
    private PsychosisRecovery(){}
}
