package minechem.item.chemistjournal;

import java.util.ArrayList;
import java.util.List;

/**
 * The tooltip lines of the chemist journal: "Active: <item>" when an item is
 * selected, then "Owner: <name>" when the journal has an owner tag. A journal that has never been opened has no owner tag,
 * so it shows no owner line.
 */
public final class ChemistJournalTooltip {
    private ChemistJournalTooltip() {
    }

    /**
     * @param active the active item's name, or null/empty when none is selected
     * @param owner  the owner's name, or null when the journal was never opened
     */
    public static List<String> lines(String active, String owner) {
        List<String> lines = new ArrayList<>();
        if (active != null && !active.isEmpty()) {
            lines.add("Active: " + active);
        }
        if (owner != null) {
            lines.add("Owner: " + owner);
        }
        return lines;
    }
}
