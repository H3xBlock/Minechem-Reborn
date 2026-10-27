package minechem.apparatus.microscope;

import java.util.List;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of the microscope: the input (slot 0), the journal (slot 1), and the nine output slots, which stay
 * empty. Putting an item under the microscope, or a journal next to an item, inspects the item: when a journal is in
 * slot 1 and the item has a recipe, the item is written into the journal.
 */
public class MicroscopeBlockEntity extends BaseTileEntity implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(MicroscopeRules.SLOTS, ItemStack.EMPTY);

    public MicroscopeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MICROSCOPE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return MicroscopeRules.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /**
     * The whole slot content is taken, whatever amount was asked for.
     */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
        if (level != null && !level.isClientSide()) {
            if (slot == MicroscopeRules.INPUT_SLOT && !stack.isEmpty()) {
                inspect(stack);
            }
            if (slot == MicroscopeRules.JOURNAL_SLOT && !stack.isEmpty() && !items.get(MicroscopeRules.INPUT_SLOT).isEmpty()) {
                inspect(items.get(MicroscopeRules.INPUT_SLOT));
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return MicroscopeRules.STACK_LIMIT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return MicroscopeRules.canPlace(slot, stack.getItem() instanceof ChemistJournalItem);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    /** Writes the item into the journal when a journal is present and the item has a recipe. */
    void inspect(ItemStack inspected) {
        ItemStack journal = items.get(MicroscopeRules.JOURNAL_SLOT);
        String key = DecomposerBlockEntity.keyFor(inspected);
        boolean hasRecipe = SynthesisRecipes.RECIPES.get(key) != null
            || DecomposerBlockEntity.RECIPES.get(key) != null;
        if (MicroscopeRules.inspectionAddsToJournal(!journal.isEmpty(), hasRecipe)) {
            ChemistJournalItem.addDiscovered(journal, key);
            // The products of its decomposition are discovered too, so a fermented spider eye finds polonium.
            for (String product : DecomposerBlockEntity.RECIPES.productKeys(key)) {
                ChemistJournalItem.addDiscovered(journal, product);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> saved = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(saved.size(), items.size()); i++) {
            items.set(i, saved.get(i));
        }
    }
}
