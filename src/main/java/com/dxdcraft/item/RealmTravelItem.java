package com.dxdcraft.item;

import com.dxdcraft.world.DimensionTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
 * Heaven's Gate Key / Underworld Magic Circle. Use it to travel to that realm; use it again
 * while there to return to the Overworld. Reusable, with a 10 second cooldown.
 */
public class RealmTravelItem extends Item {
    private final ResourceKey<Level> realm;
    private final String realmName;

    public RealmTravelItem(ResourceKey<Level> realm, String realmName, Properties properties) {
        super(properties);
        this.realm = realm;
        this.realmName = realmName;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer sp = (ServerPlayer) player;
        ResourceKey<Level> dest = level.dimension() == realm ? Level.OVERWORLD : realm;
        if (level.dimension() != Level.OVERWORLD && level.dimension() != realm) {
            player.displayClientMessage(Component.literal("This only works from the Overworld or " + realmName + ".").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (DimensionTravel.travel(sp, dest)) {
            player.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use: travel to " + realmName).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use again there to return to the Overworld").withStyle(ChatFormatting.GRAY));
    }
}
