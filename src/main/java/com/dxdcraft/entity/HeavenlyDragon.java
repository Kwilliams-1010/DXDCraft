package com.dxdcraft.entity;

import com.dxdcraft.entity.projectile.DragonShotEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Boss. Circles its target in the air, breathes Dragon Shots, and dives for melee strikes.
 * Below half health it starts BOOSTING: faster, stronger, and fires volleys.
 */
public class HeavenlyDragon extends Monster {
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("Heavenly Dragon").withStyle(ChatFormatting.GOLD),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);

    private int shotCooldown = 60;
    private int meleeCooldown = 0;
    private int boostTimer = 0;
    private boolean enraged = false;

    public HeavenlyDragon(EntityType<? extends HeavenlyDragon> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (!enraged && getHealth() < getMaxHealth() / 2) {
            enraged = true;
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 1.2F);
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }

        LivingEntity target = getTarget();
        Vec3 goal;
        if (target != null && target.isAlive()) {
            boolean diving = (tickCount % 200) >= 150;
            if (diving) {
                goal = target.getEyePosition();
            } else {
                double angle = tickCount * 0.025;
                goal = target.position().add(Math.cos(angle) * 12.0, 7.0, Math.sin(angle) * 12.0);
            }
            faceTarget(target);
            attackLogic(target);
        } else {
            goal = position().add(0, Math.sin(tickCount * 0.05) * 0.5, 0);
        }

        double accel = enraged ? 0.09 : 0.06;
        Vec3 dir = goal.subtract(position());
        if (dir.lengthSqr() > 1.0) {
            setDeltaMovement(getDeltaMovement().add(dir.normalize().scale(accel)));
        }
        double max = enraged ? 1.0 : 0.75;
        Vec3 v = getDeltaMovement();
        if (v.length() > max) setDeltaMovement(v.normalize().scale(max));
    }

    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void attackLogic(LivingEntity target) {
        if (meleeCooldown > 0) meleeCooldown--;
        if (meleeCooldown <= 0 && distanceToSqr(target) < 16.0) {
            doHurtTarget(target);
            meleeCooldown = 20;
        }

        if (--shotCooldown <= 0) {
            int volley = enraged ? 3 : 1;
            for (int i = 0; i < volley; i++) shootAt(target, i * 4.0F);
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3.0F, 0.8F);
            shotCooldown = enraged ? 40 : 60;
        }

        if (enraged && ++boostTimer >= 200) {
            boostTimer = 0;
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            heal(10.0F);
            level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 4.0F, 0.5F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5, getZ(), 60, 1.5, 1.0, 1.5, 0.1);
            }
            for (ServerPlayer p : bossEvent.getPlayers()) {
                p.displayClientMessage(Component.literal("The dragon roars: BOOST!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            }
        }
    }

    private void shootAt(LivingEntity target, float spread) {
        DragonShotEntity shot = new DragonShotEntity(this, level());
        shot.setPos(getX(), getY() + getBbHeight() * 0.7, getZ());
        shot.setDamage(enraged ? 10.0F : 8.0F);
        Vec3 d = target.getEyePosition().subtract(shot.position());
        shot.shoot(d.x, d.y, d.z, 1.4F, 2.0F + spread);
        level().addFreshEntity(shot);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }

    @Override
    protected float getSoundVolume() { return 3.0F; }
}
