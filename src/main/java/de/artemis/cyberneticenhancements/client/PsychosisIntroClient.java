package de.artemis.cyberneticenhancements.client;

import java.util.*;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.cyberware.PsychosisIntroMotion;
import de.artemis.cyberneticenhancements.common.cyberware.PsychosisRecoveryMotion;
import de.artemis.cyberneticenhancements.common.network.PsychosisIntroPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class PsychosisIntroClient {
    private static final Map<UUID,PsychosisIntroPayload> INTROS=new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static boolean recoveryFinished;
    private static void level(){var current=Minecraft.getInstance().level;if(level!=current){if(level!=null)INTROS.clear();level=current;}}
    public static void clear(){INTROS.clear();level=null;recoveryFinished=false;}
    public static void receive(PsychosisIntroPayload p){
        level();
        if(p.started()<0){
            var previous=INTROS.remove(p.player());
            if(previous!=null&&level!=null)handoff(previous,level.getGameTime()+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)-previous.started());
        }else {
            var player=Minecraft.getInstance().player;
            if(player!=null&&player.getUUID().equals(p.player()))recoveryFinished=false;
            INTROS.put(p.player(),p);
        }
    }
    public record View(float baseYaw,float yaw,float pitch,PsychosisIntroMotion.Frame pose){}
    public static View view(LivingEntity entity,float partial){
        level();if(level==null||entity==null||!entity.isAlive())return null;
        var p=INTROS.get(entity.getUUID());if(p==null)return null;
        float elapsed=level.getGameTime()+partial-p.started();
        if(elapsed>=duration(p)){
            INTROS.remove(entity.getUUID());
            handoff(p,duration(p));
            return null;
        }
        var frame=p.recovery()?PsychosisRecoveryMotion.sample(Math.max(0,elapsed)):PsychosisIntroMotion.sample(Math.max(0,elapsed));
        var look=look(p,Math.max(0,elapsed));
        return new View(p.yaw(),look.yaw(),look.pitch(),frame);
    }
    private static int duration(PsychosisIntroPayload p){return p.recovery()?PsychosisRecoveryMotion.DURATION:PsychosisIntroMotion.DURATION;}
    private static PsychosisIntroMotion.Look look(PsychosisIntroPayload p,float elapsed){return p.recovery()?PsychosisRecoveryMotion.look(elapsed,p.yaw(),p.pitch()):PsychosisIntroMotion.look(elapsed,p.yaw(),p.pitch());}
    private static void handoff(PsychosisIntroPayload p,float elapsed){
        var player=Minecraft.getInstance().player;
        if(player==null||!player.isAlive()||!player.getUUID().equals(p.player()))return;
        if(p.recovery())recoveryFinished=true;
        var look=look(p,Math.clamp(elapsed,0,duration(p)));
        de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState.alignView(look.yaw(),look.pitch());
        player.setYRot(look.yaw());player.yRotO=look.yaw();
        player.setXRot(look.pitch());player.xRotO=look.pitch();
        player.setYHeadRot(look.yaw());player.yHeadRotO=look.yaw();
        player.setYBodyRot(p.yaw());player.yBodyRotO=p.yaw();
    }
    public static boolean local(){var mc=Minecraft.getInstance();return view(mc.player,0)!=null;}
    public static float buildup(){
        level();var mc=Minecraft.getInstance();if(level==null||mc.player==null)return 0;
        var intro=INTROS.get(mc.player.getUUID());
        if(intro==null)return recoveryFinished?0:1;
        float elapsed=level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false)-intro.started();
        return intro.recovery()?PsychosisRecoveryMotion.intensity(elapsed):PsychosisIntroMotion.buildup(elapsed);
    }
    public static void pose(HumanoidModel<?> model,LivingEntity entity,float partial){
        var v=view(entity,partial);if(v==null)return;var f=v.pose();
        model.head.xRot=(float)Math.toRadians(v.pitch());model.head.yRot=(float)Math.toRadians(v.yaw()-v.baseYaw());
        model.body.xRot=f.lean();
        model.rightArm.setRotation(f.rightX(),f.rightY(),f.rightZ());
        model.leftArm.setRotation(f.leftX(),f.leftY(),f.leftZ());
        model.hat.copyFrom(model.head);
        if(model instanceof PlayerModel<?> player){player.rightSleeve.copyFrom(model.rightArm);player.leftSleeve.copyFrom(model.leftArm);player.jacket.copyFrom(model.body);}
    }
    public static void hands(RenderHandEvent event){
        var mc=Minecraft.getInstance();var v=view(mc.player,event.getPartialTick());if(v==null)return;
        event.setCanceled(true);if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        if(!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer))return;
        var model=renderer.getModel();
        var parts=List.of(model.rightArm,model.leftArm,model.rightSleeve,model.leftSleeve,model.head,model.hat,model.body,model.jacket);
        var saved=parts.stream().map(net.minecraft.client.model.geom.ModelPart::storePose).toList();
        var visible=parts.stream().map(p->p.visible).toList();
        var stack=event.getPoseStack();stack.pushPose();
        try{
            pose(model,mc.player,event.getPartialTick());
            // Same anatomical arm pose as third person, transformed from body space into camera space.
            stack.mulPose(Axis.XP.rotationDegrees(v.pitch()));
            stack.mulPose(Axis.YP.rotationDegrees(v.yaw()-v.baseYaw()));
            stack.translate(0,-mc.player.getEyeHeight(),0);
            stack.scale(-1,-1,1);stack.translate(0,-1.501F,0);
            var pixels=event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(mc.player.getSkin().texture()));
            for(var part:List.of(model.rightArm,model.leftArm,model.rightSleeve,model.leftSleeve)){
                part.visible=true;part.render(stack,pixels,event.getPackedLight(),OverlayTexture.NO_OVERLAY);
            }
        }finally{for(int i=0;i<parts.size();i++){parts.get(i).loadPose(saved.get(i));parts.get(i).visible=visible.get(i);}stack.popPose();}
    }
    private PsychosisIntroClient(){}
}
