package com.dxdcraft.block;

import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AngelicWardBlockEntity extends BlockEntity {
    public static final double RADIUS = 16.0;
    private int timer;

    public AngelicWardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ANGELIC_WARD_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AngelicWardBlockEntity be) {
        if (++be.timer < 20) return;
        be.timer = 0;
        if (!(level instanceof ServerLevel server)) return;
        Vec3 c = Vec3.atCenterOf(pos);
        boolean hit = false;
        for (Mob m : server.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(RADIUS), e -> e instanceof Enemy && e.isAlive())) {
            boolean weak = m.isInvertedHealAndHarm() || DxDLogic.isDevil(m);
            m.hurt(server.damageSources().magic(), weak ? 8.0F : 4.0F);
            Vec3 push = m.position().subtract(c).normalize().scale(0.8);
            m.push(push.x, 0.25, push.z);
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            server.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 4, 0.2, 0.3, 0.2, 0.02);
            hit = true;
        }
        if (hit) server.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.6F);
        if (server.random.nextInt(4) == 0) {
            server.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.8, c.z, 3, 0.2, 0.3, 0.2, 0.01);
        }
    }
}
