package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Element casts ([R] / Sneak+[R]) and passives. Damage scales +2% per DxD level. */
public final class ElementCasting {
    private ElementCasting() {}

    /** slot 0 = first element, slot 1 = second element. */
    public static void cast(ServerPlayer p, int slot) {
        DxDData d = DxDLogic.data(p);
        Element e = slot == 0 ? d.getElement1() : d.getElement2();
        if (e == null) {
            String msg = d.getElement1() == null
                    ? (d.hasInnateGear() ? "Your power is a Sacred Gear, not an element." : "You have no elemental affinity.")
                    : "You don't have a second element yet.";
            p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = p.level().getGameTime();
        if (now < d.getElementReadyAt(slot) && !p.getAbilities().instabuild) return;
        if (!DxDLogic.spendMana(p, e.manaCost())) return;

        float scale = 1.0F + 0.02F * d.getLevel();
        boolean ok = switch (e) {
            case LIGHTNING -> lightning(p);
            case FIRE -> fire(p, scale);
            case WATER -> water(p, scale);
            case WIND -> wind(p, scale);
            case LIGHT -> light(p, scale);
            case DARK -> dark(p, scale);
        };
        if (ok) {
            d.setElementReadyAt(slot, now + e.cooldownSeconds() * 20L);
            p.displayClientMessage(Component.literal(e.castName() + "!").withStyle(e.color(), ChatFormatting.BOLD), true);
        } else {
            d.setMana(d.getMana() + e.manaCost()); // refund if nothing happened
        }
    }

    /** Enemies inside a cone in front of the player. */
    private static List<LivingEntity> cone(ServerPlayer p, double range, double degrees) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F).normalize();
        double cos = Math.cos(Math.toRadians(degrees));
        return Abilities.hostiles(p, range).stream().filter(t -> {
            Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
            return to.length() <= range && to.normalize().dot(look) >= cos;
        }).toList();
    }

    private static void coneParticles(ServerPlayer p, ParticleOptions particle, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        for (double dist = 1; dist <= range; dist += 0.75) {
            Vec3 at = eye.add(look.scale(dist));
            double spread = dist * 0.25;
            level.sendParticles(particle, at.x, at.y, at.z, 4, spread, spread, spread, 0.02);
        }
    }

    private static boolean lightning(ServerPlayer p) {
        LivingEntity target = Abilities.lookTarget(p, 24.0);
        Vec3 at;
        if (target != null) {
            at = target.position();
        } else {
            HitResult hit = p.pick(24.0, 1.0F, false);
            if (hit.getType() == HitResult.Type.MISS) {
                p.displayClientMessage(Component.literal("Nothing in range").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            at = hit.getLocation();
        }
        if (at.distanceTo(p.position()) < 3.0) return false;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(p.serverLevel());
        if (bolt == null) return false;
        bolt.moveTo(at);
        bolt.setCause(p);
        p.serverLevel().addFreshEntity(bolt);
        return true;
    }

    private static boolean fire(ServerPlayer p, float scale) {
        for (LivingEntity t : cone(p, 8.0, 35.0)) {
            t.hurt(Abilities.magic(p), 6.0F * scale);
            t.igniteForSeconds(5.0F);
        }
        coneParticles(p, ParticleTypes.FLAME, 8.0);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    private static boolean water(ServerPlayer p, float scale) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity t : Abilities.hostiles(p, 6.0)) {
            Vec3 push = t.position().subtract(p.position()).normalize().scale(1.2);
            t.push(push.x, 0.3, push.z);
            t.hurtMarked = true;
            t.hurt(Abilities.magic(p), 4.0F * scale);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            t.clearFire();
        }
        for (LivingEntity a : Abilities.allies(p, 6.0)) {
            a.clearFire();
            a.heal(2.0F);
        }
        p.heal(2.0F);
        level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), 150, 3.0, 0.5, 3.0, 0.2);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 60, 3.0, 1.0, 3.0, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean wind(ServerPlayer p, float scale) {
        Vec3 look = p.getViewVector(1.0F);
        p.setDeltaMovement(look.x * 1.8, Math.max(0.35, look.y * 1.2 + 0.3), look.z * 1.8);
        p.hurtMarked = true;
        p.resetFallDistance();
        for (LivingEntity t : cone(p, 6.0, 45.0)) {
            t.push(look.x * 1.8, 0.5, look.z * 1.8);
            t.hurtMarked = true;
            t.hurt(Abilities.magic(p), 3.0F * scale);
        }
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 40, 0.5, 0.3, 0.5, 0.15);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean light(ServerPlayer p, float scale) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity t : Abilities.hostiles(p, 8.0)) {
            boolean weak = DxDLogic.isDevil(t) || t.isInvertedHealAndHarm();
            t.hurt(Abilities.magic(p), 6.0F * scale * (weak ? 2.0F : 1.0F));
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
        for (LivingEntity a : Abilities.allies(p, 8.0)) a.heal(3.0F);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 120, 4.0, 1.5, 4.0, 0.1);
        level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.8F);
        return true;
    }

    private static boolean dark(ServerPlayer p, float scale) {
        ServerLevel level = p.serverLevel();
        LivingEntity target = Abilities.lookTarget(p, 16.0);
        List<LivingEntity> targets = target != null ? List.of(target) : Abilities.hostiles(p, 5.0);
        if (targets.isEmpty()) {
            p.displayClientMessage(Component.literal("No target for the shadows").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        float stolen = 0;
        for (LivingEntity t : targets) {
            float before = t.getHealth();
            t.hurt(Abilities.magic(p), 5.0F * scale);
            stolen += Math.max(0, before - t.getHealth());
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            level.sendParticles(ParticleTypes.SQUID_INK, t.getX(), t.getY() + 1, t.getZ(), 40, 0.4, 0.6, 0.4, 0.05);
        }
        p.heal(stolen * 0.5F);
        level.playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6F, 1.4F);
        return true;
    }

    /** Called once per second: passive bonuses for each element the player has. */
    public static void passives(ServerPlayer p, DxDData d) {
        for (Element e : new Element[]{d.getElement1(), d.getElement2()}) {
            if (e == null) continue;
            switch (e) {
                case LIGHTNING -> buff(p, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
                case FIRE -> buff(p, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
                case WATER -> {
                    buff(p, new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
                    if (p.isInWater()) buff(p, new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
                }
                case WIND -> buff(p, new MobEffectInstance(MobEffects.JUMP, 60, 1, true, false, true));
                case LIGHT -> buff(p, new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                case DARK -> {
                    if (p.isShiftKeyDown()) buff(p, new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, false));
                }
            }
        }
    }

    private static void buff(ServerPlayer p, MobEffectInstance inst) {
        p.addEffect(inst);
    }
}
