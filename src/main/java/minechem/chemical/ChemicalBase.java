package minechem.chemical;

import java.util.List;

/**
 * The shared base of {@link Element} and {@link Molecule}. It has no NBT read/write or ItemStack helpers: items reference
 * chemicals by their full name through a data component (see minechem.item.chemical.ChemicalItem).
 */
public abstract class ChemicalBase {
    public static enum Form {
        solid, liquid, gas, plasma
    }

    public final Form form;
    public final String fullName;
    public int colour;

    public ChemicalBase(String fullName, String form, int colour) {
        this.fullName = fullName;
        this.form = Form.valueOf(form);
        this.colour = colour;
    }

    /** Logs the chemical through the mod logger. */
    public abstract void log();

    /** Short representation, e.g. "H" or "H2O". */
    public abstract String getFormula();

    /** True for elements, false for molecules. */
    public abstract boolean isElement();

    public int getColour() {
        return colour;
    }

    public abstract List<String> getToolTip();

    @Override
    public String toString() {
        return getFormula();
    }
}
