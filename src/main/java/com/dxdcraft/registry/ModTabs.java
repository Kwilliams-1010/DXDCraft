package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DxDCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dxdcraft"))
            .icon(() -> new ItemStack(ModItems.BOOSTED_GEAR.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.BOOSTED_GEAR.get());
                out.accept(ModItems.DIVINE_DIVIDING.get());
                out.accept(ModItems.TWILIGHT_HEALING.get());
                out.accept(ModItems.SWORD_BIRTH.get());
                out.accept(ModItems.ABSORPTION_LINE.get());
                out.accept(ModItems.ANNIHILATION_MAKER.get());
                out.accept(ModItems.DIMENSION_LOST.get());
                out.accept(ModItems.REGULUS_NEMEA.get());
                out.accept(ModItems.ZENITH_TEMPEST.get());
                out.accept(ModItems.FORBIDDEN_BALOR_VIEW.get());
                out.accept(ModItems.EVIL_PIECE_PAWN.get());
                out.accept(ModItems.EVIL_PIECE_KNIGHT.get());
                out.accept(ModItems.EVIL_PIECE_BISHOP.get());
                out.accept(ModItems.EVIL_PIECE_ROOK.get());
                out.accept(ModItems.EVIL_PIECE_QUEEN.get());
                out.accept(ModItems.HOLY_FEATHER.get());
                out.accept(ModItems.FALLEN_FEATHER.get());
                out.accept(ModItems.PURIFICATION_SCROLL.get());
                out.accept(ModItems.DESTRUCTION_SIGIL.get());
                out.accept(ModItems.LIGHT_SPEAR.get());
                out.accept(ModItems.HOLY_WATER.get());
                out.accept(ModItems.DRAGON_SUMMONING_GEM.get());
                out.accept(ModItems.DRAGON_HEART.get());
                out.accept(ModItems.HEAVEN_GATE_KEY.get());
                out.accept(ModItems.UNDERWORLD_MAGIC_CIRCLE.get());
                out.accept(ModItems.DEMONIC_ESSENCE.get());
                out.accept(ModItems.LIGHT_FRAGMENT.get());
                out.accept(ModItems.DRAGON_SCALE.get());
                out.accept(ModItems.STRAY_DEVIL_SPAWN_EGG.get());
                out.accept(ModItems.ROGUE_FALLEN_ANGEL_SPAWN_EGG.get());
                out.accept(ModItems.HEAVENLY_DRAGON_SPAWN_EGG.get());
            })
            .build());
}
