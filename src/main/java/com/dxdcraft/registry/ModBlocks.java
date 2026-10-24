package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.block.AngelicWardBlock;
import com.dxdcraft.block.AngelicWardBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DxDCraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DxDCraft.MODID);

    // ---- Heaven ores (in floating-island stone) ----
    public static final DeferredBlock<Block> HOLY_SILVER_ORE = BLOCKS.register("holy_silver_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> SERAPHITE_ORE = BLOCKS.register("seraphite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).lightLevel(s -> 5)));

    // ---- Underworld ores (in netherrack) ----
    public static final DeferredBlock<Block> DEMONITE_ORE = BLOCKS.register("demonite_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_GOLD_ORE).strength(3.0F, 3.0F).sound(SoundType.NETHER_ORE)));
    public static final DeferredBlock<Block> HELLFIRE_RUBY_ORE = BLOCKS.register("hellfire_ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_QUARTZ_ORE).strength(4.5F, 3.0F).lightLevel(s -> 7)));

    // ---- Storage blocks ----
    public static final DeferredBlock<Block> HOLY_SILVER_BLOCK = BLOCKS.register("holy_silver_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> DEMONITE_BLOCK = BLOCKS.register("demonite_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> DRAGONITE_BLOCK = BLOCKS.register("dragonite_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)));

    // ---- Defense ----
    public static final DeferredBlock<Block> ANGELIC_WARD = BLOCKS.register("angelic_ward",
            () -> new AngelicWardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEACON).lightLevel(s -> 15).strength(5.0F, 1200.0F).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AngelicWardBlockEntity>> ANGELIC_WARD_BE =
            BLOCK_ENTITIES.register("angelic_ward", () -> BlockEntityType.Builder.of(AngelicWardBlockEntity::new, ANGELIC_WARD.get()).build(null));

    // ---- Block items (registered in ModItems' register) ----
    public static final DeferredItem<BlockItem> HOLY_SILVER_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(HOLY_SILVER_ORE);
    public static final DeferredItem<BlockItem> SERAPHITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SERAPHITE_ORE);
    public static final DeferredItem<BlockItem> DEMONITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(DEMONITE_ORE);
    public static final DeferredItem<BlockItem> HELLFIRE_RUBY_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(HELLFIRE_RUBY_ORE);
    public static final DeferredItem<BlockItem> HOLY_SILVER_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem(HOLY_SILVER_BLOCK);
    public static final DeferredItem<BlockItem> DEMONITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem(DEMONITE_BLOCK);
    public static final DeferredItem<BlockItem> DRAGONITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("dragonite_block", DRAGONITE_BLOCK,
            new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> ANGELIC_WARD_ITEM = ModItems.ITEMS.registerSimpleBlockItem("angelic_ward", ANGELIC_WARD,
            new Item.Properties().rarity(Rarity.EPIC));

    private ModBlocks() {}
}
