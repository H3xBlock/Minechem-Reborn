package minechem.radiation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The radioactivity of elements (bundled data) and of molecules: the component with the shortest life that is not stable.
 *
 * <p>A molecule's components are taken from its formula (every element symbol in it, in order).
 */
public final class RadiationRules {
    /** The bundled element data, on the classpath. */
    public static final String RESOURCE = "/assets/minechem/data/radioactivity.json";

    private static final Pattern SYMBOL = Pattern.compile("[A-Z][a-z]?");
    private static final Map<String, RadiationEnum> ELEMENTS = load();

    private RadiationRules() {
    }

    /** The radioactivity of an element by its symbol. Unknown symbols are stable. */
    public static RadiationEnum ofElement(String symbol) {
        return ELEMENTS.getOrDefault(symbol, RadiationEnum.stable);
    }

    /**
     * The radioactivity of a molecule from its formula: the non-stable component with the shortest life; on a tie the first one
     * in the formula stays.
     */
    public static RadiationEnum ofFormula(String formula) {
        RadiationEnum result = RadiationEnum.stable;
        Matcher symbols = SYMBOL.matcher(formula);
        while (symbols.find()) {
            RadiationEnum level = ofElement(symbols.group());
            if (level != RadiationEnum.stable && (result == RadiationEnum.stable || result.getLife() > level.getLife())) {
                result = level;
            }
        }
        return result;
    }

    private static Map<String, RadiationEnum> load() {
        try (InputStream stream = RadiationRules.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled data file " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, RadiationEnum> levels = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("elements").entrySet()) {
                levels.put(entry.getKey(), RadiationEnum.valueOf(entry.getValue().getAsString()));
            }
            return Map.copyOf(levels);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }
}
