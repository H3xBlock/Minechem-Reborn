package minechem.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.junit.jupiter.api.Test;

/** The uranium ore loop and the vein shape, without a world. */
class UraniumOreRulesTest {
    /** A random that returns the largest value of each nextInt bound and records the bounds and the nextDouble calls. */
    private static final class MaxRandom implements RandomSource {
        final List<Integer> bounds = new ArrayList<>();
        int doubles;

        @Override public int nextInt(int bound) { bounds.add(bound); return bound - 1; }
        @Override public float nextFloat() { return 0.25F; }
        @Override public double nextDouble() { doubles++; return 0.5D; }
        @Override public RandomSource fork() { throw new UnsupportedOperationException(); }
        @Override public PositionalRandomFactory forkPositional() { throw new UnsupportedOperationException(); }
        @Override public void setSeed(long seed) { }
        @Override public int nextInt() { throw new UnsupportedOperationException(); }
        @Override public long nextLong() { throw new UnsupportedOperationException(); }
        @Override public boolean nextBoolean() { throw new UnsupportedOperationException(); }
        @Override public double nextGaussian() { throw new UnsupportedOperationException(); }
    }

    /** A world of stone (or of nothing replaceable) that records the ore blocks placed. */
    private static final class Target implements UraniumOreRules.VeinTarget {
        final boolean stone;
        final Set<List<Integer>> ores = new HashSet<>();
        int tests;

        Target(boolean stone) { this.stone = stone; }

        /** Stone until it is ore: a block already turned into ore is not stone any more, as in the world. */
        @Override public boolean canReplace(int x, int y, int z) { tests++; return stone && !ores.contains(List.of(x, y, z)); }
        @Override public void placeOre(int x, int y, int z) { ores.add(List.of(x, y, z)); }
    }

    @Test
    void theDefaultDensityGivesSixVeinsAndTheDefaultClusterGivesUpToTwelve() {
        assertEquals(6, UraniumOreRules.attempts(true, 5), "k = 0 .. 5 inclusive");
        assertEquals(2, UraniumOreRules.attempts(true, 1));
        assertEquals(65, UraniumOreRules.attempts(true, 64));
        assertEquals(0, UraniumOreRules.attempts(false, 5), "generateOre off: no vein");
        assertEquals(13, UraniumOreRules.veinSizeBound(3), "nextInt(3 + 10): 0 .. 12");
        assertEquals(20, UraniumOreRules.veinSizeBound(10));
    }

    @Test
    void aChunkDrawsXYZAndTheSizeForEachVeinInFixedOrder() {
        MaxRandom random = new MaxRandom();
        UraniumOreRules.generateChunk(random, 32, -48, true, 5, 3, new Target(true));
        List<Integer> expected = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            // x, y, z, the vein size, then the two y ends of WorldGenMinable.
            expected.addAll(List.of(16, 81, 16, 13, 3, 3));
        }
        assertEquals(expected, random.bounds);
        // Each vein of the largest size (12) draws one nextDouble per step, from 0 to 12 inclusive.
        assertEquals(6 * 13, random.doubles);
    }

    @Test
    void anotherConfigChangesTheVeinCountAndSize() {
        MaxRandom random = new MaxRandom();
        UraniumOreRules.generateChunk(random, 0, 0, true, 2, 7, new Target(true));
        assertEquals(3 * 6, random.bounds.size(), "density 2: three veins");
        assertEquals(17, (int) random.bounds.get(3), "cluster size 7: nextInt(17)");
        assertEquals(3 * 17, random.doubles, "veins of 16 blocks: 17 steps each");
    }

    @Test
    void generateOreOffDrawsAndPlacesNothing() {
        MaxRandom random = new MaxRandom();
        Target target = new Target(true);
        assertEquals(0, UraniumOreRules.generateChunk(random, 0, 0, false, 5, 3, target));
        assertTrue(random.bounds.isEmpty() && random.doubles == 0 && target.ores.isEmpty());
    }

    @Test
    void aVeinOfSizeZeroPlacesNothing() {
        Target target = new Target(true);
        assertEquals(0, UraniumOreRules.vein(RandomSource.create(1L), 100, 20, 100, 0, target));
        assertEquals(0, target.tests, "no block is even tested");
    }

    @Test
    void aVeinIsCentredEightBlocksPastItsCornerAndOnlyReplacesStone() {
        for (long seed = 0; seed < 20; seed++) {
            Target target = new Target(true);
            int placed = UraniumOreRules.vein(RandomSource.create(seed), 160, 30, -320, 12, target);
            assertTrue(placed > 0, "a vein of 12 in stone places ore (seed " + seed + ")");
            assertEquals(placed, target.ores.size());
            for (List<Integer> ore : target.ores) {
                // Ends at x + 8 +- 12/8, radius at most (2 * 12/16 + 1) / 2; y ends at y - 2 .. y.
                assertTrue(ore.get(0) >= 165 && ore.get(0) <= 170, "x " + ore);
                assertTrue(ore.get(2) >= -315 && ore.get(2) <= -310, "z " + ore);
                assertTrue(ore.get(1) >= 27 && ore.get(1) <= 30, "y " + ore);
            }
            Target air = new Target(false);
            assertEquals(0, UraniumOreRules.vein(RandomSource.create(seed), 160, 30, -320, 12, air), "nothing but stone is replaced");
            assertTrue(air.tests > 0);
        }
    }
}
