package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.entity.projectile.DragonShotEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Gauntlet that gains a Boost every 10 seconds while held (max 10).
 * Punch releases all boosts as bonus damage; use fires them as a Dragon Shot.
 */
public class BoostedGearItem extends SacredGearItem {
    public static final int MIN_BOOSTS_FOR_SHOT = 2;
    public static final int SHOT_MANA = 10;
    public static final int SMASHER_MANA = 40;

    public BoostedGearItem(Properties properties) {
        super(properties.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 2, -2.0F)));
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        if (d.jdActive() && d.isJdComplete() && d.getJdType() == DxDData.JD_RED) {
            // Longinus Smasher: the chest cannon of the completed drive
            if (!DxDLogic.spendMana(player, SMASHER_MANA)) return InteractionResultHolder.fail(stack);
            DragonShotEntity smasher = new DragonShotEntity(player, level);
            smasher.setDamage(80.0F);
            smasher.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.0F);
            level.addFreshEntity(smasher);
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.8F);
            player.displayClientMessage(Component.literal("LONGINUS SMASHER!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            player.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.consume(stack);
        }
        int boosts = d.getBoosts();
        if (boosts < MIN_BOOSTS_FOR_SHOT) {
            player.displayClientMessage(Component.literal("Need at least " + MIN_BOOSTS_FOR_SHOT + " boosts for a Dragon Shot").withStyle(ChatFormatting.GREEN), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!DxDLogic.spendMana(player, SHOT_MANA)) return InteractionResultHolder.fail(stack);

        DragonShotEntity shot = new DragonShotEntity(player, level);
        shot.setDamage(5.0F * boosts);
        shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 0.2F);
        level.addFreshEntity(shot);
        d.setBoosts(0);
        d.setBoostTimer(0);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.2F, 1.0F);
        player.displayClientMessage(Component.literal("DRAGON SHOT!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Balance Breaker: Scale Mail"; }

    @Override
    public String ultimateDesc() { return "Instant max boosts, 30s of Strength III, Resistance III, Speed II"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        DxDData d = DxDLogic.data(player);
        d.setBoosts(DxDLogic.MAX_BOOSTS);
        d.setBoostTimer(0);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 150, 0.6, 1.2, 0.6, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 1.2F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Hold: gain a Boost every " + DxDLogic.SECONDS_PER_BOOST + "s (max " + DxDLogic.MAX_BOOSTS + ")"));
        tooltip.add(line("Punch: spend all boosts for +50% damage each"));
        tooltip.add(line("Use: fire a Dragon Shot (" + MIN_BOOSTS_FOR_SHOT + "+ boosts, " + SHOT_MANA + " mana)"));
        tooltip.add(Component.literal("[J] Red Dragon Juggernaut Drive (full ULT gauge)").withStyle(ChatFormatting.RED));
        tooltip.add(line("  Complete drive: Use = Longinus Smasher (" + SMASHER_MANA + " mana)"));
    }
}
