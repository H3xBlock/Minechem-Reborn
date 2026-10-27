package minechem.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.microscope.MicroscopeBlockEntity;
import minechem.apparatus.synthesis.SynthesisBlockEntity;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.chemical.ChemicalBase;
import minechem.chemical.ChemicalStructure;
import minechem.chemical.Element;
import minechem.chemical.Molecule;
import minechem.item.chemical.ChemicalItem;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationState;
import minechem.radiation.RadiationTicker;
import minechem.registry.ItemRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The turtle's peripheral (type "chemical"): sixteen Lua methods that read the chemicals of the turtle's inventory, read and
 * write the known recipes and the journal, scan and fill the machines in front of the turtle, and report the decays of its
 * inventory. The methods keep their names, arguments and messages.
 *
 * <p>The "known" recipes are kept as keys: a chemical's name, or an item's registry id. They are not saved with the turtle.
 */
public final class ChemicalTurtlePeripheral implements IDynamicPeripheral {
    private static final String TYPE = "chemical";

    /** The Lua method's code: the peripheral, the computer that calls it and its arguments. */
    @FunctionalInterface
    private interface Handler {
        MethodResult call(ChemicalTurtlePeripheral self, IComputerAccess computer, IArguments args) throws LuaException;
    }

    /** One Lua method: its name, its argument text, its detail lines (after the signature) and its code. */
    private record Method(String name, String args, String[] details, Handler handler) {
        String signature() {
            return name + args;
        }
    }

