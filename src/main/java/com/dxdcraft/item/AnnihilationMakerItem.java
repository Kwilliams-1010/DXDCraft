package com.dxdcraft.item;

import com.dxdcraft.data.DxDData;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Creates loyal creatures out of shadow. */
public class AnnihilationMakerItem extends SacredGearItem {
    public static final int MANA_COST = 60;

    public AnnihilationMakerItem(Properties properties) {
        super(properties);
    }

    private static void puff(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 30, 0.4, 0.5, 0.4, 0.02);
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
        for (int i = 0; i < 2; i++) {
            Wolf wolf = EntityType.WOLF.create(server);
            if (wolf == null) continue;
            double x = player.getX() + (i == 0 ? 1.5 : -1.5);
            wolf.moveTo(x, player.getY(), player.getZ(), player.getYRot(), 0.0F);
            wolf.tame(player);
            wolf.setCustomName(Component.literal("Annihilation Beast").withStyle(ChatFormatting.DARK_GRAY));
            AttributeInstance hp = wolf.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(40.0);
            AttributeInstance atk = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(7.0);
            wolf.setHealth(40.0F);
            server.addFreshEntity(wolf);
            puff(server, x, player.getY(), player.getZ());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("Annihilation Beasts created!").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), true);
        player.getCooldowns().addCooldown(this, 600);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Anti-Monster Legion"; }

    @Override
    public String ultimateDesc() { return "Creates two empowered iron guardians that fight for you"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem == null) continue;
            double x = player.getX() + (i == 0 ? 2.5 : -2.5);
            golem.moveTo(x, player.getY(), player.getZ(), player.getYRot(), 0.0F);
            golem.setPlayerCreated(true);
            golem.setCustomName(Component.literal("Anti-Monster").withStyle(ChatFormatting.DARK_GRAY));
            golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1));
            golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
            level.addFreshEntity(golem);
            puff(level, x, player.getY(), player.getZ());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use: create 2 tamed Annihilation Beasts (" + MANA_COST + " mana, 30s cooldown)"));
    }
}
