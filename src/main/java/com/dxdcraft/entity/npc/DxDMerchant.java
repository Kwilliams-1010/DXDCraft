package com.dxdcraft.entity.npc;

import com.dxdcraft.logic.DxDAdvancements;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModGear;
import com.dxdcraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * Trading NPCs that help progression:
 *  - Wandering Exorcist (Overworld, emeralds): starter supplies and the realm travel items
 *  - Angel Merchant (Heaven, Holy Silver): holy gear, the Seraph Sigil, rare Sacred Gears
 *  - Devil Merchant (Underworld, Demonite): Evil Pieces, the Infernal Sigil, rare Sacred Gears
 * Merchants placed by world generation stay forever; ones that wander in leave after a day.
 */
public class DxDMerchant extends AbstractVillager {
    public enum Kind { EXORCIST, ANGEL, DEVIL }

    private final Kind kind;
    private int despawnDelay = 0; // 0 = never (worldgen merchants)

    public DxDMerchant(EntityType<? extends DxDMerchant> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
    }

    public static DxDMerchant exorcist(EntityType<? extends DxDMerchant> type, Level level) { return new DxDMerchant(type, level, Kind.EXORCIST); }
    public static DxDMerchant angel(EntityType<? extends DxDMerchant> type, Level level) { return new DxDMerchant(type, level, Kind.ANGEL); }
    public static DxDMerchant devil(EntityType<? extends DxDMerchant> type, Level level) { return new DxDMerchant(type, level, Kind.DEVIL); }

    public Kind kind() { return kind; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    public void setDespawnDelay(int ticks) { despawnDelay = ticks; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.5, 0.5));
        goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.35));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    // ---------------------------------------------------------------- trading

    private static MerchantOffer offer(ItemLike cost, int count, ItemStack result, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, count), result, maxUses, 5, 0.05F);
    }

    private static MerchantOffer offer(ItemLike cost, int count, ItemLike costB, int countB, ItemStack result, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, count), java.util.Optional.of(new ItemCost(costB, countB)), result, maxUses, 10, 0.05F);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        switch (kind) {
            case EXORCIST -> {
                offers.add(offer(Items.EMERALD, 4, new ItemStack(ModItems.HOLY_WATER.get(), 4), 16));
                offers.add(offer(Items.EMERALD, 8, new ItemStack(ModItems.LIGHT_FRAGMENT.get(), 3), 12));
                offers.add(offer(Items.EMERALD, 8, new ItemStack(ModItems.DEMONIC_ESSENCE.get(), 3), 12));
                offers.add(offer(Items.EMERALD, 12, new ItemStack(ModItems.PURIFICATION_SCROLL.get()), 4));
                offers.add(offer(Items.EMERALD, 20, new ItemStack(ModItems.HEAVEN_GATE_KEY.get()), 2));
                offers.add(offer(Items.EMERALD, 20, new ItemStack(ModItems.UNDERWORLD_MAGIC_CIRCLE.get()), 2));
                offers.add(offer(ModItems.LIGHT_FRAGMENT.get(), 4, new ItemStack(Items.EMERALD, 6), 16));
                offers.add(offer(ModItems.DEMONIC_ESSENCE.get(), 4, new ItemStack(Items.EMERALD, 6), 16));
            }
            case ANGEL -> {
                offers.add(offer(ModItems.LIGHT_FRAGMENT.get(), 6, new ItemStack(ModGear.HOLY_SILVER_INGOT.get(), 2), 16));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 3, new ItemStack(ModItems.HOLY_WATER.get(), 6), 16));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 4, new ItemStack(Items.GOLDEN_CARROT, 12), 16));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 10, new ItemStack(ModItems.HOLY_FEATHER.get()), 3));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 12, new ItemStack(ModGear.SERAPHITE.get()), 8));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 16, Items.DIAMOND, 2, new ItemStack(ModGear.SERAPH_SIGIL.get()), 2));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 24, ModGear.SERAPHITE.get(), 2, new ItemStack(ModGear.BLESSED_HOE.get()), 1));
                offers.add(offer(ModGear.HOLY_SILVER_INGOT.get(), 40, ModGear.SERAPHITE.get(), 4, new ItemStack(ModItems.TWILIGHT_HEALING.get()), 1));
            }
            case DEVIL -> {
                offers.add(offer(ModItems.DEMONIC_ESSENCE.get(), 6, new ItemStack(ModGear.DEMONITE_INGOT.get(), 2), 16));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 6, new ItemStack(ModItems.EVIL_PIECE_PAWN.get()), 8));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 12, ModItems.DEMONIC_ESSENCE.get(), 8, new ItemStack(ModItems.EVIL_PIECE_KNIGHT.get()), 2));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 12, ModItems.DEMONIC_ESSENCE.get(), 8, new ItemStack(ModItems.EVIL_PIECE_BISHOP.get()), 2));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 12, ModItems.DEMONIC_ESSENCE.get(), 8, new ItemStack(ModItems.EVIL_PIECE_ROOK.get()), 2));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 12, new ItemStack(ModGear.HELLFIRE_RUBY.get()), 8));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 16, Items.DIAMOND, 2, new ItemStack(ModGear.INFERNAL_SIGIL.get()), 2));
                offers.add(offer(ModGear.DEMONITE_INGOT.get(), 40, ModGear.HELLFIRE_RUBY.get(), 4, new ItemStack(ModItems.SWORD_BIRTH.get()), 1));
            }
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
        }
        if (getTradingPlayer() instanceof ServerPlayer sp) {
            DxDAdvancements.grant(sp, switch (kind) {
                case EXORCIST -> "trade_exorcist";
                case ANGEL -> "trade_angel";
                case DEVIL -> "trade_devil";
            });
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby()) return super.mobInteract(player, hand);
        if (!level().isClientSide && !getOffers().isEmpty()) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && despawnDelay > 0 && !isTrading() && --despawnDelay == 0) discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DespawnDelay", despawnDelay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        despawnDelay = tag.getInt("DespawnDelay");
    }

    @Override
    protected SoundEvent getAmbientSound() { return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WANDERING_TRADER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WANDERING_TRADER_DEATH; }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean yes) { return yes ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO; }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.WANDERING_TRADER_YES; }

    /** Used by world generation and the near-player spawner. */
    public static EntityType<? extends DxDMerchant> typeFor(Kind kind) {
        return switch (kind) {
            case EXORCIST -> ModEntities.WANDERING_EXORCIST.get();
            case ANGEL -> ModEntities.ANGEL_MERCHANT.get();
            case DEVIL -> ModEntities.DEVIL_MERCHANT.get();
        };
    }

    @SuppressWarnings("unused")
    private static boolean isPathfinder(PathfinderMob m) { return true; }
}
