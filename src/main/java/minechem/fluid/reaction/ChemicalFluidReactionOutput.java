package minechem.fluid.reaction;

import java.util.List;
import minechem.chemical.ChemicalBase;

/**
 * What a reaction makes: the products, in order, and the power of the explosion the reaction makes. A NaN power means no
 * explosion.
 */
public record ChemicalFluidReactionOutput(List<ChemicalBase> outputs, float explosionLevel) {
}
