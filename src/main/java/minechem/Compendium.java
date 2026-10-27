package minechem;

/**
 * Constants shared across the mod: names, colours and data file paths.
 */
public class Compendium {
    public static final class Naming {
        public static final String id = "minechem";
        public static final String name = "Minechem";
        public static final String microscope = "microscope";
        public static final String synthesis = "synthesis";
        public static final String decomposer = "chemicalDecomposer";
    }

    public static final class Color {
        public static final class TrueColor {
            public static final int transparent = 0;
        }
    }

    public static final class Data {
        public static final String elementsJson = "elementsData.json";
        public static final String moleculesJson = "moleculesData.json";
        public static final String dataPrefix = "/assets/" + Naming.id + "/data/";
    }
}
