package minechem.apparatus.multiblock;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Walls of the fusion chamber (registry names {@code fusion_wall_1} and {@code fusion_wall_2}). Both are walls with a
 * {@link ReactorWallBlockEntity}. They are placed by the blueprint projector or by hand (a ghost block in the projection, see
 * {@link GhostBlock}).
 *
 * <p>The walls are currently drawn with a placeholder model.
 */
public class ReactorWallBlock extends BaseEntityBlock {
    public ReactorWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 2, resistance 50. */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F, 50.0F);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ReactorWallBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Right-click on a wall opens the menu of its manager, when the structure is complete.
     * Otherwise nothing happens.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ReactorWallBlockEntity wall)) {
            return InteractionResult.PASS;
        }
        ReactorBlockEntity manager = wall.linkedManager();
        if (manager == null || !(level.getBlockState(manager.getBlockPos()).getBlock() instanceof BasicBlockContainer container)) {
            return InteractionResult.PASS;
        }
        return container.openMenuAt(level, manager.getBlockPos(), player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorWallBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, BlockEntityRegistry.REACTOR_WALL.get(), ReactorWallBlockEntity::serverTick);
    }
}