    /** The methods, in order (the index is the method number used by callMethod). */
    private static final List<Method> METHODS = List.of(
        new Method("getMethods", "()", new String[] {"Returns: List of Method Names and Arguments"}, ChemicalTurtlePeripheral::getMethods),
        new Method("getDetails", "(Method Name)", new String[] {"Arg: Method Name - from getMethods()", "Returns: Details of arguments and results"},
            ChemicalTurtlePeripheral::getDetails),
        new Method("getChemicalName", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Chemical name of stack in slot, or nil if not a chemical"}, ChemicalTurtlePeripheral::getChemicalName),
        new Method("getChemicalFormula", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Chemical formula of stack in slot, or nil if not a chemical"}, ChemicalTurtlePeripheral::getChemicalFormula),
        new Method("getChemicalsAsTable", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Chemical formula of stack in slot as a table, or nil if not a chemical"}, ChemicalTurtlePeripheral::getChemicalsAsTable),
        new Method("getAtomicMass", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Atomic Mass of elements"}, ChemicalTurtlePeripheral::getAtomicMass),
        new Method("getRadioactivity", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Radioactivity level of chemicals"}, ChemicalTurtlePeripheral::getRadioactivity),
        new Method("getDecayTimeInTicks", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Time left to Radioactive Decay in Ticks - Infinite for stable chemicals, null for non-chemicals"},
            ChemicalTurtlePeripheral::getDecayTimeInTicks),
        new Method("getDecayTime", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Time left to Radioactive Decay - Infinite for stable chemicals, null for non-chemicals"}, ChemicalTurtlePeripheral::getDecayTime),
        new Method("syncJournal", "(?Slot)", new String[] {"Arg: Optional Slot Number, defaults to current slot",
            "Returns: Number of Journal Entries loaded and saved"}, ChemicalTurtlePeripheral::syncJournal),
        new Method("readRecipe", "(?Direction)", new String[] {"Arg: Optional Direction, defaults to front",
            "Returns: Confirmtion String, Recipe read"}, ChemicalTurtlePeripheral::readRecipe),
        new Method("getSynthesisRecipe", "(name,?metadata)", new String[] {
            "Args: Item Name and optional metadata (ignored in 26.1, where items have no damage value)",
            "Returns: Synthesiser Recipes in table form"}, ChemicalTurtlePeripheral::getSynthesisRecipe),
        new Method("getSynthesisRecipes", "()", new String[] {"Returns: All Synthesiser Recipes in table form"},
            ChemicalTurtlePeripheral::getSynthesisRecipes),
        new Method("setRecipe", "(?Direction,name,?metadata)", new String[] {"Arg: Optional Direction, defaults to front", "Arg: Item name",
            "Arg: Optional metadata, ignored in 26.1", "Returns: boolean success"}, ChemicalTurtlePeripheral::setRecipe),
        new Method("setMicroscope", "(?Direction,?slot)", new String[] {"Arg: Optional Direction, defaults to front",
            "Arg: Optional slot number, defaults to current", "Returns: boolean success"}, ChemicalTurtlePeripheral::setMicroscope),
        new Method("getState", "(?Direction)", new String[] {"Arg: Optional Direction, defaults to front", "Returns: Machine state"},
            ChemicalTurtlePeripheral::getState));

    private final ITurtleAccess turtle;
    private final List<String> known = new ArrayList<>();
    private IComputerAccess computer;

    public ChemicalTurtlePeripheral(ITurtleAccess turtle) {
        this.turtle = turtle;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String[] getMethodNames() {
        return METHODS.stream().map(Method::name).toArray(String[]::new);
    }

    /**
     * CC:Tweaked calls a peripheral on the computer thread. Every method but getMethods and getDetails reads or changes the turtle's
     * inventory, the machines in front of it or the recipe tables, so it runs on the server thread through
     * {@link ILuaContext#executeMainThreadTask} (the Lua call waits for the result).
     */
    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException {
        Method target = METHODS.get(method);
        // Methods 0 and 1 (getMethods, getDetails) only read the method table.
        if (method <= 1) {
            return target.handler().call(this, computer, arguments);
        }
        arguments.escapes();
        return context.executeMainThreadTask(() -> {
            Object[] result = target.handler().call(this, computer, arguments).getResult();
            return result == null ? new Object[0] : result;
        });
    }

    @Override
    public void attach(IComputerAccess computer) {
        this.computer = computer;
    }

    @Override
    public void detach(IComputerAccess computer) {
        this.computer = null;
    }

    @Override
    public boolean equals(IPeripheral other) {
        return false;
    }

    /**
     * Called every tick by the upgrade. Every 20 ticks, with a computer attached, the inventory decays and the computer gets an
     * "onDecay" event for each decay.
     */
    public void tick(net.minecraft.world.level.Level level) {
        if (computer == null || level.getGameTime() % 20 != 0) {
            return;
        }
        String dimension = level.dimension().identifier().toString();
        for (RadiationTicker.Decay decay : RadiationTicker.decayContainer(turtle.getInventory(), level.getGameTime(), dimension)) {
            computer.queueEvent("onDecay", decay.before().fullName,
                decay.after() == null ? "nothing" : decay.after().fullName, decay.damage());
        }
    }

    // ---- Lua methods ------------------------------------------------------------------------------------------------------

    private MethodResult getMethods(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() != 0) {
            throw new LuaException("getMethods does not take any arguments");
        }
        return MethodResult.of((Object[]) METHODS.stream().map(Method::signature).toArray(String[]::new));
    }

    private MethodResult getDetails(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() != 1) {
            throw new LuaException("getDetails takes a single argument");
        }
        String name = args.getString(0);
        for (Method method : METHODS) {
            if (method.name().equalsIgnoreCase(name)) {
                List<String> lines = new ArrayList<>();
                lines.add(method.signature());
                lines.addAll(List.of(method.details()));
                return MethodResult.of((Object[]) lines.toArray(String[]::new));
            }
        }
        throw new LuaException("Invalid Method Name - do not include brackets");
    }

    private MethodResult getChemicalName(IComputerAccess computer, IArguments args) throws LuaException {
        ItemStack stack = stackAt(args);
        if (ChemicalItem.getChemicalBase(stack) == null) {
            return MethodResult.of();
        }
        return MethodResult.of(stack.getHoverName().getString());
    }

    private MethodResult getChemicalFormula(IComputerAccess computer, IArguments args) throws LuaException {
        ItemStack stack = stackAt(args);
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical instanceof Element element) {
            return MethodResult.of(element.shortName);
        }
        if (chemical instanceof Molecule molecule) {
            return MethodResult.of(molecule.getFormula());
        }
        return MethodResult.of();
    }

    private MethodResult getChemicalsAsTable(IComputerAccess computer, IArguments args) throws LuaException {
        ItemStack stack = stackAt(args);
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        Map<Integer, Object> table = new HashMap<>();
        if (chemical instanceof Element) {
            table.put(1, entry(stack.getHoverName().getString(), 1));
            return MethodResult.of(table);
        }
        if (chemical instanceof Molecule molecule) {
            int index = 1;
            for (ChemicalStructure.ChemicalBaseSet set : molecule.structure.getInternalStructure()) {
                table.put(index++, entry(set.getChemical().fullName, set.getCount()));
            }
            return MethodResult.of(table);
        }
        return MethodResult.of();
    }

    /** Returns the atomic number, not the mass. */
    private MethodResult getAtomicMass(IComputerAccess computer, IArguments args) throws LuaException {
        if (ChemicalItem.getChemicalBase(stackAt(args)) instanceof Element element) {
            return MethodResult.of(element.atomicNumber);
        }
        return MethodResult.of();
    }

    private MethodResult getRadioactivity(IComputerAccess computer, IArguments args) throws LuaException {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stackAt(args));
        if (chemical == null) {
            return MethodResult.of();
        }
        return MethodResult.of(RadiationTicker.levelOf(chemical).name());
    }

    private MethodResult getDecayTimeInTicks(IComputerAccess computer, IArguments args) throws LuaException {
        Object result = decayTime(stackAt(args));
        return result == null ? MethodResult.of() : MethodResult.of(result);
    }

    /** The decay time in hours and minutes; "unstable" for a negative time. */
    private MethodResult getDecayTime(IComputerAccess computer, IArguments args) throws LuaException {
        Object result = decayTime(stackAt(args));
        if (result instanceof Long ticks) {
            return MethodResult.of(ticks < 0 ? "unstable" : ChemicalItem.formatTicks(ticks));
        }
        return result == null ? MethodResult.of() : MethodResult.of(result);
    }

    /** Long ticks left for a decaying chemical, "Infinite" for a stable one, null for a non-chemical. */
    private Object decayTime(ItemStack stack) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical == null) {
            return null;
        }
        RadiationEnum level = RadiationTicker.levelOf(chemical);
        RadiationState state = stack.get(ItemRegistry.RADIATION.get());
        if (level != RadiationEnum.stable && state != null) {
            return ChemicalItem.remainingTicks(state, level, turtle.getLevel().getGameTime());
        }
        return "Infinite";
    }

    /**
     * Loads the journal's recipes into the turtle's known list, and saves the known recipes into the journal. A book in the
     * slot is turned into a journal.
     */
    private MethodResult syncJournal(IComputerAccess computer, IArguments args) throws LuaException {
        int slot = slotOf(args);
        ItemStack stack = turtle.getInventory().getItem(slot);
        String result;
        if (stack.getItem() instanceof ChemistJournalItem) {
            result = upload(stack) + " - " + download(stack, slot, computer);
        } else if (stack.getItem() == Items.BOOK && stack.getCount() == 1) {
            result = download(stack, slot, computer);
        } else {
            throw new LuaException("Invalid Stack - not a journal or a book");
        }
        return MethodResult.of(result);
    }

    /** Adds the journal's recipes that are not known yet. */
    private String upload(ItemStack journal) {
        List<String> add = new ArrayList<>(discovered(journal));
        add.removeAll(known);
        known.addAll(add);
        return "Loaded " + add.size() + " recipe" + (add.size() != 1 ? "s" : "");
    }

    /**
     * Writes the known recipes that the journal does not have yet. The journal is set back in its slot.
     */
    private String download(ItemStack journal, int slot, IComputerAccess computer) {
        ItemStack target = journal.getItem() == Items.BOOK ? new ItemStack(ItemRegistry.CHEMIST_JOURNAL.get()) : journal;
        List<String> add = knownKeys();
        add.removeAll(discovered(target));
        for (String key : add) {
            ChemistJournalItem.addDiscovered(target, key);
        }
        String owner = ChemistJournalItem.owner(target);
        if (owner == null || owner.isEmpty()) {
            ChemistJournalItem.setOwner(target, "Chemistry Turtle " + computer.getID());
        }
        turtle.getInventory().setItem(slot, target);
        return "Saved " + add.size() + " recipe" + (add.size() != 1 ? "s" : "");
    }

    /**
     * Scans the recipe of the machine in front of the turtle, or of the microscope's item, into the known list. The machines
     * are the synthesis machine (its current recipe), the decomposer (its input's recipe) and the microscope.
     */
    private MethodResult readRecipe(IComputerAccess computer, IArguments args) throws LuaException {
        Direction dir = args.count() == 0 ? turtle.getDirection() : directionArgument(args, 0);
        if (dir == null || args.count() > 1) {
            throw new LuaException("Invalid Arguments");
        }
        BlockEntity entity = blockAt(dir);
        if (entity instanceof SynthesisBlockEntity synthesis) {
            SynthesisRecipes.Recipe recipe = synthesis.getCurrentRecipe();
            if (recipe != null) {
                return scannedResult(recipe.output(), "Synthesis Recipe Scanned");
            }
        } else if (entity instanceof DecomposerBlockEntity decomposer) {
            ItemStack input = decomposer.getItem(DecomposerBlockEntity.INPUT_SLOT);
            DecomposerRecipes.Recipe recipe = input.isEmpty() ? null : DecomposerBlockEntity.RECIPES.get(DecomposerBlockEntity.keyFor(input));
            if (recipe != null) {
                return scannedResult(recipe.input(), "Decomposer Recipe Scanned");
            }
        } else if (entity instanceof MicroscopeBlockEntity microscope) {
            ItemStack scanned = microscope.getItem(0);
            if (!scanned.isEmpty()) {
                return scannedResult(DecomposerBlockEntity.keyFor(scanned), "Microscope scan complete");
            }
        }
        return MethodResult.of();
    }

    private MethodResult scannedResult(String key, String message) {
        if (!addToKnown(key)) {
            return MethodResult.of("Item already known");
        }
        return MethodResult.of(message, keyMap(key));
    }

    /** The recipe of a known name (the second argument, the metadata, is accepted and ignored). */
    private MethodResult getSynthesisRecipe(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() != 1 && args.count() != 2) {
            throw new LuaException("Invalid arguments");
        }
        String name = args.getString(0);
        for (String key : knownKeys()) {
            if (key.equals(name)) {
                SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get(key);
                if (recipe != null) {
                    return MethodResult.of(synthesisMap(recipe));
                }
                return MethodResult.of("No Synthesiser recipe exists for " + name);
            }
        }
        return MethodResult.of(name + " is unknown.");
    }

