package minechem.compat.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import minechem.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The chemical turtle: a peripheral upgrade whose name is "Chemical", or "Jenkins" with the
 * advancedTurtleAI setting. Its crafting item is the atomic manipulator, given by the datapack file.
 */
public final class ChemicalTurtleUpgrade extends AbstractTurtleUpgrade {
    public ChemicalTurtleUpgrade(ItemStackTemplate craftingItem) {
        super(TurtleUpgradeType.PERIPHERAL, adjective(), craftingItem);
    }

    private static Component adjective() {
        return Component.translatable(Config.advancedTurtleAI() ? "turtle_upgrade.minechem.jenkins" : "turtle_upgrade.minechem.chemical");
    }

    @Override
    public UpgradeType<? extends ITurtleUpgrade> getType() {
        return ComputerCraftCompat.CHEMICAL.value();
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new ChemicalTurtlePeripheral(turtle);
    }

    /** Every tick, the peripheral of this side decays the turtle's radioactive stacks. */
    @Override
    public void update(ITurtleAccess turtle, TurtleSide side) {
        if (turtle.getPeripheral(side) instanceof ChemicalTurtlePeripheral peripheral) {
            peripheral.tick(turtle.getLevel());
        }
    }
}
