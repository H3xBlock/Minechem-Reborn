package minechem.item.chemistjournal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The table of elements puts every element in one cell, in its period and group (the standard layout). */
class TableOfElementsLayoutTest {
    @Test
    void knownElementsSitInTheirCells() {
        assertArrayEquals(new int[] {1, 1}, TableOfElementsScreen.slotOf(1), "hydrogen");
        assertArrayEquals(new int[] {1, 18}, TableOfElementsScreen.slotOf(2), "helium");
        assertArrayEquals(new int[] {4, 8}, TableOfElementsScreen.slotOf(26), "iron");
        assertArrayEquals(new int[] {6, 3}, TableOfElementsScreen.slotOf(57), "lanthanum");
        assertArrayEquals(new int[] {8, 3}, TableOfElementsScreen.slotOf(58), "cerium");
        assertArrayEquals(new int[] {8, 16}, TableOfElementsScreen.slotOf(71), "lutetium");
        assertArrayEquals(new int[] {7, 3}, TableOfElementsScreen.slotOf(89), "actinium");
        assertArrayEquals(new int[] {9, 3}, TableOfElementsScreen.slotOf(90), "thorium");
        assertArrayEquals(new int[] {9, 16}, TableOfElementsScreen.slotOf(103), "lawrencium");
        assertArrayEquals(new int[] {7, 13}, TableOfElementsScreen.slotOf(113), "element 113");
        assertArrayEquals(new int[] {7, 14}, TableOfElementsScreen.slotOf(114), "element 114");
        assertArrayEquals(new int[] {7, 18}, TableOfElementsScreen.slotOf(118), "element 118");
    }

    @Test
    void everyElementHasItsOwnCell() {
        Set<String> cells = new HashSet<>();
        for (int z = 1; z <= 118; z++) {
            int[] slot = TableOfElementsScreen.slotOf(z);
            assertEquals(true, slot[1] >= 1 && slot[1] <= 18, "column of " + z);
            assertEquals(true, slot[0] >= 1 && slot[0] <= 9, "row of " + z);
            assertEquals(true, cells.add(slot[0] + ":" + slot[1]), "cell of " + z + " is free");
        }
        assertEquals(118, cells.size());
    }
}
