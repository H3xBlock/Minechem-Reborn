package minechem.apparatus.multiblock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import minechem.registry.BlockEntityRegistry;

/**
 * Outline of a block the blueprint projector still needs (registry name "ghostBlock"). It is not solid and cannot be broken.
 * Right-click with the block it stands for places that block and takes one from the hand, as long as the player is within
 * eight blocks.
 *
 * <p>The block state "wall" is the structure id of the wall it stands for (1 for the first wall, 2 for the
 * second), and picks the wall texture (ghost_wall_1.png or ghost_wall_2.png), drawn translucent.
 */
public class GhostBlock extends BaseEntityBlock {
    /** A player farther than 8 blocks (a squared distance of 64) cannot place it. */
    private static final double REACH_SQUARED = 64.0;

    /** The wall this ghost stands for: {@link ReactorBlueprint#WALL_1} or {@link ReactorBlueprint#WALL_2}. */
    public static final IntegerProperty WALL = IntegerProperty.create("wall", ReactorBlueprint.WALL_1, ReactorBlueprint.WALL_2);

    public GhostBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WALL, ReactorBlueprint.WALL_1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL);
    }

    /** Hardness 1000 (not breakable), no collision, not opaque. */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of().strength(1000.0F).noCollision().noOcclusion();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(GhostBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GhostBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GhostBlockEntity ghost)) {
            return InteractionResult.PASS;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQUARED) {
            return InteractionResult.PASS;
        }
        Block wall = ReactorBlockEntity.wallBlock(ghost.wall());
        if (stack.isEmpty() || !stack.is(wall.asItem())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, wall.defaultBlockState(), Block.UPDATE_ALL);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
