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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Controls a teleporting mist. */
public class DimensionLostItem extends SacredGearItem {
    public static final int MANA_COST = 20;

    public DimensionLostItem(Properties properties) {
        super(properties);
    }

    private static void mist(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 1, at.z, 40, 0.6, 0.8, 0.6, 0.02);
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
        // Mist Step: blink up to 12 blocks where you're looking, and enemies lose track of you.
        HitResult hit = player.pick(12.0, 1.0F, false);
        Vec3 dest = hit.getLocation();
        if (hit.getType() != HitResult.Type.MISS) dest = dest.subtract(player.getViewVector(1.0F).scale(0.6));
        mist(server, player.position());
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();
        mist(server, dest);
        for (LivingEntity e : Abilities.hostiles((ServerPlayer) player, 16.0)) {
            if (e instanceof Mob m && m.getTarget() == player) m.setTarget(null);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public String ultimateName() { return "Mist Banishment"; }

    @Override
    public String ultimateDesc() { return "Every enemy within 16 blocks is swallowed by mist and sent far away"; }

    @Override
    public boolean performUltimate(ServerPlayer player, ServerLevel level) {
        List<LivingEntity> targets = Abilities.hostiles(player, 16.0);
        for (LivingEntity e : targets) {
            if (e.getMaxHealth() >= 200) continue; // bosses resist
            mist(level, e.position());
            Abilities.banish(e, level);
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 400, 8, 1.5, 8, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 2.0F, 0.7F);
        return true;
    }

    @Override
    protected void gearTooltip(List<Component> tooltip) {
        tooltip.add(line("Use: Mist Step - teleport up to 12 blocks ahead (" + MANA_COST + " mana)"));
        tooltip.add(line("Nearby enemies lose track of you"));
    }
}
