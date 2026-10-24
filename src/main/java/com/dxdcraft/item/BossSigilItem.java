package com.dxdcraft.item;

import com.dxdcraft.logic.Combat;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.world.DimensionTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Seraph Sigil (Heaven) / Infernal Sigil (Underworld): summons that realm's boss. */
public class BossSigilItem extends Item {
    public enum Boss { SERAPH, OVERLORD }

    private final Boss boss;

    public BossSigilItem(Boss boss, Properties properties) {
        super(properties);
        this.boss = boss;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        boolean rightRealm = boss == Boss.SERAPH ? level.dimension() == DimensionTravel.HEAVEN : level.dimension() == DimensionTravel.UNDERWORLD;
        if (!rightRealm) {
            if (player != null) player.displayClientMessage(Component.literal(boss == Boss.SERAPH
                    ? "The sigil only answers in Heaven." : "The sigil only answers in the Underworld.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().above(boss == Boss.SERAPH ? 3 : 1);
        Mob mob = (boss == Boss.SERAPH ? ModEntities.SERAPH_GUARDIAN.get() : ModEntities.UNDERWORLD_OVERLORD.get()).create(level);
        if (mob == null) return InteractionResult.FAIL;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player != null ? player.getYRot() + 180F : 0F, 0F);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        level.sendParticles(boss == Boss.SERAPH ? ParticleTypes.END_ROD : ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 150, 1.5, 2, 1.5, 0.1);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, pos, boss == Boss.SERAPH ? SoundEvents.BEACON_ACTIVATE : SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.8F);
        Component msg = Component.literal(boss == Boss.SERAPH ? "The Seraph Guardian descends!" : "The Underworld Overlord rises!")
                .withStyle(boss == Boss.SERAPH ? ChatFormatting.GOLD : ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        if (player instanceof ServerPlayer sp) Combat.announce(sp, msg);
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(boss == Boss.SERAPH ? "Use on the ground in Heaven to summon the Seraph Guardian"
                : "Use on the ground in the Underworld to summon the Underworld Overlord").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Boss fight! Bring friends.").withStyle(ChatFormatting.RED));
    }
}
