package minechem.api;

import java.util.ArrayList;
import java.util.List;
import minechem.chemical.ChemicalBase;
import minechem.fluid.reaction.ChemicalFluidReactionHandler;
import minechem.helper.Jenkins;

/**
 * The chemical API, for other mods.
 *
 * <p>A chemical is named as follows: an element by its symbol (for example "H") or its name ("Hydrogen"), a molecule by its
 * name ("Water") or its formula. The names are case-sensitive, and an amount before a name ("2water") is not parsed: a name that is not
 * known makes the call return false.
 */
public final class ChemicalAPI {
    private ChemicalAPI() {
    }

    /**
     * Adds a reaction between two chemicals of a fluid, with the chemicals it produces.
     * If the explosion level is NaN, the reaction does not explode.
     *
     * @param chemicalA one of the chemicals involved in the reaction
     * @param chemicalB another of the chemicals involved in the reaction
     * @param explosionLevel the level of the explosion
     * @param outputChemicals the chemicals of its outputs, for example "Water" or "K"
     * @return whether the reaction was added (false when a name is not known)
     */
    public static boolean registerFluidChemicalReaction(String chemicalA, String chemicalB, float explosionLevel, String... outputChemicals) {
        try {
            List<ChemicalBase> outputs = new ArrayList<>();
            for (String name : outputChemicals) {
                outputs.add(chemical(name));
            }
            ChemicalFluidReactionHandler.addRule(chemical(chemicalA), chemical(chemicalB), explosionLevel, outputs);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // IllegalStateException: the reaction table cannot be built yet (the chemicals are not loaded); nothing was added.
            return false;
        }
    }

    /** The chemical of a name, or an IllegalArgumentException when the name is not known. */
    static ChemicalBase chemical(String name) {
        ChemicalBase chemical = Jenkins.get(name);
        if (chemical == null) {
            throw new IllegalArgumentException("unknown chemical: " + name);
        }
        return chemical;
    }
}
