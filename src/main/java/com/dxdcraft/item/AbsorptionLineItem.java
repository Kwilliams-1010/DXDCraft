package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A line that latches onto a target and drains its life and power. */
public class AbsorptionLineItem extends SacredGearItem {
    public AbsorptionLineItem(Properties properties) {
        super(properties);
    }

    private static void drawLine(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 a = from.getEyePosition().subtract(0, 0.3, 0);
        Vec3 b = to.position().add(0, to.getBbHeight() / 2, 0);
        int steps = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(1, steps));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        LivingEntity target = Abilities.lookTarget(player, 16.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("No target in sight").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!DxDLogic.spendMana(player, 10)) return InteractionResultHolder.fail(stack);
        ServerPlayer sp = (ServerPlayer) player;
        target.hurt(Abilities.magic(sp), 5.0F);
        player.heal(3.0F);
        DxDData d = DxDLogic.data(player);
        d.setMana(d.getMana() + 25);
        drawLine(server, player, target);
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Line Drain"; }

    @Override
    public String ultimateDesc() { return "Drains every enemy within 12 blocks; you heal and refill mana"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        List<LivingEntity> targets = Abilities.hostiles(player, 12.0);
        float drained = 0;
        for (LivingEntity e : targets) {
            float before = e.getHealth();
            e.hurt(Abilities.magic(player), 8.0F);
            drained += Math.max(0, before - e.getHealth());
            drawLine(level, player, e);
        }
        player.heal(Math.max(4.0F, drained * 0.5F));
        DxDData d = DxDLogic.data(player);
        d.setMana(d.getMaxMana());
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use on what you're looking at (16 blocks): drain 5 HP"));
        tooltip.add(line("Costs 10 mana; you heal 1.5 hearts and steal back 25 mana"));
    }
}
