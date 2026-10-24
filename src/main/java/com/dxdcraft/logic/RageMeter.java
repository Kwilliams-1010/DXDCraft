package com.dxdcraft.logic;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The dragon sealed in your gear feeds on your anger.
 *
 * Who has a Rage meter: players at DxD Lv 20+ carrying the Boosted Gear or Divine Dividing
 * (anywhere in their inventory).
 *
 * What fills it:
 *  - taking damage (1.5 rage per point of damage)
 *  - being at low health (under 30%): +3 every second
 *  - a player dying within 24 blocks: +40
 *  - one of your pets dying: +25
 *  - a villager being killed within 16 blocks: +10
 *
 * It drains by 1 per second once you've gone 10 seconds without being hurt and are above half health,
 * and quickly (5/s) if you stop carrying a dragon gear.
 *
 * At 100% the dragon takes over: an INCOMPLETE Juggernaut Drive starts on its own, with no ULT gauge,
 * mana or key press needed, even if you have mastered the gear. The meter then resets to 0.
 */
public final class RageMeter {
    public static final double ALLY_DEATH_RANGE = 24.0;
    public static final double VILLAGER_DEATH_RANGE = 16.0;

    private RageMeter() {}

    /** Which dragon gear the player carries: hands first, then inventory. JD_NONE if none. */
    public static int dragonGear(Player p) {
        Item red = ModItems.BOOSTED_GEAR.get();
        Item white = ModItems.DIVINE_DIVIDING.get();
        if (p.getMainHandItem().is(red) || p.getOffhandItem().is(red)) return DxDData.JD_RED;
        if (p.getMainHandItem().is(white) || p.getOffhandItem().is(white)) return DxDData.JD_WHITE;
        int found = DxDData.JD_NONE;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(red)) return DxDData.JD_RED;
            if (s.is(white)) found = DxDData.JD_WHITE;
        }
        return found;
    }

    public static boolean eligible(ServerPlayer p, DxDData d) {
        return !p.isSpectator() && d.getLevel() >= DxDData.JD_LEVEL && dragonGear(p) != DxDData.JD_NONE;
    }

    /** Adds rage. {@code reason} (may be null) is shown in chat when it's a big emotional trigger. */
    public static void add(ServerPlayer p, float amount, String reason) {
        DxDData d = DxDLogic.data(p);
        if (d.jdActive() || !eligible(p, d) || amount <= 0) return;
        if (reason != null) {
            p.sendSystemMessage(Component.literal(reason).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        d.setRage(d.getRage() + Math.max(1, Math.round(amount)));
        warn(p, d);
        if (d.getRage() >= DxDData.MAX_RAGE) trigger(p, d);
    }

    public static void onHurt(ServerPlayer p, float amount) {
        DxDData d = DxDLogic.data(p);
        d.setLastHurtTime(p.level().getGameTime());
        add(p, amount * 1.5F, null);
    }

    /** Called once per second (when no drive is active). */
    public static void secondTick(ServerPlayer p, DxDData d) {
        if (!eligible(p, d)) {
            if (d.getRage() > 0) d.setRage(d.getRage() - 5);
            return;
        }
        float hpFrac = p.getHealth() / p.getMaxHealth();
        if (hpFrac < 0.3F) {
            add(p, 3, null);
            return;
        }
        long calm = p.level().getGameTime() - d.getLastHurtTime();
        if (calm > 200 && hpFrac > 0.5F && d.getRage() > 0) d.setRage(d.getRage() - 1);

        if (d.getRage() >= 75 && p.tickCount % 40 < 20) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void warn(ServerPlayer p, DxDData d) {
        int rage = d.getRage();
        int stage = rage >= 90 ? 3 : rage >= 75 ? 2 : rage >= 50 ? 1 : 0;
        if (stage <= d.getRageStage()) return;
        d.setRageStage(stage);
        String msg = switch (stage) {
            case 1 -> "Something inside your Sacred Gear stirs... (Rage 50%)";
            case 2 -> "The dragon is waking. Calm down! (Rage 75%)";
            default -> "You're losing control! (Rage 90%)";
        };
        p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.4F + stage * 0.2F, 0.5F);
        p.serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2.0, p.getZ(), 3 * stage, 0.4, 0.2, 0.4, 0.0);
    }

    private static void trigger(ServerPlayer p, DxDData d) {
        int type = dragonGear(p);
        if (type == DxDData.JD_NONE) return;
        d.setRage(0);
        d.setRageStage(0);
        p.sendSystemMessage(Component.literal("Your rage boils over. The dragon takes control!")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        JuggernautDrive.start(p, type, false); // rage-triggered drives are always incomplete
    }
}
