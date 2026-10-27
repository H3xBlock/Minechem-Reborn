package minechem.polytool;

import java.util.HashMap;
import java.util.Map;
import minechem.polytool.types.PolytoolTypeArgon;
import minechem.polytool.types.PolytoolTypeBeryllium;
import minechem.polytool.types.PolytoolTypeBoron;
import minechem.polytool.types.PolytoolTypeBromine;
import minechem.polytool.types.PolytoolTypeCaesium;
import minechem.polytool.types.PolytoolTypeCalcium;
import minechem.polytool.types.PolytoolTypeCarbon;
import minechem.polytool.types.PolytoolTypeChlorine;
import minechem.polytool.types.PolytoolTypeChromium;
import minechem.polytool.types.PolytoolTypeFluorine;
import minechem.polytool.types.PolytoolTypeFrancium;
import minechem.polytool.types.PolytoolTypeGold;
import minechem.polytool.types.PolytoolTypeHelium;
import minechem.polytool.types.PolytoolTypeHydrogen;
import minechem.polytool.types.PolytoolTypeIron;
import minechem.polytool.types.PolytoolTypeKrypton;
import minechem.polytool.types.PolytoolTypeLead;
import minechem.polytool.types.PolytoolTypeMagnesium;
import minechem.polytool.types.PolytoolTypeMercury;
import minechem.polytool.types.PolytoolTypeNeon;
import minechem.polytool.types.PolytoolTypeNickel;
import minechem.polytool.types.PolytoolTypeNitrogen;
import minechem.polytool.types.PolytoolTypeOxygen;
import minechem.polytool.types.PolytoolTypePhosphorus;
import minechem.polytool.types.PolytoolTypePlatnium;
import minechem.polytool.types.PolytoolTypeRubidium;
import minechem.polytool.types.PolytoolTypeSilicon;
import minechem.polytool.types.PolytoolTypeSilver;
import minechem.polytool.types.PolytoolTypeSodium;
import minechem.polytool.types.PolytoolTypeSulfur;
import minechem.polytool.types.PolytoolTypeTitanium;
import minechem.polytool.types.PolytoolTypeUranium;
import minechem.polytool.types.PolytoolTypeZirconium;

/**
 * The polytool upgrade of each element. An element without a registered type gives no upgrade.
 */
public final class PolytoolHelper {
    private static final Map<Integer, PolytoolUpgradeType> TYPES = new HashMap<>();

    static {
        register(new PolytoolTypeArgon());
        register(new PolytoolTypeBeryllium());
        register(new PolytoolTypeBoron());
        register(new PolytoolTypeBromine());
        register(new PolytoolTypeCaesium());
        register(new PolytoolTypeCalcium());
        register(new PolytoolTypeCarbon());
        register(new PolytoolTypeChlorine());
        register(new PolytoolTypeChromium());
        register(new PolytoolTypeFluorine());
        register(new PolytoolTypeFrancium());
        register(new PolytoolTypeGold());
        register(new PolytoolTypeHelium());
        register(new PolytoolTypeHydrogen());
        register(new PolytoolTypeIron());
        register(new PolytoolTypeKrypton());
        register(new PolytoolTypeLead());
        register(new PolytoolTypeMagnesium());
        register(new PolytoolTypeMercury());
        register(new PolytoolTypeNeon());
        register(new PolytoolTypeNickel());
        register(new PolytoolTypeNitrogen());
        register(new PolytoolTypeOxygen());
        register(new PolytoolTypePhosphorus());
        register(new PolytoolTypePlatnium());
        register(new PolytoolTypeRubidium());
        register(new PolytoolTypeSilicon());
        register(new PolytoolTypeSilver());
        register(new PolytoolTypeSodium());
        register(new PolytoolTypeSulfur());
        register(new PolytoolTypeTitanium());
        register(new PolytoolTypeUranium());
        register(new PolytoolTypeZirconium());
        for (ElementAlloy alloy : ElementAlloy.values()) {
            register(new PolytoolTypeAlloy(alloy));
        }
    }

    private PolytoolHelper() {
    }

    /** The upgrade of an element with a power, or null when the element has no polytool upgrade. */
    public static PolytoolUpgradeType getTypeFromElement(int atomicNumber, float power) {
        PolytoolUpgradeType type = TYPES.get(atomicNumber);
        return type == null ? null : type.setPower(power);
    }

    /** Registers the type of an element; the first registration of an element wins. */
    public static void register(PolytoolUpgradeType type) {
        TYPES.putIfAbsent(type.atomicNumber(), type);
    }
}
