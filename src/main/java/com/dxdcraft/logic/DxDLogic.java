package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.StrayDevil;
import com.dxdcraft.registry.ModAttachments;
import com.dxdcraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Shared gameplay rules for races, mana, flight and the Boosted Gear. */
public final class DxDLogic {
    public static final int MAX_BOOSTS = 10;
    public static final int SECONDS_PER_BOOST = 10;

    private DxDLogic() {}

    public static DxDData data(Player player) {
        return player.getData(ModAttachments.DXD);
    }

    public static boolean isDevil(LivingEntity entity) {
        if (entity instanceof StrayDevil) return true;
        return entity instanceof Player p && data(p).getRace() == Race.DEVIL;
    }

    public static void setRace(ServerPlayer player, Race race, Piece piece) {
        DxDData d = data(player);
        d.setRace(race);
        d.setPiece(race == Race.DEVIL ? piece : Piece.NONE);
        d.setMana(d.getMaxMana());
        updateFlight(player, d);

        ServerLevel level = player.serverLevel();
        switch (race) {
            case DEVIL -> {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1, player.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            case ANGEL -> {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            case FALLEN_ANGEL -> {
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            case HUMAN -> {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        }
    }

    public static Component describe(DxDData d) {
        MutableComponent c = Component.literal(d.getRace().displayName()).withStyle(d.getRace().color());
        if (d.getRace() == Race.DEVIL && d.getPiece() != Piece.NONE) {
            c.append(Component.literal(" (" + d.getPiece().displayName() + ")").withStyle(ChatFormatting.RED));
        }
        return c;
    }

    /** Called once per second for every server player. */
    public static void secondTick(ServerPlayer player) {
        DxDData d = data(player);
        checkLevelUp(player, d);
        Destiny.secondTick(player, d);
        GearGrowth.secondTick(player, d);
        ArmorSets.secondTick(player, d);
        stateAchievements(player, d);
        ElementCasting.passives(player, d);
        Race race = d.getRace();
        boolean day = player.level().isDay();

        // Juggernaut Drive overrides everything else while active.
        if (d.jdActive()) {
            JuggernautDrive.secondTick(player, d);
            if (d.jdActive()) {
                player.displayClientMessage(Component.literal(JuggernautDrive.name(d.getJdType(), d.isJdComplete()) + "  " + d.getJdSeconds() + "s")
                        .withStyle(d.getJdType() == DxDData.JD_RED ? ChatFormatting.RED : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
                updateFlight(player, d);
                return;
            }
        }

        RageMeter.secondTick(player, d);
        if (d.jdActive()) return; // rage just triggered a drive

        // Mana: flying drains it, otherwise it regenerates.
        boolean flyingWithWings = race.hasWings() && player.getAbilities().flying && !player.isCreative() && !player.isSpectator();
        if (flyingWithWings) {
            d.setMana(d.getMana() - 1);
        } else {
            int regen = d.getManaRegen();
            if (race == Race.DEVIL && !day) regen += 1;      // devils thrive at night
            if (race == Race.ANGEL && day) regen += 1;        // angels thrive in daylight
            d.setMana(d.getMana() + regen);
        }
        updateFlight(player, d);

        // Race perks
        switch (race) {
            case DEVIL -> {
                if (!day) effect(player, new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                Piece p = d.getPiece();
                if (p.speed()) effect(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false, true));
                if (p.power()) {
                    effect(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
                    effect(player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                }
                if (p == Piece.PAWN) effect(player, new MobEffectInstance(MobEffects.JUMP, 60, 0, true, false, true));
            }
            case ANGEL -> {
                if (day && player.tickCount % 80 < 20 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
            }
            case FALLEN_ANGEL -> {
                if (!day) effect(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            }
            default -> {}
        }

        // Realm effects: Heaven's holy air burns Devils; the Underworld's miasma weakens Angels.
        if (com.dxdcraft.world.DimensionTravel.inHeaven(player) && race == Race.DEVIL) {
            effect(player, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
            if (player.tickCount % 100 < 20) player.hurt(player.damageSources().magic(), 1.0F);
        } else if (com.dxdcraft.world.DimensionTravel.inUnderworld(player) && race == Race.ANGEL) {
            effect(player, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        } else if (com.dxdcraft.world.DimensionTravel.inUnderworld(player) && race == Race.DEVIL) {
            d.setMana(d.getMana() + 2); // home turf
        } else if (com.dxdcraft.world.DimensionTravel.inHeaven(player) && race == Race.ANGEL) {
            d.setMana(d.getMana() + 2);
        }

        tickBoostedGear(player, d);

        // ULT gauge slowly fills on its own (1% every 4 seconds)
        if (player.tickCount % 80 == 0) d.addUltCharge(1);

        // Status line while holding a DxD item or flying
        if (holdingModItem(player) || flyingWithWings || d.ultReady() || d.getRage() > 0) {
            MutableComponent line = Component.literal("Lv " + d.getLevel() + "  ").withStyle(ChatFormatting.LIGHT_PURPLE).append(describe(d))
                    .append(Component.literal("   Mana " + d.getMana() + "/" + d.getMaxMana()).withStyle(ChatFormatting.AQUA));
            if (d.getElement1() != null) line.append(Component.literal("   ")).append(Destiny.elements(d));
            line.append(Component.literal("   ULT " + d.getUltCharge() + "%")
                    .withStyle(d.ultReady() ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
            if (d.getRage() > 0) {
                line.append(Component.literal("   RAGE " + d.getRage() + "%")
                        .withStyle(d.getRage() >= 75 ? ChatFormatting.DARK_RED : ChatFormatting.RED, ChatFormatting.BOLD));
            }
            if (d.getBoosts() > 0) {
                line.append(Component.literal("   Boost x" + d.getBoosts()).withStyle(ChatFormatting.GREEN));
            }
            player.displayClientMessage(line, true);
        }
    }

    /** Achievements that depend on the player's current state (checked once per second). */
    private static void stateAchievements(ServerPlayer p, DxDData d) {
        if (p.tickCount % 100 != 0) return; // every 5 seconds is plenty
        DxDAdvancements.grant(p, "root");
        if (d.getRace() != Race.HUMAN) DxDAdvancements.grant(p, "born_anew");
        if (d.getLevel() >= 20) DxDAdvancements.grant(p, "level_20");
        if (d.getLevel() >= 50) DxDAdvancements.grant(p, "level_50");
        if (d.getLevel() >= 100) DxDAdvancements.grant(p, "level_100");
        if (d.getElement2() != null) DxDAdvancements.grant(p, "twin_elements");
        if (d.isGearAwakened()) DxDAdvancements.grant(p, "sacred_awakening");
        for (String gid : d.grownGears()) {
            if (d.getGearStage(gid) >= 3) DxDAdvancements.grant(p, "balance_breaker");
            if (d.getGearStage(gid) >= 4) DxDAdvancements.grant(p, "sublimation");
        }
    }

    /** Spend mana for an ability. Creative is free. Tells the player when they are short. */
    public static boolean spendMana(Player player, int cost) {
        if (player.getAbilities().instabuild) return true;
        int stage = data(player).getCastingStage();
        if (stage > 1) cost = Math.max(1, Math.round(cost * GearGrowth.manaDiscount(stage)));
        if (data(player).tryUseMana(cost)) return true;
        player.displayClientMessage(Component.literal("Not enough mana (" + cost + " needed, "
                + data(player).getMana() + " left)").withStyle(ChatFormatting.AQUA), true);
        return false;
    }

    /** Awards DxD XP and announces any level-ups right away. */
    public static void giveXp(ServerPlayer player, int amount) {
        DxDData d = data(player);
        d.addXp(amount);
        checkLevelUp(player, d);
    }

    public static void checkLevelUp(ServerPlayer player, DxDData d) {
        int lvl = d.getLevel();
        int last = d.getAnnouncedLevel();
        d.setAnnouncedLevel(lvl);
        if (last < 0 || lvl <= last) return; // first check after login, or level went down via command
        player.sendSystemMessage(Component.literal("DxD Level Up! ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("Lv " + last + " -> Lv " + lvl + "  (max mana " + d.getMaxMana() + ")").withStyle(ChatFormatting.WHITE)));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (last < DxDData.JD_LEVEL && lvl >= DxDData.JD_LEVEL) {
            player.sendSystemMessage(Component.literal("Your Sacred Gear stirs... the Juggernaut Drive is now available! [J]")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        if (last < DxDData.JD_COMPLETE_LEVEL && lvl >= DxDData.JD_COMPLETE_LEVEL) {
            player.sendSystemMessage(Component.literal("Your body can now withstand a COMPLETE Juggernaut Drive (needs a mastered gear).")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    private static void effect(ServerPlayer player, MobEffectInstance inst) {
        player.addEffect(inst);
    }

    private static boolean holdingModItem(Player player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return isModItem(main) || isModItem(off) || com.dxdcraft.compat.CuriosCompat.wornGear(player) != null;
    }

    private static boolean isModItem(Item item) {
        return item instanceof com.dxdcraft.item.SacredGearItem || item == ModItems.DESTRUCTION_SIGIL.get()
                || item == ModItems.LIGHT_SPEAR.get();
    }

    public static boolean holdingBoostedGear(Player player) {
        return player.getMainHandItem().is(ModItems.BOOSTED_GEAR.get()) || player.getOffhandItem().is(ModItems.BOOSTED_GEAR.get())
                || com.dxdcraft.compat.CuriosCompat.has(player, ModItems.BOOSTED_GEAR.get());
    }

    private static void tickBoostedGear(ServerPlayer player, DxDData d) {
        if (!holdingBoostedGear(player)) {
            d.setBoosts(0);
            d.setBoostTimer(0);
            return;
        }
        if (d.getBoosts() >= MAX_BOOSTS) return;
        d.setBoostTimer(d.getBoostTimer() + 1);
        if (d.getBoostTimer() >= SECONDS_PER_BOOST) {
            d.setBoostTimer(0);
            d.setBoosts(d.getBoosts() + 1);
            player.displayClientMessage(Component.literal("BOOST! ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(Component.literal("x" + d.getBoosts()).withStyle(ChatFormatting.WHITE)), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.6F + d.getBoosts() * 0.08F);
            player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.4, 0.5, 0.4, 0.02);
        }
    }

    /** Grants or removes wing flight. Creative/spectator flight is never touched. */
    public static void updateFlight(ServerPlayer player, DxDData d) {
        if (player.isCreative() || player.isSpectator()) return;
        boolean shouldFly = (d.getRace().hasWings() && d.getMana() > 0) || d.jdActive();
        var abilities = player.getAbilities();
        if (abilities.mayfly != shouldFly) {
            abilities.mayfly = shouldFly;
            if (!shouldFly && abilities.flying) {
                abilities.flying = false;
                // Out of mana mid-air: wings give out gently instead of a deadly drop.
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false, true));
            }
            player.onUpdateAbilities();
        }
    }
}
