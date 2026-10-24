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

/** Right-click to be reincarnated as a Devil holding this piece (or swap pieces if already a Devil). */
public class EvilPieceItem extends Item {
    private final Piece piece;

    public EvilPieceItem(Piece piece, Properties properties) {
        super(properties);
        this.piece = piece;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        if (d.getRace().usesLight()) {
            player.displayClientMessage(Component.literal("Your light rejects the Evil Piece.").withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.fail(stack);
        }
        if (d.getRace() == Race.DEVIL && d.getPiece() == piece) {
            player.displayClientMessage(Component.literal("You already serve as a " + piece.displayName() + ".").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        DxDLogic.setRace((ServerPlayer) player, Race.DEVIL, piece);
        player.sendSystemMessage(Component.literal("You have been reincarnated as a Devil ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal("[" + piece.displayName() + "]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use to become a Devil").withStyle(ChatFormatting.DARK_RED));
        String perk = switch (piece) {
            case PAWN -> "Pawn: higher jumps";
            case KNIGHT -> "Knight: great speed";
            case BISHOP -> "Bishop: double mana and regen";
            case ROOK -> "Rook: strength and toughness";
            case QUEEN -> "Queen: all piece traits";
            default -> "";
        };
        tooltip.add(Component.literal(perk).withStyle(ChatFormatting.GRAY));
    }
}
