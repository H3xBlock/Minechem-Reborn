package minechem.apparatus.synthesis;

import minechem.Config;
import minechem.item.chemical.ChemicalItem;
import minechem.item.chemistjournal.ChemistJournalItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The item access of the synthesis machine from one side. Each index is a live view of the machine's own slot
 * ({@link SlotView}), so what a hopper or a pipe moves is what the machine holds.
 *
 * <p>Rules: from any side but the bottom, elements and molecules go into the storage (never the recipe grid, which no side
 * reaches), and a journal goes into the empty journal slot from any side; the journal can be taken from any side but the
 * bottom.
 *
 * <p>The output is taken from the bottom only. The batch runs inside the
 * transaction through {@link SynthesisBlockEntity#extractOutput}, so a rollback restores the ingredients and the energy. The
 * output index shows the machine's automation output ({@link SynthesisBlockEntity#automationOutput}).
 */
final class SynthesisItemHandler implements ResourceHandler<ItemResource> {
    private final SynthesisBlockEntity machine;
    private final SlotView[] slots;
    private final boolean bottom;

    /**
     * @param slots  the machine's slot views, shared by every side
     * @param bottom whether this is the bottom side, which the output and the storage do not face.
     */
    SynthesisItemHandler(SynthesisBlockEntity machine, SlotView[] slots, boolean bottom) {
        this.machine = machine;
        this.slots = slots;
        this.bottom = bottom;
    }

    /** One machine slot as a transactional handler, read and written in the machine's own list. */
    static final class SlotView extends ItemStackResourceHandler {
        private final SynthesisBlockEntity machine;
        private final NonNullList<ItemStack> items;
        private final int index;

        SlotView(SynthesisBlockEntity machine, NonNullList<ItemStack> items, int index) {
            this.machine = machine;
            this.items = items;
            this.index = index;
        }

        @Override
        protected ItemStack getStack() {
            return items.get(index);
        }

        @Override
        protected void setStack(ItemStack stack) {
            items.set(index, stack);
        }

        @Override
        protected int getCapacity(ItemResource resource) {
            int limit = index == SynthesisBlockEntity.JOURNAL ? 1 : 64;
            return resource.isEmpty() ? limit : Math.min(resource.getMaxStackSize(), limit);
        }

        /** Keeps the machine's own state in step once the transaction is committed: the journal's recipe ghosts, the save. */
        @Override
        protected void onRootCommit(ItemStack original) {
            machine.slotChanged(index);
        }
    }

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index == SynthesisBlockEntity.OUTPUT) {
            return ItemResource.of(machine.automationOutput());
        }
        return slots[index].getResource(0);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index == SynthesisBlockEntity.OUTPUT) {
            return machine.automationOutput().getCount();
        }
        return slots[index].getAmountAsLong(0);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (index == SynthesisBlockEntity.OUTPUT) {
            return 0;
        }
        return resource.isEmpty() || canInsert(index, resource) ? slots[index].getCapacityAsLong(0, resource) : 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return canInsert(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!canInsert(index, resource)) {
            return 0;
        }
        return slots[index].insert(0, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (index == SynthesisBlockEntity.OUTPUT) {
            boolean same = resource.matches(machine.automationOutput());
            return Config.ALLOW_AUTOMATION.get() && bottom && same ? machine.extractOutput(amount, transaction) : 0;
        }
        if (!canExtract(index)) {
            return 0;
        }
        return slots[index].extract(0, resource, amount, transaction);
    }

    private boolean canInsert(int index, ItemResource resource) {
        if (!Config.ALLOW_AUTOMATION.get() || resource.isEmpty()) {
            return false;
        }
        ItemStack stack = resource.toStack();
        if (index == SynthesisBlockEntity.JOURNAL) {
            return machine.getItem(SynthesisBlockEntity.JOURNAL).isEmpty() && stack.getItem() instanceof ChemistJournalItem;
        }
        // The recipe grid (1 to 9) holds ghosts and is never reachable from a side: chemicals go into the storage.
        return !bottom && index >= SynthesisBlockEntity.STORAGE_FIRST && ChemicalItem.getChemicalBase(stack) != null;
    }

    private boolean canExtract(int index) {
        return Config.ALLOW_AUTOMATION.get() && !bottom && index == SynthesisBlockEntity.JOURNAL;
    }
}
