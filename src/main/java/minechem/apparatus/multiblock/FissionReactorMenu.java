package minechem.apparatus.multiblock;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container of the fission chamber: the input at (80, 16), the output at (80, 62), and the player inventory at y 84. The
 * chamber opens only while its structure is complete.
 */
public class FissionReactorMenu extends BasicContainer {
    private final FissionReactorBlockEntity fission;
    private int syncedEnergy;

    public FissionReactorMenu(int id, Inventory playerInventory, FissionReactorBlockEntity fission) {
        super(MenuRegistry.FISSION_REACTOR.get(), id, playerInventory);
        this.fission = fission;
        bindPlayerInventory(playerInventory, 84);
        addSlot(new InputSlot(fission, FissionReactorBlockEntity.INPUT, 80, 16));
        addSlot(new OutputSlot(fission, FissionReactorBlockEntity.OUTPUT, 80, 62));
        // The client's block entity never gets the energy, so the stored amount is sent as a data slot.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return fission.getEnergy().getAmountAsInt();
            }

            @Override
            public void set(int value) {
                syncedEnergy = value;
            }
        });
    }

    /** The energy stored in the chamber, as the server last sent it (the screen reads this on the client). */
    public int storedEnergy() {
        return syncedEnergy;
    }

    public FissionReactorBlockEntity getFission() {
        return fission;
    }

    @Override
    public boolean stillValid(Player player) {
        return fission.isComplete();
    }

    /** The input takes elements only. */
    private static final class InputSlot extends Slot {
        private final int slot;
        private final FissionReactorBlockEntity fission;

        InputSlot(FissionReactorBlockEntity fission, int slot, int x, int y) {
            super(fission, slot, x, y);
            this.fission = fission;
            this.slot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fission.canPlaceItem(slot, stack);
        }
    }

    /** The output takes nothing; items only come out of it. */
    private static final class OutputSlot extends Slot {
        OutputSlot(FissionReactorBlockEntity fission, int slot, int x, int y) {
            super(fission, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
