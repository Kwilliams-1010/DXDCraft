package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** An eye that stops time for whatever it sees. */
public class ForbiddenBalorViewItem extends SacredGearItem {
    public static final int MANA_COST = 40;

    public ForbiddenBalorViewItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        LivingEntity target = Abilities.lookTarget(player, 24.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("No target in sight").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        DxDData d = DxDLogic.data(player);
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        Abilities.freeze(target, 100);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(), 40, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
        player.displayClientMessage(Component.literal("Freeze Gaze!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Time Stop"; }

    @Override
    public String ultimateDesc() { return "Freezes every enemy within 20 blocks for 8 seconds"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        List<LivingEntity> targets = Abilities.hostiles(player, 20.0);
        for (LivingEntity e : targets) {
            Abilities.freeze(e, 160);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use: Freeze Gaze - stop what you're looking at for 5s (24 blocks, " + MANA_COST + " mana)"));
    }
}
