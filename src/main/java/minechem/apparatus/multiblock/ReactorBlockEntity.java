package minechem.apparatus.multiblock;

import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.chemical.Element;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Block entity of the manager of a reactor: the block at the centre of the
 * structure, which holds the items and the energy. Every {@value #CHECK_INTERVAL} ticks it checks every cell of its
 * blueprint; while the structure is complete it links the walls (see {@link ReactorWallBlockEntity}) to itself, and runs its
 * machine each tick ({@link #tickMachine}).
 *
 * <p>The structure is not rotated: the cells are at the manager's position plus the blueprint's offset.
 * The first check is made on the first tick, not after the first interval, so a placed or rebuilt reactor starts at once.
 *
 * <p>Automation: the manager does not accept or give items from its own sides. The walls do, through
 * {@link ReactorWallBlockEntity#itemHandler}, with the rules of {@link #proxyView}.
 */
public abstract class ReactorBlockEntity extends BaseTileEntity implements WorldlyContainer, minechem.radiation.IRadiationShield {
    @Override
    public float radiationReductionFactor() {
        return 0.4F;
    }

    /** The structure is checked every 40 ticks. */
    public static final int CHECK_INTERVAL = 40;
    /** The stack limit of every slot. */
    public static final int STACK_LIMIT = 64;

    private final ReactorBlueprint blueprint;
    private final SimpleEnergyHandler energy;
    private final NonNullList<ItemStack> items;
    private boolean complete;
    private boolean checkedOnce;
    private long lastCheck;

    protected ReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ReactorBlueprint blueprint, int energyCapacity, int slots) {
        super(type, pos, state);
        this.blueprint = blueprint;
        this.energy = new SimpleEnergyHandler(energyCapacity);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
    }

    /** The server tick of every reactor's block. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorBlockEntity reactor) {
        reactor.tick(level);
    }

    private void tick(Level level) {
        long now = level.getGameTime();
        if (!checkedOnce || now - lastCheck >= CHECK_INTERVAL) {
            checkedOnce = true;
            lastCheck = now;
            boolean correct = structureIsCorrect(level);
            if (complete && !correct) {
                complete = false;
                linkWalls(level, false);
            } else if (!complete && correct) {
                complete = true;
                linkWalls(level, true);
            }
        }
        if (complete && !level.isClientSide()) {
            tickMachine(level);
        }
    }

    /** Whether the structure is complete. Only the server knows it, after its check. */
    public boolean isComplete() {
        return complete;
    }

    public ReactorBlueprint blueprint() {
        return blueprint;
    }

    /** The energy storage behind the NeoForge energy capability. */
    public SimpleEnergyHandler getEnergy() {
        return energy;
    }

    /** One tick of the machine, while the structure is complete. Server only. */
    protected abstract void tickMachine(Level level);

    /** The slots a face of the manager reaches. */
    protected abstract int[] slotsForFace(Direction side);

    /** Whether a stack may go into a slot of the manager. */
    @Override
    public abstract boolean canPlaceItem(int slot, ItemStack stack);

    /** Machine data written after the items and the energy. */
    protected void saveMachine(ValueOutput output) {
    }

    /** Machine data read after the items and the energy. */
    protected void loadMachine(ValueInput input) {
    }

    /** The world position of blueprint cell (x, y, z): the manager's position plus the cell's offset from the manager. */
    protected BlockPos cellPos(int x, int y, int z) {
        return getBlockPos().offset(x - blueprint.managerX(), y - blueprint.managerY(), z - blueprint.managerZ());
    }

    /**
     * Uses {@code amount} of the stored energy, all or nothing. Zero always succeeds, and so does any amount
     * when power use is disabled (Config.POWER_USE_ENABLED).
     */
    protected boolean useEnergy(int amount) {
        if (!minechem.Config.powerUseEnabled()) {
            return true;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (energy.extract(amount, transaction) == amount) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }

    /** The element a stack holds, or null when it is not an element. */
    static Element elementOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return ChemicalItem.getChemicalBase(stack) instanceof Element element ? element : null;
    }

    /** A stack of the element with this atomic number. */
    static ItemStack elementStack(int atomicNumber, int count) {
        ItemStack stack = ChemicalItem.stackFor(Jenkins.get(atomicNumber));
        stack.setCount(count);
        return stack;
    }

    /** The wall block of a structure id. */
    static Block wallBlock(int id) {
        return id == ReactorBlueprint.WALL_1 ? BlockRegistry.FUSION_WALL_1.get() : BlockRegistry.FUSION_WALL_2.get();
    }

    private boolean structureIsCorrect(Level level) {
        for (int y = 0; y < blueprint.ySize(); y++) {
            for (int x = 0; x < blueprint.xSize(); x++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    if (blueprint.isManager(x, y, z)) {
                        continue;
                    }
                    int id = blueprint.idAt(x, y, z);
                    if (id == ReactorBlueprint.WILDCARD) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cellPos(x, y, z));
                    if (id == ReactorBlueprint.AIR ? !state.isAir() : !state.is(wallBlock(id))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Every wall of the structure gets this manager, or none. */
    private void linkWalls(Level level, boolean link) {
        for (int y = 0; y < blueprint.ySize(); y++) {
            for (int x = 0; x < blueprint.xSize(); x++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    int id = blueprint.idAt(x, y, z);
                    if (id != ReactorBlueprint.WALL_1 && id != ReactorBlueprint.WALL_2) {
                        continue;
                    }
                    if (level.getBlockEntity(cellPos(x, y, z)) instanceof ReactorWallBlockEntity wall) {
                        wall.setManager(link ? getBlockPos() : null);
                    }
                }
            }
        }
    }

    /**
     * The items as a proxy wall gives them: an insert is allowed when the manager accepts the stack,
     * an extract only from the bottom and only from slot 2 (the output), and only while automation is allowed.
     */
    public WorldlyContainer proxyView() {
        return new ProxyView(this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return STACK_LIMIT;
    }

    @Override
    public boolean stillValid(Player player) {
        return complete && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slotsForFace(side);
    }

    /** The manager's own sides take nothing; only the walls move items. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        energy.serialize(output);
        saveMachine(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        energy.deserialize(input);
        loadMachine(input);
        // The structure is checked again by the first tick.
        complete = false;
        checkedOnce = false;
    }

    /** The items of a wall's view: the manager's slots, with the proxy rules. */
    private static final class ProxyView implements WorldlyContainer {
        private static final int OUTPUT_SLOT = 2;
        private final ReactorBlockEntity manager;

        ProxyView(ReactorBlockEntity manager) {
            this.manager = manager;
        }

        @Override
        public int getContainerSize() {
            return manager.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return manager.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return manager.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return manager.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return manager.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            manager.setItem(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return manager.getMaxStackSize();
        }

        @Override
        public void setChanged() {
            manager.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return manager.stillValid(player);
        }

        @Override
        public void clearContent() {
            manager.clearContent();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return manager.canPlaceItem(slot, stack);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return manager.getSlotsForFace(side);
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            // Governed by the allowAutomation config setting.
            return minechem.Config.ALLOW_AUTOMATION.get() && manager.canPlaceItem(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return minechem.Config.ALLOW_AUTOMATION.get() && side == Direction.DOWN && slot == OUTPUT_SLOT;
        }
    }
}
