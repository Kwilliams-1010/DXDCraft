package com.dxdcraft.logic;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The birth roll ("Destiny"), rolled once per player on their first join.
 *
 *  - 35%: a dormant Sacred Gear sleeps inside you. It must be AWAKENED by meeting its tier's requirement.
 *  - 65%: an Element affinity instead (Lightning, Fire, Water, Wind, Light, Dark),
 *         with a 15% chance of being born with two.
 *
 * Element users (no gear) who were born with one element get chances at a second as their Growth
 * (DxD level) rises: 40% at Lv 15, and if that fails, 60% at Lv 30.
 */
public final class Destiny {
    public static final float GEAR_CHANCE = 0.35F;
    public static final float DOUBLE_ELEMENT_CHANCE = 0.15F;
    public static final int SECOND_ELEMENT_LEVEL_1 = 15;
    public static final float SECOND_ELEMENT_CHANCE_1 = 0.40F;
    public static final int SECOND_ELEMENT_LEVEL_2 = 30;
    public static final float SECOND_ELEMENT_CHANCE_2 = 0.60F;
    public static final int RESUMMON_COOLDOWN_TICKS = 20 * 60 * 5;

    /** Sacred Gear tiers and their awakening requirements. */
    public enum Tier {
        COMMON(5, "reach Growth Lv 5, then survive a near-death moment (3 hearts or less)"),
        RARE(10, "reach Growth Lv 10 and defeat 50 hostile creatures"),
        LONGINUS(20, "reach Growth Lv 20 and slay a boss-class foe (200+ max health)");

        public final int level;
        public final String requirement;

        Tier(int level, String requirement) {
            this.level = level;
            this.requirement = requirement;
        }
    }

    private record GearRoll(String id, Tier tier, int weight) {}

    private static final List<GearRoll> GEARS = List.of(
            new GearRoll("twilight_healing", Tier.COMMON, 10),
            new GearRoll("sword_birth", Tier.COMMON, 10),
            new GearRoll("absorption_line", Tier.COMMON, 10),
            new GearRoll("dimension_lost", Tier.RARE, 6),
            new GearRoll("forbidden_balor_view", Tier.RARE, 6),
            new GearRoll("annihilation_maker", Tier.RARE, 6),
            new GearRoll("boosted_gear", Tier.LONGINUS, 2),
            new GearRoll("divine_dividing", Tier.LONGINUS, 2),
            new GearRoll("zenith_tempest", Tier.LONGINUS, 2),
            new GearRoll("regulus_nemea", Tier.LONGINUS, 2));

    public static final int HOSTILE_KILLS_FOR_RARE = 50;
    public static final float BOSS_HEALTH = 200.0F;

    private Destiny() {}

    public static List<String> gearIds() {
        return GEARS.stream().map(GearRoll::id).toList();
    }

    public static Tier tierOf(String gearId) {
        for (GearRoll g : GEARS) if (g.id.equals(gearId)) return g.tier;
        return Tier.COMMON;
    }

    public static Item gearItem(String gearId) {
        return BuiltInRegistries.ITEM.get(DxDCraft.id(gearId));
    }

    public static String gearName(String gearId) {
        return new ItemStack(gearItem(gearId)).getHoverName().getString();
    }

    // ------------------------------------------------------------------ rolling

    /** Roll once, on first join. */
    public static void rollIfNeeded(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (d.isDestinyRolled()) return;
        roll(p, d, p.getRandom());
    }

