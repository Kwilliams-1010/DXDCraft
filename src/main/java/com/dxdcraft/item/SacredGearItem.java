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
        if (level.isClientSide) return activeUse(level, player, hand);
        // Server: run the skill at this gear's growth stage (power + mana discount), then award gear XP.
        ServerPlayer sp = (ServerPlayer) player;
        com.dxdcraft.data.DxDData d = com.dxdcraft.logic.DxDLogic.data(sp);
        d.setCastingStage(com.dxdcraft.logic.GearGrowth.stage(sp, this));
        try {
            InteractionResultHolder<ItemStack> result = activeUse(level, player, hand);
            if (result.getResult().consumesAction()) com.dxdcraft.logic.GearGrowth.addXp(sp, this, 5);
            return result;
        } finally {
            d.setCastingStage(0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Sacred Gear").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        int stage = com.dxdcraft.logic.GearGrowth.stackStage(stack);
        String gid = com.dxdcraft.logic.GearGrowth.id(this);
        ChatFormatting sc = stage >= 4 ? ChatFormatting.LIGHT_PURPLE : stage >= 3 ? ChatFormatting.GOLD : ChatFormatting.AQUA;
        tooltip.add(Component.literal(com.dxdcraft.logic.GearGrowth.label(stage)).withStyle(sc, ChatFormatting.BOLD));
        if (stage < 4) {
            tooltip.add(Component.literal("  Gear XP " + com.dxdcraft.logic.GearGrowth.stackXp(stack) + "/"
                    + com.dxdcraft.logic.GearGrowth.xpForStage(gid, stage + 1) + ", needs Lv "
                    + com.dxdcraft.logic.GearGrowth.levelForStage(stage + 1) + " for the next stage").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stage > 1) {
            tooltip.add(Component.literal("  +" + Math.round((com.dxdcraft.logic.GearGrowth.power(stage) - 1) * 100) + "% skill power, -"
                    + Math.round((1 - com.dxdcraft.logic.GearGrowth.manaDiscount(stage)) * 100) + "% mana cost").withStyle(ChatFormatting.DARK_GRAY));
        }
        gearTooltip(tooltip);
        tooltip.add(Component.literal("Ultimate: " + ultimateName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  " + ultimateDesc()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(stage >= 3 ? "  Full ULT gauge + " + Abilities.ULT_MANA_COST + " mana, then [G] or Sneak+Use"
                : "  Locked until Stage III: Balance Breaker").withStyle(stage >= 3 ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
    }

    protected static Component line(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }
}
