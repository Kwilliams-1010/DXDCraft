package com.dxdcraft.entity.projectile;

import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Thrown flask of holy water. Burns devils and undead, soothes everyone else. */
public class HolyWaterEntity extends ThrowableItemProjectile {
    public HolyWaterEntity(EntityType<? extends HolyWaterEntity> type, Level level) {
        super(type, level);
    }

    public HolyWaterEntity(LivingEntity shooter, Level level) {
        super(ModEntities.HOLY_WATER.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.HOLY_WATER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0, 2.0, 3.0))) {
            boolean mayHurt = com.dxdcraft.logic.Combat.canHarm(getOwner(), e);
            if (DxDLogic.isDevil(e) && mayHurt) {
                e.hurt(damageSources().indirectMagic(this, getOwner()), 8.0F);
            } else if (e.isInvertedHealAndHarm() && mayHurt) {
                e.hurt(damageSources().indirectMagic(this, getOwner()), 5.0F);
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 40, 1.0, 0.3, 1.0, 0.1);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 15, 1.0, 0.5, 1.0, 0.02);
            server.playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        discard();
    }
}
