package minechem.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The uranium ore generation without a world: the per-chunk loop and the vein shape. {@link UraniumOreFeature} runs it on the
 * world being generated; the unit tests run it on a fake.
 *
 * <p>The random sequence does not match worlds from earlier versions (different seeds and generators).
 */
public final class UraniumOreRules {
    /** x = 16 * chunkX + random.nextInt(16), the same for z. */
    public static final int CHUNK_SPREAD = 16;
    /**
     * The height band: -64 .. 16 (81 heights), as the vanilla diamond, so the uranium is found deep like the diamond.
     */
    public static final int MIN_Y = -64;
    public static final int HEIGHT_BOUND = 81;
    /** The vein size is drawn as random.nextInt(clusterSize + 10). */
    public static final int VEIN_SIZE_EXTRA = 10;

    /** Where a vein is written: the stone test and the ore placement. */
    public interface VeinTarget {
        /** Whether the block at (x, y, z) is replaced by ore: only stone is. */
        boolean canReplace(int x, int y, int z);

        /** Writes the ore block at (x, y, z), and sends the change to the clients. */
        void placeOre(int x, int y, int z);
    }

    private UraniumOreRules() {
    }

    /** The veins tried in one chunk: density + 1 (k = 0 .. density inclusive); none when generateOre is false. */
    public static int attempts(boolean generateOre, int density) {
        return generateOre ? density + 1 : 0;
    }

    /** The bound of random.nextInt for a vein size: the size is 0 .. clusterSize + 9 (0 .. 12 with the default 3). */
    public static int veinSizeBound(int clusterSize) {
        return clusterSize + VEIN_SIZE_EXTRA;
    }

    /**
     * Generates the veins of one chunk whose lowest block corner is (chunkMinX, chunkMinZ), that is 16 * chunkX and 16 * chunkZ.
     * Draws from the random in this order: x, y, z, the vein size, then the vein itself.
     *
     * @return the number of ore blocks placed
     */
    public static int generateChunk(RandomSource random, int chunkMinX, int chunkMinZ, boolean generateOre, int density, int clusterSize,
            VeinTarget target) {
        int placed = 0;
        int attempts = attempts(generateOre, density);
        for (int k = 0; k < attempts; k++) {
            int x = chunkMinX + random.nextInt(CHUNK_SPREAD);
            int y = MIN_Y + random.nextInt(HEIGHT_BOUND);
            int z = chunkMinZ + random.nextInt(CHUNK_SPREAD);
            int oreCount = random.nextInt(veinSizeBound(clusterSize));
            placed += vein(random, x, y, z, oreCount, target);
        }
        return placed;
    }

    /**
     * Places one vein of numberOfBlocks ore blocks: a line of numberOfBlocks + 1 ellipsoids between two ends around (x + 8, z + 8).
     * A size of 0 places nothing (its divisions by zero give NaN, so no block passes the tests), but still draws from the random.
     *
     * @return the number of ore blocks placed
     */
    public static int vein(RandomSource random, int x, int y, int z, int numberOfBlocks, VeinTarget target) {
        int placed = 0;
        float angle = random.nextFloat() * (float) Math.PI;
        double x0 = (float) (x + 8) + Mth.sin(angle) * (float) numberOfBlocks / 8.0F;
        double x1 = (float) (x + 8) - Mth.sin(angle) * (float) numberOfBlocks / 8.0F;
        double z0 = (float) (z + 8) + Mth.cos(angle) * (float) numberOfBlocks / 8.0F;
        double z1 = (float) (z + 8) - Mth.cos(angle) * (float) numberOfBlocks / 8.0F;
        double y0 = y + random.nextInt(3) - 2;
        double y1 = y + random.nextInt(3) - 2;

        for (int l = 0; l <= numberOfBlocks; l++) {
            double cx = x0 + (x1 - x0) * l / numberOfBlocks;
            double cy = y0 + (y1 - y0) * l / numberOfBlocks;
            double cz = z0 + (z1 - z0) * l / numberOfBlocks;
            double scale = random.nextDouble() * numberOfBlocks / 16.0D;
            double width = (Mth.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * scale + 1.0D;
            double height = (Mth.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * scale + 1.0D;
            int minX = Mth.floor(cx - width / 2.0D);
            int minY = Mth.floor(cy - height / 2.0D);
            int minZ = Mth.floor(cz - width / 2.0D);
            int maxX = Mth.floor(cx + width / 2.0D);
            int maxY = Mth.floor(cy + height / 2.0D);
            int maxZ = Mth.floor(cz + width / 2.0D);

            for (int bx = minX; bx <= maxX; bx++) {
                double dx = (bx + 0.5D - cx) / (width / 2.0D);
                if (dx * dx < 1.0D) {
                    for (int by = minY; by <= maxY; by++) {
                        double dy = (by + 0.5D - cy) / (height / 2.0D);
                        if (dx * dx + dy * dy < 1.0D) {
                            for (int bz = minZ; bz <= maxZ; bz++) {
                                double dz = (bz + 0.5D - cz) / (width / 2.0D);
                                if (dx * dx + dy * dy + dz * dz < 1.0D && target.canReplace(bx, by, bz)) {
                                    target.placeOre(bx, by, bz);
                                    placed++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }
}
