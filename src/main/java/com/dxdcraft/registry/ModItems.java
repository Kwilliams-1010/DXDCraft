package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.item.AbsorptionLineItem;
import com.dxdcraft.item.AnnihilationMakerItem;
import com.dxdcraft.item.BoostedGearItem;
import com.dxdcraft.item.DimensionLostItem;
import com.dxdcraft.item.ForbiddenBalorViewItem;
import com.dxdcraft.item.RegulusNemeaItem;
import com.dxdcraft.item.SwordBirthItem;
import com.dxdcraft.item.ZenithTempestItem;
import com.dxdcraft.item.DestructionSigilItem;
import com.dxdcraft.item.DivineDividingItem;
import com.dxdcraft.item.DragonHeartItem;
import com.dxdcraft.item.DragonSummoningGemItem;
import com.dxdcraft.item.EvilPieceItem;
import com.dxdcraft.item.HolyWaterItem;
import com.dxdcraft.item.LightSpearItem;
import com.dxdcraft.item.RaceChangeItem;
import com.dxdcraft.item.RealmTravelItem;
import com.dxdcraft.world.DimensionTravel;
import com.dxdcraft.item.TwilightHealingItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DxDCraft.MODID);

    // ---- Materials ----
    public static final DeferredItem<Item> DEMONIC_ESSENCE = ITEMS.register("demonic_essence", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIGHT_FRAGMENT = ITEMS.register("light_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGON_SCALE = ITEMS.register("dragon_scale", () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    // ---- Evil Pieces ----
    public static final DeferredItem<Item> EVIL_PIECE_PAWN = ITEMS.register("evil_piece_pawn", () -> new EvilPieceItem(Piece.PAWN, new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> EVIL_PIECE_KNIGHT = ITEMS.register("evil_piece_knight", () -> new EvilPieceItem(Piece.KNIGHT, new Item.Properties().stacksTo(2).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EVIL_PIECE_BISHOP = ITEMS.register("evil_piece_bishop", () -> new EvilPieceItem(Piece.BISHOP, new Item.Properties().stacksTo(2).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EVIL_PIECE_ROOK = ITEMS.register("evil_piece_rook", () -> new EvilPieceItem(Piece.ROOK, new Item.Properties().stacksTo(2).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EVIL_PIECE_QUEEN = ITEMS.register("evil_piece_queen", () -> new EvilPieceItem(Piece.QUEEN, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ---- Race items ----
    public static final DeferredItem<Item> HOLY_FEATHER = ITEMS.register("holy_feather", () -> new RaceChangeItem(Race.ANGEL, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> FALLEN_FEATHER = ITEMS.register("fallen_feather", () -> new RaceChangeItem(Race.FALLEN_ANGEL, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> PURIFICATION_SCROLL = ITEMS.register("purification_scroll", () -> new RaceChangeItem(Race.HUMAN, new Item.Properties().stacksTo(16)));

    // ---- Magic ----
    public static final DeferredItem<Item> HOLY_WATER = ITEMS.register("holy_water", () -> new HolyWaterItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DESTRUCTION_SIGIL = ITEMS.register("destruction_sigil", () -> new DestructionSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> LIGHT_SPEAR = ITEMS.register("light_spear", () -> new LightSpearItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ---- Sacred Gears ----
    public static final DeferredItem<Item> BOOSTED_GEAR = ITEMS.register("boosted_gear", () -> new BoostedGearItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> DIVINE_DIVIDING = ITEMS.register("divine_dividing", () -> new DivineDividingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> TWILIGHT_HEALING = ITEMS.register("twilight_healing", () -> new TwilightHealingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SWORD_BIRTH = ITEMS.register("sword_birth", () -> new SwordBirthItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> ABSORPTION_LINE = ITEMS.register("absorption_line", () -> new AbsorptionLineItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> ANNIHILATION_MAKER = ITEMS.register("annihilation_maker", () -> new AnnihilationMakerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> DIMENSION_LOST = ITEMS.register("dimension_lost", () -> new DimensionLostItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> REGULUS_NEMEA = ITEMS.register("regulus_nemea", () -> new RegulusNemeaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> ZENITH_TEMPEST = ITEMS.register("zenith_tempest", () -> new ZenithTempestItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> FORBIDDEN_BALOR_VIEW = ITEMS.register("forbidden_balor_view", () -> new ForbiddenBalorViewItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ---- Boss ----
    public static final DeferredItem<Item> DRAGON_SUMMONING_GEM = ITEMS.register("dragon_summoning_gem", () -> new DragonSummoningGemItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> DRAGON_HEART = ITEMS.register("dragon_heart", () -> new DragonHeartItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC).fireResistant()));

    // ---- Realm travel ----
    public static final DeferredItem<Item> HEAVEN_GATE_KEY = ITEMS.register("heaven_gate_key",
            () -> new RealmTravelItem(DimensionTravel.HEAVEN, "Heaven", new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> UNDERWORLD_MAGIC_CIRCLE = ITEMS.register("underworld_magic_circle",
            () -> new RealmTravelItem(DimensionTravel.UNDERWORLD, "the Underworld", new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---- Projectile visuals (not in creative tab) ----
    public static final DeferredItem<Item> DESTRUCTION_ORB = ITEMS.register("destruction_orb", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGON_SHOT = ITEMS.register("dragon_shot", () -> new Item(new Item.Properties()));

    // ---- Spawn eggs ----
    public static final DeferredItem<Item> STRAY_DEVIL_SPAWN_EGG = ITEMS.register("stray_devil_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STRAY_DEVIL, 0x2B0F2E, 0xC4122F, new Item.Properties()));
    public static final DeferredItem<Item> ROGUE_FALLEN_ANGEL_SPAWN_EGG = ITEMS.register("rogue_fallen_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ROGUE_FALLEN_ANGEL, 0x15131A, 0xE8C547, new Item.Properties()));
    public static final DeferredItem<Item> HEAVENLY_DRAGON_SPAWN_EGG = ITEMS.register("heavenly_dragon_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HEAVENLY_DRAGON, 0x8A4B1C, 0xF2B33D, new Item.Properties()));
}
