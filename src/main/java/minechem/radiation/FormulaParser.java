package minechem.radiation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads a molecule's formula into the number of atoms of each element (for example "Ca(OH)2" gives Ca 1, O 2, H 2). The decay of
 * a molecule uses these counts.
 */
public final class FormulaParser {

    private FormulaParser() {
    }

    /** The atoms of each element symbol in the formula, in the order they first appear, parentheses expanded. */
    public static Map<String, Integer> parse(String formula) {
        Deque<Map<String, Integer>> groups = new ArrayDeque<>();
        groups.push(new LinkedHashMap<>());
        int i = 0;
        while (i < formula.length()) {
            char c = formula.charAt(i);
            if (c == '(') {
                groups.push(new LinkedHashMap<>());
                i++;
            } else if (c == ')') {
                i++;
                int multiplier = readNumber(formula, i);
                i = skipNumber(formula, i);
                Map<String, Integer> group = groups.pop();
                for (Map.Entry<String, Integer> atoms : group.entrySet()) {
                    groups.peek().merge(atoms.getKey(), atoms.getValue() * multiplier, Integer::sum);
                }
            } else if (Character.isUpperCase(c)) {
                int start = i;
                i++;
                while (i < formula.length() && Character.isLowerCase(formula.charAt(i))) {
                    i++;
                }
                String symbol = formula.substring(start, i);
                int count = readNumber(formula, i);
                i = skipNumber(formula, i);
                groups.peek().merge(symbol, count, Integer::sum);
            } else {
                i++;
            }
        }
        return groups.pop();
    }

    /** The number written at {@code from}, or 1 when there is none. */
    private static int readNumber(String formula, int from) {
        int end = skipNumber(formula, from);
        return end == from ? 1 : Integer.parseInt(formula.substring(from, end));
    }

    private static int skipNumber(String formula, int from) {
        int end = from;
        while (end < formula.length() && Character.isDigit(formula.charAt(end))) {
            end++;
        }
        return end;
    }
}
