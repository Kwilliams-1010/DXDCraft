package com.dxdcraft.entity.projectile;

import com.dxdcraft.logic.DxDLogic;
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
import net.minecraft.world.phys.EntityHitResult;

/** Spear of light. Deals double damage to devils and the undead. */
public class LightSpearEntity extends MagicProjectile {
    public LightSpearEntity(EntityType<? extends LightSpearEntity> type, Level level) {
        super(type, level);
    }

    public LightSpearEntity(LivingEntity shooter, Level level) {
        super(ModEntities.LIGHT_SPEAR.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LIGHT_SPEAR.get();
    }

    @Override
    protected ParticleOptions trailParticle() {
        return ParticleTypes.END_ROD;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target == getOwner()) return;
        float dmg = damage;
        if (target instanceof LivingEntity living && (DxDLogic.isDevil(living) || living.isInvertedHealAndHarm())) {
            dmg *= 2.0F;
        }
        target.hurt(damageSources().indirectMagic(this, getOwner()), dmg);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 15, 0.2, 0.2, 0.2, 0.1);
            server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
    }
}
