package com.dxdcraft.event;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.command.DxDCommand;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.projectile.HolyWaterEntity;
import com.dxdcraft.entity.projectile.LightSpearEntity;
import com.dxdcraft.item.DivineDividingItem;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.logic.RageMeter;
import com.dxdcraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DxDCraft.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            DxDLogic.secondTick(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.dxdcraft.logic.Destiny.rollIfNeeded(player);
            DxDLogic.updateFlight(player, DxDLogic.data(player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DxDData d = DxDLogic.data(player);
            d.clearJd();
            d.setRage(0);
            d.setMana(d.getMaxMana() / 2);
            DxDLogic.updateFlight(player, d);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        // Wind affinity: no fall damage.
        if (victim instanceof Player wp && source.is(DamageTypes.FALL)
                && DxDLogic.data(wp).hasElement(com.dxdcraft.data.Element.WIND)) {
            event.setCanceled(true);
            return;
        }
        Entity direct = source.getDirectEntity();

        // Devil players are vulnerable to holy power.
        if (victim instanceof Player p && DxDLogic.data(p).getRace() == Race.DEVIL
                && (direct instanceof LightSpearEntity || direct instanceof HolyWaterEntity)) {
            event.setAmount(event.getAmount() * 1.5F);
        }

        // Melee hits by a player holding a Sacred Gear in the main hand.
        if (source.getEntity() instanceof Player attacker && direct == attacker && source.is(DamageTypes.PLAYER_ATTACK) && !attacker.level().isClientSide) {
            if (attacker.getMainHandItem().is(ModItems.BOOSTED_GEAR.get()) || com.dxdcraft.compat.CuriosCompat.has(attacker, ModItems.BOOSTED_GEAR.get())) {
                DxDData d = DxDLogic.data(attacker);
                int boosts = d.getBoosts();
                if (boosts > 0) {
                    event.setAmount(event.getAmount() * (1.0F + 0.5F * boosts));
                    d.setBoosts(0);
                    d.setBoostTimer(0);
                    attacker.displayClientMessage(Component.literal("EXPLOSION! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                            .append(Component.literal("x" + boosts + " released").withStyle(ChatFormatting.WHITE)), true);
                    attacker.level().playSound(null, victim.blockPosition(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    if (attacker.level() instanceof net.minecraft.server.level.ServerLevel server) {
                        server.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1, victim.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            } else if (attacker.getMainHandItem().is(ModItems.DIVINE_DIVIDING.get()) || com.dxdcraft.compat.CuriosCompat.has(attacker, ModItems.DIVINE_DIVIDING.get())) {
                DivineDividingItem.divide(attacker, victim);
            }
        }

        // ULT gauge: dealing damage charges it, taking damage charges it more.
        float amount = Math.min(event.getAmount(), 20.0F);
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            chargeUlt(attacker, amount);
        }
        if (victim instanceof ServerPlayer hurtPlayer) {
            chargeUlt(hurtPlayer, amount * 2.0F);
            RageMeter.onHurt(hurtPlayer, amount);
        }
    }

    private static void chargeUlt(ServerPlayer player, float amount) {
        DxDData d = DxDLogic.data(player);
        boolean wasReady = d.ultReady();
        d.addUltCharge(amount);
        if (!wasReady && d.ultReady()) {
            player.displayClientMessage(Component.literal("ULTIMATE READY! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("Press [G]").withStyle(ChatFormatting.YELLOW)), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // DxD XP for defeating things: equal to the victim's max health (players are worth 20).
        LivingEntity dead = event.getEntity();

        // Rage triggers: watching allies, pets or innocents die.
        if (!dead.level().isClientSide) {
            if (dead instanceof Player) {
                for (Player near : dead.level().getEntitiesOfClass(Player.class, dead.getBoundingBox().inflate(RageMeter.ALLY_DEATH_RANGE))) {
                    if (near != dead && near instanceof ServerPlayer sp) {
                        RageMeter.add(sp, 40, dead.getName().getString() + " has fallen... your blood boils.");
                    }
                }
            } else if (dead instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner) {
                RageMeter.add(owner, 25, "Your companion was killed!");
            } else if (dead instanceof Villager) {
                for (Player near : dead.level().getEntitiesOfClass(Player.class, dead.getBoundingBox().inflate(RageMeter.VILLAGER_DEATH_RANGE))) {
                    if (near instanceof ServerPlayer sp && event.getSource().getEntity() != sp) RageMeter.add(sp, 10, null);
                }
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer && dead != killer && !dead.level().isClientSide) {
            int xp = dead instanceof Player ? 20 : Math.max(1, Math.round(dead.getMaxHealth()));
            DxDLogic.giveXp(killer, xp);
            com.dxdcraft.logic.Destiny.onKill(killer, dead);
        }

        // An Angel who slays an innocent villager falls from grace.
        if (event.getEntity() instanceof Villager && event.getSource().getEntity() instanceof ServerPlayer player) {
            if (DxDLogic.data(player).getRace() == Race.ANGEL) {
                DxDLogic.setRace(player, Race.FALLEN_ANGEL, Piece.NONE);
                player.sendSystemMessage(Component.literal("You have sinned. Your wings turn black... you are now a Fallen Angel.")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DxDCommand.register(event.getDispatcher());
    }
}
