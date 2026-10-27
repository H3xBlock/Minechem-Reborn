package minechem.polytool;

import minechem.item.chemical.ChemicalItem;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One infusion slot at the top left and the player's inventory. Stacks of 64 elements that have an upgrade are infused into the
 * held polytool when they are put in the slot: the element is used up and its power is added to the tool.
 */
public class PolytoolMenu extends AbstractContainerMenu {
    /** The stack an element needs to be infused: a full stack of 64. */
    static final int INFUSION_STACK = 64;

    private final Inventory playerInventory;
    private final Container infusion;

    public PolytoolMenu(int id, Inventory playerInventory) {
        super(MenuRegistry.POLYTOOL.get(), id);
        this.playerInventory = playerInventory;
        this.infusion = new SimpleContainer(1) {
            @Override
            public void setItem(int slot, ItemStack stack) {
                if (isInfusion(stack) && infuse(stack)) {
                    super.setItem(slot, ItemStack.EMPTY);
                    return;
                }
                super.setItem(slot, stack);
            }
        };
        addSlot(new Slot(infusion, 0, 8, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isInfusion(stack) || stack.isEmpty();
            }
        });
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(playerInventory, x, 8 + 18 * x, 64 + 130));
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + 18 * x, 64 + 72 + y * 18));
            }
        }
    }

    /** Whether a stack is an infusable element: a full stack of an element that has a polytool upgrade. */
    static boolean isInfusion(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != INFUSION_STACK) {
            return false;
        }
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        return chemical instanceof Element element && PolytoolHelper.getTypeFromElement(element.atomicNumber, 1) != null;
    }

    /** Adds the element's power to the held polytool when the alloy keeps the tool valid. */
    private boolean infuse(ItemStack element) {
        ItemStack tool = playerInventory.getSelectedItem();
        if (!(tool.getItem() instanceof PolytoolItem)) {
            return false;
        }
        Element chemical = (Element) ChemicalItem.getChemicalBase(element);
        if (!PolytoolItem.validAlloyInfusion(tool, chemical.atomicNumber)) {
            return false;
        }
        PolytoolItem.addPower(tool, chemical.atomicNumber, 1);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Shift-click: infusable elements go to the infusion slot, the rest between the two inventory parts. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isInfusion(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 10) {
            if (!moveItemStackTo(stack, 10, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 10, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
