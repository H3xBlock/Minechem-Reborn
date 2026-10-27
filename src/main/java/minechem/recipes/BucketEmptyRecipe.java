package minechem.recipes;

import minechem.chemical.ChemicalBase;
import minechem.fluid.ChemicalBuckets;
import minechem.item.chemical.ChemicalItem;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationState;
import minechem.radiation.RadiationTicker;
import minechem.registry.ItemRegistry;
import minechem.registry.RecipeRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One bucket of a chemical, and nothing else, makes eight vials of that chemical (the bucket goes back as the empty bucket, as the
 * craft remainder of the bucket). A radioactive chemical keeps the decay state of its bucket.
 *
 * <p>The water bucket always gives water. No option changes that.
 */
public class BucketEmptyRecipe extends CustomRecipe {
    public static final BucketEmptyRecipe INSTANCE = new BucketEmptyRecipe();
    /** The vials one bucket gives back: eight. */
    static final int VIALS = 8;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return chemicalInBucket(input) != null;
    }

    /** The chemical of the one bucket in a matching grid, or null: exactly one chemical bucket and nothing else. */
    static ChemicalBase chemicalInBucket(CraftingInput input) {
        int buckets = 0;
        ChemicalBase type = null;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.isEmpty()) {
                    continue;
                }
                ChemicalBase chemical = ChemicalBuckets.chemicalOfBucket(stack);
                if (chemical == null) {
                    return null;
                }
                type = chemical;
                buckets++;
            }
        }
        return buckets == 1 ? type : null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack bucket = findBucket(input);
        ItemStack vials = ChemicalItem.stackFor(ChemicalBuckets.chemicalOfBucket(bucket));
        vials.setCount(VIALS);
        if (RadiationTicker.levelOf(ChemicalBuckets.chemicalOfBucket(bucket)) != RadiationEnum.stable) {
            RadiationState state = bucket.get(ItemRegistry.RADIATION.get());
            if (state != null) {
                vials.set(ItemRegistry.RADIATION.get(), state);
            }
        }
        return vials;
    }

    private static ItemStack findBucket(CraftingInput input) {
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return RecipeRegistry.BUCKET_EMPTY.get();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}
