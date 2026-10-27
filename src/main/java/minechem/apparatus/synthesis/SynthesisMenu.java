package minechem.apparatus.synthesis;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.item.chemical.ChemicalItem;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The synthesis machine's menu: the output at (134, 18), which only gives its item when the machine can make it; the 3 by 3
 * recipe grid at (62, 18), ghost slots that copy any stack and never give one (see {@link #clicked}); the storage, two rows
 * of nine at (8, 84), elements and molecules only; the journal at (26, 36), journals only and one per slot; and the player
 * inventory at y 123 (hotbar at y 181).
 *
 * <p>Shift-click is handled in quickMoveStack.
 */
public class SynthesisMenu extends BasicContainer {
    private final SynthesisBlockEntity machine;

    public SynthesisMenu(int id, Inventory playerInventory, SynthesisBlockEntity machine) {
        super(MenuRegistry.SYNTHESIS.get(), id, playerInventory);
        this.machine = machine;
        bindPlayerInventory(playerInventory, 123);
        addSlot(new Slot(machine, SynthesisBlockEntity.OUTPUT, 134, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return machine.canTakeOutputStack(false);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = SynthesisBlockEntity.RECIPE_FIRST + row * 3 + col;
                addSlot(new GhostSlot(machine, slot, 62 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = SynthesisBlockEntity.STORAGE_FIRST + row * 9 + col;
                addSlot(new Slot(machine, slot, 8 + col * 18, 84 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return ChemicalItem.getChemicalBase(stack) != null;
                    }
                });
            }
        }
        addSlot(new Slot(machine, SynthesisBlockEntity.JOURNAL, 26, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChemistJournalItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    /**
     * A recipe grid slot: it holds a ghost of a stack, never a real item. The player cannot take from it, and clicks on it
     * go through {@link #clicked}.
     */
    static final class GhostSlot extends Slot {
        GhostSlot(SynthesisBlockEntity machine, int slot, int x, int y) {
            super(machine, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Menu slot indices: the player's 36 slots come first, then the machine's slots in the order they were added. */
    private static final int PLAYER_END = 36;
    private static final int OUTPUT_INDEX = 36;
    /** The recipe grid (ghost slots): menu indices 37 to 45. */
    public static final int GRID_START = 37;
    public static final int GRID_END = 46;
    /** The cap of a ghost stack. */
    private static final int GHOST_STACK_LIMIT = 64;

    /**
     * A click on a recipe grid slot copies the stack on the cursor into it (the whole stack with the left button, one item
     * with any other button) or, with an empty cursor, clears it. The cursor keeps its stack, and the grid never gives an
     * item to the player. A ghost already in the slot only grows by the amount, whatever the cursor's item, up to 64.
     *
     * <p>A drag (QUICK_CRAFT) goes through the vanilla code, which skips the grid ({@link #canDragTo}), so the drag state
     * stays consistent. The ghost count is also capped by the item's own stack size, so the saved stack stays valid.
     */
    @Override
    public void clicked(int slotIndex, int buttonNum, net.minecraft.world.inventory.ContainerInput containerInput, Player player) {
        if (slotIndex >= GRID_START && slotIndex < GRID_END
            && containerInput != net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT) {
            Slot slot = slots.get(slotIndex);
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && slot.mayPlace(carried)) {
                int amount = buttonNum == 0 ? carried.getCount() : 1;
                ItemStack inSlot = slot.getItem();
                if (!inSlot.isEmpty()) {
                    slot.set(inSlot.copyWithCount(Math.min(inSlot.getCount() + amount, Math.min(GHOST_STACK_LIMIT, inSlot.getMaxStackSize()))));
                } else {
                    slot.set(carried.copyWithCount(Math.min(amount, Math.min(GHOST_STACK_LIMIT, carried.getMaxStackSize()))));
                }
            } else {
                slot.set(ItemStack.EMPTY);
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** The ghost grid takes no part in drags. */
    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof GhostSlot) && super.canDragTo(slot);
    }
    private static final int STORAGE_START = 46;
    private static final int STORAGE_END = 64;
    private static final int JOURNAL_INDEX = 64;

    /**
     * Shift-clicking the output crafts as much as the player's inventory has room for, and the outputs go into the player's
     * inventory. The room is counted over the player's slots: an empty slot holds a
     * full stack of the output, a slot with the same item holds what is missing to a full stack.
     */
    private void craftMaximum() {
        ItemStack output = machine.getItem(SynthesisBlockEntity.OUTPUT);
        if (output.isEmpty()) {
            return;
        }
        int amount = 0;
        for (int slot = 0; slot < PLAYER_END; slot++) {
            ItemStack stack = slots.get(slot).getItem();
            if (stack.isEmpty()) {
                amount += output.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, output)) {
                amount += output.getMaxStackSize() - stack.getCount();
            }
        }
        for (ItemStack made : machine.craftOutput(amount)) {
            if (!made.isEmpty()) {
                moveItemStackTo(made, 0, PLAYER_END, true);
            }
        }
    }

    /**
     * Shift-click. A chemical from the player goes into the storage, a
     * journal into the journal slot when it is empty, and storage or journal stacks go back into the player's inventory.
     * Shift-clicking the output crafts the maximum (craftMaximum).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == OUTPUT_INDEX) {
            craftMaximum();
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < PLAYER_END) {
            if (ChemicalItem.getChemicalBase(original) != null) {
                moveItemStackTo(original, STORAGE_START, STORAGE_END, false);
            } else if (original.getItem() instanceof ChemistJournalItem && !slots.get(JOURNAL_INDEX).hasItem()) {
                moveItemStackTo(original, JOURNAL_INDEX, JOURNAL_INDEX + 1, false);
            } else {
                return ItemStack.EMPTY;
            }
        } else if ((index >= STORAGE_START && index < STORAGE_END) || index == JOURNAL_INDEX) {
            moveItemStackTo(original, 0, PLAYER_END, true);
        } else {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        return copy;
    }

    /** The machine behind the menu: the client's copy is kept in step by the block entity's update packet. */
    public SynthesisBlockEntity machine() {
        return machine;
    }
}
