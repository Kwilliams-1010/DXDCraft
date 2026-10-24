package com.dxdcraft.logic;

import com.dxdcraft.DxDCraft;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants DxD Craft achievements whose criteria are code-driven ("minecraft:impossible" criteria
 * named "done"), e.g. awakening a gear or reaching Balance Breaker.
 */
public final class DxDAdvancements {
    private DxDAdvancements() {}

    public static void grant(ServerPlayer p, String id) {
        if (p.server == null || p.connection == null) return;
        AdvancementHolder adv = p.server.getAdvancements().get(DxDCraft.id(id));
        if (adv == null) return;
        AdvancementProgress progress = p.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            p.getAdvancements().award(adv, criterion);
        }
    }

    public static boolean has(ServerPlayer p, String id) {
        AdvancementHolder adv = p.server.getAdvancements().get(DxDCraft.id(id));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }
}
