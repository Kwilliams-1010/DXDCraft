package com.dxdcraft.world;

import com.dxdcraft.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The eight points of interest. Heaven: 4 holy sites. Underworld: 4 devil / fallen angel sites. */
public final class DxDPois {
    private DxDPois() {}

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState stairs(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    // =====================================================================================
    // HEAVEN
    // =====================================================================================

    /** A towering white-and-gold gate with chain "bars" and an offering chest behind it. */
    public static class GateOfHeaven extends PoiFeature {
        public GateOfHeaven() { super(false); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-6, 0, -3, 6, 14, 3);
            b.foundation(-6, -3, 6, 3, s(Blocks.QUARTZ_BLOCK), 10);
            b.fill(-6, -1, -3, 6, -1, 3, s(Blocks.SMOOTH_QUARTZ));
            b.fill(-2, -1, -3, 2, -1, 3, s(Blocks.GOLD_BLOCK)); // golden path through the gate
            // pillars
            b.fill(-5, 0, -1, -4, 10, 1, s(Blocks.QUARTZ_PILLAR));
            b.fill(4, 0, -1, 5, 10, 1, s(Blocks.QUARTZ_PILLAR));
            // arch and crown
            b.fill(-6, 11, -1, 6, 12, 1, s(Blocks.SMOOTH_QUARTZ));
            b.fill(-3, 13, -1, 3, 13, 1, s(Blocks.GOLD_BLOCK));
            b.fill(-1, 14, 0, 1, 14, 0, s(Blocks.GOLD_BLOCK));
            b.set(0, 12, -1, s(Blocks.GLOWSTONE));
            b.set(0, 12, 1, s(Blocks.GLOWSTONE));
            // the gate itself: golden chains you can walk between
            for (int x : new int[]{-3, -1, 1, 3}) b.fill(x, 0, 0, x, 10, 0, s(Blocks.CHAIN));
            // lanterns at the corners
            b.set(-6, 0, -3, s(Blocks.LANTERN));
            b.set(6, 0, -3, s(Blocks.LANTERN));
            b.set(-6, 0, 3, s(Blocks.LANTERN));
            b.set(6, 0, 3, s(Blocks.LANTERN));
            b.chest(0, 0, 2, "gate_of_heaven", Direction.NORTH);
        }
    }

    /** A quartz cathedral with stained glass, pews and an altar chest. */
    public static class SeraphCathedral extends PoiFeature {
        public SeraphCathedral() { super(false); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-7, 0, -10, 7, 13, 10);
            b.foundation(-7, -10, 7, 10, s(Blocks.QUARTZ_BRICKS), 10);
            b.fill(-7, -1, -10, 7, -1, 10, s(Blocks.SMOOTH_QUARTZ));
            b.walls(-7, 0, -10, 7, 7, 10, s(Blocks.QUARTZ_BRICKS));
            // side windows
            for (int z = -8; z <= 8; z += 3) {
                b.fill(-7, 2, z, -7, 5, z, s(z % 2 == 0 ? Blocks.YELLOW_STAINED_GLASS : Blocks.WHITE_STAINED_GLASS));
                b.fill(7, 2, z, 7, 5, z, s(z % 2 == 0 ? Blocks.YELLOW_STAINED_GLASS : Blocks.WHITE_STAINED_GLASS));
            }
            // stepped roof
            for (int k = 0; k <= 4; k++) b.fill(-7 + k, 8 + k, -10, 7 - k, 8 + k, 10, s(Blocks.SMOOTH_QUARTZ));
            // entrance and rose window
            b.clear(-1, 0, -10, 1, 3, -10);
            b.fill(-1, 5, -10, 1, 6, -10, s(Blocks.YELLOW_STAINED_GLASS));
            // aisle and pews
            b.fill(-1, 0, -9, 1, 0, 6, s(Blocks.LIGHT_BLUE_CARPET));
            for (int z = -7; z <= 4; z += 2) {
                for (int x = 3; x <= 5; x++) {
                    b.set(x, 0, z, stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
                    b.set(-x, 0, z, stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
                }
            }
            // altar
            b.fill(-3, 0, 7, 3, 0, 9, s(Blocks.SMOOTH_QUARTZ));
            b.set(-1, 1, 8, s(Blocks.GOLD_BLOCK));
            b.set(1, 1, 8, s(Blocks.GOLD_BLOCK));
            b.chest(0, 1, 8, "seraph_cathedral", Direction.NORTH);
            b.merchant(0, 0, 4, com.dxdcraft.entity.npc.DxDMerchant.Kind.ANGEL);
            b.set(-2, 1, 9, s(Blocks.LANTERN));
            b.set(2, 1, 9, s(Blocks.LANTERN));
            // ceiling lights
            for (int z = -8; z <= 8; z += 4) {
                b.set(-3, 7, z, s(Blocks.SEA_LANTERN));
                b.set(3, 7, z, s(Blocks.SEA_LANTERN));
            }
        }
    }

