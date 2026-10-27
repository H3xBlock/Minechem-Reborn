package minechem.apparatus.decomposer;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container of the decomposer. The input slot sits at (80, 16) and accepts only items with a recipe.
 * The 18 output slots sit in two rows of 9 at y 62 and 80, and accept any item. The player inventory starts at y 102.
 */
public class DecomposerMenu extends BasicContainer {
    /** The button id of the dump button. */
    public static final int DUMP_BUTTON = 0;

    private final DecomposerBlockEntity decomposer;

    public DecomposerMenu(int id, Inventory playerInventory, DecomposerBlockEntity decomposer) {
        super(MenuRegistry.DECOMPOSER.get(), id, playerInventory);
        this.decomposer = decomposer;
        bindPlayerInventory(playerInventory, 102);
        addSlot(new InputSlot(decomposer, DecomposerBlockEntity.INPUT_SLOT, 80, 16));
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = DecomposerMachine.FIRST_OUTPUT + row * 9 + column;
                addSlot(new OutputSlot(decomposer, slot, 8 + column * 18, 62 + row * 18));
            }
        }
    }

    /** The dump button: clicked on the server, it empties the fluid tank. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != DUMP_BUTTON) {
            return false;
        }
        decomposer.dumpFluid();
        return true;
    }

    public DecomposerBlockEntity getDecomposer() {
        return decomposer;
    }

    /** Input slot: accepts an item only when the decomposer has a recipe for it. */
    private static class InputSlot extends Slot {
        InputSlot(DecomposerBlockEntity container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return DecomposerBlockEntity.acceptsItem(stack);
        }
    }

    /** Output slot: any item may be placed here. */
    private static class OutputSlot extends Slot {
        OutputSlot(DecomposerBlockEntity container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
}
