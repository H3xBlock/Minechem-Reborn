package minechem.apparatus.multiblock;

import minechem.apparatus.prefab.gui.container.BasicContainer;
import minechem.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Container of the blueprint projector: the blueprint slot at (25, 47), which takes blueprints only, and the
 * player inventory at y 84.
 */
public class BlueprintProjectorMenu extends BasicContainer {
    private final BlueprintProjectorBlockEntity projector;

    public BlueprintProjectorMenu(int id, Inventory playerInventory, BlueprintProjectorBlockEntity projector) {
        super(MenuRegistry.BLUEPRINT_PROJECTOR.get(), id, playerInventory);
        this.projector = projector;
        bindPlayerInventory(playerInventory, 84);
        addSlot(new Slot(projector, 0, 25, 47) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return projector.canPlaceItem(0, stack);
            }
        });
    }

    /** The blueprint in the projector's slot, or null when there is none (the screen shows its contents). */
    public ReactorBlueprint blueprint() {
        return projector.blueprint();
    }

    public net.minecraft.world.item.ItemStack blueprintStack() {
        return projector.getItem(0);
    }
}
