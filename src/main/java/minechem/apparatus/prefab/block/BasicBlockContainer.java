package minechem.apparatus.prefab.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base class for machine blocks. Right-click opens the block's menu, if it has one. Breaking the block drops its inventory.
 * Subclasses provide the block entity, the codec and, when they have a GUI, the menu.
 */
public abstract class BasicBlockContainer extends BaseEntityBlock {
    /**
     * The facing is a horizontal property, set from the placing player's direction when the block is placed.
     */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * The angle a machine's model turns by for its facing: west 90, north 180, east 270. South is the model's own direction.
     */
    public static float modelYRotation(Direction facing) {
        return switch (facing) {
            case WEST -> 90F;
            case NORTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }

    protected BasicBlockContainer(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Extra stacks dropped when the block breaks, after the inventory. Default: none. */
    public void addStacksDroppedOnBlockBreak(BlockEntity blockEntity, List<ItemStack> droppedStacks) {
    }

    /** Whether the inventory drops when the block breaks. Default: yes. */
    public boolean dropInventory() {
        return true;
    }

    /** Whether right-click opens a GUI. Blocks without a GUI return false here. */
    public boolean hasMenu() {
        return true;
    }

    /**
     * The block itself is not drawn: the apparatus is drawn by its block entity renderer, which carries the full model.
     * The block item still uses the placeholder JSON model.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Breaking the block: drop the inventory (and any extra stacks) before the block entity goes away.
     *
     * <p>Note (26.1): LevelChunk.setBlockState removes the block entity before it calls this method, so for a normal break
     * the block entity is already gone here. The drops of a Container block entity come from the vanilla
     * BlockEntity.preRemoveSideEffects, which a block entity overrides to choose its drops (see SynthesisBlockEntity).
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            List<ItemStack> droppedStacks = new ArrayList<>();
            if (dropInventory() && blockEntity instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        droppedStacks.add(stack);
                    }
                }
            }
            addStacksDroppedOnBlockBreak(blockEntity, droppedStacks);
            for (ItemStack stack : droppedStacks) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openMenuAt(level, pos, player);
    }

    /**
     * Right-click on the block at {@code pos}: opens the menu, as the block's own right-click does.
     * The reactor walls use it to open their manager's menu.
     */
    public InteractionResult openMenuAt(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && !player.isShiftKeyDown()) {
            if (hasMenu() && !level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inventory, p) -> createMenu(id, inventory, blockEntity), getName()),
                    buffer -> buffer.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Builds the server-side menu for the given block entity. Only called when {@link #hasMenu()} is true. */
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        throw new UnsupportedOperationException("hasMenu() is true but createMenu() is not overridden");
    }
}
