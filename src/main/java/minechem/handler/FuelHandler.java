package minechem.handler;

import java.util.Map;
import minechem.chemical.ChemicalBase;
import minechem.item.chemical.ChemicalItem;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/**
 * The fuel values of the chemicals. The burn times are in ticks (coal is 1600 in vanilla). Each fuel is keyed by the
 * chemical's full name, since its items carry the name in a data component.
 *
 * Buckets are not given a fuel value here.
 */
public final class FuelHandler {
    private static final Map<String, Integer> BURN_TIMES = Map.ofEntries(
        Map.entry("Carbon", 200),
        Map.entry("Hydrogen", 100),
        Map.entry("Sulfur", 300),
        Map.entry("Phosphorus", 250),
        Map.entry("Cellulose", 65),
        Map.entry("Methyl Alcohol", 500),
        Map.entry("Ethyl Alcohol", 1100),
        Map.entry("Allyl Alcohol", 800),
        Map.entry("Propane", 1400),
        Map.entry("Toluene", 2200),
        Map.entry("Trinitrotoluene", 4000),
        Map.entry("Isoprene", 1800),
        Map.entry("Butene", 1600),
        Map.entry("Methyl Methacrylate", 5700));

    private FuelHandler() {
    }

    /** Sets the burn time of a chemical item used as furnace fuel. Other items keep the vanilla burn time. */
    public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(event.getItemStack());
        if (chemical == null) {
            return;
        }
        Integer burnTime = BURN_TIMES.get(chemical.fullName);
        if (burnTime != null) {
            event.setBurnTime(burnTime);
        }
    }
}
