package minechem.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FormulaParserTest {

    @Test
    void countsEachElementOfAPlainFormula() {
        assertEquals(Map.of("H", 2, "O", 1), FormulaParser.parse("H2O"));
        assertEquals(Map.of("C", 6, "H", 12, "O", 6), FormulaParser.parse("C6H12O6"));
    }

    @Test
    void expandsParentheses() {
        assertEquals(Map.of("Ca", 1, "O", 2, "H", 2), FormulaParser.parse("Ca(OH)2"));
    }

    @Test
    void sumsRepeatedSymbols() {
        assertEquals(Map.of("N", 2, "O", 4), FormulaParser.parse("N2O4"));
        assertEquals(Map.of("H", 2, "O", 2), FormulaParser.parse("HOOH"));
    }

    @Test
    void readsTwoLetterSymbols() {
        assertEquals(Map.of("Tc", 1, "Po", 1), FormulaParser.parse("TcPo"));
    }
}
