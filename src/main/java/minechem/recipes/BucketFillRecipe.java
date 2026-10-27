package minechem.recipes;

import minechem.chemical.ChemicalBase;
import minechem.fluid.ChemicalBuckets;
import minechem.item.chemical.ChemicalItem;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationState;
import minechem.radiation.RadiationTicker;
import minechem.registry.ItemRegistry;
import minechem.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * An empty bucket and eight vials of one chemical make that chemical's bucket. The bucket of a radioactive chemical keeps the
 * oldest decay start of the vials.
 *
 * <p>The decay state of such a bucket names the overworld as its dimension (minecraft:overworld).
 */
public class BucketFillRecipe extends CustomRecipe {
    public static final BucketFillRecipe INSTANCE = new BucketFillRecipe();
    /** The vials that fill one bucket: eight. */
    static final int VIALS = 8;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return chemicalToFill(input) != null;
    }

    /** The chemical of a matching grid, or null: one empty bucket and eight vials of one chemical, nothing else. */
    static ChemicalBase chemicalToFill(CraftingInput input) {
        boolean hasBucket = false;
        int vials = 0;
        ChemicalBase type = null;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.is(Items.BUCKET)) {
                    if (hasBucket) {
                        return null;
                    }
                    hasBucket = true;
                    continue;
                }
                if (!stack.is(ItemRegistry.CHEMICAL.get())) {
                    return null;
                }
                ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
                if (chemical == null) {
                    return null;
                }
                if (type == null) {
                    type = chemical;
                } else if (!type.fullName.equals(chemical.fullName)) {
                    return null;
                }
                vials++;
            }
        }
        if (!hasBucket || vials != VIALS || type == null || ChemicalBuckets.bucketOf(type).isEmpty()) {
            return null;
        }
        return type;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ChemicalBase type = chemicalToFill(input);
        ItemStack bucket = ChemicalBuckets.bucketOf(type);
        if (RadiationTicker.levelOf(type) == RadiationEnum.stable) {
            return bucket;
        }
        RadiationState oldest = null;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                RadiationState state = input.getItem(x, y).get(ItemRegistry.RADIATION.get());
                if (state != null && (oldest == null || state.decayStarted() < oldest.decayStarted())) {
                    oldest = state;
                }
            }
        }
        if (oldest != null) {
            bucket.set(ItemRegistry.RADIATION.get(),
                new RadiationState(oldest.decayStarted(), oldest.decayStarted(), "minecraft:overworld"));
        }
        return bucket;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return RecipeRegistry.BUCKET_FILL.get();
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
