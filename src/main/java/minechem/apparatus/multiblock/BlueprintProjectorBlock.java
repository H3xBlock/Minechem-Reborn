package minechem.apparatus.multiblock;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * Block of the blueprint projector (registry name "blueprintProjector"). Right-click opens the
 * projector, which holds one blueprint. Breaking it removes its ghosts first.
 *
 * <p>The block state "blueprint" is true while the projector holds a blueprint: the model then shows its lit texture.
 * The item uses the block model.
 */
public class BlueprintProjectorBlock extends BasicBlockContainer {
    /** True while the projector holds a blueprint; it picks the lit or the unlit texture. */
    public static final BooleanProperty HAS_BLUEPRINT = BooleanProperty.create("blueprint");

    public BlueprintProjectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_BLUEPRINT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_BLUEPRINT);
    }

    /** Hardness 2, resistance 50. */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F, 50.0F).noOcclusion();
    }

    @Override
    protected MapCodec<? extends BasicBlockContainer> codec() {
        return simpleCodec(BlueprintProjectorBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintProjectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, BlockEntityRegistry.BLUEPRINT_PROJECTOR.get(), BlueprintProjectorBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The ghosts go with the projector. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof BlueprintProjectorBlockEntity projector) {
            projector.destroyProjection();
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        return new BlueprintProjectorMenu(id, inventory, (BlueprintProjectorBlockEntity) blockEntity);
    }
}
