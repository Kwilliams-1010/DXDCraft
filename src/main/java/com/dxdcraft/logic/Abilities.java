package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.projectile.DestructionOrbEntity;
import com.dxdcraft.entity.projectile.LightSpearEntity;
import com.dxdcraft.item.SacredGearItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Ultimates, racial abilities and shared targeting helpers. All methods are server side. */
public final class Abilities {
    public static final int ACTION_ULTIMATE = 0;
    public static final int ACTION_RACE_ABILITY = 1;
    public static final int ACTION_JUGGERNAUT = 2;
    public static final int ACTION_ELEMENT_1 = 3;
    public static final int ACTION_ELEMENT_2 = 4;
    /** Every ultimate (gear or race) costs this much mana on top of a full ULT gauge. */
    public static final int ULT_MANA_COST = 50;

    private Abilities() {}

    // ------------------------------------------------------------------ helpers

    /** Mobs that are hostile, or are currently targeting this player. Never the player's pets or other players. */
    public static List<LivingEntity> hostiles(ServerPlayer p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e ->
                e != p && e.isAlive() && !(e instanceof Player)
                        && !(e instanceof TamableAnimal t && t.isOwnedBy(p))
                        && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == p)));
    }

    /** The player, nearby players, and the player's pets. */
    public static List<LivingEntity> allies(ServerPlayer p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e ->
                e.isAlive() && (e instanceof Player || (e instanceof TamableAnimal t && t.isOwnedBy(p))));
    }

    public static DamageSource magic(ServerPlayer p) {
        return p.damageSources().indirectMagic(p, p);
    }

    /** Living entity the player is looking at, within range. */
    public static LivingEntity lookTarget(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    public static void title(ServerPlayer p, String title, ChatFormatting color, String subtitle) {
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.WHITE)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
    }

    public static void freeze(LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 9, false, false));
        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 4, false, false));
        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        e.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean creative(Player p) {
        return p.getAbilities().instabuild;
    }

    // ------------------------------------------------------------------ ultimates

    /** Ultimate key: the Sacred Gear in hand if there is one, otherwise the race ultimate. */
    public static void ultimateKey(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof SacredGearItem gear) {
            tryGearUltimate(p, gear);
        } else if (p.getOffhandItem().getItem() instanceof SacredGearItem gear) {
            tryGearUltimate(p, gear);
        } else if (com.dxdcraft.compat.CuriosCompat.wornGear(p) != null) {
            tryGearUltimate(p, com.dxdcraft.compat.CuriosCompat.wornGear(p));
        } else {
            tryRaceUltimate(p);
        }
    }

    private static boolean checkGauge(ServerPlayer p, DxDData d) {
        if (d.ultReady() || creative(p)) return true;
        p.displayClientMessage(Component.literal("ULT gauge " + d.getUltCharge() + "% - fight to charge it!").withStyle(ChatFormatting.GOLD), true);
        return false;
    }

    private static void spend(ServerPlayer p, DxDData d) {
        if (!creative(p)) d.setUltCharge(0);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public static void tryGearUltimate(ServerPlayer p, SacredGearItem gear) {
        DxDData d = DxDLogic.data(p);
        if (p.getCooldowns().isOnCooldown(gear)) return;
        if (!checkGauge(p, d)) return;
        if (!DxDLogic.spendMana(p, ULT_MANA_COST)) return;
        if (gear.performUltimate(p, p.serverLevel())) {
            title(p, gear.ultimateName(), ChatFormatting.GOLD, "Ultimate");
            spend(p, d);
            p.getCooldowns().addCooldown(gear, 40);
        }
    }

    public static void tryRaceUltimate(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (!checkGauge(p, d)) return;
        if (!DxDLogic.spendMana(p, ULT_MANA_COST)) return;
        ServerLevel level = p.serverLevel();
        switch (d.getRace()) {
            case HUMAN -> {
                // Last Stand
                p.setHealth(p.getMaxHealth());
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.3);
                title(p, "Last Stand", ChatFormatting.WHITE, "Human Ultimate");
            }
            case DEVIL -> {
                if (d.getPiece() == Piece.PAWN) {
                    // Promotion: a Pawn temporarily becomes a Queen
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));
                    d.setMana(d.getMaxMana());
                    level.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1, p.getZ(), 120, 0.8, 1.2, 0.8, 0.05);
                    title(p, "Promotion: Queen", ChatFormatting.DARK_RED, "Pawn Ultimate (60s)");
                } else {
                    // Crimson Annihilation: a fan of Destruction orbs
                    for (int i = -4; i <= 4; i++) {
                        DestructionOrbEntity orb = new DestructionOrbEntity(p, level);
                        orb.setDamage(18.0F);
                        orb.shootFromRotation(p, p.getXRot(), p.getYRot() + i * 6.0F, 0.0F, 1.7F, 0.0F);
                        level.addFreshEntity(orb);
                    }
                    level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 1.0F);
                    title(p, "Crimson Annihilation", ChatFormatting.DARK_RED, "Devil Ultimate");
                }
            }
            case ANGEL -> {
                // Judgment Light: smites enemies, heals allies
                for (LivingEntity e : hostiles(p, 10)) {
                    e.hurt(magic(p), DxDLogic.isDevil(e) || e.isInvertedHealAndHarm() ? 30.0F : 15.0F);
                    level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 2, e.getZ(), 30, 0.2, 2.0, 0.2, 0.02);
                }
                for (LivingEntity a : allies(p, 10)) {
                    a.heal(10.0F);
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
                level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 4, p.getZ(), 200, 5, 3, 5, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.8F);
                title(p, "Judgment Light", ChatFormatting.GOLD, "Angel Ultimate");
            }
            case FALLEN_ANGEL -> {
                // Black Wing Storm: 16 spears in every direction and enemies lifted helplessly
                for (int i = 0; i < 16; i++) {
                    LightSpearEntity spear = new LightSpearEntity(p, level);
                    spear.setDamage(10.0F);
                    spear.shootFromRotation(p, 0.0F, p.getYRot() + i * 22.5F, 0.0F, 1.8F, 0.0F);
                    level.addFreshEntity(spear);
                }
                for (LivingEntity e : hostiles(p, 10)) {
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
                }
                level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 120, 2, 1, 2, 0.1);
                level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 2.0F, 0.6F);
                title(p, "Black Wing Storm", ChatFormatting.DARK_PURPLE, "Fallen Angel Ultimate");
            }
        }
        spend(p, d);
    }

    // ------------------------------------------------------------------ racial abilities

    public static void raceAbility(ServerPlayer p) {
        if (p.isShiftKeyDown() && Destiny.tryResummon(p)) return;
        DxDData d = DxDLogic.data(p);
        long now = p.level().getGameTime();
        if (now < d.getAbilityReadyAt() && !creative(p)) {
            long secs = (d.getAbilityReadyAt() - now + 19) / 20;
            p.displayClientMessage(Component.literal("Ability ready in " + secs + "s").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Race race = d.getRace();
        int cost = switch (race) {
            case HUMAN -> 20;
            case DEVIL -> 40;
            case ANGEL -> 30;
            case FALLEN_ANGEL -> 40;
        };
        if (!creative(p) && !d.tryUseMana(cost)) {
            p.displayClientMessage(Component.literal("Not enough mana (" + cost + ")").withStyle(ChatFormatting.AQUA), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        int cooldownSecs;
        switch (race) {
            case HUMAN -> {
                // Adrenaline
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));
                p.displayClientMessage(Component.literal("Adrenaline!").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
                cooldownSecs = 30;
            }
            case DEVIL -> {
                // Magic Circle Teleport: return to your bed / spawn
                magicCircleTeleport(p);
                cooldownSecs = 60;
            }
            case ANGEL -> {
                // Holy Barrier: shield yourself and blast enemies away
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2));
                for (LivingEntity e : hostiles(p, 6)) {
                    Vec3 push = e.position().subtract(p.position()).normalize().scale(1.6);
                    e.push(push.x, 0.5, push.z);
                    e.hurtMarked = true;
                    if (DxDLogic.isDevil(e)) e.hurt(magic(p), 6.0F);
                }
                level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, 2, 1, 2, 0.1);
                level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
                p.displayClientMessage(Component.literal("Holy Barrier!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                cooldownSecs = 25;
            }
            case FALLEN_ANGEL -> {
                // Light Rain: spears fall on every enemy nearby
                List<LivingEntity> targets = hostiles(p, 14);
                for (LivingEntity e : targets) {
                    LightSpearEntity spear = new LightSpearEntity(p, level);
                    spear.setDamage(8.0F);
                    spear.setPos(e.getX(), e.getY() + 8.0, e.getZ());
                    spear.shoot(0.0, -1.0, 0.0, 2.0F, 0.0F);
                    level.addFreshEntity(spear);
                }
                p.displayClientMessage(Component.literal("Light Rain! (" + targets.size() + " targets)").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
                cooldownSecs = 20;
            }
            default -> cooldownSecs = 10;
        }
        d.setAbilityReadyAt(now + cooldownSecs * 20L);
    }

    private static void magicCircleTeleport(ServerPlayer p) {
        ServerLevel from = p.serverLevel();
        from.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 0.1, p.getZ(), 120, 1.0, 0.05, 1.0, 0.0);
        from.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);

        ServerLevel target = p.server.getLevel(p.getRespawnDimension());
        BlockPos pos = p.getRespawnPosition();
        if (target == null || pos == null) {
            target = p.server.overworld();
            pos = target.getSharedSpawnPos();
        }
        p.teleportTo(target, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        target.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 0.1, p.getZ(), 120, 1.0, 0.05, 1.0, 0.0);
        p.displayClientMessage(Component.literal("Magic Circle Teleport").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    /** Random safe-ish teleport far away (used by Dimension Lost). */
    public static void banish(LivingEntity e, Level level) {
        for (int i = 0; i < 16; i++) {
            double angle = e.getRandom().nextDouble() * Math.PI * 2;
            double dist = 30 + e.getRandom().nextDouble() * 30;
            double x = e.getX() + Math.cos(angle) * dist;
            double z = e.getZ() + Math.sin(angle) * dist;
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            if (e.randomTeleport(x, y, z, true)) return;
        }
    }

    public static boolean isSelfOrAlly(ServerPlayer p, Entity e) {
        return e == p || (e instanceof TamableAnimal t && t.isOwnedBy(p));
    }
}
