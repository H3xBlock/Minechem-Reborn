package minechem.apparatus.leadedchest;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Container of the leaded chest: nine chest slots in one row at y 18, and the player inventory at y 50
 * (hotbar at y 108).
 */
public class LeadedChestMenu extends BasicContainer {
    public LeadedChestMenu(int id, Inventory playerInventory, LeadedChestBlockEntity chest) {
        super(MenuRegistry.LEADED_CHEST.get(), id, playerInventory);
        bindPlayerInventory(playerInventory, 50);
        for (int i = 0; i < LeadedChestSlots.SLOTS; i++) {
            addSlot(new Slot(chest, i, 8 + i * 18, 18));
        }
    }
}
