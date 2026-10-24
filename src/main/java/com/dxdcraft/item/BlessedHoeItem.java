package com.dxdcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Farming tool. Sneak + use on any crop/sapling/grass: blesses a 5x5 area, giving every growable
 * plant a bonemeal growth pulse. Normal use tills like a hoe.
 */
public class BlessedHoeItem extends HoeItem {
    public BlessedHoeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return super.useOn(ctx);
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;

        BlockPos center = ctx.getClickedPos();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(level, pos, state)) {
                if (b.isBonemealSuccess(level, level.random, pos, state)) b.performBonemeal(level, level.random, pos, state);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                grown++;
            }
        }
        if (grown == 0) return InteractionResult.PASS;
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        ctx.getItemInHand().hurtAndBreak(Math.max(1, grown / 4), player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Sneak + use: bless a 5x5 area of crops (growth pulse)").withStyle(ChatFormatting.GREEN));
    }
}
