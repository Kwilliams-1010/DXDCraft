package com.dxdcraft.entity;

import com.dxdcraft.entity.projectile.DestructionOrbEntity;
import com.dxdcraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Underworld boss. A hulking devil lord: heavy melee, Power of Destruction barrages, and calls
 * Stray Devils to its side. Below half health it erupts in hellfire (fire ring + Strength II).
 */
public class UnderworldOverlord extends Monster {
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("Underworld Overlord").withStyle(ChatFormatting.DARK_RED),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);

    private int orbCooldown = 60;
    private int summonCooldown = 200;
    private boolean enraged = false;

    public UnderworldOverlord(EntityType<? extends UnderworldOverlord> type, Level level) {
        super(type, level);
        xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel server)) return;

        if (!enraged && getHealth() < getMaxHealth() / 2) {
            enraged = true;
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            server.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.6F);
            for (ServerPlayer p : bossEvent.getPlayers()) {
                p.displayClientMessage(Component.literal("The Overlord erupts in hellfire!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
        if (enraged && tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4.0))) p.igniteForSeconds(4.0F);
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                server.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 4, getY() + 0.2, getZ() + Math.sin(a) * 4, 1, 0, 0.1, 0, 0.01);
            }
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (--orbCooldown <= 0 && distanceToSqr(target) > 9.0) {
            int volley = enraged ? 5 : 3;
            for (int i = 0; i < volley; i++) {
                DestructionOrbEntity orb = new DestructionOrbEntity(this, level());
                orb.setDamage(enraged ? 12.0F : 9.0F);
                Vec3 d = target.getEyePosition().subtract(orb.position());
                orb.shoot(d.x, d.y, d.z, 1.3F, 6.0F);
                level().addFreshEntity(orb);
            }
            playSound(SoundEvents.WITHER_SHOOT, 2.0F, 0.6F);
            orbCooldown = enraged ? 50 : 80;
        }
        if (--summonCooldown <= 0) {
            summonCooldown = enraged ? 240 : 320;
            long nearby = server.getEntitiesOfClass(StrayDevil.class, getBoundingBox().inflate(24)).size();
            for (int i = 0; i < 3 - nearby && i < 2; i++) {
                StrayDevil minion = ModEntities.STRAY_DEVIL.get().create(server);
                if (minion == null) continue;
                minion.moveTo(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, 0, 0);
                minion.setTarget(target);
                server.addFreshEntity(minion);
                server.sendParticles(ParticleTypes.CRIMSON_SPORE, minion.getX(), minion.getY() + 1, minion.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            }
            server.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.6F);
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
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
}