    /** The synthesis recipes of the known names, numbered from 1. */
    private MethodResult getSynthesisRecipes(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() != 0) {
            throw new LuaException("getSynthesisRecipes does not take any arguments");
        }
        Map<Integer, Object> result = new HashMap<>();
        int index = 1;
        for (String key : knownKeys()) {
            SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get(key);
            if (recipe != null) {
                result.put(index++, synthesisMap(recipe));
            }
        }
        return MethodResult.of(result);
    }

    /**
     * Sets the grid of the synthesis machine in front of the turtle to a known recipe. The arguments are (name),
     * (name, metadata), (direction, name) or (direction, name, metadata). A call with a direction and no name is an
     * invalid-arguments error.
     */
    private MethodResult setRecipe(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() < 1 || args.count() > 3) {
            return MethodResult.of(false);
        }
        Direction dir = directionArgument(args, 0);
        String name;
        if (dir == null) {
            if (args.count() >= 3) {
                throw new LuaException("Invalid Arguments");
            }
            name = args.getString(0);
            dir = turtle.getDirection();
        } else {
            if (args.count() < 2) {
                throw new LuaException("Invalid Arguments");
            }
            name = args.getString(1);
        }
        if (!(blockAt(dir) instanceof SynthesisBlockEntity synthesis)) {
            return MethodResult.of(false);
        }
        for (String key : knownKeys()) {
            if (key.equals(name)) {
                SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get(key);
                if (recipe != null) {
                    synthesis.setRecipe(recipe);
                    return MethodResult.of(true);
                }
                return MethodResult.of(false);
            }
        }
        return MethodResult.of(false);
    }

    /**
     * Moves one item between the turtle's slot and the microscope in front of it.
     */
    private MethodResult setMicroscope(IComputerAccess computer, IArguments args) throws LuaException {
        Direction dir = null;
        Integer slot = null;
        switch (args.count()) {
            case 0 -> {
                slot = turtle.getSelectedSlot();
                dir = turtle.getDirection();
            }
            case 1 -> {
                dir = directionArgument(args, 0);
                if (dir == null) {
                    slot = validSlot(args.getInt(0));
                    dir = turtle.getDirection();
                }
            }
            case 2 -> {
                dir = directionArgument(args, 0);
                slot = validSlot(args.getInt(1));
            }
            default -> {
            }
        }
        if (dir == null || slot == null) {
            throw new LuaException("Invalid Arguments");
        }
        ItemStack current = turtle.getInventory().getItem(slot);
        if (!(blockAt(dir) instanceof MicroscopeBlockEntity microscope)) {
            return MethodResult.of(false);
        }
        Container inventory = turtle.getInventory();
        ItemStack scanned = microscope.getItem(0);
        if (scanned.isEmpty() && current.isEmpty()) {
            return MethodResult.of(false);
        }
        if (scanned.isEmpty()) {
            microscope.setItem(0, inventory.removeItem(slot, 1));
            return MethodResult.of(true);
        }
        if (current.isEmpty()) {
            inventory.setItem(slot, microscope.removeItem(0, 1));
            return MethodResult.of(true);
        }
        if (ItemStack.isSameItemSameComponents(current, scanned) && current.getCount() < current.getMaxStackSize()) {
            microscope.removeItem(0, 1);
            current.grow(1);
            inventory.setItem(slot, current);
            return MethodResult.of(true);
        }
        return MethodResult.of(false);
    }

    /** The state of the machine in front of the turtle: synthesis or decomposer. */
    private MethodResult getState(IComputerAccess computer, IArguments args) throws LuaException {
        if (args.count() >= 2) {
            throw new LuaException("Invalid Arguments");
        }
        Direction dir = turtle.getDirection();
        if (args.count() == 1) {
            dir = directionArgument(args, 0);
            if (dir == null) {
                throw new LuaException("Invalid Arguments");
            }
        }
        BlockEntity entity = blockAt(dir);
        if (entity instanceof SynthesisBlockEntity synthesis) {
            return MethodResult.of(synthesis.getState());
        }
        if (entity instanceof DecomposerBlockEntity decomposer) {
            return MethodResult.of(decomposer.getStateString());
        }
        throw new LuaException("Invalid TileEntity");
    }

    // ---- Helpers ----------------------------------------------------------------------------------------------------------

    /** The optional slot argument, default the selected slot ("Invalid Slot Number." when out of range). */
    private int slotOf(IArguments args) throws LuaException {
        if (args.count() == 0) {
            return turtle.getSelectedSlot();
        }
        if (args.count() > 1) {
            throw new LuaException("Maximum 1 argument for slot number.");
        }
        Integer slot = validSlot(args.getInt(0));
        if (slot == null) {
            throw new LuaException("Invalid Slot Number.");
        }
        return slot;
    }

    private ItemStack stackAt(IArguments args) throws LuaException {
        return turtle.getInventory().getItem(slotOf(args));
    }

    /** The slot if it is in the inventory, else null. A negative slot is invalid. */
    private Integer validSlot(int slot) {
        return slot >= 0 && slot < turtle.getInventory().getContainerSize() ? slot : null;
    }

    /** A direction name (top, bottom, front, back, left, right), or null for any other argument. */
    private Direction directionFor(String side) {
        return switch (side) {
            case "top" -> Direction.UP;
            case "bottom" -> Direction.DOWN;
            case "front" -> turtle.getDirection();
            case "back" -> turtle.getDirection().getOpposite();
            // Left is counter-clockwise from the front, right is clockwise.
            case "left" -> turtle.getDirection().getCounterClockWise();
            case "right" -> turtle.getDirection().getClockWise();
            default -> null;
        };
    }

    private Direction directionArgument(IArguments args, int index) throws LuaException {
        return args.get(index) instanceof String side ? directionFor(side) : null;
    }

    private BlockEntity blockAt(Direction dir) {
        return turtle.getLevel().getBlockEntity(turtle.getPosition().relative(dir));
    }

    private List<String> discovered(ItemStack journal) {
        return new ArrayList<>(journal.getOrDefault(ItemRegistry.JOURNAL_DISCOVERED.get(), List.of()));
    }

    private boolean addToKnown(String key) {
        if (key == null || known.contains(key)) {
            return false;
        }
        known.add(key);
        return true;
    }

    /**
     * The known names that have a recipe: a decomposer input or a synthesis output (the others are dropped). Returned as a
     * new list, so the caller may change it.
     */
    private List<String> knownKeys() {
        List<String> result = new ArrayList<>();
        for (String key : known) {
            if (DecomposerBlockEntity.RECIPES.get(key) != null || SynthesisRecipes.RECIPES.get(key) != null) {
                result.add(key);
            }
        }
        return result;
    }

    private static Map<String, Object> keyMap(String key) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", key);
        return map;
    }

    private static Map<String, Object> entry(String chemical, int quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("chemical", chemical);
        map.put("quantity", quantity);
        return map;
    }

    /** A synthesis recipe as a table. Empty slots of a shaped recipe are skipped. */
    private static Map<String, Object> synthesisMap(SynthesisRecipes.Recipe recipe) {
        Map<String, Object> result = new HashMap<>();
        result.put("output", keyMap(recipe.output()));
        result.put("shaped", recipe.shaped());
        result.put("energyCost", SynthesisRecipes.energyCost(recipe));
        Map<Integer, Object> inputs = new HashMap<>();
        int index = 1;
        for (SynthesisRecipes.Ingredient ingredient : recipe.ingredients()) {
            if (ingredient != null) {
                Map<String, Object> input = new HashMap<>();
                input.put("element", ingredient.chemical().fullName);
                input.put("quantity", ingredient.amount());
                inputs.put(index++, input);
            }
        }
        result.put("ingredients", inputs);
        return result;
    }
}
