package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.PsychosisIntroClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class PsychosisIntroCameraMixin {
    @Shadow protected abstract void setRotation(float yaw,float pitch,float roll);
    @Inject(method="setup",at=@At("TAIL"))
    private void psychosis$camera(BlockGetter level,Entity entity,boolean detached,boolean reverse,float partial,CallbackInfo ci){
        if(detached||!(entity instanceof LivingEntity living))return;
        var view=PsychosisIntroClient.view(living,partial);
        if(view!=null)setRotation(view.yaw(),view.pitch(),0);
    }
}
