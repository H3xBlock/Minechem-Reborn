package minechem.polytool;

/**
 * The alloys a polytool can take from an element. Each alloy adds to the tool's strength for the axe, shovel, pickaxe (ore),
 * stone and sword. The values are doubled when the alloy is created.
 */
public enum ElementAlloy {
    // Axe-Shovel-Pickaxe-Stone-Sword
    Aluminium(13, "Al", 4F, -2F, -2F, 0F, 0F), Scandium(21, "Sc", 4F, 0F, 0F, -2F, -2F), Vandium(23, "V", 4F, -1F, -1F, -1F, -1F),
    Manganese(25, "Mn", -2F, 4F, -2F, 0F, 0F), Cobalt(27, "Co", 0F, 4F, 0F, -2F, -2F), Cadmium(48, "Cd", -1F, 4F, -1F, -1F, -1F),
    Gallium(31, "Ga", -2F, -2F, 4F, 0F, 0F), Selenium(34, "Se", 0F, 0F, 4F, -2F, -2F), Strontium(38, "Sr", -1F, -1F, 4F, -1F, -1F),
    Yttrium(39, "Y", -2F, 0F, -2F, 4F, 0F), Niobium(41, "Nb", 0F, -2F, 0F, 4F, -2F), Molybdeum(42, "Mo", -1F, -1F, -1F, 4F, -1F),
    Ruthenium(44, "Ru", -2F, 0F, -2F, 0F, 4F), Rhodium(45, "Rh", 0F, -2F, 0F, -2F, 4F), Palladium(46, "Pd", -1F, -1F, -1F, -1F, 4F);

    /** The atomic number and the symbol of the alloy's element. */
    public final int atomicNumber;
    public final String symbol;
    public final float axe;
    public final float shovel;
    public final float pickaxe;
    public final float stone;
    public final float sword;

    ElementAlloy(int atomicNumber, String symbol, float axe, float shovel, float pickaxe, float stone, float sword) {
        this.atomicNumber = atomicNumber;
        this.symbol = symbol;
        this.axe = 2 * axe;
        this.shovel = 2 * shovel;
        this.pickaxe = 2 * pickaxe;
        this.stone = 2 * stone;
        this.sword = 2 * sword;
    }
}
