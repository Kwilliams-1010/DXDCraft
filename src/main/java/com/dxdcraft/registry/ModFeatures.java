package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.world.DxDPois;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, DxDCraft.MODID);

    private static DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> poi(String name, Supplier<Feature<NoneFeatureConfiguration>> f) {
        return FEATURES.register(name, f);
    }

    // Heaven
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GATE_OF_HEAVEN = poi("gate_of_heaven", DxDPois.GateOfHeaven::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SERAPH_CATHEDRAL = poi("seraph_cathedral", DxDPois.SeraphCathedral::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BRAVE_SAINT_SHRINE = poi("brave_saint_shrine", DxDPois.BraveSaintShrine::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GARDEN_OF_EDEN = poi("garden_of_eden", DxDPois.GardenOfEden::new);
    // Underworld
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DEVIL_NOBLE_CASTLE = poi("devil_noble_castle", DxDPois.DevilNobleCastle::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RATING_GAME_ARENA = poi("rating_game_arena", DxDPois.RatingGameArena::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STRAY_DEVIL_LAIR = poi("stray_devil_lair", DxDPois.StrayDevilLair::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GRIGORI_OUTPOST = poi("grigori_outpost", DxDPois.GrigoriOutpost::new);
}
