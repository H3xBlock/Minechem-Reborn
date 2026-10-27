package minechem.chemical;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import minechem.helper.LogHelper;
import minechem.registry.ElementRegistry;
import minechem.registry.MoleculeRegistry;

/**
 * Data object for molecules.
 */
public class Molecule extends ChemicalBase {
    public ChemicalStructure structure;

    /**
     * Parses a String to a Molecule.
     *
     * @param colour int colour; 0 (transparent) makes the colour come from the structure
     * @return can throw an IllegalArgumentException
     */
    public static Molecule parseMolecule(String fullName, String form, int colour, String formula) {
        return parseMolecule(fullName, form, colour, colour, formula);
    }

    /**
     * A molecule has two colours: the first tints the liquid layer, the second the molecule layer.
     */
    public static Molecule parseMolecule(String fullName, String form, int colour, int colour2, String formula) {
        return new Molecule(fullName, form, colour, colour2, parseStructure(formula));
    }

    private final int colour2;

    private Molecule(String fullName, String form, int colour, int colour2, ChemicalStructure structure) {
        super(fullName, form, colour);
        this.colour2 = colour2;
        this.structure = structure;
    }

    /** The second colour; equal to {@link #getColour()} when the data gives none. */
    public int getColour2() {
        return colour2;
    }

    @Override
    public void log() {
        LogHelper.debug("Molecule name: " + this.fullName);
        LogHelper.debug("Molecule structure: " + this.getFormula());
        LogHelper.debug("Form: " + this.form);
    }

    @Override
    public boolean isElement() {
        return false;
    }

    @Override
    public int getColour() {
        int colour = super.getColour();
        return colour == minechem.Compendium.Color.TrueColor.transparent ? structure.getColour() : colour;
    }

    @Override
    public String getFormula() {
        return structure.getFormula();
    }

    @Override
    public List<String> getToolTip() {
        List<String> list = new LinkedList<String>();
        list.add("Form: " + this.form);
        list.add("Formula: " + this.getFormula());
        return list;
    }

    private static int toInteger(String string) {
        if (isNull(string)) {
            return 1;
        }
        return Integer.valueOf(string);
    }

    private static Element toElement(String string) {
        return ElementRegistry.getInstance().getElement(string);
    }

    private static Molecule toMolecule(String string, boolean name) {
        return name ? MoleculeRegistry.getInstance().getMoleculeByName(string) : MoleculeRegistry.getInstance().getMoleculeByFormula(string);
    }

    private static boolean isNull(String string) {
        return string == null || string.isEmpty();
    }

    private static boolean put(ChemicalStructure structure, ChemicalBase chemical, int multiplier) {
        if (chemical == null) {
            return false;
        }
        structure.add(chemical, multiplier);
        return true;
    }

    /**
     * Pattern for detecting molecules from strings (equation safe). Group 1: number of molecules; Group 2: element symbol;
     * Group 3: molecule grouping without brackets; Group 4: @name shorthand; Group 5: multiplier.
     *
     * @author Hilburn
     */
    public static Pattern molecule = Pattern.compile("(\\d*)(?:([A-Z][a-z]{0,2})|(?:\\((\\w+)\\))|(?:@([^\\d\\s]+)))(\\d*)");

    private static ChemicalStructure parseStructure(String formula) {
        if (isNull(formula)) {
            throw new NullPointerException("Can't parse null or empty formula");
        }
        ChemicalStructure structure = new ChemicalStructure();
        Matcher matcher = molecule.matcher(formula);
        while (matcher.find()) {
            int multiplier = toInteger(matcher.group(5));
            Element element = toElement(matcher.group(2));
            if (put(structure, element, multiplier)) {
                continue;
            }
            Molecule chemMolecule = toMolecule(parseStructure(matcher.group(3)).getFormula(), false);
            if (put(structure, chemMolecule, multiplier)) {
                continue;
            }
            Molecule definedMolecule = toMolecule(matcher.group(4), true);
            if (put(structure, definedMolecule, multiplier)) {
                continue;
            }
            throw new IllegalArgumentException("Error parsing " + formula);
        }
        return structure;
    }
}
