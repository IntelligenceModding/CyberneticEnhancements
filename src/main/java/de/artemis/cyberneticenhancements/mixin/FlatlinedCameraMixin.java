package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.FlatlinedDeathCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class FlatlinedCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);

    @ModifyVariable(method = "setup", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean cyberneticenhancements$firstPerson(boolean detached, BlockGetter level, Entity entity, boolean originalDetached, boolean reverse, float partialTick) {
        if (de.artemis.cyberneticenhancements.client.render.DeathBodies.enabled()) return detached;
        return FlatlinedDeathCamera.active(entity) ? false : detached;
    }

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$fallBack(BlockGetter level, Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
        if (!FlatlinedDeathCamera.active(entity)) return;
        if (de.artemis.cyberneticenhancements.client.render.DeathBodies.enabled() && detached) return;
        var view = FlatlinedDeathCamera.view(partialTick);
        setPosition(view.position());
        setRotation(view.yaw(), view.pitch(), view.roll());
    }
}
