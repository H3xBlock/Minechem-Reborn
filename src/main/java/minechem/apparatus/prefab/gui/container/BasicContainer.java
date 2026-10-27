package minechem.apparatus.prefab.gui.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Player inventory slots come first (0..35: hotbar and main), then the apparatus slots.
 */
public abstract class BasicContainer extends AbstractContainerMenu {
    protected BasicContainer(MenuType<?> type, int id, Inventory playerInventory) {
        super(type, id);
    }

    protected void bindPlayerInventory(Inventory inventory) {
        bindPlayerInventory(inventory, 84);
    }

    /** Binds the player's inventory with its top row at {@code y}; the hotbar is 58 below the main rows. */
    protected void bindPlayerInventory(Inventory inventory, int y) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, y + 58));
        }
    }

    /** The menu stays valid for any player. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Shift-click. Player slots move into the apparatus slots, apparatus slots move into the player inventory.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack stack = original.copy();
        int playerSlots = Inventory.INVENTORY_SIZE;
        if (index < playerSlots) {
            if (!moveItemStackTo(original, playerSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, playerSlots, true)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack;
    }
}
