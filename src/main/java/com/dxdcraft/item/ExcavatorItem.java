package com.dxdcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Mining tool: breaks a 3x3 area facing you (sneak to mine a single block). Each extra block goes
 * through the normal player block-breaking path, so drops, durability, protection and claim mods
 * all apply exactly as if you had mined it yourself.
 */
public class ExcavatorItem extends PickaxeItem {
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    public ExcavatorItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /** Called from the block-break event after the centre block was broken by this player. */
    public static void breakArea(ServerPlayer player, BlockPos center) {
        if (BUSY.get() || player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof ExcavatorItem)) return;
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        Direction face = hit instanceof BlockHitResult bhr ? bhr.getDirection() : Direction.UP;
        BUSY.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos pos = switch (face.getAxis()) {
                        case X -> center.offset(0, a, b);
                        case Y -> center.offset(a, 0, b);
                        case Z -> center.offset(a, b, 0);
                    };
                    BlockState state = player.level().getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(player.level(), pos) < 0) continue;
                    if (!tool.isCorrectToolForDrops(state)) continue;
                    if (tool.isEmpty() || !(player.getMainHandItem().getItem() instanceof ExcavatorItem)) return; // broke
                    player.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            BUSY.set(false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Mines a 3x3 area (sneak for a single block)").withStyle(ChatFormatting.GOLD));
    }
}
