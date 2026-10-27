package minechem.helper;

import com.google.gson.JsonObject;
import net.neoforged.fml.ModList;

/**
 * The optional mod of a data entry: an entry with "requires": "modid" is used only when that mod is loaded (the recipes of Applied
 * Energistics 2 and Ender IO, for example). An entry without the key is always used.
 */
public final class ModRequirement {
    private ModRequirement() {
    }

    /** No mod list outside a running loader (the unit tests): no optional mod is loaded then. */
    public static boolean satisfied(JsonObject entry) {
        if (!entry.has("requires")) {
            return true;
        }
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(entry.get("requires").getAsString());
    }
}
