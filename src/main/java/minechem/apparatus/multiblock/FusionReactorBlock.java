package minechem.apparatus.multiblock;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Manager block of the Chemical Fusion Chamber (registry name {@code fusion_reactor}).
 * It has no item, so it is made by the blueprint projector. Right-click opens the chamber
 * once its structure is complete.
 *
 * <p>Drawn with the same chamber texture on every side.
 */
public class FusionReactorBlock extends BasicBlockContainer {
    public FusionReactorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 2, resistance 50. */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F, 50.0F).noOcclusion();
    }

    @Override
    protected MapCodec<? extends BasicBlockContainer> codec() {
        return simpleCodec(FusionReactorBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, BlockEntityRegistry.FUSION_REACTOR.get(), ReactorBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        return new FusionReactorMenu(id, inventory, (FusionReactorBlockEntity) blockEntity);
    }
}
