package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.projectile.LightSpearEntity;
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

/** Angels and Fallen Angels conjure and throw spears of light. Never consumed. */
public class LightSpearItem extends Item {
    public static final int MANA_COST = 15;

    public LightSpearItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        if (!d.getRace().usesLight()) {
            player.displayClientMessage(Component.literal("The light slips through your fingers. (Angels and Fallen Angels only)").withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }

        LightSpearEntity spear = new LightSpearEntity(player, level);
        spear.setDamage(d.getRace() == Race.FALLEN_ANGEL ? 9.0F : 7.0F);
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.2F);
        level.addFreshEntity(spear);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Angels and Fallen Angels only").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Costs " + MANA_COST + " mana. Double damage to devils and undead").withStyle(ChatFormatting.GRAY));
    }
}
