package minechem.helper;

import minechem.Config;
import minechem.Compendium;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging through the NeoForge logger (SLF4J).
 */
public class LogHelper {
    private static final Logger LOG = LoggerFactory.getLogger(Compendium.Naming.id);

    public static void debug(Object obj) {
        if (Config.debugMode()) {
            LOG.info(String.valueOf(obj));
        } else {
            LOG.debug(String.valueOf(obj));
        }
    }

    public static void warn(Object obj) {
        LOG.warn(String.valueOf(obj));
    }

    public static void info(Object obj) {
        LOG.info(String.valueOf(obj));
    }

    public static void exception(Object obj, Throwable exception) {
        LOG.error(String.valueOf(obj), exception);
    }
}
