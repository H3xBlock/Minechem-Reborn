package minechem.fluid;

import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.chemical.Molecule;
import minechem.helper.Jenkins;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * The buckets of the elements and molecules: the bucket of a chemical and the chemical of a bucket. The vanilla water bucket
 * is the bucket of water (the water molecule is "Water" in the data).
 */
public final class ChemicalBuckets {
    private static final String WATER = "Water";

    private ChemicalBuckets() {
    }

    /** One bucket of a chemical, or the empty stack when the chemical has no bucket (no fluid, or not a chemical). */
    public static ItemStack bucketOf(ChemicalBase chemical) {
        if (chemical == null) {
            return ItemStack.EMPTY;
        }
        if (chemical instanceof Molecule && WATER.equals(chemical.fullName)) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        if (chemical instanceof Element element) {
            for (ElementFluids.Entry entry : ElementFluids.entries()) {
                if (entry.symbol().equals(element.shortName)) {
                    return new ItemStack(entry.bucket().get());
                }
            }
        } else if (chemical instanceof Molecule) {
            for (MoleculeFluids.Entry entry : MoleculeFluids.entries()) {
                if (entry.molecule().equals(chemical.fullName)) {
                    return new ItemStack(entry.bucket().get());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** The chemical a bucket holds, or null for an empty stack or any item that is not one of the chemical buckets. */
    public static ChemicalBase chemicalOfBucket(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            return Jenkins.get(WATER);
        }
        if (!(stack.getItem() instanceof BucketItem)) {
            return null;
        }
        for (MoleculeFluids.Entry entry : MoleculeFluids.entries()) {
            if (entry.bucket().get() == stack.getItem()) {
                return Jenkins.get(entry.molecule());
            }
        }
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            if (entry.bucket().get() == stack.getItem()) {
                return elementBySymbol(entry.symbol());
            }
        }
        return null;
    }

    static Element elementBySymbol(String symbol) {
        for (ChemicalBase chemical : Jenkins.getAll()) {
            if (chemical instanceof Element element && element.shortName.equals(symbol)) {
                return element;
            }
        }
        return null;
    }
}
