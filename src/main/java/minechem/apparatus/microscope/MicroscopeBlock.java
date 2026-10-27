package minechem.apparatus.microscope;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Block of the microscope (registry name "microscope"). Right-click opens its GUI. The block is currently drawn
 * with a placeholder cube.
 */
public class MicroscopeBlock extends BasicBlockContainer {
    public MicroscopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties createProperties() {
        // Not opaque: the block model is empty, so neighbours must keep their faces.
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F).noOcclusion();
    }

    @Override
    protected MapCodec<? extends BasicBlockContainer> codec() {
        return simpleCodec(MicroscopeBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroscopeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        return new MicroscopeMenu(id, inventory, (MicroscopeBlockEntity) blockEntity);
    }
}
