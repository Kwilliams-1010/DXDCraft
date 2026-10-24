package com.dxdcraft.world;

import com.dxdcraft.DxDCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Base for the Heaven / Underworld points of interest. Each POI is built block by block in code,
 * centred on the feature origin and kept within 12 blocks of it so it never writes outside the
 * chunks worldgen allows.
 */
public abstract class PoiFeature extends Feature<NoneFeatureConfiguration> {
    private final boolean underworld;

    protected PoiFeature(boolean underworld) {
        super(NoneFeatureConfiguration.CODEC);
        this.underworld = underworld;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        if (underworld) {
            origin = findFloor(level, origin.getX(), origin.getZ());
            if (origin == null) return false;
        } else {
            if (origin.getY() < level.getMinBuildHeight() + 8) return false; // empty sky column
            BlockState below = level.getBlockState(origin.below());
            if (below.isAir() || !below.getFluidState().isEmpty()) return false;
        }
        build(new Builder(level, origin, random), random);
        return true;
    }

    /** Underworld has a ceiling: scan down for open air standing on solid ground (not lava). */
    private static BlockPos findFloor(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 100, z);
        for (int y = 100; y > 34; y--) {
            pos.setY(y);
            BlockState here = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            if (here.isAir() && !below.isAir() && below.getFluidState().isEmpty()) return pos.immutable();
        }
        return null;
    }

    protected abstract void build(Builder b, RandomSource random);

    /** Small helper for placing blocks relative to the POI origin. */
    public static final class Builder {
        private final WorldGenLevel level;
        private final BlockPos o;
        private final RandomSource random;

        Builder(WorldGenLevel level, BlockPos origin, RandomSource random) {
            this.level = level;
            this.o = origin;
            this.random = random;
        }

        public BlockPos at(int x, int y, int z) {
            return o.offset(x, y, z);
        }

        public BlockState get(int x, int y, int z) {
            return level.getBlockState(at(x, y, z));
        }

        public void set(int x, int y, int z, BlockState state) {
            level.setBlock(at(x, y, z), state, 2);
        }

        public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                        set(x, y, z, state);
        }

        public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
            fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
        }

        /** Four walls (no floor/roof) of the box. */
        public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            fill(x1, y1, z1, x2, y2, z1, state);
            fill(x1, y1, z2, x2, y2, z2, state);
            fill(x1, y1, z1, x1, y2, z2, state);
            fill(x2, y1, z1, x2, y2, z2, state);
        }

        /** Fills downward under the footprint until it meets solid ground (or maxDepth). */
        public void foundation(int x1, int z1, int x2, int z2, BlockState state, int maxDepth) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    for (int dy = 1; dy <= maxDepth; dy++) {
                        BlockState s = get(x, -dy, z);
                        if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                        set(x, -dy, z, state);
                    }
                }
            }
        }

        public void chest(int x, int y, int z, String lootTable, Direction facing) {
            BlockState chest = chestBlock().defaultBlockState();
            if (chest.hasProperty(ChestBlock.FACING)) chest = chest.setValue(ChestBlock.FACING, facing);
            set(x, y, z, chest);
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, DxDCraft.id("chests/" + lootTable));
            RandomizableContainer.setBlockEntityLootTable(level, random, at(x, y, z), key);
        }

        /** Lootr's per-player chest when Lootr is installed, otherwise a vanilla chest. */
        private static net.minecraft.world.level.block.Block chestBlock() {
            if (net.neoforged.fml.ModList.get().isLoaded("lootr")) {
                net.minecraft.world.level.block.Block lootr = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lootr", "lootr_chest"));
                if (lootr != Blocks.AIR) return lootr;
            }
            return Blocks.CHEST;
        }

        public void spawner(int x, int y, int z, EntityType<?> type) {
            set(x, y, z, Blocks.SPAWNER.defaultBlockState());
            BlockEntity be = level.getBlockEntity(at(x, y, z));
            if (be instanceof SpawnerBlockEntity spawner) spawner.setEntityId(type, random);
        }

        public RandomSource random() {
            return random;
        }
    }
}
