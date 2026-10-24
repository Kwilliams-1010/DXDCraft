package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.item.BlessedHoeItem;
import com.dxdcraft.item.BossSigilItem;
import com.dxdcraft.item.ExcavatorItem;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Realm materials, the five armor/tool sets, special tools and boss trophies. */
public final class ModGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, DxDCraft.MODID);
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ---------------------------------------------------------------- materials
    public static final DeferredItem<Item> RAW_HOLY_SILVER = ITEMS.register("raw_holy_silver", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HOLY_SILVER_INGOT = ITEMS.register("holy_silver_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SERAPHITE = ITEMS.register("seraphite", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RAW_DEMONITE = ITEMS.register("raw_demonite", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEMONITE_INGOT = ITEMS.register("demonite_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HELLFIRE_RUBY = ITEMS.register("hellfire_ruby", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final DeferredItem<Item> DRAGONITE_INGOT = ITEMS.register("dragonite_ingot", () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // ---------------------------------------------------------------- boss trophies & summons
    public static final DeferredItem<Item> SERAPH_HALO = ITEMS.register("seraph_halo", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> OVERLORD_CROWN = ITEMS.register("overlord_crown", () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> SERAPH_SIGIL = ITEMS.register("seraph_sigil",
            () -> new BossSigilItem(BossSigilItem.Boss.SERAPH, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFERNAL_SIGIL = ITEMS.register("infernal_sigil",
            () -> new BossSigilItem(BossSigilItem.Boss.OVERLORD, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ---------------------------------------------------------------- the five sets
    public enum GearSet {
        //          id               tierTag (what it CAN'T mine)                 uses  speed dmg  ench | boots legs chest helm tough kb   dur   repair
        HOLY_SILVER("holy_silver", BlockTags.INCORRECT_FOR_IRON_TOOL, 400, 7.0F, 2.5F, 18, 2, 5, 6, 2, 0.0F, 0.0F, 18, SoundEvents.ARMOR_EQUIP_IRON, () -> HOLY_SILVER_INGOT),
        DEMONITE("demonite", BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 7.0F, 3.0F, 14, 2, 5, 7, 2, 1.0F, 0.0F, 20, SoundEvents.ARMOR_EQUIP_IRON, () -> DEMONITE_INGOT),
        SERAPHITE("seraphite", BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5F, 3.5F, 20, 3, 6, 8, 3, 2.5F, 0.0F, 35, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> ModGear.SERAPHITE),
        HELLFIRE_RUBY("hellfire_ruby", BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1700, 8.0F, 4.0F, 12, 3, 6, 8, 3, 2.5F, 0.05F, 35, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> ModGear.HELLFIRE_RUBY),
        DRAGONITE("dragonite", BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 10.0F, 5.0F, 18, 4, 7, 9, 4, 4.0F, 0.15F, 45, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> DRAGONITE_INGOT);

        public final String id;
        public final Tier tier;
        final int boots, legs, chest, helm, enchant, durability;
        final float toughness, knockback;
        final Holder<SoundEvent> sound;
        final Supplier<DeferredItem<Item>> repair;

        GearSet(String id, TagKey<Block> incorrect, int uses, float speed, float damage, int enchant,
                int boots, int legs, int chest, int helm, float toughness, float knockback, int durability,
                Holder<SoundEvent> sound, Supplier<DeferredItem<Item>> repair) {
            this.id = id;
            this.tier = new SimpleTier(incorrect, uses, speed, damage, enchant, () -> Ingredient.of(repair.get().get()));
            this.boots = boots; this.legs = legs; this.chest = chest; this.helm = helm;
            this.enchant = enchant; this.durability = durability;
            this.toughness = toughness; this.knockback = knockback;
            this.sound = sound;
            this.repair = repair;
        }

        public boolean fireResistant() {
            return this == HELLFIRE_RUBY || this == DRAGONITE || this == DEMONITE;
        }
    }

    public static final Map<GearSet, DeferredHolder<ArmorMaterial, ArmorMaterial>> MATERIALS = new EnumMap<>(GearSet.class);
    public static final Map<GearSet, Map<ArmorItem.Type, DeferredItem<ArmorItem>>> ARMOR = new EnumMap<>(GearSet.class);
    public static final Map<GearSet, Map<String, DeferredItem<? extends Item>>> TOOLS = new EnumMap<>(GearSet.class);
    public static final List<DeferredItem<? extends Item>> TAB_ITEMS = new ArrayList<>();

    static {
        for (GearSet set : GearSet.values()) {
            MATERIALS.put(set, ARMOR_MATERIALS.register(set.id, () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                        m.put(ArmorItem.Type.BOOTS, set.boots);
                        m.put(ArmorItem.Type.LEGGINGS, set.legs);
                        m.put(ArmorItem.Type.CHESTPLATE, set.chest);
                        m.put(ArmorItem.Type.HELMET, set.helm);
                        m.put(ArmorItem.Type.BODY, set.chest);
                    }),
                    set.enchant, set.sound, () -> Ingredient.of(set.repair.get().get()),
                    List.of(new ArmorMaterial.Layer(DxDCraft.id(set.id))), set.toughness, set.knockback)));
        }
        for (GearSet set : GearSet.values()) {
            Map<ArmorItem.Type, DeferredItem<ArmorItem>> armor = new LinkedHashMap<>();
            for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                String name = set.id + "_" + type.getName();
                DeferredItem<ArmorItem> item = ITEMS.register(name, () -> new ArmorItem(MATERIALS.get(set), type, props(set).durability(type.getDurability(set.durability))));
                armor.put(type, item);
                TAB_ITEMS.add(item);
            }
            ARMOR.put(set, armor);

            Map<String, DeferredItem<? extends Item>> tools = new LinkedHashMap<>();
            tools.put("sword", ITEMS.register(set.id + "_sword", () -> new SwordItem(set.tier, props(set).attributes(SwordItem.createAttributes(set.tier, 3, -2.4F)))));
            tools.put("pickaxe", ITEMS.register(set.id + "_pickaxe", () -> new PickaxeItem(set.tier, props(set).attributes(DiggerItem.createAttributes(set.tier, 1.0F, -2.8F)))));
            tools.put("axe", ITEMS.register(set.id + "_axe", () -> new AxeItem(set.tier, props(set).attributes(DiggerItem.createAttributes(set.tier, 5.5F, -3.0F)))));
            tools.put("shovel", ITEMS.register(set.id + "_shovel", () -> new ShovelItem(set.tier, props(set).attributes(DiggerItem.createAttributes(set.tier, 1.5F, -3.0F)))));
            tools.put("hoe", ITEMS.register(set.id + "_hoe", () -> new HoeItem(set.tier, props(set).attributes(DiggerItem.createAttributes(set.tier, -2.0F, 0.0F)))));
            TOOLS.put(set, tools);
            TAB_ITEMS.addAll(tools.values());
        }
    }

    private static Item.Properties props(GearSet set) {
        Item.Properties p = new Item.Properties();
        if (set.fireResistant()) p = p.fireResistant();
        if (set == GearSet.DRAGONITE) p = p.rarity(Rarity.EPIC);
        else if (set == GearSet.SERAPHITE || set == GearSet.HELLFIRE_RUBY) p = p.rarity(Rarity.RARE);
        return p;
    }

    // ---------------------------------------------------------------- special tools
    /** Farming: grows everything in a 5x5 area. */
    public static final DeferredItem<Item> BLESSED_HOE = ITEMS.register("blessed_hoe",
            () -> new BlessedHoeItem(GearSet.HOLY_SILVER.tier, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(DiggerItem.createAttributes(GearSet.HOLY_SILVER.tier, -2.0F, 0.0F))));
    /** Mining: 3x3 netherite-tier pickaxe made with the Overlord's Crown. */
    public static final DeferredItem<Item> OVERLORDS_EXCAVATOR = ITEMS.register("overlords_excavator",
            () -> new ExcavatorItem(GearSet.DRAGONITE.tier, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(DiggerItem.createAttributes(GearSet.DRAGONITE.tier, 2.0F, -2.9F))));

    public static ArmorItem armor(GearSet set, ArmorItem.Type type) {
        return ARMOR.get(set).get(type).get();
    }

    public static Item tool(GearSet set, String kind) {
        return TOOLS.get(set).get(kind).get();
    }

    private ModGear() {}
}
