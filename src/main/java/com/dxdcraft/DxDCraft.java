package com.dxdcraft;

import com.dxdcraft.registry.ModAttachments;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModFeatures;
import com.dxdcraft.registry.ModItems;
import com.dxdcraft.registry.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(DxDCraft.MODID)
public class DxDCraft {
    public static final String MODID = "dxdcraft";

    public DxDCraft(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, DxDConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
