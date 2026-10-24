package com.dxdcraft.item;

import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.DxDLogic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Rings that heal anyone, friend or foe, human or devil. */
public class TwilightHealingItem extends SacredGearItem {
    public TwilightHealingItem(Properties properties) {
        super(properties);
    }

    private static void heal(LivingEntity target, float amount) {
        target.heal(amount);
        target.clearFire();
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.5, 0.6, 0.5, 0.0);
            server.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (!DxDLogic.spendMana(player, 15)) return InteractionResultHolder.fail(stack);
            heal(player, 6.0F);
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            if (!DxDLogic.spendMana(player, 20)) return InteractionResult.FAIL;
            heal(target, 10.0F);
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public String ultimateName() { return "Saint's Miracle"; }

    @Override
    public String ultimateDesc() { return "Fully heals you and every ally within 16 blocks, cures bad effects"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        for (LivingEntity a : Abilities.allies(player, 16.0)) {
            heal(a, a.getMaxHealth());
            List<MobEffectInstance> bad = new ArrayList<>();
            for (MobEffectInstance inst : a.getActiveEffects()) {
                if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(inst);
            }
            for (MobEffectInstance inst : bad) a.removeEffect(inst.getEffect());
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
        }
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2, player.getZ(), 60, 6, 1, 6, 0.0);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use: heal yourself 3 hearts (15 mana)"));
        tooltip.add(line("Use on a creature: heal it 5 hearts (20 mana)"));
    }
}
