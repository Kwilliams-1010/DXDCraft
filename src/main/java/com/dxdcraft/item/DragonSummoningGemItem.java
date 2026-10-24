package com.dxdcraft.item;

import com.dxdcraft.entity.HeavenlyDragon;
import com.dxdcraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Use on the ground to summon the Heavenly Dragon boss. */
public class DragonSummoningGemItem extends Item {
    public DragonSummoningGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().above(4);

        HeavenlyDragon dragon = ModEntities.HEAVENLY_DRAGON.get().create(level);
        if (dragon == null) return InteractionResult.FAIL;
        dragon.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player != null ? player.getYRot() + 180F : 0F, 0F);
        dragon.setPersistenceRequired();
        level.addFreshEntity(dragon);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 120, 2, 2, 2, 0.1);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.8F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("A Heavenly Dragon has awakened!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        ItemStack stack = context.getItemInHand();
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on the ground to summon a Heavenly Dragon").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Boss fight! Bring friends.").withStyle(ChatFormatting.RED));
    }
}
