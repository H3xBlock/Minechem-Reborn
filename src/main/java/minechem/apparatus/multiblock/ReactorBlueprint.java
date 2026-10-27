package minechem.apparatus.multiblock;

/**
 * The structure of a reactor, one cell per block position, and the cell of its manager (the machine block itself).
 *
 * <p>The structure is written as {@code structure[y][x][z]}. Each layer is a list of rows, one row per x, and each
 * character of a row is one z. The characters are: {@code -} the wildcard (any block), {@code .} air, {@code A} the wall
 * of metadata 0 and {@code C} the wall of metadata 1 (structure ids 1 and 2). The manager cell is air in the fusion
 * chamber and the wildcard in the fission reactor; the structure check skips it.
 */
public final class ReactorBlueprint {

    /** A cell that may hold anything. */
    public static final int WILDCARD = -1;
    /** A cell that must be empty. */
    public static final int AIR = 0;
    /** The wall of metadata 0 (A): a fusion wall that forwards items and energy to the manager. */
    public static final int WALL_1 = 1;
    /** The wall of metadata 1 (C). */
    public static final int WALL_2 = 2;

    /** The fusion chamber: 13 x 5 x 13, the manager at (6, 1, 6). */
    public static final ReactorBlueprint FUSION = new ReactorBlueprint("fusion", new String[][]{
        {
            "----AAAAA----",
            "--AAAAAAAAA--",
            "-AAACCCCCAAA-",
            "-AACCCCCCCAA-",
            "AACCCCCCCCCAA",
            "AACCCCCCCCCAA",
            "AACCCCCCCCCAA",
            "AACCCCCCCCCAA",
            "AACCCCCCCCCAA",
            "-AACCCCCCCAA-",
            "-AAACCCCCAAA-",
            "--AAAAAAAAA--",
            "----AAAAA----",
        },
        {
            "----AAAAA----",
            "--AACCCCCAA--",
            "-ACC-----CCA-",
            "-AC-------CA-",
            "AC---------CA",
            "AC----C----CA",
            "AC---C.C---CA",
            "AC----C----CA",
            "AC---------CA",
            "-AC-------CA-",
            "-ACC-----CCA-",
            "--AACCCCCAA--",
            "----AAAAA----",
        },
        {
            "----AAAAA----",
            "--AACCCCCAA--",
            "-ACC-----CCA-",
            "-AC-------CA-",
            "AC---------CA",
            "AC----C----CA",
            "AC---C.C---CA",
            "AC----C----CA",
            "AC---------CA",
            "-AC-------CA-",
            "-ACC-----CCA-",
            "--AACCCCCAA--",
            "----AAAAA----",
        },
        {
            "----AAAAA----",
            "--AACCCCCAA--",
            "-ACC-----CCA-",
            "-AC-------CA-",
            "AC---------CA",
            "AC----C----CA",
            "AC---C.C---CA",
            "AC----C----CA",
            "AC---------CA",
            "-AC-------CA-",
            "-ACC-----CCA-",
            "--AACCCCCAA--",
            "----AAAAA----",
        },
        {
            "-------------",
            "-------------",
            "----CCCCC----",
            "---CCCCCCC---",
            "--CCCCCCCCC--",
            "--CCCCACCCC--",
            "--CCCAAACCC--",
            "--CCCCACCCC--",
            "--CCCCCCCCC--",
            "---CCCCCCC---",
            "----CCCCC----",
            "-------------",
            "-------------",
        },
    }, 6, 1, 6);

    /** The fission reactor: 5 x 5 x 5, a hollow shell with the manager in the middle at (2, 2, 2). */
    public static final ReactorBlueprint FISSION = new ReactorBlueprint("fission", new String[][]{
        {"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
        {"CCCCC", "C---C", "C---C", "C---C", "CCCCC"},
        {"CCCCC", "C---C", "C---C", "C---C", "CCCCC"},
        {"CCCCC", "C---C", "C---C", "C---C", "CCCCC"},
        {"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
    }, 2, 2, 2);

    private final String name;
    private final int xSize;
    private final int ySize;
    private final int zSize;
    private final int managerX;
    private final int managerY;
    private final int managerZ;
    private final int[][][] structure;

    /**
     * @param name     the name of the blueprint
     * @param layers   the layers, {@code layers[y][x]} is a row whose characters are the z cells
     * @param managerX the manager's x in the structure
     * @param managerY the manager's y in the structure
     * @param managerZ the manager's z in the structure
     */
    private ReactorBlueprint(String name, String[][] layers, int managerX, int managerY, int managerZ) {
        this.name = name;
        this.ySize = layers.length;
        this.xSize = layers[0].length;
        this.zSize = layers[0][0].length();
        this.managerX = managerX;
        this.managerY = managerY;
        this.managerZ = managerZ;
        this.structure = new int[ySize][xSize][zSize];
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                String row = layers[y][x];
                if (row.length() != zSize) {
                    throw new IllegalArgumentException(name + ": row " + x + " of layer " + y + " is not " + zSize + " long");
                }
                for (int z = 0; z < zSize; z++) {
                    structure[y][x][z] = idOf(row.charAt(z));
                }
            }
        }
    }

    private static int idOf(char c) {
        return switch (c) {
            case '-' -> WILDCARD;
            case '.' -> AIR;
            case 'A' -> WALL_1;
            case 'C' -> WALL_2;
            default -> throw new IllegalArgumentException("unknown structure character " + c);
        };
    }

    public String name() {
        return name;
    }

    public int xSize() {
        return xSize;
    }

    public int ySize() {
        return ySize;
    }

    public int zSize() {
        return zSize;
    }

    public int managerX() {
        return managerX;
    }

    public int managerY() {
        return managerY;
    }

    public int managerZ() {
        return managerZ;
    }

    /**
     * The number of cells of each id, in id order. The air and the wildcard are
     * counted too; the projector shows only the walls.
     */
    public java.util.Map<Integer, Integer> cellCounts() {
        java.util.TreeMap<Integer, Integer> counts = new java.util.TreeMap<>();
        for (int[][] layer : structure) {
            for (int[] row : layer) {
                for (int id : row) {
                    counts.merge(id, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /** The id of a cell, as {@link #WILDCARD}, {@link #AIR}, {@link #WALL_1} or {@link #WALL_2}. */
    public int idAt(int x, int y, int z) {
        return structure[y][x][z];
    }

    /** Whether a cell is the manager's (the manager's cell is always correct). */
    public boolean isManager(int x, int y, int z) {
        return x == managerX && y == managerY && z == managerZ;
    }

    /** The number of cells: x * y * z. */
    public int totalSize() {
        return xSize * ySize * zSize;
    }
}
