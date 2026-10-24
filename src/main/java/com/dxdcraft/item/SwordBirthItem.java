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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

import java.util.List;

/** Creates demonic blades. A strong sword that can also make blades erupt from the ground. */
public class SwordBirthItem extends SacredGearItem {
    public static final int MANA_COST = 25;

    public SwordBirthItem(Properties properties) {
        super(properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.4F)));
    }

    private static void bladesAt(ServerLevel level, LivingEntity e) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.2, e.getZ(), 20, 0.3, 0.8, 0.3, 0.3);
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        DxDData d = DxDLogic.data(player);
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerPlayer sp = (ServerPlayer) player;
        for (LivingEntity e : Abilities.hostiles(sp, 6.0)) {
            e.hurt(Abilities.magic(sp), 8.0F);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            bladesAt(server, e);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
        player.displayClientMessage(Component.literal("Blade Blackness!").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), true);
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Blade Blacksmith"; }

    @Override
    public String ultimateDesc() { return "A forest of swords: 20 damage to all enemies within 10 blocks and launches them"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : Abilities.hostiles(player, 10.0)) {
            e.hurt(Abilities.magic(player), 20.0F);
            e.push(0.0, 1.1, 0.0);
            e.hurtMarked = true;
            bladesAt(level, e);
        }
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2, player.getZ(), 400, 10, 0.5, 10, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Netherite-grade blade"));
        tooltip.add(line("Use: blades erupt under enemies within 6 blocks (" + MANA_COST + " mana)"));
    }
}
