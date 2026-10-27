package minechem.apparatus.multiblock;

import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A wall of a reactor that gives what it receives to its manager.
 * It keeps energy of its own (up to {@link #ENERGY_CAPACITY}); every tick it gives that energy to
 * the manager, and it shows the manager's items to the outside, so a pipe or hopper at the wall reaches the reactor's slots.
 *
 * <p>The wall has no manager while the structure is not complete. The manager links and unlinks walls when it checks its
 * structure ({@link ReactorBlockEntity}).
 */
public class ReactorWallBlockEntity extends BlockEntity {
    /** The energy storage capacity of a wall (the energy packet size, 100). */
    public static final int ENERGY_CAPACITY = 100;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY);
    private BlockPos manager;

    public ReactorWallBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.REACTOR_WALL.get(), pos, state);
    }

    /** The energy storage behind the NeoForge energy capability. */
    public SimpleEnergyHandler getEnergy() {
        return energy;
    }

    /** The manager's position, or null while the wall is not linked. */
    public void setManager(BlockPos managerPos) {
        this.manager = managerPos == null ? null : managerPos.immutable();
        setChanged();
    }

    /** The linked manager, when it is there and its structure is complete; otherwise null. */
    public ReactorBlockEntity linkedManager() {
        Level level = getLevel();
        if (manager == null || level == null) {
            return null;
        }
        return level.getBlockEntity(manager) instanceof ReactorBlockEntity reactor && reactor.isComplete() ? reactor : null;
    }

    /**
     * The item side of the wall, for the NeoForge item capability: the manager's items with the proxy rules.
     * Null while there is no complete manager, so nothing reaches the wall's neighbours.
     */
    public WorldlyContainerWrapper itemHandler(Direction side) {
        ReactorBlockEntity reactor = linkedManager();
        if (reactor == null || side == null) {
            return null;
        }
        WorldlyContainer view = reactor.proxyView();
        return new WorldlyContainerWrapper(view, side);
    }

    /**
     * One server tick: the stored energy is given to the manager, as much as it accepts; what is
     * accepted leaves the wall.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorWallBlockEntity wall) {
        ReactorBlockEntity reactor = wall.linkedManager();
        if (reactor == null) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = reactor.getEnergy().insert(wall.energy.getAmountAsInt(), transaction);
            if (accepted > 0 && wall.energy.extract(accepted, transaction) == accepted) {
                transaction.commit();
            }
        }
    }
}
