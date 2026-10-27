package minechem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 26.1 rejects resource paths with capitals (Identifier.assertValidPath: only [a-z0-9/._-]). A camelCase texture path
 * crashed the client at start. This test reads the main sources and fails on any such path passed to Identifier.
 */
class AssetPathTest {
    private static final Pattern RESOURCE_PATH = Pattern.compile("fromNamespaceAndPath\\(\\s*[^,]+,\\s*\"([^\"]*)\"");

    @Test
    void resourcePathsInSourcesAreLowercase() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            offenders = files.filter(p -> p.toString().endsWith(".java")).flatMap(p -> offendersIn(p).stream()).toList();
        }
        assertTrue(offenders.isEmpty(), "Resource paths with capitals: " + offenders);
    }

    private static List<String> offendersIn(Path file) {
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = RESOURCE_PATH.matcher(source);
            List<String> found = new java.util.ArrayList<>();
            while (matcher.find()) {
                String path = matcher.group(1);
                if (!path.equals(path.toLowerCase(java.util.Locale.ROOT))) {
                    found.add(file.getFileName() + ": " + path);
                }
            }
            return found;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
