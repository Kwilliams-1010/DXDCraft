package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.registry.ModGear;
import com.dxdcraft.registry.ModGear.GearSet;
import com.dxdcraft.world.DimensionTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

/**
 * Full-set bonuses (all four pieces of one material):
 *  Holy Silver   - Regeneration in daylight or in Heaven, immune to Holy Water
 *  Demonite      - Fire Resistance, Night Vision in the Underworld
 *  Seraphite     - +3 mana regen, Slow Falling while sneaking in the air
 *  Hellfire Ruby - Strength, attackers are set on fire
 *  Dragonite     - Strength + Resistance, ULT gauge +1 per second
 */
public final class ArmorSets {
    private ArmorSets() {}

    /** The material of the player's full armor set, or null if not wearing a full set. */
    public static GearSet fullSet(LivingEntity e) {
        GearSet found = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof ArmorItem armor)) return null;
            GearSet set = null;
            for (GearSet s : GearSet.values()) {
                if (armor.getMaterial().is(ModGear.MATERIALS.get(s).getKey())) {
                    set = s;
                    break;
                }
            }
            if (set == null || (found != null && found != set)) return null;
            found = set;
        }
        return found;
    }

    private static void buff(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp, int dur) {
        p.addEffect(new MobEffectInstance(effect, dur, amp, true, false, true));
    }

    /** Called once per second. */
    public static void secondTick(ServerPlayer p, DxDData d) {
        GearSet set = fullSet(p);
        if (set == null) return;
        switch (set) {
            case HOLY_SILVER -> {
                if (p.level().isDay() || DimensionTravel.inHeaven(p)) buff(p, MobEffects.REGENERATION, 0, 60);
            }
            case DEMONITE -> {
                buff(p, MobEffects.FIRE_RESISTANCE, 0, 60);
                if (DimensionTravel.inUnderworld(p)) buff(p, MobEffects.NIGHT_VISION, 0, 400);
            }
            case SERAPHITE -> {
                d.setMana(d.getMana() + 3);
                if (p.isShiftKeyDown() && !p.onGround()) buff(p, MobEffects.SLOW_FALLING, 0, 30);
            }
            case HELLFIRE_RUBY -> buff(p, MobEffects.DAMAGE_BOOST, 0, 60);
            case DRAGONITE -> {
                buff(p, MobEffects.DAMAGE_BOOST, 0, 60);
                buff(p, MobEffects.DAMAGE_RESISTANCE, 0, 60);
                d.addUltCharge(1);
            }
        }
        DxDAdvancements.grant(p, "full_set_" + set.id);
    }
}
