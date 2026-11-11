package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanTimeDilation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Projectile.class)
abstract class SandevistanProjectileImpactMixin {
    @WrapMethod(method = "onHit")
    private void cyberneticenhancements$preserveImpact(HitResult hit, Operation<Void> original) {
        Projectile projectile = (Projectile) (Object) this;
        SandevistanTimeDilation.beginHit(projectile, hit);
        try {
            original.call(hit);
        } finally {
            SandevistanTimeDilation.endImpact(projectile);
        }
    }

    @WrapMethod(method = "deflect")
    private boolean cyberneticenhancements$preserveDeflection(ProjectileDeflection deflection, Entity entity,
                                                              Entity owner, boolean byPlayer, Operation<Boolean> original) {
        Projectile projectile = (Projectile) (Object) this;
        SandevistanTimeDilation.beginImpact(projectile);
        try {
            return original.call(deflection, entity, owner, byPlayer);
        } finally {
            SandevistanTimeDilation.endImpact(projectile);
        }
    }
}
