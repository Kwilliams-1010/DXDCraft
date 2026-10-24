package com.dxdcraft.entity.projectile;

import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Dragon Shot: released Boosted Gear power, or the dragon boss's breath. Small area blast, no block damage. */
public class DragonShotEntity extends MagicProjectile {
    public DragonShotEntity(EntityType<? extends DragonShotEntity> type, Level level) {
        super(type, level);
    }

    public DragonShotEntity(LivingEntity shooter, Level level) {
        super(ModEntities.DRAGON_SHOT.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DRAGON_SHOT.get();
    }

    @Override
    protected ParticleOptions trailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Never hit whoever fired it (the boss would otherwise hit itself).
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().indirectMagic(this, getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide) {
            // Splash damage to everyone nearby except the shooter.
            AABB box = getBoundingBox().inflate(2.5);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == getOwner() || !com.dxdcraft.logic.Combat.canHarm(getOwner(), e)) continue;
                if (result instanceof EntityHitResult ehr && ehr.getEntity() == e) continue;
                e.hurt(damageSources().indirectMagic(this, getOwner()), damage * 0.5F);
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 40, 0.8, 0.8, 0.8, 0.1);
                server.playSound(null, blockPosition(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
        super.onHit(result);
    }
}
