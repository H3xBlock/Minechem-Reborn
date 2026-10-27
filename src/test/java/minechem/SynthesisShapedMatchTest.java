package minechem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import minechem.apparatus.synthesis.SynthesisStorage;
import minechem.apparatus.synthesis.SynthesisStorage.Stack;
import org.junit.jupiter.api.Test;

/** The shaped grid rule of the synthesis machine. */
public class SynthesisShapedMatchTest {

    private static final SynthesisStorage<String> RULES = new SynthesisStorage<>(Objects::equals);

    private static List<Stack<String>> grid(String... items) {
        List<Stack<String>> grid = new ArrayList<>();
        for (String item : items) {
            grid.add(item == null ? null : new Stack<>(item.split(":")[0], Integer.parseInt(item.split(":")[1])));
        }
        return grid;
    }

    @Test
    public void anExactGridMatches() {
        List<Stack<String>> recipe = grid("H:2", null, "O:1");
        assertTrue(RULES.matchesShaped(grid("H:2", null, "O:1", null, null, null, null, null, null), recipe));
    }

    @Test
    public void theSameIngredientsOneSlotAwayDoNotMatch() {
        List<Stack<String>> recipe = grid("H:2", null, "O:1");
        assertFalse(RULES.matchesShaped(grid(null, "H:2", "O:1", null, null, null, null, null, null), recipe));
    }

    @Test
    public void aCountMismatchDoesNotMatch() {
        List<Stack<String>> recipe = grid("H:2", null, "O:1");
        assertFalse(RULES.matchesShaped(grid("H:1", null, "O:1", null, null, null, null, null, null), recipe));
    }

    @Test
    public void aFilledSlotWhereTheRecipeIsEmptyDoesNotMatch() {
        List<Stack<String>> recipe = grid("H:2", null, "O:1");
        assertFalse(RULES.matchesShaped(grid("H:2", "C:1", "O:1", null, null, null, null, null, null), recipe));
    }

    @Test
    public void anEmptyGridMatchesAnEmptyRecipe() {
        assertTrue(RULES.matchesShaped(grid(null, null, null), grid(null, null, null)));
    }
}
