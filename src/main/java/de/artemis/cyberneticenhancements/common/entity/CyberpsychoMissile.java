package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class CyberpsychoMissile extends ThrowableItemProjectile {
    public CyberpsychoMissile(EntityType<? extends CyberpsychoMissile> type, Level level) { super(type, level); }
    public CyberpsychoMissile(Level level, LivingEntity owner) {
        super(ModEntityTypes.CYBERPSYCHO_MISSILE.get(), owner, level);
        setNoGravity(true);
        setPos(owner.getX(), owner.getEyeY() - 0.3, owner.getZ());
    }
    @Override protected Item getDefaultItem() { return Items.FIRE_CHARGE; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.03, 0.03, 0.03, 0);
        if (tickCount > 80) discard();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        var point = hit.getLocation();
        server.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 18, 0.6, 0.4, 0.6, 0.03);
        server.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.1F);
        for (var target : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(3), player -> !player.isCreative() && !player.isSpectator())) {
            double distance = target.getBoundingBox().getCenter().distanceTo(point);
            if (distance > 3 || !getSensingLine(target)) continue;
            float damage = (float) (4 + (1 - distance / 3) * 8);
            target.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity owner ? owner : null), damage);
            target.knockback(0.7, point.x - target.getX(), point.z - target.getZ());
        }
        discard();
    }

    private boolean getSensingLine(LivingEntity target) {
        return level().clip(new net.minecraft.world.level.ClipContext(position(), target.getBoundingBox().getCenter(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
}
