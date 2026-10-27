package minechem.apparatus.multiblock;

import minechem.helper.Jenkins;

/**
 * The rules of the fission and fusion reactors, without any block or block entity: the reactors use them, and so do the JEI
 * plugin and the unit tests (a block entity cannot be loaded outside the game).
 */
public final class ReactorRules {
    private ReactorRules() {
    }

    /**
     * The atomic number of the element that a split of this element gives (two of it), or 0 when the split gives nothing: the
     * element is 0, or half of it is not an element.
     */
    public static int halfOf(int atomicNumber) {
        if (atomicNumber <= 0) {
            return 0;
        }
        int half = atomicNumber / 2;
        return half > 0 && Jenkins.get(half) != null ? half : 0;
    }

    /**
     * The atomic number that two elements fuse into, or 0 when they cannot be fused: one is 0, or the sum is not an element.
     */
    public static int fusedOf(int left, int right) {
        int fused = left + right;
        return left > 0 && right > 0 && Jenkins.get(fused) != null ? fused : 0;
    }
}
