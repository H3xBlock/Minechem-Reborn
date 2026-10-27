package minechem.compat.jei;

import java.util.ArrayList;
import java.util.List;
import minechem.Compendium;
import minechem.Config;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.multiblock.FissionReactorBlockEntity;
import minechem.apparatus.multiblock.FusionReactorBlockEntity;
import minechem.apparatus.multiblock.ReactorRules;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.BlockRegistry;
import minechem.registry.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import minechem.fluid.ChemicalBuckets;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemStack;

/**
 * Shows the machines' recipes in JEI: the Chemical Decomposer (its plain and select recipes), the Chemical Synthesis Machine,
 * and the fission and fusion reactors. The decomposer and synthesis recipes come from DecomposerRecipes and SynthesisRecipes;
 * the reactor recipes come from the reactors' rules (see ReactorRules), listed for every element they apply to. The reactors
 * have no item, so their recipes are catalogued under the blueprint that builds them.
 *
 * <p>A decomposer recipe for a fluid is shown with the fluid's bucket as its input, and a composite recipe (its outputs are
 * other recipes) is not shown. The chance of a recipe is not shown. A select recipe is shown as one recipe per alternative.
 */
@JeiPlugin
public class MinechemJeiPlugin implements IModPlugin {
    public static final RecipeType<DecomposerRecipes.Recipe> DECOMPOSER = new RecipeType<>(id("decomposer"), DecomposerRecipes.Recipe.class);
    public static final RecipeType<SynthesisRecipes.Recipe> SYNTHESIS = new RecipeType<>(id("synthesis"), SynthesisRecipes.Recipe.class);
    public static final RecipeType<FissionRecipe> FISSION = new RecipeType<>(id("fission"), FissionRecipe.class);
    public static final RecipeType<FusionRecipe> FUSION = new RecipeType<>(id("fusion"), FusionRecipe.class);
    public static final RecipeType<BucketRecipe> BUCKET = new RecipeType<>(id("chemical_bucket"), BucketRecipe.class);

    /**
     * A chemical bucket: {@code fill} true is eight vials of the chemical and an empty bucket made into its bucket; false is the
     * bucket emptied into eight vials.
     */
    public record BucketRecipe(ChemicalBase chemical, boolean fill) {
    }

    /** An element split into two of half its atomic number (the split of ReactorRules.halfOf). */
    public record FissionRecipe(int input, int half) {
    }

