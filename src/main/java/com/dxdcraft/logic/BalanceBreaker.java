package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.item.DivineDividingItem;
import com.dxdcraft.item.SacredGearItem;
import com.dxdcraft.network.TransformPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Balance Breaker: the gear transforms into armor (Stage III+). Toggle with the Balance Breaker key [H].
 *
 * Costs mana to start and drains mana every second; ends when mana runs out, when toggled off,
 * on death, or when a Juggernaut Drive takes over. Each gear grants its own set of powers.
 * The visible armor is drawn by every client (see client.TransformRenderLayer); the server only
 * broadcasts who is transformed and with which gear.
 */
public final class BalanceBreaker {
    public static final int START_MANA = 40;
    /** Transformation sequence length in ticks (the armor assembles piece by piece). */
    public static final int ASSEMBLE_TICKS = 30;
    /** Length of the animated transformation scenes (must match the animation files). */
    public static final int SCENE_TICKS_BB = 60, SCENE_TICKS_JD = 70;
    private static final Map<UUID, Long> SCENE_UNTIL = new HashMap<>();

    public static boolean scenesEnabled() {
        return com.dxdcraft.DxDConfig.get(com.dxdcraft.DxDConfig.TRANSFORM_SCENES::get, true);
    }

    /** True while the player is in the middle of a transformation scene (can't move or be hurt). */
    public static boolean transforming(Player p) {
        Long until = SCENE_UNTIL.get(p.getUUID());
        if (until == null) return false;
        if (p.level().getGameTime() >= until) {
            SCENE_UNTIL.remove(p.getUUID());
            return false;
        }
        return true;
    }

    /** Starts the protected scene window and tells every client to play it. */
    public static void playScene(ServerPlayer p, String gear, int mode) {
        boolean animated = scenesEnabled();
        if (animated) {
            SCENE_UNTIL.put(p.getUUID(), p.level().getGameTime()
                    + (mode == TransformPayload.MODE_BALANCE_BREAKER ? SCENE_TICKS_BB : SCENE_TICKS_JD));
            p.setDeltaMovement(0, Math.max(0, p.getDeltaMovement().y), 0);
            p.hurtMarked = true;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new TransformPayload(p.getId(), gear, mode, animated));
    }

    private record Form(String gear, long startedAt, boolean forced) {}
    private static final Map<UUID, Form> ACTIVE = new HashMap<>();

    private BalanceBreaker() {}

    /** Per-gear powers while transformed. */
    private record Powers(int drainPerSecond, boolean flight, List<Buff> buffs) {}
    private record Buff(Holder<MobEffect> effect, int amp) {}

