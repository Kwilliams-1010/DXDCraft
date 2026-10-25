package com.dxdcraft.event;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.entity.HeavenlyDragon;
import com.dxdcraft.entity.RogueFallenAngel;
import com.dxdcraft.entity.StrayDevil;
import com.dxdcraft.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.dxdcraft.network.AbilityPayload;

@EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
    @SubscribeEvent
    public static void onPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(AbilityPayload.TYPE, AbilityPayload.STREAM_CODEC, AbilityPayload::handle);
        registrar.playToClient(com.dxdcraft.network.CutscenePayload.TYPE, com.dxdcraft.network.CutscenePayload.STREAM_CODEC,
                com.dxdcraft.network.CutscenePayload::handle);
        registrar.playToClient(com.dxdcraft.network.FxPayload.TYPE, com.dxdcraft.network.FxPayload.STREAM_CODEC,
                com.dxdcraft.network.FxPayload::handle);
        registrar.playToClient(com.dxdcraft.network.TransformPayload.TYPE, com.dxdcraft.network.TransformPayload.STREAM_CODEC,
                com.dxdcraft.network.TransformPayload::handle);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.STRAY_DEVIL.get(), StrayDevil.createAttributes().build());
        event.put(ModEntities.ROGUE_FALLEN_ANGEL.get(), RogueFallenAngel.createAttributes().build());
        event.put(ModEntities.HEAVENLY_DRAGON.get(), HeavenlyDragon.createAttributes().build());
        event.put(ModEntities.SERAPH_GUARDIAN.get(), com.dxdcraft.entity.SeraphGuardian.createAttributes().build());
        event.put(ModEntities.UNDERWORLD_OVERLORD.get(), com.dxdcraft.entity.UnderworldOverlord.createAttributes().build());
        event.put(ModEntities.WANDERING_EXORCIST.get(), com.dxdcraft.entity.npc.DxDMerchant.createAttributes().build());
        event.put(ModEntities.ANGEL_MERCHANT.get(), com.dxdcraft.entity.npc.DxDMerchant.createAttributes().build());
        event.put(ModEntities.DEVIL_MERCHANT.get(), com.dxdcraft.entity.npc.DxDMerchant.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.STRAY_DEVIL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ROGUE_FALLEN_ANGEL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
