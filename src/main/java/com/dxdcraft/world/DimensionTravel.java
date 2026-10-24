package com.dxdcraft.world;

import com.dxdcraft.DxDCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Safe teleporting between the Overworld, Heaven and the Underworld. */
public final class DimensionTravel {
    public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, DxDCraft.id("heaven"));
    public static final ResourceKey<Level> UNDERWORLD = ResourceKey.create(Registries.DIMENSION, DxDCraft.id("underworld"));

    private DimensionTravel() {}

    public static boolean inHeaven(ServerPlayer p) {
        return p.level().dimension() == HEAVEN;
    }

    public static boolean inUnderworld(ServerPlayer p) {
        return p.level().dimension() == UNDERWORLD;
    }

    /** Teleports the player to the given dimension at (roughly) the same x/z, on safe ground. */
    public static boolean travel(ServerPlayer p, ResourceKey<Level> destKey) {
        ServerLevel dest = p.server.getLevel(destKey);
        if (dest == null) {
            p.displayClientMessage(Component.literal("That realm doesn't exist in this world.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int x = p.getBlockX();
        int z = p.getBlockZ();
        BlockPos target;
        if (destKey == UNDERWORLD) target = findCaveFloor(dest, x, z);
        else target = findSurface(dest, x, z);

        p.serverLevel().sendParticles(destKey == HEAVEN ? ParticleTypes.END_ROD : ParticleTypes.CRIMSON_SPORE,
                p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
        p.teleportTo(dest, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        dest.playSound(null, target, destKey == HEAVEN ? SoundEvents.BEACON_ACTIVATE : SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        dest.sendParticles(destKey == HEAVEN ? ParticleTypes.END_ROD : ParticleTypes.CRIMSON_SPORE,
                target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, 80, 0.6, 1.0, 0.6, 0.05);
        String name = destKey == HEAVEN ? "Heaven" : destKey == UNDERWORLD ? "the Underworld" : "the Human World";
        p.displayClientMessage(Component.literal("You arrive in " + name).withStyle(
                destKey == HEAVEN ? ChatFormatting.GOLD : destKey == UNDERWORLD ? ChatFormatting.DARK_RED : ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        return true;
    }

    /** For open-sky dimensions (Overworld, Heaven's floating islands). Searches outward for land. */
    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        int min = level.getMinBuildHeight();
        for (int r = 0; r <= 48; r += 16) { // at most 25 columns -> keeps chunk generation on travel small
            for (int i = 0; i < (r == 0 ? 1 : 8); i++) {
                double a = i * Math.PI / 4;
                int cx = x + (int) (Math.cos(a) * r);
                int cz = z + (int) (Math.sin(a) * r);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                if (y > min + 2) {
                    BlockState ground = level.getBlockState(new BlockPos(cx, y - 1, cz));
                    if (ground.getFluidState().isEmpty()) return new BlockPos(cx, y, cz);
                }
            }
        }
        // Nothing found (open sky in Heaven): make a small cloud platform.
        BlockPos base = new BlockPos(x, 100, z);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.WHITE_WOOL.defaultBlockState());
        return base;
    }

    /** For the Underworld (has a ceiling): scan down for an air pocket on solid ground. */
    private static BlockPos findCaveFloor(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 32; r += 16) {
            for (int i = 0; i < (r == 0 ? 1 : 8); i++) {
                double a = i * Math.PI / 4;
                int cx = x + (int) (Math.cos(a) * r);
                int cz = z + (int) (Math.sin(a) * r);
                for (int y = 110; y > 33; y--) {
                    BlockPos pos = new BlockPos(cx, y, cz);
                    BlockState below = level.getBlockState(pos.below());
                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                            && !below.isAir() && below.getFluidState().isEmpty()) {
                        return pos;
                    }
                }
            }
        }
        // Carve a safe pocket.
        BlockPos base = new BlockPos(x, 70, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                for (int dy = 0; dy <= 2; dy++) level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        return base;
    }
}