    public static void roll(ServerPlayer p, DxDData d, RandomSource r) {
        d.setDestinyRolled(true);
        d.setInnateGear("");
        d.setGearAwakened(false);
        d.setElement1(null);
        d.setElement2(null);
        d.setSecondChanceStage(0);

        if (r.nextFloat() < GEAR_CHANCE) {
            int total = GEARS.stream().mapToInt(GearRoll::weight).sum();
            int pick = r.nextInt(total);
            for (GearRoll g : GEARS) {
                pick -= g.weight;
                if (pick < 0) {
                    d.setInnateGear(g.id);
                    break;
                }
            }
            Tier tier = tierOf(d.getInnateGear());
            p.sendSystemMessage(Component.literal("✦ Destiny ✦ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("A dormant Sacred Gear sleeps within you...").withStyle(ChatFormatting.GREEN)));
            p.sendSystemMessage(Component.literal("  To awaken it: " + tier.requirement + ". (/dxd info shows your progress)")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            Element[] all = Element.values();
            Element first = all[r.nextInt(all.length)];
            d.setElement1(first);
            if (r.nextFloat() < DOUBLE_ELEMENT_CHANCE) {
                d.setElement2(pickOther(first, r));
                d.setSecondChanceStage(2); // already has two, no further rolls
            }
            p.sendSystemMessage(Component.literal("✦ Destiny ✦ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("You were born with an elemental affinity: ").withStyle(ChatFormatting.WHITE))
                    .append(elements(d)));
            p.sendSystemMessage(Component.literal("  Press [R] to cast" + (d.getElement2() != null ? ", Sneak+[R] for your second element." : ".")
                    + (d.getElement2() == null ? " Grow stronger for a chance at a second element." : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static Element pickOther(Element not, RandomSource r) {
        Element[] all = Element.values();
        Element e;
        do {
            e = all[r.nextInt(all.length)];
        } while (e == not);
        return e;
    }

    public static MutableComponent elements(DxDData d) {
        MutableComponent c = Component.empty();
        Element a = d.getElement1();
        Element b = d.getElement2();
        if (a == null) return c.append(Component.literal("none").withStyle(ChatFormatting.GRAY));
        c.append(Component.literal(a.displayName()).withStyle(a.color(), ChatFormatting.BOLD));
        if (b != null) {
            c.append(Component.literal(" + ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(b.displayName()).withStyle(b.color(), ChatFormatting.BOLD));
        }
        return c;
    }

    // ------------------------------------------------------------------ progress hooks

    /** Called when the player kills something. */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        DxDData d = DxDLogic.data(p);
        if (victim instanceof net.minecraft.world.entity.monster.Enemy) d.setHostileKills(d.getHostileKills() + 1);
        if (victim.getMaxHealth() >= BOSS_HEALTH) d.setBossSlain(true);
        checkAwakening(p, d);
    }

    /** Called once per second. */
    public static void secondTick(ServerPlayer p, DxDData d) {
        checkAwakening(p, d);
        checkSecondElement(p, d);
    }

    public static String progress(DxDData d) {
        if (!d.hasInnateGear()) return "";
        if (d.isGearAwakened()) return "awakened";
        Tier t = tierOf(d.getInnateGear());
        String lvl = "Lv " + d.getLevel() + "/" + t.level;
        return switch (t) {
            case COMMON -> lvl + ", then survive at 3 hearts or less";
            case RARE -> lvl + ", kills " + Math.min(d.getHostileKills(), HOSTILE_KILLS_FOR_RARE) + "/" + HOSTILE_KILLS_FOR_RARE;
            case LONGINUS -> lvl + ", boss slain: " + (d.isBossSlain() ? "yes" : "no");
        };
    }

    private static void checkAwakening(ServerPlayer p, DxDData d) {
        if (!d.hasInnateGear() || d.isGearAwakened() || !p.isAlive()) return;
        Tier t = tierOf(d.getInnateGear());
        if (d.getLevel() < t.level) return;
        boolean met = switch (t) {
            case COMMON -> p.getHealth() <= 6.0F;
            case RARE -> d.getHostileKills() >= HOSTILE_KILLS_FOR_RARE;
            case LONGINUS -> d.isBossSlain();
        };
        if (met) awaken(p, d);
    }

    public static void awaken(ServerPlayer p, DxDData d) {
        if (!d.hasInnateGear()) return;
        d.setGearAwakened(true);
        giveGear(p, d);
        String name = gearName(d.getInnateGear());
        Abilities.title(p, "SACRED GEAR AWAKENED", ChatFormatting.GREEN, name);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 150, 0.6, 1.2, 0.6, 0.5);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (tierOf(d.getInnateGear()) == Tier.COMMON) p.heal(10.0F); // the gear saves you
        p.server.getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " has awakened the Sacred Gear ")
                .withStyle(ChatFormatting.GREEN).append(Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)), false);
        p.sendSystemMessage(Component.literal("If you ever lose it, Sneak + [V] re-manifests it (5 min cooldown).").withStyle(ChatFormatting.GRAY));
    }

    private static void giveGear(ServerPlayer p, DxDData d) {
        ItemStack stack = new ItemStack(gearItem(d.getInnateGear()));
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    /** Sneak + racial ability key: bring back a lost awakened gear. Returns true if handled. */
    public static boolean tryResummon(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (!d.hasInnateGear() || !d.isGearAwakened()) return false;
        Item gear = gearItem(d.getInnateGear());
        if (com.dxdcraft.compat.CuriosCompat.has(p, gear)) {
            p.displayClientMessage(Component.literal("Your Sacred Gear is already with you.").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(gear)) {
                p.displayClientMessage(Component.literal("Your Sacred Gear is already with you.").withStyle(ChatFormatting.GRAY), true);
                return true;
            }
        }
        long now = p.level().getGameTime();
        if (now < d.getResummonReadyAt() && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.literal("You can re-manifest your gear in " + (d.getResummonReadyAt() - now) / 20 + "s").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        giveGear(p, d);
        d.setResummonReadyAt(now + RESUMMON_COOLDOWN_TICKS);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        p.displayClientMessage(Component.literal("Your Sacred Gear answers your call.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        return true;
    }

    private static void checkSecondElement(ServerPlayer p, DxDData d) {
        if (d.hasInnateGear() || d.getElement1() == null || d.getElement2() != null) return;
        int stage = d.getSecondChanceStage();
        int lvl = d.getLevel();
        float chance;
        if (stage == 0 && lvl >= SECOND_ELEMENT_LEVEL_1) chance = SECOND_ELEMENT_CHANCE_1;
        else if (stage == 1 && lvl >= SECOND_ELEMENT_LEVEL_2) chance = SECOND_ELEMENT_CHANCE_2;
        else return;
        d.setSecondChanceStage(stage + 1);
        if (p.getRandom().nextFloat() < chance) {
            Element e = pickOther(d.getElement1(), p.getRandom());
            d.setElement2(e);
            d.setSecondChanceStage(2);
            Abilities.title(p, "NEW ELEMENT", e.color(), e.displayName() + " awakens within you!");
            p.sendSystemMessage(Component.literal("Your growth unlocked a second element: ").withStyle(ChatFormatting.WHITE)
                    .append(elements(d)).append(Component.literal("  (Sneak+[R] to cast it)").withStyle(ChatFormatting.GRAY)));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.4F);
        } else {
            String next = stage == 0 ? " Another chance comes at Lv " + SECOND_ELEMENT_LEVEL_2 + "." : "";
            p.sendSystemMessage(Component.literal("You feel a second power stir... but it fades." + next).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
