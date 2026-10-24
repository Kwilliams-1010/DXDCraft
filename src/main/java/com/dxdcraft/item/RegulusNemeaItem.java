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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The lion's power, worn as a golden medallion that hits like an axe. */
public class RegulusNemeaItem extends SacredGearItem {
    public static final int MANA_COST = 30;

    public RegulusNemeaItem(Properties properties) {
        super(properties.attributes(AxeItem.createAttributes(Tiers.DIAMOND, 6.0F, -3.0F)));
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
        // Lion's Roar: blast enemies back and weaken them
        for (LivingEntity e : Abilities.hostiles((ServerPlayer) player, 7.0)) {
            Vec3 push = e.position().subtract(player.position()).normalize().scale(1.8);
            e.push(push.x, 0.4, push.z);
            e.hurtMarked = true;
            e.hurt(Abilities.magic((ServerPlayer) player), 4.0F);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
        }
        server.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 1.0F);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Lion King Rage"; }

    @Override
    public String ultimateDesc() { return "45s of Strength III, Resistance II, Speed, Regeneration II, Knockback immunity"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 900, 2));
        player.heal(12.0F);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 120, 0.6, 1.2, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 3.0F, 0.6F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Hits like a diamond axe"));
        tooltip.add(line("Use: Lion's Roar - blast back enemies within 7 blocks (" + MANA_COST + " mana)"));
    }
}
