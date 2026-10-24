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

    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        return loaded;
    }

    /** Every non-empty stack the entity has equipped in Curios slots (empty list without Curios). */
    public static List<ItemStack> equipped(LivingEntity entity) {
        if (!isLoaded() || failed) return List.of();
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
