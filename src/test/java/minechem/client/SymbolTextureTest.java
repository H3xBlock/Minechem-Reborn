package minechem.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Checks the element symbol models: each one points to a texture file that exists, and there is one per element. */
class SymbolTextureTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minechem");

    @Test
    void everySymbolModelHasItsTexture() throws IOException {
        List<Path> models;
        try (Stream<Path> files = Files.list(ASSETS.resolve("models/item/symbol"))) {
            models = files.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
        }
        assertTrue(models.size() >= 118, "expected one symbol model per element, found " + models.size());
        for (Path model : models) {
            JsonObject json = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : json.getAsJsonObject("textures").entrySet()) {
                String texture = entry.getValue().getAsString().replace("minechem:", "");
                Path file = ASSETS.resolve("textures/" + texture + ".png");
                assertTrue(Files.isRegularFile(file), model.getFileName() + " points to a missing texture: " + file);
                assertEquals(16, readWidth(file), file + " should be 16 pixels wide");
            }
        }
    }

    /** The PNG header stores the width in bytes 16 to 19 (big-endian). */
    private static int readWidth(Path png) throws IOException {
        byte[] header = Files.readAllBytes(png);
        return ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
    }
}
