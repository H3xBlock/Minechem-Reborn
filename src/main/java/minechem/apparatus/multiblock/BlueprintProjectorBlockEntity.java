package minechem.apparatus.multiblock;

import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.registry.BlockEntityRegistry;
import minechem.registry.BlockRegistry;
import minechem.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of the blueprint projector. It holds one blueprint. While the blueprint is not built,
 * each tick it projects the missing blocks as ghost blocks, column by column from the front of the blueprint; when every cell is
 * right, it builds the structure: the manager at its cell, then the walls and the air.
 *
 * <p>The projector hums while it projects. The completion flag is not saved: after a
 * reload the structure is checked and built again.
 */
public class BlueprintProjectorBlockEntity extends BaseTileEntity implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    /** The structure is built, so nothing is projected any more (not saved). */
    private boolean complete;

    public BlueprintProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BLUEPRINT_PROJECTOR.get(), pos, state);
    }

    /** The blueprint in the slot, or null when there is none. */
    public ReactorBlueprint blueprint() {
        return BlueprintItem.blueprintOf(items.get(0));
    }

    /** The server tick: projects while the blueprint is not built, and hums meanwhile. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BlueprintProjectorBlockEntity projector) {
        ReactorBlueprint blueprint = projector.blueprint();
        if (blueprint != null && !projector.complete) {
            projector.project(level, blueprint);
            projector.hum(level, pos);
        }
    }

    /**
     * The looping sound "minechem:projector" (volume 0.2, pitch 1, soundLength 20): the first call plays at once, then
     * one play every 20 calls, at the projector's block corner. Not saved.
     */
    private static final int HUM_INTERVAL = 20;
    private static final float HUM_VOLUME = 0.2F;
    private int humTimer = HUM_INTERVAL;

    private void hum(Level level, BlockPos pos) {
        if (humTimer == HUM_INTERVAL) {
            humTimer = 0;
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundRegistry.PROJECTOR.get(), SoundSource.BLOCKS, HUM_VOLUME, 1.0F);
        }
        humTimer++;
    }

    private enum Status { CORRECT, INCORRECT }

    /**
     * Projects the blueprint. Ghosts go up column by column (x); once a column is not right, the columns after it get no
     * ghosts, and their ghosts are removed. When every cell is right, the structure is built.
     */
    private void project(Level level, ReactorBlueprint blueprint) {
        Direction facing = getBlockState().getValue(BasicBlockContainer.FACING);
        ProjectionFrame frame = ProjectionFrame.of(getBlockPos(), facing, blueprint);
        boolean projecting = true;
        int incorrect = blueprint.totalSize();
        for (int x = 0; x < blueprint.xSize(); x++) {
            int verticalIncorrect = blueprint.ySize() * blueprint.zSize();
            for (int y = 0; y < blueprint.ySize(); y++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    if (!projecting) {
                        removeGhost(level, frame.at(x, y, z));
                        continue;
                    }
                    Status status = blueprint.isManager(x, y, z) ? Status.CORRECT : projectCell(level, frame, blueprint, x, y, z);
                    if (status == Status.CORRECT) {
                        verticalIncorrect--;
                        incorrect--;
                    }
                }
            }
            if (verticalIncorrect != 0) {
                projecting = false;
            }
        }
        if (incorrect == 0) {
            // Built once every cell is right. Projection runs only while the projector is not complete, so the build
            // always happens here, even if a manager is already there (it is replaced).
            complete = true;
            buildStructure(level, blueprint, frame);
        }
    }

    /** A missing wall gets a ghost, and the cell is not right yet. */
    private Status projectCell(Level level, ProjectionFrame frame, ReactorBlueprint blueprint, int x, int y, int z) {
        int id = blueprint.idAt(x, y, z);
        if (id == ReactorBlueprint.WILDCARD) {
            return Status.CORRECT;
        }
        BlockPos pos = frame.at(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (id == ReactorBlueprint.AIR) {
            return state.isAir() ? Status.CORRECT : Status.INCORRECT;
        }
        if (state.isAir()) {
            createGhost(level, pos, id);
            return Status.INCORRECT;
        }
        return state.is(ReactorBlockEntity.wallBlock(id)) ? Status.CORRECT : Status.INCORRECT;
    }

    private static void createGhost(Level level, BlockPos pos, int wall) {
        level.setBlock(pos, BlockRegistry.GHOST_BLOCK.get().defaultBlockState().setValue(GhostBlock.WALL, wall), 3);
        if (level.getBlockEntity(pos) instanceof GhostBlockEntity ghost) {
            ghost.setWall(wall);
        }
    }

    private static void removeGhost(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(BlockRegistry.GHOST_BLOCK.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Builds the manager first, then the walls and the air; a cell that is already right is left alone. */
    private void buildStructure(Level level, ReactorBlueprint blueprint, ProjectionFrame frame) {
        BlockPos managerPos = frame.at(blueprint.managerX(), blueprint.managerY(), blueprint.managerZ());
        BlockState managerState = (blueprint == ReactorBlueprint.FUSION
            ? BlockRegistry.FUSION_REACTOR.get()
            : BlockRegistry.FISSION_REACTOR.get()).defaultBlockState();
        level.setBlock(managerPos, managerState, 3);
        for (int x = 0; x < blueprint.xSize(); x++) {
            for (int y = 0; y < blueprint.ySize(); y++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    int id = blueprint.idAt(x, y, z);
                    if (blueprint.isManager(x, y, z) || id == ReactorBlueprint.WILDCARD) {
                        continue;
                    }
                    BlockPos pos = frame.at(x, y, z);
                    BlockState wanted = id == ReactorBlueprint.AIR
                        ? Blocks.AIR.defaultBlockState()
                        : ReactorBlockEntity.wallBlock(id).defaultBlockState();
                    if (!level.getBlockState(pos).equals(wanted)) {
                        level.setBlock(pos, wanted, 3);
                    }
                }
            }
        }
    }

    /**
     * Removes the ghosts of the blueprint, when the blueprint is taken out or the projector is
     * broken. Only the server removes them, and the block updates reach the client: the client's copy of the projector gets
     * its slot from the menu sync (setItem), which must not remove the client's ghosts while the server keeps them.
     */
    public void destroyProjection() {
        ReactorBlueprint blueprint = blueprint();
        Level level = getLevel();
        if (blueprint == null || level == null || level.isClientSide()) {
            return;
        }
        ProjectionFrame frame = ProjectionFrame.of(getBlockPos(), getBlockState().getValue(BasicBlockContainer.FACING), blueprint);
        for (int x = 0; x < blueprint.xSize(); x++) {
            for (int y = 0; y < blueprint.ySize(); y++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    removeGhost(level, frame.at(x, y, z));
                }
            }
        }
    }

    /** The slot changes: the projection of the old blueprint goes, and the structure is checked again for the new one. */
    private void changeSlot(Runnable change) {
        destroyProjection();
        change.run();
        complete = false;
        setChanged();
        syncBlueprintState();
    }

    /**
     * The block's "blueprint" state follows the slot (the blueprint is set), which picks the lit or the
     * unlit texture of the model.
     */
    private void syncBlueprintState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        boolean has = blueprint() != null;
        if (state.getValue(BlueprintProjectorBlock.HAS_BLUEPRINT) != has) {
            level.setBlock(getBlockPos(), state.setValue(BlueprintProjectorBlock.HAS_BLUEPRINT, has), 3);
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack[] removed = new ItemStack[1];
        changeSlot(() -> removed[0] = ContainerHelper.removeItem(items, slot, amount));
        return removed[0];
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack[] removed = new ItemStack[1];
        changeSlot(() -> removed[0] = ContainerHelper.takeItem(items, slot));
        return removed[0];
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        changeSlot(() -> items.set(slot, stack));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BlueprintItem.blueprintOf(stack) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        changeSlot(() -> items.set(0, ItemStack.EMPTY));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        complete = false;
    }
}
