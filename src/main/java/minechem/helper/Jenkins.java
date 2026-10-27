package minechem.helper;

import java.util.ArrayList;
import java.util.List;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.registry.ElementRegistry;
import minechem.registry.MoleculeRegistry;

/**
 * Chemical lookup by atomic number, symbol, name or formula.
 */
public class Jenkins {
    public static Element get(int atomicNumber) {
        return ElementRegistry.getInstance().getElement(atomicNumber);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ChemicalBase> T get(String s) {
        ChemicalBase chemicalBase = ElementRegistry.getInstance().getElement(s);
        if (chemicalBase == null) {
            chemicalBase = ElementRegistry.getInstance().getElementByName(s);
        }
        if (chemicalBase == null) {
            chemicalBase = MoleculeRegistry.getInstance().getMoleculeByFormula(s);
        }
        if (chemicalBase == null) {
            chemicalBase = MoleculeRegistry.getInstance().getMoleculeByName(s);
        }
        return (T) chemicalBase;
    }

    public static List<? extends ChemicalBase> getAll() {
        List<ChemicalBase> all = new ArrayList<ChemicalBase>();
        all.addAll(ElementRegistry.getInstance().getElements());
        all.addAll(MoleculeRegistry.getInstance().getMolecules());
        return all;
    }
}
