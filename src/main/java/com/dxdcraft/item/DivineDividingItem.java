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

/** Every melee hit "Divides" the target (weaken + slow) and feeds the wielder. */
public class DivineDividingItem extends SacredGearItem {
    public static final int MANA_COST = 30;
    public static final int COMPRESSION_MANA = 40;

    public DivineDividingItem(Properties properties) {
        super(properties.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 2, -2.0F)));
    }

    public static void divide(Player user, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        user.heal(1.0F);
        user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        if (user.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        }
    }

    @Override
    protected InteractionResultHolder<ItemStack> activeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        DxDData d = DxDLogic.data(player);
        if (d.jdActive() && d.isJdComplete() && d.getJdType() == DxDData.JD_WHITE) {
            // Compression Divider: crush one target down, halving it again and again
            LivingEntity target = Abilities.lookTarget(player, 24.0);
            if (target == null) {
                player.displayClientMessage(Component.literal("No target in sight").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!DxDLogic.spendMana(player, COMPRESSION_MANA)) return InteractionResultHolder.fail(stack);
            float dmg = Math.min(target.getMaxHealth() * 0.5F, 80.0F);
            target.hurt(Abilities.magic((ServerPlayer) player), dmg);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 120, 0.2, 0.2, 0.2, 0.6);
                server.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 1, 0, 0, 0, 0);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 1.4F);
            player.displayClientMessage(Component.literal("COMPRESSION DIVIDER!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            player.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.consume(stack);
        }
        if (!player.getAbilities().instabuild && !d.tryUseMana(MANA_COST)) {
            player.displayClientMessage(Component.literal("Not enough mana (" + MANA_COST + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        List<LivingEntity> targets = Abilities.hostiles((ServerPlayer) player, 8.0);
        for (LivingEntity e : targets) divide(player, e);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.displayClientMessage(Component.literal("DIVIDE! ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(targets.size() + " target(s)").withStyle(ChatFormatting.WHITE)), true);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Half Dimension"; }

    @Override
    public String ultimateDesc() { return "Halves the health of every enemy within 16 blocks"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : Abilities.hostiles(player, 16.0)) {
            float half = Math.min(e.getHealth() / 2.0F, 60.0F); // bosses lose at most 60
            e.hurt(Abilities.magic(player), half);
            divide(player, e);
        }
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 300, 8, 2, 8, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 0.6F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Hit: weakens and slows the target, empowers you"));
        tooltip.add(line("Use: Divide every enemy within 8 blocks (" + MANA_COST + " mana)"));
        tooltip.add(Component.literal("[J] White Dragon Juggernaut Drive (full ULT gauge)").withStyle(ChatFormatting.AQUA));
        tooltip.add(line("  Complete drive: Use = Compression Divider (" + COMPRESSION_MANA + " mana)"));
    }
}
