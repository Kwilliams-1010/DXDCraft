package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.entity.HeavenlyDragon;
import com.dxdcraft.entity.SeraphGuardian;
import com.dxdcraft.entity.UnderworldOverlord;
import com.dxdcraft.entity.npc.DxDMerchant;
import com.dxdcraft.entity.RogueFallenAngel;
import com.dxdcraft.entity.StrayDevil;
import com.dxdcraft.entity.projectile.DestructionOrbEntity;
import com.dxdcraft.entity.projectile.DragonShotEntity;
import com.dxdcraft.entity.projectile.HolyWaterEntity;
import com.dxdcraft.entity.projectile.LightSpearEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DxDCraft.MODID);

    // ---- Projectiles ----
    public static final DeferredHolder<EntityType<?>, EntityType<LightSpearEntity>> LIGHT_SPEAR =
            ENTITY_TYPES.register("light_spear", () -> EntityType.Builder.<LightSpearEntity>of(LightSpearEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(5).build("light_spear"));

    public static final DeferredHolder<EntityType<?>, EntityType<DestructionOrbEntity>> DESTRUCTION_ORB =
            ENTITY_TYPES.register("destruction_orb", () -> EntityType.Builder.<DestructionOrbEntity>of(DestructionOrbEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(5).build("destruction_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<DragonShotEntity>> DRAGON_SHOT =
            ENTITY_TYPES.register("dragon_shot", () -> EntityType.Builder.<DragonShotEntity>of(DragonShotEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(5).fireImmune().build("dragon_shot"));

    public static final DeferredHolder<EntityType<?>, EntityType<HolyWaterEntity>> HOLY_WATER =
            ENTITY_TYPES.register("holy_water", () -> EntityType.Builder.<HolyWaterEntity>of(HolyWaterEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("holy_water"));

    // ---- Mobs ----
    public static final DeferredHolder<EntityType<?>, EntityType<StrayDevil>> STRAY_DEVIL =
            ENTITY_TYPES.register("stray_devil", () -> EntityType.Builder.of(StrayDevil::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("stray_devil"));

    public static final DeferredHolder<EntityType<?>, EntityType<RogueFallenAngel>> ROGUE_FALLEN_ANGEL =
            ENTITY_TYPES.register("rogue_fallen_angel", () -> EntityType.Builder.of(RogueFallenAngel::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("rogue_fallen_angel"));

    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyDragon>> HEAVENLY_DRAGON =
            ENTITY_TYPES.register("heavenly_dragon", () -> EntityType.Builder.of(HeavenlyDragon::new, MobCategory.MONSTER)
                    .sized(3.0F, 2.5F).fireImmune().clientTrackingRange(16).build("heavenly_dragon"));

    // ---- Bosses ----
    public static final DeferredHolder<EntityType<?>, EntityType<SeraphGuardian>> SERAPH_GUARDIAN =
            ENTITY_TYPES.register("seraph_guardian", () -> EntityType.Builder.of(SeraphGuardian::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.2F).fireImmune().clientTrackingRange(16).build("seraph_guardian"));
    public static final DeferredHolder<EntityType<?>, EntityType<UnderworldOverlord>> UNDERWORLD_OVERLORD =
            ENTITY_TYPES.register("underworld_overlord", () -> EntityType.Builder.of(UnderworldOverlord::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.6F).fireImmune().clientTrackingRange(16).build("underworld_overlord"));

    // ---- NPCs ----
    public static final DeferredHolder<EntityType<?>, EntityType<DxDMerchant>> WANDERING_EXORCIST =
            ENTITY_TYPES.register("wandering_exorcist", () -> EntityType.Builder.<DxDMerchant>of(DxDMerchant::exorcist, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("wandering_exorcist"));
    public static final DeferredHolder<EntityType<?>, EntityType<DxDMerchant>> ANGEL_MERCHANT =
            ENTITY_TYPES.register("angel_merchant", () -> EntityType.Builder.<DxDMerchant>of(DxDMerchant::angel, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("angel_merchant"));
    public static final DeferredHolder<EntityType<?>, EntityType<DxDMerchant>> DEVIL_MERCHANT =
            ENTITY_TYPES.register("devil_merchant", () -> EntityType.Builder.<DxDMerchant>of(DxDMerchant::devil, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10).build("devil_merchant"));
}
