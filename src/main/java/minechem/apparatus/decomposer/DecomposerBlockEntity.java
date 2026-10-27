package minechem.apparatus.decomposer;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.IntStream;
import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.apparatus.synthesis.SynthesisRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import minechem.apparatus.decomposer.DecomposerMachine.Stack;
import minechem.chemical.ChemicalBase;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Item side of the decomposer. The slots are the machine's: slot 0 is the input, 1..18 the outputs.
 * The server ticks the {@link DecomposerMachine}; the block entity is the
 * {@link Container} view of its slots, and saves them with its buffer.
 *
 * <p>The state is saved and sent to the client, so the renderer can turn the windmill while the machine is active.
 *
 * <p>Energy: a NeoForge energy storage. A decomposition needs ENERGY_COST and uses it when it starts. Energy is used
 * unless Config.POWER_USE_ENABLED is false.
 */
public class DecomposerBlockEntity extends BaseTileEntity implements WorldlyContainer, minechem.radiation.IRadiationShield, minechem.radiation.INoDecay {
    @Override
    public float radiationReductionFactor() {
        return 0.4F;
    }

    public static final int INPUT_SLOT = 0;
    public static final int SIZE = DecomposerMachine.LAST_OUTPUT + 1;

    /** The top (side 1) reaches the input, every other side reaches the outputs. */
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = IntStream.rangeClosed(DecomposerMachine.FIRST_OUTPUT, DecomposerMachine.LAST_OUTPUT).toArray();

    /**
     * The recipe table, filled at common setup from the bundled data file (DecomposerRecipeLoader) and on each tag update by
     * the OreDictionary rules (DecomposerOreBinder).
     */
    public static final DecomposerRecipes RECIPES = new DecomposerRecipes();

    private final DecomposerMachine<ItemStack> machine = new DecomposerMachine<>(ItemStack::isSameItemSameComponents);
    /** Client only: the windmill angle in radians, advanced by the renderer. */
    private float windmillAngle;

