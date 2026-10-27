package minechem.apparatus.multiblock;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container of the fusion chamber: the left input at (22, 62), the right input at (138, 62) and the output at (80, 62),
 * the player inventory at y 105. The chamber opens only while its structure is complete.
 */
public class FusionReactorMenu extends BasicContainer {
    private final FusionReactorBlockEntity fusion;
    private int syncedEnergy;

    public FusionReactorMenu(int id, Inventory playerInventory, FusionReactorBlockEntity fusion) {
        super(MenuRegistry.FUSION_REACTOR.get(), id, playerInventory);
        this.fusion = fusion;
        bindPlayerInventory(playerInventory, 105);
        addSlot(new ElementSlot(fusion, FusionReactorBlockEntity.INPUT_LEFT, 22, 62));
        addSlot(new ElementSlot(fusion, FusionReactorBlockEntity.INPUT_RIGHT, 138, 62));
        addSlot(new OutputSlot(fusion, FusionReactorBlockEntity.OUTPUT, 80, 62));
        // The client's block entity never gets the energy, so the stored amount is sent as a data slot.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return fusion.getEnergy().getAmountAsInt();
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

    public FusionReactorBlockEntity getFusion() {
        return fusion;
    }

    @Override
    public boolean stillValid(Player player) {
        return fusion.isComplete();
    }

    /** An input slot takes elements only. */
    private static final class ElementSlot extends Slot {
        private final FusionReactorBlockEntity fusion;
        private final int slot;

        ElementSlot(FusionReactorBlockEntity fusion, int slot, int x, int y) {
            super(fusion, slot, x, y);
            this.fusion = fusion;
            this.slot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fusion.canPlaceItem(slot, stack);
        }
    }

    /** The output slot takes nothing; items only come out of it. */
    private static final class OutputSlot extends Slot {
        OutputSlot(FusionReactorBlockEntity fusion, int slot, int x, int y) {
            super(fusion, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
