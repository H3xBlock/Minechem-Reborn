package minechem.compat.computercraft;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import minechem.Compendium;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The ComputerCraft part: the chemical turtle upgrade type. It is registered only when computercraft is loaded (see the
 * constructor of Minechem), so no class of this package is loaded otherwise. The upgrade itself is data: data/minechem/computercraft/turtle_upgrade/chemical.json.
 */
public final class ComputerCraftCompat {
    private static final DeferredRegister<UpgradeType<? extends ITurtleUpgrade>> TYPES =
        DeferredRegister.create(ITurtleUpgrade.typeRegistry(), Compendium.Naming.id);

    public static final DeferredHolder<UpgradeType<? extends ITurtleUpgrade>, UpgradeType<ChemicalTurtleUpgrade>> CHEMICAL =
        TYPES.register("chemical", () -> UpgradeType.simpleWithCustomItem(ChemicalTurtleUpgrade::new));

    private ComputerCraftCompat() {
    }

    /** Called from the mod constructor, only when computercraft is loaded. */
    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