    /** Energy storage capacity and the energy cost of one decomposition. */
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_COST = 1000;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY);
    /** The stored energy last sent to the client (see serverTick). */
    private int lastSyncedEnergy = -1;
    /** The game time of the last update packet (see serverTick). */
    private long lastEnergySync = Long.MIN_VALUE / 2;
    /** At most one energy-only update packet per this many ticks (it changes no gameplay value). */
    static final int ENERGY_SYNC_TICKS = 10;

    /** The tank's capacity in mB. */
    public static final int FLUID_CAPACITY = 5000;

    /**
     * Stands in for the input item while the tank is used. It is put in the machine's input
     * for one tick and taken back out if no decomposition starts. Compared by identity, never stored in a slot. Made on first
     * use, not when the class loads: an ItemStack needs the registries, which are not bound yet while the class is first
     * touched during mod loading (the server crashed on it).
     */
    private static ItemStack fluidInput;

    private static ItemStack fluidInput() {
        if (fluidInput == null) {
            fluidInput = new ItemStack(Items.STONE);
        }
        return fluidInput;
    }

    private final FluidStacksResourceHandler fluidTank = new FluidStacksResourceHandler(1, FLUID_CAPACITY);

    public DecomposerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DECOMPOSER.get(), pos, state);
    }

    public DecomposerMachine.State getMachineState() {
        return machine.state();
    }

    public float windmillAngle() {
        return windmillAngle;
    }

    /**
     * Turns the windmill: adds 0.2 per render while active, and wraps at 360. Client only. The angle is in radians,
     * so the wrap happens after 360 radians (about 57 turns).
     */
    public void advanceWindmill() {
        windmillAngle += 0.2F;
        if (windmillAngle >= 360.0F) {
            windmillAngle = 0.0F;
        }
    }

    /**
     * Empties the tank. The server does it when the GUI's button is clicked (DecomposerMenu.clickMenuButton).
     */
    public void dumpFluid() {
        try (Transaction transaction = Transaction.openRoot()) {
            FluidResource resource = fluidTank.getResource(0);
            int amount = fluidTank.getAmountAsInt(0);
            if (!resource.isEmpty() && amount > 0 && fluidTank.extract(0, resource, amount, transaction) == amount) {
                transaction.commit();
            }
        }
        setChanged();
    }

    /**
     * The status line of the GUI: "No Recipe" with no recipe for the input, "Active" with the
     * energy for a decomposition, otherwise "No Power".
     */
    public String getStateString() {
        if (machine.input() == null || !hasRecipe(machine.input().item())) {
            return "No Recipe";
        }
        return hasEnergy() ? "Active" : "No Power";
    }

    /**
     * Whether a decomposition can start: energy stored >= ENERGY_COST, or power use disabled (see Config.POWER_USE_ENABLED).
     */
    public boolean hasEnergy() {
        return energyStored() >= ENERGY_COST || !minechem.Config.powerUseEnabled();
    }

    /** The energy a decomposition needs: ENERGY_COST, or 0 when power use is disabled. */
    public static int energyNeeded() {
        return minechem.Config.powerUseEnabled() ? ENERGY_COST : 0;
    }

    public int energyStored() {
        return energy.getAmountAsInt();
    }

    /** The fluid storage behind the NeoForge fluid capability (pipes fill and drain it). */
    public FluidStacksResourceHandler getFluidTank() {
        return fluidTank;
    }

    /** The energy storage behind the NeoForge energy capability. */
    public SimpleEnergyHandler getEnergy() {
        return energy;
    }

    /** One server tick (updateEntity). Marks the block entity changed and sends its state when the state changes. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DecomposerBlockEntity decomposer) {
        DecomposerMachine.State before = decomposer.machine.state();
        if (decomposer.machine.input() == null && decomposer.fluidTank.getAmountAsInt(0) > 0) {
            // With no item in the input slot, the tank is the input.
            decomposer.machine.setInput(new DecomposerMachine.Stack<>(fluidInput(), 1));
        }
        decomposer.machine.tick(decomposer.hasEnergy(), decomposer::hasRecipe, decomposer::roll);
        if (decomposer.machine.input() != null && decomposer.machine.input().item() == fluidInput()) {
            decomposer.machine.setInput(null);
        }
        if (decomposer.machine.state() != DecomposerMachine.State.idle) {
            decomposer.setChanged();
        }
        // Energy comes from outside the machine (an energy source, a cable), so a change of the stored energy is sent too:
        // the state tab shows it, and the client would otherwise keep the value it had when the machine was opened.
        // The update packet is the full save, so a change of the stored energy alone (a cable fills the store every tick) is sent
        // at most every ENERGY_SYNC_TICKS; a change of the machine's state is sent at once. The last value always follows.
        int stored = decomposer.energy.getAmountAsInt();
        boolean energyDue = stored != decomposer.lastSyncedEnergy && level.getGameTime() - decomposer.lastEnergySync >= ENERGY_SYNC_TICKS;
        if (decomposer.machine.state() != before || energyDue) {
            decomposer.lastSyncedEnergy = stored;
            decomposer.lastEnergySync = level.getGameTime();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The key a recipe is looked up by. A chemical item is keyed by its chemical name; any other item by its registry id.
     * The recipe data must use the same keys when it is loaded.
     */
    public static String keyFor(ItemStack stack) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical != null) {
            return chemical.fullName;
        }
        // Of the potions, only the water bottle has a key of its own.
        if (stack.is(Items.POTION) && isWaterBottle(stack)) {
            return SynthesisRecipes.WATER_BOTTLE;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static boolean isWaterBottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().map(potion -> potion.is(Potions.WATER)).orElse(false);
    }

    /** Whether the input slot accepts an item: only items with a recipe. */
    static boolean acceptsItem(ItemStack stack) {
        return RECIPES.get(keyFor(stack)) != null;
    }

    /** The recipe for the tank's fluid; null when the tank is empty or has no recipe. */
    private DecomposerRecipes.Recipe fluidRecipe() {
        FluidResource resource = fluidTank.getResource(0);
        if (resource.isEmpty()) {
            return null;
        }
        return RECIPES.get(DecomposerRecipes.fluidKey(BuiltInRegistries.FLUID.getKey(resource.getFluid()).getPath()));
    }

    /** Whether the machine can start on its input: an item with a recipe, or the tank with its fluid recipe. */
    private boolean hasRecipe(ItemStack stack) {
        if (stack == fluidInput()) {
            DecomposerRecipes.Recipe recipe = fluidRecipe();
            return recipe != null && fluidTank.getAmountAsInt(0) >= recipe.fluidAmount();
        }
        return RECIPES.get(keyFor(stack)) != null;
    }

    /**
     * The outputs for one input, as stacks of chemical items: null on a chance miss, as DecomposerRecipes.roll. For the
     * tank, the fluid is used up first, so a chance miss still costs the fluid.
     */
    private List<Stack<ItemStack>> roll(ItemStack stack) {
        consumeEnergy();
        String key;
        if (stack == fluidInput()) {
            DecomposerRecipes.Recipe recipe = fluidRecipe();
            if (recipe == null) {
                return null;
            }
            consumeFluid(recipe.fluidAmount());
            key = recipe.input();
        } else {
            key = keyFor(stack);
        }
        List<DecomposerRecipes.Output> outputs = RECIPES.roll(key);
        if (outputs == null) {
            return null;
        }
        return outputs.stream()
            .map(output -> new Stack<>(ChemicalItem.stackFor(output.chemical()), output.amount()))
            .toList();
    }

    /** Uses the decomposition's energy, all or nothing. */
    private void consumeEnergy() {
        if (!minechem.Config.powerUseEnabled()) {
            // With power use disabled nothing is used.
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (energy.extract(ENERGY_COST, transaction) == ENERGY_COST) {
                transaction.commit();
            }
        }
    }

    /** Takes {@code amount} mB out of the tank, all or nothing. */
    private void consumeFluid(int amount) {
        FluidResource resource = fluidTank.getResource(0);
        try (Transaction transaction = Transaction.openRoot()) {
            if (fluidTank.extract(0, resource, amount, transaction) == amount) {
                transaction.commit();
            }
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SIZE;
    }

    private Stack<ItemStack> slotStack(int slot) {
        return slot == INPUT_SLOT ? machine.input() : machine.outputSlot(slot);
    }

    private void setSlotStack(int slot, Stack<ItemStack> stack) {
        if (slot == INPUT_SLOT) {
            machine.setInput(stack);
        } else {
            machine.setOutputSlot(slot, stack);
        }
    }

    private static ItemStack toItemStack(Stack<ItemStack> stack) {
        return stack == null ? ItemStack.EMPTY : stack.item().copyWithCount(stack.count());
    }

    private static Stack<ItemStack> fromItemStack(ItemStack stack) {
        return stack.isEmpty() ? null : new Stack<>(stack.copyWithCount(1), stack.getCount());
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < SIZE; slot++) {
            if (slotStack(slot) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return toItemStack(slotStack(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        Stack<ItemStack> stack = slotStack(slot);
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, stack.count());
        int left = stack.count() - taken;
        setSlotStack(slot, left > 0 ? new Stack<>(stack.item(), left) : null);
        setChanged();
        return stack.item().copyWithCount(taken);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = toItemStack(slotStack(slot));
        setSlotStack(slot, null);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        setSlotStack(slot, fromItemStack(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /** A hopper or pipe on top feeds the input, the other sides take the outputs. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? INPUT_SLOTS : OUTPUT_SLOTS;
    }

    /** Only an item with a recipe goes into the input (from any side that reaches it). */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == INPUT_SLOT && acceptsItem(stack);
    }

    /** Items can be taken from every slot and side. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SIZE; slot++) {
            setSlotStack(slot, null);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        NonNullList<ItemStack> slots = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < SIZE; slot++) {
            slots.set(slot, getItem(slot));
        }
        ContainerHelper.saveAllItems(output, slots);
        fluidTank.serialize(output);
        energy.serialize(output);
        output.putString("state", machine.state().name());
        List<Stack<ItemStack>> buffer = machine.buffer();
        output.store("bufferItems", ItemStack.CODEC.listOf(), buffer.stream().map(stack -> stack.item().copyWithCount(1)).toList());
        output.store("bufferCounts", Codec.INT.listOf(), buffer.stream().map(Stack::count).toList());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        NonNullList<ItemStack> slots = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, slots);
        fluidTank.deserialize(input);
        energy.deserialize(input);
        for (int slot = 0; slot < SIZE; slot++) {
            setSlotStack(slot, fromItemStack(slots.get(slot)));
        }
        List<ItemStack> items = input.read("bufferItems", ItemStack.CODEC.listOf()).orElse(List.of());
        List<Integer> counts = input.read("bufferCounts", Codec.INT.listOf()).orElse(List.of());
        List<Stack<ItemStack>> buffer = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(items.size(), counts.size()); i++) {
            buffer.add(new Stack<>(items.get(i).copyWithCount(1), counts.get(i)));
        }
        machine.setBuffer(buffer);
        machine.setState(input.read("state", Codec.STRING).map(DecomposerBlockEntity::stateNamed).orElse(DecomposerMachine.State.idle));
    }

    private static DecomposerMachine.State stateNamed(String name) {
        for (DecomposerMachine.State state : DecomposerMachine.State.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return DecomposerMachine.State.idle;
    }

    /** The client gets the state (and the slots) through the block entity's update packet. */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
