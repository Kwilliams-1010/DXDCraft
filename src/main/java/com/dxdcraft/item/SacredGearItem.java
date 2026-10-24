package com.dxdcraft.item;

import com.dxdcraft.logic.Abilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Base for every Sacred Gear. Normal use = the gear's active skill.
 * Sneak + use (or the Ultimate key) with a full ULT gauge = the gear's ultimate.
 */
public abstract class SacredGearItem extends Item {
    protected SacredGearItem(Properties properties) {
        super(properties);
    }

    /** Short label shown in the title when the ultimate fires. */
    public abstract String ultimateName();

    /** One-line ultimate description for the tooltip. */
    public abstract String ultimateDesc();

    /** Gear-specific tooltip lines (active skill etc). */
    protected abstract void gearTooltip(List<Component> tooltip);

    /** Server side. Returns true if the ultimate actually happened (gauge is then spent). */
    public abstract boolean performUltimate(ServerPlayer player, ServerLevel level);

    /** The gear's normal right-click skill. */
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public final InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) Abilities.tryGearUltimate((ServerPlayer) player, this);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return activeUse(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Sacred Gear").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        gearTooltip(tooltip);
        tooltip.add(Component.literal("Ultimate: " + ultimateName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  " + ultimateDesc()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Full ULT gauge + " + Abilities.ULT_MANA_COST + " mana, then [G] or Sneak+Use").withStyle(ChatFormatting.DARK_GRAY));
    }

    protected static Component line(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }
}
