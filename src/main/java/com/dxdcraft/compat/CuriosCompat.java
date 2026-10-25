package com.dxdcraft.compat;

import com.dxdcraft.item.SacredGearItem;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Optional Curios support, done by reflection so DxD Craft builds and runs without Curios installed.
 *
 * Which slot each Sacred Gear goes in is set by data (item tags in data/curios/tags/item/ and the
 * slot assignment in data/dxdcraft/curios/entities/). This class lets the game logic "see" gears
 * that are worn in those slots.
 */
public final class CuriosCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Boolean loaded;
    private static boolean failed;
    private static Method getCuriosInventory;
    private static Method getEquippedCurios;

    private CuriosCompat() {}

    /** Which Curios slot each Sacred Gear goes in (mirrors data/curios/tags/item/*.json). */
    public static final java.util.Map<net.neoforged.neoforge.registries.DeferredItem<Item>, String> GEAR_SLOTS = java.util.Map.of(
            com.dxdcraft.registry.ModItems.BOOSTED_GEAR, "hands",
            com.dxdcraft.registry.ModItems.DIVINE_DIVIDING, "back",
            com.dxdcraft.registry.ModItems.SWORD_BIRTH, "belt",
            com.dxdcraft.registry.ModItems.ABSORPTION_LINE, "bracelet",
            com.dxdcraft.registry.ModItems.FORBIDDEN_BALOR_VIEW, "charm",
            com.dxdcraft.registry.ModItems.DIMENSION_LOST, "charm",
            com.dxdcraft.registry.ModItems.ZENITH_TEMPEST, "charm",
            com.dxdcraft.registry.ModItems.ANNIHILATION_MAKER, "charm",
            com.dxdcraft.registry.ModItems.REGULUS_NEMEA, "necklace",
            com.dxdcraft.registry.ModItems.TWILIGHT_HEALING, "ring");

    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        return loaded;
    }

    /** Every non-empty stack the entity has equipped in Curios slots (empty list without Curios). */
    private static final java.util.Map<java.util.UUID, Cached> CACHE = new java.util.HashMap<>();
    private record Cached(long time, List<ItemStack> stacks) {}

    public static List<ItemStack> equipped(LivingEntity entity) {
        if (!isLoaded() || failed) return List.of();
        if (entity.level().isClientSide) return lookup(entity);
        long now = entity.level().getGameTime();
        Cached c = CACHE.get(entity.getUUID());
        if (c != null && c.time == now) return c.stacks;
        List<ItemStack> stacks = lookup(entity);
        if (CACHE.size() > 512) CACHE.clear(); // stale entries from logged-out players
        CACHE.put(entity.getUUID(), new Cached(now, stacks));
        return stacks;
    }

    private static List<ItemStack> lookup(LivingEntity entity) {
        try {
            if (getCuriosInventory == null) {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                getEquippedCurios = handler.getMethod("getEquippedCurios");
            }
            Optional<?> inv = (Optional<?>) getCuriosInventory.invoke(null, entity);
            if (inv.isEmpty()) return List.of();
            IItemHandler items = (IItemHandler) getEquippedCurios.invoke(inv.get());
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack s = items.getStackInSlot(i);
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        } catch (Throwable t) {
            failed = true;
            LOGGER.warn("DxD Craft: Curios is installed but its API could not be reached; worn Sacred Gears will be ignored.", t);
            return List.of();
        }
    }

    public static boolean has(LivingEntity entity, Item item) {
        for (ItemStack s : equipped(entity)) if (s.is(item)) return true;
        return false;
    }

    /** First Sacred Gear worn in a Curios slot, or null. */
    public static SacredGearItem wornGear(LivingEntity entity) {
        for (ItemStack s : equipped(entity)) if (s.getItem() instanceof SacredGearItem g) return g;
        return null;
    }
}
