package minechem.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import minechem.Compendium;

/**
 * Reads a bundled data file through config/minechem/data/. The bundled data is copied there and read from there.
 * With alwaysCopy (the useDefault* settings), the copy is replaced on every start, so edits are lost. Without it, the copy
 * is made once and then kept, so it can be edited.
 */
public final class DataFileHelper {
    private DataFileHelper() {
    }

    public static InputStream open(Path configDir, Class<?> owner, String name, boolean alwaysCopy) throws IOException {
        Path target = configDir.resolve("minechem").resolve("data").resolve(name);
        if (alwaysCopy || !Files.isRegularFile(target)) {
            String resource = Compendium.Data.dataPrefix + name;
            try (InputStream bundled = owner.getResourceAsStream(resource)) {
                if (bundled == null) {
                    throw new IllegalStateException("Missing bundled data file " + resource);
                }
                Files.createDirectories(target.getParent());
                Files.copy(bundled, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            LogHelper.debug("Copied " + name + " to " + target);
        }
        return Files.newInputStream(target);
    }
}
