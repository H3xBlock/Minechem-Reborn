package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;

/**
 * Silicon has no effect hooks of its own; its effect is the drop decomposition in PolytoolEvents.
 */
public class PolytoolTypeSilicon extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 14;
    }

    @Override
    public String getDescription() {
        return "Advanced circuitry decomposes mob drops";
    }
}