    /** A ring of eight pillars around a golden dais where Brave Saints are said to train. */
    public static class BraveSaintShrine extends PoiFeature {
        public BraveSaintShrine() { super(false); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-6, 0, -6, 6, 8, 6);
            b.foundation(-6, -6, 6, 6, s(Blocks.SMOOTH_QUARTZ), 8);
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= 6.4) b.set(x, -1, z, s(d >= 4.5 && d <= 5.5 ? Blocks.CHISELED_QUARTZ_BLOCK : Blocks.SMOOTH_QUARTZ));
                }
            }
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int x = (int) Math.round(5 * Math.cos(a));
                int z = (int) Math.round(5 * Math.sin(a));
                b.fill(x, 0, z, x, 4, z, s(Blocks.QUARTZ_PILLAR));
                b.set(x, 5, z, s(Blocks.SEA_LANTERN));
            }
            b.fill(-1, 0, -1, 1, 0, 1, s(Blocks.GOLD_BLOCK));
            b.set(0, 1, 0, s(Blocks.ENCHANTING_TABLE));
            b.chest(0, 0, 2, "brave_saint_shrine", Direction.NORTH);
        }
    }

    /** A meadow with a blossoming tree of golden fruit. */
    public static class GardenOfEden extends PoiFeature {
        private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.ALLIUM, Blocks.AZURE_BLUET,
                Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};

        public GardenOfEden() { super(false); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-6, 0, -6, 6, 10, 6);
            b.foundation(-6, -6, 6, 6, s(Blocks.DIRT), 6);
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > 6.4) continue;
                    b.set(x, -1, z, s(Blocks.GRASS_BLOCK));
                    if (d > 2 && r.nextFloat() < 0.35F) b.set(x, 0, z, s(FLOWERS[r.nextInt(FLOWERS.length)]));
                }
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx * dx + dy * dy + dz * dz > 10) continue;
                        Block leaf = r.nextFloat() < 0.6F ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES;
                        b.set(dx, 6 + dy, dz, leaf.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                    }
                }
            }
            b.fill(0, 0, 0, 0, 6, 0, s(Blocks.OAK_LOG));
            b.chest(1, 0, 0, "garden_of_eden", Direction.EAST);
        }
    }

    // =====================================================================================
    // UNDERWORLD
    // =====================================================================================

    /** A walled crimson castle with corner towers and a throne room keep. */
    public static class DevilNobleCastle extends PoiFeature {
        public DevilNobleCastle() { super(true); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-10, 0, -10, 10, 13, 10);
            b.foundation(-10, -10, 10, 10, s(Blocks.NETHER_BRICKS), 12);
            b.fill(-10, -1, -10, 10, -1, 10, s(Blocks.POLISHED_BLACKSTONE));
            // outer wall + battlements
            b.walls(-10, 0, -10, 10, 6, 10, s(Blocks.RED_NETHER_BRICKS));
            for (int i = -10; i <= 10; i += 2) {
                b.set(i, 7, -10, s(Blocks.RED_NETHER_BRICKS));
                b.set(i, 7, 10, s(Blocks.RED_NETHER_BRICKS));
                b.set(-10, 7, i, s(Blocks.RED_NETHER_BRICKS));
                b.set(10, 7, i, s(Blocks.RED_NETHER_BRICKS));
            }
            // corner towers
            int[][] corners = {{-10, -10}, {7, -10}, {-10, 7}, {7, 7}};
            for (int[] c : corners) {
                b.fill(c[0], 0, c[1], c[0] + 3, 11, c[1] + 3, s(Blocks.NETHER_BRICKS));
                b.walls(c[0], 12, c[1], c[0] + 3, 12, c[1] + 3, s(Blocks.RED_NETHER_BRICKS));
                b.set(c[0] + 1, 12, c[1] + 1, s(Blocks.SHROOMLIGHT));
            }
            // gate
            b.clear(-1, 0, -10, 1, 3, -10);
            b.set(-6, 0, -8, s(Blocks.SOUL_LANTERN));
            b.set(6, 0, -8, s(Blocks.SOUL_LANTERN));
            // keep / throne room
            b.walls(-5, 0, -2, 5, 9, 8, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            b.fill(-5, 10, -2, 5, 10, 8, s(Blocks.BLACKSTONE));
            b.clear(-1, 0, -2, 1, 2, -2);
            b.fill(-1, 0, -1, 1, 0, 6, s(Blocks.RED_CARPET));
            b.set(0, 0, 7, stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH));
            b.set(-1, 0, 7, s(Blocks.GOLD_BLOCK));
            b.set(1, 0, 7, s(Blocks.GOLD_BLOCK));
            b.set(-5, 5, 3, s(Blocks.SHROOMLIGHT));
            b.set(5, 5, 3, s(Blocks.SHROOMLIGHT));
            b.set(0, 9, 3, s(Blocks.SHROOMLIGHT));
            b.chest(-3, 0, 6, "devil_noble_castle", Direction.EAST);
            b.chest(3, 0, 6, "devil_noble_castle", Direction.WEST);
            b.merchant(0, 0, 2, com.dxdcraft.entity.npc.DxDMerchant.Kind.DEVIL);
        }
    }

    /** A chessboard battlefield ringed by stepped stands, guarded by a Stray Devil spawner. */
    public static class RatingGameArena extends PoiFeature {
        public RatingGameArena() { super(true); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-11, 0, -11, 11, 6, 11);
            b.foundation(-11, -11, 11, 11, s(Blocks.BLACKSTONE), 10);
            for (int x = -11; x <= 11; x++) {
                for (int z = -11; z <= 11; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= 8.5) {
                        b.set(x, -1, z, s(((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.RED_NETHER_BRICKS));
                    } else if (d <= 11.5) {
                        int h = d <= 9.5 ? 0 : d <= 10.5 ? 1 : 2;
                        b.fill(x, -1, z, x, h, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
                    }
                }
            }
            b.clear(-1, 0, -11, 1, 2, -9); // entrance through the stands
            b.set(11, 3, 0, s(Blocks.SOUL_LANTERN));
            b.set(-11, 3, 0, s(Blocks.SOUL_LANTERN));
            b.set(0, 3, 11, s(Blocks.SOUL_LANTERN));
            b.spawner(0, 0, 0, ModEntities.STRAY_DEVIL.get());
            b.chest(0, 0, 3, "rating_game_arena", Direction.NORTH);
        }
    }

    /** A crumbling blackstone crypt where Stray Devils nest. */
    public static class StrayDevilLair extends PoiFeature {
        public StrayDevilLair() { super(true); }

        private static BlockState brick(RandomSource r) {
            return s(r.nextFloat() < 0.4F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-5, 0, -5, 5, 5, 5);
            b.foundation(-5, -5, 5, 5, s(Blocks.BLACKSTONE), 8);
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    b.set(x, -1, z, r.nextFloat() < 0.2F ? s(Blocks.SOUL_SOIL) : brick(r));
                    boolean edge = Math.abs(x) == 5 || Math.abs(z) == 5;
                    if (edge) {
                        int height = r.nextFloat() < 0.25F ? r.nextInt(3) : 5; // ruined gaps
                        for (int y = 0; y < height; y++) b.set(x, y, z, brick(r));
                    } else if (r.nextFloat() < 0.5F) {
                        b.set(x, 5, z, brick(r)); // partial roof
                    }
                }
            }
            for (int i = 0; i < 10; i++) {
                b.set(r.nextInt(9) - 4, r.nextInt(4) + 1, r.nextInt(9) - 4, s(Blocks.COBWEB));
            }
            b.set(-4, 0, -4, s(Blocks.BONE_BLOCK));
            b.set(4, 0, 4, s(Blocks.BONE_BLOCK));
            b.clear(0, 0, -5, 0, 1, -5);
            b.spawner(0, 0, 0, ModEntities.STRAY_DEVIL.get());
            b.chest(-3, 0, 3, "stray_devil_lair", Direction.EAST);
            b.chest(3, 0, -3, "stray_devil_lair", Direction.WEST);
        }
    }

    /** An obsidian research tower of the Fallen Angels, with a spawner below and loot at the top. */
    public static class GrigoriOutpost extends PoiFeature {
        public GrigoriOutpost() { super(true); }

        @Override
        protected void build(Builder b, RandomSource r) {
            b.clear(-4, 0, -4, 4, 18, 4);
            b.foundation(-4, -4, 4, 4, s(Blocks.OBSIDIAN), 10);
            b.fill(-4, -1, -4, 4, -1, 4, s(Blocks.PURPUR_BLOCK));
            for (int y = 0; y <= 15; y++) {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        if (Math.abs(x) != 3 && Math.abs(z) != 3) continue;
                        b.set(x, y, z, s(r.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN));
                    }
                }
            }
            for (int y : new int[]{3, 8, 13}) {
                b.set(0, y, -3, s(Blocks.PURPLE_STAINED_GLASS));
                b.set(0, y, 3, s(Blocks.PURPLE_STAINED_GLASS));
                b.set(-3, y, 0, s(Blocks.PURPLE_STAINED_GLASS));
            }
            b.clear(0, 0, -3, 0, 1, -3);
            b.fill(-2, 5, -2, 2, 5, 2, s(Blocks.PURPUR_BLOCK));
            b.fill(-2, 10, -2, 2, 10, 2, s(Blocks.PURPUR_BLOCK));
            b.fill(-3, 16, -3, 3, 16, 3, s(Blocks.PURPUR_BLOCK));
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
            for (int y = 0; y <= 14; y++) b.set(2, y, 0, ladder);
            for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) b.set(c[0], 17, c[1], s(Blocks.END_ROD));
            b.set(0, 9, 0, s(Blocks.SEA_LANTERN));
            b.set(0, 15, 0, s(Blocks.SEA_LANTERN));
            b.spawner(-1, 0, 1, ModEntities.ROGUE_FALLEN_ANGEL.get());
            b.chest(-2, 11, -2, "grigori_outpost", Direction.EAST);
        }
    }
}
