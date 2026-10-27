package minechem.registry;

import com.mojang.serialization.MapCodec;
import minechem.Compendium;
import minechem.item.chemistjournal.ChemistJournalCloneRecipe;
import minechem.recipes.BucketEmptyRecipe;
import minechem.recipes.BucketFillRecipe;
import minechem.recipes.ElementIngredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe serializers for the custom recipes: the journal cloning recipe, and the bucket fill and empty recipes, with the
 * element ingredient type.
 */
public class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Compendium.Naming.id);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChemistJournalCloneRecipe>> CLONE_JOURNAL = SERIALIZERS.register(
        "clone_journal",
        () -> new RecipeSerializer<>(MapCodec.unit(ChemistJournalCloneRecipe.INSTANCE), StreamCodec.unit(ChemistJournalCloneRecipe.INSTANCE)));

    /** The bucket of a chemical from eight vials. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BucketFillRecipe>> BUCKET_FILL = SERIALIZERS.register(
        "bucket_fill",
        () -> new RecipeSerializer<>(MapCodec.unit(BucketFillRecipe.INSTANCE), StreamCodec.unit(BucketFillRecipe.INSTANCE)));

    /** The eight vials of a chemical from its bucket. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BucketEmptyRecipe>> BUCKET_EMPTY = SERIALIZERS.register(
        "bucket_empty",
        () -> new RecipeSerializer<>(MapCodec.unit(BucketEmptyRecipe.INSTANCE), StreamCodec.unit(BucketEmptyRecipe.INSTANCE)));

    /** The element ingredient, read as {@code minechem:element} in JSON. */
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Compendium.Naming.id);

    public static final DeferredHolder<IngredientType<?>, IngredientType<ElementIngredient>> ELEMENT = INGREDIENT_TYPES.register(
        "element", () -> ElementIngredient.TYPE);
}
