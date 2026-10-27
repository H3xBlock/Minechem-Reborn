package minechem;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration, stored as a NeoForge common config in config/minechem-common.toml.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_MODE = BUILDER
        .comment("Log debug messages at info level.")
        .define("debugMode", false);
    public static final ModConfigSpec.BooleanValue USE_DEFAULT_ELEMENTS = BUILDER
        .comment("Load the built-in element data instead of the one in the config folder.")
        .define("useDefaultElements", true);
    public static final ModConfigSpec.BooleanValue USE_DEFAULT_MOLECULES = BUILDER
        .comment("Load the built-in molecule data instead of the one in the config folder.")
        .define("useDefaultMolecules", true);
    public static final ModConfigSpec.BooleanValue ALLOW_AUTOMATION = BUILDER
        .comment("Let hoppers and pipes insert and extract items in the synthesis machine and the reactor walls.")
        .define("allowAutomation", true);
    public static final ModConfigSpec.BooleanValue PLAYER_PRIVATE_KNOWLEDGE = BUILDER
        .comment("Keep the player's knowledge private: sneak-clicking a journal does not write it into the journal.")
        .define("playerPrivateKnowledge", false);

    public static final ModConfigSpec.BooleanValue RECREATIONAL_CHEMICAL_EFFECTS = BUILDER
        .comment("The recreational drugs: their effects when drunk, and the decompositions that give them (brown mushroom, nether wart,",
            "duskberry). Default true. Read at start and on a tag reload.")
        .define("recreationalChemicalEffects", true);

    public static final ModConfigSpec.BooleanValue FLUID_EFFECTS = BUILDER
        .comment("Liquids of molecules and elements affect the living entities in them: pharmacology and radioactive poison.")
        .define("fluidEffects", true);

    public static final ModConfigSpec.BooleanValue REACTION_ITEM_MEET_FLUID = BUILDER
        .comment("A bucket of a chemical that falls into a water-like liquid reacts with it.")
        .define("reactionItemMeetFluid", true);

    public static final ModConfigSpec.BooleanValue REACTION_FLUID_MEET_FLUID = BUILDER
        .comment("Two liquids that touch react when their chemicals have a reaction.")
        .define("reactionFluidMeetFluid", true);

    /**
     * Whether the machines use energy. The default is true: the machines need energy to work. Set it to false to play
     * without an energy mod: the machines then work without energy.
     */
    public static final ModConfigSpec.BooleanValue POWER_USE_ENABLED = BUILDER
        .comment("The machines need energy (Forge Energy) to work. Set to false to play without an energy mod:",
            "the decomposer, the synthesis machine and the fission reactor then work without energy.",
            "The fusion reactor still waits for more stored energy than the fusion needs.")
        .define("powerUseEnabled", true);

    public static final ModConfigSpec.BooleanValue ADVANCED_TURTLE_AI = BUILDER
        .comment("ComputerCraft: name the chemical turtle upgrade \"Jenkins\" instead of \"Chemical\".")
        .define("advancedTurtleAI", false);

    // The "worldgen" category: uranium ore generation settings (generateOre, uraniumOreClusterSize, uraniumoredensity).
    // Read by minechem.worldgen.UraniumOreFeature each time it places.
    static {
        BUILDER.push("worldgen");
    }

    public static final ModConfigSpec.BooleanValue GENERATE_ORE = BUILDER
        .comment("Generate uranium ore in the overworld.")
        .define("generateOre", true);
    public static final ModConfigSpec.IntValue URANIUM_ORE_CLUSTER_SIZE = BUILDER
        .comment("Uranium ore vein size: each vein has a random size from 0 to this value + 9.")
        .defineInRange("uraniumOreClusterSize", 3, 1, 10);
    public static final ModConfigSpec.IntValue URANIUM_ORE_DENSITY = BUILDER
        .comment("Uranium ore veins tried per chunk, minus one: the generation loop runs from 0 to this value inclusive.")
        .defineInRange("uraniumoredensity", 5, 1, 64);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    /** Reads debugMode, or false before the config is loaded (early startup logging). */
    public static boolean debugMode() {
        return SPEC.isLoaded() && DEBUG_MODE.get();
    }

    /** Reads useDefaultElements. Before the config is loaded, the default (true) is used and the call is logged. */
    public static boolean useDefaultElements() {
        return readOrDefault(USE_DEFAULT_ELEMENTS, true, "useDefaultElements");
    }

    /** Reads useDefaultMolecules. Before the config is loaded, the default (true) is used and the call is logged. */
    public static boolean useDefaultMolecules() {
        return readOrDefault(USE_DEFAULT_MOLECULES, true, "useDefaultMolecules");
    }

    private static boolean readOrDefault(ModConfigSpec.BooleanValue value, boolean fallback, String key) {
        if (SPEC.isLoaded()) {
            return value.get();
        }
        org.slf4j.LoggerFactory.getLogger(Compendium.Naming.id).warn("Config {} read before it is loaded; using {}", key, fallback);
        return fallback;
    }

    /** Reads recreationalChemicalEffects, or the default (true) before the config is loaded. */
    public static boolean recreationalChemicalEffects() {
        return readOrDefault(RECREATIONAL_CHEMICAL_EFFECTS, true, "recreationalChemicalEffects");
    }

    /** Reads fluidEffects, or the default (true) before the config is loaded. */
    public static boolean fluidEffects() {
        return readOrDefault(FLUID_EFFECTS, true, "fluidEffects");
    }

    /** Reads reactionItemMeetFluid, or the default (true) before the config is loaded. */
    public static boolean reactionItemMeetFluid() {
        return readOrDefault(REACTION_ITEM_MEET_FLUID, true, "reactionItemMeetFluid");
    }

    /** Reads reactionFluidMeetFluid, or the default (true) before the config is loaded. */
    public static boolean reactionFluidMeetFluid() {
        return readOrDefault(REACTION_FLUID_MEET_FLUID, true, "reactionFluidMeetFluid");
    }

    /** Reads powerUseEnabled, or the default (true) before the config is loaded. */
    public static boolean powerUseEnabled() {
        return readOrDefault(POWER_USE_ENABLED, true, "powerUseEnabled");
    }

    /** Reads advancedTurtleAI, or the default (false) before the config is loaded. */
    public static boolean advancedTurtleAI() {
        return readOrDefault(ADVANCED_TURTLE_AI, false, "advancedTurtleAI");
    }

    /** Reads worldgen.generateOre, or the default (true) before the config is loaded. */
    public static boolean generateOre() {
        return readOrDefault(GENERATE_ORE, true, "generateOre");
    }

    /** Reads worldgen.uraniumoredensity, or the default (5) before the config is loaded. */
    public static int uraniumOreDensity() {
        return readOrDefault(URANIUM_ORE_DENSITY, 5, "uraniumoredensity");
    }

    /** Reads worldgen.uraniumOreClusterSize, or the default (3) before the config is loaded. */
    public static int uraniumOreClusterSize() {
        return readOrDefault(URANIUM_ORE_CLUSTER_SIZE, 3, "uraniumOreClusterSize");
    }

    private static int readOrDefault(ModConfigSpec.IntValue value, int fallback, String key) {
        if (SPEC.isLoaded()) {
            return value.get();
        }
        org.slf4j.LoggerFactory.getLogger(Compendium.Naming.id).warn("Config {} read before it is loaded; using {}", key, fallback);
        return fallback;
    }

    /** Reads playerPrivateKnowledge, or the default before the config is loaded. */
    public static boolean playerPrivateKnowledge() {
        return SPEC.isLoaded() ? PLAYER_PRIVATE_KNOWLEDGE.get() : false;
    }
}
