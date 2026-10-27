package minechem.apparatus.multiblock;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A blueprint of one reactor. The fusion blueprint and the fission blueprint are two items
 * ({@code blueprint_fusion} and {@code blueprint_fission}).
 *
 * <p>Dungeon loot and librarian trades for blueprints are not implemented.
 */
public class BlueprintItem extends Item {
    private final ReactorBlueprint blueprint;

    public BlueprintItem(Properties properties, ReactorBlueprint blueprint) {
        super(properties);
        this.blueprint = blueprint;
    }

    public ReactorBlueprint blueprint() {
        return blueprint;
    }

    /** The blueprint a stack holds, or null when it is not a blueprint. */
    public static ReactorBlueprint blueprintOf(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem item ? item.blueprint : null;
    }
}
