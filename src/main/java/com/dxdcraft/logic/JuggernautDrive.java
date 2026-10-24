package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.item.DivineDividingItem;
import com.dxdcraft.registry.ModItems;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Juggernaut Drive for the two Heavenly Dragon gears.
 *
 * INCOMPLETE (default): 30s of overwhelming power, but it eats your life (1 HP/s),
 * you lash out at EVERYTHING nearby (friends included), and you collapse afterwards.
 *
 * COMPLETE (after mastering the gear with a Heavenly Dragon Heart): 45s, stronger,
 * no life drain, full control, and unlocks the gear's special move.
 */
public final class JuggernautDrive {
    public static final int INCOMPLETE_SECONDS = 30;
    public static final int COMPLETE_SECONDS = 45;
    public static final int MANA_COST = 80;

    private JuggernautDrive() {}

    public static String name(int type, boolean complete) {
        String base = type == DxDData.JD_RED ? "Red Dragon Juggernaut Drive" : "White Dragon Juggernaut Drive";
        return (complete ? "" : "Incomplete ") + base;
    }

    private static ChatFormatting color(int type) {
        return type == DxDData.JD_RED ? ChatFormatting.RED : ChatFormatting.AQUA;
    }

    private static ParticleOptions particle(int type) {
        return type == DxDData.JD_RED ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK;
    }

