package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Dropped by the Heavenly Dragon. Use it while holding the Boosted Gear or Divine Dividing
 * in your other hand to master that gear, unlocking its COMPLETE Juggernaut Drive.
 */
public class DragonHeartItem extends Item {
    public DragonHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        DxDData d = DxDLogic.data(player);
        String gearName;
        if (other.is(ModItems.BOOSTED_GEAR.get())) {
            if (d.isMasteredRed()) return already(player, stack);
            d.setMasteredRed(true);
            gearName = "Red Dragon";
        } else if (other.is(ModItems.DIVINE_DIVIDING.get())) {
            if (d.isMasteredWhite()) return already(player, stack);
            d.setMasteredWhite(true);
            gearName = "White Dragon";
        } else {
            player.displayClientMessage(Component.literal("Hold the Boosted Gear or Divine Dividing in your other hand").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 120, 0.6, 1.0, 0.6, 0.4);
        server.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 1.4F);
        player.sendSystemMessage(Component.literal("You and the " + gearName + " are now one. ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Complete Juggernaut Drive unlocked!").withStyle(ChatFormatting.YELLOW)));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    private static InteractionResultHolder<ItemStack> already(Player player, ItemStack stack) {
        player.displayClientMessage(Component.literal("You have already mastered this gear.").withStyle(ChatFormatting.GRAY), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use with the Boosted Gear or Divine Dividing").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("in your other hand to master it.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Unlocks the COMPLETE Juggernaut Drive.").withStyle(ChatFormatting.GRAY));
    }
}
