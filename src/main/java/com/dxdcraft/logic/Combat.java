package com.dxdcraft.logic;

import com.dxdcraft.DxDConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Multiplayer rules: who may hurt whom, and who hears announcements. */
public final class Combat {
    private Combat() {}

    /**
     * Whether damage from {@code attacker} (or anything it owns) may hit {@code target}.
     * Mobs can always hit players. Players only hit other players when PvP allows it and they
     * aren't teammates; they never hit their own pets.
     */
    public static boolean canHarm(Entity attacker, Entity target) {
        if (attacker == null || target == null) return true;
        if (attacker == target) return false;
        if (!(attacker instanceof Player pa)) return true;
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(pa)) return false;
        if (!(target instanceof Player pt)) return true;
        if (!DxDConfig.get(DxDConfig.ABILITIES_RESPECT_PVP::get, true)) return true;
        if (pa.isAlliedTo(pt)) return false;
        MinecraftServer server = pa.getServer();
        return server == null || server.isPvpAllowed();
    }

    /** Sends a message to everyone (or everyone nearby, per config). */
    public static void announce(ServerPlayer origin, Component message) {
        if (!DxDConfig.get(DxDConfig.ANNOUNCEMENTS::get, true)) {
            origin.sendSystemMessage(message);
            return;
        }
        int radius = DxDConfig.get(DxDConfig.ANNOUNCE_RADIUS::get, 0);
        if (radius <= 0) {
            origin.server.getPlayerList().broadcastSystemMessage(message, false);
            return;
        }
        ServerLevel level = origin.serverLevel();
        Vec3 at = origin.position();
        double r2 = (double) radius * radius;
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(at) <= r2) p.sendSystemMessage(message);
        }
    }
}
