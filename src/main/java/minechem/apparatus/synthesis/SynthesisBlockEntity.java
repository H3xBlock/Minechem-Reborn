package minechem.apparatus.synthesis;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.apparatus.synthesis.SynthesisRecipes.Ingredient;
import minechem.apparatus.synthesis.SynthesisRecipes.Recipe;
import minechem.apparatus.synthesis.SynthesisStorage.Stack;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.item.chemistjournal.ChemistJournalData;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The chemical synthesis machine. Slots: 0 the output, 1 to 9
 * the recipe grid ("ghost" slots that show a journal recipe, and that the player may also fill with ghosts: see
 * SynthesisMenu.clicked; they never hold real items and never drop), 10 to 27 the storage
 * (elements and molecules, two rows of nine in the GUI), and 28 the chemist's journal.
 *
 * <p>Storage covers slots 10 to 28, so slot 28 is both the last storage slot and the journal. Only chemical stacks are ever
 * taken from storage, so the journal in slot 28 is never consumed.
 *
 * <p>Energy: a synthesis costs {@link SynthesisRecipes#energyCost} and uses it when its ingredients are taken, all or nothing.
 */
public class SynthesisBlockEntity extends BaseTileEntity implements net.minecraft.world.WorldlyContainer, minechem.radiation.IRadiationShield, minechem.radiation.INoDecay {
    @Override
    public float radiationReductionFactor() {
        return 0.4F;
    }

    public static final int OUTPUT = 0;
    public static final int RECIPE_FIRST = 1;
    public static final int RECIPE_SLOTS = 9;
    public static final int STORAGE_FIRST = 10;
    public static final int STORAGE_SLOTS = SynthesisStorage.STORAGE_SLOTS;
    public static final int JOURNAL = 28;
    public static final int SIZE = 29;

    /** Maximum stored energy. */
    public static final int ENERGY_CAPACITY = 100_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY);

    /** The recipe of the current grid; null when the grid matches nothing. */
    private Recipe currentRecipe;
    /** Whether the grid matches a recipe (see hasRecipeRunning); saved, so the update packet carries it. */
    private boolean recipeRunning;
    /** Count of crafts made (each takeInputStacks); saved, so the client sees each craft and plays the arm for it. */
    private int crafts;
    /** Client only: the craft count last seen, and the renders the arm still has to move for the last craft. */
    private int seenCrafts = -1;
    private int craftArmRenders;
    /** One craft starts a full arm cycle: 200 steps of 0.5 each (SynthesisModel.updateArm), one step per render. */
    public static final int CRAFT_ARM_RENDERS = 400;

    /**
     * Client: whether the arm moves for a craft. Each new craft starts a full cycle, whatever the craft's time. The first
     * count seen (on loading) starts none.
     */
    public boolean advanceCraftArm() {
        if (seenCrafts == -1) {
            seenCrafts = crafts;
        } else if (crafts != seenCrafts) {
            seenCrafts = crafts;
            craftArmRenders = CRAFT_ARM_RENDERS;
        }
        if (craftArmRenders > 0) {
            craftArmRenders--;
            return true;
        }
        return false;
    }
    /** The grid as it was when the recipe was last looked up. */
    private List<ItemStack> oldGrid = emptyGrid();
    private boolean recipeLoaded;
    private int lastSyncedEnergy = -1;
    /** The game time of the last energy sync (see tick). */
    private long lastEnergySync = Long.MIN_VALUE / 2;
    /** At most one energy-only update packet per this many ticks. */
    static final int ENERGY_SYNC_TICKS = 10;

    public SynthesisBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SYNTHESIS.get(), pos, state);
    }

    /** What an automated output extraction changed, so a rolled-back transaction can undo it (items and energy). */
    private record OutputSnapshot(List<ItemStack> items, int energy) {
    }

    private final SnapshotJournal<OutputSnapshot> outputJournal = new SnapshotJournal<>() {
        @Override
        protected OutputSnapshot createSnapshot() {
            List<ItemStack> copy = new ArrayList<>(items.size());
            for (ItemStack stack : items) {
                copy.add(stack.copy());
            }
            return new OutputSnapshot(copy, energyStored());
        }

        @Override
        protected void revertToSnapshot(OutputSnapshot snapshot) {
            for (int i = 0; i < snapshot.items().size(); i++) {
                items.set(i, snapshot.items().get(i));
            }
            energy.set(snapshot.energy());
        }

        @Override
        protected void onRootCommit(OutputSnapshot snapshot) {
            setChanged();
        }
    };

    /**
     * Automation takes the output from the bottom: the batch takes its
     * ingredients and energy, and the whole output stack leaves. Inside a transaction, a rollback restores the items and the
     * energy. Returns how many items were taken (0 when the output cannot be taken, or {@code amount} is too small).
     */
    public int extractOutput(int amount, TransactionContext transaction) {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty() || amount < output.getCount() || !canTakeOutputStack(false)) {
            return 0;
        }
        outputJournal.updateSnapshots(transaction);
        if (!takeInputStacks()) {
            return 0;
        }
        int count = output.getCount();
        items.set(OUTPUT, ItemStack.EMPTY);
        return count;
    }

    /** What automation sees in the output slot (see {@link SynthesisItemHandler}). */
    ItemStack automationOutput() {
        return items.get(OUTPUT);
    }

    /** The item handlers by side: the six directions, then the one for no side (index 6). */
    private final SynthesisItemHandler[] itemHandlers = new SynthesisItemHandler[7];
    /**
     * One live view per slot, shared by the item handlers of every side. The views read and write the machine's own list: an
     * ItemStacksResourceHandler would copy the list, and what a hopper moved would never reach the machine.
     */
    private SynthesisItemHandler.SlotView[] slotViews;

    /** The item access from one side (see {@link SynthesisItemHandler}); null is the machine's own access. */
    public SynthesisItemHandler itemHandler(net.minecraft.core.Direction side) {
        if (slotViews == null) {
            slotViews = new SynthesisItemHandler.SlotView[SIZE];
            for (int slot = 0; slot < SIZE; slot++) {
                slotViews[slot] = new SynthesisItemHandler.SlotView(this, items, slot);
            }
        }
        int index = side == null ? 6 : side.ordinal();
        if (itemHandlers[index] == null) {
            itemHandlers[index] = new SynthesisItemHandler(this, slotViews, side == net.minecraft.core.Direction.DOWN);
        }
        return itemHandlers[index];
    }

    /** Called by the item handler after a slot changed: the journal's recipe ghosts follow it, and the state is saved. */
    void slotChanged(int slot) {
        if (slot == JOURNAL) {
            if (items.get(JOURNAL).isEmpty()) {
                clearRecipeMatrix();
            } else {
                onPutJournal(items.get(JOURNAL));
            }
        }
        setChanged();
    }

    /** The energy storage behind the NeoForge energy capability. */
    public SimpleEnergyHandler getEnergy() {
        return energy;
    }

    public int energyStored() {
        return energy.getAmountAsInt();
    }

    // ---- Container ------------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == JOURNAL) {
            clearRecipeMatrix();
        }
        if (items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (slot == OUTPUT) {
            return takeOutput(amount);
        }
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    /**
     * Taking the output goes through the output's own rule, and putting a journal into
     * slot 28 loads the active recipe into the grid.
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == OUTPUT && !items.get(OUTPUT).isEmpty()) {
            if (stack.isEmpty()) {
                removeItem(OUTPUT, 1);
                return;
            }
            if (items.get(OUTPUT).getItem() == stack.getItem()) {
                removeItem(OUTPUT, stack.getCount());
                return;
            }
        }
        items.set(slot, stack);
        setChanged();
        if (slot == JOURNAL && !stack.isEmpty()) {
            onPutJournal(stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == OUTPUT) {
            return false;
        }
        if (slot == JOURNAL) {
            return ChemistJournalData.acceptsInJournalSlot(stack.getItem() instanceof ChemistJournalItem);
        }
        return true;
    }

    // ---- Sides (WorldlyContainer) ---------------------------------------------------------------------------------------
    // A vanilla hopper treats a block entity that is a Container as a container and never asks for the item capability, so
    // the side rules must be on the container itself. Without them a hopper reached every slot: it filled the recipe grid
    // (whose ghosts are then deleted) and pulled ghosts and storage out from below.

    private static final int[] OUTPUT_SIDE_SLOTS = {OUTPUT, JOURNAL};
    private static final int[] STORAGE_SIDE_SLOTS = java.util.stream.IntStream.rangeClosed(STORAGE_FIRST, JOURNAL).toArray();

    /**
     * Any side but the top sees the output and the journal while a batch can be made, and the storage (10 to 28) otherwise;
     * the top always sees the storage. The recipe grid is never seen.
     */
    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        if (side != net.minecraft.core.Direction.UP && takeStacksFromStorage(false)) {
            return OUTPUT_SIDE_SLOTS;
        }
        return STORAGE_SIDE_SLOTS;
    }

    /**
     * Elements and molecules are inserted from any side but the bottom, into a slot past the output, when automation is
     * allowed; a journal into the empty journal slot from any side. Slot 28 takes journals only (canPlaceItem).
     */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        if (!minechem.Config.ALLOW_AUTOMATION.get() || stack.isEmpty()) {
            return false;
        }
        if (slot == JOURNAL) {
            return items.get(JOURNAL).isEmpty() && stack.getItem() instanceof ChemistJournalItem;
        }
        return slot >= STORAGE_FIRST && side != net.minecraft.core.Direction.DOWN && ChemicalItem.getChemicalBase(stack) != null;
    }

    /**
     * The output is taken from the bottom when it can be made, and the journal from any other side, when automation is
     * allowed.
     */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        if (!minechem.Config.ALLOW_AUTOMATION.get()) {
            return false;
        }
        return (side == net.minecraft.core.Direction.DOWN && slot == OUTPUT && canTakeOutputStack(false))
            || (side != net.minecraft.core.Direction.DOWN && slot == JOURNAL);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    /**
     * Breaking the block: only the storage and the journal drop. The output and the recipe grid hold ghosts and never drop.
     * This replaces the vanilla BlockEntity.preRemoveSideEffects, which drops every slot of a Container.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) {
            return;
        }
        for (int slot = STORAGE_FIRST; slot <= JOURNAL; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    // ---- Recipe grid ----------------------------------------------------------------------------------------------

    /** The grid's stacks are removed (they are only ghosts of the recipe). */
    public void clearRecipeMatrix() {
        for (int slot = RECIPE_FIRST; slot < RECIPE_FIRST + RECIPE_SLOTS; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
    }

    /** The recipe's ingredients go into the grid, shaped in place, shapeless in order. */
    public void setRecipe(Recipe recipe) {
        clearRecipeMatrix();
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(RECIPE_SLOTS, ingredients.size()); i++) {
            items.set(RECIPE_FIRST + i, stackOf(ingredients.get(i)));
        }
    }

    /** The journal's active item, if it has a recipe, is loaded into the grid. */
    private void onPutJournal(ItemStack journal) {
        String active = ChemistJournalItem.activeName(journal);
        if (active == null) {
            return;
        }
        Recipe recipe = SynthesisRecipes.RECIPES.get(active);
        if (recipe != null) {
            setRecipe(recipe);
        }
    }

    private List<ItemStack> grid() {
        return new ArrayList<>(items.subList(RECIPE_FIRST, RECIPE_FIRST + RECIPE_SLOTS));
    }

    private static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < RECIPE_SLOTS; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }

    /** The first recipe (in table order) that matches the grid sets the output and the recipe. */
    private boolean getRecipeResult() {
        Recipe recipe = findRecipe(grid());
        if (recipe != null) {
            items.set(OUTPUT, outputStack(recipe));
            currentRecipe = recipe;
            return true;
        }
        items.set(OUTPUT, ItemStack.EMPTY);
        currentRecipe = null;
        return false;
    }

    /** The recipe is looked up again only when the grid changed. */
    private void updateRecipe() {
        List<ItemStack> grid = grid();
        for (int i = 0; i < RECIPE_SLOTS; i++) {
            if (!ItemStack.matches(grid.get(i), oldGrid.get(i))) {
                oldGrid = grid;
                getRecipeResult();
                return;
            }
        }
    }

    private static Recipe findRecipe(List<ItemStack> grid) {
        List<Stack<ChemicalBase>> stacks = new ArrayList<>();
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) {
                stacks.add(null);
                continue;
            }
            ChemicalBase chemical = chemicalOf(stack);
            if (chemical == null) {
                return null;
            }
            stacks.add(new Stack<>(chemical, stack.getCount()));
        }
        SynthesisStorage<ChemicalBase> rules = new SynthesisStorage<>(Objects::equals);
        for (Recipe recipe : SynthesisRecipes.RECIPES.values()) {
            List<Stack<ChemicalBase>> ingredients = recipeStacks(recipe.ingredients(), true);
            boolean matches = recipe.shaped()
                ? rules.matchesShaped(stacks, ingredients)
                : rules.matchesShapeless(withoutEmpty(stacks), recipeStacks(recipe.ingredients(), false));
            if (matches) {
                return recipe;
            }
        }
        return null;
    }

    private static List<Stack<ChemicalBase>> withoutEmpty(List<Stack<ChemicalBase>> stacks) {
        List<Stack<ChemicalBase>> result = new ArrayList<>();
        for (Stack<ChemicalBase> stack : stacks) {
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    /** The ingredients as stacks. {@code keepEmpty} keeps a null entry for each empty shaped slot. */
    private static List<Stack<ChemicalBase>> recipeStacks(List<Ingredient> ingredients, boolean keepEmpty) {
        List<Stack<ChemicalBase>> stacks = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) {
                if (keepEmpty) {
                    stacks.add(null);
                }
            } else {
                stacks.add(new Stack<>(ingredient.chemical(), ingredient.amount()));
            }
        }
        return stacks;
    }

    // ---- Storage, output and energy -------------------------------------------------------------------------------

    /** The storage as the pure rules see it: chemical stacks by slot, other stacks (the journal) as empty. */
    private SynthesisStorage<ChemicalBase> loadStorage() {
        SynthesisStorage<ChemicalBase> storage = new SynthesisStorage<>(Objects::equals);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = items.get(STORAGE_FIRST + i);
            ChemicalBase chemical = chemicalOf(stack);
            if (chemical != null) {
                storage.setSlot(i, new Stack<>(chemical, stack.getCount()));
            }
        }
        return storage;
    }

    /** Writes the storage back. Non-chemical stacks keep their slot. */
    private void writeStorage(SynthesisStorage<ChemicalBase> storage) {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            int slot = STORAGE_FIRST + i;
            if (chemicalOf(items.get(slot)) == null && !items.get(slot).isEmpty()) {
                continue;
            }
            Stack<ChemicalBase> stack = storage.slot(i);
            items.set(slot, stack == null ? ItemStack.EMPTY : ChemicalItem.stackFor(stack.item()).copyWithCount(stack.count()));
        }
        setChanged();
    }

    /** The recipe's ingredients are taken from the storage, and the energy is used. */
    public boolean takeStacksFromStorage(boolean doTake) {
        if (currentRecipe == null || !hasEnoughPowerForCurrentRecipe()) {
            return false;
        }
        SynthesisStorage<ChemicalBase> storage = loadStorage();
        if (!storage.take(recipeStacks(currentRecipe.ingredients(), false), doTake)) {
            return false;
        }
        if (doTake) {
            writeStorage(storage);
            useEnergy(SynthesisRecipes.energyCost(currentRecipe));
        }
        return true;
    }

    /** The output is present, the energy is enough, and the ingredients can be taken. */
    public boolean canTakeOutputStack(boolean doTake) {
        return !items.get(OUTPUT).isEmpty() && hasEnoughPowerForCurrentRecipe() && takeStacksFromStorage(doTake);
    }

    /** One batch of ingredients, taken only when they can all be taken. */
    private boolean takeInputStacks() {
        if (takeStacksFromStorage(false)) {
            takeStacksFromStorage(true);
            crafts++;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return true;
        }
        return false;
    }

    /**
     * Each batch takes the ingredients once. The returned stack keeps the output's count, and a short batch returns what
     * was made so far.
     */
    private ItemStack takeOutput(int amount) {
        int toRemove = amount;
        ItemStack result = items.get(OUTPUT).copy();
        while (toRemove > 0) {
            if (takeInputStacks()) {
                toRemove -= amount;
            } else {
                result.setCount(amount - toRemove);
                return result;
            }
            if (toRemove < 1) {
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Used by the shift-click craft: makes batches of the current recipe until {@code amount} is reached or the ingredients
     * or energy run out, and returns the output stacks. Each batch takes its ingredients from storage. The loop counts the
     * batches against the amount.
     */
    public List<ItemStack> craftOutput(int amount) {
        List<ItemStack> outputs = new java.util.ArrayList<>();
        if (currentRecipe == null) {
            return outputs;
        }
        ItemStack template = outputStack(currentRecipe);
        outputs.add(template.copyWithCount(0));
        int took = 0;
        while (canTakeOutputStack(false) && amount > took && takeInputStacks()) {
            took++;
            ItemStack output = outputs.get(outputs.size() - 1);
            int limit = output.getMaxStackSize();
            if (output.getCount() + template.getCount() > limit) {
                int leftOver = template.getCount() - (limit - output.getCount());
                output.setCount(limit);
                if (leftOver > 0) {
                    outputs.add(template.copyWithCount(leftOver));
                }
            } else {
                output.grow(template.getCount());
            }
            amount -= template.getCount();
        }
        return outputs;
    }

    /**
     * Whether the machine is active: the grid matches a recipe, the power is enough, and the storage holds the recipe's
     * ingredients (takeStacksFromStorage, without taking them). Kept as its own flag, saved and sent to the client
     * (currentRecipe is not), so the renderer can read it there.
     */
    public boolean hasRecipeRunning() {
        return recipeRunning;
    }

    /** Always true when power use is disabled (Config.POWER_USE_ENABLED). */
    public boolean hasEnoughPowerForCurrentRecipe() {
        return !minechem.Config.powerUseEnabled() || currentRecipe == null || canAffordRecipe(currentRecipe);
    }

    public boolean canAffordRecipe(Recipe recipe) {
        return energyStored() >= SynthesisRecipes.energyCost(recipe);
    }

    /** Uses the energy all or nothing; nothing when power use is disabled. */
    private void useEnergy(int amount) {
        if (!minechem.Config.powerUseEnabled()) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (energy.extract(amount, transaction) == amount) {
                transaction.commit();
            }
        }
    }

    /**
     * The energy the current recipe needs, 0 without one or when power use is disabled.
     */
    public int energyNeeded() {
        return currentRecipe == null || !minechem.Config.powerUseEnabled() ? 0 : SynthesisRecipes.energyCost(currentRecipe);
    }

    /** The recipe the grid matches, or null. Read by the ComputerCraft turtle's readRecipe. */
    public Recipe getCurrentRecipe() {
        return currentRecipe;
    }

    /** The state line of the GUI. */
    public String getState() {
        return SynthesisStorage.state(canTakeOutputStack(false), !items.get(OUTPUT).isEmpty(), hasEnoughPowerForCurrentRecipe());
    }

    // ---- Ticking and sync -----------------------------------------------------------------------------------------

    /** The server tick: the output is refilled, and the recipe follows the grid. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SynthesisBlockEntity machine) {
        machine.tick();
    }

    private void tick() {
        if (!recipeLoaded) {
            recipeLoaded = true;
            getRecipeResult();
        }
        int stored = energyStored();
        // The stored energy is sent to the client for the state tab. The update packet is the full save, so an energy change
        // alone is sent at most every ENERGY_SYNC_TICKS (a cable fills the store every tick); the last value always follows.
        if (stored != lastSyncedEnergy) {
            setChanged();
            if (level.getGameTime() - lastEnergySync >= ENERGY_SYNC_TICKS) {
                lastSyncedEnergy = stored;
                lastEnergySync = level.getGameTime();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        if (currentRecipe != null && items.get(OUTPUT).isEmpty()) {
            items.set(OUTPUT, outputStack(currentRecipe));
        } else {
            updateRecipe();
        }
        // The client reads hasRecipeRunning to animate the arm. A change of that flag is sent here, since the rest of the
        // block is not sent with each change.
        // The output slot is not a product: tick refills it from the recipe while the grid matches, so it is never empty. The
        // machine is active while the storage holds the recipe's ingredients (and the power is enough).
        boolean running = takeStacksFromStorage(false);
        if (running != recipeRunning) {
            recipeRunning = running;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The output stack of a recipe: the item or chemical, with the recipe's count. */
    static ItemStack outputStack(Recipe recipe) {
        ChemicalBase chemical = Jenkins.get(recipe.output());
        ItemStack stack;
        if (SynthesisRecipes.WATER_BOTTLE.equals(recipe.output())) {
            // The water bottle: a potion of water in 26.1.
            stack = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        } else if (chemical != null) {
            stack = ChemicalItem.stackFor(chemical);
        } else {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(recipe.output()));
            stack = new ItemStack(item);
        }
        return stack.copyWithCount(recipe.count());
    }

    private static ItemStack stackOf(Ingredient ingredient) {
        if (ingredient == null) {
            return ItemStack.EMPTY;
        }
        return ChemicalItem.stackFor(ingredient.chemical()).copyWithCount(ingredient.amount());
    }

    /** The chemical of a stack, or null for an empty stack or an item that is not a chemical. */
    private static ChemicalBase chemicalOf(ItemStack stack) {
        return stack.isEmpty() ? null : ChemicalItem.getChemicalBase(stack);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // ---- Save -----------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.store("recipeRunning", Codec.BOOL, recipeRunning);
        output.store("crafts", Codec.INT, crafts);
        energy.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> saved = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(saved.size(), items.size()); i++) {
            items.set(i, saved.get(i));
        }
        recipeRunning = input.read("recipeRunning", Codec.BOOL).orElse(false);
        crafts = input.read("crafts", Codec.INT).orElse(0);
        energy.deserialize(input);
    }
}