    private static Powers powers(String gear) {
        return switch (gear) {
            case "boosted_gear" -> new Powers(3, true, List.of(b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.FIRE_RESISTANCE, 0)));
            case "divine_dividing" -> new Powers(3, true, List.of(b(MobEffects.MOVEMENT_SPEED, 1), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.DAMAGE_BOOST, 0)));
            case "sword_birth" -> new Powers(2, false, List.of(b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.DIG_SPEED, 1), b(MobEffects.MOVEMENT_SPEED, 0)));
            case "absorption_line" -> new Powers(2, false, List.of(b(MobEffects.REGENERATION, 0), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.JUMP, 1)));
            case "forbidden_balor_view" -> new Powers(3, false, List.of(b(MobEffects.MOVEMENT_SPEED, 1), b(MobEffects.NIGHT_VISION, 0), b(MobEffects.DAMAGE_RESISTANCE, 0)));
            case "dimension_lost" -> new Powers(3, false, List.of(b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.SLOW_FALLING, 0), b(MobEffects.NIGHT_VISION, 0)));
            case "zenith_tempest" -> new Powers(3, true, List.of(b(MobEffects.MOVEMENT_SPEED, 2), b(MobEffects.JUMP, 2), b(MobEffects.DAMAGE_RESISTANCE, 0)));
            case "annihilation_maker" -> new Powers(3, false, List.of(b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.NIGHT_VISION, 0)));
            case "regulus_nemea" -> new Powers(2, false, List.of(b(MobEffects.DAMAGE_RESISTANCE, 2), b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.MOVEMENT_SPEED, 0)));
            case "twilight_healing" -> new Powers(2, false, List.of(b(MobEffects.REGENERATION, 1), b(MobEffects.DAMAGE_RESISTANCE, 0), b(MobEffects.ABSORPTION, 1)));
            default -> new Powers(3, false, List.of(b(MobEffects.DAMAGE_RESISTANCE, 0)));
        };
    }

    private static Buff b(Holder<MobEffect> e, int amp) {
        return new Buff(e, amp);
    }

    public static String formName(String gear) {
        return Destiny.gearName(gear) + " - Balance Breaker";
    }

    public static boolean active(Player p) {
        return ACTIVE.containsKey(p.getUUID());
    }

    public static String activeGear(Player p) {
        Form f = ACTIVE.get(p.getUUID());
        return f == null ? null : f.gear;
    }

    /** The gear the player is holding (main hand first) or wearing in a Curios slot. */
    public static SacredGearItem heldOrWornGear(Player p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof SacredGearItem g) return g;
        }
        return com.dxdcraft.compat.CuriosCompat.wornGear(p);
    }

    /** Balance Breaker key pressed: transform, or release the form if already transformed. */
    public static void toggle(ServerPlayer p) {
        if (active(p)) {
            end(p, true);
            return;
        }
        DxDData d = DxDLogic.data(p);
        if (d.jdActive()) {
            p.displayClientMessage(Component.literal("You are already in Juggernaut Drive").withStyle(ChatFormatting.RED), true);
            return;
        }
        SacredGearItem gear = heldOrWornGear(p);
        if (gear == null) {
            p.displayClientMessage(Component.literal("Hold or wear a Sacred Gear to use Balance Breaker").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        int stage = GearGrowth.stage(p, gear);
        if (stage < GearGrowth.BALANCE_BREAKER && !creative) {
            p.displayClientMessage(Component.literal("Your gear must reach " + GearGrowth.label(GearGrowth.BALANCE_BREAKER)
                    + " first (now " + GearGrowth.label(Math.max(1, stage)) + ")").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!creative && !DxDLogic.spendMana(p, START_MANA)) return;
        start(p, GearGrowth.id(gear), false);
    }

    /**
     * Starts the form without checks (key after its checks, and /dxd balancebreaker).
     * forced = started by an admin command: it does not need the gear item to be carried.
     */
    public static void start(ServerPlayer p, String gear, boolean forced) {
        ACTIVE.put(p.getUUID(), new Form(gear, p.level().getGameTime(), forced));
        playScene(p, gear, TransformPayload.MODE_BALANCE_BREAKER);
        DxDAdvancements.grant(p, "balance_breaker");
        ServerLevel level = p.serverLevel();
        Abilities.title(p, "BALANCE BREAKER", color(gear), formName(gear));
        if (!scenesEnabled()) {
            level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.0F, 0.7F);
            level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 2.0F, 0.8F);
            level.sendParticles(particle(gear), p.getX(), p.getY() + 1, p.getZ(), 120, 0.5, 1.2, 0.5, 0.15);
        }
        Combat.announce(p, Component.literal(p.getName().getString() + " entered Balance Breaker: ").withStyle(color(gear))
                .append(Component.literal(formName(gear)).withStyle(ChatFormatting.BOLD)));
        applyBuffs(p, gear);
        DxDLogic.updateFlight(p, DxDLogic.data(p));
    }

    /** Ends the form. voluntary = the player released it (no message about running dry). */
    public static void end(ServerPlayer p, boolean voluntary) {
        SCENE_UNTIL.remove(p.getUUID());
        Form f = ACTIVE.remove(p.getUUID());
        if (f == null) return;
        broadcast(p, f.gear, TransformPayload.MODE_NONE);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2F, 0.9F);
        p.serverLevel().sendParticles(particle(f.gear), p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1.0, 0.5, 0.1);
        p.displayClientMessage(Component.literal(voluntary ? "Balance Breaker released." : "Out of mana - your Balance Breaker shatters!")
                .withStyle(voluntary ? ChatFormatting.GRAY : ChatFormatting.RED), true);
        DxDLogic.updateFlight(p, DxDLogic.data(p));
    }

    /** Silent cleanup (logout / death / Juggernaut Drive taking over). */
    public static void clear(ServerPlayer p) {
        SCENE_UNTIL.remove(p.getUUID());
        Form f = ACTIVE.remove(p.getUUID());
        if (f != null) broadcast(p, f.gear, TransformPayload.MODE_NONE);
    }

    /** Once per second from DxDLogic. Returns true while the form is active. */
    public static boolean secondTick(ServerPlayer p, DxDData d) {
        Form f = ACTIVE.get(p.getUUID());
        if (f == null) return false;
        if (d.jdActive()) {
            clear(p);
            return false;
        }
        // The gear has to stay with you.
        Item item = Destiny.gearItem(f.gear);
        boolean stillHas = p.getMainHandItem().is(item) || p.getOffhandItem().is(item) || com.dxdcraft.compat.CuriosCompat.has(p, item)
                || p.getInventory().contains(new ItemStack(item));
        if (!stillHas && !f.forced) {
            end(p, true);
            return false;
        }
        Powers pw = powers(f.gear);
        int drain = Math.max(1, Math.round(pw.drainPerSecond * GearGrowth.manaDiscount(d.getGearStage(f.gear))));
        if (!p.getAbilities().instabuild) {
            if (d.getMana() < drain) {
                end(p, false);
                return false;
            }
            d.setMana(d.getMana() - drain);
        }
        applyBuffs(p, f.gear);
        aura(p, f.gear);
        GearGrowth.addXp(p, item, 1);
        long secs = (p.level().getGameTime() - f.startedAt) / 20;
        p.displayClientMessage(Component.literal(formName(f.gear) + "  " + secs + "s   Mana " + d.getMana() + "/" + d.getMaxMana()
                + "   [H] release").withStyle(color(f.gear)), true);
        return true;
    }

    private static void applyBuffs(ServerPlayer p, String gear) {
        for (Buff b : powers(gear).buffs) p.addEffect(new MobEffectInstance(b.effect, 45, b.amp, false, false, true));
    }

    /** Gear-specific effect on the surroundings each second. */
    private static void aura(ServerPlayer p, String gear) {
        ServerLevel level = p.serverLevel();
        switch (gear) {
            case "boosted_gear" -> {
                // Boosts build twice as fast inside the armor.
                DxDData d = DxDLogic.data(p);
                d.setBoosts(Math.min(DxDLogic.MAX_BOOSTS, d.getBoosts() + 1));
            }
            case "divine_dividing" -> {
                if (p.tickCount % 60 < 20) for (LivingEntity e : Abilities.hostiles(p, 6.0)) DivineDividingItem.divide(p, e);
            }
            case "forbidden_balor_view" -> {
                for (LivingEntity e : Abilities.hostiles(p, 8.0)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
            case "dimension_lost" -> {
                for (LivingEntity e : Abilities.hostiles(p, 8.0)) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
            case "annihilation_maker" -> {
                for (LivingEntity e : Abilities.hostiles(p, 5.0)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            }
            case "twilight_healing" -> {
                for (Player ally : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8.0), x -> x != p && x.isAlive() && !Combat.canHarm(p, x))) {
                    ally.heal(1.0F);
                }
            }
            default -> {}
        }
        level.sendParticles(particle(gear), p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.8, 0.4, 0.02);
    }

    public static boolean grantsFlight(Player p) {
        Form f = ACTIVE.get(p.getUUID());
        return f != null && powers(f.gear).flight;
    }

    // ------------------------------------------------------------------ sync

    public static void broadcast(ServerPlayer p, String gear, int mode) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new TransformPayload(p.getId(), gear, mode));
    }

    /** What a client that starts seeing {@code p} needs to know (Balance Breaker or Juggernaut Drive). */
    public static TransformPayload stateOf(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (d.jdActive()) {
            return new TransformPayload(p.getId(), d.getJdType() == DxDData.JD_RED ? "boosted_gear" : "divine_dividing",
                    d.isJdComplete() ? TransformPayload.MODE_JD_COMPLETE : TransformPayload.MODE_JD);
        }
        Form f = ACTIVE.get(p.getUUID());
        return f == null ? null : new TransformPayload(p.getId(), f.gear, TransformPayload.MODE_BALANCE_BREAKER);
    }

    private static ChatFormatting color(String gear) {
        return switch (gear) {
            case "boosted_gear", "regulus_nemea" -> ChatFormatting.RED;
            case "divine_dividing", "zenith_tempest" -> ChatFormatting.AQUA;
            case "sword_birth", "forbidden_balor_view", "annihilation_maker" -> ChatFormatting.LIGHT_PURPLE;
            case "absorption_line" -> ChatFormatting.GREEN;
            case "twilight_healing" -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        };
    }

    private static ParticleOptions particle(String gear) {
        return switch (gear) {
            case "boosted_gear", "regulus_nemea" -> ParticleTypes.FLAME;
            case "divine_dividing", "zenith_tempest" -> ParticleTypes.ELECTRIC_SPARK;
            case "sword_birth", "forbidden_balor_view" -> ParticleTypes.WITCH;
            case "annihilation_maker", "dimension_lost" -> ParticleTypes.SQUID_INK;
            case "absorption_line" -> ParticleTypes.HAPPY_VILLAGER;
            case "twilight_healing" -> ParticleTypes.END_ROD;
            default -> ParticleTypes.END_ROD;
        };
    }
}
