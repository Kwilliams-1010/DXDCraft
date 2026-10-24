package com.dxdcraft.entity.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Straight-flying spell projectile with a damage value and a short lifetime. */
public abstract class MagicProjectile extends ThrowableItemProjectile {
    protected float damage = 6.0F;

    protected MagicProjectile(EntityType<? extends MagicProjectile> type, Level level) {
        super(type, level);
    }

    protected MagicProjectile(EntityType<? extends MagicProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    public MagicProjectile setDamage(float damage) {
        this.damage = damage;
        return this;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    protected abstract ParticleOptions trailParticle();

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        // Pass through players you aren't allowed to hurt (PvP off, teammates, your pets).
        return super.canHitEntity(target) && com.dxdcraft.logic.Combat.canHarm(getOwner(), target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(trailParticle(), getX(), getY() + 0.1, getZ(), 0, 0, 0);
        } else if (tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