    /** Two elements fused into one (the fusion of ReactorRules.fusedOf); the inputs are listed smaller first. */
    public record FusionRecipe(int left, int right, int fused) {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Compendium.Naming.id, path);
    }

    @Override
    public Identifier getPluginUid() {
        return id("machines");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new DecomposerCategory(gui), new SynthesisCategory(gui), new FissionCategory(gui),
            new FusionCategory(gui), new BucketCategory(gui));
    }

    /**
     * Every chemical is one item with its chemical name as a component, so JEI must read the component to tell the chemicals apart;
     * without it the 290 chemicals share one identity and JEI reports them as duplicates.
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(ItemRegistry.CHEMICAL.get(), ItemRegistry.CHEMICAL_NAME.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(DECOMPOSER, decomposerRecipes());
        registration.addRecipes(SYNTHESIS, new ArrayList<>(SynthesisRecipes.RECIPES.values()));
        registration.addRecipes(FISSION, fissionRecipes());
        registration.addRecipes(FUSION, fusionRecipes());
        registration.addRecipes(BUCKET, bucketRecipes());
    }

    /** The fill and the empty recipe of every chemical that has a bucket (the chemicals without one, and the empty stacks, are left out). */
    private static List<BucketRecipe> bucketRecipes() {
        List<BucketRecipe> recipes = new ArrayList<>();
        for (ChemicalBase chemical : Jenkins.getAll()) {
            if (!ChemicalBuckets.bucketOf(chemical).isEmpty()) {
                recipes.add(new BucketRecipe(chemical, true));
                recipes.add(new BucketRecipe(chemical, false));
            }
        }
        return recipes;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BlockRegistry.DECOMPOSER_ITEM.get()), DECOMPOSER);
        registration.addRecipeCatalyst(new ItemStack(BlockRegistry.SYNTHESIS_ITEM.get()), SYNTHESIS);
        registration.addRecipeCatalyst(new ItemStack(ItemRegistry.BLUEPRINT_FISSION.get()), FISSION);
        registration.addRecipeCatalyst(new ItemStack(ItemRegistry.BLUEPRINT_FUSION.get()), FUSION);
        registration.addRecipeCatalyst(new ItemStack(Items.BUCKET), BUCKET);
    }

    /** The fissions of every element: the elements run from 1 up to the last one, so the loop stops at the first gap. */
    private static List<FissionRecipe> fissionRecipes() {
        List<FissionRecipe> recipes = new ArrayList<>();
        for (int input = 1; Jenkins.get(input) != null; input++) {
            int half = ReactorRules.halfOf(input);
            if (half > 0) {
                recipes.add(new FissionRecipe(input, half));
            }
        }
        return recipes;
    }

    /** The fusions of every pair of elements, each pair once (the reactor takes the two inputs in either slot). */
    private static List<FusionRecipe> fusionRecipes() {
        List<FusionRecipe> recipes = new ArrayList<>();
        for (int left = 1; Jenkins.get(left) != null; left++) {
            for (int right = left; Jenkins.get(right) != null; right++) {
                int fused = ReactorRules.fusedOf(left, right);
                if (fused > 0) {
                    recipes.add(new FusionRecipe(left, right, fused));
                }
            }
        }
        return recipes;
    }

    /** The colour of the energy text (dark grey, as the other JEI texts). */
    private static final int ENERGY_COLOUR = 0xFF404040;

    /** The energy text of a reactor recipe. */
    static Component energyText(int energy) {
        return Component.literal(energy + " FE");
    }

    /** The stack of an element by its atomic number, with a count. */
    static ItemStack elementStack(int atomicNumber, int count) {
        ItemStack stack = ChemicalItem.stackFor(Jenkins.get(atomicNumber));
        stack.setCount(count);
        return stack;
    }

    /** The decomposer's recipes with outputs, one per alternative of a select recipe. */
    private static List<DecomposerRecipes.Recipe> decomposerRecipes() {
        List<DecomposerRecipes.Recipe> shown = new ArrayList<>();
        for (DecomposerRecipes.Recipe recipe : DecomposerBlockEntity.RECIPES.values()) {
            if (!recipe.components().isEmpty()) {
                continue;
            }
            if (recipe.alternatives().isEmpty()) {
                shown.add(recipe);
            } else {
                for (DecomposerRecipes.Recipe alternative : recipe.alternatives()) {
                    if (alternative.components().isEmpty()) {
                        shown.add(alternative);
                    }
                }
            }
        }
        return shown;
    }

    /**
     * The stack for a recipe key: an item id ("minecraft:stone"), or a molecule or element name (the chemical item).
     * Empty when the key names nothing.
     */
    static ItemStack stackFor(String key, int count) {
        if (key.startsWith("fluid:")) {
            // A fluid input is shown as its bucket.
            Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(key.substring("fluid:".length())));
            Item bucket = fluid == null ? Items.AIR : fluid.getBucket();
            return bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket, count);
        }
        if (key.contains(":")) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(key)), count);
        }
        ChemicalBase chemical = Jenkins.get(key);
        if (chemical == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ChemicalItem.stackFor(chemical);
        stack.setCount(count);
        return stack;
    }

    /** The decomposer: one input, then the outputs of the recipe, in rows of six. */
    static final class DecomposerCategory implements IRecipeCategory<DecomposerRecipes.Recipe> {
        private static final int OUTPUT_COLUMNS = 6;
        private static final int MAX_OUTPUTS = 12;
        private final IGuiHelper gui;

        DecomposerCategory(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<DecomposerRecipes.Recipe> getRecipeType() {
            return DECOMPOSER;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.minechem.chemical_decomposer");
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 42;
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(BlockRegistry.DECOMPOSER_ITEM.get()));
        }

        /** The energy one decomposition needs (0 when power use is disabled, and then nothing is shown). */
        @Override
        public void draw(DecomposerRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            int energy = DecomposerBlockEntity.energyNeeded();
            if (energy > 0) {
                graphics.text(Minecraft.getInstance().font, energyText(energy), 1, 34, ENERGY_COLOUR);
            }
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, DecomposerRecipes.Recipe recipe, IFocusGroup focuses) {
            ItemStack input = stackFor(recipe.input(), 1);
            if (!input.isEmpty()) {
                builder.addInputSlot(1, 13).addItemStack(input);
            }
            int shown = 0;
            for (DecomposerRecipes.Output output : recipe.outputs()) {
                if (shown++ == MAX_OUTPUTS) {
                    break;
                }
                ItemStack stack = ChemicalItem.stackFor(output.chemical());
                stack.setCount(Math.min(output.amount(), 64));
                builder.addOutputSlot(37 + (shown - 1) % OUTPUT_COLUMNS * 18, 1 + (shown - 1) / OUTPUT_COLUMNS * 18)
                    .addItemStack(stack);
            }
        }
    }

    /** The synthesis machine: the 3x3 grid of ingredients (empty cells are skipped), then the output. */
    static final class SynthesisCategory implements IRecipeCategory<SynthesisRecipes.Recipe> {
        private final IGuiHelper gui;

        SynthesisCategory(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<SynthesisRecipes.Recipe> getRecipeType() {
            return SYNTHESIS;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.minechem.synthesis");
        }

        @Override
        public int getWidth() {
            return 120;
        }

        @Override
        public int getHeight() {
            return 56;
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(BlockRegistry.SYNTHESIS_ITEM.get()));
        }

        /** The energy the recipe needs (the cost of its ingredients; nothing is shown when power use is disabled). */
        @Override
        public void draw(SynthesisRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            int energy = Config.powerUseEnabled() ? SynthesisRecipes.energyCost(recipe) : 0;
            if (energy > 0) {
                graphics.text(Minecraft.getInstance().font, energyText(energy), 60, 44, ENERGY_COLOUR);
            }
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SynthesisRecipes.Recipe recipe, IFocusGroup focuses) {
            for (int cell = 0; cell < recipe.ingredients().size() && cell < 9; cell++) {
                SynthesisRecipes.Ingredient ingredient = recipe.ingredients().get(cell);
                if (ingredient == null) {
                    continue;
                }
                ItemStack stack = ChemicalItem.stackFor(ingredient.chemical());
                stack.setCount(Math.min(ingredient.amount(), 64));
                builder.addInputSlot(1 + cell % 3 * 18, 1 + cell / 3 * 18).addItemStack(stack);
            }
            ItemStack output = stackFor(recipe.output(), Math.min(recipe.count(), 64));
            if (!output.isEmpty()) {
                builder.addOutputSlot(92, 19).addItemStack(output);
            }
        }
    }

    /**
     * The chemical buckets, as a crafting grid: four vials, the bucket in the middle of the 3 x 3 grid, then four vials; the
     * output is at the right. A bucket's recipe is any arrangement of its ingredients in the crafting table, so the order
     * shown is only the layout.
     */
    static final class BucketCategory implements IRecipeCategory<BucketRecipe> {
        private static final int GRID = 3;
        private static final int CENTRE = 4;
        private static final int VIALS = 8;
        /** The output slot, right of the grid: the crafting table's output column (the grid is 54 pixels wide, plus a gap). */
        private static final int OUTPUT_X = 84;
        private final IGuiHelper gui;

        BucketCategory(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<BucketRecipe> getRecipeType() {
            return BUCKET;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("item.minecraft.bucket");
        }

        @Override
        public int getWidth() {
            return 103;
        }

        @Override
        public int getHeight() {
            return GRID * 18;
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(Items.BUCKET));
        }

        /** Grid cell of a slot index 0 to 8 (row by row), at the crafting table's 18-pixel steps. */
        private static int cellX(int index) {
            return 1 + index % GRID * 18;
        }

        private static int cellY(int index) {
            return 1 + index / GRID * 18;
        }

        /**
         * Fill: the eight vials around an empty bucket, the chemical's bucket at the right. Empty: the bucket in the centre,
         * the eight vials at the right as one stack, as a crafting-table recipe with one output slot.
         */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BucketRecipe recipe, IFocusGroup focuses) {
            ItemStack vial = ChemicalItem.stackFor(recipe.chemical());
            ItemStack bucket = ChemicalBuckets.bucketOf(recipe.chemical());
            ItemStack emptyBucket = new ItemStack(Items.BUCKET);
            if (recipe.fill()) {
                for (int index = 0; index < GRID * GRID; index++) {
                    if (index == CENTRE) {
                        builder.addInputSlot(cellX(index), cellY(index)).addItemStack(emptyBucket);
                    } else {
                        builder.addInputSlot(cellX(index), cellY(index)).addItemStack(vial.copy());
                    }
                }
                builder.addOutputSlot(OUTPUT_X, cellY(CENTRE)).addItemStack(bucket);
            } else {
                builder.addInputSlot(cellX(CENTRE), cellY(CENTRE)).addItemStack(bucket);
                ItemStack vials = vial.copy();
                vials.setCount(VIALS);
                builder.addOutputSlot(OUTPUT_X, cellY(CENTRE)).addItemStack(vials);
            }
        }
    }

    /** The fission reactor: one element in, two of half its atomic number out. */
    static final class FissionCategory implements IRecipeCategory<FissionRecipe> {
        private final IGuiHelper gui;

        FissionCategory(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<FissionRecipe> getRecipeType() {
            return FISSION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.minechem.fission_reactor");
        }

        @Override
        public int getWidth() {
            return 80;
        }

        @Override
        public int getHeight() {
            return 36;
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(ItemRegistry.BLUEPRINT_FISSION.get()));
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FissionRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 4).addItemStack(elementStack(recipe.input(), 1));
            builder.addOutputSlot(56, 4).addItemStack(elementStack(recipe.half(), 2));
        }

        /** The energy one split needs (the reactor's energyNeeded: atomic number times FISSION_MULTIPLIER). */
        @Override
        public void draw(FissionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            graphics.text(Minecraft.getInstance().font, energyText(recipe.input() * FissionReactorBlockEntity.FISSION_MULTIPLIER),
                1, 26, ENERGY_COLOUR);
        }
    }

    /** The fusion reactor: two elements in, the element of their summed atomic number out. */
    static final class FusionCategory implements IRecipeCategory<FusionRecipe> {
        private final IGuiHelper gui;

        FusionCategory(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<FusionRecipe> getRecipeType() {
            return FUSION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.minechem.fusion_reactor");
        }

        @Override
        public int getWidth() {
            return 100;
        }

        @Override
        public int getHeight() {
            return 36;
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(ItemRegistry.BLUEPRINT_FUSION.get()));
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FusionRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 4).addItemStack(elementStack(recipe.left(), 1));
            builder.addInputSlot(24, 4).addItemStack(elementStack(recipe.right(), 1));
            builder.addOutputSlot(68, 4).addItemStack(elementStack(recipe.fused(), 1));
        }

        /** The energy one fusion needs (the reactor's energyNeeded: the fused number times FUSION_MULTIPLIER). */
        @Override
        public void draw(FusionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            graphics.text(Minecraft.getInstance().font, energyText(recipe.fused() * FusionReactorBlockEntity.FUSION_MULTIPLIER),
                1, 26, ENERGY_COLOUR);
        }
    }
}
