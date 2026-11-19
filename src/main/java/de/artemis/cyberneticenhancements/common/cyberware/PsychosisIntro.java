package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.PsychosisIntroPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PsychosisIntro {
    public static final String START="cyberneticenhancements.psychosis_intro_start", YAW=START+"_yaw", PITCH=START+"_pitch";
    public static final String READY=START+"_pursuit_ready";
    public static boolean pending(Player p){return p.getPersistentData().contains(START);}
    public static boolean active(Player p){return pending(p)&&p.level().getGameTime()-p.getPersistentData().getLong(START)<PsychosisIntroMotion.DURATION;}
    private static PsychosisIntroPayload payload(Player p){var t=p.getPersistentData();return new PsychosisIntroPayload(p.getUUID(),t.getLong(START),t.getFloat(YAW),t.getFloat(PITCH));}
    public static void begin(ServerPlayer p){
        p.stopUsingItem();p.setSprinting(false);p.setShiftKeyDown(false);
        var t=p.getPersistentData();t.putLong(START,p.level().getGameTime());t.putFloat(YAW,p.getYRot());t.putFloat(PITCH,p.getXRot());
        t.putLong(READY,p.level().getGameTime()+PsychosisIntroMotion.DURATION+PsychosisIntroMotion.SETTLE_TICKS);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,payload(p));
    }
    public static boolean tick(ServerPlayer p){
        if(!pending(p))return false;
        if(!active(p)) {
            var look=PsychosisIntroMotion.look(PsychosisIntroMotion.DURATION,p.getPersistentData().getFloat(YAW),p.getPersistentData().getFloat(PITCH));
            p.setYRot(look.yaw());p.setXRot(look.pitch());
            clearData(p); CyberwareEffects.refreshPlayerCyberware(p);return false;
        }
        var t=p.getPersistentData();float elapsed=p.level().getGameTime()-t.getLong(START);
        CyberstrainManager.maintainEpisodeBlindness(p);
        var look=PsychosisIntroMotion.look(elapsed,t.getFloat(YAW),t.getFloat(PITCH));
        float pitch=look.pitch(),yaw=look.yaw();
        p.setYRot(yaw);p.setXRot(pitch);p.setYHeadRot(yaw);p.setYBodyRot(t.getFloat(YAW));
        hold(p);
        return true;
    }
    public static boolean settling(Player p){return p.getPersistentData().getLong(READY)>p.level().getGameTime();}
    public static void hold(ServerPlayer p){
        var motion=p.getDeltaMovement();p.setDeltaMovement(0,motion.y,0);p.setSprinting(false);p.setJumping(false);
        p.hasImpulse=true;p.hurtMarked=true;
        PacketDistributor.sendToPlayer(p,new de.artemis.cyberneticenhancements.common.network.CyberpsychosisControlPayload(true,p.getYRot(),p.getXRot(),0,0,false,false));
    }
    public static void cancel(ServerPlayer p){if(pending(p))PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new PsychosisIntroPayload(p.getUUID(),-1,0,0));clearData(p);p.getPersistentData().remove(READY);}
    private static void clearData(Player p){var t=p.getPersistentData();t.remove(START);t.remove(YAW);t.remove(PITCH);}
    public static void tracking(PlayerEvent.StartTracking event){if(event.getEntity() instanceof ServerPlayer viewer&&event.getTarget() instanceof ServerPlayer p&&active(p))PacketDistributor.sendToPlayer(viewer,payload(p));}
    public static void sync(ServerPlayer p){if(active(p))PacketDistributor.sendToPlayer(p,payload(p));}
    private PsychosisIntro(){}
}
