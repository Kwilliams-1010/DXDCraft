package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Holy Feather (Angel), Fallen Feather (Fallen Angel) and Purification Scroll (back to Human). */
public class RaceChangeItem extends Item {
    private final Race target;

    public RaceChangeItem(Race target, Properties properties) {
        super(properties);
        this.target = target;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        Race current = d.getRace();
        if (current == target) {
            player.displayClientMessage(Component.literal("Nothing happens.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (target == Race.ANGEL && current != Race.HUMAN) {
            player.displayClientMessage(Component.literal("Only a pure human can ascend. Purify yourself first.").withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.fail(stack);
        }
        if (target == Race.FALLEN_ANGEL && current == Race.DEVIL) {
            player.displayClientMessage(Component.literal("A Devil cannot take the black wings.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return InteractionResultHolder.fail(stack);
        }

        DxDLogic.setRace((ServerPlayer) player, target, Piece.NONE);
        String msg = switch (target) {
            case ANGEL -> "White wings unfold. You are now an Angel.";
            case FALLEN_ANGEL -> "Your wings turn black. You are now a Fallen Angel.";
            case HUMAN -> "You feel ordinary again. You are Human.";
            default -> "";
        };
        player.sendSystemMessage(Component.literal(msg).withStyle(target.color()));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String text = switch (target) {
            case ANGEL -> "Use to become an Angel (humans only)";
            case FALLEN_ANGEL -> "Use to become a Fallen Angel";
            case HUMAN -> "Use to return to being Human";
            default -> "";
        };
        tooltip.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }
}