    /** Juggernaut Drive key pressed. */
    public static void tryActivate(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (d.jdActive()) {
            p.displayClientMessage(Component.literal("Juggernaut Drive is already active (" + d.getJdSeconds() + "s)").withStyle(ChatFormatting.RED), true);
            return;
        }
        int type;
        if (holding(p, true)) type = DxDData.JD_RED;
        else if (holding(p, false)) type = DxDData.JD_WHITE;
        else {
            p.displayClientMessage(Component.literal("Hold or wear the Boosted Gear or Divine Dividing to use Juggernaut Drive").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        if (d.getLevel() < DxDData.JD_LEVEL && !creative) {
            p.displayClientMessage(Component.literal("Juggernaut Drive requires DxD level " + DxDData.JD_LEVEL
                    + " (you are Lv " + d.getLevel() + ")").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!d.ultReady() && !creative) {
            p.displayClientMessage(Component.literal("Juggernaut Drive needs a full ULT gauge (" + d.getUltCharge() + "%)").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        net.minecraft.world.item.Item gearItem = type == DxDData.JD_RED ? ModItems.BOOSTED_GEAR.get() : ModItems.DIVINE_DIVIDING.get();
        int gearStage = GearGrowth.stage(p, gearItem);
        if (gearStage < GearGrowth.BALANCE_BREAKER && !creative) {
            p.displayClientMessage(Component.literal("Your gear must reach Stage III: Balance Breaker first (now "
                    + GearGrowth.label(gearStage) + ")").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean mastered = type == DxDData.JD_RED ? d.isMasteredRed() : d.isMasteredWhite();
        boolean complete = mastered && ((d.getLevel() >= DxDData.JD_COMPLETE_LEVEL && gearStage >= GearGrowth.SUBLIMATION) || creative);
        if (!DxDLogic.spendMana(p, MANA_COST)) return;
        if (mastered && !complete) {
            p.sendSystemMessage(Component.literal("The complete drive needs Lv " + DxDData.JD_COMPLETE_LEVEL
                    + " and Stage IV: Sublimation... the dragon's rage takes over!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        if (!creative) d.setUltCharge(0);
        start(p, type, complete);
    }

    /** Starts a drive with no gauge or gear checks (used by the key after its checks, and by /dxd juggernaut). */
    public static void start(ServerPlayer p, int type, boolean complete) {
        DxDAdvancements.grant(p, complete ? "juggernaut_complete" : "juggernaut");
        DxDData d = DxDLogic.data(p);
        if (d.jdActive()) end(p, d);
        d.startJd(type, complete ? COMPLETE_SECONDS : INCOMPLETE_SECONDS, complete);

        ServerLevel level = p.serverLevel();
        if (!complete) {
            // Extra hearts so the life drain is survivable, barely.
            p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, INCOMPLETE_SECONDS * 20 + 20, 3, false, false, true));
            p.heal(16.0F);
        } else {
            p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, COMPLETE_SECONDS * 20 + 20, 4, false, false, true));
            p.setHealth(p.getMaxHealth());
        }
        if (type == DxDData.JD_RED) d.setBoosts(DxDLogic.MAX_BOOSTS);

        Abilities.title(p, complete ? "JUGGERNAUT DRIVE" : "JUGGERNAUT DRIVE?!", color(type),
                complete ? name(type, true) + " - full control" : "The dragon's rage is consuming you...");
        level.sendParticles(particle(type), p.getX(), p.getY() + 1, p.getZ(), 250, 1.2, 1.5, 1.2, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, complete ? 1.0F : 0.6F);
        Combat.announce(p, Component.literal(p.getName().getString() + " has activated the " + name(type, complete) + "!")
                .withStyle(color(type), ChatFormatting.BOLD));
        DxDLogic.updateFlight(p, d);
    }

    private static boolean holding(Player p, boolean red) {
        var item = red ? ModItems.BOOSTED_GEAR.get() : ModItems.DIVINE_DIVIDING.get();
        return p.getMainHandItem().is(item) || p.getOffhandItem().is(item) || com.dxdcraft.compat.CuriosCompat.has(p, item);
    }

    /** Called once per second from DxDLogic while a drive is active. */
    public static void secondTick(ServerPlayer p, DxDData d) {
        int type = d.getJdType();
        boolean complete = d.isJdComplete();
        ServerLevel level = p.serverLevel();

        // Power
        buff(p, MobEffects.DAMAGE_BOOST, complete ? 4 : 3);
        buff(p, MobEffects.DAMAGE_RESISTANCE, 2);
        buff(p, MobEffects.MOVEMENT_SPEED, complete ? 2 : 1);
        buff(p, MobEffects.JUMP, 1);
        buff(p, MobEffects.FIRE_RESISTANCE, 0);
        buff(p, MobEffects.GLOWING, 0);
        if (complete) buff(p, MobEffects.REGENERATION, 0);
        d.setMana(d.getMana() + (complete ? 6 : 3)); // the dragon's power fuels your mana

        // Gear-specific aura
        if (type == DxDData.JD_RED) {
            d.setBoosts(complete ? DxDLogic.MAX_BOOSTS : Math.min(DxDLogic.MAX_BOOSTS, d.getBoosts() + (p.tickCount % 40 < 20 ? 1 : 0)));
        } else {
            // White Dragon: every 2s, Divide all enemies within 8 blocks and steal their strength
            if (p.tickCount % 40 < 20) {
                for (LivingEntity e : Abilities.hostiles(p, 8.0)) {
                    DivineDividingItem.divide(p, e);
                    e.hurt(Abilities.magic(p), 2.0F);
                    p.heal(1.0F);
                }
            }
        }

        if (!complete) {
            // Life force drain - cannot drop you below half a heart by itself, but nothing heals it back
            if (p.getHealth() > 1.0F) p.setHealth(Math.max(1.0F, p.getHealth() - 1.0F));
            p.getFoodData().addExhaustion(2.0F);
            // Berserk: roaring shockwaves hit EVERYTHING close by, friends included
            if (p.tickCount % 40 < 20) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.0), x -> x != p && x.isAlive()
                        && (!(x instanceof Player) || com.dxdcraft.DxDConfig.get(com.dxdcraft.DxDConfig.BERSERK_HITS_PLAYERS::get, true))
                        && Combat.canHarm(p, x))) {
                    e.hurt(Abilities.magic(p), 5.0F);
                    e.push((e.getX() - p.getX()) * 0.3, 0.3, (e.getZ() - p.getZ()) * 0.3);
                    e.hurtMarked = true;
                }
                level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
        }

        level.sendParticles(particle(type), p.getX(), p.getY() + 1, p.getZ(), complete ? 30 : 50, 0.6, 1.0, 0.6, 0.05);

        GearGrowth.addXp(p, type == DxDData.JD_RED ? ModItems.BOOSTED_GEAR.get() : ModItems.DIVINE_DIVIDING.get(), 1);

        int left = d.getJdSeconds() - 1;
        d.setJdSeconds(left);
        if (left <= 0) end(p, d);
    }

    private static void buff(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        p.addEffect(new MobEffectInstance(effect, 40, amp, false, false, true));
    }

    public static void end(ServerPlayer p, DxDData d) {
        boolean complete = d.isJdComplete();
        int type = d.getJdType();
        d.clearJd();
        d.setBoosts(0);
        d.setBoostTimer(0);
        p.removeEffect(MobEffects.HEALTH_BOOST);
        p.removeEffect(MobEffects.GLOWING);
        if (!complete) {
            // The price: you collapse.
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 2));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600, 2));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
            d.setMana(0);
            p.displayClientMessage(Component.literal("The drive fades. Your body is wrecked...").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        } else {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            p.displayClientMessage(Component.literal(name(type, true) + " released.").withStyle(color(type)), true);
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 0.5F);
        DxDLogic.updateFlight(p, d);
    }
}
