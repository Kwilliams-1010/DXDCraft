package com.dxdcraft.entity;

import com.dxdcraft.entity.projectile.LightSpearEntity;
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
 * Heaven boss. Hovers above its foe hurling volleys of Light Spears; every 10 seconds it calls
 * down Judgment (a beam that smites and lifts everyone near it). Below half health it raises a
 * Sanctuary shield (Resistance + healing) and fires twice as many spears.
 */
public class SeraphGuardian extends Monster {
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("Seraph Guardian").withStyle(ChatFormatting.GOLD),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);

    private int spearCooldown = 40;
    private int judgmentTimer = 0;
    private boolean ascended = false;

    public SeraphGuardian(EntityType<? extends SeraphGuardian> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!ascended && getHealth() < getMaxHealth() / 2) {
            ascended = true;
            bossEvent.setColor(BossEvent.BossBarColor.WHITE);
            level().playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.7F);
            for (ServerPlayer p : bossEvent.getPlayers()) {
                p.displayClientMessage(Component.literal("The Seraph raises its Sanctuary!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
        }
        if (ascended && tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
            heal(2.0F);
        }

        LivingEntity target = getTarget();
        Vec3 goal;
        if (target != null && target.isAlive()) {
            double a = tickCount * 0.03;
            goal = target.position().add(Math.cos(a) * 8, 5.0, Math.sin(a) * 8);
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;

            if (--spearCooldown <= 0) {
                int count = ascended ? 6 : 3;
                for (int i = 0; i < count; i++) {
                    LightSpearEntity spear = new LightSpearEntity(this, level());
                    spear.setDamage(ascended ? 9.0F : 7.0F);
                    Vec3 d = target.getEyePosition().subtract(spear.position());
                    spear.shoot(d.x, d.y, d.z, 1.6F, 4.0F + i * 2.0F);
                    level().addFreshEntity(spear);
                }
                playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 1.4F);
                spearCooldown = ascended ? 30 : 45;
            }
            if (++judgmentTimer >= 200) {
                judgmentTimer = 0;
                judgment(target);
            }
        } else {
            goal = position().add(0, Math.sin(tickCount * 0.05) * 0.3, 0);
        }
        Vec3 dir = goal.subtract(position());
        if (dir.lengthSqr() > 1.0) setDeltaMovement(getDeltaMovement().add(dir.normalize().scale(0.05)));
        Vec3 v = getDeltaMovement();
        if (v.length() > 0.6) setDeltaMovement(v.normalize().scale(0.6));
    }

    private void judgment(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 at = target.position();
        for (double y = 0; y < 12; y += 0.5) server.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 3, 0.3, 0.1, 0.3, 0.01);
        server.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (Player p : server.getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(4.0))) {
            p.hurt(damageSources().indirectMagic(this, this), ascended ? 12.0F : 8.0F);
            p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
        }
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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.BEACON_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.BEACON_DEACTIVATE; }
}
