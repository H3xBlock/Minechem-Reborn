package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;

public class PolytoolTypeNitrogen extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 7;
    }

    @Override
    public String getDescription() {
        return "Preservation allows retrieving of fresh meat from rotten flesh";
    }
}
