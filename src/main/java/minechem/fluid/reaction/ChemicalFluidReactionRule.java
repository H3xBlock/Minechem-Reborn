package minechem.fluid.reaction;

import minechem.chemical.ChemicalBase;

/**
 * The two chemicals that react. The order does not matter: lithium in water is the same rule as water with lithium, so
 * equality and hashing ignore the order.
 */
public final class ChemicalFluidReactionRule {

    public final ChemicalBase chemicalA;
    public final ChemicalBase chemicalB;

    public ChemicalFluidReactionRule(ChemicalBase a, ChemicalBase b) {
        chemicalA = a;
        chemicalB = b;
    }

    @Override
    public int hashCode() {
        return chemicalA.hashCode() ^ chemicalB.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ChemicalFluidReactionRule another) {
            return (chemicalA == another.chemicalA && chemicalB == another.chemicalB) || (chemicalB == another.chemicalA && chemicalA == another.chemicalB);
        }
        return false;
    }
}
