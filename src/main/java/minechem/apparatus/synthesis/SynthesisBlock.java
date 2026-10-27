package minechem.apparatus.synthesis;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
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
import minechem.registry.BlockEntityRegistry;

/**
 * The synthesis machine block (registry name "synthesis"). Right-click opens its GUI; the machine ticks on the server.
 * The block is drawn with a placeholder cube.
 */
public class SynthesisBlock extends BasicBlockContainer {
    public SynthesisBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties createProperties() {
        // Not opaque: the block model is empty, so neighbours must keep their faces.
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F).noOcclusion();
    }

    @Override
    protected MapCodec<? extends BasicBlockContainer> codec() {
        return simpleCodec(SynthesisBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SynthesisBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, BlockEntityRegistry.SYNTHESIS.get(), SynthesisBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        return new SynthesisMenu(id, inventory, (SynthesisBlockEntity) blockEntity);
    }
}
