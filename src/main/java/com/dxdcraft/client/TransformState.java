package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.network.TransformPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.HashMap;
import java.util.Map;

/** Client copy of who is transformed (Balance Breaker / Juggernaut Drive), with timing for the animations. */
@EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
public final class TransformState {
    /** Ticks the armor takes to dissolve after the form ends. */
    public static final int DISSOLVE_TICKS = 12;

    public static final class Entry {
        public final String gear;
        public final int mode;
        public final long startTick;
        public long endTick = -1;

        Entry(String gear, int mode, long startTick) {
            this.gear = gear;
            this.mode = mode;
            this.startTick = startTick;
        }

        public boolean juggernaut() {
            return mode == TransformPayload.MODE_JD || mode == TransformPayload.MODE_JD_COMPLETE;
        }
    }

    private static final Map<Integer, Entry> FORMS = new HashMap<>();

    private TransformState() {}

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    public static void accept(TransformPayload p) {
        if (p.mode() == TransformPayload.MODE_NONE) {
            Entry e = FORMS.get(p.entityId());
            if (e != null && e.endTick < 0) e.endTick = now();
            return;
        }
        FORMS.put(p.entityId(), new Entry(p.gear(), p.mode(), now()));
    }

    /** The entity's form, or null. Entries that finished dissolving are dropped here. */
    public static Entry get(int entityId) {
        Entry e = FORMS.get(entityId);
        if (e != null && e.endTick >= 0 && now() - e.endTick > DISSOLVE_TICKS) {
            FORMS.remove(entityId);
            return null;
        }
        return e;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FORMS.clear();
    }

    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        // Respawn / dimension change gives the local player a new entity id; the server re-sends the state.
        FORMS.remove(event.getOldPlayer().getId());
    }
}
