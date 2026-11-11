package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class SandevistanGravityMixin {
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void cyberneticenhancements$scaledGravity(CallbackInfoReturnable<Double> result) {
        result.setReturnValue(result.getReturnValueD() * SandevistanTimeDilation.gravityScale((Entity) (Object) this));
    }
}
