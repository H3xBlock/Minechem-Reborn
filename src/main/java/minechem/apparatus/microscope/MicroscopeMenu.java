package minechem.apparatus.microscope;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container of the microscope: the input at (44, 45), the journal at (80, 95) (journals only, one per slot), the
 * 3 by 3 output grid at (98, 27) that refuses every item, and the player inventory at y 135 (hotbar at y 193).
 */
public class MicroscopeMenu extends BasicContainer {
    private final MicroscopeBlockEntity microscope;

    public MicroscopeMenu(int id, Inventory playerInventory, MicroscopeBlockEntity microscope) {
        super(MenuRegistry.MICROSCOPE.get(), id, playerInventory);
        this.microscope = microscope;
        bindPlayerInventory(playerInventory, 135);
        addSlot(new Slot(microscope, MicroscopeRules.INPUT_SLOT, 44, 45));
        addSlot(new Slot(microscope, MicroscopeRules.JOURNAL_SLOT, 80, 95) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MicroscopeRules.canPlace(MicroscopeRules.JOURNAL_SLOT, stack.getItem() instanceof ChemistJournalItem);
            }

            @Override
            public int getMaxStackSize() {
                return MicroscopeRules.STACK_LIMIT;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = MicroscopeRules.FIRST_OUTPUT_SLOT + row * 3 + col;
                addSlot(new Slot(microscope, slot, 98 + col * 18, 27 + row * 18) {
                    /** The output slots refuse every item. */
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
    }

    /** The microscope behind the menu (the screen reads the input slot for its overlay). */
    public MicroscopeBlockEntity microscope() {
        return microscope;
    }
}
