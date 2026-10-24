package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Command over the weather itself. */
public class ZenithTempestItem extends SacredGearItem {
    public static final int MANA_COST = 30;

    public ZenithTempestItem(Properties properties) {
        super(properties);
    }

    private static void strike(ServerLevel level, ServerPlayer player, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at);
        bolt.setCause(player);
        level.addFreshEntity(bolt);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        LivingEntity target = Abilities.lookTarget(player, 32.0);
        Vec3 at;
        if (target != null) {
            at = target.position();
        } else {
            HitResult hit = player.pick(32.0, 1.0F, false);
            if (hit.getType() == HitResult.Type.MISS) {
                player.displayClientMessage(Component.literal("Nothing in range to strike").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            at = hit.getLocation();
        }
        if (at.distanceTo(player.position()) < 3.0) {
            player.displayClientMessage(Component.literal("Too close!").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        DxDData d = DxDLogic.data(player);
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        strike(server, (ServerPlayer) player, at);
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Tempest Storm"; }

    @Override
    public String ultimateDesc() { return "Summons a thunderstorm and strikes every enemy within 20 blocks"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        level.setWeatherParameters(0, 2400, true, true);
        List<LivingEntity> targets = Abilities.hostiles(player, 20.0);
        for (LivingEntity e : targets) {
            if (e.distanceTo(player) > 3.0F) strike(level, player, e.position());
        }
        if (targets.isEmpty()) {
            BlockPos p = player.blockPosition();
            for (int i = 0; i < 4; i++) {
                strike(level, player, Vec3.atBottomCenterOf(p.offset(level.random.nextInt(21) - 10, 0, level.random.nextInt(21) - 10)));
            }
        }
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use: call lightning on what you're looking at (32 blocks, " + MANA_COST + " mana)"));
    }
}
