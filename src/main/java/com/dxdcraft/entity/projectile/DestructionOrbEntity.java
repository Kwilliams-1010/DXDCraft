package com.dxdcraft.entity.projectile;

import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Power of Destruction. Hits hard, withers the target, and erases the soft block it lands on
 * (no drops; never touches blocks harder than stone-ish or unbreakable ones).
 */
public class DestructionOrbEntity extends MagicProjectile {
    public DestructionOrbEntity(EntityType<? extends DestructionOrbEntity> type, Level level) {
        super(type, level);
    }

    public DestructionOrbEntity(LivingEntity shooter, Level level) {
        super(ModEntities.DESTRUCTION_ORB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DESTRUCTION_ORB.get();
    }

    @Override
    protected ParticleOptions trailParticle() {
        return ParticleTypes.CRIMSON_SPORE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target == getOwner()) return;
        target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }
        burst();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) return;
        BlockPos pos = result.getBlockPos();
        BlockState state = level().getBlockState(pos);
        float hardness = state.getDestroySpeed(level(), pos);
        boolean allowed = com.dxdcraft.DxDConfig.get(com.dxdcraft.DxDConfig.DESTRUCTION_BREAKS_BLOCKS::get, true)
                && getOwner() instanceof Player p && p.mayBuild() && level().mayInteract(p, pos);
        if (allowed && hardness >= 0 && hardness <= 3.0F && !state.hasBlockEntity()) {
            // Let claim/protection mods veto it, exactly like a player breaking the block.
            var breakEvent = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level(), pos, state, (Player) getOwner());
            if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(breakEvent).isCanceled()) {
                level().destroyBlock(pos, false, getOwner());
            }
        }
        burst();
    }

    private void burst() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            server.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY(), getZ(), 40, 0.5, 0.5, 0.5, 0.1);
            server.playSound(null, blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }
}
