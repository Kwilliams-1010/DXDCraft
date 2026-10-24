package com.dxdcraft.world;

import com.dxdcraft.entity.npc.DxDMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Like the vanilla wandering trader: occasionally a merchant walks up near a player.
 * Overworld -> Wandering Exorcist, Heaven -> Angel Merchant, Underworld -> Devil Merchant.
 * Visiting merchants leave after one in-game day.
 */
public final class NpcSpawner {
    private static final int STAY_TICKS = 24000;

    private NpcSpawner() {}

    public static DxDMerchant.Kind kindFor(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) return DxDMerchant.Kind.EXORCIST;
        if (level.dimension() == DimensionTravel.HEAVEN) return DxDMerchant.Kind.ANGEL;
        if (level.dimension() == DimensionTravel.UNDERWORLD) return DxDMerchant.Kind.DEVIL;
        return null;
    }

    /** @return the spawned merchant, or null if there was no suitable spot or one is already around. */
    public static DxDMerchant trySpawnNear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        DxDMerchant.Kind kind = kindFor(level);
        if (kind == null) return null;
        if (!level.getEntitiesOfClass(DxDMerchant.class, p.getBoundingBox().inflate(64), m -> m.kind() == kind).isEmpty()) return null;

        for (int attempt = 0; attempt < 10; attempt++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            double r = 8 + p.getRandom().nextDouble() * 12;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * r);
            int z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
            if (!level.hasChunk(x >> 4, z >> 4)) continue; // only already-loaded ground near the player
            BlockPos at = kind == DxDMerchant.Kind.DEVIL ? caveSpot(level, x, p.getBlockY(), z) : surfaceSpot(level, x, z);
            if (at == null) continue;
            DxDMerchant npc = DxDMerchant.typeFor(kind).create(level);
            if (npc == null) return null;
            npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360F, 0F);
            npc.setDespawnDelay(STAY_TICKS);
            level.addFreshEntity(npc);
            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            return npc;
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos at) {
        BlockState below = level.getBlockState(at.below());
        return level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()
                && below.isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP) && below.getFluidState().isEmpty();
    }

    private static BlockPos surfaceSpot(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (y <= level.getMinBuildHeight() + 2) return null;
        BlockPos at = new BlockPos(x, y, z);
        return standable(level, at) ? at : null;
    }

    private static BlockPos caveSpot(ServerLevel level, int x, int nearY, int z) {
        for (int dy = 6; dy >= -10; dy--) {
            BlockPos at = new BlockPos(x, nearY + dy, z);
            if (standable(level, at)) return at;
        }
        return null;
    }
}
