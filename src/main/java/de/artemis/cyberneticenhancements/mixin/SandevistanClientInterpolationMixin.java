package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class SandevistanClientInterpolationMixin {
    @ModifyVariable(method = "lerpTo", at = @At("HEAD"), argsOnly = true)
    private int cyberneticenhancements$scaledInterpolation(int steps) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return entity instanceof Mob && entity.level().isClientSide() && SandevistanTimeDilation.scale(entity) < 1 ? 1 : steps;
    }
}
