package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.projectile.DestructionOrbEntity;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

/** Devil-only focus that fires the Power of Destruction. */
public class DestructionSigilItem extends Item {
    public static final int MANA_COST = 25;

    public DestructionSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        if (d.getRace() != Race.DEVIL) {
            player.displayClientMessage(Component.literal("Only Devils can wield the Power of Destruction.").withStyle(ChatFormatting.DARK_RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }

        DestructionOrbEntity orb = new DestructionOrbEntity(player, level);
        float dmg = d.getPiece().magic() ? 16.0F : 12.0F;
        orb.setDamage(dmg);
        orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.5F);
        level.addFreshEntity(orb);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7F, 1.4F);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Devils only. Fires the Power of Destruction").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("Costs " + MANA_COST + " mana. Erases soft blocks it hits").withStyle(ChatFormatting.GRAY));
    }
}
